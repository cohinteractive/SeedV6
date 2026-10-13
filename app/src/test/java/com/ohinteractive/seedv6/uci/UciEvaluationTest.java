package com.ohinteractive.seedv6.uci;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.model.*;
import static org.junit.jupiter.api.Assertions.*;

class UciEvaluationTest {
    @TempDir Path root;

    @Test void nullResearchIsExplicitCumulativeEffortAndSingleWorkerOnly() {
        assertEquals(com.ohinteractive.seedv6.search.exact.NullMovePolicy.OFF,UciEvaluation.load(new Properties()).nullPolicy());
        for(var policy:com.ohinteractive.seedv6.search.exact.NullMovePolicy.values()) {
            if(policy==com.ohinteractive.seedv6.search.exact.NullMovePolicy.OFF)continue;
            var props=new Properties();props.setProperty(UciEvaluation.PREFIX+"nmpResearch",policy.name().toLowerCase());
            assertThrows(IllegalArgumentException.class,()->UciEvaluation.load(props));
            var selected=new UciEvaluation(SearchEvaluation.handcraftedIsolation(),"test nmp=off",-1,-1,"off",-1,0,-1,policy);
            assertThrows(IllegalArgumentException.class,()->selected.service(1));
            assertThrows(IllegalArgumentException.class,()->selected.service(2));
            assertTrue(selected.identity(1).contains("nmp="+policy.name().toLowerCase()));
            assertTrue(selected.identity(1).contains("experimental=true"));
            props.setProperty(UciEvaluation.PREFIX+"pair2Store","missing");
            for(String key:new String[]{"reuseResearch","lmpResearch","lmrResearch","qsearchResearch"}) {
                props.setProperty(UciEvaluation.PREFIX+key,"off");
                assertThrows(IllegalArgumentException.class,()->UciEvaluation.load(props));props.remove(UciEvaluation.PREFIX+key);
            }
        }
    }

    @Test void reuseRequiresExplicitFullWindowPair2AndOneWorker() {
        assertEquals(UciEvaluation.REUSE_PRODUCTION,UciEvaluation.load(new Properties()).reusePolicy());
        assertThrows(IllegalArgumentException.class,()->UciEvaluation.reusePolicy("typo"));
        for(String name:new String[]{"observe","root","pv","score","effort","history","pvhistory"}) {
            int policy=UciEvaluation.reusePolicy(name);assertEquals(name,UciEvaluation.reuseName(policy));
            var props=new Properties();props.setProperty(UciEvaluation.PREFIX+"reuseResearch",name);
            assertThrows(IllegalArgumentException.class,()->UciEvaluation.load(props));
            var selected=new UciEvaluation(SearchEvaluation.handcraftedIsolation(),"test",-1,-1,"off",-1,0,policy);
            assertThrows(IllegalArgumentException.class,()->selected.service(1));
            assertThrows(IllegalArgumentException.class,()->selected.service(2));
            assertTrue(selected.identity(1).contains("reuse="+name));
            assertTrue(selected.identity(1).contains("experimental=true"));
            props.setProperty(UciEvaluation.PREFIX+"pair2Store","missing");
            for(String key:new String[]{"qsearchResearch","lmrResearch","lmpResearch","aspirationResearch","checkResearch"}) {
                props.setProperty(UciEvaluation.PREFIX+key,key.equals("qsearchResearch")?"static":key.equals("aspirationResearch")?"one16":key.equals("checkResearch")?"forced":"off");
                assertThrows(IllegalArgumentException.class,()->UciEvaluation.load(props));props.remove(UciEvaluation.PREFIX+key);
            }
        }
    }

