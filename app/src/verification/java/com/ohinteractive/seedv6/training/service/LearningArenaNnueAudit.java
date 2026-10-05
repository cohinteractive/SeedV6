package com.ohinteractive.seedv6.training.service;

import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Features;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.validation.ValidationArena;
import com.ohinteractive.seedv6.tools.nnue.cglhw.*;
import java.nio.file.*;
import java.util.*;

/** Read-only campaign diagnosis. No service run/resume, source-volume reads or checkpoint writes. */
public final class LearningArenaNnueAudit {
    public static void main(String[] args)throws Exception {
        Path root=Path.of(args[0]),out=Path.of(args[1]);Files.createDirectory(out);
        var state=LearningArenaState.read(root);var config=state.config();
        var json=new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        var positions=new LinkedHashMap<String,long[]>();
        var start=Board.fromFen(config.arena().startingFen());
        positions.put("campaign-start",start);
        for(int i=0;i<Math.min(4,config.arena().games()/2);i++) {
            var opening=ValidationArena.opening(start,GameHistory.initial(start),config.arena().matches(config.seed()),i);
            if(!opening.identity().equals(state.history().getFirst().pairs().get(i).openingHash()))throw new AssertionError("Opening replay mismatch");
            positions.put("campaign-opening-"+i,opening.board());
        }
        var tranche=LearningArenaTranche.read(root.resolve("rounds/000001/tranche"),config,0);
        for(int i=0;i<4;i++)positions.put("frozen-training-position-"+i,tranche.rows.get(i).position().toBoard(0));
        positions.put("white-extra-queen",Board.fromFen("4k3/8/8/8/8/8/Q7/4K3 w - - 0 1"));
        positions.put("black-stm-white-extra-queen",Board.fromFen("4k3/8/8/8/8/8/Q7/4K3 b - - 0 1"));
        var rows=new ArrayList<Object>();
        for(var round:List.of(state.history().getFirst(),state.current())) {
            var checkpoint=CheckpointStore.readSnapshot(root.resolve("A"),round.a().checkpoint());
            var model=(NetworkModel.Nnue)checkpoint.model();var network=model.network();
            var arena=model.evaluation(NnueScoreMapping.V1).newState(1);
            var direct=SearchEvaluation.incremental(network,NnueScoreMapping.V1).newState(1);
            var parity=SearchEvaluation.incrementalWithMaterial(network,NnueScoreMapping.V1).newState(1);
            var neural=new NnueEvaluator(network);
            var knownGood=round.number()==0?new MaterialBootstrapModel(new HalfKpHead(config.a().seed(),false,false)).worker(1):null;
            for(var entry:positions.entrySet()) {
                var board=entry.getValue();arena.initialize(board,0);direct.initialize(board,0);parity.initialize(board,0);
                double raw=neural.evaluate(board);int legacyScore=arena.evaluate(board,0),corrected=parity.evaluate(board,0);
                if(legacyScore!=direct.evaluate(board,0))throw new AssertionError("Legacy construction mismatch");
                if(knownGood!=null){knownGood.refresh(board,0);if(corrected!=knownGood.evaluate(board,0))throw new AssertionError("CGLHW parity mismatch");}
                var row=new LinkedHashMap<String,Object>();row.put("round",round.number());row.put("checkpoint",checkpoint.manifest().id());
                row.put("optimizerStep",checkpoint.manifest().optimizerStep());row.put("position",entry.getKey());row.put("fen",Fen.fromBoard(board));
                row.put("sideToMove",Board.player((int)board[4]));row.put("referenceMaterialPawns",Brn3Features.material(board));
                row.put("rawNeuralOutput",raw);row.put("boundedNeuralOutcome",neural.boundedValue());row.put("arenaFinalScore",legacyScore);
                row.put("cglhwMaterialAblationScore",corrected);row.put("materialEnabledInArena",false);
                row.put("trainedCheckpointAblationIsNotCompatibleMigration",round.number()>0);rows.add(row);
            }
        }
        var report=new LinkedHashMap<String,Object>();report.put("campaign",root.toString());report.put("binding",state.binding());
        report.put("status",state.status());report.put("config",config);report.put("recipe",LearningArenaTraining.recipe(config.a().architecture()));
        report.put("sourceTrancheHash",tranche.manifest.hash());
        var rounds=new ArrayList<Object>();for(var round:state.history())if(round.arenaComplete()) {
            var stats=round.result(config).statistics();rounds.add(Map.of("round",round.number(),"wins",stats.wins(),"draws",stats.draws(),"losses",stats.losses()));
        }
        report.put("completedRounds",rounds);report.put("scores",rows);
        Files.writeString(out.resolve("audit.json"),json.toJson(report),StandardOpenOption.CREATE_NEW);
        System.out.println("Verified campaign "+state.id()+"; "+rows.size()+" score decompositions; legacy Arena equals legacy search; Gen0 parity ablation equals CGLHW.");
    }
}
