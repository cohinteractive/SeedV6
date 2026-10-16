package com.ohinteractive.seedv6.training.selfplay;

import java.util.*;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import static org.junit.jupiter.api.Assertions.*;

class BootstrapPartitionTest {
    private static final NnueNetwork ACTOR = NnueNetwork.initialized(73);
    static SelfPlayBatch batch(int games) {
        return SelfPlayBatch.generate(ACTOR, SelfPlayConfig.defaults(games, 71), new SelfPlayControl(),
                index -> HeadlessGameTest.foolsMate().trajectory());
    }
    @Test void sixtyFourGamesGiveThirteenWholeValidationGamesAndNoSampleReuse() {
        var batch = batch(64); var split = BootstrapPartition.split(batch, 9);
        assertEquals(51, split.trainingGames().size()); assertEquals(13, split.heldOutGames().size());
        assertEquals(204, split.training().size()); assertEquals(52, split.heldOut().size());
        assertTrue(Collections.disjoint(split.trainingGames(), split.heldOutGames()));
        assertTrue(Collections.disjoint(split.training(), split.heldOut()));
        assertEquals(split, BootstrapPartition.split(batch, 9));
        assertNotEquals(split.heldOutGames(), BootstrapPartition.split(batch, 10).heldOutGames());
        for (int game : split.heldOutGames()) for (int ply = 0; ply < 4; ply++)
            assertTrue(split.heldOut().contains(batch.samples().get(game * 4 + ply)));
    }
    @Test void splittingRetainsExactSideToMoveTargetsAndDoesNotLabelCappedGames() {
        var batch = batch(4); var split = BootstrapPartition.split(batch, 17);
        assertEquals(2, split.trainingGames().size()); assertEquals(2, split.heldOutGames().size());
        // Fool's mate is a Black win: White positions -1, Black positions +1.
        for (var samples : List.of(split.training(), split.heldOut())) {
            for (int i = 0; i < samples.size(); i++) assertEquals(i % 2 == 0 ? -1 : 1, samples.get(i).target());
        }
        var capped = SelfPlayBatch.generate(ACTOR, SelfPlayConfig.defaults(4, 1), new SelfPlayControl(),
                index -> {
                    var game = new HeadlessGame(com.ohinteractive.seedv6.core.Board.startingPosition(), 1);
                    HeadlessGameTest.play(game, "e2e4"); return game.trajectory();
                });
        assertTrue(capped.samples().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> BootstrapPartition.split(capped, 1));
    }
    @Test void tooFewCompletedGamesFailsClearlyInsteadOfPublishingATinyDecision() {
        var failure = assertThrows(IllegalArgumentException.class, () -> BootstrapPartition.split(batch(3), 1));
        assertTrue(failure.getMessage().contains("at least four completed games"));
    }
}
