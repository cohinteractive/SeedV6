package com.ohinteractive.seedv6.training.service;

import com.google.gson.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Features;
import com.ohinteractive.seedv6.core.nnue.NnueEvaluator;
import com.ohinteractive.seedv6.search.evaluation.NnueScoreMapping;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.data.DataSource;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.tools.nnue.cglhw.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static com.ohinteractive.seedv6.training.service.LearningArenaConfig.*;

/** Real production Arena round zero; isolated new root, fixed settings, no strength tuning. */
public final class LearningArenaMaterialSmoke {
    public static void main(String[] args) throws Exception {
        if(args.length!=3)throw new IllegalArgumentException("NEW_STATE_ROOT NEW_REPORT_DIR DIAGNOSIS_JSON");
        Path root=Path.of(args[0]),out=Path.of(args[1]);Files.createDirectory(out);
        Path source=out.resolve("fixture.jsonl").toAbsolutePath();
        Files.writeString(source,"{\"fen\":\"4k3/8/8/8/3pP3/8/8/4K3 w - - 0 1\",\"evals\":[{\"depth\":20,\"pvs\":[{\"cp\":100}]}]}\n",StandardOpenOption.CREATE_NEW);
        var config=new LearningArenaConfig("E012 integration smoke",new Competitor("NNUE material parity",TrainingArchitecture.NNUE_MATERIAL,71,128),
                new Competitor("BRN-3",TrainingArchitecture.BRN3,71,128),DataSource.register("Unused round-zero fixture",source,1),
                1,1,0,71,new Arena(8,Limit.DEPTH,3,100,1,0,8,1024,TrainerConfig.STANDARD_START));
        var owner=new AtomicReference<LearningArenaService>();
        try(var service=LearningArenaService.create(root,config,u->{
            if(u.state().current().stage()==LearningArenaState.Stage.ROUND_COMPLETE)owner.get().pause();
        })) {owner.set(service);service.run();}
        var state=LearningArenaState.read(root);var round=state.current();
        if(round.number()!=0||!round.arenaComplete())throw new AssertionError("Round-zero completion");
        var model=(NetworkModel.NnueMaterial)CheckpointStore.readSnapshot(root.resolve("A"),round.a().checkpoint()).model();
        var arena=model.evaluation(NnueScoreMapping.V1).newState(1);
        var cglhw=new MaterialBootstrapModel(new HalfKpHead(71,false,false)).worker(1);
        var neural=new NnueEvaluator(model.network());var scores=new ArrayList<Object>();
        var diagnosis=JsonParser.parseString(Files.readString(Path.of(args[2]))).getAsJsonObject();
        for(var entry:diagnosis.getAsJsonArray("scores")) {
            var old=entry.getAsJsonObject();if(old.get("round").getAsInt()!=0)continue;
            String fen=old.get("fen").getAsString();var board=Board.fromFen(fen);
            arena.initialize(board,0);cglhw.refresh(board,0);int score=arena.evaluate(board,0);
            if(score!=cglhw.evaluate(board,0)||score!=old.get("cglhwMaterialAblationScore").getAsInt())throw new AssertionError("Cross-path score mismatch: "+fen);
            double raw=neural.evaluate(board);scores.add(Map.of("fen",fen,"rawNeural",raw,"neuralScore",NnueScoreMapping.V1.map(neural.boundedValue()),
                    "materialPawns",Brn3Features.material(board),"arenaFinal",score,"cglhwFinal",cglhw.evaluate(board,0)));
        }
        var report=new LinkedHashMap<String,Object>();report.put("purpose","Software integration smoke, not a trained strength estimate");
        report.put("campaign",root.toString());report.put("config",config);report.put("recipe",LearningArenaTraining.recipe(config.a().architecture()));
        report.put("binding",state.binding());report.put("round",round);report.put("statistics",round.result(config).statistics());report.put("crossPathScores",scores);
        Files.writeString(out.resolve("report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report),StandardOpenOption.CREATE_NEW);
        System.out.println("E012 material Arena smoke: "+round.result(config).statistics()+"; "+scores.size()+" exact CGLHW/Arena score matches.");
    }
}
