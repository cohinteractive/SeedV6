package com.ohinteractive.seedv6.gui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.move.LegalMoveResolver;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.move.MoveIntent;
import com.ohinteractive.seedv6.core.move.MoveIntent.Promotion;
import com.ohinteractive.seedv6.core.util.Piece;
import com.ohinteractive.seedv6.rules.DrawAdjudicator;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;

/** EDT-confined authoritative board/history state for the Swing harness. */
final class GameSession {

    static GameSession startingPosition() {
        return new GameSession(Board.startingPosition());
    }

    static GameSession fromFen(String fen) {
        return new GameSession(Board.fromFen(fen));
    }

    private GameSession(long[] initialBoard) {
        board = initialBoard.clone();
        history = GameHistory.builder(board);
        refreshLegalAndStatus();
    }

    long[] boardSnapshot() {
        return board.clone();
    }

    GameHistory historySnapshot() {
        final GameHistory snapshot = history.snapshot();
        snapshot.requireCurrent(board);
        return snapshot;
    }

    PositionStatus status() {
        return status;
    }

    List<String> moveHistory() {
        return List.copyOf(moveHistory);
    }

    int historySize() {
        return history.size();
    }

    int lastFrom() {
        return lastFrom;
    }

    int lastTo() {
        return lastTo;
    }

    long[] legalMoves() {
        final long[] copy = new long[legalMoveCount];
        System.arraycopy(legalMoves, 0, copy, 0, legalMoveCount);
        return copy;
    }

    int[] legalDestinations(int fromSquare) {
        final Set<Integer> destinations = new LinkedHashSet<>();
        for(int index = 0; index < legalMoveCount; index ++) {
            if(Move.fromSquare(legalMoves[index]) == fromSquare) {
                destinations.add(Move.toSquare(legalMoves[index]));
            }
        }
        return destinations.stream().mapToInt(Integer::intValue).toArray();
    }

    List<Promotion> promotionChoices(int fromSquare, int toSquare) {
        final Set<Promotion> available = new LinkedHashSet<>();
        for(int index = 0; index < legalMoveCount; index ++) {
            final long move = legalMoves[index];
            if(Move.fromSquare(move) == fromSquare && Move.toSquare(move) == toSquare
                && Move.promotion(move) != Promotion.NONE) {
                available.add(Move.promotion(move));
            }
        }
        final List<Promotion> ordered = new ArrayList<>(4);
        for(Promotion promotion : PROMOTION_ORDER) {
            if(available.contains(promotion)) ordered.add(promotion);
        }
        return List.copyOf(ordered);
    }

    long applyIntent(int fromSquare, int toSquare, Promotion promotion) {
        final long move = resolver.resolve(board, new MoveIntent(fromSquare, toSquare, promotion));
        applyGeneratedMove(move);
        return move;
    }

    void applyGeneratedMove(long move) {
        boolean generatedLegal = false;
        for(int index = 0; index < legalMoveCount; index ++) {
            if(legalMoves[index] == move) {
                generatedLegal = true;
                break;
            }
        }
        if(!generatedLegal) {
            throw new IllegalArgumentException("Move is not an exact generated legal move: " + move);
        }

        final long[] child = new long[Board.MAX_BITBOARDS];
        Board.makeMoveInto(
            board[0], board[1], board[2], board[3],
            (int) board[Board.STATUS], board[Board.KEY], move, child
        );
        history.appendPosition(child);
        board = child;
        moveHistory.add(Move.coordinate(move));
        lastFrom = Move.fromSquare(move);
        lastTo = Move.toSquare(move);
        refreshLegalAndStatus();
    }

    private static final int MAX_MOVES = 256;
    private static final Promotion[] PROMOTION_ORDER = {
        Promotion.QUEEN, Promotion.ROOK, Promotion.BISHOP, Promotion.KNIGHT
    };

    private final LegalMoveResolver resolver = new LegalMoveResolver();
    private final long[] legalMoves = new long[MAX_MOVES];
    private final long[] generatorScratch = new long[Board.MAX_BITBOARDS];
    private final List<String> moveHistory = new ArrayList<>();
    private long[] board;
    private GameHistory.Builder history;
    private int legalMoveCount;
    private PositionStatus status;
    private int lastFrom = -1;
    private int lastTo = -1;

    private void refreshLegalAndStatus() {
        legalMoveCount = Gen.genAll(
            board[0], board[1], board[2], board[3],
            (int) board[Board.STATUS], board[Board.KEY], true,
            legalMoves, generatorScratch
        );
        final int side = Board.player((int) board[Board.STATUS]);
        final boolean check = Board.isPlayerInCheckPext(
            board[0], board[1], board[2], board[3], side
        );
        final PositionStatus.Outcome outcome;
        if(legalMoveCount == 0) {
            outcome = check
                ? PositionStatus.Outcome.CHECKMATE : PositionStatus.Outcome.STALEMATE;
        } else {
            final DrawAdjudicator.RuleDraw draw = DrawAdjudicator.adjudicateNonTerminal(
                board, new SearchLineHistory(history.snapshot())
            );
            outcome = PositionStatus.fromRuleDraw(draw);
        }
        status = new PositionStatus(side, check, outcome, check ? findKingSquare(side) : -1);
    }

    private int findKingSquare(int side) {
        final int king = Piece.KING | (side << Board.PLAYER_SHIFT);
        for(int square = 0; square < 64; square ++) {
            if(Board.getSquare(board[0], board[1], board[2], board[3], square) == king) {
                return square;
            }
        }
        return -1;
    }
}
