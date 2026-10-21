package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.brnnext.BrnNextModel;
import java.io.*;
import java.util.*;
import static com.ohinteractive.seedv6.core.brnnext.BrnNextArchitecture.*;

/** Research-only additive ablation. No model/state export or original-store mutation.
 * Each square's unary term is assigned to exactly one existing incident relation.
 * Algebraically equivalent; binary64 summation order changes and needs measured oracles. */
public final class BrnUnaryResearchCompiler {
    public static BrnPair2Model compile(BrnPair2Model base,BrnNextModel unary) throws IOException {
        Objects.requireNonNull(base);Objects.requireNonNull(unary);
        if(!unary.architecture().frozenBase()||unary.architecture().calibratedHeads())throw new IllegalArgumentException("Ordinary frozen-base correction required");
        for(int i=EMBEDDING;i<unary.architecture().parameters();i++)
            if(unary.weight(i)!=0)throw new IllegalArgumentException("Explicit unary-only correction required");
        var keys=new TreeSet<Integer>();
        for(int side=0;side<2;side++)for(int t=0;t<BrnPair2Geometry.TEMPLATES;t++) {
            int a=BrnPair2Geometry.cell(t,0)^(side*56),b=BrnPair2Geometry.cell(t,1)^(side*56);
            keys.add(Math.min(a,b)*64+Math.max(a,b));
        }
        if(keys.size()!=BrnPair2Codec.PAIRS)throw new IllegalStateException("Compiled relation identity changed");
        int[] owner=new int[64];Arrays.fill(owner,-1);int index=0;
        for(int key:keys){int a=key/64,b=key%64;if(owner[a]<0)owner[a]=index;if(owner[b]<0)owner[b]=index;index++;}
        for(int value:owner)if(value<0)throw new IllegalStateException("Uncovered unary square");
        var result=new BrnPair2Model(null,base.bias()+unary.weight(0));index=0;
        for(int key:keys) {
            int a=key/64,b=key%64;
            for(int low=0;low<13;low++)for(int high=0;high<13;high++)for(int side=0;side<2;side++) {
                double addition=(owner[a]==index?value(unary,a,low,side):0)
                        +(owner[b]==index?value(unary,b,high,side):0);
                int address=(index*169+low+13*high)*2+side;
                result.weights[address]=base.weight(address)+addition/BrnPair2Geometry.NORM;
            }
            index++;
        }
        // Validate shape, finite values and checksum with the unchanged public codec.
        var bytes=new ByteArrayOutputStream();BrnPair2Codec.writeModel(result,bytes);
        return BrnPair2Codec.readModel(new ByteArrayInputStream(bytes.toByteArray()));
    }
    private static double value(BrnNextModel model,int physical,int category,int side) {
        if(category==0)return 0;
        if(side==1)category=category<=6?category+6:category-6;
        return PIECE_NORM*model.weight(UNARY+(category-1)*64+(physical^(side*56)));
    }
    private BrnUnaryResearchCompiler() {}
}
