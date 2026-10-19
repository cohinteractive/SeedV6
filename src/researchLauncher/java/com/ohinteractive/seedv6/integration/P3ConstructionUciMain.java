package com.ohinteractive.seedv6.integration;

import java.nio.file.Path;
import com.ohinteractive.seedv6.core.brnpair2.ProductionModel;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.driver.AspirationSearch;
import com.ohinteractive.seedv6.search.exact.*;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;
import com.ohinteractive.seedv6.uci.*;

/** Explicit Workstream 3 executable, owned by the approved research launcher seam.
 * Never included in GUI/UCI production distributions or selected by their factories. */
public final class P3ConstructionUciMain {
    public static void main(String[] args) throws Exception {
        if(args.length!=2 || !args[0].equals("--model"))
            throw new IllegalArgumentException("Usage: P3ConstructionUciMain --model <pinned generation-32 payload>");
        var evaluation=SearchEvaluation.brnPair2(ProductionModel.load(Path.of(args[1])));
        var base=switch(System.getProperty("seedv6.construction.profile","rich")) {
            case "rich" -> FlatSearchConfig.candidate();
            case "policy4" -> FlatSearchConfig.policy4();
            case "exact" -> FlatSearchConfig.exact();
            default -> throw new IllegalArgumentException("Unknown construction profile");
        };
        long flags=base.flags();
        for(String name:System.getProperty("seedv6.construction.disable","").split(","))
            if(!name.isBlank()) flags&=~(1L<<FlatSearchConfig.Feature.valueOf(name.trim()).ordinal());
        var config=new FlatSearchConfig(flags,Integer.parseInt(System.getProperty("seedv6.construction.futilityMargin",Integer.toString(base.futilityMargin()))),
                Integer.parseInt(System.getProperty("seedv6.construction.reverseMargin",Integer.toString(base.reverseMargin()))));
        String selected=System.getProperty("seedv6.construction.aspiration","WIDEN16");
        var aspiration=selected.equals("OFF")?null:AspirationSearch.Policy.valueOf(selected);
        var configuration=new UciConfiguration() {
            @Override public SearchLifecycleService service(int workers,int hashMiB) {
                var frozen=new ConstructionSearch(config,workers,hashMiB,aspiration);
                return new SearchLifecycleService(TimeSource.SYSTEM,()->frozen.create(evaluation));
            }
            @Override public boolean standalone(){return true;}
            @Override public boolean acceptsWorkers(int count){return count>=1 && count<=16;}
            @Override public String name(){return "SeedV6 Construction V1";}
            @Override public String identity(int workers) {
                return "mode=research experimental=true model.sha256="+ProductionModel.SHA256
                        +" model.checkpoint="+ProductionModel.CHECKPOINT
                        +" mapping=BRN_PAIR2_MATERIAL_RESIDUAL_QUARTER_V1 workers="+workers
                        +" aspiration="+selected+" configuration="+config.identity()+" warmup=off";
            }
        };
        new UciEngine(System.in,System.out,System.err,configuration).run();
    }
    private P3ConstructionUciMain() {}
}
