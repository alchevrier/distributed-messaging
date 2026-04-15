# distributed-messaging

A from-scratch implementation of a distributed messaging system — append-only
log storage, binary TCP wire protocol, MurmurHash3 partitioning, and Raft
consensus for replication. Built to understand the implementation-level
decisions that production systems make at each layer, and where the tradeoffs
actually live.

## What's implemented

**Storage layer** — Append-only log with NIO `FileChannel`, segment-based
layout, index files for O(log n) offset lookup, and crash recovery on startup.

**Wire protocol** — Custom binary TCP protocol over Java NIO (non-blocking
I/O). Intentional choice over REST: frame-level control, no HTTP overhead,
explicit serialisation/deserialisation boundary.

**Partitioning** — MurmurHash3 on message key, least-used partition assignment.
Partition metadata managed by broker.

**Concurrency model** — `ReentrantReadWriteLock` on the log (read-heavy: N
concurrent consumers hold read lock simultaneously; append acquires write lock
exclusively). `AtomicLongArray` for partition offsets — lock-free on the hot
path via CAS, justified by single-variable write pattern.

**Raft consensus** — Leader election (happy path, split vote, stale leader
rejection) and log replication (`AppendEntries`, conflict resolution, commit
index advancement via majority check). Zero-allocation `LogScanFunction`
`@FunctionalInterface` with primitive `long` index to avoid boxing on the
replication scan path. Election timer correctly reset on `AppendEntries`
receipt (§5.2) and on vote grant only — not on every `RequestVote` to prevent
timer disruption by non-viable candidates. `CompositeRaftLog` separates indexing from storage:
`MemoryMappedRaftIndexer` for O(1) offset lookup via mmap, `FileChannelRaftLogger`
for sequential append writes — persistent across restarts.

**Raft integration tests** — Full 3-node cluster integration tests running
against real TCP, real timers, and real disk: leader election, re-election
after leader failure, `ACK=ALL` / `ACK=LEADER` / `ACK=NONE` replication modes,
and majority-quorum append with one node down. Pending futures drained
immediately on step-down so clients fail fast rather than waiting for the
broker timeout.

**Virtual Threads** — Used for broker I/O handling and concurrent heartbeat
dispatch. Each heartbeat cycle fires one virtual thread per peer so a slow or
dead peer cannot block heartbeats to the remaining cluster. Justified by
I/O-bound workload profile: high concurrency, low CPU intensity per connection.

## Architecture decisions

All non-trivial decisions are documented as ADRs in [`docs/adr`](docs/adr).
Each ADR captures the options considered, the tradeoff made, and the load
profile that drove the decision.

| ADR                                                                    | Decision | Status |
|------------------------------------------------------------------------|----------|--------|
| [ADR-0001](docs/adr/0001-use-architecture-decision-records.md)         | Use Architecture Decision Records | Accepted |
| [ADR-0002](docs/adr/0002-choose-java-25-as-implementation-language.md) | Java 25 as Implementation Language | Accepted |
| [ADR-0003](docs/adr/0003-use-append-only-log-storage.md)               | Append-Only Log Storage | Accepted |
| [ADR-0004](docs/adr/0004-use-binary-wire-protocol.md)                  | Binary Wire Protocol (future) / REST API (Phase 1) | Accepted |
| [ADR-0005](docs/adr/0005-use-virtual-threads-for-concurrency.md)       | Virtual Threads for Concurrency | Accepted |
| [ADR-0006](docs/adr/0006-phased-implementation-approach.md)            | Phased Implementation Approach | Accepted |
| [ADR-0007](docs/adr/0007-choose-gradle-as-build-tool.md)               | Gradle as Build Tool | Accepted |
| [ADR-0008](docs/adr/0008-phase-1-project-structure.md)                 | Phase 1 Project Structure | Accepted |
| [ADR-0009](docs/adr/0009-use-binary-wire-protocol.md)                  | Use Binary Wire Protocol (Phase 2) | Accepted |
| [ADR-0010](docs/adr/0010-phase-2-project-structure.md)                 | Phase 2 Project Structure | Accepted |
| [ADR-0011](docs/adr/0011-phase-3-project-structure.md)                 | Phase 3 Project Structure | Accepted |
| [ADR-0012](docs/adr/0012-phase-4-project-structure.md)                 | Phase 4 Project Structure | Accepted |
| [ADR-0013](docs/adr/0013-phase-5-production-readiness.md)                 | ADR-0013: Phase 5 Production Readiness | Accepted |

