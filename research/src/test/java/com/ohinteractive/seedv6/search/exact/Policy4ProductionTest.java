package com.ohinteractive.seedv6.search.exact;

import java.lang.reflect.Field;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn.BrnModel;
import com.ohinteractive.seedv6.core.brn1.Brn1Model;
import com.ohinteractive.seedv6.core.brn2.Brn2Model;
import com.ohinteractive.seedv6.core.brn3.*;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.search.tt.TTable;
import static org.junit.jupiter.api.Assertions.*;

/** Actual production owners are inspected before attaching observation-only counters. */
@Timeout(180)
class Policy4ProductionTest {
    private static final String[] POLICIES = {"completedHints", "ordering", "mechanics", "sortCrossover",
            "traversal", "quiescence", "nominalQsearch", "qsearchPruning", "qdepthLimit", "qtable",
            "lmrPolicy", "lmpPolicy", "seePolicy", "checkPolicy", "nullMovePruning", "reverseFutility",
            "mateDistancePruning", "pairNull", "internalPolicy", "futilityPolicy", "depthFeedback",
            "tacticalExtension", "historyLearning", "recapturesOnlyAfterFirst", "quietChecks"};

    @Test void allWorkerConfigurationsMatchAcceptedResearchAndPreservePrivateState() throws Exception {
        var evaluation = P3InformationBaselineTest.evaluation();
        for(int count = 1; count <= 16; count++) {
            try(var production = ProductionSearch.create(count, evaluation);
                var candidate = InformationResearch.create(evaluation, count, 64, 4);
                var control = InformationResearch.create(evaluation, count, 64, 0)) {
                var actual = searches(production); var accepted = searches(candidate);
                var previous = searches(control);
                assertEquals(count, actual.length);
                var privateHistories = Collections.newSetFromMap(new IdentityHashMap<>());
                var privateBoards = Collections.newSetFromMap(new IdentityHashMap<>());
                Object table = field(actual[0], "table");
                assertEquals(2_097_152, ((long[])field(table, "key")).length); // requested 64 MiB
                for(int i = 0; i < count; i++) {
                    for(String name : POLICIES) assertEquals(field(accepted[i], name), field(actual[i], name), name);
                    assertEquals(true, field(actual[i], "completedHints"));
                    assertEquals(false, field(previous[i], "completedHints"));
                    assertNull(field(actual[i], "historyLearning"));
                    assertEquals(IterationReuse.EFFORT, ((IterationReuse)field(actual[i], "iterationReuse")).policy);
                    assertSame(table, field(actual[i], "table"));
                    assertNotSame(table, field(accepted[i], "table"));
                    assertTrue(privateHistories.add(field(actual[i], "quietHistory")));
                    assertTrue(privateBoards.add(field(actual[i], "boards")));
                }
                if(count > 1) {
                    assertEquals(ConcurrencyPolicy.ROOT_REUSE, field(production, "policy"));
                    assertEquals(field(candidate, "policy"), field(production, "policy"));
                }
            }
        }
    }

    @Test void singleWorkerMatchesAcceptedCandidateAcrossIterationsRepeatedAndChangedHistory() throws Exception {
        var e = P3InformationBaselineTest.evaluation();
        String[] fens = {Board.FEN_STARTING_POSITION, SeePruningTest.MIDDLE,
                "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2", "8/k1P5/2K5/8/8/8/8/8 w - - 0 1",
                "7k/6Q1/5K2/8/8/8/8/8 b - - 100 1", "7k/5Q2/6K1/8/8/8/8/8 b - - 0 1"};
        try(var production = new SearchDriver(ProductionSearch.create(1, e));
            var candidate = new SearchDriver(InformationResearch.create(e, 1, 64, 4))) {
            for(String fen : fens) {
                var board = Board.fromFen(fen); var original = board.clone();
                var initial = GameHistory.initial(board);
                var repetition = GameHistory.builder(initial).appendPosition(board).appendPosition(board).snapshot();
                for(var history : List.of(initial, initial, repetition)) {
                    var request = new SearchRequest(board, history, 4);
                    var a = production.search(request); var b = candidate.search(request);
                    assertEquals(b.lastCompletedResult(), a.lastCompletedResult(), fen);
                    assertEquals(b.nodes(), a.nodes(), fen);
                    assertTrue(a.targetDepthCompleted());
                    Phase2Oracle.legalPv(board, a.lastCompletedResult().principalVariation());
                    assertArrayEquals(original, board);
                }
            }
        }
    }

