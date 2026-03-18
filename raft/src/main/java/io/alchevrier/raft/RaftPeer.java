package io.alchevrier.raft;

public record RaftPeer(int nodeId, String host, int port) {
}
