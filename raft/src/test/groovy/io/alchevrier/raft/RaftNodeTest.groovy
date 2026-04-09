package io.alchevrier.raft

import io.alchevrier.message.raft.AckMode
import io.alchevrier.message.raft.AppendEntriesRequest
import io.alchevrier.message.raft.AppendEntriesResponse
import io.alchevrier.message.raft.AppendRequest
import io.alchevrier.message.raft.RequestVoteRequest
import io.alchevrier.message.raft.RequestVoteResponse
import io.alchevrier.raft.election.ElectionTimerService
import io.alchevrier.raft.election.HeartbeatTimerService
import io.alchevrier.raft.log.InMemoryRaftLog
import spock.lang.Specification

import static org.awaitility.Awaitility.await

class RaftNodeTest extends Specification {

    def raftClient = Mock(RaftClient)
    def electionTimer = Mock(ElectionTimerService)
    def heartbeatTimer = Mock(HeartbeatTimerService)
    def log = new InMemoryRaftLog()

    def "startElection - single node in cluster"() {
        given: "a single node in cluster"
            def objectUnderTest = new RaftNode(1, Collections.emptyList(), log, raftClient, electionTimer, heartbeatTimer)
        when: "starting an election"
            objectUnderTest.startElection()
        then: "should win immediately"
            objectUnderTest.getState() == RaftState.LEADER
            objectUnderTest.leaderState != null
    }



    def "startElection - single node in cluster and setting election timer on start"() {
        given: "a single node in cluster"
            def objectUnderTest = new RaftNode(1, Collections.emptyList(), log, raftClient, electionTimer, heartbeatTimer)
        when: "starting an election"
            objectUnderTest.start()
        then: "should win immediately and set the election timer"
            1 * electionTimer.resetTimer(_)
    }

    def "startElection - 3 node cluster majority"() {
        given: "3 nodes in cluster"
            def firstPeer = new RaftPeer(2, "localhost", 9092)
            def secondPeer = new RaftPeer(3, "localhost", 9093)
            def objectUnderTest = new RaftNode(1, List.of(firstPeer, secondPeer), log, raftClient, electionTimer, heartbeatTimer)

            def expectedRequestForVote = new RequestVoteRequest(1, 1, 0, 0)
        when: "starting an election"
            objectUnderTest.startElection()
        then: "should win if peers vote for node"
            1 * raftClient.requestVote(firstPeer, expectedRequestForVote) >> new RequestVoteResponse(true, 0)
            1 * raftClient.requestVote(secondPeer, expectedRequestForVote) >> new RequestVoteResponse(true, 0)
            1 * heartbeatTimer.startTimer(_)
            objectUnderTest.getState() == RaftState.LEADER
            objectUnderTest.leaderState != null
            objectUnderTest.leaderState.getNextIndex(2) == 1
            objectUnderTest.leaderState.getMatchIndex(2) == 0
            objectUnderTest.leaderState.getNextIndex(3) == 1
            objectUnderTest.leaderState.getMatchIndex(3) == 0
    }

    def "append - given node is follower, when append called, then returns failed future immediately"(AckMode ackMode) {
        given: "a node that is a follower"
            def objectUnderTest = new RaftNode(1, Collections.emptyList(), log, raftClient, electionTimer, heartbeatTimer)
        when: "appending"
            def result = objectUnderTest.append(new AppendRequest("Hello", new byte[0][0], ackMode))
        then: "returns failed future immediately"
            result.isDone()
            def response = result.get()
            !response.success()
            response.peersAck() == null

        where:
            ackMode << [AckMode.ALL, AckMode.LEADER, AckMode.NONE]

    }

    def "append - given node is leader, when append with ack=NONE and valid key, then entry persisted in log and future has no value"() {
        given: "a node that is a leader"
            def objectUnderTest = new RaftNode(1, Collections.emptyList(), log, raftClient, electionTimer, heartbeatTimer)
            objectUnderTest.startElection()
        when: "appending with ack=NONE"
            def result = objectUnderTest.append(new AppendRequest("Hello", new byte[][] { "Hello".getBytes() }, AckMode.NONE))
        then: "returns future with no value"
            result.isDone()
            result.get() == null
            objectUnderTest.log().lastIndex == 1
            def logEntry = objectUnderTest.log().get(1)
            logEntry.term() == 1
            new String(logEntry.data()) == "Hello"
    }

