package com.ohinteractive.seedv6.tools.eval;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Fen;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.driver.SearchDriver;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.training.checkpoint.CheckpointStore;
import com.ohinteractive.seedv6.training.data.DataFiles;
import com.ohinteractive.seedv6.training.validation.*;
import com.ohinteractive.seedv6.training.selfplay.HeadlessGame;
import com.ohinteractive.seedv6.core.move.LegalMoveResolver;
import com.ohinteractive.seedv6.core.move.MoveIntent;
import com.ohinteractive.seedv6.training.model.NetworkModel;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.util.*;

/** Single-worker paired diagnostic with immutable per-game receipts and explicit equal search. */
public final class BrnSuccessorMatch {
    record AverageInput(String root,String checkpoint,String networkSha256) {}
    record HybridBase(String root,String checkpoint,String networkSha256,Double changeScale) {
        HybridBase {
            if(changeScale==null||!Double.isFinite(changeScale)||changeScale<0||changeScale>1)
                throw new IllegalArgumentException("Hybrid base change scale must be in [0,1]");
        }
    }
    record Model(String name, String root, String checkpoint, String networkSha256, Double residualGain,
                 boolean gatedCache, boolean cubicCache, Double blendCandidateWeight,
                 Double correctionScale, String correctionAblation, double[] constantGates, boolean gatedDeltaCache, boolean cubicDeltaCache,
                 List<AverageInput> averagedInputs, List<Integer> retainedCubicRanks, HybridBase hybridBase,
                 Boolean packedCubicCache, Boolean compiledUnary, Boolean compiledConstantGates) {
        Model {
            if(constantGates!=null&&!"constant-gates".equals(correctionAblation))
                throw new IllegalArgumentException("Explicit constant-gates ablation required");
            if(cubicDeltaCache&&(gatedCache||cubicCache||gatedDeltaCache))throw new IllegalArgumentException("Choose one cache");
            if(gatedDeltaCache&&(gatedCache||cubicCache))throw new IllegalArgumentException("Choose one cache");
            if(Boolean.TRUE.equals(packedCubicCache)&&(gatedCache||cubicCache||gatedDeltaCache||cubicDeltaCache))
                throw new IllegalArgumentException("Choose one cache");
            if(Boolean.TRUE.equals(compiledUnary)&&(!"unary".equals(correctionAblation)||gatedCache||cubicCache
                    ||gatedDeltaCache||cubicDeltaCache||Boolean.TRUE.equals(packedCubicCache)||residualGain!=null||blendCandidateWeight!=null))
                throw new IllegalArgumentException("Compiled unary requires explicit unary ablation and ordinary Pair2 evaluation");
            if(Boolean.TRUE.equals(compiledConstantGates)&&(!"constant-gates".equals(correctionAblation)||constantGates==null
                    ||gatedCache||cubicCache||gatedDeltaCache||cubicDeltaCache||Boolean.TRUE.equals(packedCubicCache)
                    ||Boolean.TRUE.equals(compiledUnary)||residualGain!=null||blendCandidateWeight!=null))
                throw new IllegalArgumentException("Compiled constant gates require the explicit ablation and exclusive additive evaluation");
            if(hybridBase!=null&&(residualGain!=null||blendCandidateWeight!=null||gatedCache||cubicCache))
                throw new IllegalArgumentException("Hybrid requires explicit correction scaling and canonical or delta evaluation");
        }
        boolean packedCubicCacheEnabled() { return Boolean.TRUE.equals(packedCubicCache); }
        boolean compiledUnaryEnabled() { return Boolean.TRUE.equals(compiledUnary); }
        boolean compiledConstantGatesEnabled() { return Boolean.TRUE.equals(compiledConstantGates); }
    }
    static Map<String,Object> constantBinding(com.ohinteractive.seedv6.core.brnpair2.BrnConstantGateResearchCompiler.Compiled c) {
        return Map.of("kind","brn-constant-gates-compiled-v1","foldedBaseSha256",c.foldedBaseSha256(),
                "extraTablesSha256",c.extraTablesSha256(),"functionSha256",c.functionSha256(),
                "extraFirst",c.extraFirst(),"extraSecond",c.extraSecond(),"relations",c.model().pairs(),
                "immutablePrimitiveBytes",c.model().immutablePrimitiveBytes(),"ordinaryCheckpointExport",false);
    }
    static com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model loadHybridBase(Model spec) throws Exception {
        if(spec.hybridBase()==null)return null;
        var input=spec.hybridBase();
        var snapshot=CheckpointStore.readSnapshot(Path.of(input.root()),input.checkpoint());
        if(!snapshot.manifest().networkSha256().equals(input.networkSha256()))
            throw new IllegalArgumentException("Hybrid base hash changed");
        if(!(snapshot.model() instanceof NetworkModel.BrnPair2 pair))
            throw new IllegalArgumentException("Hybrid base must be ordinary BRE-Pair2");
        return BrnSuccessorVariants.additiveChange(pair.model(),input.changeScale());
    }
    static Map<String,Object> hybridBinding(Model spec,com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model base,
                                             String correctionHash) throws Exception {
        var bytes=new ByteArrayOutputStream();com.ohinteractive.seedv6.core.brnpair2.BrnPair2Codec.writeModel(base,bytes);
        String baseHash=HexFormat.of().formatHex(DataFiles.digest().digest(bytes.toByteArray()));
        String function="brn-hybrid-raw-v1\n"+baseHash+"\n"+correctionHash+"\n";
        var result=new LinkedHashMap<String,Object>();
        result.put("input",spec.hybridBase());
        result.put("originalManifest",CheckpointStore.readSnapshot(Path.of(spec.hybridBase().root()),spec.hybridBase().checkpoint()).manifest());
        result.put("effectiveBaseSha256",baseHash);result.put("effectiveCorrectionSha256",correctionHash);
        result.put("functionSha256",DataFiles.hash(function));
        result.put("identityEncoding",function);
        result.put("mapping","Compiled continued base + scaled nonlinear correction; material + .25 * (raw-material), one rounding; not a trainable combined checkpoint");
        return result;
    }
    /** Bound inference-only mean. The anchor remains the latest source checkpoint, never a fake
     * trainable checkpoint. Every ingredient and the effective model hash are retained in receipts. */
    static NetworkModel loadModel(Model spec) throws Exception {
        var model=loadAveragedOrAnchor(spec);
        if(spec.retainedCubicRanks()!=null) {
            if(!(model instanceof NetworkModel.BrnNext n)
                    ||n.model().architecture()!=com.ohinteractive.seedv6.core.brnnext.BrnNextArchitecture.GEN32_CUBIC)
                throw new IllegalArgumentException("Explicit frozen-base cubic rank mask required");
            model=new NetworkModel.BrnNext(BrnCubicRankProbe.keep(n.model(),spec.retainedCubicRanks()));
        }
        return model;
    }
    private static NetworkModel loadAveragedOrAnchor(Model spec) throws Exception {
        var anchor=CheckpointStore.readSnapshot(Path.of(spec.root()),spec.checkpoint());
        if(!anchor.manifest().networkSha256().equals(spec.networkSha256()))throw new IllegalArgumentException("Model hash changed");
        if(spec.averagedInputs()==null)return anchor.model();
        if(spec.averagedInputs().size()!=8)throw new IllegalArgumentException("Explicit eight-checkpoint window required");
        var models=new ArrayList<NetworkModel>();String parent=null;long generation=-1,step=-1;
        for(var input:spec.averagedInputs()) {
            var snapshot=CheckpointStore.readSnapshot(Path.of(input.root()),input.checkpoint());var manifest=snapshot.manifest();
            if(!manifest.networkSha256().equals(input.networkSha256()))throw new IllegalArgumentException("Average input hash changed");
            if(parent!=null&&(!manifest.parentId().equals(parent)||manifest.generation()!=generation+1||manifest.optimizerStep()!=step+8192))
                throw new IllegalArgumentException("Consecutive same-lineage mean required");
            parent=input.checkpoint();generation=manifest.generation();step=manifest.optimizerStep();models.add(snapshot.model());
        }
        var last=spec.averagedInputs().getLast();
        if(!last.checkpoint().equals(spec.checkpoint())||!last.networkSha256().equals(spec.networkSha256()))
            throw new IllegalArgumentException("Mean anchor must be latest ingredient");
        return BrnSuccessorVariants.average(models);
    }
    record Plan(String id, List<Model> models, int pairs, long seed, long nodes, long moveMillis,
                int maximumPlies, int batchGames, int batchSeconds, List<List<String>> openingMoves, boolean legalFallback) {
        Plan {
            if (models == null || models.size() < 2 || pairs < 1 || batchGames < 1 || batchSeconds < 1
                    || (nodes > 0) == (moveMillis > 0) || maximumPlies < 1)
                throw new IllegalArgumentException("Invalid bounded match plan");
            if (openingMoves != null && openingMoves.size() != pairs) throw new IllegalArgumentException("Opening count");
            if (models.stream().map(Model::name).distinct().count()!=models.size())throw new IllegalArgumentException("Distinct model names required");
        }
    }
    record Move(int player, long move, Integer score, int depth, long nodes, long elapsedNanos, String fenAfter, boolean fallback) {}
    record Receipt(String planHash, String candidate, String reference, int pair, int colour,
                   String openingHash, String openingFen, ValidationResult.Game result, List<Move> moves, String candidateName) {}
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("PLAN OUTPUT_DIRECTORY (same plan resumes completed games)");
        Path planFile = Path.of(args[0]), output = Path.of(args[1]);
        var plan = DataFiles.read(planFile, Plan.class);
        String hash = DataFiles.hash(Files.readString(planFile));
        Files.createDirectories(output);
        Path binding = output.resolve("plan.sha256");
        if (Files.exists(binding)) {
            if (!Files.readString(binding).equals(hash)) throw new IllegalStateException("Different resume plan");
        } else Files.writeString(binding, hash, StandardOpenOption.CREATE_NEW);
        var models = new ArrayList<SearchEvaluation>();
        var bindings = new ArrayList<Object>();
        for (var spec : plan.models()) {
            var snapshot = CheckpointStore.readSnapshot(Path.of(spec.root()), spec.checkpoint());
            if (!snapshot.manifest().networkSha256().equals(spec.networkSha256())) throw new IllegalStateException("Model hash changed");
            NetworkModel model=loadModel(spec);
            if(spec.correctionScale()!=null||spec.correctionAblation()!=null) {
                if(spec.residualGain()!=null||spec.blendCandidateWeight()!=null)
                    throw new IllegalArgumentException("Separate correction controls from W1 gain/blending");
                if(model instanceof NetworkModel.BrnNext n)
                    model=new NetworkModel.BrnNext(BrnSuccessorVariants.correction(n.model(),
                            spec.correctionScale()==null?1:spec.correctionScale(),spec.correctionAblation(),spec.constantGates()));
                else if(model instanceof NetworkModel.BrnPair2 p && spec.correctionScale()!=null
                        && spec.correctionAblation()==null && spec.constantGates()==null)
                    model=new NetworkModel.BrnPair2(BrnSuccessorVariants.additiveChange(p.model(),spec.correctionScale()));
                else throw new IllegalArgumentException("Residual correction or additive interpolation required");
            }
            if(spec.residualGain()!=null) {
                if(!(model instanceof NetworkModel.BrnNext n))throw new IllegalArgumentException("Gain override requires W1");
                model=new NetworkModel.BrnNext(BrnSuccessorVariants.gain(n.model(),spec.residualGain()));
            }
            SearchEvaluation evaluation;
            var hybridBase=loadHybridBase(spec);
            com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model compiledUnary=null;
            com.ohinteractive.seedv6.core.brnpair2.BrnConstantGateResearchCompiler.Compiled compiledConstant=null;
            if(spec.gatedCache()&&spec.cubicCache())throw new IllegalArgumentException("Choose one cache");
            if(spec.compiledConstantGatesEnabled()) {
                if(!(model instanceof NetworkModel.BrnNext n))throw new IllegalArgumentException("Constant-gate residual component required");
                compiledConstant=com.ohinteractive.seedv6.core.brnpair2.BrnConstantGateResearchCompiler.compile(
                        hybridBase==null?com.ohinteractive.seedv6.core.brnnext.BrnNextBase.model():hybridBase,n.model());
                evaluation=SearchEvaluation.brnPair2(compiledConstant.model());
            } else if(spec.compiledUnaryEnabled()) {
                if(!(model instanceof NetworkModel.BrnNext n))throw new IllegalArgumentException("Unary residual component required");
                compiledUnary=com.ohinteractive.seedv6.core.brnpair2.BrnUnaryResearchCompiler.compile(
                        hybridBase==null?com.ohinteractive.seedv6.core.brnnext.BrnNextBase.model():hybridBase,n.model());
                evaluation=SearchEvaluation.brnPair2(compiledUnary);
            } else if(spec.packedCubicCacheEnabled()) {
                if(!(model instanceof NetworkModel.BrnNext n))throw new IllegalArgumentException("Packed cubic requires a frozen correction");
                evaluation=hybridBase==null?SearchEvaluation.brnCubicPackedResearch(n.model())
                        :SearchEvaluation.brnCubicPackedHybridResearch(n.model(),hybridBase);
            } else if(hybridBase!=null) {
                if(!(model instanceof NetworkModel.BrnNext n)||!n.model().architecture().frozenBase())
                    throw new IllegalArgumentException("Hybrid requires a frozen-base correction");
                if(spec.gatedDeltaCache()&&n.model().architecture()!=com.ohinteractive.seedv6.core.brnnext.BrnNextArchitecture.GEN32_GATED
                        ||spec.cubicDeltaCache()&&n.model().architecture()!=com.ohinteractive.seedv6.core.brnnext.BrnNextArchitecture.GEN32_CUBIC)
                    throw new IllegalArgumentException("Hybrid cache architecture mismatch");
                evaluation=spec.gatedDeltaCache()||spec.cubicDeltaCache()
                        ?SearchEvaluation.brnResidualHybridResearch(n.model(),hybridBase)
                        :SearchEvaluation.brnResidualHybridFullResearch(n.model(),hybridBase);
            } else if(spec.cubicDeltaCache()) {
                if(!(model instanceof NetworkModel.BrnNext n))throw new IllegalArgumentException("CUBIC delta cache requires W3");
                evaluation=SearchEvaluation.brnCubicDeltaResearch(n.model());
            } else if(spec.gatedDeltaCache()) {
                if(!(model instanceof NetworkModel.BrnNext n))throw new IllegalArgumentException("GATE delta cache requires W3");
                evaluation=SearchEvaluation.brnGatedDeltaResearch(n.model());
            } else if(spec.gatedCache()) {
                if(!(model instanceof NetworkModel.BrnNext n))throw new IllegalArgumentException("GATE cache requires W1");
                evaluation=SearchEvaluation.brnGatedIncrementalResearch(n.model());
            } else if(spec.cubicCache()) {
                if(!(model instanceof NetworkModel.BrnNext n))throw new IllegalArgumentException("CUBIC cache requires W1");
                evaluation=SearchEvaluation.brnCubicIncrementalResearch(n.model());
            } else evaluation=model.evaluation(NnueScoreMapping.V1);
            if(spec.blendCandidateWeight()!=null) {
                if(models.isEmpty())throw new IllegalArgumentException("The reference cannot be a blend");
                evaluation=SearchEvaluation.blendResearch(models.getFirst(),evaluation,spec.blendCandidateWeight());
            }
            models.add(evaluation);
            var bytes=new ByteArrayOutputStream();model.write(bytes);
            var modelBinding=new LinkedHashMap<String,Object>(Map.of("name",spec.name(),"originalManifest",snapshot.manifest(),
                    "effectiveNetworkSha256",HexFormat.of().formatHex(DataFiles.digest().digest(bytes.toByteArray())),
                    "residualGain",spec.residualGain()==null?.25:spec.residualGain(),"gatedCache",spec.gatedCache()));
            if(spec.cubicCache())modelBinding.put("cubicCache",true);
            if(spec.averagedInputs()!=null) {
                modelBinding.put("averagedInputs",spec.averagedInputs());
                modelBinding.put("averaging","Uniform parameter mean; Pair2 compiled tables; inference only, no optimizer continuation");
            }
            if(spec.retainedCubicRanks()!=null) {
                modelBinding.put("retainedCubicRanks",spec.retainedCubicRanks());
                modelBinding.put("rankMask",spec.packedCubicCacheEnabled()
                        ?"Both omitted output heads zeroed after averaging, before correction scaling; explicit packed cache"
                        :"Both omitted output heads zeroed after averaging, before correction scaling; dense verified cache, no claimed speedup");
            }
            if(spec.cubicDeltaCache())modelBinding.put("cubicDeltaCache","polynomial-period64-canonical-rounding-guard-v1");
            if(spec.packedCubicCacheEnabled())modelBinding.put("packedCubicCache","packed-polynomial-period64-canonical-rounding-guard-v1");
            if(compiledConstant!=null)modelBinding.put("compiledConstantGates",constantBinding(compiledConstant));
            if(compiledUnary!=null) {
                var compiledBytes=new java.io.ByteArrayOutputStream();
                com.ohinteractive.seedv6.core.brnpair2.BrnPair2Codec.writeModel(compiledUnary,compiledBytes);
                modelBinding.put("compiledUnary",Map.of("kind","brn-unary-compiled-v1",
                        "compiledNetworkSha256",HexFormat.of().formatHex(DataFiles.digest().digest(compiledBytes.toByteArray())),
                        "payloadBytes",compiledBytes.size(),"numericalLimit","Algebraically equivalent; binary64 summation order changes, actual score/search oracles required"));
            }
            if(spec.gatedDeltaCache())modelBinding.put("gatedDeltaCache","period64-canonical-rounding-guard-v1");
            if(model instanceof NetworkModel.BrnNext n && n.model().architecture().frozenBase()) {
                if(compiledUnary==null&&compiledConstant==null)modelBinding.put("nativeResidualCache",true);
                else modelBinding.put("sourceFrozenResidual",true);
                if(hybridBase==null)modelBinding.put("frozenBaseSha256",com.ohinteractive.seedv6.core.brnnext.BrnNextBase.SHA256);
            }
            if(hybridBase!=null)modelBinding.put("hybrid",hybridBinding(spec,hybridBase,(String)modelBinding.get("effectiveNetworkSha256")));
            if(spec.correctionScale()!=null||spec.correctionAblation()!=null) {
                modelBinding.put("correctionScale",spec.correctionScale()==null?1:spec.correctionScale());
                modelBinding.put("correctionAblation",spec.correctionAblation());
                modelBinding.put("constantGates",spec.constantGates());
                if(model instanceof NetworkModel.BrnPair2) {
                    modelBinding.put("frozenBaseSha256",com.ohinteractive.seedv6.core.brnnext.BrnNextBase.SHA256);
                    modelBinding.put("additiveMapping","Gen32 compiled tables + scale * (continued compiled tables - Gen32 tables)");
                }
            }
            if(spec.blendCandidateWeight()!=null) {
                modelBinding.put("blendCandidateWeight",spec.blendCandidateWeight());
                modelBinding.put("blendReference",plan.models().getFirst());
                modelBinding.put("blendMapping","Convex calibrated integer scores, symmetric half-away rounding");
            }
            bindings.add(modelBinding);
        }
        String bound=DataFiles.JSON.toJson(Map.of("planHash",hash,"models",bindings))+"\n";
        Path identities=output.resolve("model-bindings.json");
        if(Files.exists(identities)) {
            if(!com.google.gson.JsonParser.parseString(Files.readString(identities)).equals(com.google.gson.JsonParser.parseString(bound)))
                throw new IllegalStateException("Effective model binding changed");
        } else Files.writeString(identities,bound,StandardOpenOption.CREATE_NEW);
        var config = new ValidationConfig(plan.pairs(), plan.seed(), plan.openingMoves()==null?4:0,
                plan.openingMoves()==null?12:0, 64, 1, NnueScoreMapping.V1, plan.maximumPlies());
        if (plan.moveMillis() > 0) for (var evaluation : models) for (int i=0;i<4;i++)
            try (var warmup = new SearchDriver(BrnSuccessorAudit.common(evaluation))) {
                var opening=opening(plan,config,i%plan.pairs());
                warmup.search(new SearchRequest(opening.board(), opening.history(), 64,
                        SearchControl.controlled(32768,System.nanoTime(),2_000_000_000L,TimeSource.SYSTEM)));
            }
        long start = System.nanoTime(); int played = 0;
        // Alternate candidates within an opening to reduce host/time-order confounding.
        outer: for (int pair = 0; pair < plan.pairs(); pair++) {
            var opening = opening(plan,config,pair);
            for (int candidate = 1; candidate < models.size(); candidate++) for (int colour = 0; colour < 2; colour++) {
                String name = String.format(Locale.ROOT, "p%03d-m%d-c%d.json", pair, candidate, colour);
                Path file = output.resolve(name);
                if (Files.exists(file)) {
                    var prior = DataFiles.read(file, Receipt.class);
                    if (!prior.planHash().equals(hash) || !prior.openingHash().equals(opening.identity())
                            || prior.pair() != pair || prior.colour() != colour
                            || !prior.candidate().equals(plan.models().get(candidate).checkpoint())
                            || prior.candidateName()!=null&&!prior.candidateName().equals(plan.models().get(candidate).name())
                            || !prior.reference().equals(plan.models().getFirst().checkpoint()))
                        throw new IllegalStateException("Receipt binding mismatch: " + file);
                    continue;
                }
                if (played >= plan.batchGames() || System.nanoTime() - start > plan.batchSeconds() * 1_000_000_000L) break outer;
                var game = opening.newGame(plan.maximumPlies());
                var trace = new ArrayList<Move>();
                try (var a = new SearchDriver(BrnSuccessorAudit.common(models.get(candidate)));
                     var b = new SearchDriver(BrnSuccessorAudit.common(models.getFirst()))) {
                    while (game.active()) {
                        int player = game.sideToMove();
                        var control = SearchControl.controlled(plan.nodes() > 0 ? plan.nodes() : -1, System.nanoTime(),
                                plan.moveMillis() > 0 ? plan.moveMillis() * 1_000_000 : 2_000_000_000L, TimeSource.SYSTEM);
                        var outcome = (player == colour ? a : b).search(new SearchRequest(
                                game.boardSnapshot(), game.historySnapshot(), 64, control));
                        var result = outcome.lastCompletedResult();
                        if (result == null && plan.legalFallback()
                                && (control.termination()==SearchTermination.TIME_LIMIT || control.termination()==SearchTermination.NODE_LIMIT)) {
                            long move=game.legalMoves()[0];game.play(move);
                            trace.add(new Move(player,move,null,0,control.nodes(),control.elapsedNanos(),Fen.fromBoard(game.boardSnapshot()),true));
                            continue; // Same legal-only fallback as UCI; never invent a searched score/depth.
                        }
                        if (result == null || !result.completed() || !result.hasMove()
                                || plan.nodes() > 0 && control.termination() == SearchTermination.TIME_LIMIT)
                            throw new IllegalStateException("Search did not produce a usable bounded iteration: termination="
                                    + control.termination() + ", nodes=" + control.nodes() + ", elapsedNanos="
                                    + control.elapsedNanos() + ", result=" + result + ", fen=" + Fen.fromBoard(game.boardSnapshot()));
                        game.play(result.bestMove());
                        trace.add(new Move(player, result.bestMove(), result.score(), result.depth(), control.nodes(),
                                control.elapsedNanos(), Fen.fromBoard(game.boardSnapshot()),false));
                    }
                } catch (RuntimeException failure) {
                    Files.writeString(output.resolve(name + "." + UUID.randomUUID() + ".failed"),
                            DataFiles.JSON.toJson(Map.of("failure", failure.toString(), "moves", trace,
                                    "planHash", hash, "pair", pair, "colour", colour)) + "\n", StandardOpenOption.CREATE_NEW);
                    throw failure; // Never silently exclude a failure and continue selection.
                }
                var receipt = new Receipt(hash, plan.models().get(candidate).checkpoint(), plan.models().getFirst().checkpoint(),
                        pair, colour, opening.identity(), Fen.fromBoard(opening.board()),
                        new ValidationResult.Game(game.termination(), game.playedPlies()), List.copyOf(trace),plan.models().get(candidate).name());
                DataFiles.write(file, receipt);
                System.out.println(name + " " + game.termination() + " plies=" + game.playedPlies());
                played++;
            }
        }
        var summaries = new ArrayList<Object>();
        for (int candidate = 1; candidate < models.size(); candidate++) {
            int wins=0, draws=0, losses=0, missing=0, incomplete=0;
            var pairScores = new ArrayList<Double>();
            for (int pair=0; pair<plan.pairs(); pair++) {
                var white = output.resolve(String.format(Locale.ROOT, "p%03d-m%d-c0.json", pair, candidate));
                var black = output.resolve(String.format(Locale.ROOT, "p%03d-m%d-c1.json", pair, candidate));
                if (!Files.exists(white) || !Files.exists(black)) { missing++; continue; }
                var w=DataFiles.read(white, Receipt.class); var b=DataFiles.read(black, Receipt.class);
                if (!w.result().termination().completed() || !b.result().termination().completed()) { incomplete++; continue; }
                double ws=w.result().score(Value.WHITE), bs=b.result().score(Value.BLACK);
                for (double score : new double[]{ws,bs}) { if (score==1) wins++; else if(score==0) losses++; else draws++; }
                pairScores.add((ws+bs)/2);
            }
            summaries.add(Map.of("candidate",plan.models().get(candidate).name(),"wins",wins,"draws",draws,"losses",losses,
                    "missingPairs",missing,"incompletePairs",incomplete,"pairScores",pairScores));
        }
        DataFiles.write(output.resolve("summary.json"), Map.of("planHash",hash,"models",summaries,
                "java",System.getProperty("java.runtime.version"),"activeProcessors",Runtime.getRuntime().availableProcessors()));
    }
    static ValidationArena.Opening opening(Plan plan,ValidationConfig config,int pair) {
        var board=Board.startingPosition();
        if(plan.openingMoves()==null)return ValidationArena.opening(board,GameHistory.initial(board),config,pair);
        var game=new HeadlessGame(board,1024);var resolver=new LegalMoveResolver();
        for(String uci:plan.openingMoves().get(pair)) {
            if(!uci.matches("[a-h][1-8][a-h][1-8][qrbn]?"))throw new IllegalArgumentException("Invalid opening UCI");
            var promotion=uci.length()==4?MoveIntent.Promotion.NONE:switch(uci.charAt(4)) {
                case 'q'->MoveIntent.Promotion.QUEEN;case 'r'->MoveIntent.Promotion.ROOK;
                case 'b'->MoveIntent.Promotion.BISHOP;case 'n'->MoveIntent.Promotion.KNIGHT;default->throw new IllegalArgumentException();};
            int from=uci.charAt(0)-'a'+8*(uci.charAt(1)-'1'),to=uci.charAt(2)-'a'+8*(uci.charAt(3)-'1');
            game.play(resolver.resolve(game.boardSnapshot(),new MoveIntent(from,to,promotion)));
        }
        if(!game.active())throw new IllegalArgumentException("Terminal opening");
        return new ValidationArena.Opening(game.boardSnapshot(),game.historySnapshot(),game.playedPlies());
    }
    private BrnSuccessorMatch() {}
}
