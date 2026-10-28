package com.ohinteractive.seedv6.core.brnpair2;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CanonicalAttackFieldNetworkTest {
    @Test void rawUpdaterAndCanonicalCheckpointContinuationAreExact()throws Exception {
        var data=new PairCoverageDiagnostic.Data(257);
        for(int i=0;i<257;i++){long[] b=i%2==0?Board.startingPosition():Board.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1");System.arraycopy(b,0,data.boards,6*i,6);data.targets[i]=(i%5)/4.0;}
        var legacy=new AttackFieldNetwork(true,.01,71);var raw=new CanonicalAttackFieldNetwork(false,.01,71);
        for(int epoch=1;epoch<=3;epoch++) {
            AttackFieldDiagnostic.epoch(legacy,data,71,epoch);CanonicalAttackFieldDiagnostic.epoch(raw,data,71,epoch);var a=new ByteArrayOutputStream();var b=new ByteArrayOutputStream();legacy.write(a);raw.model.write(b);assertArrayEquals(a.toByteArray(),b.toByteArray());
        }
        var canonical=new CanonicalAttackFieldNetwork(true,.01,71);CanonicalAttackFieldDiagnostic.epoch(canonical,data,71,1);var bytes=new ByteArrayOutputStream();canonical.write(bytes);var restored=CanonicalAttackFieldNetwork.read(new ByteArrayInputStream(bytes.toByteArray()));assertTrue(restored.canonical);
        CanonicalAttackFieldDiagnostic.epoch(canonical,data,71,2);CanonicalAttackFieldDiagnostic.epoch(restored,data,71,2);var a=new ByteArrayOutputStream();var b=new ByteArrayOutputStream();canonical.write(a);restored.write(b);assertArrayEquals(a.toByteArray(),b.toByteArray());
        byte[] corrupt=a.toByteArray();corrupt[11]^=1;assertThrows(IOException.class,()->CanonicalAttackFieldNetwork.read(new ByteArrayInputStream(corrupt)));
    }
    @Test void canonicalScoresAreMirrorInvariantAndMatchExplicitViewIncludingSpecialSuccessors() {
        var wrapper=new CanonicalAttackFieldNetwork(true,.01,71);var w=new CanonicalAttackFieldNetwork.Workspace();var boards=new ArrayList<long[]>();boards.add(Board.startingPosition());long[] moves=new long[256],scratch=new long[6];
        for(String fen:List.of("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1","4k3/P7/8/8/8/8/7p/4K3 b - - 0 1")) {
            var b=Board.fromFen(fen);boards.add(b);int n=Gen.genAll(b[0],b[1],b[2],b[3],(int)b[4],b[5],true,moves,scratch);
            for(int i=0;i<n;i++){var next=new long[6];Board.makeMoveInto(b[0],b[1],b[2],b[3],(int)b[4],b[5],moves[i],next);boards.add(next);}
        }
        for(var board:boards) {
            var saved=board.clone();long[] mirror=new long[6],view=new long[6];HorizontalScoreView.reflect(board,mirror);HorizontalScoreView.canonical(board,view);
            assertEquals(wrapper.scoreCp(board,w),wrapper.scoreCp(mirror,w));assertEquals(wrapper.model.scoreCp(view,w.neural),wrapper.scoreCp(board,w));assertArrayEquals(saved,board);
        }
    }
}
