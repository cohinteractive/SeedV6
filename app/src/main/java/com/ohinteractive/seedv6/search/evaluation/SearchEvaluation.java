package com.ohinteractive.seedv6.search.evaluation;

import java.util.Objects;

import com.ohinteractive.seedv6.core.Eval;
import com.ohinteractive.seedv6.core.brn.BrnModel;
import com.ohinteractive.seedv6.core.brn.BrnFeatures;
import com.ohinteractive.seedv6.core.brn1.Brn1Model;
import com.ohinteractive.seedv6.core.brn1.Brn1Workspace;
import com.ohinteractive.seedv6.core.brn2.Brn2Model;
import com.ohinteractive.seedv6.core.brn2.Brn2Workspace;
import com.ohinteractive.seedv6.core.brn2.Brn2Accumulator;
import com.ohinteractive.seedv6.core.brn3.Brn3Model;
import com.ohinteractive.seedv6.core.brn3.Brn3Workspace;
import com.ohinteractive.seedv6.core.nnue.NnueAccumulator;
import com.ohinteractive.seedv6.core.nnue.NnueEvaluator;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.core.nnue.NnueMaterialBootstrap;

/**
 * Immutable search-evaluator definition. Normal application construction uses handcrafted().
 * NNUE requires an explicit immutable network; V1 mapping is the default, with research overrides.
 * No runtime replacement exists.
 * Definitions may be shared, but each board stack must own a distinct State.
 */
public final class SearchEvaluation {
    private static final SearchEvaluation HANDCRAFTED = new SearchEvaluation(null, null, false);
    private final NnueNetwork network;
    private final BrnModel brn;
    private final Brn1Model brn1;
    private final Brn2Model brn2;
    private record Brn3Definition(Brn3Model model, double residualGain) {}
    private final Brn3Definition brn3;
    private final com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model pair2;
    private final NnueScoreMapping mapping;
    private final boolean incremental;
    private final boolean scalarOracle;
    private final boolean materialBootstrap;

    private SearchEvaluation(NnueNetwork network, NnueScoreMapping mapping, boolean incremental) {
        this(network, mapping, incremental, false);
    }

    private SearchEvaluation(NnueNetwork network, NnueScoreMapping mapping, boolean incremental, boolean scalarOracle) {
        this(network, mapping, incremental, scalarOracle, false);
    }

    private SearchEvaluation(NnueNetwork network, NnueScoreMapping mapping, boolean incremental,
                             boolean scalarOracle, boolean materialBootstrap) {
        this.materialBootstrap = materialBootstrap;
        this.brn = null; this.brn1 = null; this.brn2 = null; this.brn3 = null; pair2 = null;
        this.network = network;
        this.mapping = mapping;
        this.incremental = incremental;
        this.scalarOracle = scalarOracle;
    }

    private SearchEvaluation(BrnModel model) {
        brn = Objects.requireNonNull(model, "BRN model"); brn1 = null; brn2 = null; brn3 = null; pair2 = null;
        network = null; mapping = null; incremental = false; scalarOracle = false; materialBootstrap = false;
    }

    private SearchEvaluation(Brn1Model model) {
        brn1 = Objects.requireNonNull(model, "BRN-1 model"); brn = null; brn2 = null; brn3 = null; pair2 = null;
        network = null; mapping = null; incremental = false; scalarOracle = false; materialBootstrap = false;
    }

    public static SearchEvaluation brn1(Brn1Model model) { return new SearchEvaluation(model); }

    private SearchEvaluation(Brn2Model model, boolean incremental) {
        brn2 = Objects.requireNonNull(model, "BRN-2 model"); brn = null; brn1 = null; brn3 = null; pair2 = null;
        network = null; mapping = null; this.incremental = incremental; scalarOracle = false; materialBootstrap = false;
    }

    public static SearchEvaluation brn2(Brn2Model model) { return new SearchEvaluation(model, model.boundedIntermediates()); }
    /** Trusted original accumulation order, including both ReLUs; equivalence/benchmark oracle. */
    public static SearchEvaluation brn2FullRecompute(Brn2Model model) { return new SearchEvaluation(model, false); }

    public static SearchEvaluation brn(BrnModel model) { return new SearchEvaluation(model); }

