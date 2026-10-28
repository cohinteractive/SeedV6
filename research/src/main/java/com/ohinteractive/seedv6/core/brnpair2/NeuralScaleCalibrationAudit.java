package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.ToDoubleFunction;

/** Bounded training-only scalar fit; all neural states remain immutable. */
public final class NeuralScaleCalibrationAudit {
    private static double[] predict(ToDoubleFunction<long[]> predictor,PairCoverageDiagnostic.Data data)throws java.io.IOException {
        long[] board=new long[6];double[] logits=new double[data.rows];
        for(int i=0;i<data.rows;i++){if(i%32768==0)PairObjectiveDiagnostic.permission();data.board(i,board);logits[i]=predictor.applyAsDouble(board);}return logits;
    }
    public static void main(String[] args)throws Exception {
        PairObjectiveDiagnostic.permission();Path configPath=Path.of(args[0]);var c=JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
        Path trainPath=Path.of(c.get("cases").getAsString()),validationPath=Path.of(c.get("validation").getAsString()),originalPath=Path.of(c.get("original_validation").getAsString()),out=Path.of(c.get("output").getAsString());
        for(var pair:List.of(new String[]{"cases","cases_sha256"},new String[]{"validation","validation_sha256"},new String[]{"original_validation","original_validation_sha256"}))
            if(!PairCoverageDiagnostic.sha(Path.of(c.get(pair[0]).getAsString())).equals(c.get(pair[1]).getAsString()))throw new IllegalArgumentException("Data identity");
        long start=System.nanoTime();var train=PairCoverageDiagnostic.Data.read(trainPath,c.get("rows").getAsInt());var validation=PairCoverageDiagnostic.Data.read(validationPath,4096);var original=PairCoverageDiagnostic.Data.read(originalPath,4096);
        if(!Arrays.equals(validation.boards,original.boards))throw new IllegalArgumentException("Validation row identity");var models=new ArrayList<Object>();
        for(var element:c.getAsJsonArray("models")) {
            PairObjectiveDiagnostic.permission();var spec=element.getAsJsonObject();Path state=Path.of(spec.get("state").getAsString());String stateHash=spec.get("state_sha256").getAsString();if(!PairCoverageDiagnostic.sha(state).equals(stateHash))throw new IllegalArgumentException("Model identity");
            var predictor=FieldPredictionAudit.load(state,"COMPOSE8");double[] logits=predict(predictor,train);double baseline=NeuralScaleCalibration.mse(logits,train.targets,1);
            if(Math.abs(baseline-spec.get("expected_train_mse").getAsDouble())>1e-13)throw new IllegalArgumentException("Frozen training function mismatch");
            var fit=NeuralScaleCalibration.fit(logits,train.targets);double[] dev=predict(predictor,validation);double validationMse=NeuralScaleCalibration.mse(dev,validation.targets,1);
            if(Math.abs(validationMse-spec.get("expected_validation_mse").getAsDouble())>1e-13)throw new IllegalArgumentException("Frozen development function mismatch");
            var result=new LinkedHashMap<String,Object>();result.put("id",spec.get("id").getAsString());result.put("state_sha256",stateHash);result.put("scale",fit.scale());result.put("boundary",fit.boundary());result.put("objective_evaluations",fit.evaluations());
            result.put("baseline_training_mse",baseline);result.put("calibrated_training_mse",fit.loss());result.put("baseline_validation_mse",validationMse);result.put("calibrated_validation_mse",NeuralScaleCalibration.mse(dev,validation.targets,fit.scale()));
            result.put("baseline_bt4_mse",NeuralScaleCalibration.mse(dev,original.targets,1));result.put("calibrated_bt4_mse",NeuralScaleCalibration.mse(dev,original.targets,fit.scale()));models.add(result);
            if(!PairCoverageDiagnostic.sha(state).equals(stateHash))throw new IllegalArgumentException("Base state changed");
        }
        var result=Map.of("config_sha256",PairCoverageDiagnostic.sha(configPath),"rows",train.rows,"models",models,"seconds",(System.nanoTime()-start)/1e9,
                "scope","One positive scale fitted on original training labels;no neural weights changed;no search/promotion/strength claim");
        Files.writeString(out,new GsonBuilder().setPrettyPrinting().create().toJson(result)+"\n",StandardOpenOption.CREATE_NEW);System.out.println("Completed training-only scalar fits: "+models.size());
    }
    private NeuralScaleCalibrationAudit(){}
}
