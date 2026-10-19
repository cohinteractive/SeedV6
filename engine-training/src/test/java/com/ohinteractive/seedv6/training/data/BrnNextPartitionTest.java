package com.ohinteractive.seedv6.training.data;

import com.ohinteractive.seedv6.corpus.CorpusPosition;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BrnNextPartitionTest {
    @Test void allPermittedInputAliasesStayTogetherRegardlessOfClockTurnAndColorFileTransforms() {
        var a = CorpusPosition.fromFen("4k3/8/3p4/2n5/4B3/3P4/8/4K3 w - - 0 1");
        for (String fen : new String[]{
                "4k3/8/3p4/2n5/4B3/3P4/8/4K3 b - - 99 100",
                "4k3/8/3p4/4b3/2N5/3P4/8/4K3 b - - 0 1",
                "3k4/8/4p3/5n2/3B4/4P3/8/3K4 w - - 0 1"}) {
            var b = CorpusPosition.fromFen(fen);
            assertEquals(BrnNextPartition.groupKey(a), BrnNextPartition.groupKey(b));
            assertEquals(BrnNextPartition.part(a), BrnNextPartition.part(b));
        }
    }
}
