package com.ohinteractive.seedv6.training.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.ohinteractive.seedv6.training.history.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.training.service.TrainerServiceTest.*;

@Tag("slow-nnue")
class TrainerHistoryTest {
    @TempDir Path root;
    @Test void completedCappedValidationIsInconclusiveNotRejection() throws Exception {
        var cfg=config(root,1);
        var bounded=new TrainerConfig(root,cfg.masterSeed(),cfg.selfPlay(),cfg.training(),
                new TrainerConfig.Validation(2,0,0,1,1,1,MAPPING,GATE),1,cfg.depthChange(),cfg.startingFen());
        try(var service=TrainerService.fresh(bounded,trainer())) { finish(service); }
        var record=new HistoryRepository(root).refresh().records().getFirst();
        assertEquals(GenerationRecord.Outcome.INCONCLUSIVE,record.outcome());assertNull(record.score());
        assertEquals(2,record.incompletePairs());assertEquals(0,record.wins()+record.draws()+record.losses());
    }
    @Test void analyticsFailureCannotInvalidatePromotionOrStopNextGeneration() throws Exception {
        Matches operations=new Matches(true) {
            @Override void appendHistory(HistoryRepository repository,GenerationRecord record) throws IOException {
                assertEquals(record.candidate(),record.resultingBest(),"Promotion already authoritative before analytics");
                throw new IOException("Injected history failure");
            }
        };
        try(var service=TrainerService.fresh(config(root,2),trainer(),operations,ignore())) {
            var end=finish(service);assertEquals(2,end.totals().promotions());assertEquals(2,end.totals().completedGenerations());
            assertTrue(service.historyWarning().contains("NOT confirmed persisted"));
            assertTrue(service.historyWarning().contains("Injected history failure"));assertTrue(service.failure().isEmpty());
        }
        assertTrue(new HistoryRepository(root).refresh().records().isEmpty());
    }
    @Test void boundedRealTrainingComparisonAndForcedAppendCost() throws Exception {
        List<Long> writes=new ArrayList<>(); List<Double> enabled=new ArrayList<>(),disabled=new ArrayList<>();
        // Real self-play, Adam, arena, checkpoint and promotion path on both sides. Two order-reversed trials.
        for(int trial=0;trial<2;trial++) {
            String identity=null;
            for(boolean recording:trial==0?List.of(false,true):List.of(true,false)) {
                var ops=new TrainerService.Operations() {
                    @Override void appendHistory(HistoryRepository repository,GenerationRecord record) throws IOException {
                        if(recording) { long start=System.nanoTime();super.appendHistory(repository,record);writes.add(System.nanoTime()-start); }
                    }
                };
                Path location=root.resolve(trial+"-"+recording);
                try(var service=TrainerService.fresh(config(location,2),trainer(),ops,ignore())) {
                    long start=System.nanoTime();var end=finish(service);double elapsed=(System.nanoTime()-start)/1e9;
                    (recording?enabled:disabled).add(elapsed);
                    if(identity==null) identity=end.latestTrainingId();else assertEquals(identity,end.latestTrainingId());
                    assertEquals(recording?2:0,new HistoryRepository(location).refresh().records().size());
                }
            }
        }
        System.out.println("PHASE3_REAL_COMPARISON enabledSeconds="+enabled+" disabledSeconds="+disabled+" forcedAppendMillis="+writes.stream().map(n->n/1e6).toList()+" identicalFinalCheckpoint=true; bounded plumbing scenario, not strength or full-workload benchmark");
    }
}
