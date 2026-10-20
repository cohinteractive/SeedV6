package com.ohinteractive.seedv6.tools.eval;

import com.ohinteractive.seedv6.core.brnnext.*;
import static com.ohinteractive.seedv6.core.brnnext.BrnNextArchitecture.*;

/** Research-only immutable function variants. Never write back to a checkpoint or optimizer. */
final class BrnSuccessorVariants {
    static BrnNextModel gain(BrnNextModel source,double gain) {
        if(!Double.isFinite(gain)||gain<0||gain>1)throw new IllegalArgumentException("Residual gain must be 0..1");
        if(gain==BrnNextModel.RESIDUAL_GAIN)return source;
        var kind=source.architecture();double[] weights=new double[kind.parameters()];
        for(int i=0;i<weights.length;i++)weights[i]=source.weight(i);
        double factor=gain/BrnNextModel.RESIDUAL_GAIN;
        for(int i=0;i<EMBEDDING;i++)weights[i]*=factor;
        int first=kind==CUBIC?kind.head():kind.head()+kind.width();
        for(int i=first;i<weights.length;i++)weights[i]*=factor;
        return new BrnNextModel(kind,weights);
    }
    private BrnSuccessorVariants() {}
}
