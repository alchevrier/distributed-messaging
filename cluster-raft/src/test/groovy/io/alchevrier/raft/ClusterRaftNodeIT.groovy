package io.alchevrier.raft

import io.alchevrier.message.raft.AckMode
import io.alchevrier.message.raft.AppendRequest
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

import static org.awaitility.Awaitility.await

class ClusterRaftNodeIT extends Specification {
    ClusterRaftNodeTestHarness testHarness
    ClusterRaftNodeTestHarness firstFallbackTestHarness
    ClusterRaftNodeTestHarness secondFallbackTestHarness

    def setup() {
        Files.deleteIfExists(Path.of("/tmp/mini-kafka/raft/node1/index/currentIndex.raftindex"))
        Files.deleteIfExists(Path.of("/tmp/mini-kafka/raft/node1/logger/currentLog.raftlog"))
        Files.deleteIfExists(Path.of("/tmp/mini-kafka/raft/node2/index/currentIndex.raftindex"))
        Files.deleteIfExists(Path.of("/tmp/mini-kafka/raft/node2/logger/currentLog.raftlog"))
        Files.deleteIfExists(Path.of("/tmp/mini-kafka/raft/node3/index/currentIndex.raftindex"))
        Files.deleteIfExists(Path.of("/tmp/mini-kafka/raft/node3/logger/currentLog.raftlog"))

        def node1PropertiesPath = Thread.currentThread().getContextClassLoader().getResource("node1.properties").file
        testHarness = new ClusterRaftNodeTestHarness(9182, new ClusterRaftNodeFactory().buildFromFile(node1PropertiesPath))
        def node2PropertiesPath = Thread.currentThread().getContextClassLoader().getResource("node2.properties").file
        firstFallbackTestHarness = new ClusterRaftNodeTestHarness(9185, new ClusterRaftNodeFactory().buildFromFile(node2PropertiesPath))
        def node3PropertiesPath = Thread.currentThread().getContextClassLoader().getResource("node3.properties").file
        secondFallbackTestHarness = new ClusterRaftNodeTestHarness(9187, new ClusterRaftNodeFactory().buildFromFile(node3PropertiesPath))
    }

    def cleanup() {
        testHarness.shutdown()
        firstFallbackTestHarness.shutdown()
        secondFallbackTestHarness.shutdown()
    }

    def "startElection - a leader should be elected and have two followers"() {
        when: "starting the different node participants"
            testHarness.start()
            firstFallbackTestHarness.start()
            secondFallbackTestHarness.start()
        then: "a leader should have been elected with two followers"
            await().atMost(2000, TimeUnit.MILLISECONDS).until {
                def nodes = [testHarness, firstFallbackTestHarness, secondFallbackTestHarness]
                nodes.count { it.raftNode.state == RaftState.LEADER } == 1 &&
                        nodes.count { it.raftNode.state == RaftState.FOLLOWER } == 2
            }
    }

    def "startElection - a leader should be re-elected if the current leader is down"() {
        when: "starting an election and shutting down the leader and wait re-election"
            def leader = findLeader()
            leader.shutdown()
            def newLeader = captureCurrentLeader()
        then: "everyone should have commited the entries"
            findNodeId(leader) != findNodeId(newLeader)
    }

    def "append - appending to a leader with ACK=ALL should be be successful to all peers"() {
        when: "append request on the leader with ACK=ALL"
            def leader = findLeader()
            def response = leader.append(new AppendRequest("first", new byte[][] { "hello".getBytes(), "world".getBytes() }, AckMode.ALL))
        then: "everyone should have commited the entries"
            response.success()
            def nodes = [testHarness, firstFallbackTestHarness, secondFallbackTestHarness] - leader
            nodes.forEach {  response.peersAck().get(findNodeId(it)) }
    }

    def "append - appending to a leader with ACK=ALL while one node is done should be be successful to majority of peers"() {
        when: "append request on the leader with ACK=ALL"
            def leader = findLeader()
            def downNode = ([testHarness, firstFallbackTestHarness, secondFallbackTestHarness] - leader)[0]
            downNode.shutdown()
            // by the time we shutdown the downNode, the leader election has been re-triggered again and the leader changed
            // basically the issue is that the downNode is probably ahead of the hierarchy and called first leading to the second FOLLOWER
            // thinking the leader is gone as we the first call to appendEntries to the downNode is timing-out
            leader = captureCurrentLeader()
            def response = leader.append(new AppendRequest("first", new byte[][] { "hello".getBytes(), "world".getBytes() }, AckMode.ALL))
        then: "everyone should have commited the entries"
            response.success()
            def remainingNode = [testHarness, firstFallbackTestHarness, secondFallbackTestHarness] - leader - downNode
            remainingNode.forEach {  response.peersAck().get(findNodeId(it)) }
            !response.peersAck().get(findNodeId(downNode))
    }

    def "append - appending to a leader with ACK=NONE should be be successful with no peers information"() {
        when: "append request on the leader with ACK=NONE"
            def leader = findLeader()
            def response = leader.append(new AppendRequest("first", new byte[][] { "hello".getBytes(), "world".getBytes() }, AckMode.NONE))
        then: "everyone should have commited the entries"
            response.success()
            response.peersAck() == null
    }

    def "append - appending to a leader with ACK=LEADER should be be successful with no peers information"() {
        when: "append request on the leader with ACK=LEADER"
            def leader = findLeader()
            def response = leader.append(new AppendRequest("first", new byte[][] { "hello".getBytes(), "world".getBytes() }, AckMode.LEADER))
        then: "everyone should have commited the entries"
            response.success()
            response.peersAck() == null
    }

    ClusterRaftNodeTestHarness findLeader() {
        testHarness.start()
        firstFallbackTestHarness.start()
        secondFallbackTestHarness.start()

        return captureCurrentLeader()
    }

    ClusterRaftNodeTestHarness captureCurrentLeader() {
        def nodes = [testHarness, firstFallbackTestHarness, secondFallbackTestHarness]
        def leaderRef = [null]
        await().atMost(3000, TimeUnit.MILLISECONDS).until {
            def found = nodes.find { it.raftNode.state == RaftState.LEADER }
            leaderRef[0] = found
            found != null
        }
        return leaderRef[0]
    }

    int findNodeId(ClusterRaftNodeTestHarness toBeTested) {
        return switch (toBeTested) {
            case testHarness -> 1
            case firstFallbackTestHarness -> 2
            case secondFallbackTestHarness -> 3
        }
    }
}
