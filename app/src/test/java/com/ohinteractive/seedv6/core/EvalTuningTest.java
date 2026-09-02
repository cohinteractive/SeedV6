package com.ohinteractive.seedv6.core;

import com.ohinteractive.seedv6.core.util.Piece;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EvalTuningTest {

    @Test
    void retainedDataHasExactAuditedLengthsAndChecksums() {
        Eval.TuningSummary summary = Eval.tuningSummary();

        assertEquals(125, summary.materialEntries());
        assertEquals(19_200, summary.pieceSquareEntries());
        assertEquals(7_861, summary.criteriaEntries());
        assertEquals(64, summary.materialSha256().length());
        assertEquals(64, summary.pieceSquareSha256().length());
        assertEquals(64, summary.criteriaSha256().length());
    }

    @Test
    void corruptPartialAndUnexpectedDataFailExplicitly() throws IOException {
        assertThrows(IllegalStateException.class, () -> EvalTuning.decodeAndVerify(
                "bad-base64", "not base64", 1, "00"));

        byte[] data = {1, 2, 3};
        String encoded = gzip(data);
        String sha256 = sha256(data);
        assertThrows(IllegalStateException.class, () -> EvalTuning.decodeAndVerify(
                "partial", encoded, 4, sha256));
        assertThrows(IllegalStateException.class, () -> EvalTuning.decodeAndVerify(
                "checksum", encoded, 3, "0".repeat(64)));
    }

    @Test
    void correctedKnightPawnTableIsDistinctFromRookPawnTable() {
        EvalTuning tuning = EvalTuning.INSTANCE;

        assertEquals(-31, tuning.piecePawn(Piece.KNIGHT, 1, 1, 23));
        assertEquals(7, tuning.piecePawn(Piece.ROOK, 1, 1, 23));
        assertNotEquals(
                tuning.piecePawn(Piece.ROOK, 1, 1, 23),
                tuning.piecePawn(Piece.KNIGHT, 1, 1, 23));
    }

    @Test
    void exchangeValuesAreStableAndRejectUnknownTypes() {
        assertEquals(20_000, Eval.exchangeValue(Piece.KING));
        assertEquals(975, Eval.exchangeValue(Piece.QUEEN));
        assertEquals(500, Eval.exchangeValue(Piece.ROOK));
        assertEquals(330, Eval.exchangeValue(Piece.BISHOP));
        assertEquals(320, Eval.exchangeValue(Piece.KNIGHT));
        assertEquals(100, Eval.exchangeValue(Piece.PAWN));
        assertThrows(IllegalArgumentException.class, () -> Eval.exchangeValue(0));
        assertThrows(IllegalArgumentException.class, () -> Eval.exchangeValue(7));
    }

    private static String gzip(byte[] data) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream output = new GZIPOutputStream(bytes)) {
            output.write(data);
        }
        return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(
                    java.security.MessageDigest.getInstance("SHA-256").digest(data));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new AssertionError(exception);
        }
    }
}
