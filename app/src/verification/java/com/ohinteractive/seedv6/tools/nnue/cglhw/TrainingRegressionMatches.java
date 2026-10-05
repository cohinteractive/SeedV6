package com.ohinteractive.seedv6.tools.nnue.cglhw;

import com.google.gson.Gson;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.Brn3Trainer;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.training.validation.*;
import com.ohinteractive.seedv6.training.nnue.NnueTrainingRegression;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** E013 bounded paired games; evaluator scale is explicit and never changes a checkpoint. */
public final class TrainingRegressionMatches {
    static SearchEvaluation load(String file,double scale)throws Exception {
        if(file.equals("material"))return SearchEvaluation.brn3(new Brn3Trainer(71).snapshot());
        NnueNetwork network;
        if(file.startsWith("seed:"))network=NnueNetwork.initialized(Long.parseLong(file.substring(5)));
        else try(var in=new BufferedInputStream(Files.newInputStream(Path.of(file)))){network=((com.ohinteractive.seedv6.training.model.NetworkModel.NnueMaterial)
                com.ohinteractive.seedv6.training.model.NetworkModel.read(com.ohinteractive.seedv6.training.model.TrainingArchitecture.NNUE_MATERIAL,in)).network();}
        return SearchEvaluation.incrementalWithMaterial(network,new NnueScoreMapping(scale));
    }
    public static void main(String[] args)throws Exception {
        String a=args[0],b=args[1];Path out=Path.of(args[2]);int pairs=Integer.parseInt(args[3]),depth=Integer.parseInt(args[4]);long seed=Long.parseLong(args[5]);double scale=Double.parseDouble(args[6]);
        Files.createDirectory(out);var candidate=load(a,scale);var opponent=load(b,32511);
        NnueTrainingRegression.json(out.resolve("config.json"),Map.of("candidate",a,"opponent",b,"candidateNeuralScale",scale,"opponentNeuralScale",32511,"pairs",pairs,"depth",depth,"seed",seed,"search","CGLHW exact depth, one worker, private 4MiB TT per side/game; no qsearch/book/tablebase; uniform 6..10 opening plies; 1024-ply caps remain unscored"));
        var config=new ValidationConfig(pairs,seed,6,10,depth,1,NnueScoreMapping.V1,1024);var root=Board.startingPosition();var json=new Gson();
        try(var writer=Files.newBufferedWriter(out.resolve("games.jsonl"),StandardOpenOption.CREATE_NEW)) {
            for(int i=0;i<pairs;i++)for(int j=0;j<2;j++) {
                var opening=ValidationArena.opening(root,GameHistory.initial(root),config,i);int colour=(i+j)%2;
                var game=BootstrapAudit.game(opening,()->new SearchDriver(new ExactSearchAdapter(candidate,new TTable(4))),()->new SearchDriver(new ExactSearchAdapter(opponent,new TTable(4))),colour,depth,1024);
                var row=new LinkedHashMap<String,Object>(game);row.put("pair",i);row.put("openingHash",opening.identity());row.put("openingAlreadyTerminal",!opening.newGame(1024).active());writer.write(json.toJson(row));writer.newLine();writer.flush();
                System.out.println("pair "+i+" colour "+colour+" score "+row.get("nnueScore")+" "+row.get("termination")+" plies "+row.get("plies"));
            }
        }
    }
}
