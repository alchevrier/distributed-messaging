# ADR-0003: Use Append-Only Log Storage

## Status
Accepted

## Context
We need to choose a storage model for message persistence. The storage layer is critical for:
- Message durability and reliability
- Read/write performance
- Storage efficiency
- Recovery and replication

Storage model options:
1. **Append-only log** (Kafka-style)
2. **Traditional database** (PostgreSQL, MySQL)
3. **In-memory only** (no persistence)
4. **Key-value store** (RocksDB, LevelDB)

## Decision
We will use an **append-only log storage model** with file-based segments.

Architecture:
- Messages are appended sequentially to log files
- Log is divided into segments for manageability
- Each segment has an offset index for fast lookups
- Immutable segments enable safe replication and caching
- Sequential writes optimize disk I/O
- **Use NIO (FileChannel, MappedByteBuffer) instead of traditional I/O to avoid virtual thread pinning**

## Consequences

### Positive
- **Write performance**: Sequential writes are extremely fast (500+ MB/s on modern SSDs)
- **Simplicity**: Simple data structure, easy to reason about
- **Replication-friendly**: Immutable segments are easy to replicate
- **Zero-copy**: Can use `FileChannel.transferTo()` for efficient network transfer
- **Order preservation**: Natural ordering by offset
- **Debugging**: Easy to inspect raw log files
- **Compaction**: Can implement log compaction for key-based retention
- **Virtual thread friendly**: NIO FileChannel avoids carrier thread pinning
- **Memory-mapped I/O**: Can use `MappedByteBuffer` for high-performance reads

### Negative
- **No random updates**: Cannot modify or delete individual messages easily
- **Storage growth**: Requires retention policies to manage disk usage
- **Index overhead**: Need separate index structures for fast lookups
- **Deletion complexity**: Compaction is more complex than simple DELETE statements

### Design Details

#### Log Structure
```
topic-name/
├── 00000000000000000000.log    # Segment file (messages)
├── 00000000000000000000.index  # Offset index
├── 00000000000000005000.log    # Next segment
└── 00000000000000005000.index
```

#### Record Format

Each record has 3 parts:

Length (4 bytes) - How many bytes is the actual data?
Offset (8 bytes) - What's the global offset number for this message?
Data (variable) - The actual message bytes

```
[length: 4 bytes][offset: 8 bytes][data: variable]
```

Example: 
```
Message 1: [4 bytes: length=13][8 bytes: offset=0][13 bytes: "Hello, World!"]
Message 2: [4 bytes: length=7][8 bytes: offset=1][7 bytes: "Message"]
Message 3: [4 bytes: length=9][8 bytes: offset=2][9 bytes: "Test data"]
```

#### Index Format

The index is a lookup table to find messages quickly without scanning the whole log:

```
[offset: 8 bytes][file_position: 8 bytes]
```

Example:
```
[offset: 0][file position: 0]       ← Message at offset 0 is at byte 0 in the .log
[offset: 1][file position: 25]      ← Message at offset 1 is at byte 25 in the .log
[offset: 2][file position: 44]      ← Message at offset 2 is at byte 44 in the .log
```

### Implementation Phases
1. **Phase 1**: Single segment with FileChannel, sequential reads
2. **Phase 2**: Multi-segment with rolling
3. **Phase 3**: Offset indexing for random access, consider MappedByteBuffer for index
4. **Phase 4**: Log compaction (future)

### File I/O Implementation
- Use `java.nio.channels.FileChannel` for all file operations
- Use `ByteBuffer` for read/write operations
- Consider `MappedByteBuffer` for read-heavy segments
- Use `FileChannel.transferTo()` for zero-copy network transfers
- Avoid `java.io.FileInputStream/FileOutputStream` to prevent virtual thread pinning

## References
- [The Log: What every software engineer should know](https://engineering.linkedin.com/distributed-systems/log-what-every-software-engineer-should-know-about-real-time-datas-unifying)
- [Kafka Storage Internals](https://kafka.apache.org/documentation/#design_filesystem)
