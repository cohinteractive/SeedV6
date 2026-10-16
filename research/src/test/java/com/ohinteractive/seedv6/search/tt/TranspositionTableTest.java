package com.ohinteractive.seedv6.search.tt;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.LegalMoveResolver;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.move.MoveIntent;
import com.ohinteractive.seedv6.core.move.MoveIntent.Promotion;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.Bound;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.Cacheability;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.Probe;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.ProbeOutcome;
import com.ohinteractive.seedv6.search.tt.TranspositionTable.StoreOutcome;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranspositionTableTest {

    @Test
    void directEntriesPreserveEveryFieldForEveryBound() {
        final TranspositionTable table = new TranspositionTable(4);
        final long[] keys = {0x1234_5678_9abc_def0L, -7L, 42L};
        final int[] depths = {0, 17, 64};
        final int[] scores = {91, 120, -120};
        final long[] moves = {0L, Long.MIN_VALUE, 0xfedc_ba98_7654_3210L};
        final Bound[] bounds = Bound.values();

        for(int i = 0; i < bounds.length; i ++) {
            table.advanceGeneration();
            assertEquals(
                StoreOutcome.STORED,
                table.store(
                    keys[i], depths[i], bounds[i], scores[i], 0, moves[i],
                    Cacheability.POSITION_ONLY
                )
            );
            final Probe probe = new Probe();
            final int alpha = bounds[i] == Bound.UPPER ? scores[i] : scores[i] - 1;
            final int beta = bounds[i] == Bound.LOWER ? scores[i] : scores[i] + 1;
            final ProbeOutcome outcome = table.probe(
                keys[i], depths[i], alpha, beta, 0, probe
            );

            assertEquals(expectedUsable(bounds[i]), outcome);
            assertTrue(probe.keyMatches());
            assertTrue(probe.scoreUsable());
            assertEquals(keys[i], probe.key());
            assertEquals(depths[i], probe.depth());
            assertEquals(scores[i], probe.score());
            assertEquals(bounds[i], probe.bound());
            assertEquals(moves[i], probe.move());
            assertEquals(table.generation(), probe.generation());
            assertTrue(probe.currentGeneration());
        }
    }

    @Test
    void emptyMismatchZeroKeyAndClearHaveIndependentValidity() {
        final TranspositionTable table = new TranspositionTable(1);
        final Probe probe = new Probe();

        assertEquals(ProbeOutcome.EMPTY, table.probe(0L, 0, -1, 1, 0, probe));
        assertFalse(probe.keyMatches());
        assertNull(probe.bound());

        assertEquals(
            StoreOutcome.STORED,
            table.store(0L, 3, Bound.EXACT, 17, 0, 9L, Cacheability.POSITION_ONLY)
        );
        assertEquals(ProbeOutcome.EXACT_HIT, table.probe(0L, 3, -1, 1, 0, probe));
        assertTrue(probe.keyMatches());
        assertEquals(0L, probe.key());
        assertEquals(17, probe.score());

        assertEquals(ProbeOutcome.KEY_MISMATCH, table.probe(1L, 0, -1, 1, 0, probe));
        assertFalse(probe.keyMatches());
        assertEquals(0L, probe.move());

        table.clear();
        assertEquals(ProbeOutcome.EMPTY, table.probe(0L, 0, -1, 1, 0, probe));
    }

    @Test
    void depthQualificationStillExposesHashMove() {
        final TranspositionTable table = new TranspositionTable(1);
        final Probe probe = new Probe();
        final long move = 0x1234_5678L;
        table.store(8L, 5, Bound.EXACT, 73, 0, move, Cacheability.POSITION_ONLY);

        assertEquals(ProbeOutcome.DEPTH_INSUFFICIENT, table.probe(8L, 6, -100, 100, 0, probe));
        assertTrue(probe.keyMatches());
        assertFalse(probe.scoreUsable());
        assertEquals(move, probe.move());
        assertEquals(ProbeOutcome.EXACT_HIT, table.probe(8L, 5, -100, 100, 0, probe));
        assertEquals(ProbeOutcome.EXACT_HIT, table.probe(8L, 4, -100, 100, 0, probe));
    }

    @Test
    void tableStoreAndProbeApplyMateNormalizationAcrossPlies() {
        final TranspositionTable table = new TranspositionTable(1);
        final Probe probe = new Probe();
        final int winning = TranspositionScores.MATE_SCORE - 8;
        table.store(4L, 6, Bound.EXACT, winning, 3, 44L, Cacheability.POSITION_ONLY);
        assertEquals(
            ProbeOutcome.EXACT_HIT,
            table.probe(4L, 6, -TranspositionScores.MATE_SCORE, TranspositionScores.MATE_SCORE, 6, probe)
        );
        assertEquals(winning - 3, probe.score());

        table.clear();
        final int losing = -TranspositionScores.MATE_SCORE + 8;
        table.store(4L, 6, Bound.EXACT, losing, 3, 45L, Cacheability.POSITION_ONLY);
        assertEquals(
            ProbeOutcome.EXACT_HIT,
            table.probe(4L, 6, -TranspositionScores.MATE_SCORE, TranspositionScores.MATE_SCORE, 6, probe)
        );
        assertEquals(losing + 3, probe.score());
    }

    @Test
    void failSoftBoundDirectionsCutOnlyWhenTheWindowPermits() {
        final TranspositionTable table = new TranspositionTable(1);
        final Probe probe = new Probe();

        table.store(1L, 7, Bound.EXACT, 25, 0, 11L, Cacheability.POSITION_ONLY);
        assertEquals(ProbeOutcome.EXACT_HIT, table.probe(1L, 7, 30, 40, 0, probe));

        table.clear();
        table.store(1L, 7, Bound.LOWER, 50, 0, 12L, Cacheability.POSITION_ONLY);
        assertEquals(ProbeOutcome.LOWER_HIT, table.probe(1L, 7, 0, 50, 0, probe));
        assertEquals(ProbeOutcome.BOUND_UNUSABLE, table.probe(1L, 7, 0, 51, 0, probe));
        assertEquals(12L, probe.move());
        assertFalse(probe.scoreUsable());

        table.clear();
        table.store(1L, 7, Bound.UPPER, -50, 0, 13L, Cacheability.POSITION_ONLY);
        assertEquals(ProbeOutcome.UPPER_HIT, table.probe(1L, 7, -50, 0, 0, probe));
        assertEquals(ProbeOutcome.BOUND_UNUSABLE, table.probe(1L, 7, -51, 0, 0, probe));
        assertEquals(13L, probe.move());
    }

    @Test
    void replacementIsDeterministicForSameKeyCollisionsDepthExactnessAndAge() {
        final TranspositionTable table = new TranspositionTable(1);
        final Probe probe = new Probe();

        assertEquals(stored(), store(table, 1L, 5, Bound.LOWER, 10, 101L));
        assertEquals(retained(), store(table, 1L, 4, Bound.EXACT, 20, 102L));
        assertEntry(table, probe, 1L, 5, Bound.LOWER, 10, 101L);

        assertEquals(stored(), store(table, 1L, 5, Bound.EXACT, 30, 103L));
        assertEquals(retained(), store(table, 1L, 5, Bound.UPPER, 40, 104L));
        assertEntry(table, probe, 1L, 5, Bound.EXACT, 30, 103L);

        assertEquals(stored(), store(table, 1L, 6, Bound.UPPER, 50, 105L));
        assertEquals(retained(), store(table, 2L, 5, Bound.EXACT, 60, 201L));
        assertEquals(stored(), store(table, 2L, 6, Bound.EXACT, 70, 202L));
        assertEquals(retained(), store(table, 3L, 6, Bound.EXACT, 80, 301L));
        assertEntry(table, probe, 2L, 6, Bound.EXACT, 70, 202L);

        assertEquals(stored(), store(table, 2L, 6, Bound.EXACT, 71, 203L));
        assertEntry(table, probe, 2L, 6, Bound.EXACT, 71, 203L);

        table.advanceGeneration();
        assertEquals(stored(), store(table, 3L, 1, Bound.UPPER, 90, 302L));
        assertEntry(table, probe, 3L, 1, Bound.UPPER, 90, 302L);
    }

    @Test
    void oldGenerationRemainsProbeableAndWrapClearsBeforeGenerationReuse() {
        final TranspositionTable table = new TranspositionTable(1);
        final Probe probe = new Probe();
        store(table, 1L, 4, Bound.EXACT, 1, 11L);

        table.advanceGeneration();
        assertEquals(1, table.generation());
        assertEquals(ProbeOutcome.EXACT_HIT, table.probe(1L, 4, -10, 10, 0, probe));
        assertFalse(probe.currentGeneration());
        assertEquals(0, probe.generation());

        for(int i = 0; i < 254; i ++) table.advanceGeneration();
        assertEquals(255, table.generation());
        store(table, 9L, 2, Bound.EXACT, 9, 99L);
        assertEquals(ProbeOutcome.EXACT_HIT, table.probe(9L, 2, -10, 10, 0, probe));
        assertTrue(probe.currentGeneration());

        table.advanceGeneration();
        assertEquals(0, table.generation());
        assertEquals(ProbeOutcome.EMPTY, table.probe(9L, 0, -10, 10, 0, probe));
        assertEquals(stored(), store(table, 7L, 0, Bound.UPPER, -1, 77L));
    }

    @Test
    void clearPreservesGenerationWhileNewGameClearsAndResetsIt() {
        final TranspositionTable table = new TranspositionTable(2);
        final Probe probe = new Probe();
        table.advanceGeneration();
        table.advanceGeneration();
        store(table, 5L, 2, Bound.EXACT, 5, 55L);

        table.clear();
        assertEquals(2, table.generation());
        assertEquals(ProbeOutcome.EMPTY, table.probe(5L, 0, -10, 10, 0, probe));

        store(table, 5L, 2, Bound.EXACT, 5, 55L);
        table.newGame();
        assertEquals(0, table.generation());
        assertEquals(ProbeOutcome.EMPTY, table.probe(5L, 0, -10, 10, 0, probe));
    }

    @Test
    void pathDependentValuesAreRejectedWithoutChangingTheSlot() {
        final TranspositionTable table = new TranspositionTable(1);
        final Probe probe = new Probe();
        assertEquals(
            StoreOutcome.NOT_CACHEABLE,
            table.store(1L, 4, Bound.EXACT, 0, 0, 10L, Cacheability.PATH_DEPENDENT)
        );
        assertEquals(ProbeOutcome.EMPTY, table.probe(1L, 0, -1, 1, 0, probe));

        store(table, 2L, 5, Bound.EXACT, 22, 20L);
        assertEquals(
            StoreOutcome.NOT_CACHEABLE,
            table.store(2L, 9, Bound.EXACT, 0, 0, 99L, Cacheability.PATH_DEPENDENT)
        );
        assertEntry(table, probe, 2L, 5, Bound.EXACT, 22, 20L);
    }

    @Test
    void fullV6MoveValuesRoundTripAndConsumerRejectsAStaleIllegalHint() {
        final TranspositionTable table = new TranspositionTable(1);
        final Probe probe = new Probe();
        final LegalMoveResolver resolver = new LegalMoveResolver();

        assertMoveRoundTrip(table, probe, resolve(resolver, Board.startingPosition(), "e2e4"));
        assertMoveRoundTrip(
            table, probe,
            resolve(resolver, Board.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1"), "e1g1")
        );
        assertMoveRoundTrip(
            table, probe,
            resolve(resolver, Board.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1"), "e5d6")
        );
        assertMoveRoundTrip(
            table, probe,
            resolve(resolver, Board.fromFen("7k/P7/8/8/8/8/8/7K w - - 0 1"), "a7a8q")
        );
        final long underpromotion = resolve(
            resolver, Board.fromFen("7k/P7/8/8/8/8/8/7K w - - 0 1"), "a7a8n"
        );
        assertMoveRoundTrip(table, probe, underpromotion);

        final long[] starting = Board.startingPosition();
        final long staleCastle = resolve(
            resolver, Board.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1"), "e1g1"
        );
        table.advanceGeneration();
        table.store(
            starting[Board.KEY], 3, Bound.EXACT, 12, 0, staleCastle,
            Cacheability.POSITION_ONLY
        );
        assertEquals(
            ProbeOutcome.EXACT_HIT,
            table.probe(starting[Board.KEY], 3, -100, 100, 0, probe)
        );
        assertEquals(staleCastle, probe.move());
        assertFalse(isLegalStoredMove(resolver, starting, probe.move()));
    }

    @Test
    void deterministicRandomizedPrimitiveFieldCorpusRoundTripsFiftyThousandEntries() {
        final int corpusSize = 50_000;
        final Random random = new Random(0x5eed_7007L);
        final TranspositionTable table = new TranspositionTable(1);
        final Probe probe = new Probe();

        for(int i = 0; i < corpusSize; i ++) {
            table.advanceGeneration();
            final long key = i % 997 == 0 ? 0L : random.nextLong();
            final int depth = random.nextInt(257);
            final int ply = random.nextInt(65);
            final Bound bound = Bound.values()[random.nextInt(Bound.values().length)];
            final int score;
            if((i & 15) == 0) {
                final int terminalPly = ply + random.nextInt(
                    TranspositionScores.MAX_MATE_PLY - ply + 1
                );
                final int magnitude = TranspositionScores.MATE_SCORE - terminalPly;
                score = random.nextBoolean() ? magnitude : -magnitude;
            } else {
                score = random.nextInt(60_001) - 30_000;
            }
            final long move = switch(i & 1023) {
                case 0 -> 0L;
                case 1 -> Long.MIN_VALUE;
                case 2 -> Long.MAX_VALUE;
                default -> random.nextLong();
            };

            assertEquals(
                StoreOutcome.STORED,
                table.store(key, depth, bound, score, ply, move, Cacheability.POSITION_ONLY)
            );
            final int alpha;
            final int beta;
            if(bound == Bound.LOWER) {
                alpha = score - 1;
                beta = score;
            } else if(bound == Bound.UPPER) {
                alpha = score;
                beta = score + 1;
            } else {
                alpha = -TranspositionScores.MATE_SCORE - 1;
                beta = TranspositionScores.MATE_SCORE + 1;
            }

            assertEquals(expectedUsable(bound), table.probe(key, depth, alpha, beta, ply, probe));
            assertEquals(key, probe.key());
            assertEquals(depth, probe.depth());
            assertEquals(score, probe.score());
            assertEquals(bound, probe.bound());
            assertEquals(move, probe.move());
            assertEquals(table.generation(), probe.generation());
            assertTrue(probe.currentGeneration());
        }
    }

    @Test
    void constructorAndCallContractsRejectInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> new TranspositionTable(0));
        assertThrows(IllegalArgumentException.class, () -> new TranspositionTable(3));
        final TranspositionTable table = new TranspositionTable(1);
        assertEquals(1, table.capacity());
        assertEquals(27, TranspositionTable.LOGICAL_BYTES_PER_ENTRY);
        assertThrows(
            IllegalArgumentException.class,
            () -> table.store(1L, -1, Bound.EXACT, 0, 0, 0L, Cacheability.POSITION_ONLY)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> table.probe(1L, -1, -1, 1, 0, new Probe())
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> table.probe(1L, 0, 1, 1, 0, new Probe())
        );
        assertThrows(
            NullPointerException.class,
            () -> table.probe(1L, 0, -1, 1, 0, null)
        );
    }

    private static StoreOutcome store(
        TranspositionTable table, long key, int depth, Bound bound, int score, long move
    ) {
        return table.store(key, depth, bound, score, 0, move, Cacheability.POSITION_ONLY);
    }

    private static StoreOutcome stored() {
        return StoreOutcome.STORED;
    }

    private static StoreOutcome retained() {
        return StoreOutcome.RETAINED_EXISTING;
    }

    private static void assertEntry(
        TranspositionTable table, Probe probe, long key, int depth, Bound bound,
        int score, long move
    ) {
        final int alpha = bound == Bound.UPPER ? score : score - 1;
        final int beta = bound == Bound.LOWER ? score : score + 1;
        assertEquals(expectedUsable(bound), table.probe(key, depth, alpha, beta, 0, probe));
        assertEquals(key, probe.key());
        assertEquals(depth, probe.depth());
        assertEquals(bound, probe.bound());
        assertEquals(score, probe.score());
        assertEquals(move, probe.move());
    }

    private static ProbeOutcome expectedUsable(Bound bound) {
        return switch(bound) {
            case EXACT -> ProbeOutcome.EXACT_HIT;
            case LOWER -> ProbeOutcome.LOWER_HIT;
            case UPPER -> ProbeOutcome.UPPER_HIT;
        };
    }

    private static void assertMoveRoundTrip(
        TranspositionTable table, Probe probe, long move
    ) {
        table.advanceGeneration();
        assertEquals(
            StoreOutcome.STORED,
            table.store(1L, 1, Bound.EXACT, 0, 0, move, Cacheability.POSITION_ONLY)
        );
        assertEquals(ProbeOutcome.EXACT_HIT, table.probe(1L, 1, -1, 1, 0, probe));
        assertEquals(move, probe.move());
    }

    private static long resolve(LegalMoveResolver resolver, long[] board, String coordinate) {
        final Promotion promotion = coordinate.length() == 4
            ? Promotion.NONE
            : switch(coordinate.charAt(4)) {
                case 'q' -> Promotion.QUEEN;
                case 'r' -> Promotion.ROOK;
                case 'b' -> Promotion.BISHOP;
                case 'n' -> Promotion.KNIGHT;
                default -> throw new IllegalArgumentException("Unknown promotion: " + coordinate);
            };
        return resolver.resolve(
            board,
            new MoveIntent(
                square(coordinate.substring(0, 2)),
                square(coordinate.substring(2, 4)),
                promotion
            )
        );
    }

    private static boolean isLegalStoredMove(
        LegalMoveResolver resolver, long[] board, long storedMove
    ) {
        try {
            final long legal = resolver.resolve(
                board,
                new MoveIntent(
                    Move.fromSquare(storedMove), Move.toSquare(storedMove),
                    Move.promotion(storedMove)
                )
            );
            return legal == storedMove;
        } catch(IllegalArgumentException exception) {
            return false;
        }
    }

    private static int square(String coordinate) {
        return (coordinate.charAt(1) - '1') * 8 + coordinate.charAt(0) - 'a';
    }
}
