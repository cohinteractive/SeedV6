package com.ohinteractive.seedv6.training.data;

import com.github.luben.zstd.Zstd;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.corpus.CorpusPosition;
import java.io.*;
import java.nio.*;
import java.nio.file.*;

/** Simple base-only synthetic writer; continuation truth comes from the independent C++ oracle. */
public final class BinpackFixtures {
    public static final String FEN = "4k3/8/8/8/3pP3/8/8/4K3 w - - 17 1";
    public static byte[] record(String fen, int score) throws Exception {
        var position = CorpusPosition.fromFen(fen); var board = position.toBoard(0);
        ByteBuffer data = ByteBuffer.allocate(34); long occupied = 0; byte[] pieces = new byte[16]; int n = 0;
        int rules = position.rules();
        for (int s = 0; s < 64; s++) {
            int code = Board.getSquare(board[0], board[1], board[2], board[3], s);
            if (code == 0) continue;
            occupied |= 1L << s; int colour = code >>> 3, type = code & 7;
            int nibble = (6 - type) * 2 + colour;
            if (type == 1 && colour == 1 && Board.player(rules) == 1) nibble = 15;
            if (type == 3 && (s % 8 == 0 && Board.queenSide(rules, colour) || s % 8 == 7 && Board.kingSide(rules, colour))) nibble = 13 + colour;
            int ep = Board.enPassantSquare(rules);
            if (type == 6 && ep >= 0 && s == ep + (colour == 0 ? 8 : -8)) nibble = 12;
            pieces[n / 2] |= (byte) (nibble << (n % 2 * 4)); n++;
        }
        data.putLong(occupied).put(pieces).putShort((short) 0); // unused terminal base move
        data.putShort((short) ((score << 1) ^ (score >> 31))).putShort((short) (2 << 14 | 37)).putShort((short) position.halfmove()).putShort((short) 0);
        return data.array();
    }
    public static byte[] chunk(int... scores) throws Exception {
        var payload = new ByteArrayOutputStream(); for (int score : scores) payload.write(record(FEN, score));
        return frame(payload.toByteArray());
    }
    public static byte[] frame(byte[] payload) throws Exception {
        var out = new ByteArrayOutputStream(); out.write(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putInt(0x504e4942).putInt(payload.length).array()); out.write(payload); return out.toByteArray();
    }
    public static Path archive(Path file, byte[]... chunks) throws Exception {
        var out = new ByteArrayOutputStream(); for (byte[] chunk : chunks) out.write(chunk);
        Files.write(file, Zstd.compress(out.toByteArray())); return file;
    }
    public static DataSource source(Path folder) throws Exception {
        Files.createDirectories(folder);
        archive(folder.resolve("a.binpack.zst"), chunk(32002, 100, 200, 300), chunk(400, 500, 600, 700));
        archive(folder.resolve("b.binpack.zst"), chunk(-100, -200, -300, -400), chunk(-500, -600, -700, -800));
        return DataSource.register("bt4-t80", folder, 1, LabelProfile.BT4_Q_V1);
    }
    private BinpackFixtures() {}
}