    private SearchEvaluation(Brn3Model model, double residualGain) {
        if(!Double.isFinite(residualGain)||residualGain<0||residualGain>1)throw new IllegalArgumentException("BRN-3 residual gain must be in [0,1]");
        pair2=null; brn3=new Brn3Definition(Objects.requireNonNull(model,"BRN-3 model"),residualGain);brn=null;brn1=null;brn2=null;
        network=null;mapping=null;incremental=true;scalarOracle=false;materialBootstrap=false;
    }
    public static SearchEvaluation brn3(Brn3Model model){return new SearchEvaluation(model,com.ohinteractive.seedv6.core.brn3.Brn3SearchCalibration.RESIDUAL_GAIN);}
    /** Explicit research control; ordinary application callers use the fixed production calibration. */
    public static SearchEvaluation brn3Research(Brn3Model model,double residualGain){return new SearchEvaluation(model,residualGain);}

    private SearchEvaluation(com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model model) {
        pair2=Objects.requireNonNull(model);brn=null;brn1=null;brn2=null;brn3=null;
        network=null;mapping=null;incremental=true;scalarOracle=false;materialBootstrap=false;
    }
    public static SearchEvaluation brnPair2(com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model model){return new SearchEvaluation(model);}

    public static SearchEvaluation handcrafted() { return HANDCRAFTED; }

    /** Research-only HCE definition with an unpruned SearchDriver control (SR-018/SR-019 disabled). */
    public static SearchEvaluation handcraftedIsolation() {
        return new SearchEvaluation(null, null, false, false);
    }

    public static SearchEvaluation incremental(NnueNetwork network) {
        return incremental(network, NnueScoreMapping.V1);
    }

    public static SearchEvaluation incremental(NnueNetwork network, NnueScoreMapping mapping) {
        return new SearchEvaluation(Objects.requireNonNull(network, "network"),
                Objects.requireNonNull(mapping, "mapping"), true);
    }

    /** Explicit bootstrap-parity track. Existing NNUE residual mapping remains unchanged. */
    public static SearchEvaluation incrementalWithMaterial(NnueNetwork network, NnueScoreMapping mapping) {
        return new SearchEvaluation(Objects.requireNonNull(network, "network"),
                Objects.requireNonNull(mapping, "mapping"), true, false, true);
    }

    /** Independent full-refresh integration oracle for bootstrap parity. */
    public static SearchEvaluation fullRecomputeWithMaterial(NnueNetwork network, NnueScoreMapping mapping) {
        return new SearchEvaluation(Objects.requireNonNull(network, "network"),
                Objects.requireNonNull(mapping, "mapping"), false, true, true);
    }

    /** Original scalar float inference with the same incremental lifecycle and private TT construction. */
    public static SearchEvaluation incrementalScalarOracle(NnueNetwork network, NnueScoreMapping mapping) {
        return new SearchEvaluation(Objects.requireNonNull(network, "network"),
                Objects.requireNonNull(mapping, "mapping"), true, true);
    }

    /** Independent integration oracle: full transformer rebuild at every evaluation. */
    public static SearchEvaluation fullRecompute(NnueNetwork network, NnueScoreMapping mapping) {
        return new SearchEvaluation(Objects.requireNonNull(network, "network"),
                Objects.requireNonNull(mapping, "mapping"), false);
    }

    public State newState(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("State capacity must be positive.");
        if (pair2 != null) return new Pair2State(this);
        if (brn != null) return new BrnState(this);
        if (brn1 != null) return new Brn1State(this);
        if (brn2 != null) return incremental ? new Brn2IncrementalState(this, capacity) : new Brn2State(this);
        if (brn3 != null) return new Brn3State(this);
        if (network == null) return new HandcraftedState();
        return incremental ? new IncrementalState(this, capacity) : new RecomputedState(this);
    }

    /**
     * Worker-confined stack companion. All successful operations allocate nothing.
     * initializeFrom copies a main-search leaf into separate qsearch storage at the same
     * absolute ply. Parent states are never mutated by child preparation or inference.
     */
    public abstract static sealed class State {
        public abstract void initialize(long[] board, int ply);
        public abstract void child(long[] parent, long[] child, int parentPly);
        public abstract void initializeFrom(long[] board, int ply, State source);
        public abstract int evaluate(long[] board, int ply);
    }

