package com.ohinteractive.seedv6.corpus;

import java.nio.ByteBuffer;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.util.Fen;

/** Absolute Seed piece planes (a1=0), rule state, and an exact or unknown halfmove clock.
 * No network features, Zobrist key, fullmove numbering, or invented prior history is stored.
 */
public record CorpusPosition(long plane0, long plane1, long plane2, long plane3,
                             int rules, int halfmove) {
    public static final int BYTES = 40;
    public static final int UNKNOWN_HALFMOVE = -1;
    private static final int RULE_MASK = (1 << Board.HALF_MOVE_CLOCK_SHIFT) - 1;
    private static final java.util.regex.Pattern FIELD_SEPARATOR = java.util.regex.Pattern.compile("\\s+");

    public CorpusPosition {
        if ((rules & ~RULE_MASK) != 0 || halfmove < UNKNOWN_HALFMOVE)
            throw new IllegalArgumentException("Invalid corpus rule state");
        int ep = rules >>> Board.ESQUARE_SHIFT;
        if (ep != 0 && Board.enPassantSquare(rules) < 0)
            throw new IllegalArgumentException("Invalid corpus en-passant state");
        int whiteKings = 0, blackKings = 0;
        for (int s = 0; s < 64; s++) {
            int code = Board.getSquare(plane0, plane1, plane2, plane3, s);
            if (code == 1) whiteKings++;
            if (code == 9) blackKings++;
            if (code == 7 || code == 8 || code == 15)
                throw new IllegalArgumentException("Invalid corpus piece code");
        }
        if (whiteKings != 1 || blackKings != 1)
            throw new IllegalArgumentException("A corpus position requires one king per side");
    }

    /** FEN syntax/interpretation belongs to Seed. Four/five-field sources get parser-only defaults.
     * Fullmove is validated when supplied, then discarded. Halfmove is preserved without Board's
     * 127 saturation; toBoard applies that existing Seed policy only at reconstruction time.
     */
    public static CorpusPosition fromFen(String fen) {
        String[] fields = FIELD_SEPARATOR.split(fen.trim());
        if (fields.length < 4 || fields.length > 6) throw new IllegalArgumentException("Expected 4-6 FEN fields");
        String parserFen = String.join(" ", fields);
        if (fields.length == 4) parserFen += " 0 1";
        else if (fields.length == 5) parserFen += " 1";
        Fen.Parsed parsed = Fen.parse(parserFen);
        // Corpus records do not retain a Zobrist key or packed move counters.
        long p0=0,p1=0,p2=0,p3=0;
        for(int square=0;square<64;square++) {
            int piece=parsed.pieces()[square];long bit=1L<<square;
            p0|=-(piece&1)&bit;p1|=-(piece>>>1&1)&bit;
            p2|=-(piece>>>2&1)&bit;p3|=-(piece>>>3&1)&bit;
        }
        int rules=(parsed.whiteToMove()?0:1)|(parsed.castling()<<Board.CASTLING_SHIFT)
                |(Math.max(0,parsed.enPassant())<<Board.ESQUARE_SHIFT);
        return new CorpusPosition(p0,p1,p2,p3,rules,fields.length>=5?parsed.halfmove():UNKNOWN_HALFMOVE);
    }

    public boolean halfmoveKnown() { return halfmove >= 0; }

    /** An unknown rule-50 clock requires an explicit caller decision, never an implicit zero. */
    public long[] toBoard(int unknownHalfmove) {
        if (unknownHalfmove < 0) throw new IllegalArgumentException("Negative halfmove fallback");
        int clock = Math.min(halfmoveKnown() ? halfmove : unknownHalfmove, Board.MAX_HALF_MOVE_CLOCK);
        long[] board = {plane0, plane1, plane2, plane3,
                rules | ((long) clock << Board.HALF_MOVE_CLOCK_SHIFT) | (1L << Board.FULL_MOVE_NUMBER_SHIFT), 0};
        int[] pieces = new int[64];
        for (int square = 0; square < 64; square++) pieces[square] = Board.getSquare(plane0, plane1, plane2, plane3, square);
        board[Board.KEY] = com.ohinteractive.seedv6.core.util.Zobrist.getKey(pieces, Board.player(rules),
                rules >>> Board.CASTLING_SHIFT & Board.CASTLING_BITS, rules >>> Board.ESQUARE_SHIFT & Board.SQUARE_BITS);
        return board;
    }

    /** Source-like normalized FEN: unknown counters are omitted, fullmove is always excluded. */
    public String fen() {
        String[] fields = fenWithCounters(halfmoveKnown() ? halfmove : 0).split(" ");
        return String.join(" ", java.util.Arrays.copyOf(fields, 4))
                + (halfmoveKnown() ? " " + halfmove : "");
    }

    private String fenWithCounters(int clock) {
        long[] state = {plane0, plane1, plane2, plane3,
                rules | ((long) Math.min(clock, Board.MAX_HALF_MOVE_CLOCK) << Board.HALF_MOVE_CLOCK_SHIFT)
                        | (1L << Board.FULL_MOVE_NUMBER_SHIFT), 0};
        String[] fields = Fen.fromBoard(state).split(" ");
        fields[4] = Integer.toString(clock);
        return String.join(" ", fields);
    }

    /** Exact global identity, not a hash: same placement, STM, rights, EP, and known/unknown clock.
     * Fullmove and labels are excluded. Unknown and known-zero clocks are distinct identities.
     */
    public byte[] identity() {
        ByteBuffer b = ByteBuffer.allocate(BYTES);
        write(b);
        return b.array();
    }
    void write(ByteBuffer b) {
        b.putLong(plane0).putLong(plane1).putLong(plane2).putLong(plane3).putInt(rules).putInt(halfmove);
    }
    static CorpusPosition read(ByteBuffer b) {
        return new CorpusPosition(b.getLong(), b.getLong(), b.getLong(), b.getLong(), b.getInt(), b.getInt());
    }
}
