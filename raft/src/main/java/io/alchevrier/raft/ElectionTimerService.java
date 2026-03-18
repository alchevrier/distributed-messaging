package io.alchevrier.raft;

public interface ElectionTimerService {
    void resetTimer(Runnable electionFn);
}
