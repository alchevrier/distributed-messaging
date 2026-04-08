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

    def "append - appending to a leader with ACK=ALL should be be successful to all peers"() {
        when: "append request on the leader with ACK=ALL"
            def leader = findLeader()
            def response = leader.append(new AppendRequest("first", new byte[][] { "hello".getBytes(), "world".getBytes() }, AckMode.ALL))
        then: "everyone should have commited the entries"
            response.success()
            def nodes = [testHarness, firstFallbackTestHarness, secondFallbackTestHarness] - leader
            nodes.forEach {  response.peersAck().get(findNodeId(it)) }
    }

    def "append - appending to a leader with ACK=NONE should be be successful with no peers information"() {
        when: "append request on the leader with ACK=NONE"
            def leader = findLeader()
            def response = leader.append(new AppendRequest("first", new byte[][] { "hello".getBytes(), "world".getBytes() }, AckMode.NONE))
        then: "everyone should have commited the entries"
            response.success()
            response.peersAck() == null
    }

    ClusterRaftNodeTestHarness findLeader() {
        testHarness.start()
        firstFallbackTestHarness.start()
        secondFallbackTestHarness.start()

        def nodes = [testHarness, firstFallbackTestHarness, secondFallbackTestHarness]
        def leaderRef = [null]
        await().atMost(2000, TimeUnit.MILLISECONDS).until {
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
