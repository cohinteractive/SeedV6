package com.ohinteractive.seedv6.training.selfplay;

import java.util.*;

/** A seeded whole-game holdout: no positions from a held-out trajectory enter training. */
public record BootstrapPartition(List<TrajectorySampler.Sample> training, List<TrajectorySampler.Sample> heldOut,
                                 List<Integer> trainingGames, List<Integer> heldOutGames) {
    public BootstrapPartition {
        training = SampleStore.copyOf(training); heldOut = SampleStore.copyOf(heldOut);
        trainingGames = List.copyOf(trainingGames); heldOutGames = List.copyOf(heldOutGames);
        var trainIds = new HashSet<>(trainingGames); var heldIds = new HashSet<>(heldOutGames);
        if (training.size() < 2 || heldOut.size() < 2 || trainingGames.size() < 2 || heldOutGames.size() < 2
                || trainIds.size() != trainingGames.size() || heldIds.size() != heldOutGames.size()
                || !Collections.disjoint(trainIds, heldIds)
                || trainingGames.stream().anyMatch(i -> i < 0) || heldOutGames.stream().anyMatch(i -> i < 0))
            throw new IllegalArgumentException("Invalid or overlapping whole-game partition.");
    }
    public static BootstrapPartition split(SelfPlayBatch batch, long seed) {
        var eligible = new ArrayList<Integer>();
        for (int i = 0; i < batch.games().size(); i++) if (batch.games().get(i).sampledPositions() > 0) eligible.add(i);
        if (eligible.size() < 4)
            throw new IllegalArgumentException("BRN bootstrap needs at least four completed games with samples (two training and two held-out). Increase games or the game-ply bound.");
        var random = new SplittableRandom(seed);
        for (int i = eligible.size() - 1; i > 0; i--) Collections.swap(eligible, i, random.nextInt(i + 1));
        int validationGames = Math.max(2, (eligible.size() + 4) / 5);
        var reserved = new HashSet<>(eligible.subList(0, validationGames));
        var train = SampleStore.mutable();
        var held = SampleStore.mutable();
        var trainIds = new ArrayList<Integer>(); var heldIds = new ArrayList<Integer>();
        int offset = 0;
        for (int i = 0; i < batch.games().size(); i++) {
            int count = batch.games().get(i).sampledPositions();
            if (count == 0) continue;
            var target = reserved.contains(i) ? held : train;
            target.addAll(batch.samples().subList(offset, offset + count));
            (reserved.contains(i) ? heldIds : trainIds).add(batch.games().get(i).gameIndex());
            offset += count;
        }
        if (offset != batch.samples().size()) throw new IllegalArgumentException("Game/sample accounting mismatch.");
        return new BootstrapPartition(train, held, trainIds, heldIds);
    }
    /** New incremental attempts assign complete game IDs before any outcome is observed. */
    public static boolean heldOutGame(int game, int requestedGames, long seed) {
        if (requestedGames < 4) throw new IllegalArgumentException("Held-out generation requires at least four games");
        int held = Math.max(2, (int) ((requestedGames + 4L) / 5));
        return com.ohinteractive.seedv6.training.service.CorpusPermutation.index(requestedGames, seed, 0, game) < held;
    }
    public static BootstrapPartition splitAssigned(SelfPlayBatch batch, long seed) {
        var train = SampleStore.mutable(); var held = SampleStore.mutable();
        var trainIds = new ArrayList<Integer>(); var heldIds = new ArrayList<Integer>();
        int offset = 0;
        for (var game : batch.games()) {
            int count = game.sampledPositions();
            if (count == 0) continue;
            boolean reserved = heldOutGame(game.gameIndex(), batch.config().games(), seed);
            (reserved ? held : train).addAll(batch.samples().subList(offset, offset + count));
            (reserved ? heldIds : trainIds).add(game.gameIndex()); offset += count;
        }
        if (offset != batch.samples().size()) throw new IllegalArgumentException("Game/sample accounting mismatch");
        return new BootstrapPartition(train, held, trainIds, heldIds);
    }
}