## Project structure

```
distributed-messaging/
├── log-storage-engine/   # Append-only log, NIO FileChannel, index, segments
├── broker/               # Broker core — partition management, TCP handler
├── raft/                 # Raft consensus — election, log replication
├── cluster-raft/         # Raft integration with broker
├── tcp-server/           # NIO TCP server — non-blocking accept/read/write
├── tcp-client/           # TCP client — frame encoding/decoding
├── producer/             # Producer client library
├── consumer/             # Consumer client library
├── message/              # Wire types — Topic, Message, ProduceRequest
├── demo-app/             # End-to-end demo
└── docs/adr/             # Architecture Decision Records
```

## Phase status

| Phase | Focus | Status |
|-------|-------|--------|
| 1 | Append-only log, REST API, single broker | ✅ Done |
| 2 | Binary TCP protocol, NIO, serialisation | ✅ Done |
| 3 | Partitioning, MurmurHash3, multi-partition broker | ✅ Done |
| 4 | Raft consensus — leader election + log replication + integration tests | ✅ Done |
| 5 | Off-heap storage, zero-allocation read path, JMH benchmarks | ✅ Done |

## Performance

All numbers from JMH `SampleTime` mode on bare-metal Linux. JFR attached to each run to confirm GC behaviour.

### Write path (append)

| Percentile | Baseline | After Phase 5 | Δ |
|---|---|---|---|
| p50 | 3,900 ns | 4,432 ns | +14% |
| p99 | 6,500 ns | 6,584 ns | flat |
| p99.99 | 306,000 ns | 70,519 ns | **−77%** |
| p100 | 40,000,000 ns | 11,993,000 ns | **−70%** |
| Max GC pause | 655 ms (growing) | 2 ms flat | **eliminated** |

Root cause: `ByteBuffer.allocate()` on every append + `HashMap<Long, Long>` entry allocations drove Old gen growth and 655 ms stop-the-world pauses.

Fix: pre-allocated `MemorySegment` header slab (off-heap, `Arena.ofShared()`), binary search flat index slab replacing the `HashMap`. Scatter-gather `FileChannel.write(ByteBuffer[], 0, 2)` — one syscall for header + payload.

### Read path

| Percentile | Baseline | After Phase 5 | Δ |
|---|---|---|---|
| p50 | 801 ns | 465 ns | **−42%** |
| p99 | 1,114 ns | 734 ns | **−34%** |
| p99.99 | 14,236 ns | 13,984 ns | −2% |
| p100 | 1,234,944 ns | 718,848 ns | **−42%** |
| Max GC pause | ~1.5 ms | 2 ms flat | stable |

Root cause: two `FileChannel.read()` syscalls per message (length then data) + shared `ByteBuffer` that introduced a concurrency hazard under `ReentrantReadWriteLock` (concurrent readers racing on `clear()`/`flip()`).

Fix: single `FileChannel.read()` into the caller-provided buffer — full record (header + payload) in one syscall. Extract length with `getInt()`, skip the 8-byte offset field, remaining bytes are the payload. Zero allocation inside the storage engine. Topic-partition routing keys pre-computed at topic creation to eliminate `String` concatenation on the hot path.

Full methodology, JMH results, and JFR traces in [`benchmarks/results/`](benchmarks/results/).

## Build and run

**Prerequisites:** Java 25+, Gradle 9+

```bash
# Build all modules
./gradlew build

# Run tests
./gradlew test

# Start broker
./gradlew :broker:bootRun

# Run demo
./gradlew :demo-app:bootRun
```

## Tech stack

- **Language:** Java 25
- **Build:** Gradle (multi-module)
- **I/O:** Java NIO — `FileChannel`, `ByteBuffer`, non-blocking TCP
- **Testing:** JUnit 5 + AssertJ, Spock (Groovy) + Awaitility for unit and integration tests
- **Consensus:** Raft (hand-rolled — no external library)

## License

MIT License — see [LICENSE](LICENSE) for details.
