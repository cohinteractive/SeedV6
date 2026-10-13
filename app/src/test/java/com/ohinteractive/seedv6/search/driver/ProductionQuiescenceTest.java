package com.ohinteractive.seedv6.search.driver;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model;
import com.ohinteractive.seedv6.core.brnpair2.BrnPair2Trainer;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.exact.ExactEvaluator;
import com.ohinteractive.seedv6.search.exact.ExactSearch;
import com.ohinteractive.seedv6.search.exact.IterationReuse;
import com.ohinteractive.seedv6.search.manage.*;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.model.NetworkModel;
import com.ohinteractive.seedv6.training.selfplay.*;
import com.ohinteractive.seedv6.training.validation.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(60)
class ProductionQuiescenceTest {
    private static SearchEvaluation definition() {
        return SearchEvaluation.brnPair2(new BrnPair2Model(new BrnPair2Trainer(.01).snapshot()));
    }

    @Test void quietLmrControlMatchesConfirmedCompositionAndRetainsTheUnreducedControl() {
        var evaluation = definition(); var board = Board.startingPosition();
        var diagnostics = new com.ohinteractive.seedv6.search.exact.LmrDiagnostics();
        try(var production = new SearchDriver(ProductionSearch.createQuietLmrControl(1, evaluation));
            var confirmed = new SearchDriver(ExactSearchAdapter.lateMoveReductionResearch(
                    evaluation, ExactSearch.LMR_QUIET, diagnostics));
            var control = new SearchDriver(ProductionSearch.createUnprunedControl(1, evaluation));
            var unpruned = new SearchDriver(ExactSearchAdapter.unprunedQuiescence(evaluation))) {
            var request = new SearchRequest(board, 5);
            var actual = production.search(request); var expected = confirmed.search(request);
            assertTrue(actual.targetDepthCompleted());
            assertTrue(diagnostics.attempts > 0, "Exercise actual reductions, not just identical shallow leaves");
            assertEquals(expected.lastCompletedResult(), actual.lastCompletedResult());
            assertEquals(expected.nodes(), actual.nodes());
            var off = control.search(request); var reference = unpruned.search(request);
            assertEquals(reference.lastCompletedResult(), off.lastCompletedResult());
            assertEquals(reference.nodes(), off.nodes());
            assertNotEquals(off.nodes(), actual.nodes(), "Explicit off must not inherit production LMR");
        }
    }

    @Test void preservedLmpAndManagedControlExecuteMeasuredOmissionsAndRetainQuietLmrControl() throws Exception {
        var evaluation = definition(); var board = Board.startingPosition();
        var diagnostics = new com.ohinteractive.seedv6.search.exact.LmpDiagnostics();
        SearchDriverOutcome expected;
        try(var confirmed = new SearchDriver(ExactSearchAdapter.lateMovePruningResearch(
                    evaluation, ExactSearch.LMP_WINDOW, diagnostics));
            var production = new SearchDriver(ProductionSearch.createIterationReuseControl(1, evaluation));
            var control = new SearchDriver(ProductionSearch.createQuietLmrControl(1, evaluation))) {
            var request = new SearchRequest(board, 4);
            expected = confirmed.search(request);
            assertTrue(java.util.Arrays.stream(diagnostics.omitted).sum() > 0, "Exercise real omissions");
            var actual = production.search(request);
            assertTrue(actual.targetDepthCompleted());
            assertEquals(expected.lastCompletedResult(), actual.lastCompletedResult());
            assertEquals(expected.nodes(), actual.nodes());
            assertNotEquals(control.search(request).nodes(), actual.nodes(), "Off remains a distinct control");
        }
        try(var play = SearchLifecycleService.iterationReuseControl(1, evaluation)) {
            play.setBookEnabled(false);
            var done = new CompletableFuture<ManagedSearchResult>();
            play.start(board, GameHistory.initial(board), new SearchLimits(4, -1, -1, false), done::complete);
            var actual = done.get(10, TimeUnit.SECONDS);
            assertNull(actual.failure());
            assertEquals(SearchTermination.COMPLETED, actual.termination());
            assertEquals(expected.lastCompletedResult(), actual.lastCompletedResult());
            assertEquals(expected.nodes(), actual.nodes());
        }
    }