    def "append - given node is leader, when append with ack=LEADER and valid key, then returns completed future with success"() {
        given: "a node that is a leader"
            def objectUnderTest = new RaftNode(1, Collections.emptyList(), log, raftClient, electionTimer, heartbeatTimer)
            objectUnderTest.startElection()
        when: "appending with ack=LEADER"
            def result = objectUnderTest.append(new AppendRequest("Hello", new byte[][] { "Hello".getBytes() }, AckMode.LEADER))
        then: "returns completed future with success"
            result.isDone()
            var response = result.get()
            response.success()
            response.peersAck() == null
            objectUnderTest.log().lastIndex == 1
            def logEntry = objectUnderTest.log().get(1)
            logEntry.term() == 1
            new String(logEntry.data()) == "Hello"
    }

    def "append - given node is leader, when same key submitted twice for ackMode=NONE or LEADER then log appended only once"(AckMode ackMode) {
        given: "a node that is a leader"
            def objectUnderTest = new RaftNode(1, Collections.emptyList(), log, raftClient, electionTimer, heartbeatTimer)
            objectUnderTest.startElection()
        when: "appending with ack=LEADER"
            objectUnderTest.append(new AppendRequest("Hello", new byte[][] { "Hello".getBytes() }, ackMode))
            def result = objectUnderTest.append(new AppendRequest("Hello", new byte[][] { "Hello".getBytes() }, ackMode))
        then: "returns completed future with success"
            result.isDone()
            var response = result.get()
            !response.success()
            response.peersAck() == null
            objectUnderTest.log().lastIndex == 1
            def logEntry = objectUnderTest.log().get(1)
            logEntry.term() == 1
            new String(logEntry.data()) == "Hello"

        where:
            ackMode << [AckMode.LEADER, AckMode.NONE]
    }

    def "append - given node is leader, when append with ack=ALL and valid key, then returns non completed future until tryAdvanceCommitIndex is called"() {
        given: "a node that is a leader"
            def firstPeer = new RaftPeer(2, "localhost", 9092)
        when: "appending with ack=LEADER"
            def objectUnderTest = new RaftNode(1, List.of(firstPeer), log, raftClient, electionTimer, heartbeatTimer)
            objectUnderTest.startElection()
            def result = objectUnderTest.append(new AppendRequest("Hello", new byte[][] { "Hello".getBytes() }, AckMode.ALL))
        then: "returns non-completed future"
            !result.isDone()
            // should be persisted in our log already as we append entries on the leader first
            objectUnderTest.log().lastIndex == 1
            def logEntry = objectUnderTest.log().get(1)
            logEntry.term() == 1
            new String(logEntry.data()) == "Hello"
        when: "handling a successful append entries response"
            objectUnderTest.sendAppendEntries(firstPeer) // setting lastIndex for the firstPeer
            objectUnderTest.handleAppendEntriesResponse(new AppendEntriesResponse(true, 1L, null, null), 2)
        then: "should have completed the future as peers has signaled it has appended our entries"
            result.isDone()
            def response = result.get()
            response.success()
            response.peersAck().size() == 1
            response.peersAck().get(2) == true
        when: "attempting to append the same entry again"
            def failedResult = objectUnderTest.append(new AppendRequest("Hello", new byte[][] { "Hello".getBytes() }, AckMode.ALL))
        then: "should return a failed completed future immediately"
            failedResult.isDone()
            def failedResponse = failedResult.get()
            !failedResponse.success()
            failedResponse.peersAck() == null
            objectUnderTest.log().lastIndex == 1
    }

