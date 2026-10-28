package com.ohinteractive.seedv6.core.brnpair2;

import java.io.*;

/** Self-contained attack-field evaluator with an explicitly serialized score-view convention. */
public final class CanonicalAttackFieldNetwork {
    final boolean canonical;
    final AttackFieldNetwork model;
    public CanonicalAttackFieldNetwork(boolean canonical,double rate,long seed){this(canonical,new AttackFieldNetwork(true,rate,seed));}
    private CanonicalAttackFieldNetwork(boolean canonical,AttackFieldNetwork model){this.canonical=canonical;this.model=model;if(!model.allSquares)throw new IllegalArgumentException("All-square field required");}
    public static final class Workspace {
        final long[] view=new long[6];
        final AttackFieldNetwork.Workspace neural=new AttackFieldNetwork.Workspace();
        AttackFieldNetwork.Features encode(long[] board,boolean canonical) {
            if(canonical){HorizontalScoreView.canonical(board,view);return neural.encode(view,true);}return neural.encode(board,true);
        }
    }
    public double scoreCp(long[] board,Workspace w){return 400*model.logit(w.encode(board,canonical),w.neural);}
    void write(OutputStream stream)throws IOException {
        var payload=new ByteArrayOutputStream();model.write(payload);var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);
        out.writeLong(0x42524e4841463031L);out.writeInt(canonical?1:0);out.writeInt(payload.size());payload.writeTo(out);out.flush();var crc=new java.util.zip.CRC32();crc.update(bytes.toByteArray());out.writeLong(crc.getValue());stream.write(bytes.toByteArray());
    }
    static CanonicalAttackFieldNetwork read(InputStream stream)throws IOException {
        int length=64+24*AttackFieldNetwork.PARAMETERS;byte[] bytes=stream.readNBytes(length+1);if(bytes.length!=length)throw new IOException("Canonical field state length");
        var crc=new java.util.zip.CRC32();crc.update(bytes,0,bytes.length-8);var in=new DataInputStream(new ByteArrayInputStream(bytes));
        if(in.readLong()!=0x42524e4841463031L)throw new IOException("Canonical field magic");int flag=in.readInt(),payload=in.readInt();if(flag<0||flag>1||payload!=length-24)throw new IOException("Canonical field geometry");
        var model=AttackFieldNetwork.read(new ByteArrayInputStream(in.readNBytes(payload)));if(in.readLong()!=crc.getValue()||!model.allSquares)throw new IOException("Canonical field frame/CRC");return new CanonicalAttackFieldNetwork(flag==1,model);
    }
}
