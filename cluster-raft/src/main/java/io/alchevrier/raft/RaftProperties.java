package io.alchevrier.raft;

public record RaftProperties(LogProperties logProperties, SchedulerProperties schedulerProperties) {
}
