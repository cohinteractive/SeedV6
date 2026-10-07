package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.brn3.BrnTupleTrainer;
import com.ohinteractive.seedv6.training.data.DataFiles;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Draft integration check; requires the separate TUPLE_COMPILED loader branch. */
class BrnTupleCompilationTest {
    @TempDir Path temp;
    @Test void compilationPreservesSourceAndCreatesAnInferenceOnlyCommonView() throws Exception {
        var fixture=new BrnArchitectureSubsetTest();fixture.temp=temp;
        Path data=fixture.fixture("data",123),parent=temp.resolve("original"),out=temp.resolve("compiled");
        Files.createDirectory(parent);
        long[][] boards={Board.startingPosition(),Board.fromFen("4k3/8/8/3q4/8/8/4R3/4K3 w - - 0 1")};
        var trainer=new BrnTupleTrainer(2,.01);
        for(int i=0;i<6;i++)trainer.trainBatch(boards,new double[]{.1,.4},2);
        BrnTupleExperiment.save(trainer,parent,"selected");
        String model=BrnResearchComparison.digest(parent.resolve("selected.model"));
        String state=BrnResearchComparison.digest(parent.resolve("selected.state"));
        DataFiles.write(parent.resolve("result.json"),Map.of("kind","TUPLE","complete",true,"selectedModelSha256",model));
        String metadata=BrnResearchComparison.digest(parent.resolve("result.json"));
        String payload=BrnResearchComparison.digest(data.resolve("positions.bin"));
        BrnTupleCompilation.main(new String[]{data.toString(),parent.toString(),out.toString()});
        var report=DataFiles.read(out.resolve("result.json"),Map.class);
        assertEquals(true,report.get("complete"));assertEquals(true,report.get("parityPassed"));
        assertEquals("TUPLE_COMPILED",report.get("kind"));assertEquals(model,report.get("selectedModelSha256"));
        assertEquals(metadata,report.get("sourceMetadataSha256"));
        assertEquals(-1,Files.mismatch(parent.resolve("selected.model"),out.resolve("selected.model")));
        assertFalse(Files.exists(out.resolve("selected.state")));
        assertEquals(state,BrnResearchComparison.digest(parent.resolve("selected.state")));
        assertEquals(metadata,BrnResearchComparison.digest(parent.resolve("result.json")));
        assertNotEquals(metadata,BrnResearchComparison.digest(out.resolve("result.json")));
        assertEquals(metadata,BrnArchitectureControls.metadataIdentity(parent));
        assertEquals(BrnResearchComparison.digest(out.resolve("result.json")),BrnArchitectureControls.metadataIdentity(out));
        assertEquals(payload,BrnResearchComparison.digest(data.resolve("positions.bin")));
        for(double gain:new double[]{.125,.25,.5,1}) {
            var reference=BrnArchitectureControls.loadView(parent,gain);
            var actual=BrnArchitectureControls.loadView(out,gain);
            var requests=BrnArchitectureTransitions.requests(Arrays.asList(boards));
            var verification=BrnArchitectureTransitions.verify(requests,actual.evaluator().get(),reference.evaluator());
            assertEquals(0,verification.get("maximumAbsoluteIntegerScoreDifference"));
            for(var board:boards) {
                assertEquals(reference.pawns().applyAsDouble(board),actual.pawns().applyAsDouble(board),1e-10);
                assertEquals(reference.outcome().applyAsDouble(board),actual.outcome().applyAsDouble(board),0);
            }
        }
        assertThrows(IOException.class,()->BrnTupleCompilation.main(new String[]{data.toString(),parent.toString(),out.toString()}));
        var noParity=new LinkedHashMap<String,Object>(report);noParity.remove("parityPassed");
        DataFiles.write(out.resolve("result.json"),noParity);
        assertThrows(IOException.class,()->BrnArchitectureControls.loadView(out,.25));
        noParity.put("parityPassed",true);noParity.put("sourceModelSha256","altered");
        DataFiles.write(out.resolve("result.json"),noParity);
        assertThrows(IOException.class,()->BrnArchitectureControls.loadView(out,.25));
        DataFiles.write(out.resolve("result.json"),report);
        DataFiles.write(parent.resolve("result.json"),Map.of("kind","TUPLE","complete",true,"selectedModelSha256","altered"));
        assertThrows(IOException.class,()->BrnTupleCompilation.main(new String[]{data.toString(),parent.toString(),temp.resolve("invalid").toString()}));
        assertFalse(Files.exists(temp.resolve("invalid")));
    }
}
