package com.ohinteractive.seedv6.training.data;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.corpus.CorpusPosition;
import com.ohinteractive.seedv6.corpus.CorpusRecord;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

/** Stockfish BINP chunk decoder. See binpack-NOTICE.txt for upstream format provenance.
 * The internal board is the wire format's piece ordering, not a new position serialization.
 */
public final class BinpackDecoder {
    public static final int MAX_CHUNK = 100 * 1024 * 1024;
    public static final int ENCODED_SCORE = 3;
    public record Move(int from, int to, int type, int promotion) {}
    public record Record(CorpusPosition position, int target, int ply, int result, Move move) implements TrainingPosition {
        public int targetKind() { return ENCODED_SCORE; }
        public int perspective() { return CorpusRecord.SIDE_TO_MOVE; }
    }
    private final byte[] data;
    private final int[] board = new int[64]; // -1 empty; pawn/knight/bishop/rook/queen/king * 2 + colour
    private int offset, bit, remaining, stm, rights, ep, clock, ply, result, score;
    private Move move;

    public BinpackDecoder(byte[] chunk) throws IOException {
        if (chunk.length < 34 || chunk.length > MAX_CHUNK) throw invalid("chunk size");
        data = chunk;
    }
    public static byte[] readChunk(InputStream input) throws IOException {
        int first = input.read();
        if (first == -1) return null;
        byte[] tail = input.readNBytes(7);
        if (tail.length != 7 || first != 'B' || tail[0] != 'I' || tail[1] != 'N' || tail[2] != 'P')
            throw invalid("missing/truncated BINP header");
        long size = (tail[3] & 255L) | (tail[4] & 255L) << 8 | (tail[5] & 255L) << 16 | (tail[6] & 255L) << 24;
        if (size < 34 || size > MAX_CHUNK) throw invalid("chunk size " + size);
        byte[] payload = input.readNBytes((int) size);
        if (payload.length != size) throw invalid("truncated chunk");
        return payload;
    }
    public Record next() throws IOException {
        if (remaining == 0) {
            if (offset == data.length) return null;
            if (data.length - offset < 34) throw invalid("truncated base entry");
            base();
        } else {
            replay();
            move = continuationMove();
            // The upstream wire arithmetic is signed 16-bit, including score deltas.
            score = (short) (-score + signed(vle()));
            ply++; result = -result;
            if (--remaining == 0) offset = (bit + 7) / 8;
        }
        try { return new Record(position(), score, ply, result, move); }
        catch (IllegalArgumentException e) { throw new IOException("Invalid BINP position", e); }
    }
    private void base() throws IOException {
        Arrays.fill(board, -1); stm = rights = 0; ep = -1;
        long occupied = ByteBuffer.wrap(data, offset, 8).getLong();
        if (Long.bitCount(occupied) > 32) throw invalid("more than 32 pieces");
        int i = 0;
        while (occupied != 0) {
            int square = Long.numberOfTrailingZeros(occupied); occupied &= occupied - 1;
            int piece = (data[offset + 8 + i / 2] >>> (i % 2 * 4)) & 15; i++;
            if (piece == 12) {
                int rank = square / 8;
                if (ep != -1 || rank != 3 && rank != 4) throw invalid("en passant marker");
                piece = rank == 3 ? 0 : 1; ep = square + (rank == 3 ? -8 : 8);
            } else if (piece == 13 || piece == 14) {
                int colour = piece - 13;
                if (square != colour * 56 && square != colour * 56 + 7) throw invalid("castling rook marker");
                rights |= (square % 8 == 0 ? 2 : 1) << (colour * 2); piece = 6 + colour;
            } else if (piece == 15) { piece = 11; stm = 1; }
            board[square] = piece;
        }
        int packedMove = u16(offset + 24);
        move = new Move(packedMove >>> 8 & 63, packedMove >>> 2 & 63, packedMove >>> 14, packedMove & 3);
        score = signed(u16(offset + 26));
        int pr = u16(offset + 28); ply = pr & 16383; result = signed(pr >>> 14);
        if (result < -1 || result > 1) throw invalid("result");
        clock = u16(offset + 30); remaining = u16(offset + 32); offset += 34; bit = offset * 8;
        if (ep != -1 && ep / 8 != (stm == 0 ? 5 : 2)) throw invalid("en passant side to move");
        if ((rights & 3) != 0 && board[4] != 10 || (rights & 12) != 0 && board[60] != 11)
            throw invalid("castling king");
    }
    private int u16(int at) { return (data[at] & 255) << 8 | data[at + 1] & 255; }
    private static int signed(int value) { return (value >>> 1) ^ -(value & 1); }
    private int bits(int count) throws IOException {
        if ((long) bit + count > (long) data.length * 8) throw invalid("truncated continuation");
        int value = 0;
        for (int i = 0; i < count; i++, bit++) value = value << 1 | (data[bit / 8] >>> (7 - bit % 8) & 1);
        return value;
    }
    private int vle() throws IOException {
        int value = 0;
        for (int shift = 0; shift < 16; shift += 4) {
            int block = bits(5); value |= (block & 15) << shift;
            if ((block & 16) == 0) return value;
        }
        throw invalid("score delta overflow");
    }
    private int choice(int count) throws IOException {
        if (count < 1) throw invalid("empty move choices");
        int value = bits(32 - Integer.numberOfLeadingZeros(count - 1));
        if (value >= count) throw invalid("move choice outside range");
        return value;
    }
    private static int nth(long squares, int index) {
        while (index-- > 0) squares &= squares - 1;
        return Long.numberOfTrailingZeros(squares);
    }
    private Move continuationMove() throws IOException {
        long ours = 0;
        for (int s = 0; s < 64; s++) if (board[s] >= 0 && (board[s] & 1) == stm) ours |= 1L << s;
        int from = nth(ours, choice(Long.bitCount(ours))), type = board[from] / 2;
        long destinations = destinations(from, true);
        int count = Long.bitCount(destinations);
        if (type == 0 && from / 8 == (stm == 0 ? 6 : 1)) {
            int id = choice(count * 4);
            return new Move(from, nth(destinations, id / 4), 1, id % 4);
        }
        int castles = type == 5 ? Integer.bitCount(rights >>> (stm * 2) & 3) : 0;
        int id = choice(count + castles);
        if (id >= count) {
            boolean queenSide = id == count && (rights & (2 << (stm * 2))) != 0;
            return new Move(from, stm * 56 + (queenSide ? 0 : 7), 2, 0);
        }
        int to = nth(destinations, id);
        return new Move(from, to, type == 0 && to == ep ? 3 : 0, 0);
    }
    /** Pseudo-legal destinations in ascending square order, as specified by BINP. */
    private long destinations(int from, boolean pawnPushes) {
        int piece = board[from], colour = piece & 1, type = piece / 2;
        int file = from % 8, rank = from / 8;
        long squares = 0;
        if (type == 0) {
            int direction = colour == 0 ? 1 : -1, r = rank + direction;
            if (r < 0 || r > 7) return 0;
            for (int df : new int[]{-1, 1}) if (file + df >= 0 && file + df < 8) {
                int to = r * 8 + file + df;
                if (!pawnPushes || to == ep || board[to] >= 0 && (board[to] & 1) != colour) squares |= 1L << to;
            }
            int to = r * 8 + file;
            if (pawnPushes && board[to] == -1) {
                squares |= 1L << to;
                if (rank == (colour == 0 ? 1 : 6) && board[to + direction * 8] == -1) squares |= 1L << (to + direction * 8);
            }
        } else if (type == 1 || type == 5) {
            for (int df = -2; df <= 2; df++) for (int dr = -2; dr <= 2; dr++) {
                if (type == 1 ? Math.abs(df * dr) != 2 : Math.max(Math.abs(df), Math.abs(dr)) != 1) continue;
                int f = file + df, r = rank + dr;
                if (f >= 0 && f < 8 && r >= 0 && r < 8) {
                    int to = r * 8 + f;
                    if (board[to] < 0 || (board[to] & 1) != colour) squares |= 1L << to;
                }
            }
        } else {
            for (int df = -1; df <= 1; df++) for (int dr = -1; dr <= 1; dr++) {
                if (df == 0 && dr == 0 || type == 2 && df * dr == 0 || type == 3 && df * dr != 0) continue;
                for (int f = file + df, r = rank + dr; f >= 0 && f < 8 && r >= 0 && r < 8; f += df, r += dr) {
                    int to = r * 8 + f;
                    if (board[to] < 0 || (board[to] & 1) != colour) squares |= 1L << to;
                    if (board[to] >= 0) break;
                }
            }
        }
        return squares;
    }
    private void replay() throws IOException {
        int from = move.from(), to = move.to(), piece = board[from];
        if (from == to || piece < 0 || (piece & 1) != stm) throw invalid("continuation base move");
        if (move.type() == 2) {
            int home = stm * 56, castle = to == home ? 2 : to == home + 7 ? 1 : 0;
            if (piece / 2 != 5 || from != home + 4 || castle == 0 || (rights & castle << (stm * 2)) == 0 || board[to] != 6 + stm)
                throw invalid("castling move");
            for (int s = Math.min(from, to) + 1; s < Math.max(from, to); s++) if (board[s] != -1) throw invalid("blocked castling");
            board[from] = board[to] = -1;
            board[home + (castle == 2 ? 2 : 6)] = piece;
            board[home + (castle == 2 ? 3 : 5)] = 6 + stm; clock++;
        } else {
            if ((destinations(from, true) & 1L << to) == 0) throw invalid("impossible continuation move");
            boolean promotion = piece / 2 == 0 && to / 8 == (stm == 0 ? 7 : 0);
            if (promotion != (move.type() == 1)) throw invalid("promotion move");
            boolean enPassant = piece / 2 == 0 && to == ep;
            if (enPassant != (move.type() == 3)) throw invalid("en passant move");
            clock = piece / 2 == 0 || board[to] >= 0 ? 0 : clock + 1;
            if (enPassant) {
                int captured = to + (stm == 0 ? -8 : 8);
                if (board[captured] != (stm ^ 1)) throw invalid("en passant victim");
                board[captured] = -1;
            }
            board[from] = -1; board[to] = promotion ? (move.promotion() + 1) * 2 + stm : piece;
        }
        for (int s : new int[]{from, to}) rights &= switch (s) {
            case 0 -> ~2; case 7 -> ~1; case 4 -> ~3; case 56 -> ~8; case 63 -> ~4; case 60 -> ~12; default -> ~0;
        };
        ep = piece / 2 == 0 && (from ^ to) == 16 ? (from + to) / 2 : -1;
        stm ^= 1;
        if (ep >= 0 && !possibleEp()) ep = -1;
    }
    // Upstream retains a double-push EP square only if at least one capture exposes no slider check.
    private boolean possibleEp() {
        int victim = ep + (stm == 0 ? -8 : 8), king = -1;
        for (int s = 0; s < 64; s++) if (board[s] == 10 + stm) king = s;
        for (int from : new int[]{victim - 1, victim + 1}) {
            if (from < 0 || from >= 64 || from / 8 != victim / 8 || board[from] != stm) continue;
            int captured = board[victim]; board[from] = board[victim] = -1; board[ep] = stm;
            boolean attacked = false;
            for (int s = 0; s < 64; s++) if (board[s] >= 0 && (board[s] & 1) != stm && board[s] / 2 >= 2 && board[s] / 2 <= 4)
                if ((destinations(s, false) & 1L << king) != 0) { attacked = true; break; }
            board[from] = stm; board[victim] = captured; board[ep] = -1;
            if (!attacked) return true;
        }
        return false;
    }
    private CorpusPosition position() {
        long p0 = 0, p1 = 0, p2 = 0, p3 = 0;
        for (int s = 0; s < 64; s++) if (board[s] >= 0) {
            int code = 6 - board[s] / 2 + (board[s] % 2) * 8; long b = 1L << s;
            p0 |= -(code & 1) & b; p1 |= -(code >>> 1 & 1) & b; p2 |= -(code >>> 2 & 1) & b; p3 |= -(code >>> 3 & 1) & b;
        }
        return new CorpusPosition(p0, p1, p2, p3, stm | rights << Board.CASTLING_SHIFT | Math.max(0, ep) << Board.ESQUARE_SHIFT, clock);
    }
    private static IOException invalid(String message) { return new IOException("Invalid Stockfish BINP: " + message); }
}