    def "handleRequestVote - empty log valid candidate should grants vote"() {
        given: "a cluster and a valid vote"
            def objectUnderTest = new RaftNode(1, Collections.emptyList(), log, raftClient, electionTimer, heartbeatTimer)
            def validVote = new RequestVoteRequest(2, 3, 2, 2)
        when: "handling a request vote"
            def result = objectUnderTest.handleRequestVote(validVote)
        then: "should grant vote"
            result.voteGranted()
            result.currentTerm() == 2

            objectUnderTest.getCurrentTerm() == 2
            objectUnderTest.votedFor == 3
            objectUnderTest.getState() == RaftState.FOLLOWER
            objectUnderTest.getLeaderState() == null
    }

    def "handleRequestVote - two votes and one already valid the second one should be denied"() {
        given: "a cluster and two valid vote from different nodes"
            def objectUnderTest = new RaftNode(1, Collections.emptyList(), log, raftClient, electionTimer, heartbeatTimer)
            def validVote = new RequestVoteRequest(2, 3, 2, 2)
            def secondValidVote = new RequestVoteRequest(2, 4, 2, 2)
        when: "handling two request votes"
            objectUnderTest.handleRequestVote(validVote)
            def result = objectUnderTest.handleRequestVote(secondValidVote)
        then: "should deny vote"
            !result.voteGranted()
            result.currentTerm() == 2

            objectUnderTest.getCurrentTerm() == 2
            objectUnderTest.votedFor == 3
            objectUnderTest.getState() == RaftState.FOLLOWER
            objectUnderTest.getLeaderState() == null
    }

    def "handleRequestVote - log not empty and vote is given from a candidate term behind our log should be denied"() {
        given: "a cluster with a non-empty node and a vote behind of our current log"
            def objectUnderTest = new RaftNode(1, Collections.emptyList(), log, raftClient, electionTimer, heartbeatTimer)
            objectUnderTest.startElection()
            log.append(1, new byte[0])
            def behindVote = new RequestVoteRequest(0, 3, 0, 0)
        when: "handling a request vote"
            def result = objectUnderTest.handleRequestVote(behindVote)
        then: "should deny vote"
            !result.voteGranted()
            result.currentTerm() == 1

            objectUnderTest.getCurrentTerm() == 1
            objectUnderTest.votedFor == 1
            objectUnderTest.getState() == RaftState.LEADER
            objectUnderTest.getLeaderState() != null
    }

    def "handleRequestVote - log not empty and vote is given from a candidate term way higher than ours then should grant and revert to FOLLOWER"() {
        given: "a cluster with a non-empty node and a vote ahead of our current log"
            def objectUnderTest = new RaftNode(1, Collections.emptyList(), log, raftClient, electionTimer, heartbeatTimer)
            objectUnderTest.startElection()
            log.append(1, new byte[0])
            def behindVote = new RequestVoteRequest(6, 3, 6, 6)
        when: "handling a request vote"
            def result = objectUnderTest.handleRequestVote(behindVote)
        then: "should deny vote"
            result.voteGranted()
            result.currentTerm() == 6

            objectUnderTest.getCurrentTerm() == 6
            objectUnderTest.votedFor == 3
            objectUnderTest.getState() == RaftState.FOLLOWER
            objectUnderTest.getLeaderState() == null
    }

    def "sendAppendEntries - when a follower is behind"() {
        given: "3 nodes in cluster"
            def firstPeer = new RaftPeer(2, "localhost", 9092)
            def secondPeer = new RaftPeer(3, "localhost", 9093)
            def objectUnderTest = new RaftNode(1, List.of(firstPeer, secondPeer), log, raftClient, electionTimer, heartbeatTimer)


            def expectedRequestForVote = new RequestVoteRequest(1, 1, 0, 0)
            raftClient.requestVote(firstPeer, expectedRequestForVote) >> new RequestVoteResponse(true, 0)
            raftClient.requestVote(secondPeer, expectedRequestForVote) >> new RequestVoteResponse(true, 0)
            objectUnderTest.startElection()

            log.append(1, "Hello".getBytes())
            log.append(1, "World".getBytes())

            AppendEntriesRequest capturedRequest
        when: "sending append entries"
            objectUnderTest.sendAppendEntries(firstPeer)
        then: "should send append entries with missing entries"
            1 * raftClient.appendEntries(firstPeer, _) >> { peer, request -> capturedRequest = request }
            capturedRequest.term() == 1
            capturedRequest.leaderId() == 1
            capturedRequest.leaderCommitIndex() == 0
            capturedRequest.prevLogIndex() == 0
            capturedRequest.prevLogTerm() == 0
            capturedRequest.entries().length == 2
            new String(capturedRequest.entries()[0]) == "Hello"
            new String(capturedRequest.entries()[1]) == "World"
    }

