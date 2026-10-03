package com.ohinteractive.seedv6.training.service;

import com.ohinteractive.seedv6.training.data.*;
import com.ohinteractive.seedv6.training.nnue.NnueCorpusTargets;
import com.ohinteractive.seedv6.corpus.CorpusRecord;
import com.ohinteractive.seedv6.core.Board;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;
import java.security.*;

/** Bounded research sampling from the production source reader; never generates chess positions. */
public final class BrnResearchData {
    static final long MAGIC = 0x533642524e443031L;
    public record Example(long[] board, int cp, int sfMaterial, int split) {
        public double outcome() { return NnueCorpusTargets.wdl(cp, sfMaterial).target(); }
    }
    public record Dataset(List<Example> training, List<Example> validation, List<Example> test) {}
    static final class Reservoir {
        final byte[][] records;final long[] ordinals;final SplittableRandom random;long observations;int size;
        Reservoir(int capacity,long seed){records=new byte[capacity][];ordinals=new long[capacity];random=new SplittableRandom(seed);}
        void add(byte[] bytes,long ordinal){
            long slot=++observations<=records.length?observations-1:random.nextLong(observations);
            if(slot<records.length){records[(int)slot]=bytes;ordinals[(int)slot]=ordinal;if(size<records.length)size++;}
        }
        Map<String,Object> report(){return Map.of("observations",observations,"retained",size,"minimumRawOrdinal",Arrays.stream(ordinals,0,size).min().orElse(-1),
                "maximumRawOrdinal",Arrays.stream(ordinals,0,size).max().orElse(-1));}
    }
    static String key(long[] board, int perspective, boolean mirror) {
        char[] key = new char[64];
        for (int square = 0; square < 64; square++) {
            int code = Board.getSquare(board[0], board[1], board[2], board[3], square);
            key[square ^ (perspective * 56) ^ (mirror ? 7 : 0)] = (char) (code == 0 ? 'A' : 'A' + (code ^ (perspective << 3)));
        }
        return new String(key);
    }
    static int split(long[] board) {
        String group = groupKey(board);
        byte[] hash = DataFiles.digest().digest(("seedv6-brn-v1-split:" + group).getBytes(StandardCharsets.US_ASCII));
        int bucket = (((hash[0] & 255) << 8) | (hash[1] & 255)) % 10;
        return bucket == 0 ? 2 : bucket == 1 ? 1 : 0;
    }
    static String groupKey(long[] board) {
        String group = key(board, 0, false);
        for (int p = 0; p < 2; p++) for (boolean m : new boolean[]{false, true}) {
            String next = key(board, p, m); if (next.compareTo(group) < 0) group = next;
        }
        return group;
    }
    public static void prepare(Path sourcePath, Path directory, int trainingCount, int heldCount) throws Exception {
        prepare(sourcePath,directory,trainingCount,heldCount,0,null);
    }
    public static void prepare(Path sourcePath, Path directory, int trainingCount, int heldCount, long rawStart, Path existingNavigation) throws Exception {
        prepare(sourcePath,directory,trainingCount,heldCount,rawStart,existingNavigation,false);
    }
    public static void prepare(Path sourcePath, Path directory, int trainingCount, int heldCount, long rawStart, Path existingNavigation, boolean uniformHeldOut) throws Exception {
        if(trainingCount<2||heldCount<2)throw new IllegalArgumentException("Dataset counts");
        if(rawStart<0 || rawStart>0 && existingNavigation==null)throw new IllegalArgumentException("A nonzero bounded source start needs existing navigation evidence");
        if (Files.exists(directory)) throw new IOException("Use a new isolated data directory");
        Files.createDirectories(directory);
        DataSource source = DataSource.register("BRN programme CP common population", sourcePath, 1);
        if(rawStart>0){Files.createDirectories(directory.resolve("seek"));String name=source.identity()+".json";
            Files.copy(existingNavigation.resolve(name),directory.resolve("seek").resolve(name));}
        int[] counts = new int[3], limits = {trainingCount, heldCount, heldCount};
        long examined = 0, unsupported = 0, duplicates = 0, disagreements = 0, excess = 0;
        long duplicateAbsCpDifference = 0; int maximumDuplicateCpDifference = 0;
        Map<String,Integer> seen = new HashMap<>();
        var validation=new Reservoir(heldCount,73103);var test=new Reservoir(heldCount,73104);
        Reservoir[] reservoirs={null,validation,test};
        long seekRecords;
        MessageDigest[] hashes = {DataFiles.digest(), DataFiles.digest(), DataFiles.digest()};
        Path payload = directory.resolve("positions.bin");
        try (var reader = SourceReaders.open(source, directory.resolve("seek"), rawStart);
             var out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(payload, StandardOpenOption.CREATE_NEW)))) {
            out.writeLong(MAGIC); out.writeInt(trainingCount); out.writeInt(heldCount);
            while (counts[0] < limits[0] || counts[1] < limits[1] || counts[2] < limits[2]) {
                if (examined >= 4_000_000) throw new IOException("Bounded sample admission limit");
                var entry = reader.next(); if (entry == null) throw new EOFException("Source exhausted before sample budgets"); examined++;
                if(entry.ordinal()!=rawStart+examined-1)throw new IOException("Unexpected source ordinal");
                var record = entry.position();
                if (record == null || CorpusTraining.rejection(record) != null) { unsupported++; continue; }
                long[] board = record.position().toBoard(0);
                int perspective = Board.player((int) board[Board.STATUS]);
                int cp = record.target(); if (record.perspective() == CorpusRecord.WHITE && perspective == 1) cp = -cp;
                Integer priorCp = seen.putIfAbsent(key(board, perspective, false), cp);
                if (priorCp != null) {
                    duplicates++; int difference=Math.abs(cp-priorCp);
                    if(difference!=0)disagreements++;
                    duplicateAbsCpDifference+=difference;maximumDuplicateCpDifference=Math.max(maximumDuplicateCpDifference,difference);continue;
                }
                int partition = split(board);
                if (counts[partition] == limits[partition]) { excess++; if(!uniformHeldOut||partition==0)continue; }
                int material = NnueCorpusTargets.material(board);
                var buffer = new ByteArrayOutputStream(); var recordOut = new DataOutputStream(buffer);
                for (long word : board) recordOut.writeLong(word);
                recordOut.writeInt(cp); recordOut.writeInt(material); recordOut.writeByte(partition); recordOut.flush();
                byte[] bytes = buffer.toByteArray();
                if(uniformHeldOut&&partition!=0){reservoirs[partition].add(bytes,entry.ordinal());counts[partition]=reservoirs[partition].size;}
                else {out.write(bytes);hashes[partition].update(bytes);counts[partition]++;}
            }
            if(uniformHeldOut)for(int partition=1;partition<3;partition++)for(byte[] bytes:reservoirs[partition].records){out.write(bytes);hashes[partition].update(bytes);}
            seekRecords=reader.seekRecords();
            if(reader.nextPosition()!=rawStart+examined)throw new IOException("Unexpected source endpoint");
        }
        source.verify();
        var report = new LinkedHashMap<String,Object>();
        report.put("schema", uniformHeldOut?"seedv6-brn-research-data-v2":"seedv6-brn-research-data-v1"); report.put("source", source); report.put("examined", examined);
        report.put("rawStart",rawStart);report.put("rawEndExclusive",rawStart+examined);
        report.put("seekRecords",seekRecords);
        report.put("heldOutSampling",uniformHeldOut?"Algorithm R across the entire examined range; independent fixed seeds73103/73104; no label-based admission":"First eligible observations until each partition quota fills");
        if(uniformHeldOut){report.put("validationReservoir",validation.report());report.put("testReservoir",test.report());}
        report.put("unsupported", unsupported); report.put("duplicates", duplicates); report.put("excessPartition", excess);
        report.put("duplicateLabelDisagreements",disagreements);report.put("duplicateMeanAbsCpDifference",duplicates==0?0:duplicateAbsCpDifference/(double)duplicates);
        report.put("duplicateMaximumAbsCpDifference",maximumDuplicateCpDifference);report.put("duplicatePolicy","First allowed-input observation retained; disagreements measured, no label averaging");
        report.put("counts", counts); report.put("split", "SHA256 geometry group, color/rank/STM/file variants together; buckets 0=test,1=validation,2..9=train");
        report.put("trainingSha256", HexFormat.of().formatHex(hashes[0].digest()));
        report.put("validationSha256", HexFormat.of().formatHex(hashes[1].digest()));
        report.put("testSha256", HexFormat.of().formatHex(hashes[2].digest()));
        try (var input = new DigestInputStream(Files.newInputStream(payload), DataFiles.digest())) {
            input.transferTo(OutputStream.nullOutputStream()); report.put("payloadSha256", HexFormat.of().formatHex(input.getMessageDigest().digest()));
        }
        report.put("secondsNote", "See command elapsed time; no optimization in data preparation");
        DataFiles.write(directory.resolve("manifest.json"), report);
        System.out.println(DataFiles.JSON.toJson(report));
    }
    public static Dataset read(Path directory, boolean unsealTest) throws Exception {
        @SuppressWarnings("unchecked") var manifest = DataFiles.read(directory.resolve("manifest.json"), Map.class);
        var partitions = List.of(new ArrayList<Example>(), new ArrayList<Example>(), new ArrayList<Example>());
        var digest = DataFiles.digest();
        try (var in = new DataInputStream(new BufferedInputStream(new DigestInputStream(Files.newInputStream(directory.resolve("positions.bin")), digest)))) {
            if (in.readLong() != MAGIC) throw new IOException("Wrong dataset format");
            int training = in.readInt(), held = in.readInt(), total = Math.addExact(training, Math.multiplyExact(2, held));
            for (int i = 0; i < total; i++) {
                long[] board = new long[6]; for (int j = 0; j < 6; j++) board[j] = in.readLong();
                var item = new Example(board, in.readInt(), in.readInt(), in.readUnsignedByte());
                if (item.split() > 2 || split(board) != item.split()) throw new IOException("Invalid partition");
                if (item.split() != 2 || unsealTest) partitions.get(item.split()).add(item);
            }
            if (in.read() != -1) throw new IOException("Trailing data");
            if (partitions.get(0).size() != training || partitions.get(1).size() != held || unsealTest && partitions.get(2).size() != held)
                throw new IOException("Wrong dataset counts");
        }
        if (!HexFormat.of().formatHex(digest.digest()).equals(manifest.get("payloadSha256"))) throw new IOException("Dataset hash changed");
        return new Dataset(partitions.get(0), partitions.get(1), partitions.get(2));
    }
    private BrnResearchData() {}
}
