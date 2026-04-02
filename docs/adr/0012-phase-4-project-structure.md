# ADR-0012: Phase 4 Project Structure

## Status
Accepted

## Context
The aim of Phase 4 is to use the work done in Phase 3 and supercharge it with:
- Full Raft implementation using a unified log
- Same-process broker + RaftNode
- ack=0/1/all

All of this is meant to provide fault-tolerance which is the main reason why in production Kafka is used. Imagine being able to write possibly in parallel to multiple partition on multiple broker at the same time while keeping replicas in-sync and not losing temporality. 

Options considered:
1. **Using KRaft** - We then have to live with the legacy decisions made by the Kafka team when first releasing using Zookeeper. We have the liberty to not go this path as the project is not used in production and there is a huge learning opportunity to be made by implementing Raft the correct way. 
2. **Using Zookeeper** - Same as above except this time I get all the downside of the original implementation of Kafka. I would prefer use this opportunity to have full command of the current underlying protocol used which is Raft rather than focusing on learning the legacy option.

## Decision
For Phase 4, we will ramp by going from the least amount of change made to isolate the implementation of Raft to build later on the full integration:
- **tcp-server**: in order to handle multiple connection to the same server we need to modify the current implementation to be accepting now multiple connection. We will be doing this by spawing a virtual thread per connection.
- **messages**: Adding new messages relating to cluster, raft
- **raft**: NEW library which will our own implementation of the Raft protocol using a unified log as originally intended. 
- **cluster-raft**: NEW library meant to contextualise AppendEntries to do actions such as RaftNode joining/leaving, assign partition leadership and in-sync replication.
- **producer/consumer**: Now not simple clients but need to implement metadata discovery, leader routing and acks. Producer needs to have the partitioning logic instead of broker, and use the partitionNumber to find the correct leaderId and directly route the produceRequest to the correct node.

### TCP Server

In order to support both broker behaviour and RafNode behaviour we need to be able to have accept multiple incoming solutions. To do this when accepting in the loop instead of blocking the current thread we will be spawing a new virtual thread.

Here's our target:

```
TcpServer (client port 9182)          TcpServer (raft port 9282)
  └─ virtual thread: accept loop        └─ virtual thread: accept loop
       └─ virtual thread per conn             └─ virtual thread per conn
            └─ BrokerRequestHandler                └─ RaftRequestHandler
```

Here's how we want to do it:

```java
private final AtomicInteger connCounter = new AtomicInteger(0);


public void start() {
    Thread.ofVirtual().name("tcp-accept-" + port).start(() -> {
        try {
            var serverSockerChannel = ServerSocketChannel.open();
            serverSockerChannel.socket().bind(new InetSocketAddress(port));

            while (!gracefullyShutdown) {
                var socketChannel = serverSockerChannel.accept();
                Thread.ofVirtual().name("tcp-conn-" + connCounter.getAndIncrement()).start(() -> {
                   // handle connection     
                });
            }

            serverSockerChannel.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    });
}
```

What we are changing:
- We are now blocking the **serverSocketChannel** but wrapped around a virtual thread as nowadays virtual thread are lightweight and perfect for I/O operations and removing extra complexity in handling concurrency.
- We are now spawning a new thread after each accept call on the **serverSocketChannel** in order to cheaply handle multiple connection
- We already have ServerHandler which is already implemented for our broker we would just need to add a new TcpServer on another port and a new implementation of ServerHandler dedicated to RaftNode.

### Messages

In order to implement the Raft protocol and clustering we need to define many new messages as we need to communicate a lot over the wire. 

#### Raft Messages

We are introducing the following new requests:

