package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import java.io.*;
import java.util.*;
import java.util.zip.CRC32;

/** Experimental BRN family, never a production BRN-3/4 payload. All variants keep
 * nonlinear piece-local states and typed pooling. Default matches the R4 oracle. */
final class BrnSuccessorCandidate {
    enum Relations { ABSOLUTE, RELATIVE, HASH, FACTORIZED, DIRECTED_RELATIVE }
    enum Pool { SUM, SUM_MAX, SUM_SQUARE, SUM_STATE, SUM_CONTEXT, SUM_SELF }
    final Relations relations;final Pool pool;
    final int width,poolWidth,nodes,dense,bias,head,output,relative,context,parameters;
    final double[] weights,first,second,gradient;
    final int[] touched,stamps;int serial,size,count;long updates;
    final int[][] entities=new int[2][64];
    final double[][][] local,localGradient;
    final double[][][] beforeContext,contextGradient;
    final double[][] contextSum;
    final double[][] pooled,poolGradient;
    final int[][] maxima;
    final double[] hidden=new double[32];double relationNorm,poolNorm;

    BrnSuccessorCandidate(Relations relations,int width,Pool pool,long seed) {
        if(width!=8&&width!=16&&width!=32)throw new IllegalArgumentException("width");
        this.relations=relations;this.width=width;this.pool=pool;
        poolWidth=12*width*(pool==Pool.SUM_MAX||pool==Pool.SUM_SQUARE?2:1)+(pool==Pool.SUM_STATE?12:0);
        nodes=width*switch(relations){case ABSOLUTE->AbsoluteRelationCandidate.EDGE_ROWS;case RELATIVE,DIRECTED_RELATIVE->0;case HASH->32768;case FACTORIZED->768;};
        dense=nodes+768*width;bias=dense+2*poolWidth*32;head=bias+32;output=head+32;relative=output+1;
        context=relative+12*12*225*width;parameters=context+(pool==Pool.SUM_CONTEXT?2*width*width+width:pool==Pool.SUM_SELF?width*width+width:0);
        weights=new double[parameters];first=new double[parameters];second=new double[parameters];gradient=new double[parameters];
        touched=new int[parameters];stamps=new int[parameters];
        local=new double[2][64][width];localGradient=new double[2][64][width];
        beforeContext=new double[2][64][width];contextGradient=new double[2][64][width];contextSum=new double[2][width];
        pooled=new double[2][poolWidth];poolGradient=new double[2][poolWidth];maxima=new int[2][12*width];
        var rng=new Random(seed);
        for(int i=0;i<dense;i++)weights[i]=(2*rng.nextDouble()-1)*.1;
        for(int i=dense;i<bias;i++)weights[i]=(2*rng.nextDouble()-1)*Math.sqrt(6.0/(2*poolWidth+32));
        for(int i=context;i<parameters;i++) {
            // Matched self transform/bias initialization for the contextual ablation.
            if(pool==Pool.SUM_SELF&&i==context+width*width)for(int skip=0;skip<width*width;skip++)rng.nextDouble();
            weights[i]=(2*rng.nextDouble()-1)*.01;
        }
    }
    int edgeRow(int a,int b) { return BrnSuccessorProbe.row(a,b); }
    int directed(int receiver,int sender) {
        int a=receiver%64,b=sender%64,dx=(b&7)-(a&7),dy=(b>>>3)-(a>>>3);
        return relative+((receiver/64*12+sender/64)*225+(dy+7)*15+dx+7)*width;
    }
    int edge(int row) {
        if(relations==Relations.ABSOLUTE)return row*width;
        int h=row;h^=h>>>16;h*=0x7feb352d;h^=h>>>15;h*=0x846ca68b;h^=h>>>16;
        return (h&32767)*width;
    }
    double predict(long[] board) {
        int stm=Board.player((int)board[4]);
        for(int p=0;p<2;p++) {
            count=0;Arrays.fill(pooled[p],0);Arrays.fill(maxima[p],-1);
            for(long bits=board[0]|board[1]|board[2];bits!=0;bits&=bits-1) {
                int sq=Long.numberOfTrailingZeros(bits),code=Board.getSquare(board[0],board[1],board[2],board[3],sq);
                int perspective=stm^p,entity=BrnSuccessorProbe.channel(code^(perspective<<3))*64+(sq^(perspective*56));
                entities[p][count]=entity;System.arraycopy(weights,nodes+entity*width,local[p][count++],0,width);
            }
            relationNorm=1/Math.sqrt(Math.max(1,count-1));poolNorm=1/Math.sqrt(Math.max(1,count));
            for(int i=0;i<count;i++)for(int j=i+1;j<count;j++) {
                int a=entities[p][i],b=entities[p][j],row=edgeRow(a,b),abs=edge(row),rel=relative+AbsoluteRelationCandidate.RELATIVE_ROW[row]*width;
                if(relations==Relations.DIRECTED_RELATIVE) {
                    int left=directed(a,b),right=directed(b,a);
                    for(int c=0;c<width;c++){local[p][i][c]+=relationNorm*weights[left+c];local[p][j][c]+=relationNorm*weights[right+c];}
                    continue;
                }
                for(int c=0;c<width;c++) {
                    double message=weights[rel+c];
                    if(relations==Relations.ABSOLUTE||relations==Relations.HASH)message=weights[abs+c]+message;
                    else if(relations==Relations.FACTORIZED)message=weights[a*width+c]*weights[b*width+c]+message;
                    message*=relationNorm;local[p][i][c]+=message;local[p][j][c]+=message;
                }
            }
            for(int i=0;i<count;i++)for(int c=0;c<width;c++)local[p][i][c]=Math.max(0,local[p][i][c]);
            if(pool==Pool.SUM_CONTEXT||pool==Pool.SUM_SELF) {
                Arrays.fill(contextSum[p],0);
                for(int i=0;i<count;i++)for(int c=0;c<width;c++){beforeContext[p][i][c]=local[p][i][c];contextSum[p][c]+=local[p][i][c];}
                for(int i=0;i<count;i++)for(int h=0;h<width;h++) {
                    double value=beforeContext[p][i][h]+weights[context+(pool==Pool.SUM_CONTEXT?2:1)*width*width+h];
                    for(int c=0;c<width;c++) {
                        double message=pool==Pool.SUM_CONTEXT?weights[context+width*width+h*width+c]*(contextSum[p][c]-beforeContext[p][i][c])/Math.max(1,count-1):0;
                        value+=weights[context+h*width+c]*beforeContext[p][i][c]+message;
                    }
                    local[p][i][h]=Math.max(0,value);
                }
            }
            for(int i=0;i<count;i++)for(int c=0;c<width;c++) {
                double value=local[p][i][c];int at=entities[p][i]/64*width+c;
                pooled[p][at]+=poolNorm*value;
                if(pool==Pool.SUM_MAX&&value>pooled[p][12*width+at]) {pooled[p][12*width+at]=value;maxima[p][at]=i;}
                if(pool==Pool.SUM_SQUARE)pooled[p][12*width+at]+=poolNorm*value*value;
            }
            if(pool==Pool.SUM_STATE) {
                int perspective=stm^p,at=12*width,status=(int)board[4];
                pooled[p][at]=Board.kingSide(status,perspective)?1:0;pooled[p][at+1]=Board.queenSide(status,perspective)?1:0;
                pooled[p][at+2]=Board.kingSide(status,perspective^1)?1:0;pooled[p][at+3]=Board.queenSide(status,perspective^1)?1:0;
                int ep=Board.enPassantSquare(status);if(ep>=0)pooled[p][at+4+(ep&7)]=1;
            }
        }
        double residual=weights[output];
        for(int h=0;h<32;h++) {
            double z=weights[bias+h];
            for(int p=0;p<2;p++)for(int c=0;c<poolWidth;c++)z+=weights[dense+h*2*poolWidth+p*poolWidth+c]*pooled[p][c];
            hidden[h]=Math.max(0,z);residual+=weights[head+h]*hidden[h];
        }
        double value=RelationalCandidate.material(board)+residual;
        if(!Double.isFinite(value))throw new ArithmeticException("nonfinite successor");return value;
    }
    void resetGradient(){if(++serial==0){Arrays.fill(stamps,0);serial=1;}size=0;}
    void add(int index,double value){if(stamps[index]!=serial){stamps[index]=serial;gradient[index]=0;touched[size++]=index;}gradient[index]+=value;}
    void backward(double derivative) {
        for(var g:poolGradient)Arrays.fill(g,0);add(output,derivative);
        for(int h=0;h<32;h++) {
            add(head+h,derivative*hidden[h]);double dz=derivative*weights[head+h]*(hidden[h]>0?1:0);add(bias+h,dz);
            for(int p=0;p<2;p++)for(int c=0;c<poolWidth;c++) {
                int index=dense+h*2*poolWidth+p*poolWidth+c;add(index,dz*pooled[p][c]);poolGradient[p][c]+=dz*weights[index];
            }
        }
        for(int p=0;p<2;p++) {
            for(int i=0;i<count;i++)for(int c=0;c<width;c++) {
                int at=entities[p][i]/64*width+c;double value=local[p][i][c],g=poolNorm*poolGradient[p][at];
                if(pool==Pool.SUM_MAX&&maxima[p][at]==i)g+=poolGradient[p][12*width+at];
                if(pool==Pool.SUM_SQUARE)g+=2*value*poolNorm*poolGradient[p][12*width+at];
                double dz=g*(value>0?1:0);localGradient[p][i][c]=dz;
            }
            if(pool==Pool.SUM_CONTEXT||pool==Pool.SUM_SELF) {
                for(var g:contextGradient[p])Arrays.fill(g,0);var shared=new double[width];
                for(int i=0;i<count;i++)for(int h=0;h<width;h++) {
                    double g=localGradient[p][i][h];contextGradient[p][i][h]+=g;add(context+(pool==Pool.SUM_CONTEXT?2:1)*width*width+h,g);
                    for(int c=0;c<width;c++) {
                        add(context+h*width+c,g*beforeContext[p][i][c]);contextGradient[p][i][c]+=g*weights[context+h*width+c];
                        if(pool==Pool.SUM_CONTEXT) {
                            add(context+width*width+h*width+c,g*(contextSum[p][c]-beforeContext[p][i][c])/Math.max(1,count-1));
                            double all=g*weights[context+width*width+h*width+c]/Math.max(1,count-1);shared[c]+=all;contextGradient[p][i][c]-=all;
                        }
                    }
                }
                for(int i=0;i<count;i++)for(int c=0;c<width;c++)localGradient[p][i][c]=(contextGradient[p][i][c]+shared[c])*(beforeContext[p][i][c]>0?1:0);
            }
            for(int i=0;i<count;i++)for(int c=0;c<width;c++)add(nodes+entities[p][i]*width+c,localGradient[p][i][c]);
            for(int i=0;i<count;i++)for(int j=i+1;j<count;j++) {
                int a=entities[p][i],b=entities[p][j],row=edgeRow(a,b),abs=edge(row),rel=relative+AbsoluteRelationCandidate.RELATIVE_ROW[row]*width;
                if(relations==Relations.DIRECTED_RELATIVE) {
                    int left=directed(a,b),right=directed(b,a);
                    for(int c=0;c<width;c++){add(left+c,relationNorm*localGradient[p][i][c]);add(right+c,relationNorm*localGradient[p][j][c]);}
                    continue;
                }
                for(int c=0;c<width;c++) {
                    double g=relationNorm*(localGradient[p][i][c]+localGradient[p][j][c]);
                    if(relations==Relations.ABSOLUTE||relations==Relations.HASH)add(abs+c,g);
                    else if(relations==Relations.FACTORIZED){add(a*width+c,g*weights[b*width+c]);add(b*width+c,g*weights[a*width+c]);}
                    add(rel+c,g);
                }
            }
        }
    }
    void trainBatch(List<BrnResearchData.Example> examples,int[] order,int offset,int batch,double rate) {
        resetGradient();
        for(int n=0;n<batch;n++){var e=examples.get(order[offset+n]);double value=predict(e.board());backward(RelationalCandidate.crossEntropy(value,e.sfMaterial(),e.outcome())[1]);}
        updates++;double c1=1-Math.pow(.9,updates),c2=1-Math.pow(.999,updates);
        for(int n=0;n<size;n++) {
            int i=touched[n];double g=gradient[i]/batch;first[i]=.9*first[i]+.1*g;second[i]=.999*second[i]+.001*g*g;
            weights[i]-=rate*(first[i]/c1)/(Math.sqrt(second[i]/c2)+1e-8);
            if(!Double.isFinite(weights[i]))throw new ArithmeticException("nonfinite update");
        }
    }
    byte[] encode()throws IOException {
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
        out.writeLong(0x5336535543433031L);out.writeUTF(relations.name());out.writeInt(width);out.writeUTF(pool.name());out.writeLong(updates);
        for(var array:new double[][]{weights,first,second})for(double v:array)out.writeDouble(v);out.flush();
        var crc=new CRC32();crc.update(bytes.toByteArray());out.writeInt((int)crc.getValue());out.flush();return bytes.toByteArray();
    }
    static BrnSuccessorCandidate decode(byte[] bytes)throws IOException {
        if(bytes.length<32)throw new IOException("truncated successor");
        var crc=new CRC32();crc.update(bytes,0,bytes.length-4);
        try(var in=new DataInputStream(new ByteArrayInputStream(bytes))) {
            if(in.readLong()!=0x5336535543433031L)throw new IOException("wrong experimental format");
            var model=new BrnSuccessorCandidate(Relations.valueOf(in.readUTF()),in.readInt(),Pool.valueOf(in.readUTF()),0);
            model.updates=in.readLong();if(model.updates<0)throw new IOException("negative step");
            for(var array:new double[][]{model.weights,model.first,model.second})for(int i=0;i<array.length;i++) {
                double v=in.readDouble();if(!Double.isFinite(v)||array==model.second&&v<0)throw new IOException("invalid parameter");array[i]=v;
            }
            if(in.readInt()!=(int)crc.getValue()||in.read()!=-1)throw new IOException("checksum/length mismatch");return model;
        }catch(IllegalArgumentException e){throw new IOException("invalid successor config",e);}
    }
}
