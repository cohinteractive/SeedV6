package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Magic;
import com.ohinteractive.seedv6.core.util.Piece;
import java.io.*;
import java.util.*;
import static com.ohinteractive.seedv6.core.util.Bitboard.*;

/** Research-only direct conditional graph energies; no dense hidden network. */
public final class RelationalGraphEnergy {
    public enum Mode { UNARY, EDGE, CONTEXT }
    static final int BASE=769,MAX_ACTIVE=1025;
    final Mode mode;
    final double[] weights,first,second,gradient;
    final int[] stamps,touched;
    long step;
    int serial,count;
    final double rate;
    public RelationalGraphEnergy(Mode mode,double rate) {
        if(!Double.isFinite(rate)||rate<=0)throw new IllegalArgumentException("Rate");
        this.mode=Objects.requireNonNull(mode);this.rate=rate;
        int parameters=BASE+(mode==Mode.UNARY?0:12*12*225*(mode==Mode.CONTEXT?9:1));
        weights=new double[parameters];first=new double[parameters];second=new double[parameters];gradient=new double[parameters];
        stamps=new int[parameters];touched=new int[parameters];
    }
    public int parameters(){return weights.length;}
    static long attacks(int piece,int square,long occupancy) {
        return switch(piece&7) {
            case Piece.KING -> BB[KING_ATTACKS][square];
            case Piece.QUEEN -> Magic.rookMoves(square,occupancy)|Magic.bishopMoves(square,occupancy);
            case Piece.ROOK -> Magic.rookMoves(square,occupancy);
            case Piece.BISHOP -> Magic.bishopMoves(square,occupancy);
            case Piece.KNIGHT -> BB[LEAP_ATTACKS][square];
            case Piece.PAWN -> BB[PAWN_ATTACKS+(piece>>>3)][square];
            default -> throw new IllegalArgumentException("Piece");
        };
    }
    public static final class Features {
        final int[] ids=new int[MAX_ACTIVE],pieces=new int[64],defenders=new int[64],attackers=new int[64];
        final long[] edges=new long[64];
        int size;double material;
        void encode(long[] board,Mode mode) {
            Arrays.fill(defenders,0);Arrays.fill(attackers,0);Arrays.fill(pieces,0);Arrays.fill(edges,0);size=0;
            long occupied=board[0]|board[1]|board[2];int stm=Board.player((int)board[4]);
            if(Long.bitCount(occupied)>32)throw new IllegalArgumentException("More than32 pieces");
            material=BrnPair2Features.material(board)*100;ids[size++]=768;
            for(long bits=occupied;bits!=0;bits&=bits-1) {
                int s=Long.numberOfTrailingZeros(bits),piece=Board.getSquare(board[0],board[1],board[2],board[3],s);pieces[s]=piece;
                int channel=(piece&7)-1+6*((piece>>>3)^stm);ids[size++]=channel*64+(s^(stm*56));
            }
            if(mode==Mode.UNARY)return;
            for(long bits=occupied;bits!=0;bits&=bits-1) {
                int s=Long.numberOfTrailingZeros(bits);edges[s]=attacks(pieces[s],s,occupied)&occupied;
                for(long targets=edges[s];targets!=0;targets&=targets-1) {
                    int t=Long.numberOfTrailingZeros(targets);
                    if((pieces[s]>>>3)==(pieces[t]>>>3))defenders[t]++;else attackers[t]++;
                }
            }
            for(long bits=occupied;bits!=0;bits&=bits-1) {
                int s=Long.numberOfTrailingZeros(bits),a=pieces[s],ac=(a&7)-1+6*((a>>>3)^stm),sq=s^(stm*56);
                for(long targets=edges[s];targets!=0;targets&=targets-1) {
                    int t=Long.numberOfTrailingZeros(targets),b=pieces[t],bc=(b&7)-1+6*((b>>>3)^stm),tq=t^(stm*56);
                    int displacement=(tq/8-sq/8+7)*15+(tq%8-sq%8+7),base=(ac*12+bc)*225+displacement;
                    boolean friendly=(a>>>3)==(b>>>3);
                    int context=Math.min(2,defenders[t]-(friendly?1:0))*3+Math.min(2,attackers[t]-(friendly?0:1));
                    ids[size++]=BASE+(mode==Mode.CONTEXT?base*9+context:base);
                }
            }
        }
        Features frozen(){var f=new Features();f.size=size;f.material=material;System.arraycopy(ids,0,f.ids,0,size);return f;}
    }
    static double coefficient(int id){return id<BASE?1:.125;}
    double score(Features f){double sum=f.material;for(int i=0;i<f.size;i++)sum+=weights[f.ids[i]]*coefficient(f.ids[i]);return sum;}
    public double score(long[] board,Features workspace){workspace.encode(board,mode);return score(workspace);}
    double gradients(Features[] batch,double[] targets) {
        if(++serial==0){Arrays.fill(stamps,0);serial=1;}count=0;double loss=0;
        for(int n=0;n<batch.length;n++) {
            var f=batch[n];double p=PairObjectiveDiagnostic.sigmoid(score(f)/400),d=p-targets[n];loss+=d*d;
            double derivative=2*d*p*(1-p)/400;
            for(int j=0;j<f.size;j++) {
                int index=f.ids[j];if(stamps[index]!=serial){stamps[index]=serial;touched[count++]=index;gradient[index]=0;}
                gradient[index]+=derivative*coefficient(index)/batch.length;
            }
        }
        return loss/batch.length;
    }
    double train(Features[] batch,double[] targets) {
        double loss=gradients(batch,targets);step++;double c1=1-Math.pow(.9,step),c2=1-Math.pow(.999,step);
        for(int j=0;j<count;j++) {
            int i=touched[j];double g=gradient[i];first[i]=.9*first[i]+.1*g;second[i]=.999*second[i]+.001*g*g;
            weights[i]-=rate*(first[i]/c1)/(Math.sqrt(second[i]/c2)+1e-8);
            if(!Double.isFinite(weights[i]))throw new ArithmeticException("Nonfinite graph energy");
        }
        return loss;
    }
    void write(OutputStream stream)throws IOException {
        var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
        out.writeLong(0x42524e4752415031L);out.writeInt(mode.ordinal());out.writeInt(weights.length);out.writeDouble(rate);out.writeLong(step);
        for(var array:new double[][]{weights,first,second})for(double v:array)out.writeDouble(v);
        out.flush();var crc=new java.util.zip.CRC32();crc.update(bytes.toByteArray());out.writeLong(crc.getValue());stream.write(bytes.toByteArray());
    }
    static RelationalGraphEnergy read(InputStream stream)throws IOException {
        byte[] bytes=stream.readNBytes(8_000_001);if(bytes.length<40||bytes.length>8_000_000)throw new IOException("Graph state length");
        var crc=new java.util.zip.CRC32();crc.update(bytes,0,bytes.length-8);var in=new DataInputStream(new ByteArrayInputStream(bytes));
        if(in.readLong()!=0x42524e4752415031L)throw new IOException("Graph state magic");
        int mode=in.readInt(),parameters=in.readInt();if(mode<0||mode>=Mode.values().length)throw new IOException("Graph mode");
        double rate=in.readDouble();var model=new RelationalGraphEnergy(Mode.values()[mode],rate);model.step=in.readLong();
        if(parameters!=model.parameters()||model.step<0||bytes.length!=40+24*parameters)throw new IOException("Graph geometry");
        for(var array:new double[][]{model.weights,model.first,model.second})for(int i=0;i<array.length;i++){
            array[i]=in.readDouble();if(!Double.isFinite(array[i]))throw new IOException("Graph nonfinite");
        }
        if(in.readLong()!=crc.getValue())throw new IOException("Graph CRC");return model;
    }
}
