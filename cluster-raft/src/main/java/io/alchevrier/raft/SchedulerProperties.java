package io.alchevrier.raft;

public record SchedulerProperties(long triggerElectionLowerBound, long triggerElectionUpperBound, long heartbeatFixedRate) {
}
