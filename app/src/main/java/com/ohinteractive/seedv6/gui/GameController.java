package com.ohinteractive.seedv6.gui;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.swing.SwingUtilities;

import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.core.move.MoveIntent.Promotion;
import com.ohinteractive.seedv6.core.util.Value;
import com.ohinteractive.seedv6.search.common.IterationSnapshot;
import com.ohinteractive.seedv6.search.common.SearchResult;
import com.ohinteractive.seedv6.search.common.SearchTermination;
import com.ohinteractive.seedv6.search.manage.ManagedSearchResult;
import com.ohinteractive.seedv6.search.manage.SearchLimits;
import com.ohinteractive.seedv6.search.manage.TimeManager;
import com.ohinteractive.seedv6.search.tt.TranspositionScores;

/** EDT-confined controller joining Swing interaction to authoritative V6 facilities. */
final class GameController implements BoardPanel.InputListener, SearchGateway.Listener {

    enum GameMode {
        HUMAN_VS_ENGINE("Human vs Engine"),
        ENGINE_VS_ENGINE("Engine vs Engine"),
        HUMAN_VS_HUMAN("Human vs Human");

        private final String label;

        GameMode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    enum HumanSide {
        WHITE("White", Value.WHITE),
        BLACK("Black", Value.BLACK);

        private final String label;
        private final int player;

        HumanSide(String label, int player) {
            this.label = label;
            this.player = player;
        }

        int player() {
            return player;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    enum LimitKind {
        DEPTH("Depth"),
        MOVETIME("Movetime");

        private final String label;

        LimitKind(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    record SearchSettings(LimitKind kind, int depth, long movetimeMillis) {
        SearchSettings {
            Objects.requireNonNull(kind, "kind");
            if(depth < 1 || depth > 256) {
                throw new IllegalArgumentException("Depth must be in 1..256.");
            }
            if(movetimeMillis < 0L) {
                throw new IllegalArgumentException("Movetime must not be negative.");
            }
        }

        SearchLimits limits() {
            return kind == LimitKind.DEPTH
                ? new SearchLimits(depth, SearchLimits.NO_LIMIT, SearchLimits.NO_LIMIT, false)
                : new SearchLimits(
                    SearchLimits.NO_DEPTH, SearchLimits.NO_LIMIT,
                    TimeManager.movetimeBudgetMillis(movetimeMillis), false
                );
        }
    }

    record PositionView(
        long[] board,
        PositionStatus status,
        List<String> moves,
        int selectedSquare,
        int[] legalTargets,
        int lastFrom,
        int lastTo,
        int checkedKingSquare
    ) {
        PositionView {
            board = board.clone();
            moves = List.copyOf(moves);
            legalTargets = legalTargets.clone();
        }

        @Override
        public long[] board() {
            return board.clone();
        }

        @Override
        public int[] legalTargets() {
            return legalTargets.clone();
        }
    }

    record SearchInfo(
        String state,
        int depth,
        String score,
        long nodes,
        long nps,
        String pv,
        String termination
    ) {
        static SearchInfo idle() {
            return new SearchInfo("Idle", 0, "—", 0L, -1L, "", "—");
        }

        static SearchInfo thinking() {
            return new SearchInfo("Thinking", 0, "—", 0L, -1L, "", "—");
        }

        static SearchInfo fromIteration(IterationSnapshot snapshot) {
            return new SearchInfo(
                "Thinking", snapshot.depth(), formatScore(snapshot.score()),
                snapshot.nodes(), snapshot.nps(), formatPv(snapshot.principalVariation()), "—"
            );
        }

        static SearchInfo fromFinal(ManagedSearchResult result, SearchInfo previous) {
            final SearchResult completed = result.lastCompletedResult();
            final int depth = completed == null ? previous.depth : completed.depth();
            final String score = completed == null ? previous.score : formatScore(completed.score());
            final String pv = completed == null
                ? previous.pv : formatPv(completed.principalVariation());
            return new SearchInfo(
                result.failure() == null ? "Idle" : "Failed",
                depth, score, result.nodes(), previous.nps, pv,
                result.termination().name()
            );
        }

        private static String formatScore(int score) {
            if(!TranspositionScores.isMateScore(score)) return "cp " + score;
            final int pliesToMate = TranspositionScores.MATE_SCORE - Math.abs(score);
            final int movesToMate = (pliesToMate + 1) / 2;
            return "mate " + (score < 0 ? -movesToMate : movesToMate);
        }

        private static String formatPv(long[] moves) {
            final StringBuilder value = new StringBuilder();
            for(long move : moves) {
                if(!value.isEmpty()) value.append(' ');
                value.append(Move.coordinate(move));
            }
            return value.toString();
        }
    }

    interface View {
        void showPosition(PositionView position);
        void showSearch(SearchInfo search);
        void setSearchRunning(boolean running);
        Promotion choosePromotion(List<Promotion> choices);
        void showError(String title, String message);
    }

    GameController(SearchGateway search, View view) {
        requireEdt();
        this.search = Objects.requireNonNull(search, "search");
        this.view = Objects.requireNonNull(view, "view");
    }

    void initialize() {
        requireEdt();
        publishPosition();
        view.showSearch(searchInfo);
        view.setSearchRunning(false);
        startEngineIfNeeded();
    }

    void newGame() {
        requireEdt();
        ensureOpen();
        search.invalidate(SearchTermination.NEW_GAME);
        activeToken = null;
        positionRevision ++;
        session = GameSession.startingPosition();
        selfPlayContinuous = mode == GameMode.ENGINE_VS_ENGINE;
        clearSelection();
        searchInfo = SearchInfo.idle();
        publishPosition();
        view.showSearch(searchInfo);
        view.setSearchRunning(false);
        startEngineIfNeeded();
    }

    boolean loadFen(String fen) {
        requireEdt();
        ensureOpen();
        final GameSession candidate;
        try {
            candidate = GameSession.fromFen(fen);
        } catch(RuntimeException exception) {
            view.showError("Invalid FEN", exception.getMessage());
            return false;
        }
        search.invalidate(SearchTermination.POSITION_CHANGED);
        activeToken = null;
        positionRevision ++;
        session = candidate;
        selfPlayContinuous = mode == GameMode.ENGINE_VS_ENGINE;
        clearSelection();
        searchInfo = SearchInfo.idle();
        publishPosition();
        view.showSearch(searchInfo);
        view.setSearchRunning(false);
        startEngineIfNeeded();
        return true;
    }

    void setGameMode(GameMode requestedMode) {
        requireEdt();
        ensureOpen();
        Objects.requireNonNull(requestedMode, "requestedMode");
        if(mode == requestedMode) return;
        search.invalidate(SearchTermination.POSITION_CHANGED);
        activeToken = null;
        mode = requestedMode;
        selfPlayContinuous = mode == GameMode.ENGINE_VS_ENGINE;
        clearSelection();
        view.setSearchRunning(false);
        publishPosition();
        startEngineIfNeeded();
    }

    void setHumanSide(HumanSide requestedSide) {
        requireEdt();
        ensureOpen();
        Objects.requireNonNull(requestedSide, "requestedSide");
        if(humanSide == requestedSide) return;
        search.invalidate(SearchTermination.POSITION_CHANGED);
        activeToken = null;
        humanSide = requestedSide;
        clearSelection();
        view.setSearchRunning(false);
        publishPosition();
        startEngineIfNeeded();
    }

    void setSearchSettings(SearchSettings requestedSettings) {
        requireEdt();
        ensureOpen();
        Objects.requireNonNull(requestedSettings, "requestedSettings");
        if(search.isSearching()) {
            view.showError("Search active", "Search limits can be changed when the current search finishes or is invalidated.");
            return;
        }
        settings = requestedSettings;
    }

    void setWorkerCount(int requestedWorkers) {
        requireEdt();
        ensureOpen();
        if(search.isSearching()) {
            view.showError("Search active", "Threads can be changed when no result is pending.");
            return;
        }
        search.replaceWorkerCount(requestedWorkers);
        searchInfo = new SearchInfo(
            "Idle — Threads " + search.workerCount(), 0, "—", 0L, -1L, "", "—"
        );
        view.showSearch(searchInfo);
    }

    void stopSearch() {
        requireEdt();
        ensureOpen();
        if(!search.isSearching()) return;
        if(mode == GameMode.ENGINE_VS_ENGINE) selfPlayContinuous = false;
        search.stop();
        searchInfo = new SearchInfo(
            "Stopping", searchInfo.depth, searchInfo.score, searchInfo.nodes,
            searchInfo.nps, searchInfo.pv, searchInfo.termination
        );
        view.showSearch(searchInfo);
    }

    Runnable beginShutdown() {
        requireEdt();
        if(closing) return () -> {};
        closing = true;
        activeToken = null;
        clearSelection();
        return search.beginShutdown();
    }

    @Override
    public void squarePressed(int square) {
        requireEdt();
        if(!canHumanMove()) return;
        if(selectedSquare >= 0 && contains(legalTargets, square)) return;
        selectSource(square);
    }

    @Override
    public void squareReleased(int square) {
        requireEdt();
        if(!canHumanMove() || selectedSquare < 0) return;
        if(square == selectedSquare) return;
        if(!contains(legalTargets, square)) {
            selectSource(square);
            return;
        }

        final List<Promotion> promotions = session.promotionChoices(selectedSquare, square);
        final Promotion promotion;
        if(promotions.isEmpty()) {
            promotion = Promotion.NONE;
        } else {
            promotion = view.choosePromotion(promotions);
            if(promotion == null) return;
        }

        try {
            session.applyIntent(selectedSquare, square, promotion);
        } catch(RuntimeException exception) {
            view.showError("Illegal move", exception.getMessage());
            publishPosition();
            return;
        }
        positionRevision ++;
        clearSelection();
        publishPosition();
        startEngineIfNeeded();
    }

    @Override
    public void onIteration(Object uiToken, IterationSnapshot snapshot) {
        requireEdt();
        if(!accepts(uiToken)) return;
        searchInfo = SearchInfo.fromIteration(snapshot);
        view.showSearch(searchInfo);
    }

    @Override
    public void onComplete(Object uiToken, ManagedSearchResult result) {
        requireEdt();
        if(!accepts(uiToken)) return;
        activeToken = null;
        view.setSearchRunning(false);
        searchInfo = SearchInfo.fromFinal(result, searchInfo);
        view.showSearch(searchInfo);

        if(result.failure() != null || result.termination() == SearchTermination.FAILURE) {
            view.showError("Engine search failed", result.failure() == null
                ? "Search failed." : result.failure().getMessage());
            return;
        }
        if(!result.hasMove()) {
            publishPosition();
            return;
        }
        if(session.status().terminal() || !isEngineControlledSide()) return;
        if(!isApplicableTermination(result.termination())) return;

        try {
            session.applyGeneratedMove(result.bestMove());
        } catch(RuntimeException exception) {
            view.showError("Rejected engine result", exception.getMessage());
            return;
        }
        positionRevision ++;
        clearSelection();
        publishPosition();
        if(result.termination() != SearchTermination.STOPPED) startEngineIfNeeded();
    }

    long[] boardSnapshot() {
        requireEdt();
        return session.boardSnapshot();
    }

    int historySize() {
        requireEdt();
        return session.historySize();
    }

    PositionStatus positionStatus() {
        requireEdt();
        return session.status();
    }

    List<String> displayedMoves() {
        requireEdt();
        return session.moveHistory();
    }

    private final SearchGateway search;
    private final View view;
    private GameSession session = GameSession.startingPosition();
    private GameMode mode = GameMode.HUMAN_VS_ENGINE;
    private HumanSide humanSide = HumanSide.WHITE;
    private SearchSettings settings = new SearchSettings(LimitKind.DEPTH, 4, 1_000L);
    private SearchInfo searchInfo = SearchInfo.idle();
    private Object activeToken;
    private long positionRevision;
    private int selectedSquare = -1;
    private int[] legalTargets = new int[0];
    private boolean selfPlayContinuous;
    private boolean closing;

    private void selectSource(int square) {
        final int[] destinations = session.legalDestinations(square);
        if(destinations.length == 0) {
            clearSelection();
        } else {
            selectedSquare = square;
            legalTargets = destinations;
        }
        publishPosition();
    }

    private void clearSelection() {
        selectedSquare = -1;
        legalTargets = new int[0];
    }

    private void publishPosition() {
        final PositionStatus status = session.status();
        view.showPosition(new PositionView(
            session.boardSnapshot(), status, session.moveHistory(),
            selectedSquare, legalTargets, session.lastFrom(), session.lastTo(),
            status.checkedKingSquare()
        ));
        if(status.terminal() && !search.isSearching()) {
            searchInfo = new SearchInfo(
                "Terminal", searchInfo.depth, searchInfo.score, searchInfo.nodes,
                searchInfo.nps, searchInfo.pv, searchInfo.termination
            );
            view.showSearch(searchInfo);
        }
    }

    private void startEngineIfNeeded() {
        if(closing || !shouldStartEngine()) return;
        final RequestIdentity token = new RequestIdentity(
            positionRevision, mode, session.status().sideToMove()
        );
        activeToken = token;
        searchInfo = SearchInfo.thinking();
        view.showSearch(searchInfo);
        view.setSearchRunning(true);
        try {
            search.start(
                session.boardSnapshot(), session.historySnapshot(), settings.limits(),
                token, this
            );
        } catch(RuntimeException exception) {
            activeToken = null;
            view.setSearchRunning(false);
            searchInfo = new SearchInfo("Failed", 0, "—", 0L, -1L, "", "FAILURE");
            view.showSearch(searchInfo);
            view.showError("Unable to start search", exception.getMessage());
        }
    }

    private boolean shouldStartEngine() {
        return !session.status().terminal()
            && !search.isSearching()
            && isEngineControlledSide()
            && (mode != GameMode.ENGINE_VS_ENGINE || selfPlayContinuous);
    }

    private boolean isEngineControlledSide() {
        return switch(mode) {
            case HUMAN_VS_ENGINE -> session.status().sideToMove() != humanSide.player();
            case ENGINE_VS_ENGINE -> true;
            case HUMAN_VS_HUMAN -> false;
        };
    }

    private boolean canHumanMove() {
        if(closing || session.status().terminal() || search.isSearching()) return false;
        return mode == GameMode.HUMAN_VS_HUMAN
            || (mode == GameMode.HUMAN_VS_ENGINE
                && session.status().sideToMove() == humanSide.player());
    }

    private boolean accepts(Object uiToken) {
        if(closing || uiToken == null || uiToken != activeToken) return false;
        final RequestIdentity identity = (RequestIdentity) uiToken;
        return identity.positionRevision == positionRevision
            && identity.mode == mode
            && identity.sideToMove == session.status().sideToMove();
    }

    private void ensureOpen() {
        if(closing) throw new IllegalStateException("Controller is closing.");
    }

    private static boolean isApplicableTermination(SearchTermination termination) {
        return termination == SearchTermination.COMPLETED
            || termination == SearchTermination.NODE_LIMIT
            || termination == SearchTermination.TIME_LIMIT
            || termination == SearchTermination.STOPPED;
    }

    private static boolean contains(int[] values, int expected) {
        return Arrays.stream(values).anyMatch(value -> value == expected);
    }

    private static void requireEdt() {
        if(!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Game controller access must occur on the EDT.");
        }
    }

    private record RequestIdentity(long positionRevision, GameMode mode, int sideToMove) {}
}
