package com.ohinteractive.seedv6.core;

import com.ohinteractive.seedv6.core.util.Bitboard;
import com.ohinteractive.seedv6.core.util.Pext;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.core.util.Value;

/**
 * Deterministic, phase-aware static evaluation for valid V6 positions.
 *
 * <p>The returned score is always relative to the side to move: positive favours
 * that side and negative favours its opponent. Evaluation reads only piece
 * placement, side to move, and castling rights. It never adjudicates repetition,
 * the 50-move rule, dead/insufficient material, or any other history-dependent
 * rule, and it does not cache results. This makes the score suitable for a later
 * alpha-beta leaf or quiescence stand-pat contract.</p>
 */
public final class Eval {

    public static final int MAX_PHASE = EvalTuning.MAX_PHASE;
    public static final int MAX_STATIC_SCORE = 30_000;

    private static final EvalTuning TUNING = EvalTuning.INSTANCE;
    private static final int WHITE_BACK_RANK = 0;
    private static final int BLACK_BACK_RANK = 7;
    private static final int CENTRE_FILE = 3;
    private static final int CENTRE_RANK = 3;
    private static final int EDGE_WEIGHT = 10;
    private static final int PROXIMITY_WEIGHT = 20;
    private static final int EVAL_SHIFT = 32;

    private static final int[] EXCHANGE_VALUE = {
        0, 20_000, 975, 500, 330, 320, 100
    };

    private static final int[] SAFETY_VALUE = {
        0, 0, 1, 2, 3, 5, 7, 9, 12, 15,
        18, 22, 26, 30, 35, 39, 44, 50, 56, 62,
        68, 75, 82, 85, 89, 97, 105, 113, 122, 131,
        140, 150, 169, 180, 191, 202, 213, 225, 237, 248,
        260, 272, 283, 295, 307, 319, 330, 342, 354, 366,
        377, 389, 401, 412, 424, 436, 448, 459, 471, 483,
        494, 500, 500, 500, 500, 500, 500, 500, 500, 500,
        500, 500, 500, 500, 500, 500, 500, 500, 500, 500,
        500, 500, 500, 500, 500, 500, 500, 500, 500, 500,
        500, 500, 500, 500, 500, 500, 500, 500, 500, 500
    };

    private static final long LIGHT_SQUARES =
            Bitboard.BB[Bitboard.SQUARE_COLOR_LIGHT][0];
    private static final long[] FILE = Bitboard.BB[Bitboard.FILE];
    private static final long[] RANK = Bitboard.BB[Bitboard.RANK];
    private static final long[][] PAWN_ATTACKS = {
        Bitboard.BB[Bitboard.PAWN_ATTACKS_PLAYER0],
        Bitboard.BB[Bitboard.PAWN_ATTACKS_PLAYER1]
    };
    private static final long[][] FORWARD_RANKS = {
        Bitboard.BB[Bitboard.FORWARD_RANKS_PLAYER0],
        Bitboard.BB[Bitboard.FORWARD_RANKS_PLAYER1]
    };
    private static final long[][] PASSED_PAWN_FILES = {
        Bitboard.BB[Bitboard.PASSED_PAWNS_FILES_PLAYER0],
        Bitboard.BB[Bitboard.PASSED_PAWNS_FILES_PLAYER1]
    };
    private static final long[][] KING_RING = {
        Bitboard.BB[Bitboard.KING_RING_PLAYER0],
        Bitboard.BB[Bitboard.KING_RING_PLAYER1]
    };
    private static final long[] ADJACENT_FILE_MASK = new long[64];
    private static final long[] PHALANX_MASK = new long[64];
    private static final long[] WEAK_PAWN_SUPPORT_MASK = new long[2 * 64];
    private static final long[] PASSED_PAWN_MASK = new long[2 * 64];
    private static final long[] PROMOTION_PATH_MASK = new long[2 * 64];

    static {
        for (int square = 0; square < 64; square++) {
            int file = square & Value.FILE;
            int rank = square >>> 3;
            long adjacentFiles = (file > 0 ? FILE[file - 1] : 0L)
                    | (file < 7 ? FILE[file + 1] : 0L);
            ADJACENT_FILE_MASK[square] = adjacentFiles;
            PHALANX_MASK[square] = adjacentFiles & RANK[rank];
            for (int player = Value.WHITE; player <= Value.BLACK; player++) {
                int index = (player << 6) | square;
                WEAK_PAWN_SUPPORT_MASK[index] = adjacentFiles
                        & (FORWARD_RANKS[1 ^ player][rank] | RANK[rank]);
                PASSED_PAWN_MASK[index] = (FILE[file] | adjacentFiles)
                        & FORWARD_RANKS[player][rank];
                PROMOTION_PATH_MASK[index] = FORWARD_RANKS[player][rank] & FILE[file];
            }
        }
    }

    /** Evaluate a complete V6 board without modifying it. */
    public static int evaluate(long[] board) {
        requireBoardArray(board);
        return calculate(board[0], board[1], board[2], board[3],
                Math.toIntExact(board[Board.STATUS]), null);
    }

