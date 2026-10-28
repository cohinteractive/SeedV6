package com.ohinteractive.seedv6.core.brnpair2;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NeuralScaleCalibrationTest {
    @Test void recoversKnownSyntheticScaleAndIdentityWithoutMutatingInputs() {
        double[] x=new double[101],y=new double[101];for(int i=0;i<x.length;i++){x[i]=(i-50)/17.0;y[i]=1/(1+Math.exp(-1.37*x[i]));}var saved=x.clone();var targets=y.clone();
        var fit=NeuralScaleCalibration.fit(x,y);assertEquals(1.37,fit.scale(),1e-6);assertTrue(fit.loss()<1e-14);assertEquals(57,fit.evaluations());assertFalse(fit.boundary());assertArrayEquals(saved,x);assertArrayEquals(targets,y);
        for(int i=0;i<x.length;i++)y[i]=1/(1+Math.exp(-x[i]));assertEquals(1,NeuralScaleCalibration.fit(x,y).scale());
    }
    @Test void retainsBoundariesAndAgreesWithIndependentDenseScan() {
        double[] x={-3,-1,-.2,.4,1,2},y=new double[x.length];for(int i=0;i<x.length;i++)y[i]=1/(1+Math.exp(-3*x[i]));var fit=NeuralScaleCalibration.fit(x,y);assertEquals(2,fit.scale());assertTrue(fit.boundary());
        y=new double[]{.01,.21,.35,.7,.76,.94};fit=NeuralScaleCalibration.fit(x,y);double best=Double.POSITIVE_INFINITY;
        for(int j=0;j<=3000;j++){double s=.5+j*.0005,sum=0;for(int i=0;i<x.length;i++){double e=1/(1+Math.exp(-s*x[i]))-y[i];sum+=e*e;}best=Math.min(best,sum/x.length);}
        assertTrue(fit.loss()<=best+1e-12);
    }
    @Test void rejectsMalformedInputsAndOutOfRangeScales() {
        assertThrows(IllegalArgumentException.class,()->NeuralScaleCalibration.fit(new double[0],new double[0]));assertThrows(IllegalArgumentException.class,()->NeuralScaleCalibration.fit(new double[]{0},new double[0]));
        assertThrows(IllegalArgumentException.class,()->NeuralScaleCalibration.fit(new double[]{Double.NaN},new double[]{.5}));assertThrows(IllegalArgumentException.class,()->NeuralScaleCalibration.fit(new double[]{1},new double[]{1.1}));
        for(double scale:new double[]{0,2.1,Double.NaN})assertThrows(IllegalArgumentException.class,()->NeuralScaleCalibration.mse(new double[]{0},new double[]{.5},scale));
    }
}
