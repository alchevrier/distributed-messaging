# Architecture Decision Records

This directory contains Architecture Decision Records (ADRs) for the distributed messaging system project.

## What are ADRs?

Architecture Decision Records document the key architectural decisions made during the development of this system. Each ADR captures:
- The context and problem being addressed
- The decision made
- The consequences (both positive and negative)

## Index of ADRs

| ADR | Title | Status |
|-----|-------|--------|
| [0001](0001-use-architecture-decision-records.md) | Use Architecture Decision Records | Accepted |
| [0002](0002-choose-java-25-as-implementation-language.md) | Choose Java 25 as Implementation Language | Accepted |
| [0003](0003-use-append-only-log-storage.md) | Use Append-Only Log Storage | Accepted |
| [0004](0004-use-binary-wire-protocol.md) | Use Binary Wire Protocol | Accepted |
| [0005](0005-use-virtual-threads-for-concurrency.md) | Use Virtual Threads for Concurrency | Accepted |
| [0006](0006-phased-implementation-approach.md) | Phased Implementation Approach | Accepted |
| [0007](0007-choose-gradle-as-build-tool.md) | Choose Gradle as Build Tool | Accepted |
| [0008](0008-phase-1-project-structure.md) | Phase 1 Project Structure | Accepted |

## ADR Lifecycle

- **Proposed**: Decision under consideration
- **Accepted**: Decision approved and being implemented
- **Deprecated**: Decision no longer applies but kept for historical record
- **Superseded**: Replaced by a newer ADR (reference included)

## Creating a New ADR

1. Copy the template below
2. Number it sequentially (e.g., `0007-title.md`)
3. Fill in all sections
4. Update this README index

### Template

```markdown
# ADR-NNNN: [Short Title]

## Status
[Proposed | Accepted | Deprecated | Superseded by ADR-XXXX]

## Context
What is the issue that we're seeing that is motivating this decision or change?

## Decision
What is the change that we're proposing and/or doing?

## Consequences
What becomes easier or more difficult to do because of this change?

### Positive
- Benefit 1
- Benefit 2

### Negative
- Cost 1
- Cost 2

## References
- [Link to relevant resources]
```

## Reading Order

For newcomers to the project, we recommend reading ADRs in this order:
1. ADR-0007: Build tool selection
4. ADR-0006: Implementation roadmap
5. ADR-0008: Phase 1 project structure
6. ADR-0002: Language choice rationale
3. ADR-0006: Implementation roadmap
4. ADR-0003, 0004, 0005: Core technical decisions

## References
- [Architecture Decision Records](https://adr.github.io/)
- [Documenting Architecture Decisions](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions)
