package io.alchevrier.message.raft;

public record AppendEntriesResponse(
        boolean success,
        long term,
        Long conflictTerm,
        Long conflictIndex
) { }
