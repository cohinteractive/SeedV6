package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.core.brnnext.BrnNextBase;
import com.ohinteractive.seedv6.core.brnpair2.ProductionModel;
import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.service.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Explicit preparation only. Never starts training or changes a pre-existing lineage. */
public final class BrnResidualWave {
    static final String WAVE="brn-successor-2026-10-20-wave3";
    static final List<NetworkArchitecture> ARCHITECTURES=List.of(
            NetworkArchitecture.BRN_GEN32_GATED,NetworkArchitecture.BRN_GEN32_CUBIC,NetworkArchitecture.BRN_PAIR2);
    public record Entry(String candidate,String name,String architecture,String root,UUID lineageId,
                        String initialCheckpoint,String initialNetworkSha256,String initialTrainingSha256,long initialOptimizerStep,
                        String currentCheckpoint,long currentGeneration,TrainingRecipe recipe,long maximumGenerations,
                        String sourceSelectionIdentity,String partition) {}
    public record Report(String wave,String recorded,String library,CheckpointManifest reference,List<DataSource> sources,List<Entry> entries) {}
    static UUID id(NetworkArchitecture architecture) {
        return UUID.nameUUIDFromBytes((WAVE+":"+architecture.name()).getBytes(StandardCharsets.UTF_8));
    }
    static String candidate(NetworkArchitecture architecture) {
        return switch(architecture) {
            case BRN_GEN32_GATED -> "W3-RESGATE";case BRN_GEN32_CUBIC -> "W3-RESCUBIC";
            case BRN_PAIR2 -> "W3-BRE-CONTROL";default -> throw new IllegalArgumentException();
        };
    }
    static TrainingSettings settings(Path root,NetworkArchitecture architecture) {
        return new TrainingSettings(root,4,1,64,0,8,32,128,8,64,1,1024,32,architecture)
                .withSource(TrainingSource.dataSources(DataSources.directory(root)))
                .withCorpus("",new CorpusTrainingConfig(131072,"").forArchitecture(architecture.trainingArchitecture()))
                .withValidationMethod(ValidationMethod.HELD_OUT)
                .withLearningRate(.003);
    }
    /** The owner entry point supplies verified Gen32; isolated tests may supply an explicit fixture control. */
    static List<Entry> prepare(Path base,List<DataSource> sources,NetworkTrainingState control,String controlIdentity) throws IOException {
        base=base.toAbsolutePath().normalize();
        if(!Files.isDirectory(base,LinkOption.NOFOLLOW_LINKS)||Files.isSymbolicLink(base))throw new IOException("Existing unlinked library required");
        if(!(control instanceof NetworkTrainingState.BrnPair2))throw new IOException("Additive control must retain Pair2 optimizer state");
        var selection=new DataSources(1,sources,false);selection.verify();
        var entries=new ArrayList<Entry>();
        for(var architecture:ARCHITECTURES) {
            var implementation=architecture.trainingArchitecture();
            for(var source:sources) {
                source.requireReady();
                String problem=TrainingDataLibrary.incompatibility(source,List.of(implementation));
                if(!problem.isEmpty())throw new IOException(problem);
            }
            Path parent=base.resolve(architecture.folderName()),root=parent.resolve(id(architecture).toString());
            if(Files.isSymbolicLink(parent)||Files.isSymbolicLink(root))throw new IOException("Linked wave path refused");
            String name=candidate(architecture);
            if(Files.notExists(root)) {
                Files.createDirectories(parent);Files.createDirectory(root);
                var settings=settings(root,architecture);
                var lineage=new TrainingLineage(id(architecture),name,implementation,Instant.now(),
                        LineageConfiguration.encode(settings),WAVE+(architecture==NetworkArchitecture.BRN_PAIR2
                                ? ": continued frozen Gen32 weights and Adam state; new generation counter starts at zero"
                                : ": frozen Gen32 base plus a fresh seed1, zero-output correction"));
                var initial=architecture==NetworkArchitecture.BRN_PAIR2?control:
                        NetworkTrainingState.initialized(implementation,settings.seed(),.003);
                try(var store=new CheckpointStore(root,implementation)) {
                    store.requireEmptyForBootstrap();store.writeLineage(lineage);
                    selection.save(DataSources.directory(root));store.writeTrainingSource(settings.source());
                    var checkpoint=store.initialize(initial,new CheckpointManifest.Metadata(0,settings.depth(),""));
                    String origin=architecture==NetworkArchitecture.BRN_PAIR2?WAVE+": cloned "+controlIdentity:
                            WAVE+": zero correction; base="+BrnNextBase.SHA256;
                    store.writeProvenance(new LineageProvenance(checkpoint.manifest().id(),implementation,origin,
                            architecture==NetworkArchitecture.BRN_PAIR2?null:settings.seed(),settings.seed(),Instant.now()));
                }
            }
            var lineage=TrainingLineage.read(root).orElseThrow(()->new IOException("Incomplete wave lineage"));
            if(!lineage.id().equals(id(architecture))||lineage.architecture()!=implementation)throw new IOException("Wave identity collision");
            var provenance=LineageProvenance.read(root).orElseThrow(()->new IOException("Missing wave provenance"));
            if(!provenance.initializer().startsWith(WAVE+":"))throw new IOException("Foreign wave bootstrap");
            var selected=TrainingLineages.read(new TrainingLineages.Entry(root,architecture,lineage.name()));
            var library=ModelLibrary.browse(ModelLibrary.entry(root,implementation));
            var initial=library.generations().stream().map(g->g.checkpoint().manifest())
                    .filter(m->m.id().equals(provenance.initialCheckpoint())).findFirst().orElseThrow();
            var current=library.latest().orElseGet(()->library.best().orElseThrow()).checkpoint().manifest();
            CheckpointStore.readTrainingSnapshot(root,current.id());
            entries.add(new Entry(candidate(architecture),lineage.name(),implementation.name(),root.toString(),lineage.id(),
                    initial.id(),initial.networkSha256(),initial.trainingSha256(),initial.optimizerStep(),current.id(),current.generation(),
                    selected.settings().recipe(),selected.settings().maximumGenerations(),
                    DataSources.read(DataSources.directory(root)).identity(),
                    implementation.brnNext()?BrnNextPartition.ID:"Existing Pair2 sequential train/validation acquisition"));
        }
        return List.copyOf(entries);
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=2||!args[0].equals("--create"))throw new IllegalArgumentException("--create NEW_REPORT.json");
        Path report=Path.of(args[1]);if(Files.exists(report))throw new IOException("Preserve previous report");
        String configured=TrainingSettings.preferences().get("baseTrainingRoot","");
        if(configured.isBlank())throw new IOException("Existing GUI library configuration required");
        Path base=Path.of(configured).toAbsolutePath().normalize();
        var catalog=new TrainingDataLibrary(base).browse();
        if(!catalog.diagnostics().isEmpty())throw new IOException("Training Data catalog diagnostics: "+catalog.diagnostics());
        var sources=new ArrayList<DataSource>();
        for(String identity:List.of(
                "1b11dda4d7234dd43dd0c49100f717fff5cd18d775bd92d0a80c7b4d6d513ce1",
                "d8e423b9c6ab7ca56b3383d52d09d0a3c36611c6e42f517316a2cbe448c58842")) {
            var source=catalog.sources().stream().filter(s->s.identity().equals(identity)).findFirst()
                    .orElseThrow(()->new IOException("Required audited source unavailable: "+identity));
            sources.add(source.withDisplay(source.name(),1));
        }
        Path reference=base.resolve(NetworkArchitecture.BRN_PAIR2.folderName()).resolve(ProductionModel.LINEAGE);
        var snapshot=CheckpointStore.readSnapshot(reference,ProductionModel.CHECKPOINT);
        if(!snapshot.manifest().networkSha256().equals(BrnNextBase.SHA256))throw new IOException("Frozen reference mismatch");
        var state=CheckpointStore.readTrainingSnapshot(reference,ProductionModel.CHECKPOINT);
        if(!snapshot.manifest().trainingSha256().equals("f8bdda5eda0ff7893ee36c2270baaedecaf50e14ff5202ab786996f72a2788d5"))
            throw new IOException("Frozen optimizer identity mismatch");
        var entries=prepare(base,sources,state,ProductionModel.CHECKPOINT);
        DataFiles.write(report,new Report(WAVE,Instant.now().toString(),base.toString(),snapshot.manifest(),sources,entries));
        System.out.println("Prepared/verified three separate GUI lineages; no training: "+report);
    }
    private BrnResidualWave() {}
}