    def "sendAppendEntries - when a follower is already caught up should end up as a heartbeat"() {
        given: "3 nodes in cluster"
            def firstPeer = new RaftPeer(2, "localhost", 9092)
            def secondPeer = new RaftPeer(3, "localhost", 9093)
            def objectUnderTest = new RaftNode(1, List.of(firstPeer, secondPeer), log, raftClient, electionTimer, heartbeatTimer)

            def expectedRequestForVote = new RequestVoteRequest(1, 1, 0, 0)
            raftClient.requestVote(firstPeer, expectedRequestForVote) >> new RequestVoteResponse(true, 0)
            raftClient.requestVote(secondPeer, expectedRequestForVote) >> new RequestVoteResponse(true, 0)
            objectUnderTest.startElection()

            AppendEntriesRequest capturedRequest
        when: "sending append entries"
            objectUnderTest.sendAppendEntries(firstPeer)
        then: "should send append entries with missing entries"
            1 * raftClient.appendEntries(firstPeer, _) >> { peer, request -> capturedRequest = request }
            capturedRequest.term() == 1
            capturedRequest.leaderId() == 1
            capturedRequest.leaderCommitIndex() == 0
            capturedRequest.prevLogIndex() == 0
            capturedRequest.prevLogTerm() == 0
            capturedRequest.entries().length == 0
    }

    def "sendHeartbeats - when all followers are already caught up should end up as a heartbeat"() {
        given: "3 nodes in cluster"
            def firstPeer = new RaftPeer(2, "localhost", 9092)
            def secondPeer = new RaftPeer(3, "localhost", 9093)
            def objectUnderTest = new RaftNode(1, List.of(firstPeer, secondPeer), log, raftClient, electionTimer, heartbeatTimer)

            def expectedRequestForVote = new RequestVoteRequest(1, 1, 0, 0)
            raftClient.requestVote(firstPeer, expectedRequestForVote) >> new RequestVoteResponse(true, 0)
            raftClient.requestVote(secondPeer, expectedRequestForVote) >> new RequestVoteResponse(true, 0)
            objectUnderTest.startElection()

            AppendEntriesRequest[] firstPeerCapturedRequest = [null]
            AppendEntriesRequest[] secondPeerCapturedRequest = [null]
            raftClient.appendEntries(firstPeer, _) >> { peer, request -> firstPeerCapturedRequest[0] = request }
            raftClient.appendEntries(secondPeer, _) >> { peer, request -> secondPeerCapturedRequest[0] = request }
        when: "sending append entries"
            objectUnderTest.sendHeartbeats()
        then: "should send append entries with missing entries"
            await().until { firstPeerCapturedRequest[0] != null && secondPeerCapturedRequest[0] != null }
            firstPeerCapturedRequest[0].term() == 1
            firstPeerCapturedRequest[0].leaderId() == 1
            firstPeerCapturedRequest[0].leaderCommitIndex() == 0
            firstPeerCapturedRequest[0].prevLogIndex() == 0
            firstPeerCapturedRequest[0].prevLogTerm() == 0
            firstPeerCapturedRequest[0].entries().length == 0

            secondPeerCapturedRequest[0].term() == 1
            secondPeerCapturedRequest[0].leaderId() == 1
            secondPeerCapturedRequest[0].leaderCommitIndex() == 0
            secondPeerCapturedRequest[0].prevLogIndex() == 0
            secondPeerCapturedRequest[0].prevLogTerm() == 0
            secondPeerCapturedRequest[0].entries().length == 0
    }

