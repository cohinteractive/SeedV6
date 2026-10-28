package com.ohinteractive.seedv6.core.brnpair2;

import com.google.gson.Gson;
import java.nio.file.*;
import java.util.*;

/** Compare frozen compiled pair payloads on one consumed diagnostic panel. */
public final class PairPredictionDiagnostic {
    public static void main(String[] args)throws Exception {
        var data=PairObjectiveDiagnostic.read(Path.of(args[0]));var results=new ArrayList<Object>();
        for(int m=1;m<args.length;m++) {
            BrnPair2Model model;try(var in=Files.newInputStream(Path.of(args[m]))){model=BrnPair2Codec.readModel(in);}
            var workspace=model.newWorkspace();double sum=0,maxError=0;
            for(int i=0;i<data.boards().length;i++) {
                double pawns=workspace.evaluatePawns(data.boards()[i],.25);
                maxError=Math.max(maxError,Math.abs(pawns-model.newWorkspace().evaluatePawns(data.boards()[i],.25)));
                double error=PairObjectiveDiagnostic.sigmoid(pawns/4)-data.targets()[i];sum+=error*error;
            }
            if(maxError>1e-8)throw new AssertionError("Full/cache discrepancy");
            results.add(Map.of("model",args[m],"sha256",PairObjectiveDiagnostic.sha(Path.of(args[m])),
                "validation_mse",sum/data.boards().length,"maximum_full_cache_error_pawns",maxError,"positions",data.boards().length));
        }
        System.out.println(new Gson().toJson(results));
    }
    private PairPredictionDiagnostic(){}
}