```java
public class MessageType {
    // existing message types
    /**
     * 50: REQUEST_VOTE_REQUEST
     * 51: REQUEST_VOTE_RESPONSE
     * 52: APPEND_ENTRIES_REQUEST
     * 53: APPEND_ENTRIES_RESPONSE
     */
    public static final byte REQUEST_VOTE_REQUEST = 0x50;
    public static final byte REQUEST_VOTE_RESPONSE = 0x51;
    public static final byte APPEND_ENTRIES_REQUEST = 0x52;
    public static final byte APPEND_ENTRIES_RESPONSE = 0x53;
    public static final byte APPEND_REQUEST = 0x54;
    public static final byte APPEND_RESPONSE = 0x55;
}
```

##### RequestVoteRequest
```
[length: 4 bytes][type: 50][candidateTerm: 8 bytes][candidateId: 4 bytes][lastLogIndex: 8 bytes][lastLogTerm: 8 bytes]
```

We consider `candidateId` as the ID of the RaftNode requesting the vote. When starting the RaftNode it is assigned an id based on an arbitrary integer we assigned to it via configuration, brokerId and RaftNode id are meant to be the same as they are running on the same process. 

##### RequestVoteResponse

```
[length: 4 bytes][type: 51][voteGranted: 1 byte][currentTerm: 8 bytes]
```

##### AppendEntriesRequest

```
[length: 4 bytes][type: 52][term: 8 bytes][leaderId: 4 bytes][leaderCommitIndex: 8 bytes][prevLogIndex: 8 bytes][prevLogTerm: 8 bytes][numberOfEntries: 4 bytes](for each entries [data length: 4 bytes][data: N bytes])
```

We consider `leaderId` as the id of the current RaftNode that is the perceived leader. 

We consider `entries` as an opaque blob as Raft should not be aware of the possible types of data and should concern itself only to keep the log unified as expected. 

##### AppendEntriesResponse

```
[length: 4 bytes][type: 53][success: 1 byte][term: 8 bytes][conflictTerm: 8 bytes][conflictIndex: 8 bytes]
```

`conflictTerm` and `conflictIndex` are only meaningful on failure (`success: 0`). On success, both are set to `-1`. They allow the leader to jump directly to the right `nextIndex` for the follower rather than decrementing one at a time.

##### AppendRequest

```
[length: 4 bytes][messageType - 0x54][keyLength: 4 bytes][key: N Bytes][ackMode: 1 byte][entriesCount: 4 bytes]([entryLength: 4 bytes][entryData: N bytes])
```

##### AppendResponse

```
[length: 4 bytes][messageType - 0x55][success: 1 byte][peersCount: 4 bytes]([peerId: 4 byte][success: 1 byte])
```

#### Clustering Messages

We will now define messages which will be persisted in the log via AppendEntries which are relative to the metadata shared by the RafNodes such as:
- Join cluster (JoinCluster)
- Leaving cluster (LeaveCluster)
- Assign Leadership of partition (AssignLeadershipOfPartition)
- Update ISR (UpdateISR)

We will introduce an internal log type class:

```java
public class LogEntryType {
    /**
     * 01: JOIN_CLUSTER
     * 02: LEAVE_CLUSTER
     * 03: ASSIGN_LEADERSHIP_OF_PARTITION
     * 04: UPDATE_ISR
     */
    public static final byte JOIN_CLUSTER = 0x01;
    public static final byte LEAVE_CLUSTER = 0x02;
    public static final byte ASSIGN_LEADERSHIP_OF_PARTITION = 0x03;
    public static final byte UPDATE_ISR = 0x04;

}
```

Also we would have messages to be used to the client to check important information to be provided to the consumer/producer:
- Check for leadership of partition (CheckLeadershipOfPartition - Request/Response)
- Check for current index of partition (CheckIndexOfPartition - Request/Response)

We are intending of introducing the following new requests:

