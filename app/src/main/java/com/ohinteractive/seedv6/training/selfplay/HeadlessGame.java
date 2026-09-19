package com.ohinteractive.seedv6.training.selfplay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.Gen;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.rules.DrawAdjudicator;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.rules.SearchLineHistory;

/** Single-owner training session using the same pure rule facilities and precedence as GameSession. */
public final class HeadlessGame {
    private final int maximumPlies;
    private final GameHistory.Builder history;
    private final List<GameTrajectory.Position> positions = new ArrayList<>();
    private final long[] moves = new long[256];
    private final long[] scratch = new long[Board.MAX_BITBOARDS];
    private long[] board;
    private int moveCount;
    private GameTermination termination = GameTermination.ACTIVE;
    private String failure;

    /** The supplied board is a legal initial position; no earlier repetition history is implied. */
    public HeadlessGame(long[] initialBoard, int maximumPlies) {
        this(initialBoard, GameHistory.initial(initialBoard), maximumPlies);
    }

    /** Copy the entire opening state; the cap counts subsequent played plies only. */
    public HeadlessGame(long[] initialBoard, GameHistory initialHistory, int maximumPlies) {
        if (initialBoard.length != Board.MAX_BITBOARDS || maximumPlies < 1) {
            throw new IllegalArgumentException("Expected six-long board and positive ply cap.");
        }
        this.maximumPlies = maximumPlies;
        board = initialBoard.clone();
        initialHistory.requireCurrent(board);
        history = GameHistory.builder(initialHistory);
        refresh();
    }

    public long[] boardSnapshot() { return board.clone(); }
    public GameHistory historySnapshot() {
        GameHistory snapshot = history.snapshot();
        snapshot.requireCurrent(board);
        return snapshot;
    }
    public int playedPlies() { return positions.size(); }
    public int sideToMove() { return Board.player((int) board[Board.STATUS]); }
    public GameTermination termination() { return termination; }
    public boolean active() { return termination == GameTermination.ACTIVE; }
    public long[] legalMoves() { return active() ? Arrays.copyOf(moves, moveCount) : new long[0]; }

    /** Reject foreign/stale move encodings before changing board, history, or trajectory. */
    public void play(long move) {
        if (!active()) throw new IllegalStateException("Game has stopped.");
        boolean legal = false;
        for (int i = 0; i < moveCount; i++) if (moves[i] == move) { legal = true; break; }
        if (!legal) throw new IllegalArgumentException("Move is not an exact generated legal move.");
        long[] child = new long[Board.MAX_BITBOARDS];
        Board.makeMoveInto(board[0], board[1], board[2], board[3],
                (int) board[Board.STATUS], board[Board.KEY], move, child);
        history.appendPosition(child);
        positions.add(new GameTrajectory.Position(board, positions.size(), move));
        board = child;
        refresh();
    }

    public void abort(GameTermination reason, String detail) {
        if (!reason.aborted()) throw new IllegalArgumentException("Expected administrative termination.");
        if (active()) { termination = reason; failure = detail; }
    }

    public GameTrajectory trajectory() {
        return new GameTrajectory(positions, board, historySnapshot(), termination, failure);
    }

    private void refresh() {
        moveCount = Gen.genAll(board[0], board[1], board[2], board[3],
                (int) board[Board.STATUS], board[Board.KEY], true, moves, scratch);
        // Mate/stalemate precede rule draws, exactly as in the ordinary game session.
        if (moveCount == 0) {
            boolean check = Board.isPlayerInCheckPext(board[0], board[1], board[2], board[3], sideToMove());
            termination = !check ? GameTermination.STALEMATE : sideToMove() == Value.BLACK
                    ? GameTermination.WHITE_CHECKMATES_BLACK : GameTermination.BLACK_CHECKMATES_WHITE;
        } else {
            termination = switch (DrawAdjudicator.adjudicateNonTerminal(board,
                    new SearchLineHistory(history.snapshot()))) {
                case NONE -> GameTermination.ACTIVE;
                case FIFTY_MOVE -> GameTermination.FIFTY_MOVE_RULE;
                case FORMAL_THREEFOLD -> GameTermination.THREEFOLD_REPETITION;
                case INSUFFICIENT_MATERIAL -> GameTermination.INSUFFICIENT_MATERIAL;
            };
        }
        // A genuine result reached on the last permitted move remains a chess result.
        if (active() && positions.size() >= maximumPlies) termination = GameTermination.PLY_CAP;
    }
}
