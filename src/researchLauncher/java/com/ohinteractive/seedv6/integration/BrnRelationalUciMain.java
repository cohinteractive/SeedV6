package com.ohinteractive.seedv6.integration;

import java.nio.file.Path;
import com.ohinteractive.seedv6.book.Book;
import com.ohinteractive.seedv6.core.brnpair2.ProductionModel;
import com.ohinteractive.seedv6.core.brnpair2.BrnPair2Codec;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HexFormat;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.driver.AspirationSearch;
import com.ohinteractive.seedv6.search.driver.ConventionalNnueEngine;
import com.ohinteractive.seedv6.search.driver.BaselineEngine;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.exact.ConstructionSearch;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;
import com.ohinteractive.seedv6.search.tablebase.RootTablebase;
import com.ohinteractive.seedv6.uci.UciConfiguration;
import com.ohinteractive.seedv6.uci.UciEngine;

/** Matched evaluator comparison; never packaged or selected as production. */
public final class BrnRelationalUciMain {
    public static void main(String[] args) throws Exception {
        if ((args.length != 2 && args.length != 3) || !(args[0].equals("cp64") || args[0].equals("brep2") || args[0].equals("pair") || args[0].equals("compose8") || args[0].equals("piece1") || args[0].equals("tied2") || args[0].equals("mix2")))
            throw new IllegalArgumentException("cp64|brep2|pair|compose8|piece1|tied2|mix2 FROZEN_MODEL [common|cp64-profile]");
        String profile=args.length==3?args[2]:"common";
        if(!profile.equals("common")&&!profile.equals("cp64-profile"))throw new IllegalArgumentException("Search profile");
        if (System.getenv("SEED_NNUE_OPERATION") == null)
            throw new IllegalStateException("Use nnue-control.cmd run");
        var model = Path.of(args[1]);
        final SearchEvaluation evaluation;
        if(args[0].equals("cp64"))evaluation=ConventionalNnueEngine.load(model).evaluation();
        else if(args[0].equals("compose8"))evaluation=com.ohinteractive.seedv6.core.brnpair2.ResearchFieldEvaluation.load(model);
        else if(args[0].equals("piece1"))evaluation=com.ohinteractive.seedv6.core.brnpair2.ResearchPieceEvaluation.load(model);
        else if(args[0].equals("tied2"))evaluation=com.ohinteractive.seedv6.core.brnpair2.ResearchTiedEvaluation.load(model);
        else if(args[0].equals("mix2"))evaluation=com.ohinteractive.seedv6.core.brnpair2.ResearchMixedTiedEvaluation.load(model);
        else if(args[0].equals("brep2"))evaluation=SearchEvaluation.brnPair2(ProductionModel.load(model));
        else try(var input=Files.newInputStream(model)){evaluation=SearchEvaluation.brnPair2(BrnPair2Codec.readModel(input));}
        String modelHash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(model)));
        var configuration = new UciConfiguration() {
            public SearchLifecycleService service(int workers, int hash) {
                if (workers != 1) throw new IllegalArgumentException("One worker only");
                var search = profile.equals("cp64-profile")?BaselineEngine.configuration(1,hash)
                        :ConstructionSearch.v1(1, hash, AspirationSearch.Policy.ADAPTIVE16);
                return new SearchLifecycleService(TimeSource.SYSTEM, () -> search.create(evaluation),
                        RootTablebase.NONE, Book.EMPTY);
            }
            public boolean standalone() { return true; }
            public boolean acceptsWorkers(int workers) { return workers == 1; }
            public int maximumWorkers() { return 1; }
            public String name() { return "SeedV6 BRN relational research " + args[0]; }
            public String identity(int workers) { return identity(workers, 64); }
            public String identity(int workers, int hash) {
                return "mode=research workstream=seedv6-brn-relational-20261029 evaluator=" + args[0]
                        + " modelSha256=" + modelHash
                        + " profile=" + profile
                        + " configuration=" + (profile.equals("cp64-profile")?BaselineEngine.configuration(1,hash)
                            :ConstructionSearch.v1(1, hash, AspirationSearch.Policy.ADAPTIVE16)).identity()
                        + " book=OFF tablebase=OFF";
            }
        };
        try (var engine = new UciEngine(System.in, System.out, System.err, configuration)) { engine.run(); }
    }
    private BrnRelationalUciMain() {}
}
