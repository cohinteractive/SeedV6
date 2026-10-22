import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.core.brnpair2.BrnPair2Codec;
import com.ohinteractive.seedv6.core.brnpair2.BrnPair2Model;
import com.ohinteractive.seedv6.core.brnpair2.ProductionModel;
import java.nio.file.Path;
import java.util.Arrays;

/** Build-time verifier, never included in production JARs. Never starts a GUI. */
public final class PackagingProbe {
    public static void main(String[] args) throws Exception {
        ProductionModel.load(Path.of(args[1]));
        var version = ApplicationVersion.load();
        var baseline = com.ohinteractive.seedv6.search.driver.BaselineEngine.load();
        System.out.println("baseline.identity=" + baseline.identity(1, 64));
        // Load all supported competitive resources from the actual packaged core.
        for (var choice : com.ohinteractive.seedv6.search.driver.BaselineEngine.Evaluator.values())
            com.ohinteractive.seedv6.search.driver.BaselineEngine.load(choice);
        System.out.println("model.sha256=" + ProductionModel.SHA256);
        System.out.println("model.lineage=" + ProductionModel.LINEAGE);
        System.out.println("model.checkpoint=" + ProductionModel.CHECKPOINT);
        System.out.println("model.bytes=" + BrnPair2Codec.MODEL_BYTES);
        System.out.println("model.schema=seedv6.brn.pair2");
        System.out.println("model.schemaVersion=" + BrnPair2Codec.VERSION);
        System.out.println("model.evaluator=BRN_PAIR2");
        System.out.println("model.calibration=" + BrnPair2Model.CALIBRATION_ID);
        System.out.println("engine.acceptedSource=" + ProductionModel.ENGINE_SOURCE);
        System.out.println("version=" + version.version());
        System.out.println("build=" + version.build());
        if (!args[0].equals("gui")) return;
        if (!java.awt.GraphicsEnvironment.isHeadless()) throw new AssertionError("Probe must be headless");
        ClassLoader loader = PackagingProbe.class.getClassLoader();
        for (String name : new String[]{"gui.SwingLauncher", "gui.ChessFrame", "gui.TrainingPanel",
                "gui.LearningArenaPanel", "training.service.TrainerService"})
            Class.forName("com.ohinteractive.seedv6." + name, false, loader);
        for (String forbidden : new String[]{"com.ohinteractive.seedv6.Main",
                "com.ohinteractive.seedv6.uci.UciMain", "com.ohinteractive.seedv6.verification.ExactSearchHarness"}) {
            try { Class.forName(forbidden, false, loader); throw new AssertionError(forbidden); }
            catch (ClassNotFoundException expected) { }
        }
        Class.forName("jdk.incubator.vector.FloatVector", false, loader);
        Class.forName("com.sun.jna.Native").getField("POINTER_SIZE").getInt(null);
        Class.forName("com.formdev.flatlaf.FlatDarkLaf", false, loader);
        Class<?> zstd = Class.forName("com.github.luben.zstd.Zstd");
        byte[] input = "SeedV6 packaged JNI verification".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] compressed = (byte[]) zstd.getMethod("compress", byte[].class).invoke(null, (Object) input);
        byte[] decoded = (byte[]) zstd.getMethod("decompress", byte[].class, int.class).invoke(null, compressed, input.length);
        if (!Arrays.equals(input, decoded)) throw new AssertionError("Zstandard JNI round trip");
        Class.forName("org.sqlite.JDBC");
        try (var connection = java.sql.DriverManager.getConnection("jdbc:sqlite::memory:");
             var query = connection.createStatement(); var rows = query.executeQuery("select 35")) {
            if (!rows.next() || rows.getInt(1) != 35) throw new AssertionError("SQLite JNI");
        }
        for (String set : new String[]{"original", "legacy"})
            for (String side : new String[]{"w", "b"})
                for (String piece : new String[]{"k", "q", "r", "b", "n", "p"})
                    if (loader.getResource("com/ohinteractive/seedv6/gui/pieces/" + set + "/" + side + piece + ".png") == null)
                        throw new AssertionError("Missing presentation resource");
        if (loader.getResource("native/" + System.mapLibraryName("seedv6_syzygy")) != null) {
            var nativeLoader = Class.forName("com.ohinteractive.seedv6.search.tablebase.SyzygyNative")
                    .getDeclaredMethod("loadLibrary", String.class);
            nativeLoader.setAccessible(true);
            nativeLoader.invoke(null, "");
            System.out.println("gui.optionalSyzygy=loaded");
        } else System.out.println("gui.optionalSyzygy=not-bundled");
        System.out.println("gui.headlessResourcesAndJni=passed");
    }
}