    /**
     * Search-facing compatibility entry point. The key is intentionally ignored:
     * WS5 owns no evaluation cache.
     */
    public static int eval(long board0, long board1, long board2, long board3,
                           int status, long key) {
        return calculate(board0, board1, board2, board3, status, null);
    }

    /**
     * Produce an immutable breakdown through exactly the production calculation.
     * Its total is therefore mechanically identical to {@link #evaluate(long[])}.
     */
    public static Breakdown breakdown(long[] board) {
        requireBoardArray(board);
        Collector collector = new Collector();
        int total = calculate(board[0], board[1], board[2], board[3],
                Math.toIntExact(board[Board.STATUS]), collector);
        return collector.toBreakdown(total);
    }

    /** Stable, phase-independent values intended for later exchange accounting. */
    public static int exchangeValue(int pieceType) {
        if (pieceType < Piece.KING || pieceType > Piece.PAWN) {
            throw new IllegalArgumentException("Unknown piece type: " + pieceType);
        }
        return EXCHANGE_VALUE[pieceType];
    }

    /** Force initialization and expose the retained immutable-data contract. */
    public static TuningSummary tuningSummary() {
        return new TuningSummary(
                125,
                19_200,
                7_861,
                "1d647057aa7d3b6953f45aa07de337536eb3239a0bba526b3a8aeb2c31e85373",
                "71789a6d196416672e4dc50acab3236006ece49d124fb90fc065f0bf0cd17f9b",
                "fd780b7b1598722f454aee543ebf02bb8900c1dcb81d3741f1824eba4c4bbbfe");
    }

    /** Opening is 0; a pawn/king-only position is 24. Promoted material clamps at 0. */
    static int phase(int queens, int rooks, int bishops, int knights) {
        long remaining = 4L * queens + 2L * rooks + bishops + knights;
        return (int) Math.max(0L, Math.min(MAX_PHASE, MAX_PHASE - remaining));
    }

