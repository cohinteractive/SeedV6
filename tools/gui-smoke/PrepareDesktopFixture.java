package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.service.*;
import java.nio.file.*;
import java.util.*;
import static com.ohinteractive.seedv6.training.service.LearningArenaConfig.*;

/** Small real stores for native acceptance. Refuses reuse and requires isolated preferences. */
public final class PrepareDesktopFixture {
    public static void main(String[] args) throws Exception {
        if (!"com.ohinteractive.seedv6.validation.IsolatedPreferencesFactory".equals(
                System.getProperty("java.util.prefs.PreferencesFactory")))
            throw new IllegalStateException("Disposable preference factory is required");
        if (args.length != 1) throw new IllegalArgumentException("Provide a new disposable output directory");
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        Files.createDirectory(root);
        Path base = root.resolve("networks"), source = root.resolve("prepared-positions.jsonl");
        var text = new StringBuilder();
        for (int i = 0; i < 128; i++) text.append("{\"fen\":\"4k3/8/8/8/3pP3/8/8/4K3 w - - 0 1\",\"evals\":[{\"depth\":20,\"pvs\":[{\"cp\":" + i + "}]}]}\n");
        Files.writeString(source, text);
        var data = DataSource.register("WU4 prepared fixture", source, 1);
        var prefs = TrainingSettings.preferences();
        var folders = new TrainingFolders(prefs); folders.base(base);
        var identities = new Properties();
        for (String side : List.of("white", "black")) {
            var entry = TrainingLineages.create(base, NetworkArchitecture.BRN_PAIR2, "WU4 " + side);
            var settings = new TrainingSettings(entry.root(), 1, 1, 2, 0, 0, 2, 4, 1, 2, 71, 8, 1,
                    NetworkArchitecture.BRN_PAIR2, .001, .001, .001,
                    TrainingSource.dataSources(DataSources.directory(entry.root())), "", null)
                    .withCorpus(DataSources.directory(entry.root()).toString(), new CorpusTrainingConfig(4))
                    .withValidationMethod(ValidationMethod.HELD_OUT).withLearningRate(.01);
            new DataSources(1, List.of(data), false).save(DataSources.directory(entry.root()));
            TrainingLineages.save(TrainingLineages.read(entry), settings);
            String checkpoint;
            try (var store = new CheckpointStore(entry.root(), TrainingArchitecture.BRN_PAIR2)) {
                checkpoint = store.initialize(NetworkTrainingState.initialized(TrainingArchitecture.BRN_PAIR2,
                        side.equals("white") ? 71 : 97, .01), new CheckpointManifest.Metadata(0, 1, "")).manifest().id();
            }
            folders.register(base, NetworkArchitecture.BRN_PAIR2, entry.root());
            var identity = TrainingLineages.read(entry).lineage().id().toString();
            identities.setProperty(side + ".root", entry.root().toString());
            identities.setProperty(side + ".lineage", identity);
            identities.setProperty(side + ".checkpoint", checkpoint);
            for (String selection : side.equals("white") ? List.of("white", "opponent") : List.of("black")) {
                var node = prefs.node("play").node(selection);
                node.put("store", entry.root().toString()); node.put("generation", checkpoint); node.put("lineageId", identity);
            }
            if (side.equals("white")) settings.save(prefs);
        }
        var arena = new Arena(2, Limit.DEPTH, 1, 100, 1, 0, 0, 4, "7k/5K2/6Q1/8/8/8/8/8 w - - 0 1");
        var learning = new LearningArenaConfig("WU4 bounded learning", new Competitor("A", TrainingArchitecture.BRN3, 71, 2),
                new Competitor("B", TrainingArchitecture.BRN3, 97, 2), data, 2, 1, 1, 71, arena);
        var match = new LearningArenaConfig("WU4 fixed match", Competitor.handcrafted(), Competitor.handcrafted(),
                null, 0, 0, 0, 71, arena, Mode.MATCH_ONLY);
        for (var config : List.of(learning, match)) {
            Path campaign = root.resolve(config.matchOnly() ? "match-campaign" : "learning-campaign");
            try (var service = LearningArenaService.create(campaign, config, ignored -> {})) {
                identities.setProperty(campaign.getFileName() + ".id", service.state().id());
            }
        }
        folders.rememberArenaCampaign(root.resolve("learning-campaign"));
        prefs.flush();
        try (var output = Files.newOutputStream(root.resolve("fixture-identities.properties"))) {
            identities.store(output, "Initial disposable model and campaign identities");
        }
        System.out.println("Prepared disposable desktop fixtures at " + root);
    }
}
