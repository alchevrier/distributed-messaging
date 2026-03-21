package io.alchevrier.raft;

import io.alchevrier.message.raft.AppendEntriesRequest;
import io.alchevrier.message.raft.AppendEntriesResponse;
import io.alchevrier.message.raft.RequestVoteRequest;
import io.alchevrier.message.raft.RequestVoteResponse;

import java.util.List;

public class RaftNode {
    private int nodeId;
    private long currentTerm;
    private Integer votedFor;
    private RaftState state;
    private long commitIndex;
    private long lastApplied;
    private LeaderState leaderState;
    private List<RaftPeer> peers;

    private int voteReceived;

    private final ElectionTimerService electionTimerService;
    private final RaftClient raftClient;
    private final RaftLog log;

    public RaftNode(
            int nodeId,
            List<RaftPeer> peers,
            RaftLog log,
            RaftClient raftClient,
            ElectionTimerService electionTimerService
    ) {
        this.nodeId = nodeId;
        this.peers = peers;
        this.log = log;
        this.raftClient = raftClient;
        this.electionTimerService = electionTimerService;

        state = RaftState.FOLLOWER;
        currentTerm = 0;
        votedFor = null;
        commitIndex = 0;
        lastApplied = 0;
        voteReceived = 0;
        leaderState = null;
    }

    public RequestVoteResponse handleRequestVote(RequestVoteRequest request) {
        if (request.candidateTerm() > currentTerm) {
            setAsFollower(request.candidateTerm());
        }

        var voteGranted = request.candidateTerm() >= currentTerm &&
                (votedFor == null || votedFor == request.candidateId()) &&
                (request.lastLogTerm() > log.getLastTerm() ||
                        (request.lastLogTerm() == log.getLastTerm() && request.lastLogIndex() >= log.getLastIndex()));

        if (voteGranted) {
            votedFor = request.candidateId();
        }

        return new RequestVoteResponse(voteGranted, currentTerm);
    }

    public void startElection() {
        state = RaftState.CANDIDATE;
        currentTerm++;
        votedFor = nodeId;
        voteReceived = 1;

        electionTimerService.resetTimer(this::startElection);

        if (voteReceived > peers.size() / 2) {
            setAsLeader();
            return;
        }

        var requestForVote = new RequestVoteRequest(currentTerm, nodeId, log.getLastIndex(), log.getLastTerm());
        peers.forEach(it -> handleRequestVoteResponse(raftClient.requestVote(it, requestForVote), it.nodeId()));
    }

    public void sendHeartbeats() {
        peers.forEach(this::sendAppendEntries);
    }

    public void sendAppendEntries(RaftPeer peer) {
        var nextIndexForPeer = leaderState.getNextIndex(peer.nodeId());
        var prevLogIndexForPeer = nextIndexForPeer - 1;
        var prevLogTerm = log.getTermAt(prevLogIndexForPeer);

        var entryCount = Math.max(0, log.getLastIndex() - nextIndexForPeer + 1);
        var entries = new byte[Math.toIntExact(entryCount)][];
        for (var i = nextIndexForPeer; i <= log.getLastIndex(); i++) {
            entries[Math.toIntExact(i - nextIndexForPeer)] = log.get(i).data();
        }

        var appendEntries = new AppendEntriesRequest(currentTerm, nodeId, commitIndex, prevLogIndexForPeer, prevLogTerm, entries);
        leaderState.setLastSentIndex(peer.nodeId(), log.getLastIndex());
        raftClient.appendEntries(peer, appendEntries);
    }

    public void handleAppendEntriesResponse(AppendEntriesResponse response, int fromNodeId) {
        if (response.term() > currentTerm) {
            setAsFollower(response.term());
            return;
        }

        if (state != RaftState.LEADER) {
            return;
        }

        if (response.success()) {
            var sentCommitIndex = leaderState.getLastSentIndex(fromNodeId);
            leaderState.setMatchIndex(fromNodeId, sentCommitIndex);
            leaderState.setNextIndex(fromNodeId, sentCommitIndex + 1);
            tryAdvanceCommitIndex();
        } else {
            leaderState.setNextIndex(fromNodeId, findNextConflictIdx(log.getLastIndex(), response.conflictTerm(), response.conflictIndex()));
        }
    }

    private void tryAdvanceCommitIndex() {
        var majority = (peers.size() + 1) / 2;
        var n = log.scanFirst(log.getLastIndex(), commitIndex + 1, ((index, entry) -> {
            if (entry.term() != currentTerm) return false;
            var count = 1;
            for (var peer: peers) {
                if (leaderState.getMatchIndex(peer.nodeId()) >= index) count++;
            }
            return count > majority;
        }));
        if (n != -1) commitIndex = n;
    }

    private long findNextConflictIdx(long lastIndex, Long conflictTerm, Long conflictIndex) {
        if (conflictTerm == null) return conflictIndex;
        var lastEntryIndex = this.log.scanFirst(
                lastIndex,
                1,
                (_, entry) -> entry.term() == conflictTerm
        );

        return lastEntryIndex != -1 ? lastEntryIndex + 1 : conflictIndex;
    }

    private void handleRequestVoteResponse(RequestVoteResponse response, int fromNodeId) {
        if (response.currentTerm() > currentTerm) {
            setAsFollower(response.currentTerm());
        }
        if (state == RaftState.CANDIDATE && response.voteGranted()) {
            voteReceived++;
            if (voteReceived > peers.size() / 2) {
                setAsLeader();
            }
        }
    }

    private void setAsLeader() {
        state = RaftState.LEADER;
        leaderState = new LeaderState();
        leaderState.initForPeers(peers, log.getLastIndex());
    }

    private void setAsFollower(long term) {
        currentTerm = term;
        votedFor = null;
        leaderState = null;
        state = RaftState.FOLLOWER;
    }

    RaftState getState() {
        return state;
    }

    LeaderState getLeaderState() {
        return leaderState;
    }

    Integer getVotedFor() {
        return votedFor;
    }

    long getCurrentTerm() {
        return currentTerm;
    }

    long getCommitIndex() {
        return commitIndex;
    }
}
