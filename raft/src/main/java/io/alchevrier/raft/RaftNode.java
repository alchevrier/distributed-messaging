package io.alchevrier.raft;

import io.alchevrier.message.raft.AppendEntriesRequest;
import io.alchevrier.message.raft.AppendEntriesResponse;
import io.alchevrier.message.raft.RequestVoteRequest;
import io.alchevrier.message.raft.RequestVoteResponse;
import io.alchevrier.raft.election.ElectionTimerService;
import io.alchevrier.raft.election.HeartbeatTimerService;
import io.alchevrier.raft.log.RaftLog;
import io.alchevrier.raft.transport.RaftTcpClient;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

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
    private final HeartbeatTimerService heartbeatTimerService;
    private RaftClient raftClient;
    private final RaftLog log;
    private final ReentrantLock lock;

    public RaftNode(
            int nodeId,
            List<RaftPeer> peers,
            RaftLog log,
            RaftClient raftClient,
            ElectionTimerService electionTimerService,
            HeartbeatTimerService heartbeatTimerService
    ) {
        this.nodeId = nodeId;
        this.peers = peers;
        this.log = log;
        this.raftClient = raftClient;
        this.electionTimerService = electionTimerService;
        this.heartbeatTimerService = heartbeatTimerService;

        this.lock = new ReentrantLock();

        state = RaftState.FOLLOWER;
        currentTerm = 0;
        votedFor = null;
        commitIndex = 0;
        lastApplied = 0;
        voteReceived = 0;
        leaderState = null;
    }

    public void start() {
        this.electionTimerService.resetTimer(this::startElection);
    }

    public RequestVoteResponse handleRequestVote(RequestVoteRequest request) {
        this.lock.lock();
        try {
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
        } finally {
            this.lock.unlock();
        }
    }

    public void startElection() {
        this.lock.lock();
        try {
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
        } finally {
            this.lock.unlock();
        }
    }

    public void sendHeartbeats() {
        this.lock.lock();
        try {
            peers.forEach(this::sendAppendEntriesUnlocked);
        } finally {
            this.lock.unlock();
        }
    }

    public void sendAppendEntries(RaftPeer peer) {
        this.lock.lock();
        try {
            this.sendAppendEntriesUnlocked(peer);
        } finally {
            this.lock.unlock();
        }
    }

    private void sendAppendEntriesUnlocked(RaftPeer peer) {
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
        this.lock.lock();
        try {
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
        } finally {
            this.lock.unlock();
        }
    }

    public AppendEntriesResponse handleAppendEntries(AppendEntriesRequest request) {
        this.lock.lock();
        try {
            if (request.term() < currentTerm) {
                return new AppendEntriesResponse(false, currentTerm, null, null);
            }

            setAsFollower(request.term());

            if (log.getLastIndex() < request.prevLogIndex()) {
                return new AppendEntriesResponse(false, currentTerm, null, log.getLastIndex() + 1);
            }

            if (request.prevLogIndex() > 0 && log.getTermAt(request.prevLogIndex()) != request.prevLogTerm()) {
                var conflictTerm = log.getTermAt(request.prevLogIndex());
                var conflictIndex = log.scanFirst(1, log.getLastIndex(), (_, entry) -> entry.term() == conflictTerm);
                return new AppendEntriesResponse(false, currentTerm, conflictTerm, conflictIndex);
            }

            var indexTermFromDeletion = -1L;
            for (var i = 0; i < request.entries().length; i++) {
                var indexToSearch = request.prevLogIndex() + 1 + i;
                if (indexToSearch > log.getLastIndex()) break;
                var possibleEntry = log.get(indexToSearch);
                if (possibleEntry != null && possibleEntry.term() != request.term()) {
                    indexTermFromDeletion = indexToSearch;
                    break;
                }
            }

            if (indexTermFromDeletion != -1) {
                log.deleteFrom(indexTermFromDeletion);
            }

            var indexToInsertFrom = indexTermFromDeletion == -1
                    ? Math.max(0, log.getLastIndex() - request.prevLogIndex())  // skip already-matching entries
                    : indexTermFromDeletion - request.prevLogIndex() - 1;

            for (var i = indexToInsertFrom; i < request.entries().length; i++) {
                log.append(request.term(), request.entries()[(int) i]);
            }

            commitIndex = Math.min(request.leaderCommitIndex(), log.getLastIndex());

            return new AppendEntriesResponse(true, request.term(), null, null);
        } finally {
            this.lock.unlock();
        }
    }

    public void setRaftClient(RaftTcpClient raftClient) {
        this.lock.lock();
        try {
            this.raftClient = raftClient;
        } finally {
            this.lock.unlock();
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
        if (response == null) {
            return;
        }
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
        if (state == RaftState.LEADER) return;

        state = RaftState.LEADER;
        leaderState = new LeaderState();
        leaderState.initForPeers(peers, log.getLastIndex());
        heartbeatTimerService.startTimer(this::sendHeartbeats);
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

    RaftLog log() {
        return this.log;
    }
}
