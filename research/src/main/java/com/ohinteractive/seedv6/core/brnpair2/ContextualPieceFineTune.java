package com.ohinteractive.seedv6.core.brnpair2;

/** R031 update-amplitude intervention with parent shrink and optimizer history fixed. */
final class ContextualPieceFineTune {
    static ContextualPieceNetwork fork(ContextualPieceNetwork parent,double rate) {
        if(parent.compose||parent.rate!=.01||(rate!=.01&&rate!=.001))throw new IllegalArgumentException("Fine-tune parent/rate");
        var result=new ContextualPieceNetwork(false,rate,0);result.step=parent.step;
        System.arraycopy(parent.weights,0,result.weights,0,parent.weights.length);
        System.arraycopy(parent.first,0,result.first,0,parent.first.length);
        System.arraycopy(parent.second,0,result.second,0,parent.second.length);return result;
    }
    static double train(ContextualPieceNetwork m,ContextualPieceNetwork.Features[] batch,double[] targets) {
        if(m.compose||(m.rate!=.01&&m.rate!=.001))throw new IllegalArgumentException("Fine-tune model/rate");
        double loss=m.gradients(batch,targets);if(m.step==Long.MAX_VALUE)throw new ArithmeticException("Step");m.step++;
        double c1=1-Math.pow(.9,m.step),c2=1-Math.pow(.999,m.step);
        for(int i=0;i<m.weights.length;i++)m.weights[i]*=1-.01*.01;
        for(int n=0;n<m.count;n++) {
            int i=m.touched[n];double g=m.gradient[i];m.first[i]=.9*m.first[i]+.1*g;m.second[i]=.999*m.second[i]+.001*g*g;
            m.weights[i]-=m.rate*(m.first[i]/c1)/(Math.sqrt(m.second[i]/c2)+1e-8);
            if(!Double.isFinite(m.weights[i]))throw new ArithmeticException("Nonfinite fine-tune parameter");
        }
        for(int i=0;i<m.weights.length;i++)m.weights[i]=Math.max(-.99,Math.min(.99,m.weights[i]));return loss;
    }
    private ContextualPieceFineTune(){}
}
