package io.alchevrier.message.raft;

public enum AckMode {
    NONE, // Fire-and-forget - no guarantee on whether the leader will append successfully or not
    LEADER, // Waiting for the leader to have appended the entries
    ALL // Waiting for all RaftNodes in the cluster to have appended the entries
}
