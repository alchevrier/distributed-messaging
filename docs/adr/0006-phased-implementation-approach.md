# ADR-0006: Phased Implementation Approach

## Status
Accepted

## Context
Building a complete distributed messaging system is complex. We need to decide on an implementation strategy that:
- Delivers working functionality incrementally
- Allows for learning and experimentation at each phase
- Provides testable milestones
- Balances completeness with learning goals
- Manages scope to prevent over-engineering

Options:
1. **Big Bang**: Build everything at once
2. **Phased approach**: Incremental complexity
3. **Spike-based**: Explore specific features deeply
4. **Minimal viable product** then iterate

## Decision
We will use a **phased implementation approach**, building complexity incrementally across 5 phases.

Each phase will be:
- Fully functional and testable
- Building upon previous phases
- Demonstrating specific distributed systems concepts
- Merged to main branch when complete

## Consequences

### Positive
- **Reduced risk**: Each phase delivers working software
- **Learning focus**: Can deeply understand each concept before moving on
- **Early validation**: Can test and get feedback early
- **Flexibility**: Can adjust priorities between phases
- **Motivation**: Regular sense of accomplishment
- **Portfolio value**: Can showcase progressive complexity

### Negative
- **Refactoring needed**: May need to refactor earlier phases as design evolves
- **Delayed features**: Advanced features come later
- **Integration complexity**: Must ensure phases integrate smoothly
- **Testing overhead**: Need comprehensive tests at each phase

## Implementation Phases

### Phase 1: Single Broker Foundation (Weeks 1-2)
**Goal**: Basic produce/consume without distribution

**Components**:
- Single broker process
- File-based append-only log
- Single partition per topic
- Basic Producer client (synchronous)
- Basic Consumer client (manual offset tracking)
- Binary protocol (produce/fetch)

**Success Criteria**:
- Can produce messages to topic
- Can consume messages by offset
- Messages persist across broker restarts
- Unit and integration tests pass

**Learning Focus**:
- Log-structured storage
- Binary protocol design
- Virtual threads for connections

---

### Phase 2: Partitioning & Multiple Brokers (Weeks 3-4)
**Goal**: Horizontal scaling through partitioning

**Components**:
- Multi-partition topics
- Producer partitioner logic (round-robin, hash-based)
- Consumer partition assignment
- Metadata service (topic -> partition -> broker mapping)
- Multiple broker processes (no replication yet)

**Success Criteria**:
- Topic with 4 partitions across 2 brokers
- Producer distributes messages across partitions
- Consumer reads from assigned partitions
- Load balanced message distribution

**Learning Focus**:
- Partitioning strategies
- Metadata management
- Load distribution

---

### Phase 3: Replication & Fault Tolerance (Weeks 5-7)
**Goal**: High availability through replication

**Components**:
- Leader-follower replication
- In-Sync Replica (ISR) tracking
- Leader election (simplified Raft or ZooKeeper integration)
- Replica synchronization protocol
- Failover handling

**Success Criteria**:
- Partition replicated across 3 brokers
- Leader election on broker failure
- No data loss with ack=all
- Automatic failover

**Learning Focus**:
- Consensus algorithms
- Replication protocols
- Failure detection and recovery

---

### Phase 4: Consumer Groups (Weeks 8-9)
**Goal**: Parallel consumption with load balancing

**Components**:
- Consumer group coordinator
- Rebalancing protocol
- Offset commit/fetch protocol
- Distributed offset storage
- Heartbeat mechanism

**Success Criteria**:
- Multiple consumers in group
- Automatic partition assignment
- Rebalancing on consumer join/leave
- Offset persistence

**Learning Focus**:
- Coordination protocols
- Group membership
- Rebalancing strategies

---

### Phase 5: Observability & Dashboard (Weeks 10-12)
**Goal**: Production-ready monitoring

**Components**:
- Metrics collection (Prometheus)
- React-based dashboard
- Grafana integration
- Consumer lag monitoring
- Cluster health visualization
- Message throughput graphs

**Success Criteria**:
- Real-time metrics dashboard
- Consumer lag alerts
- Broker health status
- Topic management UI

**Learning Focus**:
- Observability patterns
- React/TypeScript
- Prometheus integration
- Containerization with Podman

---

## Phase Dependencies

```
Phase 1 (Foundation)
    ↓
Phase 2 (Partitioning) ← Can develop in parallel with Phase 3
    ↓
Phase 3 (Replication)
    ↓
Phase 4 (Consumer Groups)
    ↓
Phase 5 (Observability)
```

## Testing Strategy per Phase
- **Unit tests**: Core logic, algorithms
- **Integration tests**: Component interaction
- **System tests**: End-to-end scenarios (Phase 2+)
- **Performance tests**: Throughput, latency benchmarks (Phase 3+)
- **Chaos tests**: Failure scenarios (Phase 3+)

## Documentation per Phase
- Update ADRs as decisions are made
- README with current capabilities
- API documentation
- Setup/running instructions
- Architecture diagrams

## Time Estimates
- **Phase 1**: 2 weeks (foundation is critical)
- **Phase 2**: 2 weeks (partitioning logic)
- **Phase 3**: 3 weeks (replication is complex)
- **Phase 4**: 2 weeks (builds on Phase 3)
- **Phase 5**: 3 weeks (frontend + integration)

**Total**: ~12 weeks (3 months)

Actual time may vary based on:
- Available hours per week
- Complexity discoveries
- Scope adjustments

## Success Metrics
- Working software at each phase
- Comprehensive test coverage (>80%)
- Clear documentation
- Portfolio-worthy demonstration
- Deep understanding of concepts

## References
- [Building Microservices by Sam Newman](https://samnewman.io/books/building_microservices_2nd_edition/)
- [Release It! by Michael Nygard](https://pragprog.com/titles/mnee2/release-it-second-edition/)
