package com.ohinteractive.seedv6.tools.eval;

import com.ohinteractive.seedv6.core.brnnext.*;
import java.util.Arrays;
import static com.ohinteractive.seedv6.core.brnnext.BrnNextArchitecture.*;

/** Research-only immutable function variants. Never write back to a checkpoint or optimizer. */
final class BrnSuccessorVariants {
    /** Preserve the exact frozen Gen32 function while changing only its learned correction. */
    static BrnNextModel correction(BrnNextModel source,double scale,String ablation,double[] meanGates) {
        if(!source.architecture().frozenBase()||!Double.isFinite(scale)||scale<0||scale>2)
            throw new IllegalArgumentException("Frozen-base correction scale must be 0..2");
        if(meanGates!=null&&!"constant-gates".equals(ablation))
            throw new IllegalArgumentException("Mean gates require the constant-gates ablation");
        var kind=source.architecture();double[] w=new double[kind.parameters()];
        for(int i=0;i<w.length;i++)w[i]=source.weight(i);
        if(ablation!=null) switch(ablation) {
            case "unary" -> Arrays.fill(w,EMBEDDING,w.length,0);
            case "quadratic" -> {
                if(kind.correctionKind()!=CUBIC)throw new IllegalArgumentException("Cubic correction required");
                Arrays.fill(w,kind.head()+kind.width(),w.length,0);
            }
            case "constant-gates" -> {
                if(kind.correctionKind()!=GATED_PAIRS||meanGates==null||meanGates.length!=kind.width())
                    throw new IllegalArgumentException("Gated correction and explicit mean weights required");
                double sum=0;
                for(double g:meanGates) {
                    if(!Double.isFinite(g)||g<=0)throw new IllegalArgumentException("Positive finite mean gates required");
                    sum+=g;
                }
                if(Math.abs(sum-1)>1e-12)throw new IllegalArgumentException("Mean gates must sum to one");
                Arrays.fill(w,EMBEDDING,kind.head(),0);
                for(int r=0;r<meanGates.length;r++)w[kind.head()+r]=Math.log(meanGates[r]);
            }
            default -> throw new IllegalArgumentException("Unknown correction ablation");
        }
        for(int i=0;i<EMBEDDING;i++)w[i]*=scale;
        for(int i=kind.head()+(kind.correctionKind()==CUBIC?0:kind.width());i<w.length;i++)w[i]*=scale;
        return new BrnNextModel(kind,w);
    }
    static BrnNextModel gain(BrnNextModel source,double gain) {
        if(!Double.isFinite(gain)||gain<0||gain>1)throw new IllegalArgumentException("Residual gain must be 0..1");
        if(gain==BrnNextModel.RESIDUAL_GAIN)return source;
        if(source.architecture().frozenBase())throw new IllegalArgumentException("W1 gain override cannot rescale a frozen Gen32 component");
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
