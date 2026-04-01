package io.alchevrier.message.raft;

/**
 * @param key idempotency key which guarantees the broker will not append twice the same request
 * @param entries to be appended to the raft log
 * @param ackMode either NONE, LEADER or ALL
 */
public record AppendRequest(String key, byte[][] entries, AckMode ackMode) {
}
