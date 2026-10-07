package com.ohinteractive.seedv6.core.brn3;

import com.ohinteractive.seedv6.core.brnpair2.*;
import com.ohinteractive.seedv6.training.service.BrnResearchData;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.exact.ExactSearch;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.rules.GameHistory;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Explicit, bounded integration check against retained C02 models; never trains or promotes them. */
public final class BrnPair2ResearchParity {
    public static void main(String[] args)throws Exception {
        if(args.length!=3)throw new IllegalArgumentException("RESEARCH_MODEL DATASET EXPECTED_MODEL_SHA256");
        byte[] bytes=Files.readAllBytes(Path.of(args[0]));
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        if(!hash.equals(args[2]))throw new IOException("Frozen research model hash mismatch");
        var source=BrnTupleTrainer.Model.read(new ByteArrayInputStream(bytes));
        var model=new BrnPair2Model(new BrnPair2Trainer.Model(2,source.weights));
        var encoded=new ByteArrayOutputStream();BrnPair2Codec.writeModel(model,encoded);
        model=BrnPair2Codec.readModel(new ByteArrayInputStream(encoded.toByteArray()));
        var original=source.newWorkspace();var compiled=new BrnTupleCompiled(source).newWorkspace();var supported=model.newWorkspace();
        var data=BrnResearchData.read(Path.of(args[1]),true);
        double maximum=0;long count=0,integerDifferences=0;
        for(var partition:List.of(data.validation(),data.test()))for(var example:partition) {
            // Labels do not participate: only identical board geometry enters the three evaluators.
            var board=example.board();count++;
            for(double gain:new double[]{0,.125,.25,.5,1}) {
                double expected=original.evaluatePawns(board,gain),reference=compiled.evaluatePawns(board,gain),actual=supported.evaluatePawns(board,gain);
                maximum=Math.max(maximum,Math.abs(expected-actual));
                if(Double.doubleToLongBits(reference)!=Double.doubleToLongBits(actual))throw new AssertionError("Compiled C02 changed");
                if(Brn3Model.score(expected)!=Brn3Model.score(actual))integerDifferences++;
            }
        }
        if(count==0||maximum>1e-8||integerDifferences!=0)throw new AssertionError("Pair-2 model parity failed");
        var referenceCache=new BrnTupleCompiled(source).newWorkspace();
        var referenceTable=new TTable(4);var supportedTable=new TTable(4);
        var referenceSearch=new ExactSearch((b,ply)->Brn3Model.score(referenceCache.evaluatePawns(b,.25)),referenceTable);
        var supportedSearch=new ExactSearch(SearchEvaluation.brnPair2(model),supportedTable);
        long[] nanos=new long[2];long nodes=0;int roots=Math.min(32,data.validation().size());
        for(int pass=0;pass<3;pass++)for(int i=0;i<roots;i++) {
            var board=data.validation().get(i).board();var history=GameHistory.initial(board);
            com.ohinteractive.seedv6.search.exact.ExactSearchResult[] result=new com.ohinteractive.seedv6.search.exact.ExactSearchResult[2];
            for(int turn=0;turn<2;turn++) {
                int who=(pass+i+turn)&1;
                (who==0?referenceTable:supportedTable).clear();long start=System.nanoTime();
                result[who]=(who==0?referenceSearch:supportedSearch).search(board,history,3,ExactSearch.NEVER_CANCELLED);
                if(pass>0)nanos[who]+=System.nanoTime()-start;
            }
            if(!result[0].completed()||!result[1].completed()||result[0].score()!=result[1].score()
                    ||result[0].bestMove()!=result[1].bestMove()||result[0].nodes()!=result[1].nodes())throw new AssertionError("C02 search parity failed");
            if(pass>0)nodes+=result[1].nodes();
        }
        System.out.println("Pair-2 parity PASS model="+hash+" boards="+count+" gains=5 maxPawnError="+maximum+" integerDifferences="+integerDifferences);
        System.out.println("Compiled model bytes="+encoded.size()+" immutablePrimitiveBytes="+model.immutablePrimitiveBytes()+" workerPrimitiveBytes="+supported.primitiveBytes());
        System.out.println("Search depth=3 roots="+roots+" warmupPasses=1 measuredPasses=2 identicalScoresMovesNodes=true nodes="+nodes
                +" researchNs="+nanos[0]+" supportedNs="+nanos[1]+" supportedNps="+(long)(nodes*1e9/nanos[1])+" timing=bounded-observation-only");
    }
    private BrnPair2ResearchParity(){}
}
