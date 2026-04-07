package io.alchevrier.raft.election;

public interface HeartbeatTimerService {
    void startTimer(Runnable heartbeatFn);
    void stop();
}