    private static int calculate(long board0, long board1, long board2, long board3,
                                 int status, Collector collector) {
        long allOccupancy = board0 | board1 | board2;
        long whiteMask = ~board3;
        long blackMask = board3;
        long kings = board0 & ~board1 & ~board2;
        long queens = ~board0 & board1 & ~board2;
        long rooks = board0 & board1 & ~board2;
        long bishops = ~board0 & ~board1 & board2;
        long knights = board0 & ~board1 & board2;
        long pawns = ~board0 & board1 & board2;

        long whiteKing = kings & whiteMask;
        long blackKing = kings & blackMask;
        requireSingleKing("white", whiteKing);
        requireSingleKing("black", blackKing);

        long whiteQueen = queens & whiteMask;
        long whiteRook = rooks & whiteMask;
        long whiteBishop = bishops & whiteMask;
        long whiteKnight = knights & whiteMask;
        long whitePawn = pawns & whiteMask;
        long blackQueen = queens & blackMask;
        long blackRook = rooks & blackMask;
        long blackBishop = bishops & blackMask;
        long blackKnight = knights & blackMask;
        long blackPawn = pawns & blackMask;

        int whiteQueenCount = Long.bitCount(whiteQueen);
        int whiteRookCount = Long.bitCount(whiteRook);
        int whiteBishopCount = Long.bitCount(whiteBishop);
        int whiteKnightCount = Long.bitCount(whiteKnight);
        int whitePawnCount = Long.bitCount(whitePawn);
        int blackQueenCount = Long.bitCount(blackQueen);
        int blackRookCount = Long.bitCount(blackRook);
        int blackBishopCount = Long.bitCount(blackBishop);
        int blackKnightCount = Long.bitCount(blackKnight);
        int blackPawnCount = Long.bitCount(blackPawn);
        int phase = phase(
                whiteQueenCount + blackQueenCount,
                whiteRookCount + blackRookCount,
                whiteBishopCount + blackBishopCount,
                whiteKnightCount + blackKnightCount);
        if (collector != null) {
            collector.phase = phase;
            collector.sideToMove = status & Board.PLAYER_BIT;
        }

        int whiteKingSquare = Long.numberOfTrailingZeros(whiteKing);
        int blackKingSquare = Long.numberOfTrailingZeros(blackKing);
        int whiteKingRank = whiteKingSquare >>> 3;
        int whiteKingFile = whiteKingSquare & Value.FILE;
        int blackKingRank = blackKingSquare >>> 3;
        int blackKingFile = blackKingSquare & Value.FILE;
        long whiteOccupancy = allOccupancy & whiteMask;
        long blackOccupancy = allOccupancy & blackMask;

        int queenMaterial = TUNING.material(Piece.QUEEN, phase);
        int rookMaterial = TUNING.material(Piece.ROOK, phase);
        int bishopMaterial = TUNING.material(Piece.BISHOP, phase);
        int knightMaterial = TUNING.material(Piece.KNIGHT, phase);
        int pawnMaterial = TUNING.material(Piece.PAWN, phase);

        int whiteQueenMaterial = whiteQueenCount * queenMaterial;
        int whiteRookMaterial = whiteRookCount * rookMaterial;
        int whiteBishopMaterial = whiteBishopCount * bishopMaterial;
        int whiteKnightMaterial = whiteKnightCount * knightMaterial;
        int whitePawnMaterial = whitePawnCount * pawnMaterial;
        int blackQueenMaterial = blackQueenCount * queenMaterial;
        int blackRookMaterial = blackRookCount * rookMaterial;
        int blackBishopMaterial = blackBishopCount * bishopMaterial;
        int blackKnightMaterial = blackKnightCount * knightMaterial;
        int blackPawnMaterial = blackPawnCount * pawnMaterial;

        int whitePieceMaterial = whiteQueenMaterial + whiteRookMaterial
                + whiteBishopMaterial + whiteKnightMaterial;
        int blackPieceMaterial = blackQueenMaterial + blackRookMaterial
                + blackBishopMaterial + blackKnightMaterial;

        long whiteQueenResult = queenEval(whiteQueen, whiteQueenMaterial, phase, Value.WHITE,
                whiteBishop, whiteKnight, allOccupancy, whiteOccupancy,
                blackKingRank, blackKingFile, KING_RING[Value.BLACK][blackKingSquare], collector);
        long whiteRookResult = rookEval(whiteRook, whiteRookCount, whiteRookMaterial,
                (status & 0b110) != 0, phase, Value.WHITE, whitePawn, whitePawnCount,
                allOccupancy, whiteOccupancy, blackPawn, blackQueen,
                blackKingRank, blackKingFile, KING_RING[Value.BLACK][blackKingSquare], collector);
        long whiteBishopResult = bishopEval(whiteBishop, whiteBishopCount, whiteBishopMaterial,
                phase, Value.WHITE, allOccupancy, whiteOccupancy, whitePawn, blackPawn,
                whiteKingRank, whiteKingFile, blackKingRank, blackKingFile,
                KING_RING[Value.BLACK][blackKingSquare], collector);
        long whiteKnightResult = knightEval(whiteKnight, whiteKnightCount, whiteKnightMaterial,
                phase, Value.WHITE, whiteOccupancy, whitePawn, whitePawnCount, blackPawn,
                whiteKingRank, whiteKingFile, blackKingRank, blackKingFile,
                KING_RING[Value.BLACK][blackKingSquare], collector);

        long blackQueenResult = queenEval(blackQueen, blackQueenMaterial, phase, Value.BLACK,
                blackBishop, blackKnight, allOccupancy, blackOccupancy,
                whiteKingRank, whiteKingFile, KING_RING[Value.WHITE][whiteKingSquare], collector);
        long blackRookResult = rookEval(blackRook, blackRookCount, blackRookMaterial,
                (status & 0b11000) != 0, phase, Value.BLACK, blackPawn, blackPawnCount,
                allOccupancy, blackOccupancy, whitePawn, whiteQueen,
                whiteKingRank, whiteKingFile, KING_RING[Value.WHITE][whiteKingSquare], collector);
        long blackBishopResult = bishopEval(blackBishop, blackBishopCount, blackBishopMaterial,
                phase, Value.BLACK, allOccupancy, blackOccupancy, blackPawn, whitePawn,
                blackKingRank, blackKingFile, whiteKingRank, whiteKingFile,
                KING_RING[Value.WHITE][whiteKingSquare], collector);
        long blackKnightResult = knightEval(blackKnight, blackKnightCount, blackKnightMaterial,
                phase, Value.BLACK, blackOccupancy, blackPawn, blackPawnCount, whitePawn,
                blackKingRank, blackKingFile, whiteKingRank, whiteKingFile,
                KING_RING[Value.WHITE][whiteKingSquare], collector);

        long whiteEval = kingEval(Value.WHITE, whiteKingSquare, phase,
                whiteKingRank, whiteKingFile, whiteRook, whitePawn, blackPawn,
                whitePieceMaterial, blackPieceMaterial, blackKingFile, blackKingRank, collector)
                + unpackEval(whiteQueenResult) + unpackEval(whiteRookResult)
                + unpackEval(whiteBishopResult) + unpackEval(whiteKnightResult)
                + pawnEval(whitePawn, whitePawnMaterial, phase, Value.WHITE,
                whiteBishop | whiteKnight,
                blackPawn, whitePieceMaterial, whiteKingRank, whiteKingFile,
                blackKingRank, blackKingFile, blackPieceMaterial,
                status & Board.PLAYER_BIT, allOccupancy, collector);
        long blackEval = kingEval(Value.BLACK, blackKingSquare, phase,
                blackKingRank, blackKingFile, blackRook, blackPawn, whitePawn,
                blackPieceMaterial, whitePieceMaterial, whiteKingFile, whiteKingRank, collector)
                + unpackEval(blackQueenResult) + unpackEval(blackRookResult)
                + unpackEval(blackBishopResult) + unpackEval(blackKnightResult)
                + pawnEval(blackPawn, blackPawnMaterial, phase, Value.BLACK,
                blackBishop | blackKnight,
                whitePawn, blackPieceMaterial, blackKingRank, blackKingFile,
                whiteKingRank, whiteKingFile, whitePieceMaterial,
                status & Board.PLAYER_BIT, allOccupancy, collector);

        int whiteSafety = unpackSafety(blackQueenResult) + unpackSafety(blackRookResult)
                + unpackSafety(blackBishopResult) + unpackSafety(blackKnightResult);
        int blackSafety = unpackSafety(whiteQueenResult) + unpackSafety(whiteRookResult)
                + unpackSafety(whiteBishopResult) + unpackSafety(whiteKnightResult);
        int whiteSafetyPenalty = -SAFETY_VALUE[Math.min(whiteSafety, SAFETY_VALUE.length - 1)];
        int blackSafetyPenalty = -SAFETY_VALUE[Math.min(blackSafety, SAFETY_VALUE.length - 1)];
        whiteEval += whiteSafetyPenalty;
        blackEval += blackSafetyPenalty;
        add(collector, Value.WHITE, Feature.KING_SAFETY, whiteSafetyPenalty);
        add(collector, Value.BLACK, Feature.KING_SAFETY, blackSafetyPenalty);

        requireStaticRange("white", whiteEval);
        requireStaticRange("black", blackEval);
        long whiteRelative = whiteEval - blackEval;
        requireStaticRange("relative", whiteRelative);
        int result = Math.toIntExact((status & Board.PLAYER_BIT) == Value.WHITE
                ? whiteRelative : -whiteRelative);
        if (collector != null) {
            collector.verifySide(Value.WHITE, whiteEval);
            collector.verifySide(Value.BLACK, blackEval);
        }
        return result;
    }

