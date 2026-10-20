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

/** Explicit exact-state continuation preparation. No training and no writes to source lineages. */
public final class BrnContinuationWave {
    static final String WAVE="brn-successor-2026-10-21-wave4";
    static final String ORIGIN="continuation-origin.json";
    static final List<Double> RATES=List.of(.001,.003);
    public record Source(String candidate,String root,String checkpoint,String networkSha256,
                         String trainingSha256,long generation,long seed,int positionsPerGeneration,
                         String corpusConfigurationSha256) {}
    public record Plan(String id,String library,List<Source> sources) {}
    public record Origin(int version,String wave,Source source,String ledgerSha256,String selectionSha256,
                         String bootstrap,long inheritedStep,double requestedRate) {}
    public record Entry(String candidate,String architecture,String root,UUID lineageId,Origin origin,
                        String latest,long latestGeneration,TrainingRecipe recipe,long additionalGenerations,
                        Map<String,Long> sourceOffsets,String partition) {}
    public record Report(String wave,String recorded,String library,List<Entry> entries) {}

    static String name(Source source,double rate) {
        return "W4-"+source.candidate()+"-"+(rate==.001?"LR001":"LR003");
    }
    static UUID id(Source source,double rate) {
        return UUID.nameUUIDFromBytes((WAVE+":"+source.checkpoint()+":"+rate).getBytes(StandardCharsets.UTF_8));
    }
    static String hash(byte[] bytes) { return HexFormat.of().formatHex(DataFiles.digest().digest(bytes)); }
    static TrainingSettings settings(Path root,NetworkArchitecture architecture,double rate,
                                     CorpusTrainingConfig corpus,long seed) {
        return new TrainingSettings(root,4,1,64,0,8,32,128,8,64,seed,1024,32,architecture)
                .withSource(TrainingSource.dataSources(DataSources.directory(root)))
                .withCorpus("",corpus.forArchitecture(architecture.trainingArchitecture()))
                .withValidationMethod(ValidationMethod.HELD_OUT).withLearningRate(rate);
    }
    static List<Entry> prepare(Path library,List<Source> specifications) throws IOException {
        return prepare(library,specifications,false);
    }
    private static List<Entry> prepare(Path library,List<Source> specifications,boolean repairingSeek) throws IOException {
        library=library.toAbsolutePath().normalize();
        if(!Files.isDirectory(library,LinkOption.NOFOLLOW_LINKS)||Files.isSymbolicLink(library))
            throw new IOException("Existing unlinked library required");
        if(specifications.isEmpty()||specifications.stream().map(Source::checkpoint).distinct().count()!=specifications.size())
            throw new IOException("Distinct explicit source checkpoints required");
        var results=new ArrayList<Entry>();
        for(var source:specifications) {
            Path sourceRoot=Path.of(source.root()).toAbsolutePath().normalize();
            if(Files.isSymbolicLink(sourceRoot)||source.generation()<1)throw new IOException("Stopped nonzero source required");
            // Existing writer lock only: never create/recover/reconfigure an original store.
            try(var channel=FileChannel.open(sourceRoot.resolve("store.lock"),StandardOpenOption.WRITE);
                var lock=channel.tryLock()) {
                if(lock==null)throw new IOException("Source lineage has an active writer");
                if(!CheckpointInspection.reference(sourceRoot,"latest-training").equals(source.checkpoint()))
                    throw new IOException("Source latest differs from the frozen continuation plan");
                var manifest=CheckpointStore.readSnapshot(sourceRoot,source.checkpoint()).manifest();
                if(manifest.generation()!=source.generation()||!manifest.networkSha256().equals(source.networkSha256())
                        ||!manifest.trainingSha256().equals(source.trainingSha256()))
                    throw new IOException("Source checkpoint identity differs");
                var architecture=NetworkArchitecture.valueOf(manifest.architecture().name());
                if(!Set.of(NetworkArchitecture.BRN_GEN32_GATED,NetworkArchitecture.BRN_GEN32_CUBIC,
                        NetworkArchitecture.BRN_PAIR2).contains(architecture))throw new IOException("Unsupported continuation architecture");
                if(TrainingSession.read(sourceRoot).filter(s->s.state()==TrainingSession.State.ACTIVE).isPresent()
                        ||GenerationAttempt.inspect(sourceRoot).filter(a->a.generation()>manifest.generation()).isPresent())
                    throw new IOException("Finish or safely stop the source session before preparation");
                if(!manifest.parentId().isEmpty()&&!CheckpointInspection.validations(sourceRoot).containsKey(manifest.id()))
                    throw new IOException("Unsettled source candidate");
                var sourceSettings=TrainingLineages.read(new TrainingLineages.Entry(sourceRoot,architecture,source.candidate())).settings();
                Path sourceData=DataSources.directory(sourceRoot);
                var selection=DataSources.read(sourceData);selection.verify();
                for(var item:selection.sources())item.requireReady();
                var ledger=new SourceLedger(sourceRoot);
                if(ledger.active().isPresent()||ledger.state().reservations().stream()
                        .anyMatch(r->r.status()!=SourceLedger.Status.COMPLETED||r.generation()>manifest.generation()))
                    throw new IOException("Continuation requires settled, completed source reservations");
                ContinuationSeek.verify(selection,ledger.state().next(),sourceData.resolve("seek"));
                var corpus=DataFiles.read(sourceData.resolve("configuration.json"),CorpusTrainingConfig.class);
                byte[] ledgerBytes=Files.readAllBytes(sourceData.resolve("cursors.json"));
                byte[] selectionBytes=Files.readAllBytes(sourceData.resolve("sources.json"));
                byte[] configurationBytes=Files.readAllBytes(sourceData.resolve("configuration.json"));
                if(sourceSettings.seed()!=source.seed()||corpus.positionsPerGeneration()!=source.positionsPerGeneration()
                        ||!hash(configurationBytes).equals(source.corpusConfigurationSha256()))
                    throw new IOException("Source acquisition settings differ from the frozen continuation plan");
                byte[] state=CheckpointStore.readTrainingSnapshot(sourceRoot,source.checkpoint()).encode();
                if(!hash(state).equals(source.trainingSha256()))throw new IOException("Training-state re-encoding differs");
                for(double rate:RATES) {
                    String candidate=name(source,rate);
                    Path parent=library.resolve(architecture.folderName()),root=parent.resolve(id(source,rate).toString());
                    if(Files.isSymbolicLink(parent)||Files.isSymbolicLink(root))throw new IOException("Linked continuation path refused");
                    if(Files.notExists(root)) {
                        Files.createDirectories(parent);Files.createDirectory(root);
                        var configured=settings(root,architecture,rate,corpus,sourceSettings.seed());
                        var lineage=new TrainingLineage(id(source,rate),candidate,manifest.architecture(),Instant.now(),
                                LineageConfiguration.encode(configured),WAVE+": inherited "+source.checkpoint()+"; new exposure starts after this bootstrap");
                        try(var store=new CheckpointStore(root,manifest.architecture())) {
                            store.requireEmptyForBootstrap();store.writeLineage(lineage);store.writeTrainingSource(configured.source());
                            Files.createDirectories(DataSources.directory(root));
                            Files.write(DataSources.directory(root).resolve("sources.json"),selectionBytes,StandardOpenOption.CREATE_NEW);
                            var cloned=NetworkTrainingState.read(manifest.architecture(),new ByteArrayInputStream(state));
                            var bootstrap=store.initialize(cloned,new CheckpointManifest.Metadata(manifest.generation(),manifest.trainingDepth(),""));
                            if(!bootstrap.manifest().networkSha256().equals(source.networkSha256())
                                    ||!bootstrap.manifest().trainingSha256().equals(source.trainingSha256()))
                                throw new IOException("Continuation changed checkpoint payloads; preserve incomplete destination");
                            store.writeProvenance(new LineageProvenance(bootstrap.manifest().id(),manifest.architecture(),
                                    WAVE+": exact continuation of "+source.checkpoint(),null,configured.seed(),Instant.now()));
                            Files.write(DataSources.directory(root).resolve("cursors.json"),ledgerBytes,StandardOpenOption.CREATE_NEW);
                            Files.write(DataSources.directory(root).resolve("configuration.json"),configurationBytes,StandardOpenOption.CREATE_NEW);
                            ContinuationSeek.inherit(selection,ledger.state().next(),sourceData.resolve("seek"),
                                    DataSources.directory(root).resolve("seek"));
                            var origin=new Origin(1,WAVE,source,hash(ledgerBytes),hash(selectionBytes),bootstrap.manifest().id(),cloned.step(),rate);
                            DataFiles.write(root.resolve(ORIGIN),origin);
                        }
                    }
                    // Existing destinations are inspected only; never reset user settings or training.
                    var origin=DataFiles.read(root.resolve(ORIGIN),Origin.class);
                    var lineage=TrainingLineage.read(root).orElseThrow(()->new IOException("Missing continuation identity"));
                    var provenance=LineageProvenance.read(root).orElseThrow(()->new IOException("Missing continuation provenance"));
                    if(origin.version()!=1||!origin.wave().equals(WAVE)||!origin.source().equals(source)
                            ||origin.requestedRate()!=rate||!origin.ledgerSha256().equals(hash(ledgerBytes))
                            ||!origin.selectionSha256().equals(hash(selectionBytes))||!lineage.id().equals(id(source,rate))
                            ||!provenance.initialCheckpoint().equals(origin.bootstrap()))
                        throw new IOException("Foreign or incomplete continuation destination; preserved");
                    var initial=CheckpointStore.readSnapshot(root,origin.bootstrap()).manifest();
                    if(!initial.networkSha256().equals(source.networkSha256())||!initial.trainingSha256().equals(source.trainingSha256())
                            ||initial.generation()!=source.generation()||initial.optimizerStep()!=origin.inheritedStep())
                        throw new IOException("Continuation bootstrap mismatch");
                    var currentLedger=new SourceLedger(root).state();
                    if(currentLedger.reservations().size()<ledger.state().reservations().size()
                            ||!currentLedger.reservations().subList(0,ledger.state().reservations().size()).equals(ledger.state().reservations()))
                        throw new IOException("Inherited reservation prefix differs");
                    if(!repairingSeek)ContinuationSeek.verify(DataSources.read(DataSources.directory(root)),
                            currentLedger.next(),DataSources.directory(root).resolve("seek"));
                    var selected=TrainingLineages.read(new TrainingLineages.Entry(root,architecture,candidate));
                    String latest=CheckpointInspection.reference(root,"latest-training");
                    var current=CheckpointStore.readSnapshot(root,latest).manifest();
                    results.add(new Entry(lineage.name(),architecture.name(),root.toString(),lineage.id(),origin,latest,current.generation(),
                            selected.settings().recipe(),selected.settings().maximumGenerations(),currentLedger.next(),
                            manifest.architecture().brnNext()?BrnNextPartition.ID:"Existing Pair2 sequential train/validation acquisition"));
                }
                if(!Arrays.equals(ledgerBytes,Files.readAllBytes(sourceData.resolve("cursors.json")))
                        ||!Arrays.equals(selectionBytes,Files.readAllBytes(sourceData.resolve("sources.json")))
                        ||!Arrays.equals(configurationBytes,Files.readAllBytes(sourceData.resolve("configuration.json"))))
                    throw new IOException("Source metadata changed during preparation");
            } catch(java.nio.channels.OverlappingFileLockException busy) {
                throw new IOException("Source lineage has an active writer",busy);
            }
        }
        return List.copyOf(results);
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=3||!Set.of("--create","--repair-seek").contains(args[0]))
            throw new IllegalArgumentException("--create|--repair-seek PLAN NEW_REPORT");
        var plan=DataFiles.read(Path.of(args[1]),Plan.class);Path report=Path.of(args[2]);
        if(!plan.id().equals(WAVE)||Files.exists(report))throw new IOException("Explicit wave plan and new report required");
        Path library=Path.of(plan.library()).toAbsolutePath().normalize();
        String configured=TrainingSettings.preferences().get("baseTrainingRoot","");
        if(configured.isBlank()||!Path.of(configured).toAbsolutePath().normalize().equals(library))
            throw new IOException("Plan does not match the existing GUI library");
        if(args[0].equals("--repair-seek")) {
            // Repair must not create a replacement lineage if an expected owner root is absent.
            for(var source:plan.sources()) for(double rate:RATES) {
                var architecture=CheckpointInspection.manifest(Path.of(source.root()).resolve("checkpoints").resolve(source.checkpoint())).architecture();
                Path root=library.resolve(NetworkArchitecture.valueOf(architecture.name()).folderName()).resolve(id(source,rate).toString());
                if(!Files.isRegularFile(root.resolve(ORIGIN)))throw new IOException("Existing continuation required for repair: "+root);
            }
        }
        var entries=prepare(library,plan.sources(),args[0].equals("--repair-seek"));
        if(args[0].equals("--repair-seek")) for(var entry:entries) repairSeek(entry);
        DataFiles.write(report,new Report(WAVE,Instant.now().toString(),library.toString(),entries));
        System.out.println("Prepared/verified exact-state continuation arms; no training: "+report);
    }
    static List<String> repairSeek(Entry entry) throws IOException {
        Path root=Path.of(entry.root()), sourceRoot=Path.of(entry.origin().source().root());
        try(var sourceChannel=FileChannel.open(sourceRoot.resolve("store.lock"),StandardOpenOption.WRITE);
            var sourceLock=sourceChannel.tryLock();
            var destinationChannel=FileChannel.open(root.resolve("store.lock"),StandardOpenOption.WRITE);
            var destinationLock=destinationChannel.tryLock()) {
            if(sourceLock==null||destinationLock==null)throw new IOException("Active writer prevents seek repair");
            if(!DataFiles.read(root.resolve(ORIGIN),Origin.class).equals(entry.origin()))throw new IOException("Continuation origin changed");
            Path data=DataSources.directory(root), original=DataSources.directory(sourceRoot);
            var selection=DataSources.read(data);
            if(!hash(Files.readAllBytes(data.resolve("sources.json"))).equals(entry.origin().selectionSha256())
                    ||!hash(Files.readAllBytes(original.resolve("cursors.json"))).equals(entry.origin().ledgerSha256()))
                throw new IOException("Continuation source binding changed");
            var ledger=new SourceLedger(root);
            if(!ledger.state().next().equals(entry.sourceOffsets()))throw new IOException("Destination cursors changed during repair");
            return ContinuationSeek.inherit(selection,ledger.state().next(),original.resolve("seek"),data.resolve("seek"));
        } catch(java.nio.channels.OverlappingFileLockException busy) {
            throw new IOException("Active writer prevents seek repair",busy);
        }
    }
    private BrnContinuationWave() {}
}
