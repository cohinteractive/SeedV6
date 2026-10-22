package com.ohinteractive.seedv6.uci;

import java.nio.file.Path;
import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.book.Book;
import com.ohinteractive.seedv6.core.nnue.conventional.ConventionalNetwork;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.driver.ProductionSearch;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;
import com.ohinteractive.seedv6.search.tablebase.RootTablebase;

/** Explicit opt-in model, kept outside the pinned Gen32 default and legacy formats. */
final class ConventionalUciConfiguration {
    static UciConfiguration load(Path path) throws Exception {
        var model=ConventionalNetwork.load(path);
        var evaluation=SearchEvaluation.conventional(model);
        var version=ApplicationVersion.load();
        return new UciConfiguration() {
            public SearchLifecycleService service(int workers,int hashMiB) {
                return new SearchLifecycleService(TimeSource.SYSTEM,
                    ()->ProductionSearch.create(workers,evaluation,hashMiB),RootTablebase.NONE,Book.bundled());
            }
            public boolean standalone(){return true;}
            public boolean acceptsWorkers(int workers){return workers>=1&&workers<=16;}
            public String name(){return "SeedV6 conventional NNUE opt-in "+version.displayVersion();}
            public String identity(int workers){return identity(workers,64);}
            public String identity(int workers,int hashMiB){
                return "evaluator=CONVENTIONAL_NNUE schema="+model.schema()+" payload="+model.payloadSha256()
                    +" head="+model.headBackend()
                    +" search=recursive-unpruned-qsearch workers="+workers+" hashMiB="+hashMiB
                    +" build="+version.build()+" source="+version.sourceRevision();
            }
        };
    }
    private ConventionalUciConfiguration(){}
}