    @Test void extensionsAreExplicitPair2OnlyAndCannotMixIncompatibleControls() {
        assertEquals(0,UciEvaluation.load(new Properties()).checkPolicy());
        assertThrows(IllegalArgumentException.class,()->UciEvaluation.extensionPolicy("typo"));
        for(String name:new String[]{"boundary","giving","evasion","forced","leaf","repeat"}) {
            var p=new Properties();p.setProperty(UciEvaluation.PREFIX+"checkResearch",name);
            assertThrows(IllegalArgumentException.class,()->UciEvaluation.load(p));
            var selected=new UciEvaluation(SearchEvaluation.handcraftedIsolation(),"test",-1,-1,"off",-1,
                    UciEvaluation.extensionPolicy(name));
            assertThrows(IllegalArgumentException.class,()->selected.service(1));
            assertThrows(IllegalArgumentException.class,()->selected.service(2));
            assertTrue(selected.identity(1).contains("check="+name));
            assertTrue(selected.identity(1).contains("experimental=true"));
            for(String key:new String[]{"qsearchResearch","lmrResearch","lmpResearch","aspirationResearch"}) {
                p.setProperty(UciEvaluation.PREFIX+"pair2Store","missing");
                p.setProperty(UciEvaluation.PREFIX+key,key.equals("qsearchResearch")?"static":key.equals("aspirationResearch")?"one16":"off");
                var e=assertThrows(IllegalArgumentException.class,()->UciEvaluation.load(p));
                assertTrue(e.getMessage().contains("Check research requires"));p.remove(UciEvaluation.PREFIX+key);
            }
        }
    }

    @Test void lmpRequiresExplicitCumulativePair2SingleWorkerAndFullWindows() {
        assertEquals(UciEvaluation.LMP_PRODUCTION,UciEvaluation.load(new Properties()).lmpPolicy());
        assertThrows(IllegalArgumentException.class,()->UciEvaluation.pruningPolicy("typo"));
        for(String name:new String[]{"shallow","lmr","combined","guarded","d2","window",
                "history-protect","history-extra","history-replace","history-deep","history-gate","history-leaf"}) {
            var p=new Properties();p.setProperty(UciEvaluation.PREFIX+"lmpResearch",name);
            assertThrows(IllegalArgumentException.class,()->UciEvaluation.load(p));
            var selected=new UciEvaluation(SearchEvaluation.handcraftedIsolation(),"test",-1,-1,"off",UciEvaluation.pruningPolicy(name));
            assertThrows(IllegalArgumentException.class,()->selected.service(2));
            assertTrue(selected.identity(1).contains("lmp="+name));
            assertTrue(selected.identity(1).contains("experimental=true"));
            for(String key:new String[]{"qsearchResearch","lmrResearch","aspirationResearch"}) {
                p.setProperty(UciEvaluation.PREFIX+"pair2Store","missing");
                p.setProperty(UciEvaluation.PREFIX+key,key.equals("qsearchResearch")?"static":key.equals("lmrResearch")?"off":"one16");
                assertThrows(IllegalArgumentException.class,()->UciEvaluation.load(p));
                p.remove(UciEvaluation.PREFIX+key);
            }
        }
    }

    @Test void aspirationIsExplicitAndCannotLeakIntoOtherPoliciesOrWorkers() {
        assertEquals("off", UciEvaluation.load(new Properties()).aspirationPolicy());
        var props = new Properties();
        props.setProperty(UciEvaluation.PREFIX + "aspirationResearch", "one16");
        assertThrows(IllegalArgumentException.class, () -> UciEvaluation.load(props));
        assertThrows(IllegalArgumentException.class, () -> UciEvaluation.aspirationPolicy("typo"));
        props.setProperty(UciEvaluation.PREFIX + "pair2Store", "missing");
        for(String key : new String[]{"qsearchResearch", "lmrResearch"}) {
            props.setProperty(UciEvaluation.PREFIX + key, key.startsWith("q") ? "static" : "off");
            assertThrows(IllegalArgumentException.class, () -> UciEvaluation.load(props));
            props.remove(UciEvaluation.PREFIX + key);
        }
        var selected = new UciEvaluation(SearchEvaluation.handcraftedIsolation(), "test", -1, -1, "one16");
        assertThrows(IllegalArgumentException.class, () -> selected.service(2));
        assertTrue(selected.identity(1).contains("aspiration=one16"));
        assertTrue(selected.identity(1).contains("experimental=true"));
    }

    @Test void researchIsExplicitRejectsUnknownPoliciesAndRequiresPair2() {
        assertEquals(-1, UciEvaluation.load(new Properties()).qpolicy());
        var properties = new Properties();
        properties.setProperty(UciEvaluation.PREFIX + "qsearchResearch", "see-promo");
        assertThrows(IllegalArgumentException.class, () -> UciEvaluation.load(properties));
        assertThrows(IllegalArgumentException.class, () -> UciEvaluation.researchPolicy("typo"));
        assertEquals(2, UciEvaluation.researchPolicy("see-promo"));
        var selected = new UciEvaluation(SearchEvaluation.handcraftedIsolation(), "test", 2, 0);
        assertThrows(IllegalArgumentException.class, () -> selected.service(2));
        try(var service = selected.service(1)) { assertNotNull(service); }
    }