    def "handleAppendEntriesResponse - any node receiving a appendEntries response with a term higher than the node's current term is actually a FOLLOWER"() {
        given: "3 nodes in cluster, ours being the current LEADER"
            def firstPeer = new RaftPeer(2, "localhost", 9092)
            def secondPeer = new RaftPeer(3, "localhost", 9093)
            def objectUnderTest = new RaftNode(1, List.of(firstPeer, secondPeer), log, raftClient, electionTimer, heartbeatTimer)

            def expectedRequestForVote = new RequestVoteRequest(1, 1, 0, 0)
            raftClient.requestVote(firstPeer, expectedRequestForVote) >> new RequestVoteResponse(true, 0)
            raftClient.requestVote(secondPeer, expectedRequestForVote) >> new RequestVoteResponse(true, 0)
            objectUnderTest.startElection()
        when: "receiving a append entries response with higher term"
            objectUnderTest.handleAppendEntriesResponse(new AppendEntriesResponse(false, 10, 10, 100), 2)
        then: "should set itself as a follower"
            objectUnderTest.currentTerm == 10
            objectUnderTest.votedFor == null
            objectUnderTest.leaderState == null
            objectUnderTest.state == RaftState.FOLLOWER
    }

    def "handleAppendEntriesResponse - any node receiving a appendEntries response with a term lower while being a CANDIDATE"() {
        given: "3 nodes in cluster ours being a follower"
            def firstPeer = new RaftPeer(2, "localhost", 9092)
            def secondPeer = new RaftPeer(3, "localhost", 9093)
            def objectUnderTest = new RaftNode(1, List.of(firstPeer, secondPeer), log, raftClient, electionTimer, heartbeatTimer)

            def expectedRequestForVote = new RequestVoteRequest(1, 1, 0, 0)
            raftClient.requestVote(firstPeer, expectedRequestForVote) >> new RequestVoteResponse(false, 0)
            raftClient.requestVote(secondPeer, expectedRequestForVote) >> new RequestVoteResponse(false, 0)
            objectUnderTest.startElection()
        when: "receiving a append entries response with lower term"
            objectUnderTest.handleAppendEntriesResponse(new AppendEntriesResponse(false, 0, null, null), 2)
        then: "should stay a candidate"
            objectUnderTest.currentTerm == 1
            objectUnderTest.votedFor == 1
            objectUnderTest.leaderState == null
            objectUnderTest.state == RaftState.CANDIDATE
    }

    def "handleAppendEntriesResponse - any node receiving a appendEntries response with a term lower than the node's current term success for all nodes"() {
        given: "4 nodes in cluster, ours being the current LEADER"
            def firstPeer = new RaftPeer(2, "localhost", 9092)
            def secondPeer = new RaftPeer(3, "localhost", 9093)
            def thirdPeer = new RaftPeer(4, "localhost", 9093)
            def objectUnderTest = new RaftNode(1, List.of(firstPeer, secondPeer, thirdPeer), log, raftClient, electionTimer, heartbeatTimer)

            raftClient.requestVote(firstPeer, _) >> new RequestVoteResponse(true, 1)
            raftClient.requestVote(secondPeer, _) >> new RequestVoteResponse(true, 1)
            raftClient.requestVote(thirdPeer, _) >> new RequestVoteResponse(true, 1)
            // appending log entries before our term
            objectUnderTest.startElection()
            log.append(1, new byte[0])
            log.append(1, new byte[0])
            log.append(1, new byte[0])
            objectUnderTest.startElection()
            log.append(2, new byte[0])
            log.append(2, new byte[0])
            log.append(2, new byte[0])
            objectUnderTest.sendAppendEntries(firstPeer)
            objectUnderTest.sendAppendEntries(secondPeer)
        when: "receiving a append entries response for the first peer"
            objectUnderTest.handleAppendEntriesResponse(new AppendEntriesResponse(true, 1, null, null), 2)
        then: "should move the corresponding matchIndex and nextIndex but not the commitIndex"
            objectUnderTest.leaderState.getMatchIndex(2) == 6
            objectUnderTest.leaderState.getNextIndex(2) == 7
            objectUnderTest.commitIndex == 0
        when: "receiving a append entries response for the second peer"
            objectUnderTest.handleAppendEntriesResponse(new AppendEntriesResponse(true, 1, null, null),3)
        then: "should move the corresponding matchIndex and nextIndex and the commitIndex as it was receiving by a majority"
            objectUnderTest.leaderState.getMatchIndex(3) == 6
            objectUnderTest.leaderState.getNextIndex(3) == 7
            objectUnderTest.commitIndex == 6
    }

