package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.core.brnpair2.AttentivePieceNetwork.*;

class RoutingMechanismAuditTest {
    static byte[] bytes(AttentivePieceNetwork m)throws IOException{var out=new ByteArrayOutputStream();m.write(out);return out.toByteArray();}
    @Test void frozenReconstructionZeroCouplingAndContextAblationDoNotMutate()throws Exception {
        var model=new AttentivePieceNetwork(2,.01,71);var rng=new Random(97);for(int i=V;i<PARAMETERS;i++)model.weights[i]=.3*rng.nextGaussian();byte[] before=bytes(model);boolean contextEffect=false;
        var report=new RoutingMechanismAudit.Report();
        for(String fen:List.of("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1","r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","4k3/P7/8/8/8/8/7p/4K3 b - - 0 1")) {
            var b=Board.fromFen(fen);var saved=b.clone();var w=new Workspace();double[] scores=RoutingMechanismAudit.inspect(model,b,.65,report);
            assertEquals(model.logit(w.encode(b),w),scores[0],1e-12);assertEquals(model.logit(w.encode(b),w,1),scores[1],1e-12);assertArrayEquals(saved,b);contextEffect|=Math.abs(scores[0]-scores[2])>1e-8;
        }
        assertTrue(contextEffect);assertEquals(4,report.rows);assertArrayEquals(before,bytes(model));
        Arrays.fill(model.weights,K,PARAMETERS,0);var zero=new RoutingMechanismAudit.Report();double[] scores=RoutingMechanismAudit.inspect(model,Board.startingPosition(),.5,zero);assertEquals(scores[0],scores[1],1e-12);assertEquals(scores[0],scores[2],1e-12);assertEquals(0,zero.metrics.get("context_centered_rms").sum);assertEquals(0,zero.metrics.get("remove_neighborhood_attention_l1").sum);
        assertThrows(IllegalArgumentException.class,()->RoutingMechanismAudit.inspect(new AttentivePieceNetwork(1,.01,71),Board.startingPosition(),.5,new RoutingMechanismAudit.Report()));
    }
    @Test void centeredMetricsAndShiftedSoftmaxIgnoreReceiverOffsets() {
        assertEquals(2,RoutingMechanismAudit.centeredRms(new double[]{2,4,6},1));assertEquals(Math.sqrt(8.0/3),RoutingMechanismAudit.centeredRms(new double[]{2,4,6},-1));assertEquals(2,RoutingMechanismAudit.centeredRms(new double[]{1002,1004,1006},1));
        double[] a=RoutingMechanismAudit.softmax(new double[]{2,4,6},1),b=RoutingMechanismAudit.softmax(new double[]{1002,1004,1006},1);assertArrayEquals(a,b);assertEquals(0,a[1]);assertEquals(1,a[0]+a[2],1e-15);
        assertArrayEquals(new double[]{1,0,0},RoutingMechanismAudit.softmax(new double[]{10000,-10000,0},2));
        var summary=new RoutingMechanismAudit.Summary();assertThrows(ArithmeticException.class,()->summary.add(Double.NaN));
    }
}
