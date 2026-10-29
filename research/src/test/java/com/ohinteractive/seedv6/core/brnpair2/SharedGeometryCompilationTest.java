package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.core.brnpair2.SharedGeometryPieceNetwork.*;
import static com.ohinteractive.seedv6.core.brnpair2.SharedGeometryPieceNetworkTest.bytes;
import static com.ohinteractive.seedv6.core.brnpair2.SharedGeometryPieceNetworkTest.read;

class SharedGeometryCompilationTest {
    static Features[] batch() {
        var data=ContextualPieceFineTuneTest.data();var w=new Workspace();long[] board=new long[6];var batch=new Features[257];
        for(int i=0;i<batch.length;i++){data.board(i,board);batch[i]=w.encode(board).frozen();}return batch;
    }
    @Test void compiledForwardRetainsOwnershipBoardIsolationAndMutationInvalidation()throws Exception {
        var w=new Workspace();long[][] boards={Board.startingPosition(),Board.fromFen("q6k/8/8/8/P7/8/8/R6K w - - 0 1"),Board.fromFen("q6k/8/8/8/1P6/8/8/R6K w - - 0 1")};
        for(boolean context:new boolean[]{false,true}) {
            var a=new SharedGeometryPieceNetwork(context,.01,71);var b=new SharedGeometryPieceNetwork(context,.01,97);
            for(int cycle=0;cycle<3;cycle++)for(var model:List.of(a,b,a))for(var board:boards)assertEquals(model.scoreCpUncached(board,new Workspace()),model.scoreCp(board,w),1e-12);
            a.weights[A+10]=.4;a.invalidateProjections();for(var board:boards)assertEquals(a.scoreCpUncached(board,new Workspace()),a.scoreCp(board,w),1e-12);
            var batch=batch();double[] targets=new double[batch.length];Arrays.fill(targets,.8);a.train(batch,targets);
            for(var board:boards)assertEquals(a.scoreCpUncached(board,new Workspace()),a.scoreCp(board,w),1e-12);
            var restored=read(bytes(a));for(var board:boards)assertEquals(restored.scoreCpUncached(board,new Workspace()),restored.scoreCp(board,w),1e-12);
        }
    }
    @Test void groupedBackprojectionMatchesUncachedMasksGradientsAndHistoryUpdates()throws Exception {
        var batch=batch();double[] targets=new double[batch.length];for(int i=0;i<targets.length;i++)targets[i]=(i%5)/4.0;
        for(boolean context:new boolean[]{false,true}) {
            var compiled=new SharedGeometryPieceNetwork(context,.01,71);for(int n=0;n<3;n++)compiled.train(batch,targets);
            var reference=read(bytes(compiled));double a=compiled.gradients(batch,targets),b=reference.gradientsUncached(batch,targets);assertEquals(b,a,1e-14);
            boolean[] activeA=new boolean[PARAMETERS],activeB=new boolean[PARAMETERS];for(int n=0;n<compiled.count;n++)activeA[compiled.touched[n]]=true;for(int n=0;n<reference.count;n++)activeB[reference.touched[n]]=true;assertArrayEquals(activeB,activeA);
            double maximum=0;for(int p=0;p<PARAMETERS;p++)if(activeA[p]){maximum=Math.max(maximum,Math.abs(compiled.gradient[p]-reference.gradient[p]));assertEquals(reference.gradient[p],compiled.gradient[p],1e-10);}
            compiled.updateFromGradients();reference.updateFromGradients();assertArrayEquals(reference.weights,compiled.weights,1e-10);assertArrayEquals(reference.first,compiled.first,1e-10);assertArrayEquals(reference.second,compiled.second,1e-10);
            System.out.println("context="+context+" compiled gradient maximum error="+maximum);
        }
    }
    @Test void exactlyCancelledStaticGradientStillUpdatesExistingMoments()throws Exception {
        var model=new SharedGeometryPieceNetwork(true,.01,71);var f=new Workspace().encode(Board.startingPosition()).frozen();
        double p=PairObjectiveDiagnostic.sigmoid(model.logit(f,new Workspace()));var batch=new Features[]{f,f};double[] targets={p,p};
        Arrays.fill(model.first,.02);Arrays.fill(model.second,.03);model.step=7;var reference=read(bytes(model));
        assertEquals(0,model.gradients(batch,targets));assertEquals(0,reference.gradientsUncached(batch,targets));
        boolean[] a=new boolean[PARAMETERS],b=new boolean[PARAMETERS];for(int n=0;n<model.count;n++)a[model.touched[n]]=true;for(int n=0;n<reference.count;n++)b[reference.touched[n]]=true;assertArrayEquals(b,a);
        int coefficient=A;assertTrue(a[coefficient]);assertEquals(0,model.gradient[coefficient]);
        model.updateFromGradients();reference.updateFromGradients();assertArrayEquals(reference.weights,model.weights);assertEquals(.018,model.first[coefficient],1e-16);
    }
}
