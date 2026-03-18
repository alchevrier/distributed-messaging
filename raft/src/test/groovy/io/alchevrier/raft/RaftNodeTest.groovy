package io.alchevrier.raft

import io.alchevrier.message.raft.RequestVoteRequest
import io.alchevrier.message.raft.RequestVoteResponse
import spock.lang.Specification

class RaftNodeTest extends Specification {

    def raftClient = Mock(RaftClient)
    def electionTimer = Mock(ElectionTimerService)
    def log = new InMemoryRaftLog()

    def "startElection - single node in cluster"() {
        given: "a single node in cluster"
            def objectUnderTest = new RaftNode(1, Collections.emptyList(), log, raftClient, electionTimer)
        when: "starting an election"
            objectUnderTest.startElection()
        then: "should win immediately"
            objectUnderTest.getState() == RaftState.LEADER
            objectUnderTest.leaderState != null
    }

    def "startElection - 3 node cluster majority"() {
        given: "3 nodes in cluster"
            def firstPeer = new RaftPeer(2, "localhost", 9092)
            def secondPeer = new RaftPeer(3, "localhost", 9093)
            def objectUnderTest = new RaftNode(1, List.of(firstPeer, secondPeer), log, raftClient, electionTimer)

            def expectedRequestForVote = new RequestVoteRequest(1, 1, 0, 0)
        when: "starting an election"
            objectUnderTest.startElection()
        then: "should win if peers vote for node"
            1 * raftClient.requestVote(firstPeer, expectedRequestForVote) >> new RequestVoteResponse(true, 0)
            1 * raftClient.requestVote(secondPeer, expectedRequestForVote) >> new RequestVoteResponse(true, 0)
            objectUnderTest.getState() == RaftState.LEADER
            objectUnderTest.leaderState != null
            objectUnderTest.leaderState.getNextIndex(2) == 1
            objectUnderTest.leaderState.getMatchIndex(2) == 0
            objectUnderTest.leaderState.getNextIndex(3) == 1
            objectUnderTest.leaderState.getMatchIndex(3) == 0
    }

    def "handleRequestVote - empty log valid candidate should grants vote"() {
        given: "a cluster and a valid vote"
            def objectUnderTest = new RaftNode(1, Collections.emptyList(), log, raftClient, electionTimer)
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
            def objectUnderTest = new RaftNode(1, Collections.emptyList(), log, raftClient, electionTimer)
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
            def objectUnderTest = new RaftNode(1, Collections.emptyList(), log, raftClient, electionTimer)
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
            def objectUnderTest = new RaftNode(1, Collections.emptyList(), log, raftClient, electionTimer)
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
}
