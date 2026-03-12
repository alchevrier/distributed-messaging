package io.alchevrier.message.raft;

public record AppendEntriesRequest(
        long term,
        int leaderId,
        long leaderCommitIndex,
        long prevLogIndex,
        long prevLogTerm,
        byte[][] entries
) { }
