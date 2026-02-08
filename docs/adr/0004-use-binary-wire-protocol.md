# ADR-0004: Use Binary Wire Protocol

## Status
Accepted

## Context
We need to define a protocol for client-broker communication. The protocol impacts:
- Network bandwidth efficiency
- Serialization/deserialization performance
- Ease of debugging and tooling
- Cross-language client support
- Protocol evolution and versioning

Protocol options:
1. **Binary protocol** (custom, like Kafka)
2. **Text-based protocol** (JSON, HTTP/REST)
3. **gRPC** (Protobuf-based)
4. **MessagePack** or similar

## Decision
We will use a **custom binary protocol** for broker-client communication as the **long-term goal**.

**Phase 1 Implementation**: For Phase 1, we will use **REST API with classic Spring Boot controllers** and migrate to binary protocol in a future phase.

**Rationale for phased approach**:
- Current team lacks hands-on experience implementing custom binary protocols from scratch
- Would require significant learning overhead that would delay Phase 1 delivery
- REST allows focus on core distributed systems concepts (log storage, replication, partitioning)
- Can iterate faster during initial development and learning
- Binary protocol can be added as an optimization once fundamentals are proven
- Classic Spring Boot REST approach avoids OpenAPI code generation complexity and dependency bloat
- Direct use of our designed message DTOs without needing mappers or additional serialization layers

Binary protocol structure (future):
```
Request:
[length: 4 bytes][apiKey: 2 bytes][apiVersion: 2 bytes][correlationId: 4 bytes][payload: variable]

Response:
[length: 4 bytes][correlationId: 4 bytes][payload: variable]
```

## Consequences

### Positive
- **Performance**: Minimal serialization overhead, compact representation
- **Bandwidth efficiency**: 40-60% smaller than JSON for typical messages
- **Learning value**: Understanding protocol design from scratch
- **Control**: Full control over format, optimization, evolution
- **Low latency**: Fast serialization/deserialization
- **Type safety**: Can enforce schema at protocol level

### Negative
- **Debugging complexity**: Harder to inspect than text protocols (need tools)
- **Implementation effort**: Must handle serialization manually
- **Cross-language**: Each client language needs serialization code
- **Evolution**: Must carefully manage protocol versioning
- **No existing tooling**: Can't use curl, Postman, etc. directly

### API Keys (Message Types)
```
0x0000 - PRODUCE   (client -> broker)
0x0001 - FETCH     (client -> broker)
0x0002 - METADATA  (client -> broker)
0x0003 - OFFSET    (client -> broker)
```

### Message Formats

#### PRODUCE Request
```
[correlationId: 4][topic_len: 4][topic: var][data_len: 4][data: var]
```

#### PRODUCE Response
```
[correlationId: 4][offset: 8]
```

#### FETCH Request
```
[correlationId: 4][topic_len: 4][topic: var][offset: 8][batch_size: 4]
```

## Phase 1: REST API with Spring Boot
**Implementation details**:
- Use `@RestController` with standard Spring MVC annotations
- JSON for message serialization (Jackson)
- Standard HTTP verbs (POST for produce, GET for fetch/consume)
- Direct use of message DTOs (Topic, Message, ProduceRequest) without code generation
- REST API defines clear semantics that will map to binary protocol
- Implement binary protocol handlers alongside REST (dual-protocol support)
- Gradually migrate clients to binary
- Eventually deprecate REST endpoints
- Spring HTTP Interface clients can be replaced with custom binary protocol clieaster build times

**API Endpoints**:
```
POST /topics/{topic}/produce      # Produce messages
GET  /topics/{topic}/consume      # Consume messages (offset, batchSize params)
POST /admin/flush                 # Force flush to disk
```

**Migration path to binary**:
- Keep OpenAPI spec as documentation of API semantics
- Implement binary protocol handlers alongside REST (dual-protocol support)
- Gradually migrate clients to binary
- Eventually deprecate REST endpoints

### Future Considerations (Binary Protocol)
- Add CRC checksums for message integrity
- Implement compression (gzip, snappy, lz4)
- Support batch operations (multiple messages per request)
- Add schema registry for message schemas

### Tooling Strategy (Binary Protocol)ata_len: 4][data: var]
```

### Future Considerations
- Add CRC checksums for message integrity
- Implement compression (gzip, snappy, lz4)
- Support batch operations (multiple messages per request)
- Add schema registry for message schemas

### Tooling Strategy
- Build a CLI tool for debugging (hex dump, decode)
- Create Wireshark dissector plugin (advanced)
- Simple packet inspector for development

## References
- [Kafka Protocol Guide](https://kafka.apache.org/protocol)
- [Protocol Buffers vs Custom Binary](https://capnproto.org/news/2014-06-17-capnproto-flatbuffers-sbe.html)
