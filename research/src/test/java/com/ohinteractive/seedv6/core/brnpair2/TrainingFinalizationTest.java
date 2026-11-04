package com.ohinteractive.seedv6.core.brnpair2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real Java serialization / Python validation and cross-language lease checks.
 * No fitting, corpus loading, tournaments, or retained research artifacts.
 */
class TrainingFinalizationTest {
    @TempDir Path temporary;

    Path repository() {
        Path root = Path.of("").toAbsolutePath();
        while (!Files.isDirectory(root.resolve("tools/conventional-nnue"))) root = root.getParent();
        return root;
    }

    void python(String program, String... args) throws Exception {
        var command = new ArrayList<>(List.of("python", "-B", "-c", program));
        command.addAll(List.of(args));
        var process = new ProcessBuilder(command).directory(repository().toFile()).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(0, process.waitFor(), output);
    }

    @Test void nativeStatesPassIndependentFinalizationReaders() throws Exception {
        var workflows = new LinkedHashMap<String, byte[]>();
        var bytes = new java.io.ByteArrayOutputStream();
        new TrainableOne(8, 4, .01, 71).write(bytes); workflows.put("OneTraining", bytes.toByteArray()); bytes.reset();
        new SharedGeometryPieceNetwork(true, .01, 71).write(bytes); workflows.put("SharedGeometryTrainingDiagnostic", bytes.toByteArray()); bytes.reset();
        new TiedGeometryPieceNetwork(true, .01, 71).write(bytes); workflows.put("TiedGeometryTrainingDiagnostic", bytes.toByteArray()); bytes.reset();
        new MixedTiedPieceNetwork(true, .01, 71).write(bytes); workflows.put("MixedTiedTrainingDiagnostic", bytes.toByteArray()); bytes.reset();
        new ContextGatedPieceNetwork(true, .01, 71).write(bytes); workflows.put("ContextGatedTrainingDiagnostic", bytes.toByteArray()); bytes.reset();
        BrnPair2Codec.writeModel(new BrnPair2Model(new BrnPair2Trainer(.01).snapshot()), bytes);
        workflows.put("PairObjectiveDiagnostic", bytes.toByteArray());
        for (var entry : workflows.entrySet()) {
            Path state = temporary.resolve(entry.getKey()); Files.write(state, entry.getValue());
            python("import sys;sys.path.insert(0,'tools');import training_lifecycle as t;t.verify_model(sys.argv[1],sys.argv[2])",
                   state.toString(), entry.getKey());
        }
    }

    @Test void javaRequiresLivePythonLeaseAndRejectsTerminalRun() throws Exception {
        // Use this test class's tiny main entrypoint in a separate JVM so the
        // native Java lock competes with the actual Python lock, not a mock.
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classpath = System.getProperty("java.class.path");
        String program = """
            import json,os,pathlib,subprocess,sys
            sys.path.insert(0,'tools')
            import training_lifecycle as t
            root=pathlib.Path(sys.argv[1]); config=root/'config.json'
            config.write_text(json.dumps(dict(output=str(root/'run'),training_finalization=t.POLICY)))
            command=[sys.argv[2],'-cp',sys.argv[3],'com.ohinteractive.seedv6.core.brnpair2.TrainingFinalizationTest',str(config)]
            t.require_java_guard(command,'OneTraining')
            with t.training(config,'OneTraining'):
                good=subprocess.run(command,capture_output=True,text=True)
                assert good.returncode==0,good.stdout+good.stderr
                missing=dict(os.environ);missing.pop('SEED_TRAINING_TOKEN',None)
                assert subprocess.run(command,env=missing,capture_output=True).returncode!=0
            assert subprocess.run(command,capture_output=True).returncode!=0
            owner=t.read_json(root/'run'/t.OWNER);owner['status']='finalized'
            t.atomic(root/'run',t.OWNER,owner)
            assert subprocess.run(command,capture_output=True).returncode!=0
            """;
        python(program, temporary.toString(), java, classpath);
    }

    public static void main(String[] args) throws Exception {
        TrainingRunGuard.check(args[0]);
    }
}