    @Test void defaultIsUnchangedAndPartialConfigurationCannotFallBack() {
        assertSame(SearchEvaluation.handcrafted(), UciEvaluation.load(new Properties()).evaluation());
        var props = new Properties();
        props.setProperty(UciEvaluation.PREFIX + "checkpoint", "missing");
        assertThrows(IllegalArgumentException.class, () -> UciEvaluation.load(props));
        props.setProperty(UciEvaluation.PREFIX + "pair2Store", root.toString());
        assertThrows(IllegalArgumentException.class, () -> UciEvaluation.load(props));
        props.setProperty(UciEvaluation.PREFIX + "networkSha256", "0".repeat(64));
        assertThrows(IllegalArgumentException.class, () -> UciEvaluation.load(props));
    }

    @Test void lmrRequiresExplicitPair2SingleWorkerAndUnprunedLeaves() {
        assertEquals(UciEvaluation.LMR_PRODUCTION, UciEvaluation.load(new Properties()).lmrPolicy());
        assertThrows(IllegalArgumentException.class, () -> UciEvaluation.reductionPolicy("typo"));
        for(String name : new String[] {"fixed", "scaled", "scout", "quiet"}) {
            var props = new Properties();
            props.setProperty(UciEvaluation.PREFIX + "lmrResearch", name);
            assertThrows(IllegalArgumentException.class, () -> UciEvaluation.load(props));
            props.setProperty(UciEvaluation.PREFIX + "qsearchResearch", "static");
            assertThrows(IllegalArgumentException.class, () -> UciEvaluation.load(props));
            var selected = new UciEvaluation(SearchEvaluation.handcraftedIsolation(), "test", -1,
                    UciEvaluation.reductionPolicy(name));
            assertThrows(IllegalArgumentException.class, () -> selected.service(2));
        }
    }

