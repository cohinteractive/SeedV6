package com.ohinteractive.seedv6.core;

import com.ohinteractive.seedv6.core.util.Piece;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.zip.GZIPInputStream;

/**
 * Frozen tuning representation for pre-OPT5 baseline
 * 9981efef51dd5d7be3ea82d97a49add2653d4caf.
 * Keep its blobs, decoding, shapes, and indexing independent of production
 * layout changes.
 */
final class FrozenEvalTuning {

    static final int PHASE_COUNT = 25;
    static final int MAX_PHASE = PHASE_COUNT - 1;

    static final int ROOK_PROTECTS = 0;
    static final int KING_BLOCKS_ROOK = 1;
    static final int QUEEN_EARLY_DEVELOPMENT = 2;
    static final int ROOK_EARLY_DEVELOPMENT = 3;
    static final int ROOK_PAIR = 4;
    static final int ROOK_OPEN_FILE = 5;
    static final int ROOK_ON_QUEEN_FILE = 6;
    static final int BISHOP_PAIR = 7;
    static final int BISHOP_OUTPOST = 8;
    static final int KNIGHT_PAIR = 9;
    static final int KNIGHT_OUTPOST = 10;
    static final int DOUBLED_PAWN = 11;
    static final int WEAK_PAWN = 12;
    static final int ISOLATED_PAWN = 13;
    static final int PAWN_PROTECTS = 14;
    static final int PASSED_PAWN_PHALANX = 15;

    static final int SHIELD = 0;
    static final int STORM = 1;
    static final int CLOSE = 0;
    static final int FAR = 1;

    private static final int MATERIAL_RAW_LENGTH = 250;
    private static final int BONUS_RAW_LENGTH = 9_600;
    private static final int CRITERIA_RAW_LENGTH = 7_861;
    private static final String MATERIAL_SHA256 =
            "1d647057aa7d3b6953f45aa07de337536eb3239a0bba526b3a8aeb2c31e85373";
    private static final String BONUS_SHA256 =
            "71789a6d196416672e4dc50acab3236006ece49d124fb90fc065f0bf0cd17f9b";
    private static final String CRITERIA_SHA256 =
            "fd780b7b1598722f454aee543ebf02bb8900c1dcb81d3741f1824eba4c4bbbfe";

