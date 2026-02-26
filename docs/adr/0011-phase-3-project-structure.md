# ADR-0011: Phase 3 Project Structure

## Status
Accepted

## Context
The aim of Phase 3 is to lay out a foundation to reach our ultimate goal which will be Phase 4's end goal: replication. But to do replication we must first start with partitioning. Partitioning is a way to spread the load horizontally and make sure that:
- Data which are corresponding to the same class of messages are in the same partition
- Data which are corresponding to the same class of messages are received in the order they were produced
- Unordered data is spread nicely according to the current load on the broker

Partitioning is about:
- Consumers can focus on their target class of messages
- Consumers can also choose to let the broker assign them the messages it will read
- Producers can provide messages specifically for given partitions based on the class of messages
- Producers can also let the broker decide for them where the messages they produce go

All of this by design enables replication because broker can be replaced by a load balancer for example or equivalent and we will obtain exactly the same result as using a single broker. What replication brings on top of this is fault-tolerance by electing leaders and followers for each brokers in the swarm. 

Options considered:
1. **Already including replication** - This then introduces many concepts in one phase which expands the timeline and working in sprints is much more preferable since this project is done in my personal time
2. **Switching to replication first** - While this could have been possible, we would have to at the end up as one broker being the leader for a given topic and the rest just followers, in reality in Kafka most production load use partition number as one. This would have been a perfectly valid option but my preference would be to start with partitioning as it is something that I've not experienced at work.

## Decision
For Phase 3, there will be no new library in fact we will focus solely on updating existing ones:
- **log-storage-engine**: our LogManager now needs to be aware for which partition the message produced is meant to. Same for receiving a consumption request. We also need to add statistics about the current load the broker is under to figure out which partition will receive the message when a partition is not targeted. If a partition is targeted we simply use the targeted partition.
- **message**: need to add two new fields for ProduceRequest and ConsumeRequest, we need to add the concept of key which is then used by the broker to derive which partition is targeted. A partition is targeted by key by the given formula: hash(key) % numberOfPartition. This means breaking changes to our API but this is not harmful as this project is not used in production.
- **broker**: we need to update our API contracts to match the new format of messages
- **consumer/producer**: same as above

### Partitioning Strategy

To implement partitioning we will be using the existing components in the log-storage-engine and repurpose them:
- LogManager will be holding Log instances in its map but instead of representing the full log for the given topic it will be representing the partitions. Naming convention for each Log will be <nameOfTopic>-<partitionNumber>
- The number of partitions is defined by broker.number-of-partitions.
- LogManager when recovering will have to restore the partitions as they originally were. It will scan the log folder and look for folders named: <nameOfTopic>-<partitionNumber>
- LogManager when receiving a new request to read/produce data will have to:
-- If key is present compute the target partition
-- If key is not present use the current least-loaded partition
- The least-loaded partition is determined by comparing message counts across all partitions. The partition with the fewest messages receives the next message. On recovery, the broker restores partition message counts by scanning index entries per segment.

#### Message Formats

##### PRODUCE Request
```
If key exists: [length: 4][type: 03][4 bytes: topic length][N bytes: topic UTF-8][4 bytes: key length][N bytes: key UTF-8][4 bytes: data length][N bytes: data]
If key is not provided: [length: 4][type: 03][4 bytes: topic length][N bytes: topic UTF-8][4 bytes: key length == 0][4 bytes: data length][N bytes: data]
```

##### PRODUCE Response
```
Success: [length][type: 04][success: 1 byte][offset: 8 bytes][partition: 4 bytes]
```

#### CONSUME Request
```
[length: 4 bytes][message type: 01][4 bytes: topic length][N bytes: topic UTF-8][4 bytes: partition][8 bytes: offset (long)][4 bytes: batch size (int)]
```

### Architecture Diagram

The architecture remains unchanged

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

The main success criteria is about spreading the load automatically or to the targeted partition.
- Writing a suite of integration tests which produces and consumes message without targeting a partition and with a number of partition > 1. We need to check that messages are spread evenly across the different partitions. If we configure for example a number of partition of 3 and sending 99 messages it should spread as (33/33/33).
- Writing a suite of integration tests which produces and consumes messages with a target partition and with a number of partition > 1. We need to check that we get messages put in the target partition regardless of the current load on the server.

## Consequences

### Positive
- **No new libraries added**: We have all the elements needed in order to achieve this meaning no major refactoring
- **Direct implementation of hash**: Having to learn and understand modern non-cryptographic hash and implement them from scratch. We use MurmurHash3 for consistent key-to-partition mapping, matching Kafka's default partitioner.
- **Massive step towards fault-tolerance**: Once we introduce replication, we achieve one of the main goals of Kafka which is fault tolerance. 

### Negative
- **Not production ready**: Schema upgrade which is effectively breaking changes for our APIs but have no consequences due to not being used in production
- **Reality of fault-tolerance**: In production, replication delivers more immediate value than partitioning. We chose to implement partitioning first as a stepping stone, accepting that the standalone value of this phase is limited until Phase 4 adds replication on top
- **Changing partition count after data is written requires rebalancing**: out of scope for Phase 3.
- **Using Least-Loaded Partition**: Kafka uses Sticky partition but since our producer has no concept of batching and want to keep it simple for now, we will use the least-loaded strategy.