```java
public class MessageType {
    // existing message types
    /**
     * 70: CHECK_LEADERSHIP_OF_PARTITION_REQUEST
     * 71: CHECK_LEADERSHIP_OF_PARTITION_RESPONSE
     * 72: CHECK_INDEX_OF_PARTITION_REQUEST
     * 73: CHECK_INDEX_OF_PARTITION_RESPONSE
     */
    public static final byte CHECK_LEADERSHIP_OF_PARTITION_REQUEST = 0x70;
    public static final byte CHECK_LEADERSHIP_OF_PARTITION_RESPONSE = 0x71;
    public static final byte CHECK_INDEX_OF_PARTITION_REQUEST = 0x72;
    public static final byte CHECK_INDEX_OF_PARTITION_RESPONSE = 0x73;

}
```

##### JoinCluster

```
[length: 4 bytes][type: 01][nodeId: 4 bytes][hostLength: 4 bytes][hostName: N bytes][port: 4 bytes]
```

##### LeaveCluster

```
[length: 4 bytes][type: 02][nodeId: 4 bytes]
```

##### AssignLeadershipOfPartition

```
[length: 4 bytes][type: 03][nodeId: 4 bytes][partitionId: 4 bytes]
```

##### UpdateISR

```
[length: 4 bytes][type: 04][partitionId: 4 bytes][isrCount: 4 bytes](for each node: [nodeId: 4 bytes])
```

##### CheckLeadershipOfPartitionRequest

```
[length: 4 bytes][type: 70][partitionId: 4 bytes]
```

##### CheckLeadershipOfPartitionResponse

```
Has Leader: [length: 4 bytes][type: 71][partitionId: 4 bytes][leaderNodeId: 4 bytes]
```

leaderNodeId is set to -1 if no leader is currently elected. The producer must retry after a backoff.

##### CheckIndexOfPartitionRequest

```
[length: 4 bytes][type: 72][partitionId: 4 bytes]
```

##### CheckIndexOfPartitionResponse

```
[length: 4 bytes][type: 73][nodeId: 4 bytes][partitionId: 4 bytes][indexId: 8 bytes]
```

### Architecture Diagram

```
Producer/Consumer
      |
      | 1. CheckLeadershipOfPartition → any broker (9182/9184/9186)
      | 2. ProduceRequest → directly to leader broker port
      ↓
┌──────────────────────────────┐
│  Broker TCP   (port 9182)    │  ← produce/consume requests
│  RaftNode TCP (port 9282)    │  ← Raft LEADER: writes log, replicates via AppendEntries
└──────────────────────────────┘
          |                    |
          | AppendEntries      | AppendEntries
          | (port 9285)        | (port 9287)
          ↓                    ↓
┌──────────────────┐   ┌──────────────────┐
│  Broker  (9184)  │   │  Broker  (9186)  │
│  Raft    (9285)  │   │  Raft    (9287)  │
│  FOLLOWER        │   │  FOLLOWER        │
└──────────────────┘   └──────────────────┘
```

## Consequences

### Positive
- **Unified Log**: To get the state of the system we only need to recover the log as it was persisted. We don't need to recover an external system persisting the metadata and the log as it was designed in Kafka. 
- **Fault-tolerance**: We have replicated partition with checks to figure out at runtime who is the leader of which partition. This allows to rebalance in case of a crash of a RaftNode.
- **Broker simplicity traded for producer complexity**: The broker is kept dumb — it only checks partition leadership. The complexity of routing moves to the producer which now handles partitioning, metadata discovery and direct leader routing.

### Negative
- **High implementation complexity**: Raft is notoriously difficult to implement correctly. Split votes, log divergence, network partitions during election — each scenario requires careful handling. This implementation is not production-grade and should not be treated as such.
- **Log Grows Unboundly**: We don't have snapshotting nor log cleaning and therefore have to keep ALL log records. This could be fixed by a possible Phase 5.
- **Catching from very far in the log is costly**: Because the log may grow unboundly, a RaftNode lagging behind by a lot will take significant time to catch-up
- **Possibly a lot of additional log messages appended**: Join/Leave cluster may be spamed if the network is inconsistent, UpdateISR may be spamed as well. 