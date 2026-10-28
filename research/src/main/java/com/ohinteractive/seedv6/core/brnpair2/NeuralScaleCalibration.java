package com.ohinteractive.seedv6.core.brnpair2;

/** One bounded positive scalar fitted against training probabilities; no neural weight mutation. */
final class NeuralScaleCalibration {
    record Fit(double scale,double loss,int evaluations,boolean boundary){}
    private static void validate(double[] logits,double[] targets) {
        if(logits.length==0||logits.length!=targets.length)throw new IllegalArgumentException("Calibration shape");
        for(int i=0;i<logits.length;i++)if(!Double.isFinite(logits[i])||!Double.isFinite(targets[i])||targets[i]<0||targets[i]>1)throw new IllegalArgumentException("Calibration data");
    }
    static double mse(double[] logits,double[] targets,double scale) {
        validate(logits,targets);if(!Double.isFinite(scale)||scale<.5||scale>2)throw new IllegalArgumentException("Calibration scale");return loss(logits,targets,scale);
    }
    private static double loss(double[] logits,double[] targets,double scale) {
        double sum=0;for(int i=0;i<logits.length;i++){double error=PairObjectiveDiagnostic.sigmoid(scale*logits[i])-targets[i];sum+=error*error;}return sum/logits.length;
    }
    static Fit fit(double[] logits,double[] targets) {
        validate(logits,targets);double bestScale=.5,bestLoss=Double.POSITIVE_INFINITY;int bestIndex=0,evaluations=0;
        for(int i=0;i<=30;i++) {
            double scale=.5+.05*i,value=loss(logits,targets,scale);evaluations++;
            if(value<bestLoss){bestScale=scale;bestLoss=value;bestIndex=i;}
        }
        double left=.5+.05*Math.max(0,bestIndex-1),right=.5+.05*Math.min(30,bestIndex+1),ratio=(Math.sqrt(5)-1)/2;
        double x=right-ratio*(right-left),y=left+ratio*(right-left),fx=loss(logits,targets,x),fy=loss(logits,targets,y);evaluations+=2;
        for(int iteration=0;iteration<24;iteration++) {
            if(fx<bestLoss){bestScale=x;bestLoss=fx;}if(fy<bestLoss){bestScale=y;bestLoss=fy;}
            if(fx<=fy){right=y;y=x;fy=fx;x=right-ratio*(right-left);fx=loss(logits,targets,x);}
            else{left=x;x=y;fx=fy;y=left+ratio*(right-left);fy=loss(logits,targets,y);}evaluations++;
        }
        if(fx<bestLoss){bestScale=x;bestLoss=fx;}if(fy<bestLoss){bestScale=y;bestLoss=fy;}
        return new Fit(bestScale,bestLoss,evaluations,bestScale==.5||bestScale==2);
    }
    private NeuralScaleCalibration(){}
}