    @Test void canonicalAndManagedReuseMatchTheConfirmedEffortComposition() throws Exception {
        // Fixed nonzero tuple fixture: the zero-residual material control has tied
        // initial-position ordering and can validly admit identical work with reuse.
        var weights=new float[new BrnPair2Trainer(.01).snapshot().parameters()];
        var random=new java.util.Random(5005);
        for(int i=0;i<weights.length-1;i++)weights[i]=(random.nextFloat()-.5f)*.25f;
        var evaluation=SearchEvaluation.brnPair2(new BrnPair2Model(new BrnPair2Trainer.Model(2,weights)));
        var board=Board.startingPosition();
        var diagnostics=new IterationReuse.Diagnostics();SearchDriverOutcome expected;
        try(var confirmed=new SearchDriver(ExactSearchAdapter.iterationReuseResearch(evaluation,IterationReuse.EFFORT,diagnostics));
            var production=new SearchDriver(evaluation);
            var off=new SearchDriver(ProductionSearch.createIterationReuseControl(1,evaluation))) {
            var request=new SearchRequest(board,4);expected=confirmed.search(request);
            assertTrue(diagnostics.missing>0);assertTrue(diagnostics.promoted>0);
            var actual=production.search(request);assertTrue(actual.targetDepthCompleted());
            assertEquals(expected.lastCompletedResult(),actual.lastCompletedResult());assertEquals(expected.nodes(),actual.nodes());
            assertNotEquals(off.search(request).nodes(),actual.nodes());
        }
        try(var service=new SearchLifecycleService(1,evaluation)) {
            service.setBookEnabled(false);var done=new CompletableFuture<ManagedSearchResult>();
            service.start(board,GameHistory.initial(board),new SearchLimits(4,-1,-1,false),done::complete);
            var actual=done.get(10,TimeUnit.SECONDS);assertNull(actual.failure());
            assertEquals(SearchTermination.COMPLETED,actual.termination());
            assertEquals(expected.lastCompletedResult(),actual.lastCompletedResult());assertEquals(expected.nodes(),actual.nodes());
        }
    }

    @Test void unprunedControlKeepsIndependentReferenceAndProductionMatchesMeasuredProtectedComposition() {
        var evaluation = definition();
        assertTrue(evaluation.isBrnPair2());
        assertFalse(SearchEvaluation.handcrafted().isBrnPair2());
        for(String fen : new String[] {
                "4k3/8/2p1p3/3q4/2P1P3/3Q4/8/4K3 w - - 0 1",
                "4r1k1/8/8/8/8/8/8/4K3 w - - 99 1",
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2",
                "8/k1P5/2K5/8/8/8/8/8 w - - 0 1",
                "7k/6Q1/5K2/8/8/8/8/8 b - - 0 1"}) {
            var board = Board.fromFen(fen); var original = board.clone();
            var reference = ExactSearch.quiescenceResearch(ExactEvaluator.from(evaluation)).search(board, 2);
            try(var canonical = ProductionSearch.createUnprunedControl(1, evaluation);
                var driver = new SearchDriver(evaluation);
                var candidate = new SearchDriver(ExactSearchAdapter.iterationReuseResearch(
                        evaluation, IterationReuse.EFFORT, null))) {
                var actual = canonical.search(new SearchRequest(board, 2));
                assertTrue(actual.completed());
                assertEquals(reference.score(), actual.score(), fen);
                var production = driver.search(new SearchRequest(board, 2));
                var frozenComposition = candidate.search(new SearchRequest(board, 2));
                assertTrue(production.targetDepthCompleted());
                assertEquals(frozenComposition.lastCompletedResult(), production.lastCompletedResult());
                assertEquals(frozenComposition.nodes(), production.nodes());
            }
            assertArrayEquals(original, board);
        }
    }

    @Test void productionIncludesQworkInWholeBudgetWhileStaticAndParallelControlsStayStatic() {
        var evaluation = definition(); var board = Board.startingPosition();
        try(var production = new SearchDriver(evaluation)) {
            var control = SearchControl.controlled(5000, System.nanoTime(), -1, TimeSource.SYSTEM);
            var result = production.search(new SearchRequest(board, GameHistory.initial(board), 20,
                    SearchObserver.NONE, control, true));
            assertEquals(5000, result.nodes());
            assertTrue(result.iterationIncomplete());
            assertTrue(result.lastCompletedResult().selective());
            assertTrue(result.diagnostics().worker().nodes().qNodes() > 0);
            assertEquals(SearchTermination.NODE_LIMIT, control.termination());
            assertEquals(0, production.activeSearchThreads());
            production.newGame();
            assertTrue(production.search(new SearchRequest(board, 1)).targetDepthCompleted());
        }
        var expected = new ExactSearch(evaluation).search(board, 2);
        for(int workers : new int[] {1, 2}) try(var control = ProductionSearch.createStaticControl(workers, evaluation)) {
            var result = control.search(new SearchRequest(board, 2));
            assertTrue(result.completed()); assertFalse(result.selective());
            assertEquals(expected.score(), result.score());
        }
        try(var parallel = ProductionSearch.create(2, evaluation)) {
            var result = parallel.search(new SearchRequest(board, 2));
            assertTrue(result.completed()); assertFalse(result.selective());
            assertEquals(expected.score(), result.score());
        }
    }