    def "handleAppendEntriesResponse - LEADER node receiving failed append entries from follower with null conflictTerm"() {
        given: "3 nodes in cluster ours being a follower"
            def firstPeer = new RaftPeer(2, "localhost", 9092)
            def secondPeer = new RaftPeer(3, "localhost", 9093)
            def objectUnderTest = new RaftNode(1, List.of(firstPeer, secondPeer), log, raftClient, electionTimer, heartbeatTimer)

            def expectedRequestForVote = new RequestVoteRequest(1, 1, 0, 0)
            raftClient.requestVote(firstPeer, expectedRequestForVote) >> new RequestVoteResponse(true, 0)
            raftClient.requestVote(secondPeer, expectedRequestForVote) >> new RequestVoteResponse(true, 0)
            objectUnderTest.startElection()
        when: "receiving a append entries response with lower term"
            objectUnderTest.handleAppendEntriesResponse(new AppendEntriesResponse(false, 0, null, 1), 2)
        then: "should only have updated the nextIndex with the conflictIndex"
            objectUnderTest.leaderState.getNextIndex(2) == 1
            objectUnderTest.leaderState.getMatchIndex(2) == 0
    }

    def "handleAppendEntriesResponse - LEADER node receiving failed append entries from follower with sentinel conflictTerm"() {
        given: "3 nodes in cluster ours being a follower"
            def firstPeer = new RaftPeer(2, "localhost", 9092)
            def secondPeer = new RaftPeer(3, "localhost", 9093)
            def objectUnderTest = new RaftNode(1, List.of(firstPeer, secondPeer), log, raftClient, electionTimer, heartbeatTimer)

            def expectedRequestForVote = new RequestVoteRequest(1, 1, 0, 0)
            raftClient.requestVote(firstPeer, expectedRequestForVote) >> new RequestVoteResponse(true, 0)
            raftClient.requestVote(secondPeer, expectedRequestForVote) >> new RequestVoteResponse(true, 0)
            objectUnderTest.startElection()
        when: "receiving a append entries response with lower term"
            objectUnderTest.handleAppendEntriesResponse(new AppendEntriesResponse(false, 0, -1, -1), 2)
        then: "should only have updated the nextIndex with the conflictIndex"
            objectUnderTest.leaderState.getNextIndex(2) == 1
            objectUnderTest.leaderState.getMatchIndex(2) == 0
    }

    def "handleAppendEntriesResponse - LEADER node receiving failed append entries from follower with conflictTerm "() {
        given: "3 nodes in cluster ours being a follower"
            def firstPeer = new RaftPeer(2, "localhost", 9092)
            def secondPeer = new RaftPeer(3, "localhost", 9093)
            def objectUnderTest = new RaftNode(1, List.of(firstPeer, secondPeer), log, raftClient, electionTimer, heartbeatTimer)

            raftClient.requestVote(firstPeer, _) >> new RequestVoteResponse(true, 0)
            raftClient.requestVote(secondPeer, _) >> new RequestVoteResponse(true, 0)
            objectUnderTest.startElection()
            log.append(1, new byte[0])
            log.append(1, new byte[0])
            log.append(1, new byte[0])
            objectUnderTest.startElection()
            log.append(2, new byte[0])
            log.append(2, new byte[0])
            log.append(2, new byte[0])
        when: "receiving a append entries response with lower term"
            objectUnderTest.handleAppendEntriesResponse(new AppendEntriesResponse(false, 2, 1, 1), 2)
        then: "should only have updated the nextIndex with the index + 1 of the last entry matching the conflict term"
            objectUnderTest.leaderState.getNextIndex(2) == 4
            objectUnderTest.leaderState.getMatchIndex(2) == 0
    }

