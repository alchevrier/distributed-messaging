package io.alchevrier.raft.election;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledHeartbeatTimerService implements HeartbeatTimerService {

    private final ScheduledExecutorService scheduledExecutorService;
    private final long timeout;

    public ScheduledHeartbeatTimerService(long timeout) {
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        this.timeout = timeout;
    }

    @Override
    public void startTimer(Runnable heartbeatFn) {
        // For now we are passing RaftNode directly which is not thread safe later on we will have to pass to enqueue and execute it there
        scheduledExecutorService.scheduleAtFixedRate(heartbeatFn, timeout, timeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        this.scheduledExecutorService.close();
    }
}