    @Test void sharedDefinitionOwnersCancelIndependentlyAndRemainReusable() throws Exception {
        var evaluation = definition(); var board = Board.startingPosition();
        var history = GameHistory.initial(board);
        try(var first = new SearchLifecycleService(1, evaluation);
            var second = new SearchLifecycleService(1, evaluation)) {
            first.setBookEnabled(false); second.setBookEnabled(false);
            var firstStarted = new CountDownLatch(1); var secondStarted = new CountDownLatch(1);
            var firstDone = new CompletableFuture<ManagedSearchResult>();
            var secondDone = new CompletableFuture<ManagedSearchResult>();
            first.start(board, history, new SearchLimits(0, -1, -1, true), started(firstStarted), true, firstDone::complete);
            second.start(board, history, new SearchLimits(0, -1, -1, true), started(secondStarted), true, secondDone::complete);
            assertTrue(firstStarted.await(10, TimeUnit.SECONDS));
            assertTrue(secondStarted.await(10, TimeUnit.SECONDS));
            first.stop();
            assertEquals(SearchTermination.STOPPED, firstDone.get(10, TimeUnit.SECONDS).termination());
            assertFalse(secondDone.isDone()); assertTrue(second.isWorking());
            second.stop();
            assertEquals(SearchTermination.STOPPED, secondDone.get(10, TimeUnit.SECONDS).termination());
            var again = new CompletableFuture<ManagedSearchResult>();
            first.start(board, history, new SearchLimits(2, -1, -1, false), again::complete);
            var result = again.get(10, TimeUnit.SECONDS);
            assertNull(result.failure()); assertTrue(result.lastCompletedResult().selective());
            assertEquals(SearchTermination.COMPLETED, result.termination());
        }
    }

    @Test void selfPlayAndValidationUseTheSameAcceptedCompositionAndRequireCompletedDepth() {
        var model = new BrnPair2Model(new BrnPair2Trainer(.01).snapshot());
        var evaluation = SearchEvaluation.brnPair2(model);
        var board = Board.startingPosition();
        var omissions = new IterationReuse.Diagnostics();
        SearchDriverOutcome expected;
        try(var driver = new SearchDriver(ExactSearchAdapter.iterationReuseResearch(
                evaluation, IterationReuse.EFFORT, omissions))) {
            expected = driver.search(new SearchRequest(board, 4));
            assertTrue(omissions.omissions > 0);
            assertTrue(omissions.promoted > 0);
        }
        var config = new SelfPlayConfig(1, 4, 1, 1, 0, 0, 1, 1, NnueScoreMapping.V1, -1, -1);
        var played = SelfPlayRunner.play(evaluation, config, 0, board, new SelfPlayControl());
        assertEquals(GameTermination.PLY_CAP, played.termination());
        assertEquals(expected.lastCompletedResult().bestMove(), played.positions().getFirst().playedMove());
        var limited = new SelfPlayConfig(1, 4, 1, 1, 0, 0, 1, 1, NnueScoreMapping.V1, 1, -1);
        var incomplete = SelfPlayRunner.play(evaluation, limited, 0, board, new SelfPlayControl());
        assertEquals(GameTermination.SEARCH_FAILURE, incomplete.termination());
        assertEquals(0, incomplete.playedPlies()); assertTrue(incomplete.result().isEmpty());

        var network = new NetworkModel.BrnPair2(model);
        var progress = new java.util.ArrayList<ValidationProgress>();
        var validation = new ValidationArena().validate(network, network,
                new ValidationConfig(1, 1, 0, 0, 4, 1, NnueScoreMapping.V1, 1), board,
                GameHistory.initial(board), new ValidationControl(), progress::add);
        assertEquals(2, validation.statistics().totalPlies());
        for(int game = 1; game <= 2; game++) {
            int ordinal = game;
            var observed = progress.stream().filter(p -> p.gameOrdinal() == ordinal && p.lastMoveSearch().isPresent())
                    .findFirst().orElseThrow().lastMoveSearch().orElseThrow();
            assertEquals(4, observed.depth()); assertEquals(expected.nodes(), observed.nodes());
        }
    }

    private static SearchObserver started(CountDownLatch latch) {
        return new SearchObserver() {
            @Override public void onIterationCompleted(IterationSnapshot result) { latch.countDown(); }
        };
    }
}