    private static final String MATERIAL_DATA =
            "H4sIAAAAAAAEAA3C2yoDcAAH4N/hXSTNzMzMjJm1FjMzM+Z45XycswdQSimllAullFJKuVBKKaWUC6/k3/f5z7/+8be//OkPv/vNr37xs5/86Aff+863vvG1r3zpC5/7TC1qVZsiiiqmuBJKKqW0Msoqp7wKKmpYJZVVUVU11dXgSFDiKMscYyUYZ5UTrHEyqHOK02xwJpjlHOe5wEUWmOcQcxxklgPsZ4Z9TLOXKfYwyW4m2MU4OxljB6NsZwRLwTJWsIo1rAcb2MQWtrET7KKJPezjIDjEEY5xgtN/5AtXcPoAAAA=";
    private static final String BONUS_DATA =
            "H4sIAAAAAAAEAO2a63LaSBCFBUJXhLgYEAJsnNra93+gTdaVjRPHYDDmahCX7Z4ZjFjnsKWEMuDKV+UfTEt9Ts+MZGk0vu/7Oc/LZrOu69iWZZpGRk+nNK1A5PN5X8XpCNdxbDrCIEzTtAjbcVyCTvY8z9c0LZXWMxSkgJv1cjk/XyiWLiowAFNBcR/Z5dPpz5HYNv/ZnDgr8TbkFD5LK92spzwVS+RqtRZ+DXJLEW6+KFeCWtiEAZgKikO7aUbXMxnVM7bMmvVYXHajselGP8/dGC0WHCNfBrVzM/uqBuFoMp1Fy7WW0g3LcalTyVAQNpotGICpoDi0u1gsV6q/9Ng5hdJ48jyLFkuWoRlhuywi9PtPw/H0OVqspC8vly+Vq7V6s3V3/9B7Gk1ni7Wm0wm5QqlSa1xe//EnDMBUUBzanU6fZ3M6ZaXq56EXAdIQ/UgilMy0nCznKlfD9kPvcTCakPxaSxsmqZOvIGy2Pn/5+r3T6w8pRMYy5ItsBfU9AZgKikO7g8FwNJ6I6FJGqQN49NsP3cf+YEg9M9/MCVvMidtv30m/PxhPaLJo1MOW6+WLF9UweR0wFRSHdikgIhSi8qlQOWgZs92hzhK5qF8iOcR8oXq33+7ane7jE6mrKUGJWIPs3rW7NINIXbiig/MlWeCPAzAVFId2ByJCoQlVyWVGkYy2O50u5xqI+udzMVtTdAaJ3/OEeBrw0M/V0NuOx3bvOxSg9rl0Zb8U+MMATAXFod13jx4nHeO9iO/k/TWNA6Y6IFYcMwYMHNUu5MzqOLN5ddS5e5oXzmlivQG/6zgAemKOahfyu47T4qh1wDd6et3fUt5SCSX1DQ1FMzkwFRTfs/phqNWXXaz/HJj7/wKTA1NBcWj3VctJPPtAktv1qXxPrHhlXdeVq0W8UvQmBb6FuIc4aqoD4r+azsoVDBzVLuTM6jizeeV5alnb3a4Kv9l1nlzc2OHk77vQ7gOi9xinv2U4esVYMIszjwEDMBUUh3Y/Izqxg7pber0dJ3GS9y5MBcWhXaoxVnlvC5L4CbvJgeLQ7niH2ABDDfhoUCzskH8BBpI/ZUC776WO9zKvbiVfJP9I+MLpIQ5YIEwFxaHdm5ubv5lPgo+Cvxh4Z7hDwJsMDMBUUBzaPfycOBcy4tVaLlOmBCqg/stF/L1ruVwR6/V6bwCmSk7EX6YkuxpohRUDF2VhIDnQ7qadG1UTkUptLAgyMcRDjVr2s/kzvIC3VFyU4u/b5fLLKzcMwFRQHNpdqY+EKbFLgD/iipQi1cu+gc3Oge0GCp+3FxSKYhcF26pUKlU/J1vF3gphvFzZH4CpoDi0q7Z4iP0Ncj1FicqdH2xb7YlgnbIUl3sk1O8gCGphvd5o+nL7S0mYqVaDWq0W7g/AVFAc2t35oY7nlRdsd/Ob3dQbjebl5eVVq3V9/YH6uCoOUs1XV7IdB2AqKA7t/vQN4sT4FwqDmOSAJQAA";
    private static final String CRITERIA_DATA =
            "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";

    static final FrozenEvalTuning INSTANCE = new FrozenEvalTuning();

    private final int[][] material = new int[Piece.PAWN + 1][PHASE_COUNT];
    private final int[][][][] bonus =
            new int[Piece.PAWN + 1][2][64][PHASE_COUNT];
    private final int[][] scalar = new int[16][PHASE_COUNT];
    private final int[][][][][] pawnCover = new int[2][2][2][4][PHASE_COUNT];
    private final int[][][] enemyDistance = new int[4][15][PHASE_COUNT];
    private final int[][][] protectorDistance = new int[2][15][PHASE_COUNT];
    private final int[][][] mobility = new int[Piece.PAWN + 1][][];
    private final int[][][][] piecePawn = new int[2][3][9][PHASE_COUNT];
    private final int[][][] badBishop = new int[9][9][PHASE_COUNT];
    private final int[][] safety = new int[Piece.PAWN + 1][9];

    private FrozenEvalTuning() {
        loadMaterial();
        loadBonus();
        loadCriteria();
        validateSemantics();
    }

    int material(int pieceType, int phase) {
        return material[pieceType][phase];
    }

    int bonus(int pieceType, int player, int square, int phase) {
        return bonus[pieceType][player][square][phase];
    }

    int scalar(int criterion, int phase) {
        return scalar[criterion][phase];
    }

