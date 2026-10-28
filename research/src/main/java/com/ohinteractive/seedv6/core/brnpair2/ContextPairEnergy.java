package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.conventional.ConventionalFeatures;
import java.io.*;
import java.util.*;
import static com.ohinteractive.seedv6.core.brnpair2.BrnPair2Geometry.*;

/** Research-only scalar pair energies conditioned on a third entity (the king).
 * No NNUE network, hidden layer or neural accumulator is present. */
final class ContextPairEnergy {
    final int buckets,bias;
    final double rate;
    final String baseSha;
    final BrnPair2Model base;
    final double[] weights,first,second,gradient;
    final int[] touched,stamps;
    int serial,count;
    long step;
    final class Workspace {
        final int[][] codes=new int[2][64];
        final int[] kings=new int[2],active=new int[2*TEMPLATES];
        final BrnPair2Model.Workspace baseline=base.newWorkspace();
        void encode(long[] board) {
            Arrays.fill(kings,-1);
            for(int square=0;square<64;square++) {
                int piece=Board.getSquare(board[0],board[1],board[2],board[3],square);
                if((piece&7)==1) {
                    int side=piece>>>3;if(kings[side]>=0)throw new IllegalArgumentException("Duplicate king");kings[side]=square;
                }
                for(int side=0;side<2;side++)codes[side][square^(side*56)]=category(piece,side);
            }
            if(kings[0]<0||kings[1]<0)throw new IllegalArgumentException("Missing king");
            int stm=Board.player((int)board[4]);
            for(int role=0;role<2;role++) {
                int side=stm^role,bucket=buckets==1?0:ConventionalFeatures.context(side,kings[side])>>>1;
                for(int t=0;t<TEMPLATES;t++)active[role*TEMPLATES+t]=((role*buckets+bucket)*TEMPLATES+t)*169+state(codes[side],t,2);
            }
        }
    }
    ContextPairEnergy(boolean king,double rate,BrnPair2Model base,String baseSha) {
        if(!Double.isFinite(rate)||rate<=0||!baseSha.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("Optimizer/base identity");
        buckets=king?10:1;bias=2*buckets*TEMPLATES*169;this.rate=rate;this.base=Objects.requireNonNull(base);this.baseSha=baseSha;
        weights=new double[bias+1];first=new double[weights.length];second=new double[weights.length];gradient=new double[weights.length];
        touched=new int[weights.length];stamps=new int[weights.length];
    }
    double logit(long[] board,Workspace workspace) {
        workspace.encode(board);double sum=0;for(int i:workspace.active)sum+=weights[i];
        double value=workspace.baseline.evaluatePawns(board,.25)/4+(weights[bias]+NORM*sum)/16;
        if(!Double.isFinite(value))throw new ArithmeticException("Nonfinite context energy");return value;
    }
    void add(int i,double g){if(stamps[i]!=serial){stamps[i]=serial;gradient[i]=0;touched[count++]=i;}gradient[i]+=g;}
    double gradients(long[][] boards,double[] targets) {
        if(boards.length==0||targets.length!=boards.length)throw new IllegalArgumentException("Batch");
        if(++serial==0){Arrays.fill(stamps,0);serial=1;}count=0;double loss=0;var workspace=new Workspace();
        for(int n=0;n<boards.length;n++) {
            if(!Double.isFinite(targets[n])||targets[n]<0||targets[n]>1)throw new IllegalArgumentException("Target");
            double p=PairObjectiveDiagnostic.sigmoid(logit(boards[n],workspace)),d=p-targets[n];loss+=d*d;
            double derivative=2*d*p*(1-p)/(16*boards.length);add(bias,derivative);
            for(int i:workspace.active)add(i,NORM*derivative);
        }
        return loss/boards.length;
    }
    double train(long[][] boards,double[] targets) {
        double loss=gradients(boards,targets);if(step==Long.MAX_VALUE)throw new ArithmeticException("Step");step++;
        double c1=1-Math.pow(.9,step),c2=1-Math.pow(.999,step);
        for(int n=0;n<count;n++) {
            int i=touched[n];double g=gradient[i];first[i]=.9*first[i]+.1*g;second[i]=.999*second[i]+.001*g*g;
            weights[i]-=rate*(first[i]/c1)/(Math.sqrt(second[i]/c2)+1e-8);
            if(!Double.isFinite(weights[i]))throw new ArithmeticException("Nonfinite context parameter");
        }
        return loss;
    }
    void write(OutputStream stream)throws IOException {
        var crc=new java.util.zip.CRC32();var checked=new java.util.zip.CheckedOutputStream(stream,crc);var out=new DataOutputStream(checked);
        out.writeLong(0x42524e4354583031L);out.writeInt(buckets);out.writeInt(weights.length);out.writeDouble(rate);out.writeLong(step);out.writeUTF(baseSha);
        for(var array:new double[][]{weights,first,second})for(double value:array)out.writeDouble(value);
        out.flush();long checksum=crc.getValue();new DataOutputStream(stream).writeLong(checksum);
    }
    static ContextPairEnergy read(InputStream stream,BrnPair2Model base,String expectedSha)throws IOException {
        stream=new BufferedInputStream(stream);
        var crc=new java.util.zip.CRC32();var checked=new java.util.zip.CheckedInputStream(stream,crc);var in=new DataInputStream(checked);
        if(in.readLong()!=0x42524e4354583031L)throw new IOException("Context magic");int buckets=in.readInt(),length=in.readInt();
        if((buckets!=1&&buckets!=10)||length!=2*buckets*TEMPLATES*169+1)throw new IOException("Context geometry");
        double rate=in.readDouble();long step=in.readLong();String sha=in.readUTF();
        if(!sha.equals(expectedSha)||step<0||!Double.isFinite(rate)||rate<=0)throw new IOException("Context identity/optimizer");
        var model=new ContextPairEnergy(buckets==10,rate,base,sha);model.step=step;
        for(var array:new double[][]{model.weights,model.first,model.second})for(int i=0;i<length;i++) {
            array[i]=in.readDouble();if(!Double.isFinite(array[i])||(array==model.second&&array[i]<0))throw new IOException("Context numerical state");
        }
        long checksum=crc.getValue();if(new DataInputStream(stream).readLong()!=checksum||stream.read()!=-1)throw new IOException("Context checksum/length");return model;
    }
}
