# ADR-0008: Phase 1 Project Structure

## Status
Accepted

## Context
The aim of Phase 1 is to have a simple consumer/producer as a proof-of-concept for the log-based storage-engine we therefore decided to orchestrate this as multi-module mono-repo which aims to be extended later on individually. 

Options considered:
1. **Multi-repos** - Each repository can be deployed independently
2. **Multi-module mono-repo** - Centralized and modular way which can be worked on by multiple-teams
3. **Mono-repo** - Everything in a single-repo and only using packages to separate each aspect

## Decision
We will use **Multi-module mono-repo** as our project structure.

We chose the following modules:
- message -> common DTOs meant to be used by consumer, producer, endpoint
- log-storage-engine -> How the messages are organized in the broker filesystem and how they can be fetched/persisted
- endpoint -> REST API via OpenAPI uses the log-storage-engine to fetch/persist messages on the log
- consumer -> dedicated consumer API which is aimed to be used by consumers, will call the REST API via HTTP client
- producer -> dedicated producer API which is aimed to be used by producers, will call the REST API via HTTP client

### Architecture Diagram

```
┌──────────┐                                    ┌──────────┐
│ Consumer │                                    │ Producer │
│  Client  │                                    │  Client  │
└────┬─────┘                                    └────┬─────┘
     │                                               │
     │ HTTP/2 + TLS 1.3                             │ HTTP/2 + TLS 1.3
     │ (REST API)                                   │ (REST API)
     │                                               │
     └───────────────────┬───────────────────────────┘
                         ↓
                 ┌───────────────┐
                 │   Endpoint    │
                 │  (REST API)   │
                 │  Spring Boot  │
                 └───────┬───────┘
                         │
                         │ Direct method calls
                         ↓
                 ┌───────────────────┐
                 │ Log Storage Engine│
                 │  (Core Storage)   │
                 └───────┬───────────┘
                         │
                         │ File I/O (NIO)
                         ↓
                 ┌───────────────────┐
                 │   Filesystem      │
                 │ - *.log (segments)│
                 │ - *.index         │
                 └───────────────────┘

```

## Testing strategy

We will be using JUnit5 as it provides extensive support with Spring/Java as well as providing us a simple of using data sources for parametrized test. Unit tests will be located at src/test/java and will be located alongside integration tests (prefixed by IT). 

## Consequences

### Positive
- **Gradle support**: Provides capability to work with multi-modules mono-repos easily and at a very competitive speed
- **Use of OpenAPI documents**: Allow us to use document-driven development hence avoiding large verbose OpenAPI annotations in Spring
- **Use of Springfox**: Provides website that use the documentation written to allow the user to see/test the API out-of-the-box
- **Use of Spring**: Large existing ecosystem, endpoints implemented via plugin reading the OpenAPI documents. Provides many facilities for integration testing as well as virtual thread capabilities. Spring Boot is a temporary choice for Phase 1, will be replaced when migrating to binary protocol (see ADR-0004)
- **Use of multi-module**: Consumer/Producer application have no need whatsoever to depend on code that is not related to their immediate need. 
- **Use of JUnit 5**: Parametrized Tests facilitates multiple scenario testing via String, CSV files....
- **Future-Proof**: Separating in multiple modules in theory will allow our users to have a continuity of service while getting more and more capabilties in the future.
- **More productive**: I currently do not have knowledge on how to build a custom binary protocol, to me it is much easier to use Spring to get up and running faster
- **Debugging much easier**: I have a lot of experience debugging production Spring-Boot application

### Negative
- **Massive loss of performance**: Using REST API adds overhead which a binary protocol simply does not have 

### Mitigation Strategies
- See ADR-0004 for migration path to binary protocol which will eliminate REST overhead

## References
- [Gradle Multi-Module Projects](https://docs.gradle.org/current/userguide/multi_project_builds.html)
- [Gradle Build Cache](https://docs.gradle.org/current/userguide/build_cache.html)
- [Gradle Kotlin DSL](https://docs.gradle.org/current/userguide/kotlin_dsl.html)
- [Structuring Large Projects](https://docs.gradle.org/current/userguide/structuring_software_products.html)
