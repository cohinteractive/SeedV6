package com.ohinteractive.seedv6.training.selfplay;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;

/** Sequential bounded generation. Retains samples and small summaries, never all game trajectories. */
public final class SelfPlayBatch {
    public record GameSummary(int gameIndex, long seed, GameTermination termination, int playedPlies,
                              int rawPositions, int sampledPositions, String failure) {}

    /** Raw positions include aborted games for diagnostics; only completed positions are sampled. */
    public record Statistics(int requestedGames, int completedGames, int abortedGames,
                             int whiteWins, int draws, int blackWins, int cappedGames,
                             long totalPlayedPlies, int minimumCompletedPlies, int maximumCompletedPlies,
                             double meanCompletedPlies, long rawTrajectoryPositions, int sampledPositions) {}

    /** Completed game boundary, with no trajectory/sample ownership exposed to the observer. */
    public record Progress(GameSummary lastGame, Statistics statistics) {}

    private final NnueNetwork generationNetwork;
    private final SelfPlayConfig config;
    private final List<GameSummary> games;
    private final List<TrajectorySampler.Sample> samples;
    private final Statistics statistics;
    private final boolean cancelled;

    private SelfPlayBatch(NnueNetwork network, SelfPlayConfig config, List<GameSummary> games,
                          List<TrajectorySampler.Sample> samples, boolean cancelled, Statistics statistics) {
        generationNetwork = network;
        this.config = config;
        this.games = List.copyOf(games);
        this.samples = List.copyOf(samples);
        this.cancelled = cancelled;
        this.statistics = statistics;
    }

    private static final class Totals {
        int completed = 0, aborted = 0, white = 0, draws = 0, black = 0, capped = 0;
        int minimum = Integer.MAX_VALUE, maximum = 0, sampled = 0;
        long plies = 0, completedPlies = 0, raw = 0;
        void add(GameSummary game) {
            plies += game.playedPlies();
            raw += game.rawPositions();
            sampled += game.sampledPositions();
            if (game.termination().completed()) {
                completed++;
                completedPlies += game.playedPlies();
                minimum = Math.min(minimum, game.playedPlies());
                maximum = Math.max(maximum, game.playedPlies());
                switch (game.termination().result().orElseThrow()) {
                    case WHITE_WIN -> white++;
                    case DRAW -> draws++;
                    case BLACK_WIN -> black++;
                }
            } else {
                aborted++;
                if (game.termination() == GameTermination.PLY_CAP) capped++;
            }
        }
        Statistics snapshot(int requested) {
            return new Statistics(requested, completed, aborted, white, draws, black, capped,
                    plies, completed == 0 ? 0 : minimum, maximum,
                    completed == 0 ? 0 : (double) completedPlies / completed, raw, sampled);
        }
    }

    public static SelfPlayBatch generate(NnueNetwork network, SelfPlayConfig config, SelfPlayControl control) {
        return generate(network, config, Board.startingPosition(), control);
    }

    /** Optional legal starting position for bounded endgame experiments; every game starts with fresh history. */
    public static SelfPlayBatch generate(NnueNetwork network, SelfPlayConfig config,
                                         long[] initialBoard, SelfPlayControl control) {
        return generate(network, config, initialBoard, control, progress -> {});
    }

    /** Observer runs synchronously between games; it must return promptly and must not mutate search state. */
    public static SelfPlayBatch generate(NnueNetwork network, SelfPlayConfig config,
                                         long[] initialBoard, SelfPlayControl control, Consumer<Progress> observer) {
        long[] root = initialBoard.clone();
        return generate(network, config, control,
                index -> SelfPlayRunner.play(network, config, index, root, control), observer);
    }

    @FunctionalInterface
    interface GamePlayer { GameTrajectory play(int gameIndex); }

    static SelfPlayBatch generate(NnueNetwork network, SelfPlayConfig config,
                                  SelfPlayControl control, GamePlayer player) {
        return generate(network, config, control, player, progress -> {});
    }

    static SelfPlayBatch generate(NnueNetwork network, SelfPlayConfig config,
                                  SelfPlayControl control, GamePlayer player, Consumer<Progress> observer) {
        Objects.requireNonNull(network, "network");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(control, "control");
        Objects.requireNonNull(observer, "observer");
        List<GameSummary> games = new ArrayList<>();
        List<TrajectorySampler.Sample> samples = new ArrayList<>();
        Totals totals = new Totals();
        for (int index = 0; index < config.games() && !control.cancelled(); index++) {
            GameTrajectory game = player.play(index);
            List<TrajectorySampler.Sample> selected = game.termination().completed()
                    ? TrajectorySampler.sample(game, config.maximumSamplesPerGame()) : List.of();
            samples.addAll(selected);
            GameSummary summary = new GameSummary(index, SelfPlayRunner.gameSeed(config.seed(), index), game.termination(),
                    game.playedPlies(), game.positions().size(), selected.size(), game.failure().orElse(null));
            games.add(summary);
            totals.add(summary);
            observer.accept(new Progress(summary, totals.snapshot(config.games())));
            // The next iteration releases the full trajectory, including any aborted positions.
        }
        return new SelfPlayBatch(network, config, games, samples, control.cancelled(), totals.snapshot(config.games()));
    }

    public NnueNetwork generationNetwork() { return generationNetwork; }
    public SelfPlayConfig config() { return config; }
    public List<GameSummary> games() { return games; }
    public List<TrajectorySampler.Sample> samples() { return samples; }
    public Statistics statistics() { return statistics; }
    public boolean cancelled() { return cancelled; }
    public int unstartedGames() { return config.games() - games.size(); }
}
