# ADR-0010: Phase 2 Project Structure

## Status
Accepted

## Context
The aim of Phase 2 is to add alongside the existing REST API endpoints a binary protocol. We do not intend right now to remove the existing REST API endpoints, we would use them for proof of concept of interoperability.

Options considered:
1. **Fully implementing a binary protocol as Kafka does** - This repository is not meant to be used in production as of now. We will add more capabilities as we need them.
2. **Not implementing a binary protocol** - Defeats the purpose of learning how Kafka does things

## Decision
For Phase 2, we will implement all the required libraries in order to have a fully functional and interoperable with REST, TCP client/server broker. 

We chose the following modules:
- tcp-client -> tcp based client capable of interacting with the broker
- tcp-server -> tcp based server which the broker will use to serve requests
- message -> adding all code needed in order to serialize/deserialize binary such as interfaces MessageSerializer/MessageDeserializer and their byte implementation (ByteBufferMessageSerializer/ByteBufferMessageDeserializer)
- consumer -> adding a new TCP based implementation of the consumer (using the tcp-client) via the creation of a new class inside the sub-module (TCPConsumer).
- producer -> adding a new TCP based implementation of the producer (using the tcp-client) via the creation of a new class inside the sub-module (TCPProducer).

### Architecture Diagram

```
┌──────────┐                                    ┌──────────┐
│ Consumer │                                    │ Producer │
│  Client  │                                    │  Client  │
└────┬─────┘                                    └────┬─────┘
     │                                               │
     │ HTTP (REST API) + TCP                         │ HTTP (REST API) + TCP
     │                                               │ 
     │                                               │
     └───────────────────┬───────────────────────────┘
                         ↓
                 ┌───────────────────────────────────────────────┐
                 │              Broker Endpoint                  │
                 │   REST API (port 8080) + TCP (port 9180)      │
                 │              Spring Boot                      │
                 └───────────────────┬───────────────────────────┘
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

## Success criteria

The main success criteria is the interoperability between REST API and TCP-protocol. We will run the following integration test to validate this:
- Creation of a demo-tcp-client which is aimed to do either consume/produce or flush via command line
- Execute REST API method through the DemoApp (REST API) to consume after demo-tcp-client produced to check the content produced by the tcp-based client. 
- Execute REST API method through the DemoApp to produce and then have demo-tcp client to consume

This would validate:
- Message serialization/deserialization
- TCP-protocol is working on a live application
- Messages defined in the messages sub-modules are truly interoperable

## Consequences

### Positive
- **Use of ByteBuffer**: Carry over the knowledge we gained in Phase 1
- **Direct implementation of TCP client/server**: No magic involved, raw TCP used (no safety net)
- **Have already existing and working broker via REST API**: Allow for interoperability tests which then becomes a proof of concept of the TCP-based implementation

### Negative
- **Not production ready**: Schema upgrade would be massively difficult and would have to be through breaking changes