    def "handleAppendEntriesResponse - LEADER node receiving failed append entries from follower with conflictTerm not existing"() {
        given: "3 nodes in cluster ours being a follower"
            def firstPeer = new RaftPeer(2, "localhost", 9092)
            def secondPeer = new RaftPeer(3, "localhost", 9093)
            def objectUnderTest = new RaftNode(1, List.of(firstPeer, secondPeer), log, raftClient, electionTimer, heartbeatTimer)

            raftClient.requestVote(firstPeer, _) >> new RequestVoteResponse(true, 0)
            raftClient.requestVote(secondPeer, _) >> new RequestVoteResponse(true, 0)
            objectUnderTest.startElection()
            log.append(1, new byte[0])
            log.append(1, new byte[0])
            log.append(1, new byte[0])
            objectUnderTest.startElection()
            log.append(2, new byte[0])
            log.append(2, new byte[0])
            log.append(2, new byte[0])
        when: "receiving a append entries response with lower term"
            objectUnderTest.handleAppendEntriesResponse(new AppendEntriesResponse(false, 2, 10, 4), 2)
        then: "should only have updated the nextIndex with the conflictIndex if no last entries for the conflictTerm"
            objectUnderTest.leaderState.getNextIndex(2) == 4
            objectUnderTest.leaderState.getMatchIndex(2) == 0
    }

    def "handleAppendEntries - receiving append entries request whose term is inferior to the node's current term"() {
        given: "3 nodes in cluster ours being a follower"
            def firstPeer = new RaftPeer(2, "localhost", 9092)
            def secondPeer = new RaftPeer(3, "localhost", 9093)
            def objectUnderTest = new RaftNode(1, List.of(firstPeer, secondPeer), log, raftClient, electionTimer, heartbeatTimer)

            raftClient.requestVote(firstPeer, _) >> new RequestVoteResponse(true, 0)
            raftClient.requestVote(secondPeer, _) >> new RequestVoteResponse(true, 0)
            objectUnderTest.startElection()
            objectUnderTest.startElection()
        when: "receiving a append entries request with lower term"
            def result = objectUnderTest.handleAppendEntries(new AppendEntriesRequest(1L, 2, 3L, 1L, 1L, new byte[][] { new byte[0] }))
        then: "should send a non-successful response with current term"
            !result.success()
            result.term() == 2L
            result.conflictTerm() == null
            result.conflictIndex() == null
    }

    def "handleAppendEntries - receiving append entries request whose prevLogIndex is superior to the node's last index"() {
        given: "3 nodes in cluster ours being a follower"
            def firstPeer = new RaftPeer(2, "localhost", 9092)
            def secondPeer = new RaftPeer(3, "localhost", 9093)
            def objectUnderTest = new RaftNode(1, List.of(firstPeer, secondPeer), log, raftClient, electionTimer, heartbeatTimer)

            raftClient.requestVote(firstPeer, _) >> new RequestVoteResponse(true, 0)
            raftClient.requestVote(secondPeer, _) >> new RequestVoteResponse(true, 0)
            objectUnderTest.startElection()
            log.append(1, new byte[0])
            objectUnderTest.startElection()
        when: "receiving a append entries request with higher prevLogIndex"
            def result = objectUnderTest.handleAppendEntries(new AppendEntriesRequest(2L, 2, 3L, 2L, 1L, new byte[][] { new byte[0] }))
        then: "should send a non-successful response with current term"
            !result.success()
            result.term() == 2L
            result.conflictTerm() == null
            result.conflictIndex() == 2
    }

