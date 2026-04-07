package io.alchevrier.raft.election;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ScheduledElectionTimerService implements ElectionTimerService {

    private final ScheduledExecutorService scheduledExecutorService;
    private final long timeoutLowerBound;
    private final long timeoutUppedBound;
    private ScheduledFuture<?> nextElectionHandler;

    public ScheduledElectionTimerService(long timeoutLowerBound, long timeoutUppedBound) {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        this.timeoutLowerBound = timeoutLowerBound;
        this.timeoutUppedBound = timeoutUppedBound;
    }

    public void resetTimer(Runnable electionFn) {
        if (nextElectionHandler != null) {
            nextElectionHandler.cancel(false);
        }
        var random = new Random();
        var timeout = random.nextLong(timeoutLowerBound, timeoutUppedBound);
        nextElectionHandler = scheduledExecutorService.schedule(electionFn, timeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        this.scheduledExecutorService.close();
    }
}