    private static int kingEval(int player, int kingSquare, int phase,
                                int kingRank, int kingFile, long rooks, long pawns,
                                long enemyPawns, int material, int enemyMaterial,
                                int enemyKingFile, int enemyKingRank, Collector collector) {
        int eval = TUNING.bonus(Piece.KING, player, kingSquare, phase);
        add(collector, player, Feature.PIECE_SQUARE, eval);
        if (kingRank == (player == Value.WHITE ? WHITE_BACK_RANK : BLACK_BACK_RANK)) {
            int shelter = kingBackRankShelter(player, kingFile, phase, rooks, pawns, enemyPawns);
            eval += shelter;
            add(collector, player, Feature.KING_SHELTER, shelter);
        }
        if (material > enemyMaterial && material <= exchangeValue(Piece.QUEEN)) {
            int edgeDistance = Math.abs(enemyKingFile - CENTRE_FILE)
                    + Math.abs(enemyKingRank - CENTRE_RANK);
            int kingDistance = Math.abs(kingFile - enemyKingFile)
                    + Math.abs(kingRank - enemyKingRank);
            int distance = ((edgeDistance * EDGE_WEIGHT
                    + (14 - kingDistance) * PROXIMITY_WEIGHT) * (MAX_PHASE - phase)) / MAX_PHASE;
            eval += distance;
            add(collector, player, Feature.DISTANCE, distance);
        }
        return eval;
    }

    private static int kingBackRankShelter(int player, int file, int phase,
                                           long rooks, long pawns, long enemyPawns) {
        int eval = 0;
        long rookProtectMask = switch (file) {
            case 0 -> player == Value.WHITE ? 0x000000000000000eL : 0x0e00000000000000L;
            case 1 -> player == Value.WHITE ? 0x000000000000000cL : 0x0c00000000000000L;
            case 2 -> player == Value.WHITE ? 0x0000000000000008L : 0x0800000000000000L;
            case 5 -> player == Value.WHITE ? 0x0000000000000010L : 0x1000000000000000L;
            case 6 -> player == Value.WHITE ? 0x0000000000000030L : 0x3000000000000000L;
            case 7 -> player == Value.WHITE ? 0x0000000000000070L : 0x7000000000000000L;
            default -> 0L;
        };
        if ((rookProtectMask & rooks) != 0L) {
            eval += TUNING.scalar(EvalTuning.ROOK_PROTECTS, phase);
        }
        long rookBlockMask = switch (file) {
            case 1 -> 0x0100000000000001L;
            case 2 -> 0x0300000000000003L;
            case 3 -> 0x0700000000000007L;
            case 5 -> 0xe0000000000000e0L;
            case 6 -> 0xc0000000000000c0L;
            default -> 0L;
        };
        if ((Bitboard.BB[Bitboard.ROOK_START_POSITION_PLAYER0 + player][0]
                & rookBlockMask & rooks) != 0L) {
            eval += TUNING.scalar(EvalTuning.KING_BLOCKS_ROOK, phase);
        }
        if (file <= 2 || file >= 5) {
            int side = file <= 2 ? 1 : 0;
            boolean queenSide = side == 1;
            int shieldBase = (queenSide
                    ? Bitboard.PAWN_SHIELD_QUEENSIDE_CLOSE_PLAYER0
                    : Bitboard.PAWN_SHIELD_KINGSIDE_CLOSE_PLAYER0) + player;
            int stormBase = (queenSide
                    ? Bitboard.PAWN_STORM_QUEENSIDE_CLOSE_PLAYER0
                    : Bitboard.PAWN_STORM_KINGSIDE_CLOSE_PLAYER0) + player;
            int closeShield = Long.bitCount(Bitboard.BB[shieldBase][0] & pawns);
            int farShield = Long.bitCount(Bitboard.BB[shieldBase + 4][0] & pawns);
            int closeStorm = Long.bitCount(Bitboard.BB[stormBase][0] & enemyPawns);
            int farStorm = Long.bitCount(Bitboard.BB[stormBase + 4][0] & enemyPawns);
            eval += TUNING.pawnCover(EvalTuning.SHIELD, side, EvalTuning.CLOSE,
                    closeShield, phase);
            eval += TUNING.pawnCover(EvalTuning.SHIELD, side, EvalTuning.FAR,
                    farShield, phase);
            // PAWN_STORM data is already signed as a penalty. SeedV3 subtracted
            // these negative values and accidentally rewarded an enemy storm.
            eval += TUNING.pawnCover(EvalTuning.STORM, side, EvalTuning.CLOSE,
                    closeStorm, phase);
            eval += TUNING.pawnCover(EvalTuning.STORM, side, EvalTuning.FAR,
                    farStorm, phase);
        }
        return eval;
    }