    private static final class BrnState extends State {
        private final SearchEvaluation definition;
        private final BrnFeatures features = new BrnFeatures();
        private BrnState(SearchEvaluation definition) { this.definition = definition; }
        @Override public void initialize(long[] board, int ply) {}
        @Override public void child(long[] parent, long[] child, int parentPly) {}
        @Override public void initializeFrom(long[] board, int ply, State source) {
            if (!(source instanceof BrnState other) || definition != other.definition)
                throw new IllegalArgumentException("Evaluator mismatch.");
        }
        @Override public int evaluate(long[] board, int ply) {
            return BrnScoreMapping.map(definition.brn.evaluate(board, features));
        }
    }

    private static final class Brn1State extends State {
        private final SearchEvaluation definition;
        private final Brn1Workspace scratch = new Brn1Workspace();
        private Brn1State(SearchEvaluation definition) { this.definition = definition; }
        @Override public void initialize(long[] board, int ply) {}
        @Override public void child(long[] parent, long[] child, int parentPly) {}
        @Override public void initializeFrom(long[] board, int ply, State source) {
            if (!(source instanceof Brn1State other) || definition != other.definition)
                throw new IllegalArgumentException("Evaluator mismatch.");
        }
        @Override public int evaluate(long[] board, int ply) {
            return BrnScoreMapping.map(definition.brn1.evaluate(board, scratch));
        }
    }

    private static final class Brn2IncrementalState extends State {
        private final SearchEvaluation definition;
        private final Brn2Accumulator[] stack;
        Brn2IncrementalState(SearchEvaluation definition, int capacity) {
            this.definition = definition;
            stack = new Brn2Accumulator[capacity];
            for (int i = 0; i < capacity; i++) stack[i] = new Brn2Accumulator(definition.brn2);
        }
        @Override public void initialize(long[] board, int ply) { stack[ply].rebuild(board); }
        @Override public void child(long[] parent, long[] child, int parentPly) {
            stack[parentPly + 1].update(parent, child, stack[parentPly]);
        }
        @Override public void initializeFrom(long[] board, int ply, State source) {
            if (!(source instanceof Brn2IncrementalState other) || definition != other.definition)
                throw new IllegalArgumentException("Evaluator mismatch.");
            stack[ply].update(board, board, other.stack[ply]);
        }
        @Override public int evaluate(long[] board, int ply) { return BrnScoreMapping.map(stack[ply].evaluate(board)); }
    }

    private static final class Brn2State extends State {
        private final SearchEvaluation definition;
        private final Brn2Workspace scratch = new Brn2Workspace();
        private Brn2State(SearchEvaluation definition) { this.definition = definition; }
        @Override public void initialize(long[] board, int ply) {}
        @Override public void child(long[] parent, long[] child, int parentPly) {}
        @Override public void initializeFrom(long[] board, int ply, State source) {
            if (!(source instanceof Brn2State other) || definition != other.definition)
                throw new IllegalArgumentException("Evaluator mismatch.");
        }
        @Override public int evaluate(long[] board, int ply) {
            return BrnScoreMapping.map(definition.brn2.evaluateReference(board, scratch));
        }
    }

