# ADR-0005: Use Virtual Threads for Concurrency

## Status
Accepted

## Context
The broker must handle thousands of concurrent client connections efficiently. We need to choose a concurrency model that:
- Scales to many concurrent connections (10,000+)
- Provides good throughput and low latency
- Simplifies code compared to async/callback models
- Utilizes modern Java capabilities

Concurrency options:
1. **Virtual Threads (Project Loom)** - Lightweight, blocking-style code
2. **Traditional Thread Pool** - Classic approach, limited scalability
3. **Async/CompletableFuture** - Non-blocking but complex
4. **Reactive Streams** (Reactor/RxJava) - Powerful but steep learning curve
5. **Netty/NIO** - High performance but complex

## Decision
We will use **Virtual Threads** as the primary concurrency model.

Implementation approach:
- One virtual thread per client connection
- Blocking I/O code (simple, readable)
- Virtual thread executor for request handling
- Platform threads for CPU-intensive operations

```java
// Conceptual example
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

serverSocket.accept().thenAccept(socket -> 
    executor.submit(() -> handleClient(socket))
);
```

## Consequences

### Positive
- **Massive scalability**: Can handle 100,000+ virtual threads on single JVM
- **Simple code**: Write straightforward blocking code instead of callbacks
- **No thread pool tuning**: Don't need to configure thread pool sizes
- **Debuggability**: Stack traces are meaningful, debugging is easier
- **Low overhead**: Virtual threads are very lightweight (~1KB vs 1MB for platform threads)
- **Modern Java**: Demonstrates cutting-edge Java features

### Negative
- **JVM requirement**: Requires Java 21+ (we're using Java 25)
- **Learning curve**: Team needs to understand virtual thread concepts
- **Pinning issues**: Some blocking operations can pin carrier threads (rare)
- **Debugging tools**: Not all profilers fully support virtual threads yet

### Virtual Thread Benefits for Our Use Case

**Client Connections**:
- Each client gets dedicated virtual thread
- Blocking read/write operations are natural
- No callback hell or complex state machines

**Request Processing**:
- Simple sequential code flow
- Easy error handling with try-catch
- Natural resource management with try-with-resources

**Background Tasks**:
- Log flushing, segment rolling
- Metrics collection
- Health checks

### Avoided Complexity
- No need for thread pool sizing calculations
- No executor service configuration
- No callback chains or future composition
- No reactive stream complexity

### Pinning Considerations
Virtual threads can "pin" carrier threads when:
1. Inside synchronized blocks
2. Calling native methods
3. Using certain JVM-internal operations
4. Using blocking file I/O operations (java.io.*)

**Mitigation**:
- Use `ReentrantLock` instead of `synchronized` where needed
- Use `java.util.concurrent` locks instead of intrinsic locks
- **Use NIO (java.nio.*) instead of IO (java.io.*) packages for file operations**
- Use `FileChannel` and `MappedByteBuffer` for non-blocking file I/O
- Leverage `AsynchronousFileChannel` for async file operations where appropriate
- Monitor with JFR (Java Flight Recorder) events

### Performance Characteristics
- **Creation overhead**: ~1μs per virtual thread
- **Context switch**: ~10-100ns
- **Memory per thread**: ~1KB
- **Throughput**: Similar to async for I/O-bound workloads

### Testing Strategy
- Load testing with 10,000+ concurrent connections
- Monitor carrier thread pinning with JFR
- Compare throughput with traditional thread pool baseline

## References
- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [Virtual Thread Tuning Guide](https://inside.java/2023/04/24/virtual-threads-tuning/)
- [Common Pitfalls with Virtual Threads](https://inside.java/2021/11/30/on-parallelism-and-concurrency/)