    private static long queenEval(long queens, int material, int phase, int player,
                                  long bishops, long knights, long allOccupancy,
                                  long occupancy, int enemyKingRank, int enemyKingFile,
                                  long enemyKingRing, Collector collector) {
        int eval = material;
        add(collector, player, Feature.MATERIAL, material);
        if ((queens & Bitboard.BB[Bitboard.QUEEN_START_POSITION_PLAYER0 + player][0]) == 0L
                && (bishops & Bitboard.BB[Bitboard.BISHOP_START_POSITION_PLAYER0 + player][0]) != 0L
                && (knights & Bitboard.BB[Bitboard.KNIGHT_START_POSITION_PLAYER0 + player][0]) != 0L) {
            int development = TUNING.scalar(EvalTuning.QUEEN_EARLY_DEVELOPMENT, phase);
            eval += development;
            add(collector, player, Feature.DEVELOPMENT, development);
        }
        int safety = 0;
        while (queens != 0L) {
            int square = Long.numberOfTrailingZeros(queens);
            queens &= queens - 1;
            int squareBonus = TUNING.bonus(Piece.QUEEN, player, square, phase);
            eval += squareBonus;
            add(collector, player, Feature.PIECE_SQUARE, squareBonus);
            long attacks = Pext.queenMoves(square, allOccupancy) & ~occupancy;
            int mobility = TUNING.mobility(Piece.QUEEN, Long.bitCount(attacks), phase);
            eval += mobility;
            add(collector, player, Feature.MOBILITY, mobility);
            int distanceScore = TUNING.enemyDistance(Piece.QUEEN,
                    manhattan(square, enemyKingRank, enemyKingFile), phase);
            eval += distanceScore;
            add(collector, player, Feature.DISTANCE, distanceScore);
            safety += TUNING.safety(Piece.QUEEN, Long.bitCount(attacks & enemyKingRing));
        }
        return pack(eval, safety);
    }

    private static long rookEval(long rooks, int count, int material,
                                 boolean hasCastlingRights, int phase, int player,
                                 long pawns, int pawnCount, long allOccupancy, long occupancy,
                                 long enemyPawns, long enemyQueens, int enemyKingRank,
                                 int enemyKingFile, long enemyKingRing, Collector collector) {
        int eval = material;
        add(collector, player, Feature.MATERIAL, material);
        if (Long.bitCount(rooks & Bitboard.BB[Bitboard.ROOK_START_POSITION_PLAYER0 + player][0]) < 2
                && hasCastlingRights) {
            int development = TUNING.scalar(EvalTuning.ROOK_EARLY_DEVELOPMENT, phase);
            eval += development;
            add(collector, player, Feature.DEVELOPMENT, development);
        }
        int structure = count > 1 ? TUNING.scalar(EvalTuning.ROOK_PAIR, phase) : 0;
        structure += TUNING.piecePawn(Piece.ROOK, Math.min(count, 2),
                Math.min(pawnCount, 8), phase);
        eval += structure;
        add(collector, player, Feature.ROOK_STRUCTURE, structure);
        int safety = 0;
        while (rooks != 0L) {
            int square = Long.numberOfTrailingZeros(rooks);
            rooks &= rooks - 1;
            int squareBonus = TUNING.bonus(Piece.ROOK, player, square, phase);
            eval += squareBonus;
            add(collector, player, Feature.PIECE_SQUARE, squareBonus);
            long attacks = Pext.rookMoves(square, allOccupancy) & ~occupancy;
            int mobility = TUNING.mobility(Piece.ROOK, Long.bitCount(attacks), phase);
            eval += mobility;
            add(collector, player, Feature.MOBILITY, mobility);
            long fileMask = FILE[square & Value.FILE];
            int fileScore = (pawns & fileMask) == 0L
                    ? TUNING.scalar(EvalTuning.ROOK_OPEN_FILE, phase) : 0;
            fileScore += (enemyPawns & fileMask) == 0L
                    ? TUNING.scalar(EvalTuning.ROOK_OPEN_FILE, phase) : 0;
            fileScore += (enemyQueens & fileMask) != 0L
                    ? TUNING.scalar(EvalTuning.ROOK_ON_QUEEN_FILE, phase) : 0;
            eval += fileScore;
            add(collector, player, Feature.ROOK_STRUCTURE, fileScore);
            int distanceScore = TUNING.enemyDistance(Piece.ROOK,
                    manhattan(square, enemyKingRank, enemyKingFile), phase);
            eval += distanceScore;
            add(collector, player, Feature.DISTANCE, distanceScore);
            safety += TUNING.safety(Piece.ROOK, Long.bitCount(attacks & enemyKingRing));
        }
        return pack(eval, safety);
    }

