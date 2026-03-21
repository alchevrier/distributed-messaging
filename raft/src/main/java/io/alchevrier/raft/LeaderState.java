package io.alchevrier.raft;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LeaderState {
    private final Map<Integer, Long> nextIndex;
    private final Map<Integer, Long> matchIndex;
    private final Map<Integer, Long> lastSentIndex;

    public LeaderState() {
        nextIndex = new HashMap<>();
        matchIndex = new HashMap<>();
        lastSentIndex = new HashMap<>();
    }

    public void initForPeers(List<RaftPeer> peers, long lastLogIndex) {
        for (var peer: peers) {
            nextIndex.put(peer.nodeId(), lastLogIndex + 1);
            matchIndex.put(peer.nodeId(), 0L);
            lastSentIndex.put(peer.nodeId(), 0L);
        }
    }

    public long getNextIndex(int peerId) {
        return nextIndex.get(peerId);
    }

    public void setNextIndex(int peerId, long index) {
        nextIndex.put(peerId, index);
    }

    public long getMatchIndex(int peerId) {
        return matchIndex.get(peerId);
    }

    public void setMatchIndex(int peerId, long index) {
        matchIndex.put(peerId, index);
    }

    public long getLastSentIndex(int peerId) {
        return lastSentIndex.get(peerId);
    }

    public void setLastSentIndex(int peerId, long index) {
        lastSentIndex.put(peerId, index);
    }
}
