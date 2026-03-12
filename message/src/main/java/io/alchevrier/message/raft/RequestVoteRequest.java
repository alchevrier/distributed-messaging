package io.alchevrier.message.raft;

public record RequestVoteRequest(
        long candidateTerm,
        int candidateId,
        long lastLogIndex,
        long lastLogTerm
) { }