    private static long bishopEval(long bishops, int count, int material, int phase, int player,
                                   long allOccupancy, long occupancy, long pawns,
                                   long enemyPawns, int kingRank, int kingFile,
                                   int enemyKingRank, int enemyKingFile,
                                   long enemyKingRing, Collector collector) {
        int eval = material;
        add(collector, player, Feature.MATERIAL, material);
        int pair = count > 1 ? TUNING.scalar(EvalTuning.BISHOP_PAIR, phase) : 0;
        eval += pair;
        add(collector, player, Feature.MINOR_STRUCTURE, pair);
        int safety = 0;
        while (bishops != 0L) {
            int square = Long.numberOfTrailingZeros(bishops);
            bishops &= bishops - 1;
            int squareBonus = TUNING.bonus(Piece.BISHOP, player, square, phase);
            eval += squareBonus;
            add(collector, player, Feature.PIECE_SQUARE, squareBonus);
            long attacks = Pext.bishopMoves(square, allOccupancy) & ~occupancy;
            int mobility = TUNING.mobility(Piece.BISHOP, Long.bitCount(attacks), phase);
            eval += mobility;
            add(collector, player, Feature.MOBILITY, mobility);
            int file = square & Value.FILE;
            int rank = square >>> 3;
            int structure = 0;
            if ((PAWN_ATTACKS[1 ^ player][square] & pawns) != 0L
                    && (PASSED_PAWN_FILES[player][file] & FORWARD_RANKS[player][rank]
                    & enemyPawns) == 0L) {
                structure += TUNING.scalar(EvalTuning.BISHOP_OUTPOST, phase);
            }
            long colourComplex = ((rank & 1) == (file & 1)) ? ~LIGHT_SQUARES : LIGHT_SQUARES;
            structure += TUNING.badBishop(Long.bitCount(pawns & colourComplex),
                    Long.bitCount(enemyPawns & colourComplex), phase);
            eval += structure;
            add(collector, player, Feature.MINOR_STRUCTURE, structure);
            int distanceScore = TUNING.protectorDistance(Piece.BISHOP,
                    Math.abs(file - kingFile) + Math.abs(rank - kingRank), phase);
            distanceScore += TUNING.enemyDistance(Piece.BISHOP,
                    Math.abs(file - enemyKingFile) + Math.abs(rank - enemyKingRank), phase);
            eval += distanceScore;
            add(collector, player, Feature.DISTANCE, distanceScore);
            safety += TUNING.safety(Piece.BISHOP, Long.bitCount(attacks & enemyKingRing));
        }
        return pack(eval, safety);
    }

    private static long knightEval(long knights, int count, int material, int phase, int player,
                                   long occupancy, long pawns, int pawnCount, long enemyPawns,
                                   int kingRank, int kingFile, int enemyKingRank, int enemyKingFile,
                                   long enemyKingRing, Collector collector) {
        int eval = material;
        add(collector, player, Feature.MATERIAL, material);
        int structure = count > 1 ? TUNING.scalar(EvalTuning.KNIGHT_PAIR, phase) : 0;
        structure += TUNING.piecePawn(Piece.KNIGHT, Math.min(count, 2),
                Math.min(pawnCount, 8), phase);
        eval += structure;
        add(collector, player, Feature.MINOR_STRUCTURE, structure);
        int safety = 0;
        while (knights != 0L) {
            int square = Long.numberOfTrailingZeros(knights);
            knights &= knights - 1;
            int squareBonus = TUNING.bonus(Piece.KNIGHT, player, square, phase);
            eval += squareBonus;
            add(collector, player, Feature.PIECE_SQUARE, squareBonus);
            long attacks = Bitboard.BB[Bitboard.LEAP_ATTACKS][square] & ~occupancy;
            int mobility = TUNING.mobility(Piece.KNIGHT, Long.bitCount(attacks), phase);
            eval += mobility;
            add(collector, player, Feature.MOBILITY, mobility);
            int file = square & Value.FILE;
            int rank = square >>> 3;
            int outpost = 0;
            if ((PAWN_ATTACKS[1 ^ player][square] & pawns) != 0L
                    && (PASSED_PAWN_FILES[player][file] & FORWARD_RANKS[player][rank]
                    & enemyPawns) == 0L) {
                outpost = TUNING.scalar(EvalTuning.KNIGHT_OUTPOST, phase);
            }
            eval += outpost;
            add(collector, player, Feature.MINOR_STRUCTURE, outpost);
            int distanceScore = TUNING.protectorDistance(Piece.KNIGHT,
                    Math.abs(file - kingFile) + Math.abs(rank - kingRank), phase);
            distanceScore += TUNING.enemyDistance(Piece.KNIGHT,
                    Math.abs(file - enemyKingFile) + Math.abs(rank - enemyKingRank), phase);
            eval += distanceScore;
            add(collector, player, Feature.DISTANCE, distanceScore);
            safety += TUNING.safety(Piece.KNIGHT, Long.bitCount(attacks & enemyKingRing));
        }
        return pack(eval, safety);
    }

