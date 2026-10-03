package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.training.data.DataFiles;
import java.nio.file.*;
import java.util.*;

/** Diagnostic only: probes label calibration. Does not change the fixed prior. */
public final class BrnResearchCalibration {
    public static void main(String[] args) throws Exception {
        var data=BrnResearchData.read(Path.of(args[0]),false);var rows=new ArrayList<Map<String,Object>>();
        for(double scale:new double[]{0,.1,.2,.3,.5,.75,1,1.25,1.5,2}){
            var row=new LinkedHashMap<String,Object>();row.put("materialScaleProbe",scale);
            row.put("training",BrnResearchMain.metrics(data.training(),e->scale*RelationalCandidate.material(e.board()),true));
            row.put("validation",BrnResearchMain.metrics(data.validation(),e->scale*RelationalCandidate.material(e.board()),true));rows.add(row);
        }
        var strata=new ArrayList<Map<String,Object>>();
        for(int bucket=0;bucket<4;bucket++){
            int selected=bucket;var records=data.validation().stream().filter(e->{double a=Math.abs(RelationalCandidate.material(e.board()));return (a<.5?0:a<2?1:a<5?2:3)==selected;}).toList();
            strata.add(Map.of("absMaterialBucket",new String[]{"under .5",".5 to 2","2 to 5","5 or more"}[bucket],"metrics",BrnResearchMain.metrics(records,e->RelationalCandidate.material(e.board()),true)));
        }
        var report=Map.of("diagnosticOnly",true,"scaleProbes",rows,"materialStrata",strata);
        DataFiles.write(Path.of(args[1]),report);System.out.println(DataFiles.JSON.toJson(report));
    }
}
