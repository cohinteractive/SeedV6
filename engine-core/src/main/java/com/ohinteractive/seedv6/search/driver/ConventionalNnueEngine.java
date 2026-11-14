package com.ohinteractive.seedv6.search.driver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import com.ohinteractive.seedv6.book.Book;
import com.ohinteractive.seedv6.core.nnue.conventional.ConventionalNetwork;
import com.ohinteractive.seedv6.search.common.TimeSource;
import com.ohinteractive.seedv6.search.evaluation.SearchEvaluation;
import com.ohinteractive.seedv6.search.exact.SeePolicy;
import com.ohinteractive.seedv6.search.manage.SearchLifecycleService;
import com.ohinteractive.seedv6.search.tablebase.RootTablebase;
import com.ohinteractive.seedv6.search.tt.TTable;

/** Verified conventional model binding shared by desktop and UCI.
 * The no-argument loader and E008 entry point retain the historical CP64 identity.
 * Only immutable network tables are shared; each service owns its search, TT and accumulators. */
public final class ConventionalNnueEngine {
    public static final String NETWORK = "/models/conventional-cp64.cnnue";
    public static final int NETWORK_BYTES = 3_940_952;
    public static final String SHA256 = "8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b";
    public static final String PROFILE = "E008_QXRAY_LMR_WINDOW_EFFORT_HINTS";
    /** CP64's frozen teacher-link target scale; UCI cp is the reporting convention. */
    public static final String SCORE_MAPPING = "BT4_Q_LOGIT400_V1";
    public enum Model {
        CP64(NETWORK, SHA256, "5da0253642ab33b8e446b5b1c5764750be008cd3dbacc7cd1992a95bf4a881f6"),
        WDL25("/models/conventional-wdl25.cnnue",
                "a25e76fcdfe1b854452685bec87bc759bed2ed166312579e024410f0903517ab",
                "efdf8fef2ab863fcf7798df9bb8b2ea0b4958ab6af14deaa59d568dded1815c7");
        private final String resource, sha256, payloadSha256;
        Model(String resource, String sha256, String payloadSha256) {
            this.resource = resource; this.sha256 = sha256; this.payloadSha256 = payloadSha256;
        }
        public String resource() { return resource; }
        public String sha256() { return sha256; }
        public String payloadSha256() { return payloadSha256; }
    }
    private final Model selection;
    private final ConventionalNetwork model;
    private final SearchEvaluation evaluation;

    private ConventionalNnueEngine(Model selection, ConventionalNetwork model) {
        this.selection = selection;
        this.model = model;
        evaluation = SearchEvaluation.conventional(model);
    }

    public static ConventionalNnueEngine load() throws IOException { return load(null); }
    public static ConventionalNnueEngine load(Path override) throws IOException {
        try (var input = override == null ? ConventionalNnueEngine.class.getResourceAsStream(NETWORK)
                : Files.newInputStream(override)) {
            return new ConventionalNnueEngine(Model.CP64, readPinned(input));
        }
    }

    public static ConventionalNetwork readPinned(InputStream input) throws IOException {
        return readPinned(Model.CP64, input);
    }
    public static ConventionalNnueEngine loadBundled(Model selection) throws IOException {
        try (var input = ConventionalNnueEngine.class.getResourceAsStream(selection.resource())) {
            return new ConventionalNnueEngine(selection, readPinned(selection, input));
        }
    }
    public static ConventionalNetwork readPinned(Model selection, InputStream input) throws IOException {
        if (input == null) throw new IOException("Missing required checkpoint-64 NNUE asset " + selection.resource());
        byte[] bytes = input.readNBytes(NETWORK_BYTES + 1);
        final String hash;
        try { hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
        if (bytes.length != NETWORK_BYTES || !selection.sha256().equals(hash))
            throw new IOException("Required checkpoint-64 NNUE length/SHA-256 mismatch");
        var model = ConventionalNetwork.read(new ByteArrayInputStream(bytes));
        if (!model.schema().equals("S6CNU002") || model.outputBuckets() != 8
                || !selection.payloadSha256().equals(model.payloadSha256()))
            throw new IOException("Required S6CNU002 eight-head network is incompatible");
        return model;
    }

    public ConventionalNetwork model() { return model; }
    public Model selection() { return selection; }
    public SearchEvaluation evaluation() { return evaluation; }

    /** The frozen E008 recursive composition, also available to full-rebuild oracle checks. */
    public static ExactSearchAdapter search(SearchEvaluation evaluation, int hashMiB) {
        if (!evaluation.isConventional()) throw new IllegalArgumentException("Conventional NNUE required");
        if (hashMiB < 1 || hashMiB > 1024) throw new IllegalArgumentException("Hash must be 1..1024 MiB");
        return ExactSearchAdapter.seeResearch(evaluation, SeePolicy.QXRAY, null, new TTable(hashMiB))
                .withCompletedHints(true, null);
    }

    public SearchLifecycleService service(int workers, int hashMiB) {
        if (workers != 1) throw new IllegalArgumentException("Checkpoint-64 E008 requires exactly one search worker");
        return new SearchLifecycleService(TimeSource.SYSTEM, () -> search(evaluation, hashMiB),
                RootTablebase.NONE, Book.bundled());
    }

    public String identity(int hashMiB) {
        return "evaluator=CONVENTIONAL_NNUE checkpoint=64 schema=" + model.schema() + " heads=" + model.outputBuckets()
                + " networkSha256=" + selection.sha256() + " payload=" + model.payloadSha256()
                + " head=" + model.headBackend() + " scoreMapping=" + SCORE_MAPPING + " search=" + PROFILE
                + " execution=recursive-pvs qsearch=QXRAY lmr=QUIET lmp=WINDOW reuse=EFFORT"
                + " hints=completed_same_key history=invocation learning=all_searched"
                + " aspiration=OFF futility=OFF reverseFutility=OFF nullMove=OFF extensions=OFF"
                + " workers=1 hashMiB=" + hashMiB;
    }
}