    private static int pawnEval(long pawns, int materialScore, int phase, int player,
                                long protectedMinors,
                                long enemyPawns, int material, int kingRank, int kingFile,
                                int enemyKingRank, int enemyKingFile, int enemyMaterial,
                                int sideToMove, long allOccupancy, Collector collector) {
        int eval = materialScore;
        add(collector, player, Feature.MATERIAL, materialScore);
        long originalPawns = pawns;
        while (pawns != 0L) {
            int square = Long.numberOfTrailingZeros(pawns);
            pawns &= pawns - 1;
            int squareBonus = TUNING.bonus(Piece.PAWN, player, square, phase);
            eval += squareBonus;
            add(collector, player, Feature.PIECE_SQUARE, squareBonus);
            int file = square & Value.FILE;
            int rank = square >>> 3;
            int maskIndex = (player << 6) | square;
            long adjacentFiles = ADJACENT_FILE_MASK[square];
            long adjacentPawns = originalPawns & adjacentFiles;
            int structure = 0;
            if (Long.bitCount(originalPawns & FILE[file]) > 1) {
                structure += TUNING.scalar(EvalTuning.DOUBLED_PAWN, phase);
            }
            if ((originalPawns & WEAK_PAWN_SUPPORT_MASK[maskIndex]) == 0L) {
                structure += TUNING.scalar(EvalTuning.WEAK_PAWN, phase);
            }
            if (adjacentPawns == 0L) {
                structure += TUNING.scalar(EvalTuning.ISOLATED_PAWN, phase);
            }
            if ((PAWN_ATTACKS[player][square] & protectedMinors) != 0L) {
                structure += TUNING.scalar(EvalTuning.PAWN_PROTECTS, phase);
            }
            eval += structure;
            add(collector, player, Feature.PAWN_STRUCTURE, structure);

            if ((enemyPawns & PASSED_PAWN_MASK[maskIndex]) == 0L) {
                int passed = 50 * (player == Value.WHITE ? rank : 7 - rank);
                if ((originalPawns & PHALANX_MASK[square]) != 0L) {
                    passed += TUNING.scalar(EvalTuning.PASSED_PAWN_PHALANX, phase);
                }
                if (material < TUNING.material(Piece.QUEEN, MAX_PHASE)) {
                    int ownDistance = Math.max(Math.abs(file - kingFile), Math.abs(rank - kingRank));
                    int enemyDistance = Math.max(Math.abs(file - enemyKingFile),
                            Math.abs(rank - enemyKingRank));
                    int ownProximity = 8 - ownDistance;
                    passed += (ownProximity * ownProximity + enemyDistance * enemyDistance)
                            * (player == Value.WHITE ? rank : 7 - rank);
                }
                if (enemyMaterial == 0 && kingCannotCatchPassedPawn(player, square,
                        enemyKingRank, enemyKingFile, sideToMove, allOccupancy)) {
                    passed += TUNING.material(Piece.BISHOP, phase);
                }
                eval += passed;
                add(collector, player, Feature.PASSED_PAWN, passed);
            }
        }
        return eval;
    }

    private static boolean kingCannotCatchPassedPawn(int player, int square,
                                                     int enemyKingRank, int enemyKingFile,
                                                     int sideToMove, long allOccupancy) {
        int rank = square >>> 3;
        int file = square & Value.FILE;
        int promotionRank = player == Value.WHITE ? 7 : 0;
        long path = PROMOTION_PATH_MASK[(player << 6) | square];
        if ((path & allOccupancy) != 0L) {
            return false;
        }
        int pawnMoves = Math.abs(promotionRank - rank);
        int direction = player == Value.WHITE ? 1 : -1;
        int startingRank = player == Value.WHITE ? 1 : 6;
        if (rank == startingRank
                && (allOccupancy & (1L << (square + direction * 8))) == 0L
                && (allOccupancy & (1L << (square + direction * 16))) == 0L) {
            pawnMoves--;
        }
        int kingMovesAvailable = pawnMoves - (sideToMove == player ? 1 : 0);
        int kingDistance = Math.max(Math.abs(enemyKingFile - file),
                Math.abs(enemyKingRank - promotionRank));
        return kingDistance > kingMovesAvailable;
    }

