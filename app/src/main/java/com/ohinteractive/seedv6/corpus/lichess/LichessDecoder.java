package com.ohinteractive.seedv6.corpus.lichess;

import java.io.StringReader;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.ohinteractive.seedv6.corpus.*;

/** Lichess evaluated-position JSONL adapter; no corpus IO or model calibration.
 * Official schema/policy: https://database.lichess.org/#evals
 * Scores are White POV (Lichess ui/lib/src/ceval/protocol.ts normalizes UCI POV before caching).
 * Select highest depth, then greatest knodes, then earliest evaluation. Always take its FIRST PV;
 * PVs are already best-first for the side to move, so numeric max(cp) is wrong for Black.
 * Invalid evaluations are skipped; reject the line if no evaluation has a valid first-PV target.
 */
public final class LichessDecoder {
    public static final String ADAPTER = "lichess-evaluations-jsonl-v1";
    public static final String POLICY = "raw-white-cp-or-mate; highest-depth, then highest-knodes, then first-eval; first-PV; equal-quality duplicates keep existing; unknown-halfmove=-1; fullmove excluded";
    private LichessDecoder() {}

    public static CorpusRecord decode(String json, int sourceId, long line) {
        try {
            JsonReader reader = new JsonReader(new StringReader(json));
            reader.setStrictness(Strictness.STRICT);
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (reader.peek() != com.google.gson.stream.JsonToken.END_DOCUMENT)
                throw new IllegalArgumentException("Trailing JSON data");
            JsonElement fen = root.get("fen");
            if (fen == null || !fen.isJsonPrimitive() || !fen.getAsJsonPrimitive().isString())
                throw new IllegalArgumentException("Missing FEN string");
            CorpusPosition position = CorpusPosition.fromFen(fen.getAsString());
            CorpusRecord best = null;
            for (JsonElement element : root.getAsJsonArray("evals")) {
                try {
                    JsonObject evaluation = element.getAsJsonObject();
                    int depth = integer(evaluation.get("depth"));
                    if (depth < 0) continue;
                    long work = evaluation.has("knodes") ? number(evaluation.get("knodes")) : -1;
                    if (work < -1) continue;
                    int unit = work < 0 ? CorpusRecord.UNKNOWN_WORK : CorpusRecord.KILONODES;
                    JsonArray pvs = evaluation.getAsJsonArray("pvs");
                    if (pvs == null || pvs.isEmpty()) continue;
                    JsonObject pv = pvs.get(0).getAsJsonObject();
                    boolean cp = pv.has("cp"), mate = pv.has("mate");
                    if (cp == mate) continue;
                    CorpusRecord candidate = new CorpusRecord(position, cp ? CorpusRecord.CP : CorpusRecord.MATE,
                            integer(pv.get(cp ? "cp" : "mate")), CorpusRecord.WHITE, depth, work, unit, sourceId, line);
                    // Missing work is weaker than known work for source selection at the same depth.
                    if (best == null || depth > best.depth() || depth == best.depth() && work > best.work()) best = candidate;
                } catch (IllegalArgumentException | IllegalStateException | NullPointerException | ArithmeticException ex) {
                    // A bad sibling evaluation must not discard a usable strongest candidate.
                }
            }
            if (best == null) throw new IllegalArgumentException("No usable first-PV evaluation");
            return best;
        } catch (java.io.IOException | IllegalStateException | NullPointerException | JsonParseException | ArithmeticException ex) {
            throw new IllegalArgumentException("Invalid Lichess record", ex);
        }
    }
    private static int integer(JsonElement value) { return Math.toIntExact(number(value)); }
    private static long number(JsonElement value) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("Expected numeric integer");
        try { return value.getAsBigDecimal().longValueExact(); }
        catch (ArithmeticException ex) { throw new IllegalArgumentException("Out-of-range/non-integer value", ex); }
    }
}
