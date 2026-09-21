package com.ohinteractive.seedv6.training.history;

import java.nio.file.*;
import java.time.Instant;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static com.ohinteractive.seedv6.training.history.HistoryFixtures.*;

class HistoryRepositoryTest {
    @TempDir Path root;
    @Test void emptyAppendReloadAndIdempotence() throws Exception {
        var repo=new HistoryRepository(root);
        assertTrue(repo.refresh().records().isEmpty()); assertFalse(Files.exists(root.resolve("history")));
        var a=record(1,true,4,Instant.now(),60_000_000_000L); var b=record(2,false,5,Instant.now(),null);
        assertTrue(repo.append(a)); assertFalse(repo.append(a)); assertTrue(repo.append(b));
        var loaded=new HistoryRepository(root).refresh(); assertEquals(java.util.List.of(a,b),loaded.records());
        assertTrue(loaded.warnings().isEmpty()); assertSame(repo.refresh(),repo.refresh());
        assertThrows(UnsupportedOperationException.class,()->loaded.records().clear());
        assertThrows(IOException.class,()->repo.append(record(1,false,4,Instant.now(),null)));
    }
    @Test void damagedMiddleAndUnterminatedTailRemainPreservedWhileLaterRecordsRecover() throws Exception {
        var repo=new HistoryRepository(root); var a=record(1,false,4,Instant.now(),null); repo.append(a);
        Files.writeString(repo.file(),"damaged\npartial",StandardOpenOption.APPEND);
        byte[] original=Files.readAllBytes(repo.file());
        var loaded=repo.refresh(); assertEquals(1,loaded.records().size()); assertEquals(2,loaded.warnings().size());
        var b=record(2,true,5,Instant.now(),null); repo.append(b);
        assertArrayEquals(original,java.util.Arrays.copyOf(Files.readAllBytes(repo.file()),original.length));
        loaded=new HistoryRepository(root).refresh(); assertEquals(java.util.List.of(a,b),loaded.records()); assertEquals(2,loaded.warnings().size());
    }
    @Test void completeButUnterminatedRecordIsNotAcknowledgedAndCanBeRetried() throws Exception {
        var repo=new HistoryRepository(root);var a=record(1,false,4,Instant.now(),null);
        Files.createDirectories(repo.file().getParent()); Files.writeString(repo.file(),HistoryCodec.encode(a));
        assertTrue(repo.refresh().records().isEmpty());repo.append(a);
        assertEquals(1,new HistoryRepository(root).refresh().records().size());
        assertFalse(new HistoryRepository(root).refresh().warnings().isEmpty());
    }
    @Test void schemaChecksumStructureAndDuplicateValidation() throws Exception {
        var a=record(1,true,4,Instant.now(),null);var repo=new HistoryRepository(root);repo.append(a);
        assertThrows(IllegalArgumentException.class,()->HistoryCodec.decode(HistoryCodec.encode(a).replace("schema=1","schema=2")));
        assertThrows(IllegalArgumentException.class,()->HistoryCodec.decode("schema=1\tgarbage"));
        String payload=HistoryCodec.encode(a).split("\tsha256=")[0];
        assertTrue(assertThrows(IllegalArgumentException.class,()->HistoryCodec.decode(signed(payload.replace("schema=1","schema=2")))).getMessage().contains("Unsupported"));
        assertThrows(RuntimeException.class,()->HistoryCodec.decode(signed(payload.replace("\tgeneration=1", ""))));
        assertThrows(IllegalArgumentException.class,()->HistoryCodec.decode(signed(payload.replace("wins=30", "wins=29"))));
        Files.writeString(repo.file(),HistoryCodec.encode(a)+"\n",StandardOpenOption.APPEND);
        assertEquals(1,repo.refresh().records().size());assertTrue(repo.refresh().warnings().getFirst().contains("Duplicate"));
        Files.writeString(repo.file(),"schema=2\tunknown\n",StandardOpenOption.APPEND);
        assertThrows(IOException.class,()->repo.append(record(2,false,4,Instant.now(),null)));
    }
    @Test void failureLeavesNoClaimOfPersistence() throws Exception {
        Files.writeString(root.resolve("history"),"obstruction");var repo=new HistoryRepository(root);
        assertThrows(IOException.class,()->repo.append(record(1,false,4,Instant.now(),null)));
        assertFalse(Files.exists(repo.file()));assertEquals("obstruction",Files.readString(root.resolve("history")));
    }
    @Test void absentOptionalMeasurementsRoundTripWithoutFabrication() throws Exception {
        var measured=record(1,false,4,Instant.now(),null);
        var sparse=new GenerationRecord(1,measured.candidate(),measured.incumbent(),measured.resultingBest(),
                measured.outcome(),measured.decision(),measured.wins(),measured.draws(),measured.losses(),32,0,
                measured.score(),measured.lowerBound(),measured.threshold(),measured.regime(),
                null,null,null,null,null,measured.completed(),null,null,null,null);
        var repo=new HistoryRepository(root);repo.append(sparse);
        assertEquals(sparse,new HistoryRepository(root).refresh().records().getFirst());
    }
    @Test void compatibleAdditionalUtf8FieldsAndCrLfAreReadable() throws Exception {
        var row=record(1,false,4,Instant.now(),null);var repo=new HistoryRepository(root);
        String payload=HistoryCodec.encode(row).split("\tsha256=")[0]+"\tfutureNote=mesuré";
        Files.createDirectories(repo.file().getParent());Files.writeString(repo.file(),signed(payload)+"\r\n");
        assertEquals(java.util.List.of(row),repo.refresh().records());assertTrue(repo.refresh().warnings().isEmpty());
    }
    private static String signed(String payload) throws Exception {
        return payload+"\tsha256="+java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
}
