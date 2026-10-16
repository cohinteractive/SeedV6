import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.training.model.*;

/** Run unchanged against the frozen application and migrated distribution.
 * Reads only explicit payloads; never opens a writable checkpoint store. */
public final class UnifiedModelCompatibilityProbe {
    public static void main(String[] args) throws Exception {
        Path checkpoint = Path.of(args[0]);
        byte[] saved = Files.readAllBytes(checkpoint.resolve("training.state"));
        NetworkTrainingState state = NetworkTrainingState.read(TrainingArchitecture.BRN_PAIR2,
                new ByteArrayInputStream(saved));
        require(Arrays.equals(saved, encode(state)), "Pinned optimizer bytes changed");
        byte[] model = Files.readAllBytes(checkpoint.resolve("network.brn-pair2"));
        require(Arrays.equals(model, encode(state.snapshot())), "Pinned model recompilation differs");
        System.out.println("PINNED\t" + state.step() + "\t" + hash(saved) + "\t" + hash(model));
        // Continue a private decoded copy for one tiny batch; never publish it.
        var trainer = ((NetworkTrainingState.BrnPair2) state).trainer();
        trainer.trainBatch(new long[][] {Board.startingPosition(),
                Board.fromFen("4k3/P7/8/8/8/8/7p/4K3 b - - 0 1"),
                Board.fromFen("r3k2r/8/8/3pP3/8/8/8/R3K2R w KQkq d6 0 1")},
                new double[] {.1, -.2, .3}, 3);
        System.out.println("CONTINUED\t" + state.step() + "\t" + hash(encode(state))
                + "\t" + hash(encode(state.snapshot())));
        for (TrainingArchitecture architecture : TrainingArchitecture.values()) {
            var initial = NetworkTrainingState.initialized(architecture, 2402L, .001);
            byte[] bytes = encode(initial);
            var decoded = NetworkTrainingState.read(architecture, new ByteArrayInputStream(bytes));
            require(Arrays.equals(bytes, encode(decoded)), "Optimizer round trip: " + architecture);
            byte[] compiled = encode(initial.snapshot());
            require(Arrays.equals(compiled, encode(decoded.snapshot())), "Compilation: " + architecture);
            var runtime = NetworkModel.read(architecture, new ByteArrayInputStream(compiled));
            require(Arrays.equals(compiled, encode(runtime)), "Model round trip: " + architecture);
            System.out.println(architecture + "\t" + hash(bytes) + "\t" + hash(compiled));
        }
    }
    static byte[] encode(NetworkTrainingState state) throws IOException {
        var bytes = new ByteArrayOutputStream(); state.write(bytes); return bytes.toByteArray();
    }
    static byte[] encode(NetworkModel model) throws IOException {
        var bytes = new ByteArrayOutputStream(); model.write(bytes); return bytes.toByteArray();
    }
    static String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