    def "handleAppendEntries - receiving append entries request whose prevLogIndex is zero"() {
        given: "3 nodes in cluster ours being a follower"
            def firstPeer = new RaftPeer(2, "localhost", 9092)
            def secondPeer = new RaftPeer(3, "localhost", 9093)
            def objectUnderTest = new RaftNode(1, List.of(firstPeer, secondPeer), log, raftClient, electionTimer, heartbeatTimer)

            raftClient.requestVote(firstPeer, _) >> new RequestVoteResponse(true, 0)
            raftClient.requestVote(secondPeer, _) >> new RequestVoteResponse(true, 0)
            objectUnderTest.startElection()
            log.append(1, new byte[0])
            log.append(1, new byte[0])
            log.append(1, new byte[0])
            objectUnderTest.startElection()
        when: "receiving a append entries request with prev log index at zero"
            def result = objectUnderTest.handleAppendEntries(new AppendEntriesRequest(2L, 2, 2L, 0L, 1L, new byte[][] { new byte[0] }))
        then: "should send a non-successful response with current term"
            result.success()
            result.term() == 2L
            result.conflictTerm() == null
            result.conflictIndex() == null

            objectUnderTest.currentTerm == 2L
            objectUnderTest.votedFor == null
            objectUnderTest.leaderState == null
            objectUnderTest.state == RaftState.FOLLOWER
            objectUnderTest.commitIndex == 1L
            objectUnderTest.log().getLastIndex() == 1L
            objectUnderTest.log().getLastTerm() == 2L
            objectUnderTest.log().get(1).term() == 2L
    }

    def "handleAppendEntries - receiving append entries request whose prevLogTerm is not matching"() {
        given: "3 nodes in cluster ours being a follower"
            def firstPeer = new RaftPeer(2, "localhost", 9092)
            def secondPeer = new RaftPeer(3, "localhost", 9093)
            def objectUnderTest = new RaftNode(1, List.of(firstPeer, secondPeer), log, raftClient, electionTimer, heartbeatTimer)

            raftClient.requestVote(firstPeer, _) >> new RequestVoteResponse(true, 0)
            raftClient.requestVote(secondPeer, _) >> new RequestVoteResponse(true, 0)
            objectUnderTest.startElection()
            log.append(1, new byte[0])
            log.append(1, new byte[0])
            log.append(1, new byte[0])
            objectUnderTest.startElection()
            log.append(2, new byte[0]) // Our node is having one entry for a term which doesn't match the leader's
        when: "receiving a append entries request whose prevLogTerm is not matching"
            def result = objectUnderTest.handleAppendEntries(new AppendEntriesRequest(2L, 2, 2L, 4L, 1L, new byte[][] { new byte[0] }))
        then: "should send a non-successful response with current term"
            !result.success()
            result.term() == 2L
            result.conflictTerm() == 2L
            result.conflictIndex() == 4L
    }

    def "handleAppendEntries - receiving append entries request which just appends to our log"() {
        given: "3 nodes in cluster ours being a follower"
            def firstPeer = new RaftPeer(2, "localhost", 9092)
            def secondPeer = new RaftPeer(3, "localhost", 9093)
            def objectUnderTest = new RaftNode(1, List.of(firstPeer, secondPeer), log, raftClient, electionTimer, heartbeatTimer)

            raftClient.requestVote(firstPeer, _) >> new RequestVoteResponse(true, 0)
            raftClient.requestVote(secondPeer, _) >> new RequestVoteResponse(true, 0)
            objectUnderTest.startElection()
            log.append(1, new byte[0])
            log.append(1, new byte[0])
            log.append(1, new byte[0])
            objectUnderTest.startElection()
        when: "receiving a append entries request which just appends to our log"
            def result = objectUnderTest.handleAppendEntries(new AppendEntriesRequest(2L, 2, 4L, 3L, 1L, new byte[][] { new byte[0] }))
        then: "should send a non-successful response with current term"
            result.success()
            result.term() == 2L
            result.conflictTerm() == null
            result.conflictIndex() == null

            objectUnderTest.currentTerm == 2L
            objectUnderTest.votedFor == null
            objectUnderTest.leaderState == null
            objectUnderTest.state == RaftState.FOLLOWER
            objectUnderTest.commitIndex == 4L
            objectUnderTest.log().getLastIndex() == 4L
            objectUnderTest.log().getLastTerm() == 2L
            objectUnderTest.log().get(4).term() == 2L
    }
}
