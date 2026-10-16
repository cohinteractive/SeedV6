package com.ohinteractive.seedv6.training.selfplay;

import com.ohinteractive.seedv6.search.common.SearchControl;
import com.ohinteractive.seedv6.search.common.SearchTermination;
import com.ohinteractive.seedv6.search.common.TimeSource;

/** One generation/training caller plus a cancelling thread. Sticky cancellation, no owned threads. */
public final class SelfPlayControl {
    private final com.ohinteractive.seedv6.training.telemetry.ActiveGameFeed presentation;
    public SelfPlayControl() { this(null); }
    public SelfPlayControl(com.ohinteractive.seedv6.training.telemetry.ActiveGameFeed presentation) { this.presentation = presentation; }
    public com.ohinteractive.seedv6.training.telemetry.ActiveGameFeed presentation() { return presentation; }
    private volatile boolean cancelled;
    private SearchControl activeSearch;

    // Trainer-thread owned continuation, inspected/persisted only after synchronous work returns.
    private SelfPlayBatch.Saved savedGames = SelfPlayBatch.Saved.EMPTY;
    private SelfPlayBatch.Saved resumeGames = SelfPlayBatch.Saved.EMPTY;
    private TrainingCursor resumeTraining = TrainingCursor.EMPTY;
    private long trainedSamples, trainedUpdates, initialStep = -1;
    private double initialLoss = Double.NaN, trainingLossSum;
    public record TrainingCursor(long samples, long updates, long initialStep, double initialLoss, double lossSum) {
        public static final TrainingCursor EMPTY = new TrainingCursor(0, 0, -1, Double.NaN, 0);
        public TrainingCursor {
            if (samples < 0 || updates < 0 || initialStep < -1 || !Double.isFinite(lossSum))
                throw new IllegalArgumentException("Invalid optimizer continuation.");
        }
    }
    public SelfPlayBatch.Saved savedGames() { return savedGames; }
    public void savedGames(SelfPlayBatch.Saved value) { savedGames = resumeGames = java.util.Objects.requireNonNull(value); }
    public SelfPlayBatch.Saved takeSavedGames() { var result = resumeGames; resumeGames = SelfPlayBatch.Saved.EMPTY; return result; }
    public void recordGames(SelfPlayBatch.Saved value) { savedGames = value; }
    public TrainingCursor takeTrainingStart() { var result = resumeTraining; resumeTraining = TrainingCursor.EMPTY; return result; }
    public TrainingCursor trainingCursor() { return new TrainingCursor(trainedSamples, trainedUpdates, initialStep, initialLoss, trainingLossSum); }
    public void trainingCursor(TrainingCursor value) {
        resumeTraining = value;
        recordTraining(value.samples(), value.updates(), value.initialStep(), value.initialLoss(), value.lossSum());
    }
    public void recordTraining(long samples, long updates, long firstStep, double firstLoss, double sum) {
        trainedSamples = samples; trainedUpdates = updates; initialStep = firstStep; initialLoss = firstLoss; trainingLossSum = sum;
    }

    public synchronized void cancel() {
        cancelled = true;
        if (presentation != null) presentation.close();
        if (activeSearch != null) activeSearch.request(SearchTermination.STOPPED);
    }

    public boolean cancelled() { return cancelled; }

    synchronized SearchControl beginSearch(SelfPlayConfig config) {
        if (activeSearch != null) throw new IllegalStateException("Control already owns a search.");
        activeSearch = SearchControl.controlled(config.nodesPerMove(), System.nanoTime(),
                config.millisPerMove() < 0 ? -1 : config.millisPerMove() * 1_000_000, TimeSource.SYSTEM);
        if (cancelled) activeSearch.request(SearchTermination.STOPPED);
        return activeSearch;
    }

    synchronized void endSearch() { activeSearch = null; }
}
