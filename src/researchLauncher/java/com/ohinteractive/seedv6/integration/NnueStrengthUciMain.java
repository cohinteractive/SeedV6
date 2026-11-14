package com.ohinteractive.seedv6.integration;

import java.nio.file.Path;
import com.ohinteractive.seedv6.book.Book;
import com.ohinteractive.seedv6.core.nnue.conventional.*;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.driver.BaselineEngine;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.exact.ConstructionSearch;
import com.ohinteractive.seedv6.search.manage.*;
import com.ohinteractive.seedv6.search.tablebase.RootTablebase;
import com.ohinteractive.seedv6.uci.*;

/** Evaluator-only experiment. Both sides receive explicit, unchanged CLV_C1.
 * Never calls the evaluator-identity-dependent automatic search selector. */
public final class NnueStrengthUciMain {
    public static void main(String[] args) throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("network.cnnue");
        boolean phase2,compact;
        try(var input=java.nio.file.Files.newInputStream(Path.of(args[0]))) {
            byte[] magic=input.readNBytes(8);
            phase2=java.util.Arrays.equals(magic,"S6P2N001".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
            compact=java.util.Arrays.equals(magic,"S6P2N002".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        }
        var model=phase2||compact?null:StrengthNetwork.load(Path.of(args[0]));
        var experimental=phase2?Phase2Network.load(Path.of(args[0])):null;
        var small=compact?CompactNetwork.load(Path.of(args[0])):null;
        // Conventional 256-unit files retain the production kernel for training controls.
        SearchEvaluation evaluation=compact?small.evaluation():phase2?experimental.evaluation():model.hidden()==256
                ? SearchEvaluation.conventional(ConventionalNetwork.load(Path.of(args[0]))) : model.evaluation();
        String modelIdentity=compact?"schema=S6P2N002 architecture="+small.architecture()+" hidden=128 heads=8 payload="
                +small.payloadSha256()+" head="+small.backend():phase2?"schema=S6P2N001 architecture="+experimental.architecture()+" hidden=256 heads=8 payload="
                +experimental.payloadSha256()+" head="+experimental.backend():"schema="+model.schema()+" hidden="+model.hidden()
                +" heads="+model.outputBuckets()+" payload="+model.payloadSha256()+" head="+model.headBackend();
        var configuration=new UciConfiguration() {
            ConstructionSearch config(int workers,int hash) {
                return BaselineEngine.configuration(workers,hash,BaselineEngine.SearchProfile.CLV_C1);
            }
            public ClockPolicy clockPolicy(){return ClockPolicy.BOUNDARY2;}
            public SearchLifecycleService service(int workers,int hash) {
                return new SearchLifecycleService(TimeSource.SYSTEM,()->clockPolicy().apply(config(workers,hash).create(evaluation)),
                        RootTablebase.NONE,Book.EMPTY);
            }
            public boolean standalone(){return true;}
            public boolean acceptsWorkers(int n){return n==1||n==2;}
            public String name(){return "SeedV6 NNUE strength research";}
            public String identity(int workers){return identity(workers,32);}
            public String identity(int workers,int hash) {
                return "mode=research formulation=CLV_C1 "+modelIdentity
                        +" scoreMapping=BT4_Q_LOGIT400_V1 flags="+config(workers,hash).mechanisms().flags()
                        +" configuration="+config(workers,hash).identity()+" clockPolicy="+clockPolicy().identity()
                        +" book=OFF tablebase=OFF";
            }
        };
        try(var engine=new UciEngine(System.in,System.out,System.err,configuration)){engine.run();}
    }
}
