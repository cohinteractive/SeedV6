package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import java.io.*;
import java.util.*;

/** Research-only two-pass learned relational messages through both king anchors. */
public final class AnchorNeuralNetwork {
    static final int WIDTH=8,E=769,R1=E+768*WIDTH,R2=R1+450*WIDTH,O=R2+450*WIDTH,PARAMETERS=O+768*WIDTH;
    final boolean nonlinear;
    final double rate;
    final double[] weights=new double[PARAMETERS],first=new double[PARAMETERS],second=new double[PARAMETERS],gradient=new double[PARAMETERS];
    final int[] stamps=new int[PARAMETERS],touched=new int[PARAMETERS];
    int serial,count;
    long step;
    public AnchorNeuralNetwork(boolean nonlinear,double rate,long seed) {
        if(!Double.isFinite(rate)||rate<=0)throw new IllegalArgumentException("Rate");this.nonlinear=nonlinear;this.rate=rate;
        var random=new Random(seed);for(int i=E;i<weights.length;i++)weights[i]=.1*random.nextGaussian();
    }
    static final class Features {
        int size;
        int[] nodes=new int[32],relations=new int[64];
        double material,norm;
        Features frozen(){var f=new Features();f.size=size;f.nodes=Arrays.copyOf(nodes,size);f.relations=Arrays.copyOf(relations,2*size);f.material=material;f.norm=norm;return f;}
    }
    public static final class Workspace {
        final Features features=new Features();
        final int[] squares=new int[32],kings=new int[2];
        final double[] a=new double[16],h=new double[16],z=new double[256],anchorGradient=new double[16];
        Features encode(long[] board) {
            var f=features;f.size=0;Arrays.fill(kings,-1);int stm=Board.player((int)board[4]);
            for(int s=0;s<64;s++) {
                int piece=Board.getSquare(board[0],board[1],board[2],board[3],s);if(piece==0)continue;
                if(f.size>=32)throw new IllegalArgumentException("Too many pieces");int side=(piece>>>3)^stm,type=piece&7;
                if(type<1||type>6)throw new IllegalArgumentException("Piece identity");
                if(type==1){if(kings[side]>=0)throw new IllegalArgumentException("Duplicate king");kings[side]=f.size;}
                squares[f.size]=s^(stm*56);f.nodes[f.size++]=(type-1+6*side)*64+(s^(stm*56));
            }
            if(kings[0]<0||kings[1]<0)throw new IllegalArgumentException("Missing king");
            for(int i=0;i<f.size;i++)for(int k=0;k<2;k++) {
                int a=squares[i],b=squares[kings[k]];
                f.relations[2*i+k]=i==kings[k]?-1:k*225+(b/8-a/8+7)*15+b%8-a%8+7;
            }
            f.material=BrnPair2Features.material(board)/4;f.norm=1/Math.sqrt(f.size-1);return f;
        }
    }
    double activate(double x){return nonlinear?x/(1+Math.abs(x)):x;}
    double derivative(double x){double scale=1+Math.abs(x);return nonlinear?1/(scale*scale):1;}
    double unary(Features f){double sum=f.material+weights[768];for(int i=0;i<f.size;i++)sum+=weights[f.nodes[i]];return sum;}
    double logit(Features f,Workspace w) {
        Arrays.fill(w.a,0);
        for(int i=0;i<f.size;i++)for(int k=0;k<2;k++) {
            int relation=f.relations[2*i+k];if(relation<0)continue;
            for(int d=0;d<WIDTH;d++)w.a[k*WIDTH+d]+=f.norm*weights[E+f.nodes[i]*WIDTH+d]*weights[R1+relation*WIDTH+d];
        }
        for(int i=0;i<w.a.length;i++)w.h[i]=activate(w.a[i]);double sum=unary(f);
        for(int i=0;i<f.size;i++)for(int d=0;d<WIDTH;d++) {
            double z=0;for(int k=0;k<2;k++){int relation=f.relations[2*i+k];if(relation>=0)z+=weights[R2+relation*WIDTH+d]*w.h[k*WIDTH+d];}
            w.z[i*WIDTH+d]=z;sum+=weights[O+f.nodes[i]*WIDTH+d]*activate(z)/8;
        }
        if(!Double.isFinite(sum))throw new ArithmeticException("Nonfinite anchor score");return sum;
    }
    public double scoreCp(long[] board,Workspace w){return 400*logit(w.encode(board),w);}
    void add(int i,double g){if(stamps[i]!=serial){stamps[i]=serial;touched[count++]=i;gradient[i]=0;}gradient[i]+=g;}
    double gradients(Features[] batch,double[] targets) {
        if(batch.length==0||batch.length!=targets.length)throw new IllegalArgumentException("Batch");
        if(++serial==0){Arrays.fill(stamps,0);serial=1;}count=0;double loss=0;var w=new Workspace();
        for(int n=0;n<batch.length;n++) {
            if(!Double.isFinite(targets[n])||targets[n]<0||targets[n]>1)throw new IllegalArgumentException("Target");
            var f=batch[n];double p=PairObjectiveDiagnostic.sigmoid(logit(f,w)),error=p-targets[n];loss+=error*error;
            double g=2*error*p*(1-p)/batch.length;add(768,g);for(int i=0;i<f.size;i++)add(f.nodes[i],g);
            Arrays.fill(w.anchorGradient,0);
            for(int i=0;i<f.size;i++)for(int d=0;d<WIDTH;d++) {
                int output=O+f.nodes[i]*WIDTH+d;double z=w.z[i*WIDTH+d];add(output,g*activate(z)/8);
                double dz=g*weights[output]*derivative(z)/8;
                for(int k=0;k<2;k++) {
                    int relation=f.relations[2*i+k];if(relation<0)continue;int r=R2+relation*WIDTH+d,h=k*WIDTH+d;
                    add(r,dz*w.h[h]);w.anchorGradient[h]+=dz*weights[r];
                }
            }
            for(int h=0;h<w.anchorGradient.length;h++)w.anchorGradient[h]*=derivative(w.a[h])*f.norm;
            for(int i=0;i<f.size;i++)for(int k=0;k<2;k++) {
                int relation=f.relations[2*i+k];if(relation<0)continue;
                for(int d=0;d<WIDTH;d++) {
                    int e=E+f.nodes[i]*WIDTH+d,r=R1+relation*WIDTH+d;double da=w.anchorGradient[k*WIDTH+d];
                    add(e,da*weights[r]);add(r,da*weights[e]);
                }
            }
        }
        return loss/batch.length;
    }
    double train(Features[] batch,double[] targets) {
        double loss=gradients(batch,targets);if(step==Long.MAX_VALUE)throw new ArithmeticException("Step");step++;
        double c1=1-Math.pow(.9,step),c2=1-Math.pow(.999,step);
        for(int i=0;i<weights.length;i++)weights[i]*=1-rate*.01;
        for(int n=0;n<count;n++) {
            int i=touched[n];double g=gradient[i];first[i]=.9*first[i]+.1*g;second[i]=.999*second[i]+.001*g*g;
            weights[i]-=rate*(first[i]/c1)/(Math.sqrt(second[i]/c2)+1e-8);
            if(!Double.isFinite(weights[i]))throw new ArithmeticException("Nonfinite anchor parameter");
        }
        for(int i=0;i<weights.length;i++)weights[i]=Math.max(-.99,Math.min(.99,weights[i]));return loss;
    }
    void write(OutputStream stream)throws IOException {
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);out.writeLong(0x42524e414e433031L);out.writeInt(nonlinear?1:0);out.writeInt(PARAMETERS);out.writeDouble(rate);out.writeLong(step);
        for(var array:new double[][]{weights,first,second})for(double value:array)out.writeDouble(value);
        out.flush();var crc=new java.util.zip.CRC32();crc.update(bytes.toByteArray());out.writeLong(crc.getValue());stream.write(bytes.toByteArray());
    }
    static AnchorNeuralNetwork read(InputStream stream)throws IOException {
        byte[] bytes=stream.readNBytes(41+24*PARAMETERS);if(bytes.length!=40+24*PARAMETERS)throw new IOException("Anchor state length");
        var crc=new java.util.zip.CRC32();crc.update(bytes,0,bytes.length-8);var in=new DataInputStream(new ByteArrayInputStream(bytes));
        if(in.readLong()!=0x42524e414e433031L)throw new IOException("Anchor magic");int mode=in.readInt();if(mode<0||mode>1||in.readInt()!=PARAMETERS)throw new IOException("Anchor geometry");
        double rate=in.readDouble();long step=in.readLong();if(!Double.isFinite(rate)||rate<=0||step<0)throw new IOException("Anchor optimizer");
        var model=new AnchorNeuralNetwork(mode==1,rate,0);model.step=step;
        for(var array:new double[][]{model.weights,model.first,model.second})for(int i=0;i<array.length;i++) {
            array[i]=in.readDouble();if(!Double.isFinite(array[i])||(array==model.second&&array[i]<0))throw new IOException("Anchor nonfinite/moment");
        }
        if(in.readLong()!=crc.getValue())throw new IOException("Anchor CRC");return model;
    }
}
