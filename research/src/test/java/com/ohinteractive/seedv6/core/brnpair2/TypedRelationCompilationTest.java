package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.core.brnpair2.TypedRelationPieceNetworkTest.*;

class TypedRelationCompilationTest {
    @Test void fullDoubleTrainingRoutesAgreeWithCompactPreactivationCache()throws Exception {
        var projected=new TypedRelationPieceNetwork(true,.01,71);var rng=new Random(97);
        for(int i=TypedRelationPieceNetwork.V;i<projected.weights.length;i++)projected.weights[i]=.2*rng.nextGaussian();
        var direct=read(bytes(projected));var w=new TypedRelationPieceNetwork.Workspace();
        var batch=new TypedRelationPieceNetwork.Features[]{w.encode(Board.startingPosition()).frozen(),w.encode(Board.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1")).frozen()};double[] targets={.8,.2};
        assertEquals(direct.gradientsUncached(batch,targets),projected.gradients(batch,targets),1e-14);
        double maximum=0;for(int i=0;i<projected.gradient.length;i++)maximum=Math.max(maximum,Math.abs(projected.gradient[i]-direct.gradient[i]));assertTrue(maximum<=1e-10);
        projected.prepareProjection(w);assertEquals(5400*7*8,w.gates.length);assertEquals(5400*8,w.projection.length);
        projected.updateFromGradients();direct.updateFromGradients();assertArrayEquals(direct.weights,projected.weights,1e-10);assertArrayEquals(direct.first,projected.first,1e-12);assertArrayEquals(direct.second,projected.second,1e-12);
        System.out.println("Full-double gradient maximum discrepancy="+maximum);
    }
    @Test void finiteAndTrainingCachesSwitchOwnersVersionsAndRoundTrip()throws Exception {
        var m=new TypedRelationPieceNetwork(true,.01,71);var other=new TypedRelationPieceNetwork(false,.01,97);var w=new TypedRelationPieceNetwork.Workspace();var board=Board.startingPosition();
        m.projectionLogit(w.encode(board),w);assertNull(w.gates);
        double first=m.scoreCp(board,w);assertNotNull(w.gates);assertEquals(5400*7*8,w.gates.length);assertEquals(m.scoreCpUncached(board,new TypedRelationPieceNetwork.Workspace()),first,1e-9);
        assertEquals(other.scoreCp(board,new TypedRelationPieceNetwork.Workspace()),other.scoreCp(board,w));assertNotNull(w.gates);
        assertEquals(first,m.scoreCp(board,w));var f=w.encode(board).frozen();m.train(new TypedRelationPieceNetwork.Features[]{f},new double[]{.8});
        assertEquals(m.scoreCp(board,new TypedRelationPieceNetwork.Workspace()),m.scoreCp(board,w));assertEquals(m.scoreCp(board,w),read(bytes(m)).scoreCp(board,w));
        assertEquals(m.scoreCpUncached(board,new TypedRelationPieceNetwork.Workspace()),m.scoreCpProjection(board,w),1e-10);
    }
}
