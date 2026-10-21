package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.training.checkpoint.*;
import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.model.*;
import com.ohinteractive.seedv6.training.service.*;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Explicit degree-control continuation preparation. Source stores are locked and read, never trained or rewritten. */
public final class BrnDegreeControlWave {
    static final String WAVE="brn-successor-2026-10-21-wave6",ORIGIN="degree-continuation-origin.json";
    static final String COMPACT_WAVE="brn-successor-2026-10-21-wave7";
    public record Arm(String name,BrnContinuationWave.Source source,double rate,String projection) {
        public Arm {
            if(name==null||!(name.startsWith("W6-")||name.startsWith("W7-"))||source==null||rate!=.001&&rate!=.003)
                throw new IllegalArgumentException("Explicit W6 or W7 arm required");
            if(projection==null||!Set.of("exact","quadratic").contains(projection))
                throw new IllegalArgumentException("Explicit exact or quadratic projection required");
        }
    }
    public record Plan(String id,String library,List<Arm> arms) {}
    public record Origin(int version,String wave,Arm arm,String sourceLedgerSha256,String selectionSha256,
                         String bootstrap,String networkSha256,String trainingSha256,long inheritedStep) {}
    public record Entry(String candidate,String architecture,String root,UUID lineageId,Origin origin,
                        String latest,long latestGeneration,TrainingRecipe recipe,long additionalGenerations,
                        Map<String,Long> sourceOffsets,String partition) {}
    public record Report(String wave,String recorded,String library,List<Entry> entries) {}
    static String wave(Arm arm) { return arm.name().startsWith("W6-")?WAVE:COMPACT_WAVE; }
    static UUID id(Arm arm) {
        return UUID.nameUUIDFromBytes((wave(arm)+":"+arm.name()+":"+arm.source().checkpoint()+":"+arm.rate()+":"+arm.projection())
                .getBytes(StandardCharsets.UTF_8));
    }
    static String hash(byte[] bytes) { return BrnContinuationWave.hash(bytes); }
    static NetworkTrainingState project(NetworkTrainingState source,Arm arm) throws IOException {
        if(source.hyperparameters().learningRate()!=arm.rate())throw new IOException("Source rate differs; no silent optimizer change");
        if(arm.projection().equals("exact"))return source;
        var expected=wave(arm).equals(WAVE)?TrainingArchitecture.BRN_GEN32_CUBIC:TrainingArchitecture.BRN_GEN32_CUBIC_R4;
        if(!(source instanceof NetworkTrainingState.BrnNext n)||source.architecture()!=expected)
            throw new IOException("Quadratic projection requires the wave's explicitly bound cubic rank");
        return new NetworkTrainingState.BrnNext(n.trainer().projectQuadratic());
    }
    static List<Entry> prepare(Path library,List<Arm> arms) throws IOException {
        library=library.toAbsolutePath().normalize();
        if(!Files.isDirectory(library,LinkOption.NOFOLLOW_LINKS)||Files.isSymbolicLink(library))throw new IOException("Existing unlinked library required");
        if(arms.isEmpty()||arms.stream().map(Arm::name).distinct().count()!=arms.size())throw new IOException("Distinct explicit arms required");
        if(arms.stream().map(BrnDegreeControlWave::wave).distinct().count()!=1)throw new IOException("Mixed degree-control waves refused");
        var entries=new ArrayList<Entry>();
        for(var arm:arms) {
            var source=arm.source();Path original=Path.of(source.root()).toAbsolutePath().normalize();
            if(Files.isSymbolicLink(original)||source.generation()<1)throw new IOException("Stopped nonzero source required");
            try(var channel=FileChannel.open(original.resolve("store.lock"),StandardOpenOption.WRITE);var lock=channel.tryLock()) {
                if(lock==null)throw new IOException("Source has an active writer");
                if(!CheckpointInspection.reference(original,"latest-training").equals(source.checkpoint()))throw new IOException("Source latest changed");
                var manifest=CheckpointStore.readSnapshot(original,source.checkpoint()).manifest();
                if(manifest.generation()!=source.generation()||!manifest.networkSha256().equals(source.networkSha256())
                        ||!manifest.trainingSha256().equals(source.trainingSha256()))throw new IOException("Source checkpoint identity differs");
                var allowed=wave(arm).equals(WAVE)
                        ?Set.of(TrainingArchitecture.BRN_GEN32_CUBIC,TrainingArchitecture.BRN_GEN32_CUBIC_R4,TrainingArchitecture.BRN_PAIR2)
                        :Set.of(TrainingArchitecture.BRN_GEN32_CUBIC_R4,TrainingArchitecture.BRN_PAIR2);
                if(!allowed.contains(manifest.architecture()))throw new IOException("Degree-control source architecture differs");
                if(TrainingSession.read(original).filter(s->s.state()==TrainingSession.State.ACTIVE).isPresent()
                        ||GenerationAttempt.inspect(original).filter(a->a.generation()>manifest.generation()).isPresent()
                        ||PartialGeneration.inspect(original).isPresent())
                    throw new IOException("Source session/attempt is unsettled");
                if(!manifest.parentId().isEmpty()&&!CheckpointInspection.validations(original).containsKey(manifest.id()))
                    throw new IOException("Source candidate is unsettled");
                var sourceArchitecture=NetworkArchitecture.valueOf(manifest.architecture().name());
                var selectedSource=TrainingLineages.read(new TrainingLineages.Entry(original,sourceArchitecture,source.candidate()));
                Path data=DataSources.directory(original);var selection=DataSources.read(data);selection.verify();
                for(var item:selection.sources())item.requireReady();
                var ledger=new SourceLedger(original);
                if(ledger.active().isPresent()||ledger.state().reservations().stream().anyMatch(r->r.status()!=SourceLedger.Status.COMPLETED
                        ||r.generation()>manifest.generation()))throw new IOException("Settled source reservations required");
                ContinuationSeek.verify(selection,ledger.state().next(),data.resolve("seek"));
                byte[] ledgerBytes=Files.readAllBytes(data.resolve("cursors.json")),selectionBytes=Files.readAllBytes(data.resolve("sources.json")),
                        configBytes=Files.readAllBytes(data.resolve("configuration.json"));
                var corpus=DataFiles.read(data.resolve("configuration.json"),CorpusTrainingConfig.class);
                if(selectedSource.settings().seed()!=source.seed()||corpus.positionsPerGeneration()!=source.positionsPerGeneration()
                        ||!hash(configBytes).equals(source.corpusConfigurationSha256()))throw new IOException("Source acquisition settings differ");
                var sourceState=CheckpointStore.readTrainingSnapshot(original,source.checkpoint());byte[] sourceBytes=sourceState.encode();
                if(!hash(sourceBytes).equals(source.trainingSha256()))throw new IOException("Source optimizer re-encoding differs");
                var state=project(sourceState,arm);byte[] stateBytes=state.encode();
                var networkBytes=new ByteArrayOutputStream();state.snapshot().write(networkBytes);
                String expectedNetworkHash=hash(networkBytes.toByteArray());
                if(!Arrays.equals(sourceBytes,sourceState.encode()))throw new IOException("Projection mutated source state");
                var architecture=NetworkArchitecture.valueOf(state.architecture().name());
                Path parent=library.resolve(architecture.folderName()),root=parent.resolve(id(arm).toString());
                if(Files.isSymbolicLink(parent)||Files.isSymbolicLink(root))throw new IOException("Linked destination refused");
                if(Files.notExists(root)) {
                    Files.createDirectories(parent);Files.createDirectory(root);
                    var settings=BrnContinuationWave.settings(root,architecture,arm.rate(),corpus,source.seed());
                    var lineage=new TrainingLineage(id(arm),arm.name(),state.architecture(),Instant.now(),LineageConfiguration.encode(settings),
                            wave(arm)+": "+(arm.projection().equals("exact")?"exact continuation":"explicit degree2 projection; third-degree heads and moments omitted")+" of "+source.checkpoint());
                    try(var store=new CheckpointStore(root,state.architecture())) {
                        store.requireEmptyForBootstrap();store.writeLineage(lineage);store.writeTrainingSource(settings.source());
                        Files.createDirectories(DataSources.directory(root));
                        Files.write(DataSources.directory(root).resolve("sources.json"),selectionBytes,StandardOpenOption.CREATE_NEW);
                        var bootstrap=store.initialize(state,new CheckpointManifest.Metadata(source.generation(),manifest.trainingDepth(),""));
                        if(!bootstrap.manifest().trainingSha256().equals(hash(stateBytes))
                                ||!bootstrap.manifest().networkSha256().equals(expectedNetworkHash))throw new IOException("Bootstrap payload differs");
                        if(arm.projection().equals("exact")&&!bootstrap.manifest().networkSha256().equals(source.networkSha256()))throw new IOException("Exact control payload differs");
                        store.writeProvenance(new LineageProvenance(bootstrap.manifest().id(),state.architecture(),lineage.configurationOrigin(),null,source.seed(),Instant.now()));
                        Files.write(DataSources.directory(root).resolve("cursors.json"),ledgerBytes,StandardOpenOption.CREATE_NEW);
                        Files.write(DataSources.directory(root).resolve("configuration.json"),configBytes,StandardOpenOption.CREATE_NEW);
                        ContinuationSeek.inherit(selection,ledger.state().next(),data.resolve("seek"),DataSources.directory(root).resolve("seek"));
                        DataFiles.write(root.resolve(ORIGIN),new Origin(1,wave(arm),arm,hash(ledgerBytes),hash(selectionBytes),bootstrap.manifest().id(),
                                bootstrap.manifest().networkSha256(),bootstrap.manifest().trainingSha256(),state.step()));
                    }
                }
                var origin=DataFiles.read(root.resolve(ORIGIN),Origin.class);
                var lineage=TrainingLineage.read(root).orElseThrow(()->new IOException("Missing degree-control identity"));
                var provenance=LineageProvenance.read(root).orElseThrow(()->new IOException("Missing degree-control provenance"));
                if(origin.version()!=1||!origin.wave().equals(wave(arm))||!origin.arm().equals(arm)||!lineage.id().equals(id(arm))
                        ||!origin.sourceLedgerSha256().equals(hash(ledgerBytes))||!origin.selectionSha256().equals(hash(selectionBytes))
                        ||!origin.trainingSha256().equals(hash(stateBytes))||!origin.networkSha256().equals(expectedNetworkHash)
                        ||!provenance.initialCheckpoint().equals(origin.bootstrap()))
                    throw new IOException("Foreign or incomplete degree-control destination; preserved");
                var initial=CheckpointStore.readSnapshot(root,origin.bootstrap()).manifest();
                if(!initial.networkSha256().equals(origin.networkSha256())||!initial.trainingSha256().equals(origin.trainingSha256())
                        ||initial.generation()!=source.generation()||initial.optimizerStep()!=state.step()||initial.architecture()!=state.architecture())
                    throw new IOException("Degree-control bootstrap identity differs");
                var currentLedger=new SourceLedger(root).state();
                if(currentLedger.reservations().size()<ledger.state().reservations().size()
                        ||!currentLedger.reservations().subList(0,ledger.state().reservations().size()).equals(ledger.state().reservations()))
                    throw new IOException("Inherited reservation prefix differs");
                ContinuationSeek.verify(DataSources.read(DataSources.directory(root)),currentLedger.next(),DataSources.directory(root).resolve("seek"));
                var selected=TrainingLineages.read(new TrainingLineages.Entry(root,architecture,arm.name()));
                String latest=CheckpointInspection.reference(root,"latest-training");var current=CheckpointStore.readSnapshot(root,latest).manifest();
                entries.add(new Entry(lineage.name(),architecture.name(),root.toString(),lineage.id(),origin,latest,current.generation(),
                        selected.settings().recipe(),selected.settings().maximumGenerations(),currentLedger.next(),
                        state.architecture().brnNext()?BrnNextPartition.ID:"Existing Pair2 sequential train/validation acquisition"));
                if(!Arrays.equals(ledgerBytes,Files.readAllBytes(data.resolve("cursors.json")))
                        ||!Arrays.equals(selectionBytes,Files.readAllBytes(data.resolve("sources.json")))
                        ||!Arrays.equals(configBytes,Files.readAllBytes(data.resolve("configuration.json"))))throw new IOException("Source metadata changed");
            } catch(java.nio.channels.OverlappingFileLockException busy) {throw new IOException("Source has an active writer",busy);}
        }
        return List.copyOf(entries);
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=3||!args[0].equals("--create"))throw new IllegalArgumentException("--create PLAN NEW_REPORT");
        var plan=DataFiles.read(Path.of(args[1]),Plan.class);Path report=Path.of(args[2]);
        if(!Set.of(WAVE,COMPACT_WAVE).contains(plan.id())||plan.arms().isEmpty()
                ||plan.arms().stream().anyMatch(arm->!wave(arm).equals(plan.id()))||Files.exists(report))
            throw new IOException("Explicit matching degree-control plan and new report required");
        Path library=Path.of(plan.library()).toAbsolutePath().normalize();
        String configured=TrainingSettings.preferences().get("baseTrainingRoot","");
        if(configured.isBlank()||!Path.of(configured).toAbsolutePath().normalize().equals(library))throw new IOException("GUI library differs");
        DataFiles.write(report,new Report(plan.id(),Instant.now().toString(),library.toString(),prepare(library,plan.arms())));
        System.out.println("Prepared/verified degree-control continuation lineages; no training: "+report);
    }
    private BrnDegreeControlWave() {}
}
