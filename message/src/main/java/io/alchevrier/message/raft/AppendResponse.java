package io.alchevrier.message.raft;

import java.util.Map;

public record AppendResponse(boolean success, Map<Integer, Boolean> peersAck) { }