    static long adjacentFileMask(int square) {
        return ADJACENT_FILE_MASK[square];
    }

    static long phalanxMask(int square) {
        return PHALANX_MASK[square];
    }

    static long weakPawnSupportMask(int player, int square) {
        return WEAK_PAWN_SUPPORT_MASK[(player << 6) | square];
    }

    static long passedPawnMask(int player, int square) {
        return PASSED_PAWN_MASK[(player << 6) | square];
    }

    static long promotionPathMask(int player, int square) {
        return PROMOTION_PATH_MASK[(player << 6) | square];
    }

    private static int manhattan(int square, int rank, int file) {
        return Math.abs((square & Value.FILE) - file) + Math.abs((square >>> 3) - rank);
    }

    private static long pack(int eval, int safety) {
        // Keep all 32 safety bits. SeedV3 packed only eight and could wrap the
        // accumulated attack weight in legal promoted-material positions.
        return ((long) eval << EVAL_SHIFT) | (safety & 0xffff_ffffL);
    }

    private static int unpackEval(long packed) {
        return (int) (packed >> EVAL_SHIFT);
    }

    private static int unpackSafety(long packed) {
        return (int) packed;
    }

    private static void add(Collector collector, int player, Feature feature, int value) {
        if (collector != null) {
            collector.values[player][feature.ordinal()] += value;
        }
    }

    private static void requireBoardArray(long[] board) {
        if (board == null || board.length < Board.MAX_BITBOARDS) {
            throw new IllegalArgumentException("A complete V6 board is required");
        }
    }

    private static void requireSingleKing(String colour, long king) {
        if (Long.bitCount(king) != 1) {
            throw new IllegalArgumentException("Evaluation requires exactly one " + colour + " king");
        }
    }

    private static void requireStaticRange(String name, long value) {
        if (value < -MAX_STATIC_SCORE || value > MAX_STATIC_SCORE) {
            throw new ArithmeticException(name + " static evaluation " + value
                    + " exceeds +/-" + MAX_STATIC_SCORE);
        }
    }

    public record TuningSummary(int materialEntries, int pieceSquareEntries,
                                int criteriaEntries, String materialSha256,
                                String pieceSquareSha256, String criteriaSha256) {}

    public record SideBreakdown(int material, int pieceSquare, int mobility,
                                int development, int kingShelter, int kingSafety,
                                int pawnStructure, int passedPawns, int rookStructure,
                                int minorStructure, int distance) {
        public int total() {
            int total = Math.addExact(material, pieceSquare);
            total = Math.addExact(total, mobility);
            total = Math.addExact(total, development);
            total = Math.addExact(total, kingShelter);
            total = Math.addExact(total, kingSafety);
            total = Math.addExact(total, pawnStructure);
            total = Math.addExact(total, passedPawns);
            total = Math.addExact(total, rookStructure);
            total = Math.addExact(total, minorStructure);
            return Math.addExact(total, distance);
        }
    }

    public record Breakdown(int phase, int sideToMove, SideBreakdown white,
                            SideBreakdown black, int total) {}

    private enum Feature {
        MATERIAL,
        PIECE_SQUARE,
        MOBILITY,
        DEVELOPMENT,
        KING_SHELTER,
        KING_SAFETY,
        PAWN_STRUCTURE,
        PASSED_PAWN,
        ROOK_STRUCTURE,
        MINOR_STRUCTURE,
        DISTANCE
    }

    private static final class Collector {
        private final long[][] values = new long[2][Feature.values().length];
        private int phase;
        private int sideToMove;

        private void verifySide(int player, long expected) {
            long actual = 0;
            for (long value : values[player]) {
                actual += value;
            }
            if (actual != expected) {
                throw new IllegalStateException("Evaluation breakdown diverged: "
                        + actual + " != " + expected);
            }
        }

        private Breakdown toBreakdown(int total) {
            SideBreakdown white = side(Value.WHITE);
            SideBreakdown black = side(Value.BLACK);
            int reconstructed = sideToMove == Value.WHITE
                    ? white.total() - black.total()
                    : black.total() - white.total();
            if (reconstructed != total) {
                throw new IllegalStateException("Evaluation total diverged: "
                        + reconstructed + " != " + total);
            }
            return new Breakdown(phase, sideToMove, white, black, total);
        }

        private SideBreakdown side(int player) {
            return new SideBreakdown(
                    checked(player, Feature.MATERIAL),
                    checked(player, Feature.PIECE_SQUARE),
                    checked(player, Feature.MOBILITY),
                    checked(player, Feature.DEVELOPMENT),
                    checked(player, Feature.KING_SHELTER),
                    checked(player, Feature.KING_SAFETY),
                    checked(player, Feature.PAWN_STRUCTURE),
                    checked(player, Feature.PASSED_PAWN),
                    checked(player, Feature.ROOK_STRUCTURE),
                    checked(player, Feature.MINOR_STRUCTURE),
                    checked(player, Feature.DISTANCE));
        }

        private int checked(int player, Feature feature) {
            return Math.toIntExact(values[player][feature.ordinal()]);
        }
    }

    private Eval() {}
}
