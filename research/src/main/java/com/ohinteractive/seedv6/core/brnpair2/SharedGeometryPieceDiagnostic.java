package com.ohinteractive.seedv6.core.brnpair2;

import java.util.*;

/** Shared deterministic batch/segment semantics; R037 admits no large training. */
public final class SharedGeometryPieceDiagnostic {
    static void segment(SharedGeometryPieceNetwork model,PairCoverageDiagnostic.Data data,long seed,int epoch,int begin,int end) {
        if(begin<0||begin%128!=0||end>data.rows||end<=begin||(end!=data.rows&&end%128!=0))throw new IllegalArgumentException("Segment bounds");
        int[] order=new int[data.rows];for(int i=0;i<order.length;i++)order[i]=i;var random=new Random(seed+epoch);
        for(int i=order.length-1;i>0;i--){int j=random.nextInt(i+1),v=order[i];order[i]=order[j];order[j]=v;}
        var w=new SharedGeometryPieceNetwork.Workspace();long[] board=new long[PairCoverageDiagnostic.Data.STRIDE];
        var buffer=new SharedGeometryPieceNetwork.Features[128];for(int i=0;i<buffer.length;i++)buffer[i]=new SharedGeometryPieceNetwork.Features();double[] targets=new double[128];
        for(int offset=begin;offset<end;offset+=128) {
            int n=Math.min(128,end-offset);var batch=n==128?buffer:Arrays.copyOf(buffer,n);var target=n==128?targets:new double[n];
            for(int j=0;j<n;j++){int i=order[offset+j];data.board(i,board);batch[j].copyFrom(w.encode(board));target[j]=data.targets[i];}
            model.train(batch,target);
        }
    }
    private SharedGeometryPieceDiagnostic(){}
}
