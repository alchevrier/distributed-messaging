package io.alchevrier.raft.election;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ScheduledElectionTimerService implements ElectionTimerService {

    private final ScheduledExecutorService scheduledExecutorService;
    private ScheduledFuture<?> nextElectionHandler;

    public ScheduledElectionTimerService() {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    }

    public void resetTimer(Runnable electionFn) {
        if (nextElectionHandler != null) {
            nextElectionHandler.cancel(true);
        }
        var random = new Random();
        var timeout = random.nextLong(150, 300);
        nextElectionHandler = scheduledExecutorService.schedule(electionFn, timeout, TimeUnit.MILLISECONDS);
    }
}
