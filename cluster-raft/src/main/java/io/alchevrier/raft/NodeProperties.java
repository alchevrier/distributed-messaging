package io.alchevrier.raft;

import java.util.List;

public record NodeProperties(int nodeId, int port, List<RaftPeer> peers) {
}
