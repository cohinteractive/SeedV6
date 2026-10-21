package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.brnnext.*;
import java.io.*;
import java.util.*;
import java.security.MessageDigest;
import static com.ohinteractive.seedv6.core.brnnext.BrnNextArchitecture.*;

/** Exact additive structure of a constant-gate ablation, including its four non-C02 relations.
 * Binary64 summation changes; preserve source identities and verify actual score/search parity. */
public final class BrnConstantGateResearchCompiler {
    public record Compiled(BrnPair2Model model,String foldedBaseSha256,String extraTablesSha256,
                           String functionSha256,int[] extraFirst,int[] extraSecond) {}
    public static Compiled compile(BrnPair2Model base,BrnNextModel constant)throws IOException {
        var kind=constant.architecture();
        if(kind!=GEN32_GATED)throw new IllegalArgumentException("Frozen constant-gate correction required");
        for(int i=EMBEDDING;i<kind.head();i++)if(constant.weight(i)!=0)
            throw new IllegalArgumentException("Position-dependent gates cannot be compiled as additive tables");
        double[] unaryWeights=new double[kind.parameters()];
        for(int i=0;i<EMBEDDING;i++)unaryWeights[i]=constant.weight(i);
        var folded=BrnUnaryResearchCompiler.compile(base,new BrnNextModel(kind,unaryWeights));
        var keys=new TreeSet<Integer>();
        for(int side=0;side<2;side++)for(int t=0;t<BrnPair2Geometry.TEMPLATES;t++) {
            int a=BrnPair2Geometry.cell(t,0)^(side*56),b=BrnPair2Geometry.cell(t,1)^(side*56);
            keys.add(Math.min(a,b)*64+Math.max(a,b));
        }
        var indices=new HashMap<Integer,Integer>();int index=0;for(int key:keys)indices.put(key,index++);
        if(index!=BrnPair2Codec.PAIRS)throw new IllegalStateException("C02 relation identity changed");
        double[] gates=new double[kind.width()];double maximum=Double.NEGATIVE_INFINITY,total=0;
        for(int expert=0;expert<gates.length;expert++)maximum=Math.max(maximum,constant.weight(kind.head()+expert));
        for(int expert=0;expert<gates.length;expert++){gates[expert]=Math.exp(constant.weight(kind.head()+expert)-maximum);total+=gates[expert];}
        for(int expert=0;expert<gates.length;expert++)gates[expert]/=total;
        var extras=new TreeMap<Integer,double[]>();
        for(int side=0;side<2;side++)for(int shape=0;shape<4;shape++)for(int square=0;square<64;square++) {
            int dx=shape==1?0:shape==3?-1:1,dy=shape==0?0:1;
            int x=(square&7)+dx,y=(square>>>3)+dy;if(x<0||x>=8||y>=8)continue;
            int a=square^(side*56),b=(y*8+x)^(side*56),key=Math.min(a,b)*64+Math.max(a,b);
            Integer pair=indices.get(key);double[] extra=pair==null?extras.computeIfAbsent(key,k->new double[338]):null;
            for(int low=0;low<13;low++)for(int high=0;high<13;high++) {
                int first=a<b?low:high,second=a<b?high:low;
                if(side==1){first=opposite(first);second=opposite(second);}
                double value=0;
                for(int expert=0;expert<gates.length;expert++)value+=gates[expert]*constant.weight(
                        kind.head()+kind.width()+expert*PAIR_STATES+shape*169+first*13+second);
                value*=PAIR_NORM/BrnPair2Geometry.NORM;
                int address=(low+13*high)*2+side;
                if(pair==null)extra[address]+=value;else folded.weights[pair*338+address]+=value;
            }
        }
        if(!extras.keySet().equals(new TreeSet<>(List.of(1*64+8,6*64+15,48*64+57,55*64+62))))
            throw new IllegalStateException("Unexpected supplemental relation geometry");
        var baseBytes=new ByteArrayOutputStream();BrnPair2Codec.writeModel(folded,baseBytes);
        folded=BrnPair2Codec.readModel(new ByteArrayInputStream(baseBytes.toByteArray()));
        int[] first=new int[4],second=new int[4];double[] tables=new double[4*338];index=0;
        var extraBytes=new ByteArrayOutputStream();var out=new DataOutputStream(extraBytes);
        for(var entry:extras.entrySet()) {
            first[index]=entry.getKey()/64;second[index]=entry.getKey()%64;out.writeInt(first[index]);out.writeInt(second[index]);
            System.arraycopy(entry.getValue(),0,tables,index*338,338);for(double value:entry.getValue())out.writeDouble(value);index++;
        }
        String baseHash=hash(baseBytes.toByteArray()),extraHash=hash(extraBytes.toByteArray());
        String identity="brn-constant-gates-compiled-v1\n"+baseHash+"\n"+extraHash+"\n";
        return new Compiled(BrnPair2Model.withResearchPairs(folded,first,second,tables),baseHash,extraHash,
                hash(identity.getBytes(java.nio.charset.StandardCharsets.UTF_8)),first,second);
    }
    private static int opposite(int c){return c==0?0:c<=6?c+6:c-6;}
    private static String hash(byte[] data){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));}
        catch(java.security.NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}}
    private BrnConstantGateResearchCompiler(){}
}
