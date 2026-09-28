"""SR-001H untimed shadow instrumentation, following SR-003/SR-017 diagnostics.
Compile test classes first. No production source/class mutation. Each insertion
fails closed when its unique source anchor changes. --test runs focused JUnit
assertions against the shadow class; otherwise runs the diagnostic corpus.
"""
from pathlib import Path
import os, shutil, subprocess, sys
root = Path(__file__).resolve().parents[1]
relative = Path('com/ohinteractive/seedv6/search/exact/ExactSearch.java')
source = (root / 'app/src/main/java' / relative).read_text(encoding='utf-8')
EDITS = [('    private long qnodes;\n'
  '    private int maximumQply;\n'
  '    private boolean active;\n'
  '\n'
  '    public ExactSearch() { this(SearchEvaluation.handcrafted()); }\n'
  '\n',
  '    private long qnodes;\n'
  '    private int maximumQply;\n'
  '    private boolean active;\n'
  '    private QuiescenceDeltaEvidence deltaEvidence; // SR-001H only; absent from every normal/research '
  'selector.\n'
  '\n'
  '    public ExactSearch() { this(SearchEvaluation.handcrafted()); }\n'
  '\n'),
 ('     */\n'
  '    public static ExactSearch quiescenceResearch(ExactEvaluator evaluator) {\n'
  '        return quiescenceResearch(evaluator, QSEARCH_BASELINE);\n'
  '    }\n'
  '\n'
  '    /** Independent SR-001 research alternatives; no combinations or production opt-in. */\n',
  '     */\n'
  '    public static ExactSearch quiescenceResearch(ExactEvaluator evaluator) {\n'
  '        return quiescenceResearch(evaluator, QSEARCH_BASELINE);\n'
  '    }\n'
  '\n'
  '    /** Test/headless evidence seam: always the unchanged unlimited TT-off baseline. */\n'
  '    static ExactSearch deltaEvidenceResearch(ExactEvaluator evaluator, QuiescenceDeltaEvidence evidence) {\n'
  '        ExactSearch search = quiescenceResearch(evaluator);\n'
  '        search.deltaEvidence = Objects.requireNonNull(evidence);\n'
  '        return search;\n'
  '    }\n'
  '\n'
  '    /** Independent SR-001 research alternatives; no combinations or production opt-in. */\n'),
 ('        nodes = 0;\n'
  '        qnodes = 0;\n'
  '        maximumQply = 0;\n'
  '        this.cancelled = cancelled;\n'
  '        try {\n'
  '            // Invocation-local history, even within one driver request. Broader lifecycle remains OPEN.\n',
  '        nodes = 0;\n'
  '        qnodes = 0;\n'
  '        maximumQply = 0;\n'
  '        if(deltaEvidence != null) deltaEvidence.reset();\n'
  '        this.cancelled = cancelled;\n'
  '        try {\n'
  '            // Invocation-local history, even within one driver request. Broader lifecycle remains OPEN.\n'),
 ('        checkpoint();\n'
  '        nodes++;\n'
  '        qnodes++;\n'
  '        maximumQply = Math.max(maximumQply, qply);\n'
  '        if(quietCheckRuns != null && qply == 0) quietCheckRuns[ply] = 0;\n'
  '        pvLength[ply] = 0;\n',
  '        checkpoint();\n'
  '        nodes++;\n'
  '        qnodes++;\n'
  '        if(deltaEvidence != null) deltaEvidence.enter(ply, qply, nodes);\n'
  '        maximumQply = Math.max(maximumQply, qply);\n'
  '        if(quietCheckRuns != null && qply == 0) quietCheckRuns[ply] = 0;\n'
  '        pvLength[ply] = 0;\n'),
 ('        // Quiets establish existence; SR-001E may later consume their checking subset.\n'
  '        int quietCount = -1;\n'
  '        if(count == 0) {\n'
  '            if(checked) return -MATE_SCORE + ply;\n'
  '            quietCount = Gen.genQuiet(board[0], board[1], board[2], board[3], status,\n'
  '                    board[Board.KEY], true, quietScratch, generatorScratch);\n'
  '            if(quietCount == 0) return 0;\n'
  '        }\n'
  '        if(DrawAdjudicator.adjudicateNonTerminal(board, history) != DrawAdjudicator.RuleDraw.NONE) return 0;\n'
  '        final boolean allowQuietChecks = quietChecks && (quietCheckLimit < 0 ? qply == 0 : quietChecksUsed < '
  'quietCheckLimit);\n'
  '        if(quietCheckCounters != null && !checked) {\n'
  '            // Diagnostic run only: count existence even when stand-pat/tacticals will cut.\n',
  '        // Quiets establish existence; SR-001E may later consume their checking subset.\n'
  '        int quietCount = -1;\n'
  '        if(count == 0) {\n'
  '            if(checked) {\n'
  '                if(deltaEvidence != null) deltaEvidence.cause[ply] = QuiescenceDeltaEvidence.MATE;\n'
  '                return -MATE_SCORE + ply;\n'
  '            }\n'
  '            quietCount = Gen.genQuiet(board[0], board[1], board[2], board[3], status,\n'
  '                    board[Board.KEY], true, quietScratch, generatorScratch);\n'
  '            if(quietCount == 0) {\n'
  '                if(deltaEvidence != null) deltaEvidence.cause[ply] = QuiescenceDeltaEvidence.STALEMATE;\n'
  '                return 0;\n'
  '            }\n'
  '        }\n'
  '        DrawAdjudicator.RuleDraw qdraw = DrawAdjudicator.adjudicateNonTerminal(board, history);\n'
  '        if(qdraw != DrawAdjudicator.RuleDraw.NONE) {\n'
  '            if(deltaEvidence != null) deltaEvidence.cause[ply] = switch(qdraw) {\n'
  '                case FIFTY_MOVE -> QuiescenceDeltaEvidence.RULE50;\n'
  '                case FORMAL_THREEFOLD -> QuiescenceDeltaEvidence.REPETITION;\n'
  '                case INSUFFICIENT_MATERIAL -> QuiescenceDeltaEvidence.INSUFFICIENT;\n'
  '                default -> QuiescenceDeltaEvidence.NONE;\n'
  '            };\n'
  '            return 0;\n'
  '        }\n'
  '        final boolean allowQuietChecks = quietChecks && (quietCheckLimit < 0 ? qply == 0 : quietChecksUsed < '
  'quietCheckLimit);\n'
  '        if(quietCheckCounters != null && !checked) {\n'
  '            // Diagnostic run only: count existence even when stand-pat/tacticals will cut.\n'),
 ('                return qcompleted(key, ply, originalAlpha, originalBeta, best, 0);\n'
  '            if(best > alpha) alpha = best;\n'
  '        }\n'
  '        // Existing absolute storage/mate-domain boundary, NOT a score-producing qdepth cap.\n'
  '        // Resolved terminals and stand-pat proofs above are valid; unresolved move search\n'
  '        // at the last slot must unwind through the ordinary incomplete-result path.\n',
  '                return qcompleted(key, ply, originalAlpha, originalBeta, best, 0);\n'
  '            if(best > alpha) alpha = best;\n'
  '        }\n'
  '        final int standPat = best;\n'
  '        // Existing absolute storage/mate-domain boundary, NOT a score-producing qdepth cap.\n'
  '        // Resolved terminals and stand-pat proofs above are valid; unresolved move search\n'
  '        // at the last slot must unwind through the ordinary incomplete-result path.\n'),
 ('            long childChecking = suspect || quietCheck ? checkers(child, (int) child[Board.STATUS]) : 0;\n'
  '            if((suspect || quietCheck) && childChecking == 0) continue;\n'
  '            if(quietCheck && quietCheckReplies != 0 && !forcingCheckEligible(child, qply, childChecking)) '
  'continue;\n'
  '            if(quietCheck && forcingCounters != null)\n'
  '                forcingCounters[9] = Math.max(forcingCounters[9], quietChecksUsed + 1);\n'
  '            if(quietCheckCounters != null) {\n',
  '            long childChecking = suspect || quietCheck ? checkers(child, (int) child[Board.STATUS]) : 0;\n'
  '            if((suspect || quietCheck) && childChecking == 0) continue;\n'
  '            if(quietCheck && quietCheckReplies != 0 && !forcingCheckEligible(child, qply, childChecking)) '
  'continue;\n'
  '            int observation = -1;\n'
  '            if(deltaEvidence != null) {\n'
  '                // Observe only non-checking, non-promotion captures at non-check qnodes.\n'
  '                // Extra child check detection is diagnostic work; it never filters a move.\n'
  '                if(!checked && ((move >>> Board.PROMOTE_PIECE_SHIFT) & Board.PIECE_BITS) == 0\n'
  '                        && checkers(child, (int)child[Board.STATUS]) == 0) {\n'
  '                    observation = deltaEvidence.start(board, move, ply, qply, alpha, beta, standPat,\n'
  '                            tacticalMaterialValue(move, Board.enPassantSquare(status)), '
  'selectionKeys[keyBase+i] >>> 25);\n'
  '                }\n'
  '                deltaEvidence.previous[ply+1] = move;\n'
  '            }\n'
  '            if(quietCheck && forcingCounters != null)\n'
  '                forcingCounters[9] = Math.max(forcingCounters[9], quietChecksUsed + 1);\n'
  '            if(quietCheckCounters != null) {\n'),
 ('            int score;\n'
  '            try { score = -quiescence(ply + 1, qply + 1, -beta, -alpha, quietChecksUsed + (quietCheck ? 1 : '
  '0)); }\n'
  '            finally { history.popRealPosition(); }\n'
  '            if(score > best) {\n'
  '                best = score;\n'
  '                pv[ply][0] = move;\n',
  '            int score;\n'
  '            try { score = -quiescence(ply + 1, qply + 1, -beta, -alpha, quietChecksUsed + (quietCheck ? 1 : '
  '0)); }\n'
  '            finally { history.popRealPosition(); }\n'
  '            if(deltaEvidence != null) deltaEvidence.returned(observation, ply, score, best, alpha, beta);\n'
  '            if(score > best) {\n'
  '                best = score;\n'
  '                pv[ply][0] = move;\n'),
 ('    }\n'
  '\n'
  '    private int qcompleted(long key, int ply, int alpha, int beta, int score, long move) {\n'
  '        if(qtable != null) {\n'
  '            checkpoint(); // No evidence from an incomplete node, including final-child cancellation.\n'
  '            int type = score <= alpha ? TTable.TYPE_UPPER : score >= beta ? TTable.TYPE_LOWER : '
  'TTable.TYPE_EXACT;\n',
  '    }\n'
  '\n'
  '    private int qcompleted(long key, int ply, int alpha, int beta, int score, long move) {\n'
  '        if(deltaEvidence != null) deltaEvidence.completed(ply, score, move);\n'
  '        if(qtable != null) {\n'
  '            checkpoint(); // No evidence from an incomplete node, including final-child cancellation.\n'
  '            int type = score <= alpha ? TTable.TYPE_UPPER : score >= beta ? TTable.TYPE_LOWER : '
  'TTable.TYPE_EXACT;\n')]
