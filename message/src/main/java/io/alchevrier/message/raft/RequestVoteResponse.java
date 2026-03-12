package io.alchevrier.message.raft;

public record RequestVoteResponse(boolean voteGranted, long currentTerm) {
}
