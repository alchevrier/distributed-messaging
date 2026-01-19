# ADR-0002: Choose Java 25 as Implementation Language

## Status
Accepted

## Context
We need to select a programming language for implementing a distributed messaging system similar to Kafka. The language choice will impact:
- Performance characteristics (throughput, latency)
- Concurrency model
- Available libraries and ecosystem
- Learning objectives (modern Java features, distributed systems)
- Production readiness and tooling

Options considered:
1. **Java 25** - Modern Java with virtual threads
2. **Go** - Lightweight concurrency, simpler deployment
3. **Rust** - Maximum performance, memory safety

## Decision
We will use **Java 25** as the implementation language.

Key factors:
- **Virtual Threads (Project Loom)**: Native support for lightweight concurrency, perfect for handling thousands of client connections efficiently
- **Real-world alignment**: Kafka itself is written in Java/Scala, making this choice relevant for learning
- **Mature ecosystem**: Excellent tooling, libraries, and monitoring solutions
- **Learning objectives**: Opportunity to apply modern Java features (records, pattern matching, virtual threads)
- **Professional relevance**: Aligns with my work experience as a Java Developer

## Consequences

### Positive
- **Modern concurrency**: Virtual threads eliminate traditional thread pool complexity
- **Production-grade JVM**: Mature garbage collection, profiling, monitoring tools
- **Strong typing**: Compile-time safety for distributed system protocols
- **Rich ecosystem**: Excellent libraries for networking, serialization, testing
- **Learning value**: Hands-on experience with cutting-edge Java features
- **Existing professional experience**: Lead developer has professional experience in Java up to 21

### Negative
- **Memory footprint**: JVM overhead compared to Go/Rust
- **Startup time**: Slower than compiled binaries (can be mitigated with GraalVM native image later)
- **Complexity**: More verbose than Go, steeper learning curve than scripting languages
- **GC pauses**: Potential impact on tail latencies (though modern GCs like ZGC/Generational ZGC/Shenandoah help)

### Mitigation Strategies
- Use modern GC algorithms (ZGC, Generational ZGC, Shenandoah) for low-latency requirements
- Consider GraalVM native image for deployment if startup time becomes critical
- Leverage virtual threads to avoid traditional threading complexity
- Use records and modern Java syntax to reduce verbosity

## References
- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [Apache Kafka Architecture](https://kafka.apache.org/documentation/#design)
