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
replication scan path.

**Virtual Threads** — Used for broker I/O handling. Justified by I/O-bound
workload profile: high concurrency, low CPU intensity per connection.

## Architecture decisions

All non-trivial decisions are documented as ADRs in [`docs/adr`](docs/adr).
Each ADR captures the options considered, the tradeoff made, and the load
profile that drove the decision.

| ADR | Decision | Status |
|-----|----------|--------|
| [ADR-0001](docs/adr/0001-use-architecture-decision-records.md) | Use Architecture Decision Records | Accepted |
| [ADR-0002](docs/adr/0002-choose-java-25-as-implementation-language.md) | Java 25 as Implementation Language | Accepted |
| [ADR-0003](docs/adr/0003-use-append-only-log-storage.md) | Append-Only Log Storage | Accepted |
| [ADR-0004](docs/adr/0004-use-binary-wire-protocol.md) | Binary Wire Protocol (future) / REST API (Phase 1) | Accepted |
| [ADR-0005](docs/adr/0005-use-virtual-threads-for-concurrency.md) | Virtual Threads for Concurrency | Accepted |
| [ADR-0006](docs/adr/0006-phased-implementation-approach.md) | Phased Implementation Approach | Accepted |
| [ADR-0007](docs/adr/0007-choose-gradle-as-build-tool.md) | Gradle as Build Tool | Accepted |
| [ADR-0008](docs/adr/0008-phase-1-project-structure.md) | Phase 1 Project Structure | Accepted |
| [ADR-0009](docs/adr/0009-use-binary-wire-protocol.md) | Use Binary Wire Protocol (Phase 2) | Accepted |
| [ADR-0010](docs/adr/0010-phase-2-project-structure.md) | Phase 2 Project Structure | Accepted |
| [ADR-0011](docs/adr/0011-phase-3-project-structure.md) | Phase 3 Project Structure | Accepted |

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
| 4 | Raft consensus — leader election + log replication | 🚧 In progress |
| 5 | Off-heap storage, zero-copy read path, JMH benchmarks | 📅 Scheduled |

**Phase 5 target:** Off-heap `MemorySegment` slab allocator, 16-byte packed
log entries, `FileChannel.transferTo()` zero-copy reads, JMH benchmark suite
on bare-metal Linux. Goal: measure actual latency cost of each architectural
layer under load.

## Build and run

**Prerequisites:** Java 25+, Gradle 8+

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
- **Testing:** JUnit 5 + AssertJ, Spock (Groovy) for storage layer
- **Consensus:** Raft (hand-rolled — no external library)

## License

MIT License — see [LICENSE](LICENSE) for details.
