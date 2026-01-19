# distributed-messaging

A Kafka-like distributed messaging system built from scratch to learn core concepts of distributed systems, message queues, and modern Java concurrency.

## Overview

This project implements a distributed messaging platform similar to Apache Kafka, focusing on learning fundamental concepts:
- Log-structured storage
- Producer/Consumer patterns
- Partitioning and replication (future phases)
- Modern Java features (Java 25, Virtual Threads)

## Architecture

All architectural decisions are documented using Architecture Decision Records (ADRs). See the [docs/adr](docs/adr/) directory for detailed rationale.

### Key Decisions

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

## Project Structure

```
distributed-messaging/
├── message/              # Common DTOs
├── log-storage-engine/   # Core storage layer
├── endpoint/             # REST API (Spring Boot)
├── consumer/             # Consumer client library
├── producer/             # Producer client library
└── docs/                 # Documentation & ADRs
```

See [ADR-0008](docs/adr/0008-phase-1-project-structure.md) for detailed architecture diagram.

## Current Phase: Phase 1 - Foundation

**Goals:**
- Single broker, single partition
- Basic produce/consume functionality
- File-based append-only log storage
- REST API with OpenAPI specification

**What's implemented:**
- [ ] Log storage engine with NIO
- [ ] REST API endpoints
- [ ] Producer client
- [ ] Consumer client
- [ ] OpenAPI specification

## Tech Stack

- **Language**: Java 25 (Virtual Threads)
- **Build Tool**: Gradle (multi-module)
- **Framework**: Spring Boot (Phase 1 only)
- **API**: REST + OpenAPI (migrating to binary protocol later)
- **Storage**: NIO FileChannel, append-only logs
- **Testing**: JUnit 5 + AssertJ

## Getting Started

### Prerequisites
- Java 25 or higher
- Gradle 8+

### Building

```bash
# Build all modules
./gradlew build

# Build specific module
./gradlew :log-storage-engine:build

# Run tests
./gradlew test
```

### Running

```bash
# Start the broker
./gradlew :endpoint:bootRun

# Producer example (TBD)
# Consumer example (TBD)
```

## Documentation

- [Architecture Decision Records](docs/adr/) - All architectural decisions with rationale
- [ADR Index](docs/adr/README.md) - Complete list of ADRs

## Learning Goals

This project is built for learning:
- Distributed systems fundamentals
- Log-structured storage design
- Modern Java concurrency (Virtual Threads)
- Protocol design (REST → Binary migration)
- Multi-module project architecture
- Test-driven development

## License

MIT License - See LICENSE file for details
