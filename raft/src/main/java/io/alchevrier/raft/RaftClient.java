package io.alchevrier.raft;

import io.alchevrier.message.raft.AppendEntriesRequest;
import io.alchevrier.message.raft.RequestVoteRequest;
import io.alchevrier.message.raft.RequestVoteResponse;

public interface RaftClient {
    RequestVoteResponse requestVote(RaftPeer peer, RequestVoteRequest request);
    void appendEntries(RaftPeer peer, AppendEntriesRequest request);
}
