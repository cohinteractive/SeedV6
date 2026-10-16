package com.ohinteractive.seedv6.core.brn3;

import com.ohinteractive.seedv6.core.brn.BrnAdamConfig;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class Brn3VectorKernelsTest {
    @Test void adamMatchesScalarBitsForOddTailsPartialBatchesAndNonDefaultMoments() {
        var random=new Random(3197);
        for(int count:new int[]{1,3,8,19})for(int batch:new int[]{1,3,128}) {
            var config=new BrnAdamConfig(.002,.83,.977,1e-9);
            double[] weights=new double[32],first=new double[32],second=new double[32],gradient=new double[32];
            for(int i=0;i<32;i++) {
                weights[i]=random.nextDouble()-.5;first[i]=(random.nextDouble()-.5)*.1;
                second[i]=random.nextDouble()*.01;gradient[i]=random.nextDouble()-.5;
            }
            gradient[3]=-0.0;gradient[4]=Double.MIN_VALUE;gradient[5]=-Double.MIN_VALUE;
            double[] w=weights.clone(),m=first.clone(),v=second.clone();
            double c1=1-Math.pow(config.beta1(),17),c2=1-Math.pow(config.beta2(),17);
            double a=1-config.beta1(),b=1-config.beta2();
            for(int i=3;i<3+count;i++) {
                double g=gradient[i]/batch;
                m[i]=config.beta1()*m[i]+a*g;v[i]=config.beta2()*v[i]+b*g*g;
                w[i]-=config.learningRate()*(m[i]/c1)/(Math.sqrt(v[i]/c2)+config.epsilon());
            }
            Brn3VectorKernels.update(weights,first,second,gradient,3,3+count,batch,c1,c2,a,b,config);
            assertArrayEquals(w,weights);assertArrayEquals(m,first);assertArrayEquals(v,second);
        }
    }

    @Test void relationLanesPreserveScalarAdditionOrderAtUnalignedOffsets() {
        var random=new Random(97);double[] weights=new double[64],gradient=new double[64];
        double[] left=new double[8],right=new double[8];
        for(int i=0;i<64;i++){weights[i]=random.nextDouble()-.5;gradient[i]=random.nextDouble()-.5;}
        for(int i=0;i<8;i++){left[i]=random.nextDouble()-.5;right[i]=random.nextDouble()-.5;}
        double[] a=left.clone(),b=right.clone(),g=gradient.clone();double norm=1/Math.sqrt(17);
        for(int c=0;c<8;c++){double value=norm*(weights[1+c]+weights[37+c]);a[c]+=value;b[c]+=value;}
        Brn3VectorKernels.forward(weights,1,37,norm,left,right);assertArrayEquals(a,left);assertArrayEquals(b,right);
        for(int c=0;c<8;c++){double value=norm*(left[c]+right[c]);g[1+c]+=value;g[37+c]+=value;}
        Brn3VectorKernels.backward(gradient,1,37,norm,left,right);assertArrayEquals(g,gradient);
    }
}
