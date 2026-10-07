package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Fen;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class BrnArchitectureFreshDataTest {
    @TempDir Path temporary;
    @Test void sourceAcquisitionExcludesEarlierAndWithinRangeGroupsWithoutSelectingOnLabels()throws Exception {
        var positions=new ArrayList<long[]>();var random=new Random(717);
        var board=Board.startingPosition();var moves=new long[512];
        for(int i=0;i<500;i++) {
            positions.add(board.clone());int count=BrnResearchDiagnostics.legal(board,moves);
            board=count==0||i%99==98?Board.startingPosition():BrnResearchDiagnostics.child(board,moves[random.nextInt(count)]);
        }
        var earlier=new HashSet<String>();for(int i=0;i<4;i++)earlier.add(BrnResearchData.groupKey(positions.get(i)));
        List<List<String>> recorded=new ArrayList<>();
        for(int sign:new int[]{1,-1}) {
            var text=new StringBuilder();
            for(var b:positions)for(int duplicate=0;duplicate<2;duplicate++)
                text.append("{\"fen\":\"").append(Fen.fromBoard(b)).append("\",\"evals\":[{\"depth\":20,\"pvs\":[{\"cp\":")
                        .append(sign*(123+duplicate)).append("}]}]}\n");
            Path source=temporary.resolve("source"+sign+".jsonl");Files.writeString(source,text);
            Path out=temporary.resolve("data"+sign);
            var admission=new BrnResearchData.GeometryAdmission(earlier,List.of());
            BrnResearchData.prepare(source,out,16,4,0,null,true,admission);
            assertTrue(admission.excludedEarlier>=8);assertTrue(admission.excludedWithin>0);
            var loaded=BrnResearchData.read(out,true);assertEquals(16,loaded.training().size());assertEquals(4,loaded.validation().size());assertEquals(4,loaded.test().size());
            var groups=new ArrayList<String>();var unique=new HashSet<String>();
            for(var partition:List.of(loaded.training(),loaded.validation(),loaded.test()))for(var e:partition) {
                String group=BrnResearchData.groupKey(e.board());assertFalse(earlier.contains(group));assertTrue(unique.add(group));groups.add(group);
            }
            recorded.add(groups);
        }
        assertEquals(recorded.get(0),recorded.get(1),"Changing eligible CP labels must not select different geometry");
        assertEquals(4,earlier.size(),"Prior evidence must not be mutated");
    }
}
