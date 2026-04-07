package io.alchevrier.raft.election;

public interface ElectionTimerService {
    void resetTimer(Runnable electionFn);
    void stop();
}
