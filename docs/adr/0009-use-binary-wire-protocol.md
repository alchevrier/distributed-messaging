# ADR-0009: Use Binary Wire Protocol (Phase 2)

## Status
Accepted

## Context
We need to define a protocol for client-broker communication. The protocol impacts:
- Network bandwidth efficiency
- Serialization/deserialization performance
- Ease of debugging and tooling

Protocol options:
1. **Binary protocol** (custom, like Kafka)
2. **Text-based protocol** (JSON, HTTP/REST)
3. **gRPC** (Protobuf-based)
4. **MessagePack** or similar

## Decision
We will use a **custom binary protocol** for broker-client communication as the **long-term goal**.

**Phase 2 Implementation**: For Phase 2, we will use **custom binary protocol** and keep the existing **REST API with classic Spring Boot controllers** from Phase 1 and let them live together.

We have decided to go for a sequential protocol for Phase 2 and move on to pipelining for future phases as needed. 

Binary protocol structure:
```
Request:
[length: 4 bytes][message type: 1 bytes][payload: variable]

Response:
[length: 4 bytes][message type: 1 byte][success (0=failure, 1=success): 1 byte][payload: variable]
```

## Consequences

### Positive
- **Performance**: Minimal serialization overhead, compact representation
- **Bandwidth efficiency**: 40-60% smaller than JSON for typical messages
- **Learning value**: Understanding protocol design from scratch
- **Control**: Full control over format, optimization, evolution
- **Low latency**: Fast serialization/deserialization
- **Type safety**: Can enforce schema at protocol level
- **Classic Request/Response**: Simplicity and easier to maintain 

### Negative
- **Debugging complexity**: Harder to inspect than text protocols (need tools)
- **Implementation effort**: Must handle serialization manually
- **Cross-language**: Each client language needs serialization code
- **Evolution**: Must carefully manage protocol versioning
- **No existing tooling**: Can't use curl, Postman, etc. directly
- **Multi-threading performance**: Single-threaded or thread-per-connection model, not using Selector for async I/O

### Message Type
```
01: CONSUME_REQUEST
02: CONSUME_RESPONSE
03: PRODUCE_REQUEST
04: PRODUCE_RESPONSE
05: FLUSH_REQUEST
06: FLUSH_RESPONSE
```

### Message Formats

#### PRODUCE Request
```
[length: 4][type: 03][4 bytes: topic length][N bytes: topic UTF-8][4 bytes: data length][N bytes: data]
```

#### PRODUCE Response
```
Success path: [length][type: 04][success: 1][offset: 8]
Error path:   [length][type: 04][success: 0][4 bytes: error length][N bytes: error UTF-8]
```

#### CONSUME Request
```
[length: 4 bytes][message type: 01][4 bytes: topic length][N bytes: topic UTF-8][8 bytes: offset (long)][4 bytes: batch size (int)]
```

#### CONSUME Response
```
Success: [length][type: 02][success: 1][8 bytes: next offset][4 bytes: message count][for each: [4 bytes: msg length][8 bytes: offset][N bytes: data]]
Error:   [length][type: 02][success: 0][4 bytes: error length][N bytes: error UTF-8]
```

#### FLUSH Request
```
[length: 4 bytes][message type: 05]
```

#### FLUSH Response
```
Success: [length][type: 06][success: 1]
Error:   [length][type: 06][success: 0][4 bytes: error length][N bytes: error UTF-8]
```

## Phase 2: Client/Server TCP Socket
- Use raw TCP socket to send/receive messages
- Specific library that serialize/deserialize messages
- Rely solely on the content of the responses (02, 04, 06) messages to define SUCCESS or FAILURE
- Using ByteBuffer to perform serialisation and deserialisation of messages
- Keep existing REST endpoints and modify messages to suit the contract set above

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
- [Implementing a TCP client/server](https://jenkov.com/tutorials/java-nio/socketchannel.html#writing-to-a-socketchannel)