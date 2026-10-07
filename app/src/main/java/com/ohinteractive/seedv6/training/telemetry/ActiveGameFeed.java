package com.ohinteractive.seedv6.training.telemetry;

import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.core.move.CapturedPieces;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.training.selfplay.HeadlessGame;
import static com.ohinteractive.seedv6.training.telemetry.ActiveGameSnapshot.*;

/**
 * Single trainer-thread writer, latest-value volatile publication, lock-free readers.
 * Only start/move/end boundaries call this; no observer, queue, GUI lock, I/O or evaluation.
 * close() may run on the cancelling thread and permanently hides late worker publications.
 * Ordinary headless callers leave the optional feed null and allocate no snapshots.
 */
public final class ActiveGameFeed {
    private volatile ActiveGameSnapshot latest;
    private volatile boolean closed;
    private Phase phase;
    private long generation, gameId, version;
    private Participant first, second;

    public void selfPlay(long generation, String latestTrainingId) {
        configure(Phase.SELF_PLAY, generation, new Participant(Role.LATEST_TRAINING, latestTrainingId),
                new Participant(Role.LATEST_TRAINING, latestTrainingId));
    }
    public void validation(long generation, String candidateId, String incumbentId) {
        configure(Phase.VALIDATION, generation, new Participant(Role.CANDIDATE, candidateId),
                new Participant(Role.BEST, incumbentId));
    }
    public void arena(long round, com.ohinteractive.seedv6.training.model.ModelLibrary.Binding first,
                      com.ohinteractive.seedv6.training.model.ModelLibrary.Binding second) {
        configure(Phase.ARENA, round, new Participant(Role.MODEL, first.checkpointId(), first),
                new Participant(Role.MODEL, second.checkpointId(), second));
    }
    public void arena(long round, Participant first, Participant second) {
        configure(Phase.ARENA, round, first, second);
    }
    private void configure(Phase phase, long generation, Participant first, Participant second) {
        clear(); this.phase = phase; this.generation = generation; this.first = first; this.second = second;
    }
    public ActiveGameSnapshot latest() {
        ActiveGameSnapshot value = latest;
        return closed ? null : value;
    }
    public void clear() { latest = null; }
    public void close() { closed = true; clear(); }

    /** Ordinals are one-based; gameInPair is zero for self-play, otherwise 1 or 2. */
    public void start(HeadlessGame game, int ordinal, int gameInPair) {
        clear();
        if (closed || phase == null || !game.active()) return;
        long now = System.nanoTime();
        boolean swap = phase != Phase.SELF_PLAY && gameInPair == 2;
        long[] board = game.boardSnapshot();
        boolean starting = game.playedPlies() == 0 && java.util.Arrays.equals(board, com.ohinteractive.seedv6.core.Board.startingPosition());
        latest = new ActiveGameSnapshot(phase, generation, ++gameId, ++version, ordinal, gameInPair,
                game.playedPlies(), swap ? second : first, swap ? first : second,
                board, 0, null, now, now, CapturedPieces.empty(starting));
    }

    /** Called after an actual legal move. Terminal positions clear instead of looking live. */
    public void moved(HeadlessGame game, long move, SearchResult result) {
        ActiveGameSnapshot before = latest();
        if (!game.active()) { clear(); return; }
        if (before == null) return;
        MoveEvaluation evaluation = result != null && result.completed() && result.hasMove() && result.bestMove() == move
                ? new MoveEvaluation(before.sideToMove() == Value.WHITE ? result.score() : -result.score(),
                        before.sideToMove(), result.depth(), before.positionKey()) : null;
        latest = new ActiveGameSnapshot(before.phase(), before.generation(), before.gameId(), ++version,
                before.gameOrdinal(), before.gameInPair(), game.playedPlies(), before.white(), before.black(),
                game.boardSnapshot(), move, evaluation, before.startedNanos(), System.nanoTime(), before.captured().afterMove(move));
    }
}
