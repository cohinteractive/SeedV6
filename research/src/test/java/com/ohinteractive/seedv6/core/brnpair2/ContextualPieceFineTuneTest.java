package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import com.ohinteractive.seedv6.core.Board;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.core.brnpair2.ContextualPieceNetworkTest.bytes;
import static com.ohinteractive.seedv6.core.brnpair2.ContextualPieceNetworkTest.read;

class ContextualPieceFineTuneTest {
    @TempDir Path directory;
    static PairCoverageDiagnostic.Data data() {
        var data=new PairCoverageDiagnostic.Data(257);
        for(int i=0;i<data.rows;i++){var b=i%2==0?Board.startingPosition():Board.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1");System.arraycopy(b,0,data.boards,i*6,6);data.targets[i]=(i%5)/4.0;}
        return data;
    }
    @Test void controlIsExactLegacyAndBothRatesResumeAcrossSegmentAndTail()throws Exception {
        var data=data();var parent=new ContextualPieceNetwork(false,.01,71);ContextualPieceDiagnostic.segment(parent,data,71,1,0,257);byte[] saved=bytes(parent);
        var control=ContextualPieceFineTune.fork(parent,.01);assertArrayEquals(saved,bytes(control));
        var reference=read(saved);ContextualPieceDiagnostic.segment(reference,data,71,2,0,257);ContextualPieceFineTuneDiagnostic.segment(control,data,71,2,0,257);assertArrayEquals(bytes(reference),bytes(control));
        for(double rate:new double[]{.01,.001}) {
            var full=ContextualPieceFineTune.fork(parent,rate);var segmented=ContextualPieceFineTune.fork(parent,rate);
            ContextualPieceFineTuneDiagnostic.segment(full,data,71,2,0,257);
            ContextualPieceFineTuneDiagnostic.segment(segmented,data,71,2,0,128);segmented=read(bytes(segmented));ContextualPieceFineTuneDiagnostic.segment(segmented,data,71,2,128,257);
            assertArrayEquals(bytes(full),bytes(segmented));assertEquals(6,full.step);
        }
        assertArrayEquals(saved,bytes(parent));assertThrows(IllegalArgumentException.class,()->ContextualPieceFineTune.fork(parent,.002));assertThrows(IllegalArgumentException.class,()->ContextualPieceFineTune.fork(new ContextualPieceNetwork(true,.01,71),.001));
    }
    @Test void lowRateMatchesIndependentAdamHistoryAndFixedShrinkIncludingUntouched()throws Exception {
        var parent=new ContextualPieceNetwork(false,.01,71);var w=new ContextualPieceNetwork.Workspace();var batch=new ContextualPieceNetwork.Features[]{w.encode(Board.startingPosition()).frozen()};double[] targets={.8};
        for(int i=0;i<3;i++)parent.train(batch,targets);int untouched=ContextualPieceNetwork.S;parent.weights[untouched]=.2;parent.first[untouched]=.25;parent.second[untouched]=.3;byte[] saved=bytes(parent);
        var m=ContextualPieceFineTune.fork(parent,.001);assertArrayEquals(parent.weights,m.weights);assertArrayEquals(parent.first,m.first);assertArrayEquals(parent.second,m.second);assertEquals(parent.step,m.step);
        m.gradients(batch,targets);boolean[] active=new boolean[m.weights.length];for(int i=0;i<m.count;i++)active[m.touched[i]]=true;
        double[] expectedW=m.weights.clone(),expectedM=m.first.clone(),expectedV=m.second.clone();long next=m.step+1;
        for(int i=0;i<expectedW.length;i++) {
            expectedW[i]*=.9999;
            if(active[i]){expectedM[i]=.9*expectedM[i]+.1*m.gradient[i];expectedV[i]=.999*expectedV[i]+.001*m.gradient[i]*m.gradient[i];expectedW[i]-=.001*(expectedM[i]/(1-Math.pow(.9,next)))/(Math.sqrt(expectedV[i]/(1-Math.pow(.999,next)))+1e-8);}
            expectedW[i]=Math.max(-.99,Math.min(.99,expectedW[i]));
        }
        ContextualPieceFineTune.train(m,batch,targets);assertArrayEquals(expectedW,m.weights,2e-16);assertArrayEquals(expectedM,m.first,2e-16);assertArrayEquals(expectedV,m.second,2e-16);assertEquals(next,m.step);assertFalse(active[untouched]);assertEquals(.2*.9999,m.weights[untouched]);assertEquals(.25,m.first[untouched]);assertArrayEquals(saved,bytes(parent));
    }
    @Test void parentIdentityConfigurationModeAndStepAreEnforced()throws Exception {
        var model=new ContextualPieceNetwork(false,.01,71);ContextualPieceDiagnostic.segment(model,data(),71,1,0,257);
        Path state=directory.resolve("parent.state"),parent=directory.resolve("parent.json"),receipt=directory.resolve("receipt.json");Files.write(state,bytes(model));
        var p=new JsonObject();p.addProperty("mode","ONE");p.addProperty("seed",71);p.addProperty("train_positions",257);p.addProperty("rate",.01);p.addProperty("epochs",1);p.addProperty("width",8);p.addProperty("batch",128);p.addProperty("weight_bound",.99);for(String name:List.of("train_sha256","validation_sha256","original_validation_sha256"))p.addProperty(name,"fixture");Files.writeString(parent,p.toString());
        var c=p.deepCopy();c.addProperty("initial_epoch",1);c.addProperty("rate",.001);c.addProperty("parent_config",parent.toString());c.addProperty("parent_config_sha256",PairCoverageDiagnostic.sha(parent));c.addProperty("initial_state",state.toString());c.addProperty("parent_receipt",receipt.toString());
        bind(c,state,receipt,model);var loaded=ContextualPieceFineTuneDiagnostic.loadParent(c);assertEquals(.001,loaded.rate);assertEquals(3,loaded.step);assertArrayEquals(model.weights,loaded.weights);
        var bad=c.deepCopy();bad.addProperty("seed",97);assertThrows(IllegalArgumentException.class,()->ContextualPieceFineTuneDiagnostic.loadParent(bad));
        var badHash=c.deepCopy();badHash.addProperty("initial_state_sha256","wrong");assertThrows(IllegalArgumentException.class,()->ContextualPieceFineTuneDiagnostic.loadParent(badHash));
        model.step=4;Files.write(state,bytes(model));bind(c,state,receipt,model);assertThrows(IllegalArgumentException.class,()->ContextualPieceFineTuneDiagnostic.loadParent(c));
        model=new ContextualPieceNetwork(true,.01,71);model.step=3;Files.write(state,bytes(model));bind(c,state,receipt,model);assertThrows(IllegalArgumentException.class,()->ContextualPieceFineTuneDiagnostic.loadParent(c));
    }
    static void bind(JsonObject c,Path state,Path receipt,ContextualPieceNetwork model)throws Exception {
        String hash=PairCoverageDiagnostic.sha(state);Files.writeString(receipt,new Gson().toJson(Map.of("epoch",1,"updates",model.step,"state_sha256",hash)));c.addProperty("initial_state_sha256",hash);c.addProperty("parent_receipt_sha256",PairCoverageDiagnostic.sha(receipt));
    }
}
