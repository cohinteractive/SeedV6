package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RelationalMessagesTest {
    @Test void regularizedCompositionPreservesBoundsAndExactContinuation()throws Exception {
        var model=new RelationalMessages(true,.01,71,true);var w=new RelationalMessages.Workspace();var f=w.encode(Board.startingPosition()).frozen();
        Arrays.fill(model.weights,RelationalMessages.C,model.weights.length,1.5);
        var batch=new RelationalMessages.Features[]{f};double[] target={.7};model.train(batch,target);
        for(double value:model.weights)assertTrue(Double.isFinite(value)&&Math.abs(value)<=.99);
        var bytes=new ByteArrayOutputStream();model.write(bytes);var restored=RelationalMessages.read(new ByteArrayInputStream(bytes.toByteArray()));
        assertTrue(restored.compose&&restored.regularized);model.train(batch,target);restored.train(batch,target);
        assertArrayEquals(model.weights,restored.weights);assertArrayEquals(model.first,restored.first);assertArrayEquals(model.second,restored.second);
    }
    @Test void regularizerBoundsContinuationAndVersionOneCompatibility()throws Exception {
        var model=new RelationalMessages(false,.01,71,true);var w=new RelationalMessages.Workspace();var f=w.encode(Board.startingPosition()).frozen();
        Arrays.fill(model.weights,RelationalMessages.E,RelationalMessages.Q,1.5);
        model.weights[RelationalMessages.C]=.5; // Inactive in PAIR, but dense decay still applies.
        var batch=new RelationalMessages.Features[]{f};double[] target={.7};model.train(batch,target);
        for(double value:model.weights)assertTrue(Math.abs(value)<=.99);
        assertEquals(.5*(1-.01*.01),model.weights[RelationalMessages.C],0);
        var bytes=new ByteArrayOutputStream();model.write(bytes);var restored=RelationalMessages.read(new ByteArrayInputStream(bytes.toByteArray()));assertTrue(restored.regularized);
        model.train(batch,target);restored.train(batch,target);assertArrayEquals(model.weights,restored.weights);assertArrayEquals(model.first,restored.first);
        // Independently emit the original R004 envelope, including its original CRC.
        var original=new RelationalMessages(false,.01,97);original.train(batch,target);
        var oldBytes=new ByteArrayOutputStream();var output=new DataOutputStream(oldBytes);
        output.writeLong(0x42524e4d53473031L);output.writeInt(0);output.writeInt(RelationalMessages.PARAMETERS);output.writeDouble(original.rate);output.writeLong(original.step);
        for(var array:new double[][]{original.weights,original.first,original.second})for(double value:array)output.writeDouble(value);
        output.flush();var crc=new java.util.zip.CRC32();crc.update(oldBytes.toByteArray());output.writeLong(crc.getValue());
        var imported=RelationalMessages.read(new ByteArrayInputStream(oldBytes.toByteArray()));assertFalse(imported.regularized);
        original.train(batch,target);imported.train(batch,target);assertArrayEquals(original.weights,imported.weights);assertArrayEquals(original.second,imported.second);
    }
    @Test void factorizedEdgesMatchIndependentOracleAndColorPerspective() {
        for(String fen:List.of("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1","r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","4k3/P7/8/8/8/8/7p/4K3 b - - 0 1")) {
            long[] b=Board.fromFen(fen);var workspace=new RelationalMessages.Workspace();var f=workspace.encode(b);
            var model=new RelationalMessages(true,.01,71);for(int i=RelationalMessages.C;i<model.weights.length;i++)model.weights[i]=.1;
            int stm=Board.player((int)b[4]),edges=0;double[] a=new double[64*8],squared=new double[64*8];
            for(int s=0;s<64;s++)for(int t=0;t<64;t++) {
                int source=Board.getSquare(b[0],b[1],b[2],b[3],s),target=Board.getSquare(b[0],b[1],b[2],b[3],t);
                if(target==0||!RelationalGraphEnergyTest.edge(b,s,t))continue;edges++;
                int sourceId=((source&7)-1+6*((source>>>3)^stm))*64+(s^(stm*56));
                int targetChannel=(target&7)-1+6*((target>>>3)^stm),sq=s^(stm*56),tq=t^(stm*56);
                int relation=targetChannel*225+(tq/8-sq/8+7)*15+tq%8-sq%8+7;
                for(int d=0;d<8;d++){double m=.5*model.weights[RelationalMessages.E+sourceId*8+d]*model.weights[RelationalMessages.R+relation*8+d];a[t*8+d]+=m;squared[t*8+d]+=m*m;}
            }
            assertEquals(edges,f.edges);double expected=f.material;
            for(int t=0;t<64;t++) {
                int piece=Board.getSquare(b[0],b[1],b[2],b[3],t);if(piece==0)continue;
                int id=((piece&7)-1+6*((piece>>>3)^stm))*64+(t^(stm*56));
                for(int d=0;d<8;d++)expected+=.125*(model.weights[RelationalMessages.Q+id*8+d]*a[t*8+d]+model.weights[RelationalMessages.C+id*8+d]*(a[t*8+d]*a[t*8+d]-squared[t*8+d]));
            }
            assertEquals(expected,model.logit(f,workspace),1e-12);
            var mirror=b.clone();long occ=b[0]|b[1]|b[2];for(int i=0;i<3;i++)mirror[i]=Long.reverseBytes(b[i]);mirror[3]=Long.reverseBytes(occ^b[3]);mirror[4]^=1;
            assertEquals(model.scoreCp(b,workspace),model.scoreCp(mirror,workspace),1e-9);
        }
    }
    @Test void distinctSourcesRequiredForComposition() {
        var model=new RelationalMessages(true,.01,71);Arrays.fill(model.weights,0);
        Arrays.fill(model.weights,RelationalMessages.E,RelationalMessages.Q,1);Arrays.fill(model.weights,RelationalMessages.C,model.weights.length,1);
        var f=new RelationalMessages.Features();f.size=3;f.nodes[0]=0;f.nodes[1]=1;f.nodes[2]=2;f.from[0]=0;f.to[0]=2;f.relation[0]=0;f.edges=1;
        var w=new RelationalMessages.Workspace();assertEquals(0,model.logit(f,w),0);
        f.from[1]=1;f.to[1]=2;f.relation[1]=1;f.edges=2;assertEquals(.5,model.logit(f,w),1e-12);
    }
    @Test void everyParameterFamilyGradientTinyFitAndResume()throws Exception {
        for(boolean compose:new boolean[]{false,true}) {
            var model=new RelationalMessages(compose,.01,71);var w=new RelationalMessages.Workspace();var f=w.encode(Board.startingPosition()).frozen();
            if(compose)for(int i=RelationalMessages.C;i<model.weights.length;i++)model.weights[i]=.17;
            var batch=new RelationalMessages.Features[]{f};double[] targets={.8};model.gradients(batch,targets);
            for(int family=0;family<(compose?5:4);family++) {
                int lower=new int[]{0,RelationalMessages.E,RelationalMessages.R,RelationalMessages.Q,RelationalMessages.C}[family];
                int upper=new int[]{RelationalMessages.E,RelationalMessages.R,RelationalMessages.Q,RelationalMessages.C,RelationalMessages.PARAMETERS}[family],checked=0;
                for(int n=0;n<model.count&&checked<7;n++) {
                    int i=model.touched[n];if(i<lower||i>=upper)continue;checked++;
                    double old=model.weights[i],eps=1e-5;model.weights[i]=old+eps;double p=PairObjectiveDiagnostic.sigmoid(model.logit(f,w));
                    model.weights[i]=old-eps;double q=PairObjectiveDiagnostic.sigmoid(model.logit(f,w));model.weights[i]=old;
                    assertEquals(((p-.8)*(p-.8)-(q-.8)*(q-.8))/(2*eps),model.gradient[i],1e-9,"family"+family);
                }
                assertTrue(checked>0);
            }
            double before=model.gradients(batch,targets);for(int i=0;i<20;i++)model.train(batch,targets);assertTrue(model.gradients(batch,targets)<before);
            var encoded=new ByteArrayOutputStream();model.write(encoded);var restored=RelationalMessages.read(new ByteArrayInputStream(encoded.toByteArray()));
            model.train(batch,targets);restored.train(batch,targets);var a=new ByteArrayOutputStream();var b=new ByteArrayOutputStream();model.write(a);restored.write(b);assertArrayEquals(a.toByteArray(),b.toByteArray());
            byte[] corrupt=a.toByteArray();corrupt[100]^=1;assertThrows(IOException.class,()->RelationalMessages.read(new ByteArrayInputStream(corrupt)));
        }
    }
}