for before, after in EDITS:
    if source.count(before) != 1:
        raise RuntimeError('SR-001H diagnostic integration point changed: ' + before)
    source = source.replace(before, after)
output = root / 'build/sr001h-instrumented'
java = output / 'src' / relative
java.parent.mkdir(parents=True, exist_ok=True)
java.write_text(source, encoding='utf-8')
classes = output / 'classes'
classes.mkdir(parents=True, exist_ok=True)
classpath = os.pathsep.join(str(root / p) for p in ['app/build/classes/java/main', 'app/build/classes/java/test', 'app/build/resources/main'])
subprocess.run([shutil.which('javac'), '-cp', classpath, '-d', str(classes), str(java)], check=True)
if '--test' in sys.argv:
    init = output / 'test.gradle'
    init.write_text("allprojects { afterEvaluate { tasks.withType(Test).configureEach { classpath = files('" + classes.as_posix() + "') + classpath; systemProperty 'sr001h.instrumented', 'true' } } }", encoding='utf-8')
    environment = dict(os.environ, DEBUG='')
    subprocess.run([str(root / 'gradlew.bat'), ':app:test', '-Pheadless', '--no-configuration-cache', '-I', str(init), '--tests', '*QuiescenceDeltaEvidenceTest'], cwd=root, env=environment, check=True)
else:
    subprocess.run([shutil.which('java'), '-Xms256m', '-Xmx256m', '-Xbatch', '-cp', str(classes) + os.pathsep + classpath,
        'com.ohinteractive.seedv6.search.exact.QuiescenceDeltaCorpus', *sys.argv[1:]], cwd=root, check=True)