    @Test void bestAndExactSelectionBindVerifiedPayloadAndRejectMismatchOrCorruption() throws Exception {
        CheckpointStore.Checkpoint best;
        CheckpointStore.Checkpoint latest;
        var trainer = (NetworkTrainingState.BrnPair2) NetworkTrainingState.initialized(TrainingArchitecture.BRN_PAIR2, 1, .01);
        try (var store = new CheckpointStore(root, TrainingArchitecture.BRN_PAIR2)) {
            best = store.initialize(trainer, new CheckpointManifest.Metadata(0, 1, ""));
            trainer.trainer().trainBatch(new long[][] {Board.startingPosition()}, new double[] {.8}, 1);
            latest = store.publish(trainer, new CheckpointManifest.Metadata(1, 1, best.manifest().id()));
        }
        var props = new Properties();
        props.setProperty(UciEvaluation.PREFIX + "pair2Store", root.toString());
        props.setProperty(UciEvaluation.PREFIX + "networkSha256", best.manifest().networkSha256());
        props.setProperty(UciEvaluation.PREFIX + "warmupDepth", "1");
        assertTrue(UciEvaluation.load(props).identity().contains(best.manifest().id()));
        props.setProperty(UciEvaluation.PREFIX + "checkpoint", latest.manifest().id());
        assertThrows(IllegalArgumentException.class, () -> UciEvaluation.load(props));
        props.setProperty(UciEvaluation.PREFIX + "networkSha256", latest.manifest().networkSha256());
        var loaded = UciEvaluation.load(props);
        assertTrue(loaded.identity().contains(latest.manifest().id()));
        assertEquals(UciEvaluation.PRODUCTION, loaded.qpolicy());
        assertEquals(UciEvaluation.LMR_PRODUCTION, loaded.lmrPolicy());
        assertEquals(UciEvaluation.LMP_PRODUCTION, loaded.lmpPolicy());
        assertEquals(UciEvaluation.REUSE_PRODUCTION, loaded.reusePolicy());
        assertTrue(loaded.identity(1).contains("reuse=effort "));
        assertTrue(loaded.identity(2).contains("reuse=off "));
        props.setProperty(UciEvaluation.PREFIX + "reuseResearch", "off");
        var reuseOff = UciEvaluation.load(props);
        assertTrue(reuseOff.identity(1).contains("reuse=off "));
        assertTrue(reuseOff.identity(1).contains("lmp=window "));
        assertFalse(reuseOff.identity(1).contains("experimental=true"));
        props.remove(UciEvaluation.PREFIX + "reuseResearch");
        assertTrue(loaded.identity(1).contains("lmp=window "));
        assertTrue(loaded.identity(1).contains("nominalLmpProof=off"));
        assertTrue(loaded.identity(2).contains("lmp=off "));
        assertFalse(loaded.identity(2).contains("nominalLmpProof=off"));
        props.setProperty(UciEvaluation.PREFIX + "lmpResearch", "off");
        var pruningOff = UciEvaluation.load(props);
        assertTrue(pruningOff.identity(1).contains("lmp=off "));
        assertTrue(pruningOff.identity(1).contains("lmr=quiet "));
        assertTrue(pruningOff.identity(1).contains("leaves=unpruned mode=control"));
        assertFalse(pruningOff.identity(1).contains("experimental=true"));
        props.remove(UciEvaluation.PREFIX + "lmpResearch");
        assertTrue(loaded.identity(1).contains("leaves=unpruned mode=production"));
        assertTrue(loaded.identity(1).contains("lmr=quiet "));
        assertTrue(loaded.identity(1).contains("nominalLmrProof=off"));
        assertFalse(loaded.identity(1).contains("experimental=true"));
        assertTrue(loaded.identity(2).contains("leaves=static mode=production"));
        assertTrue(loaded.identity(2).contains("lmr=off "));
        assertFalse(loaded.identity(2).contains("nominalLmrProof=off"));
        assertFalse(loaded.identity(2).contains("nominalQProof=off"));
        props.setProperty(UciEvaluation.PREFIX + "lmrResearch", "off");
        var off = UciEvaluation.load(props);
        assertTrue(off.identity(1).contains("leaves=unpruned mode=control"));
        assertTrue(off.identity(1).contains("lmr=off "));
        props.remove(UciEvaluation.PREFIX + "lmrResearch");
        props.setProperty(UciEvaluation.PREFIX + "qsearchResearch", "unpruned");
        assertTrue(UciEvaluation.load(props).identity(1).contains("lmr=off "));
        props.setProperty(UciEvaluation.PREFIX + "qsearchResearch", "static");
        var control = UciEvaluation.load(props);
        assertEquals(UciEvaluation.STATIC_CONTROL, control.qpolicy());
        assertTrue(control.identity(1).contains("leaves=static mode=control"));
        assertTrue(control.identity(1).contains("lmr=off "));
        for(var selected : new UciEvaluation[] {loaded, control, off, pruningOff, reuseOff})
        for(int workers : new int[] {1, 2}) try(var service = selected.service(workers)) {
            var board = Board.startingPosition();
            var book = new java.util.concurrent.CompletableFuture<com.ohinteractive.seedv6.search.manage.ManagedSearchResult>();
            service.start(board, com.ohinteractive.seedv6.rules.GameHistory.initial(board),
                    new com.ohinteractive.seedv6.search.manage.SearchLimits(2, -1, -1, false), book::complete);
            assertEquals(com.ohinteractive.seedv6.search.common.SearchTermination.BOOK,
                    book.get(5, java.util.concurrent.TimeUnit.SECONDS).termination());
            service.setBookEnabled(false);
            var searched = new java.util.concurrent.CompletableFuture<com.ohinteractive.seedv6.search.manage.ManagedSearchResult>();
            service.start(board, com.ohinteractive.seedv6.rules.GameHistory.initial(board),
                    new com.ohinteractive.seedv6.search.manage.SearchLimits(2, -1, -1, false), searched::complete);
            var result = searched.get(5, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(com.ohinteractive.seedv6.search.common.SearchTermination.COMPLETED, result.termination());
            assertNull(result.failure());
            assertEquals(workers == 1 && selected != control, result.lastCompletedResult().selective());
        }
        props.remove(UciEvaluation.PREFIX + "qsearchResearch");
        var state = loaded.evaluation().newState(2);
        var expected = latest.model().evaluation(com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping.V1).newState(2);
        assertEquals(expected.evaluate(Board.startingPosition(), 0), state.evaluate(Board.startingPosition(), 0));
        Path file = root.resolve("checkpoints").resolve(latest.manifest().id()).resolve("network.brn-pair2");
        byte[] bytes = Files.readAllBytes(file); bytes[100] ^= 1; Files.write(file, bytes);
        assertThrows(IllegalArgumentException.class, () -> UciEvaluation.load(props));
    }
}
