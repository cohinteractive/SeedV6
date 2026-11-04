package com.ohinteractive.seedv6.integration;

import java.nio.file.Path;
import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.book.Book;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.exact.*;
import com.ohinteractive.seedv6.search.manage.*;
import com.ohinteractive.seedv6.search.tablebase.RootTablebase;
import com.ohinteractive.seedv6.search.tt.TTable;
import com.ohinteractive.seedv6.uci.*;

/** Explicit MW-R001 binding, excluded from production distributions. */
public final class P4MultiworkerUciMain {
    public static void main(String[] args) throws Exception {
        if (args.length != 4 || !args[0].equals("--network") || !args[2].equals("--architecture"))
            throw new IllegalArgumentException("--network <CP64> --architecture ROOT_REUSE|WHOLE_ROOT_RACE|WHOLE_ROOT_SHARING|EAGER_ROOT");
        String architecture = args[3];
        if (!architecture.equals("ROOT_REUSE") && !architecture.equals("WHOLE_ROOT_RACE")
                && !architecture.equals("WHOLE_ROOT_SHARING") && !architecture.equals("EAGER_ROOT"))
            throw new IllegalArgumentException("Unknown MW architecture");
        var model = ConventionalNnueEngine.load(Path.of(args[1])); var clocks = ClockPolicy.configured();
        var configuration = new UciConfiguration() {
            ConstructionSearch config(int workers, int hash) {
                return BaselineEngine.configuration(workers, hash, BaselineEngine.SearchProfile.TC_P1);
            }
            public ClockPolicy clockPolicy() { return clocks; }
            public SearchLifecycleService service(int workers, int hash) {
                return new SearchLifecycleService(TimeSource.SYSTEM, () -> {
                    var c = config(workers, hash);
                    return clocks.apply(workers == 1 || architecture.equals("ROOT_REUSE") ? c.create(model.evaluation())
                            : new AspirationSearch(architecture.equals("WHOLE_ROOT_SHARING")
                                    ? ParallelSearch.constructionWholeRootSharingResearch(workers,model.evaluation(),new TTable(hash),c.mechanisms())
                                    : architecture.equals("EAGER_ROOT")
                                    ? ParallelSearch.constructionEagerRootResearch(workers,model.evaluation(),new TTable(hash),c.mechanisms())
                                    : ParallelSearch.constructionWholeRootResearch(workers,model.evaluation(),new TTable(hash),c.mechanisms()),c.aspiration()));
                }, RootTablebase.NONE, Book.EMPTY);
            }
            public boolean standalone() { return true; }
            public boolean acceptsWorkers(int n) { return n >= 1 && n <= 16; }
            public String name() { return "SeedV6 MW-R001 " + architecture; }
            public String identity(int workers) { return identity(workers, 64); }
            public String identity(int workers, int hash) {
                var version = ApplicationVersion.load();
                String actual = workers == 1 ? "OWNER" : architecture;
                return "mode=research experimental=true formulation=" + (architecture.equals("ROOT_REUSE") ? "TC-P1"
                        : architecture.equals("WHOLE_ROOT_SHARING") ? "MW-C2" : architecture.equals("EAGER_ROOT") ? "MW-C3" : "MW-C1")
                        + " architecture=" + actual + " evaluator=CONVENTIONAL_NNUE checkpoint=64 networkSha256="
                        + ConventionalNnueEngine.SHA256 + " schema=" + model.model().schema()
                        + " heads=" + model.model().outputBuckets() + " head=" + model.model().headBackend()
                        + " scoreMapping=" + ConventionalNnueEngine.SCORE_MAPPING
                        + " flags=" + config(workers, hash).mechanisms().flags()
                        + " configuration=workers=" + workers + ";hashMiB=" + hash + ";aspiration=ADAPTIVE16;parallel=" + actual
                        + " learning=" + (workers>1 && architecture.equals("WHOLE_ROOT_SHARING") ? "completed-winner-private-copies" : "request-worker-private")
                        + " ttPolicy=production-hints slotBytes=24 clockPolicy=" + clocks.identity()
                        + " book=OFF tablebase=OFF build=" + version.build() + " source=" + version.sourceRevision();
            }
        };
        try (var engine = new UciEngine(System.in, System.out, System.err, configuration)) { engine.run(); }
    }
    private P4MultiworkerUciMain() {}
}
