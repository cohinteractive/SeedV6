package com.ohinteractive.seedv6.tools.eval;

import com.google.gson.JsonParser;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Model;
import com.ohinteractive.seedv6.core.brnnext.BrnNextBase;
import com.ohinteractive.seedv6.core.brnpair2.*;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.model.NetworkModel;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.util.*;

/** Independent linear-function check of the compiled half-change additive control. */
public final class BrnAdditiveControlProbe {
    private record Plan(String id,List<BrnSuccessorMatch.Model> models) {}
    public static void main(String[] args) throws Exception {
        if(args.length!=4)throw new IllegalArgumentException("MODEL_PLAN BT4_JSONL CP_JSONL NEW_OUTPUT");
        Path output=Path.of(args[3]);
        if(Files.exists(output))throw new IllegalArgumentException("Preserve prior evidence");
        var plan=DataFiles.read(Path.of(args[0]),Plan.class);
        var spec=plan.models().stream().filter(m->m.name().equals("W3-BRE-CONTROL")).findFirst().orElseThrow();
        var snapshot=CheckpointStore.readSnapshot(Path.of(spec.root()),spec.checkpoint());
        if(!snapshot.manifest().networkSha256().equals(spec.networkSha256()))throw new IllegalArgumentException("Source binding");
        var source=((NetworkModel.BrnPair2)snapshot.model()).model();
        var variant=BrnSuccessorVariants.additiveChange(source,.5);
        var boards=new ArrayList<>(BrnSuccessorAudit.boards());
        var bindings=new TreeMap<String,String>();
        for(int i=0;i<3;i++)bindings.put(args[i],DataFiles.hash(Files.readString(Path.of(args[i]))));
        for(int i=1;i<3;i++)for(String line:Files.readAllLines(Path.of(args[i])))
            boards.add(Board.fromFen(JsonParser.parseString(line).getAsJsonObject().get("fen").getAsString()));
        var actual=variant.newWorkspace();var base=BrnNextBase.model().newWorkspace();var candidate=source.newWorkspace();
        double maximum=0;int different=0,count=0;
        for(int pass=0;pass<3;pass++)for(int i=0;i<boards.size();i++) {
            var board=boards.get(pass==1?boards.size()-1-i:i);
            double expected=.5*(base.evaluatePawns(board)+candidate.evaluatePawns(board));
            double value=actual.evaluatePawns(board),material=BrnPair2Features.material(board);
            maximum=Math.max(maximum,Math.abs(expected-value));count++;
            if(Brn3Model.score(material+.25*(expected-material))!=Brn3Model.score(actual.evaluatePawns(board,.25)))different++;
        }
        if(maximum>1e-10||different!=0)throw new IllegalStateException("Compiled interpolation parity: "+maximum+"/"+different);
        var encoded=new ByteArrayOutputStream();BrnPair2Codec.writeModel(variant,encoded);
        DataFiles.write(output,Map.of("id","wave3-additive-half-oracle-v1","inputs",bindings,
                "source",snapshot.manifest(),"comparisons",count,"maxRawError",maximum,"integerDifferences",different,
                "effectiveNetworkSha256",HexFormat.of().formatHex(DataFiles.digest().digest(encoded.toByteArray())),
                "networkBytes",encoded.size(),"immutablePrimitiveBytes",variant.immutablePrimitiveBytes(),
                "workerPrimitiveBytes",actual.primitiveBytes()));
        System.out.println("Verified compiled additive control: "+count+" comparisons, max="+maximum+", integer differences="+different);
    }
    private BrnAdditiveControlProbe() {}
}