    @ParameterizedTest @ValueSource(ints={1,2,4,8,16})
    void defaultHintsAreProducedReusedAndDrainedWhileControlStaysOff(int count) throws Exception {
        var e = P3InformationBaselineTest.evaluation(); var board = Board.startingPosition();
        var history = GameHistory.initial(board);
        for(int hash : new int[]{1,64}) for(boolean enabled : new boolean[]{true,false}) {
            var owner = enabled ? ProductionSearch.create(count,e,hash)
                    : ProductionSearch.createInformationControl(count,e,hash);
            var workers = searches(owner); var sinks = new MoveHintDiagnostics[count];
            for(int i=0;i<count;i++) {
                assertEquals(enabled, field(workers[i], "completedHints"));
                sinks[i] = new MoveHintDiagnostics(); set(workers[i], "hintDiagnostics", sinks[i]);
            }
            try(var driver = new SearchDriver(owner)) {
                long stored=0, hits=0;
                var table = (TTable)field(workers[0], "table");
                long key = SearchKey.key(board, SearchKey.rootHistory(board, history));
                for(int repeat=0;repeat<2;repeat++) {
                    var result = driver.search(new SearchRequest(board,history,4));
                    assertTrue(result.targetDepthCompleted());
                    Phase2Oracle.legalPv(board,result.lastCompletedResult().principalVariation());
                    assertEquals(0,driver.activeSearchThreads());
                    for(var worker:workers) assertEquals(false,field(worker,"requestActive"));
                    for(var sink:sinks) { stored+=sink.stored; hits+=sink.hits; assertEquals(0,sink.illegal); }
                    assertFalse(table.probe(key,new TTable.TEntry()), "Selective root cannot supply ordinary proof");
                    if(!enabled) assertEquals(0,table.probeHint(key));
                }
                assertEquals(enabled, stored>0); assertEquals(enabled, hits>0);
                driver.newGame(); assertEquals(0,table.probeHint(key));
                var cap = SearchControl.controlled(200,0,-1,TimeSource.SYSTEM);
                var stopped = driver.search(new SearchRequest(board,history,20,SearchObserver.NONE,cap,true));
                assertEquals(SearchTermination.NODE_LIMIT,cap.termination()); assertEquals(200,stopped.nodes());
                assertEquals(stopped.nodes(),stopped.diagnostics().totalEnteredNodes());
                assertEquals(0,driver.activeSearchThreads());
                if(stopped.lastCompletedResult()!=null) Phase2Oracle.legalPv(board,stopped.lastCompletedResult().principalVariation());
                assertTrue(driver.search(new SearchRequest(board,history,3)).targetDepthCompleted());
                Thread.currentThread().interrupt();
                try { assertFalse(driver.search(new SearchRequest(board,history,8)).targetDepthCompleted()); }
                finally { Thread.interrupted(); }
                assertEquals(0,driver.activeSearchThreads());
                driver.newGame(); assertTrue(driver.search(new SearchRequest(board,history,3)).targetDepthCompleted());
            }
            assertEquals(0,owner.activeSearchThreads());
        }
    }

    @Test void otherEvaluatorsKeepTheirPreviousStaticAndCalibratedPolicies() throws Exception {
        var nnue=NnueNetwork.initialized(41); var brn2=new Brn2Model(41);
        var definitions=List.of(SearchEvaluation.handcrafted(), SearchEvaluation.incremental(nnue),
                SearchEvaluation.fullRecompute(nnue,NnueScoreMapping.V1), SearchEvaluation.brn(new BrnModel()),
                SearchEvaluation.brn1(new Brn1Model()), SearchEvaluation.brn2(brn2),
                SearchEvaluation.brn2FullRecompute(brn2),
                SearchEvaluation.brn3(new Brn3Model(new float[Brn3Model.PARAMETER_COUNT])));
        var board=Board.fromFen("4k3/8/8/8/8/8/P7/4K3 w - - 0 1");
        for(var e:definitions) for(int count:new int[]{1,4,16}) {
            try(var actual=ProductionSearch.create(count,e,1);
                var old=ProductionSearch.createInformationControl(count,e,1)) {
                var a=searches(actual); var b=searches(old);
                for(int i=0;i<count;i++) {
                    for(String name:POLICIES) assertEquals(field(b[i],name),field(a[i],name),name);
                    assertEquals(false,field(a[i],"completedHints"));
                    assertEquals(false,field(a[i],"quiescence"));
                }
                var result=actual.search(new SearchRequest(board,3)); var reference=old.search(new SearchRequest(board,3));
                assertTrue(result.completed()); assertEquals(reference.score(),result.score());
                Phase2Oracle.legalPv(board,result.principalVariation());
            }
        }
    }

    static ExactSearch[] searches(Object owner) throws Exception {
        if(owner instanceof ExactSearchAdapter) return new ExactSearch[]{(ExactSearch)field(owner,"exact")};
        Object[] workers=(Object[])field(owner,"workers"); var result=new ExactSearch[workers.length];
        for(int i=0;i<workers.length;i++) result[i]=(ExactSearch)field(workers[i],"search");
        return result;
    }
    static Object field(Object owner,String name) throws Exception {
        Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);
    }
    private static void set(Object owner,String name,Object value) throws Exception {
        Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);f.set(owner,value);
    }
}