    int pawnCover(int kind, int side, int distance, int pawnCount, int phase) {
        return pawnCover[kind][side][distance][pawnCount][phase];
    }

    int enemyDistance(int pieceType, int distance, int phase) {
        return enemyDistance[pieceType - Piece.QUEEN][distance][phase];
    }

    int protectorDistance(int pieceType, int distance, int phase) {
        return protectorDistance[pieceType == Piece.BISHOP ? 0 : 1][distance][phase];
    }

    int mobility(int pieceType, int count, int phase) {
        return mobility[pieceType][count][phase];
    }

    int piecePawn(int pieceType, int pieceCount, int pawnCount, int phase) {
        return piecePawn[pieceType == Piece.ROOK ? 0 : 1][pieceCount][pawnCount][phase];
    }

    int badBishop(int ownPawns, int enemyPawns, int phase) {
        return badBishop[ownPawns][enemyPawns][phase];
    }

    int safety(int pieceType, int attacks) {
        return safety[pieceType][attacks];
    }

    private void loadMaterial() {
        BlobReader reader = new BlobReader(decodeAndVerify(
                "material", MATERIAL_DATA, MATERIAL_RAW_LENGTH, MATERIAL_SHA256));
        for (int pieceType = Piece.QUEEN; pieceType <= Piece.PAWN; pieceType++) {
            for (int phase = 0; phase < PHASE_COUNT; phase++) {
                material[pieceType][phase] = reader.nextUnsignedShort();
            }
        }
        reader.finish("material");
    }

    private void loadBonus() {
        BlobReader reader = new BlobReader(decodeAndVerify(
                "piece-square", BONUS_DATA, BONUS_RAW_LENGTH, BONUS_SHA256));
        for (int pieceType = Piece.KING; pieceType <= Piece.PAWN; pieceType++) {
            for (int square = 0; square < 64; square++) {
                int blackSquare = square ^ 56;
                for (int phase = 0; phase < PHASE_COUNT; phase++) {
                    int value = reader.nextSignedByte();
                    requireRange("piece-square value", value, -64, 64);
                    bonus[pieceType][0][square][phase] = value;
                    bonus[pieceType][1][blackSquare][phase] = value;
                }
            }
        }
        reader.finish("piece-square");
    }

    private void loadCriteria() {
        BlobReader reader = new BlobReader(decodeAndVerify(
                "criteria", CRITERIA_DATA, CRITERIA_RAW_LENGTH, CRITERIA_SHA256));
        for (int criterion = 0; criterion < scalar.length; criterion++) {
            readPhases(reader, scalar[criterion]);
        }
        for (int kind = 0; kind < 2; kind++) {
            for (int side = 0; side < 2; side++) {
                for (int pawnCount = 0; pawnCount < 4; pawnCount++) {
                    for (int distance = 0; distance < 2; distance++) {
                        readPhases(reader, pawnCover[kind][side][distance][pawnCount]);
                    }
                }
            }
        }
        for (int table = 0; table < enemyDistance.length; table++) {
            readDistances(reader, enemyDistance[table]);
        }
        for (int table = 0; table < protectorDistance.length; table++) {
            readDistances(reader, protectorDistance[table]);
        }
        mobility[Piece.QUEEN] = readMobility(reader, 28);
        mobility[Piece.ROOK] = readMobility(reader, 14);
        mobility[Piece.BISHOP] = readMobility(reader, 14);
        mobility[Piece.KNIGHT] = readMobility(reader, 8);
        for (int table = 0; table < piecePawn.length; table++) {
            for (int pieceCount = 1; pieceCount <= 2; pieceCount++) {
                for (int pawnCount = 0; pawnCount <= 8; pawnCount++) {
                    readPhases(reader, piecePawn[table][pieceCount][pawnCount]);
                }
            }
        }
        for (int ownPawns = 0; ownPawns <= 8; ownPawns++) {
            for (int enemyPawns = 0; enemyPawns <= 8; enemyPawns++) {
                readPhases(reader, badBishop[ownPawns][enemyPawns]);
            }
        }
        for (int pieceType = Piece.QUEEN; pieceType <= Piece.KNIGHT; pieceType++) {
            for (int attacks = 0; attacks <= 8; attacks++) {
                safety[pieceType][attacks] = reader.nextSignedByte();
            }
        }
        reader.finish("criteria");
    }

