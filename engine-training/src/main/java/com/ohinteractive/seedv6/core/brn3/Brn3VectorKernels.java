package com.ohinteractive.seedv6.core.brn3;

import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/** Optional Java 21 SIMD implementation. No horizontal sums or fused operations:
 * each lane retains the scalar trainer's binary64 operation order. */
final class Brn3VectorKernels {
    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;

    static void hidden(double[] weights,double[] transposed,double[][] pooled,double[] hidden) {
        System.arraycopy(weights,Brn3Layout.BIAS,hidden,0,Brn3Layout.HIDDEN_WIDTH);
        for(int p=0;p<2;p++)for(int c=0;c<Brn3Layout.POOL_WIDTH;c++) {
            int base=(p*Brn3Layout.POOL_WIDTH+c)*Brn3Layout.HIDDEN_WIDTH,h=0;
            for(;h<SPECIES.loopBound(Brn3Layout.HIDDEN_WIDTH);h+=SPECIES.length()) {
                DoubleVector.fromArray(SPECIES,hidden,h).add(DoubleVector.fromArray(SPECIES,transposed,base+h)
                        .mul(pooled[p][c])).intoArray(hidden,h);
            }
            for(;h<Brn3Layout.HIDDEN_WIDTH;h++)hidden[h]+=transposed[base+h]*pooled[p][c];
        }
        for(int h=0;h<Brn3Layout.HIDDEN_WIDTH;h++)hidden[h]=Math.max(0,hidden[h]);
    }

    static void forward(double[] weights,int row,int shared,double norm,double[] left,double[] right) {
        int c=0;
        for(;c<SPECIES.loopBound(Brn3Layout.WIDTH);c+=SPECIES.length()) {
            var message=DoubleVector.fromArray(SPECIES,weights,row+c)
                    .add(DoubleVector.fromArray(SPECIES,weights,shared+c)).mul(norm);
            DoubleVector.fromArray(SPECIES,left,c).add(message).intoArray(left,c);
            DoubleVector.fromArray(SPECIES,right,c).add(message).intoArray(right,c);
        }
        for(;c<Brn3Layout.WIDTH;c++) {
            double message=norm*(weights[row+c]+weights[shared+c]);left[c]+=message;right[c]+=message;
        }
    }

    static void backward(double[] gradient,int row,int shared,double norm,double[] left,double[] right) {
        int c=0;
        for(;c<SPECIES.loopBound(Brn3Layout.WIDTH);c+=SPECIES.length()) {
            var g=DoubleVector.fromArray(SPECIES,left,c).add(DoubleVector.fromArray(SPECIES,right,c)).mul(norm);
            DoubleVector.fromArray(SPECIES,gradient,row+c).add(g).intoArray(gradient,row+c);
            DoubleVector.fromArray(SPECIES,gradient,shared+c).add(g).intoArray(gradient,shared+c);
        }
        for(;c<Brn3Layout.WIDTH;c++) {
            double g=norm*(left[c]+right[c]);gradient[row+c]+=g;gradient[shared+c]+=g;
        }
    }

    static void update(double[] weights,double[] first,double[] second,double[] gradient,
            int start,int end,int batch,double c1,double c2,double complement1,double complement2,BrnAdamConfig config) {
        int i=start;
        for(;i<=end-SPECIES.length();i+=SPECIES.length()) {
            var g=DoubleVector.fromArray(SPECIES,gradient,i).div(batch);
            var m=DoubleVector.fromArray(SPECIES,first,i).mul(config.beta1()).add(g.mul(complement1));
            var v=DoubleVector.fromArray(SPECIES,second,i).mul(config.beta2()).add(g.mul(complement2).mul(g));
            var w=DoubleVector.fromArray(SPECIES,weights,i).sub(m.div(c1).mul(config.learningRate())
                    .div(v.div(c2).lanewise(VectorOperators.SQRT).add(config.epsilon())));
            if(!w.test(VectorOperators.IS_FINITE).allTrue())throw new ArithmeticException("Nonfinite BRN-3 optimizer update");
            m.intoArray(first,i);v.intoArray(second,i);w.intoArray(weights,i);
        }
        for(;i<end;i++) {
            double g=gradient[i]/batch;
            first[i]=config.beta1()*first[i]+complement1*g;
            second[i]=config.beta2()*second[i]+complement2*g*g;
            weights[i]-=config.learningRate()*(first[i]/c1)/(Math.sqrt(second[i]/c2)+config.epsilon());
            if(!Double.isFinite(weights[i]))throw new ArithmeticException("Nonfinite BRN-3 optimizer update");
        }
    }
    private Brn3VectorKernels() {}
}
