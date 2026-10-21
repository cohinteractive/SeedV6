package com.ohinteractive.seedv6.tools.eval;

import com.ohinteractive.seedv6.core.brnnext.*;
import com.ohinteractive.seedv6.core.brnpair2.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;
import com.ohinteractive.seedv6.training.model.NetworkModel;
import static com.ohinteractive.seedv6.core.brnnext.BrnNextArchitecture.*;

/** Research-only immutable function variants. Never write back to a checkpoint or optimizer. */
final class BrnSuccessorVariants {
    /** Uniform adjacent-checkpoint parameter average for a diagnostic, never an Adam continuation.
     * Pair2 averages compiled additive tables, hence its raw function is also the uniform mean.
     * Nonlinear parameters share their original lineage basis; function equality is not assumed. */
    static NetworkModel average(List<NetworkModel> sources) throws IOException {
        if(sources.isEmpty()||sources.size()>32)throw new IllegalArgumentException("Average needs1..32 models");
        var first=sources.getFirst();
        if(sources.stream().anyMatch(m->m.architecture()!=first.architecture()))
            throw new IllegalArgumentException("Average architecture mismatch");
        if(sources.size()==1)return first;
        double scale=1.0/sources.size();
        if(first instanceof NetworkModel.BrnNext n) {
            if(!n.model().architecture().frozenBase())throw new IllegalArgumentException("Frozen-base residuals only");
            double[] weights=new double[n.model().architecture().parameters()];
            for(var source:sources) {
                var model=((NetworkModel.BrnNext)source).model();
                for(int i=0;i<weights.length;i++)weights[i]+=scale*model.weight(i);
            }
            return new NetworkModel.BrnNext(new BrnNextModel(n.model().architecture(),weights));
        }
        if(first instanceof NetworkModel.BrnPair2 p) {
            var encoded=new ByteArrayOutputStream();BrnPair2Codec.writeModel(p.model(),encoded);
            byte[] bytes=encoded.toByteArray();var buffer=ByteBuffer.wrap(bytes);
            buffer.position(BrnPair2Codec.MODEL_BYTES-4-8*(BrnPair2Codec.TABLE_VALUES+1));
            double bias=0;
            for(var source:sources)bias+=scale*((NetworkModel.BrnPair2)source).model().bias();
            buffer.putDouble(bias);
            for(int i=0;i<BrnPair2Codec.TABLE_VALUES;i++) {
                double weight=0;
                for(var source:sources)weight+=scale*((NetworkModel.BrnPair2)source).model().weight(i);
                buffer.putDouble(weight);
            }
            var crc=new CRC32();crc.update(bytes,0,bytes.length-4);buffer.putInt((int)crc.getValue());
            return new NetworkModel.BrnPair2(BrnPair2Codec.readModel(new ByteArrayInputStream(bytes)));
        }
        throw new IllegalArgumentException("Unsupported diagnostic average");
    }
    /** Fold an additive change into one ordinary Pair2 table, retaining its ordinary inference cost.
     * The public codec validates the generated in-memory payload. No store or optimizer is written. */
    static BrnPair2Model additiveChange(BrnPair2Model source,double scale) throws IOException {
        if(!Double.isFinite(scale)||scale<0||scale>1)throw new IllegalArgumentException("Additive change scale must be 0..1");
        var reference=BrnNextBase.model();
        if(scale==0)return reference;
        if(scale==1)return source;
        var encoded=new ByteArrayOutputStream();BrnPair2Codec.writeModel(reference,encoded);
        byte[] bytes=encoded.toByteArray();
        int header=BrnPair2Codec.MODEL_BYTES-4-8*(BrnPair2Codec.TABLE_VALUES+1);
        var buffer=ByteBuffer.wrap(bytes);buffer.position(header);
        buffer.putDouble(reference.bias()+scale*(source.bias()-reference.bias()));
        for(int i=0;i<BrnPair2Codec.TABLE_VALUES;i++)
            buffer.putDouble(reference.weight(i)+scale*(source.weight(i)-reference.weight(i)));
        var crc=new CRC32();crc.update(bytes,0,bytes.length-4);buffer.putInt((int)crc.getValue());
        return BrnPair2Codec.readModel(new ByteArrayInputStream(bytes));
    }
    /** Preserve the exact frozen Gen32 function while changing only its learned correction. */
    static BrnNextModel correction(BrnNextModel source,double scale,String ablation,double[] meanGates) {
        if(!source.architecture().frozenBase()||!Double.isFinite(scale)||scale<0||scale>2)
            throw new IllegalArgumentException("Frozen-base correction scale must be 0..2");
        if(meanGates!=null&&!"constant-gates".equals(ablation))
            throw new IllegalArgumentException("Mean gates require the constant-gates ablation");
        var kind=source.architecture();double[] w=new double[kind.parameters()];
        if(kind.calibratedHeads() && (scale!=1 || ablation!=null))
            throw new IllegalArgumentException("Calibrated heads own their scale; legacy ablations are incompatible");
        for(int i=0;i<w.length;i++)w[i]=source.weight(i);
        if(kind.calibratedHeads())return source;
        if(ablation!=null) switch(ablation) {
            case "unary" -> Arrays.fill(w,EMBEDDING,w.length,0);
            case "quadratic" -> {
                if(kind.correctionKind()!=CUBIC)throw new IllegalArgumentException("Cubic correction required");
                Arrays.fill(w,kind.head()+kind.width(),w.length,0);
            }
            case "cubic-only" -> {
                if(kind.correctionKind()!=CUBIC)throw new IllegalArgumentException("Cubic correction required");
                Arrays.fill(w,kind.head(),kind.head()+kind.width(),0);
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
        for(int i=kind.head()+(kind.polynomial()?0:kind.width());i<w.length;i++)w[i]*=scale;
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
