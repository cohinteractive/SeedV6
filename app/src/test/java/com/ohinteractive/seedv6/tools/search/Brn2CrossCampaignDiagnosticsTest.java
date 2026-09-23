package com.ohinteractive.seedv6.tools.search;

import java.util.List;
import org.junit.jupiter.api.Test;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn2.Brn2Model;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.training.model.NetworkModel;
import com.ohinteractive.seedv6.training.selfplay.TrajectorySampler.Sample;
import com.ohinteractive.seedv6.training.validation.HeldOutLoss;
import static org.junit.jupiter.api.Assertions.*;

class Brn2CrossCampaignDiagnosticsTest {
    @Test void pooledLossUsesEachTeacherAndSampleWeightRatherThanBatchMeansOrWeightedLosses() {
        var losses = new Brn2CrossCampaignDiagnostics.Losses();
        // First batch has one position; second has two with a different teacher target.
        losses.add(0, 1, 1);
        losses.add(0, 1, -1); losses.add(0, 1, -1);
        var result = losses.result();
        assertEquals(3L, result.get("samples"));
        assertEquals(.5, result.get("wdlLoss")); assertEquals(.5, result.get("teacherLoss"));
        assertEquals(.25, result.get("blended75Loss")); // (.5 + .125 + .125) / 3, not .5
        assertThrows(IllegalArgumentException.class, () -> losses.add(0, 0, Double.NaN));
        assertThrows(IllegalStateException.class, () -> new Brn2CrossCampaignDiagnostics.Losses().result());
    }
    @Test void nativePredictionsAgreeWithProductionLossAndNnueBoundedValue() {
        long[] a = Board.fromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        long[] b = Board.fromFen("4k3/8/8/8/8/8/4P3/4K3 b - - 0 1");
        var samples = List.of(new Sample(a, 1), new Sample(b, -1));
        var brn = new NetworkModel.Brn2(new Brn2Model());
        var predictor = Brn2CrossCampaignDiagnostics.predictor(brn);
        var losses = new Brn2CrossCampaignDiagnostics.Losses();
        for (var s : samples) losses.add(predictor.applyAsDouble(s.board()), s.target(), 0);
        assertEquals(HeldOutLoss.compare(brn, brn, samples).candidateLoss(), losses.result().get("wdlLoss"));
        var network = NnueNetwork.initialized(17); var nativeEvaluator = new NnueEvaluator(network);
        var nnue = Brn2CrossCampaignDiagnostics.predictor(new NetworkModel.Nnue(network));
        for (var s : samples) {
            nativeEvaluator.evaluate(s.board());
            assertEquals(nativeEvaluator.boundedValue(), nnue.applyAsDouble(s.board()));
        }
    }
    @Test void sampleIdentityIncludesOrderExactBoardAndTerminalTarget() throws Exception {
        long[] board = Board.fromFen("4k3/8/8/8/8/8/4P3/4K3 b - - 0 1");
        var a = new Sample(board, 1); var b = new Sample(board, -1);
        assertEquals(Brn2CrossCampaignDiagnostics.sampleHash(List.of(a, b)),
                Brn2CrossCampaignDiagnostics.sampleHash(List.of(new Sample(board.clone(), 1), b)));
        assertNotEquals(Brn2CrossCampaignDiagnostics.sampleHash(List.of(a, b)), Brn2CrossCampaignDiagnostics.sampleHash(List.of(b, a)));
        board[Board.STATUS] ^= 1;
        assertNotEquals(Brn2CrossCampaignDiagnostics.sampleHash(List.of(a)), Brn2CrossCampaignDiagnostics.sampleHash(List.of(new Sample(board, 1))));
    }
}
