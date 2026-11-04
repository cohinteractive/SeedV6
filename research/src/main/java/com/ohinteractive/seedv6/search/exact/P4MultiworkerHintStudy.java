package com.ohinteractive.seedv6.search.exact;

import java.nio.file.*;
import java.util.*;
import com.google.gson.GsonBuilder;
import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.*;
import com.ohinteractive.seedv6.search.manage.ClockPolicy;
import com.ohinteractive.seedv6.search.tt.TTable;

/** MW-R001 provenance/delivery diagnostic. The extra monitor deliberately makes
 * provenance coherent with the returned hint; this is not a performance probe.
 * PRIVATE_DELIVERY keeps the same shared storage/replacement but suppresses hints
 * last produced by another worker. It is not a worker-private TT architecture. */
public final class P4MultiworkerHintStudy {
    enum Delivery { ALL, PRIVATE_DELIVERY }

    static final class HintTable extends TTable {
        record Producer(int worker, long move) {}
        final Delivery delivery;
        final ThreadLocal<Integer> worker = new ThreadLocal<>();
        final Map<Long, Producer> producers = new HashMap<>();
        final long[][] cross;
        long probes, available, stores, acceptedStores, sameWorker, otherWorker, deliveredOther;

        HintTable(int workers, int hashMiB, Delivery delivery) {
            super(hashMiB); this.delivery = delivery; cross = new long[workers][workers];
        }
        void bind(int id) {
            if (id < 0 || id >= cross.length) throw new IllegalArgumentException("Worker identity");
            worker.set(id);
        }
        int caller() {
            var id = worker.get();
            if (id == null) throw new AssertionError("Unbound TT caller");
            return id;
        }
        @Override public synchronized boolean saveHint(long key, int depth, long move) {
            int id = caller(); stores++;
            boolean saved = super.saveHint(key, depth, move);
            if (saved) { acceptedStores++; producers.put(key, new Producer(id, move)); }
            return saved;
        }
        @Override public synchronized long probeHint(long key) {
            int id = caller(); probes++;
            long move = super.probeHint(key);
            if (move == 0) return 0;
            available++;
            var source = producers.get(key);
            if (source == null || source.move() != move) throw new AssertionError("Missing hint provenance");
            cross[source.worker()][id]++;
            if (source.worker() == id) sameWorker++;
            else {
                otherWorker++;
                if (delivery == Delivery.PRIVATE_DELIVERY) return 0;
                deliveredOther++;
            }
            return move;
        }
        @Override public void save(long key, int depth, int type, int score, long move) {
            throw new AssertionError("Unexpected ordinary score store in exact TC-P1 diagnostic");
        }
        synchronized Map<String, Object> snapshot() {
            return Map.of("probes", probes, "available", available, "stores", stores,
                    "acceptedStores", acceptedStores, "sameWorker", sameWorker, "otherWorker", otherWorker,
                    "deliveredOther", deliveredOther, "producerConsumer", cross,
                    "provenanceKeys", producers.size());
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 8) throw new IllegalArgumentException("model corpus output workers rounds depth node-cap delivery");
        int workers = Integer.parseInt(args[3]), rounds = Integer.parseInt(args[4]), depth = Integer.parseInt(args[5]);
        long cap = Long.parseLong(args[6]); var delivery = Delivery.valueOf(args[7]);
        if ((workers != 2 && workers != 4) || rounds < 1 || rounds > 4 || depth < 3 || depth > 16
                || cap < 1 || cap > 1_000_000) throw new IllegalArgumentException("Unbounded diagnostic");
        var model = ConventionalNnueEngine.load(Path.of(args[0]));
        var roots = P3HistoryStudy.roots(Path.of(args[1]));
        if (roots.isEmpty() || roots.size() > 64) throw new IllegalArgumentException("Root count");
        var config = BaselineEngine.configuration(workers, 64, BaselineEngine.SearchProfile.TC_P1).mechanisms();
        var gson = new GsonBuilder().create(); long totalNodes = 0; int requests = 0;
        try (var out = Files.newBufferedWriter(Path.of(args[2]), StandardOpenOption.CREATE_NEW)) {
            out.write(gson.toJson(Map.of("type", "identity", "source", ApplicationVersion.load().sourceRevision(),
                    "build", ApplicationVersion.load().build(), "flags", config.flags(), "args", args,
                    "delivery", delivery, "scope", "Provenance monitor changes scheduling; delivery is not measured legal ordering consumption or strength")));
            out.newLine(); out.flush();
            for (int round = 0; round < rounds; round++) for (var root : roots) {
                var table = new HintTable(workers, 64, delivery);
                var counts = new P4MultiworkerStudy.Admissions[workers];
                var owner = ParallelSearch.constructionResearch(workers, model.evaluation(), table, config, i -> {
                    var delegate = ExactEvaluator.from(model.evaluation());
                    return counts[i] = new P4MultiworkerStudy.Admissions(new ExactEvaluator() {
                        public void initialize(long[] b) { table.bind(i); delegate.initialize(b); }
                        public void child(long[] p, long[] c, int ply) { delegate.child(p, c, ply); }
                        public void child(long[] p, long[] c, int ply, int qply) { delegate.child(p, c, ply, qply); }
                        public void reenter(int ply) { delegate.reenter(ply); }
                        public int evaluate(long[] b, int ply) { return delegate.evaluate(b, ply); }
                    });
                });
                var saved = root.board().clone();
                try (var driver = new SearchDriver(ClockPolicy.BOUNDARY2.apply(
                        new AspirationSearch(owner, AspirationSearch.Policy.ADAPTIVE16)))) {
                    var measurement = ConstructionTtStudy.request(driver, root, depth, cap);
                    var result = (SearchDriverOutcome)measurement.get("outcome");
                    if (result.nodes() != Arrays.stream(counts).mapToLong(a -> a.nodes).sum()
                            || !Arrays.equals(saved, root.board())) throw new AssertionError("Accounting/restoration");
                    totalNodes += result.nodes(); requests++;
                    out.write(gson.toJson(Map.of("type", "request", "root", root.id(), "round", round,
                            "measurement", measurement, "hints", table.snapshot())));
                    out.newLine(); out.flush();
                }
            }
            out.write(gson.toJson(Map.of("type", "summary", "status", "passed", "requests", requests,
                    "nodes", totalNodes, "activeHelpersAfterClose", Thread.getAllStackTraces().keySet().stream()
                            .filter(t -> t.isAlive() && t.getName().equals("seedv6-search-helper")).count())));
            out.newLine();
        }
    }
    private P4MultiworkerHintStudy() {}
}
