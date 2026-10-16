package com.ohinteractive.seedv6.tools.nnue.cglhw;

import com.ohinteractive.seedv6.core.brn3.Brn3Model;
import com.ohinteractive.seedv6.core.brn3.Brn3SearchCalibration;
import com.ohinteractive.seedv6.core.nnue.*;

/** Explicit trained Track B recipe: M + .25 * pawn residual, like real BRN3.
 * Not the legacy full-range tanh mapping or E008's unchanged-score ablation.
 */
public record HalfKpResidualModel(NnueNetwork network) implements IncrementalModel {
    @Override public int parameters(){return NnueNetwork.PARAMETER_COUNT;}
    @Override public Worker worker(int capacity) {
        return new Worker(1) {
            private final NnueAccumulator[] sums=new NnueAccumulator[capacity];
            private final int[] material=new int[capacity];
            private final NnueEvaluator head=new NnueEvaluator(network);
            {for(int i=0;i<capacity;i++)sums[i]=new NnueAccumulator(network);}
            @Override public void refresh(long[] board,int slot){sums[slot].rebuild(board);material[slot]=NnueMaterialBootstrap.whiteScore(board);}
            @Override public void transition(long[] p,long[] c,int src,int dst){sums[dst].update(p,c,sums[src]);material[dst]=NnueMaterialBootstrap.update(p,c,material[src]);}
            @Override public double raw(long[] board,int slot){return head.evaluate(board,sums[slot]);}
            @Override public int evaluate(long[] board,int slot){
                return Brn3Model.score(NnueMaterialBootstrap.forSideToMove(board,material[slot])/100.0+
                        Brn3SearchCalibration.RESIDUAL_GAIN*raw(board,slot));
            }
        };
    }
}