    private static final class Brn3State extends State {
        private final SearchEvaluation definition;
        private final Brn3Workspace workspace;
        Brn3State(SearchEvaluation definition){this.definition=definition;workspace=definition.brn3.model().newWorkspace();}
        @Override public void initialize(long[] board,int ply){}
        @Override public void child(long[] parent,long[] child,int parentPly){}
        @Override public void initializeFrom(long[] board,int ply,State source) {
            if(!(source instanceof Brn3State other)||definition!=other.definition)throw new IllegalArgumentException("Evaluator mismatch.");
        }
        @Override public int evaluate(long[] board,int ply){return Brn3Model.score(workspace.evaluatePawns(board,definition.brn3.residualGain()));}
    }
    private static final class Pair2State extends State {
        private final SearchEvaluation definition;
        private final com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model.Workspace workspace;
        Pair2State(SearchEvaluation definition){this.definition=definition;workspace=definition.pair2.newWorkspace();}
        @Override public void initialize(long[] board,int ply){}
        @Override public void child(long[] parent,long[] child,int parentPly){}
        @Override public void initializeFrom(long[] board,int ply,State source) {
            if(!(source instanceof Pair2State other)||definition!=other.definition)throw new IllegalArgumentException("Evaluator mismatch.");
        }
        @Override public int evaluate(long[] board,int ply){return Brn3Model.score(workspace.evaluatePawns(board,com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model.RESIDUAL_GAIN));}
    }
    private static final class HandcraftedState extends State {
        @Override public void initialize(long[] board, int ply) {}
        @Override public void child(long[] parent, long[] child, int parentPly) {}
        @Override public void initializeFrom(long[] board, int ply, State source) {
            if (!(source instanceof HandcraftedState)) throw new IllegalArgumentException("Evaluator mismatch.");
        }
        @Override public int evaluate(long[] board, int ply) { return Eval.evaluate(board); }
    }

    private static final class RecomputedState extends State {
        private final SearchEvaluation definition;
        private final NnueEvaluator inference;
        private RecomputedState(SearchEvaluation definition) {
            this.definition = definition;
            inference = NnueEvaluator.scalarOracle(definition.network);
        }
        @Override public void initialize(long[] board, int ply) {}
        @Override public void child(long[] parent, long[] child, int parentPly) {}
        @Override public void initializeFrom(long[] board, int ply, State source) {
            if (!(source instanceof RecomputedState other) || definition != other.definition) {
                throw new IllegalArgumentException("Evaluator mismatch.");
            }
        }
        @Override public int evaluate(long[] board, int ply) {
            inference.evaluate(board);
            int neural = definition.mapping.map(inference.boundedValue());
            return definition.materialBootstrap ? NnueMaterialBootstrap.combine(
                    NnueMaterialBootstrap.forSideToMove(board, NnueMaterialBootstrap.whiteScore(board)), neural) : neural;
        }
    }

    /**
     * Each search worker owns its requested accumulator capacity and one reusable
     * inference scratch accumulator. Object headers and placement are JVM-dependent.
     */
    private static final class IncrementalState extends State {
        private final SearchEvaluation definition;
        private final NnueAccumulator[] accumulators;
        private final int[] whiteMaterial;
        private final NnueEvaluator inference;
        private IncrementalState(SearchEvaluation definition, int capacity) {
            this.definition = definition;
            inference = definition.scalarOracle ? NnueEvaluator.scalarOracle(definition.network)
                    : new NnueEvaluator(definition.network);
            whiteMaterial = definition.materialBootstrap ? new int[capacity] : null;
            accumulators = new NnueAccumulator[capacity];
            for (int ply = 0; ply < capacity; ply++) {
                accumulators[ply] = new NnueAccumulator(definition.network);
            }
        }
        @Override public void initialize(long[] board, int ply) {
            accumulators[ply].rebuild(board);
            if (whiteMaterial != null) whiteMaterial[ply] = NnueMaterialBootstrap.whiteScore(board);
        }
        @Override public void child(long[] parent, long[] child, int parentPly) {
            accumulators[parentPly + 1].update(parent, child, accumulators[parentPly]);
            if (whiteMaterial != null) whiteMaterial[parentPly + 1] =
                    NnueMaterialBootstrap.update(parent, child, whiteMaterial[parentPly]);
        }
        @Override public void initializeFrom(long[] board, int ply, State source) {
            if (!(source instanceof IncrementalState other) || definition != other.definition) {
                throw new IllegalArgumentException("Evaluator mismatch.");
            }
            // A placement-preserving Workstream B transition copies all raw bits exactly.
            accumulators[ply].update(board, board, other.accumulators[ply]);
            if (whiteMaterial != null) whiteMaterial[ply] = other.whiteMaterial[ply];
        }
        @Override public int evaluate(long[] board, int ply) {
            inference.evaluate(board, accumulators[ply]);
            int neural = definition.mapping.map(inference.boundedValue());
            return whiteMaterial != null ? NnueMaterialBootstrap.combine(
                    NnueMaterialBootstrap.forSideToMove(board, whiteMaterial[ply]), neural) : neural;
        }
    }
}