    private static void readPhases(BlobReader reader, int[] target) {
        for (int phase = 0; phase < PHASE_COUNT; phase++) {
            int value = reader.nextSignedByte();
            requireRange("criterion value", value, -100, 120);
            target[phase] = value;
        }
    }

    private static void readDistances(BlobReader reader, int[][] target) {
        for (int distance = 1; distance <= 14; distance++) {
            readPhases(reader, target[distance]);
        }
    }

    private static int[][] readMobility(BlobReader reader, int maximum) {
        int[][] result = new int[maximum + 1][PHASE_COUNT];
        for (int count = 1; count <= maximum; count++) {
            readPhases(reader, result[count]);
        }
        return result;
    }

    private void validateSemantics() {
        for (int phase = 0; phase < PHASE_COUNT; phase++) {
            requireRange("queen material", material(Piece.QUEEN, phase), 800, 1_100);
            requireRange("rook material", material(Piece.ROOK, phase), 400, 700);
            requireRange("bishop material", material(Piece.BISHOP, phase), 250, 450);
            requireRange("knight material", material(Piece.KNIGHT, phase), 250, 450);
            requireRange("pawn material", material(Piece.PAWN, phase), 80, 150);
            if (material(Piece.QUEEN, phase) <= material(Piece.ROOK, phase)
                    || material(Piece.ROOK, phase) <= material(Piece.BISHOP, phase)
                    || material(Piece.ROOK, phase) <= material(Piece.KNIGHT, phase)
                    || material(Piece.BISHOP, phase) <= material(Piece.PAWN, phase)
                    || material(Piece.KNIGHT, phase) <= material(Piece.PAWN, phase)) {
                throw new IllegalStateException("Invalid material ordering at phase " + phase);
            }
        }
        for (int pieceType = Piece.QUEEN; pieceType <= Piece.KNIGHT; pieceType++) {
            for (int attacks = 0; attacks < safety[pieceType].length; attacks++) {
                requireRange("king-safety weight", safety[pieceType][attacks], 0, 120);
            }
        }
    }

    static byte[] decodeAndVerify(String name, String encoded, int expectedLength,
                                  String expectedSha256) {
        final byte[] compressed;
        try {
            compressed = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Invalid Base64 in " + name + " evaluation data", exception);
        }
        final byte[] decoded;
        try (GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            decoded = input.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("Invalid gzip stream in " + name + " evaluation data", exception);
        }
        if (decoded.length != expectedLength) {
            throw new IllegalStateException(name + " evaluation data length was " + decoded.length
                    + ", expected " + expectedLength);
        }
        String actualSha256;
        try {
            actualSha256 = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(decoded));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
        if (!actualSha256.equals(expectedSha256)) {
            throw new IllegalStateException(name + " evaluation data checksum was " + actualSha256
                    + ", expected " + expectedSha256);
        }
        return decoded;
    }

    private static void requireRange(String name, int value, int minimum, int maximum) {
        if (value < minimum || value > maximum) {
            throw new IllegalStateException(name + " " + value + " is outside ["
                    + minimum + ", " + maximum + "]");
        }
    }

    private static final class BlobReader {
        private final byte[] data;
        private int position;

        private BlobReader(byte[] data) {
            this.data = data;
        }

        private int nextSignedByte() {
            requireRemaining(1);
            return data[position++];
        }

        private int nextUnsignedShort() {
            requireRemaining(2);
            return (Byte.toUnsignedInt(data[position++]) << 8)
                    | Byte.toUnsignedInt(data[position++]);
        }

        private void requireRemaining(int count) {
            if (position + count > data.length) {
                throw new IllegalStateException("Partial evaluation data at byte " + position);
            }
        }

        private void finish(String name) {
            if (position != data.length) {
                throw new IllegalStateException(name + " evaluation data has "
                        + (data.length - position) + " trailing bytes");
            }
        }
    }
}

