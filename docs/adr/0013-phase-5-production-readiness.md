# ADR-0013: Phase 5 Production Readiness

## Status
Accepted

## Context
I created a JMH benchmarks sampling operation time made to call LogManager.append which is the heart of the log storage engine. We have found through it that our baseline is p99=6.5µs, p99.99=306µs, p100=40ms. 
JFR confirms 43 GC evacuation pauses in 100s, growing from 3ms to 655ms. Root causes are: 
- Short-lived allocation on append path (byte[] and AppendResponse for example) pressures Eden
- Long-lived allocation (TreeMap used to o(log n) find correct LogSegment to read from | HashMap used to o(1) find the correct file position for the given index) grow unboundedly in Old gen.
Production consequence: degrading tail latency over time, eventual OOM under sustained load.

## Decision

- **Replace heap-allocated index structures with off-heap MemorySegment**: GC does not scan memory allocated in a MemorySegment we are therefore freeing the GC from having to keep track of remembered sets of long-lived objects
- **Replace heap byte[] record construction with direct writes into MemorySegment**: Eliminating byte[] and AppendResponse entirely who were meant to be eventually reclaimed by the GC. Better not allocating those on the heap rather than producing garbage.
- **Use FileChannel.transferTo for zero-copy reads**: Making sure data is not copied into a user-space byte[] on reads — the kernel transfers directly from the file to the destination channel, eliminating heap allocation on the read path.

## Alternatives considered
- **Tune GC Flags**: masks the problem, doesn't fix it
- **Replace G1 with ZGC**: reduces pause duration but doesn't eliminate Old gen growth

## Consequences

### Positive
- **GC no longer scans index structures**: long-lived objects are now off-heap and handled by us 
- **Eden pressure reduced by elimination per-append allocation**: no more garbage objects produced both by us and the JVM (usually to fill the empty nearby memory to allow the GC to scan continuously an area of memory)

### Negative
- **MemorySegment requires explicit lifecycle management**: no GC safety net, we are the sole owners of this memory and have to manage every aspect of it. 
