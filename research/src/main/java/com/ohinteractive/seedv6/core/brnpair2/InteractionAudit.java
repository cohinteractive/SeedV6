package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.conventional.*;
import java.nio.file.*;
import java.util.*;
import static com.ohinteractive.seedv6.core.brnpair2.BrnPair2Geometry.*;

/** Independent one-hot cancellation proof on each counterfactual quartet. */
public final class InteractionAudit {
    static final int[] SIGNS={1,-1,-1,1};
    static void requireCancellation(long[][] boards) {
        var basis=new HashMap<Integer,Integer>();double material=0;int stm=Board.player((int)boards[0][4]);
        for(int n=0;n<4;n++) {
            var b=boards[n];if(Board.player((int)b[4])!=stm)throw new IllegalArgumentException("Perspective changed");
            material+=SIGNS[n]*BrnPair2Features.material(b);int[][] codes=new int[2][64];
            for(int s=0;s<64;s++){int p=Board.getSquare(b[0],b[1],b[2],b[3],s);for(int side=0;side<2;side++)codes[side][s^(side*56)]=category(p,side);}
            for(int role=0;role<2;role++)for(int t=0;t<TEMPLATES;t++)basis.merge((role*TEMPLATES+t)*169+state(codes[stm^role],t,2),SIGNS[n],Integer::sum);
        }
        if(Math.abs(material)>1e-12||basis.values().stream().anyMatch(x->x!=0))throw new IllegalArgumentException("Pair basis does not cancel");
    }
    static int context(long[] board) {
        int side=Board.player((int)board[4]);for(int square=0;square<64;square++) {
            int piece=Board.getSquare(board[0],board[1],board[2],board[3],square);
            if((piece&7)==1&&(piece>>>3)==side)return ConventionalFeatures.context(side,square);
        }
        throw new IllegalArgumentException("King missing");
    }
    public static void main(String[] args)throws Exception {
        PairObjectiveDiagnostic.permission();Path configPath=Path.of(args[0]);var config=JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
        Path cases=Path.of(config.get("cases").getAsString()),teacher=Path.of(config.get("teacher").getAsString());
        if(!PairObjectiveDiagnostic.sha(cases).equals(config.get("cases_sha256").getAsString())||!PairObjectiveDiagnostic.sha(teacher).equals(config.get("teacher_sha256").getAsString()))throw new IllegalArgumentException("Audit identity");
        var nnue=ConventionalNetwork.load(teacher);var models=new LinkedHashMap<String,BrnPair2Model>();
        for(var item:config.getAsJsonArray("models")) {
            var spec=item.getAsJsonObject();Path path=Path.of(spec.get("path").getAsString());
            if(!PairObjectiveDiagnostic.sha(path).equals(spec.get("sha256").getAsString()))throw new IllegalArgumentException("Pair identity");
            try(var input=Files.newInputStream(path)){models.put(spec.get("name").getAsString(),BrnPair2Codec.readModel(input));}
        }
        var results=new ArrayList<Object>();double maximumPairError=0;
        for(var item:JsonParser.parseString(Files.readString(cases)).getAsJsonArray()) {
            var value=item.getAsJsonObject();long[][] boards=new long[4][];
            for(int n=0;n<4;n++)boards[n]=Board.fromFen(value.getAsJsonArray("fens").get(n).getAsString());requireCancellation(boards);
            boolean changed=context(boards[0])!=context(boards[1]);if(changed!=value.get("context_changed").getAsBoolean())throw new IllegalArgumentException("Context classification");
            int[] scores=new int[4];int mixed=0;for(int n=0;n<4;n++){scores[n]=nnue.fullRebuild(boards[n]);mixed+=SIGNS[n]*scores[n];}
            var pairMixed=new LinkedHashMap<String,Double>();
            for(var model:models.entrySet()) {
                double delta=0;for(int n=0;n<4;n++)delta+=SIGNS[n]*model.getValue().newWorkspace().evaluatePawns(boards[n],.25)*100;
                maximumPairError=Math.max(maximumPairError,Math.abs(delta));pairMixed.put(model.getKey(),delta);
            }
            results.add(Map.of("source_row",value.get("source_row").getAsInt(),"context_changed",changed,"nnue_scores_cp",scores,"nnue_mixed_cp",mixed,"pair_mixed_cp",pairMixed));
        }
        if(maximumPairError>1e-8)throw new AssertionError("Compiled Pair-2 cancellation failed");
        var report=Map.of("config_sha256",PairObjectiveDiagnostic.sha(configPath),"maximum_pair_mixed_cp",maximumPairError,"cases",results,
                "scope","Value-free structural audit; nonlocal response capacity,not chess correctness or playing strength");
        Path out=Path.of(config.get("output").getAsString());Files.createDirectories(out.getParent());Files.writeString(out,new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n",StandardOpenOption.CREATE_NEW);
        System.out.println("Verified "+results.size()+" quartet bases; maximum pair cancellation="+maximumPairError);
    }
    private InteractionAudit(){}
}
