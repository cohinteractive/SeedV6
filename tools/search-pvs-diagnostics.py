"""Untimed SR-003 shadow instrumentation; production source/classes stay unchanged.

Run after :app:compileTestJava. Exact substitutions fail closed if call sites move.
The uninstrumented run must have identical scores, moves, PVs and nodes.
"""
from pathlib import Path
import os
import shutil
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
relative = Path('com/ohinteractive/seedv6/search/exact/ExactSearch.java')
source = (root / 'app/src/main/java' / relative).read_text(encoding='utf-8')

def insert(anchor, replacement, count=1):
    global source
    if source.count(anchor) != count:
        raise RuntimeError('Diagnostic integration point changed: ' + anchor)
    source = source.replace(anchor, replacement)

insert('        nodes++;', '        nodes++;\n        PvsDiagnostics.enter(depth, ply, alpha, beta, boards[ply][Board.KEY]);')
insert('                    fullSearch = false;',
       '                    fullSearch = false;\n                    PvsDiagnostics.scout(depth, ply, i, alpha, beta);')
anchor = '                    score = -negamax(depth - 1, ply + 1, -alpha - 1, -alpha, move);'
insert(anchor, anchor + '\n                    PvsDiagnostics.outcome(depth, ply, alpha, beta, score);')
insert('                        fullSearch = true;',
       '                        fullSearch = true;\n                        PvsDiagnostics.full(depth, ply, i, alpha, beta, true);')
anchor = '                        score = -negamax(depth - 1, ply + 1, -beta, -alpha, move);'
insert(anchor, '                        try {\n    ' + anchor + '\n                        } finally { PvsDiagnostics.researched(); }')
anchor = '                    score = -negamax(depth - 1, ply + 1, -beta, -alpha, move);'
# Exact indentation also occurs inside the re-search line; target the enclosing else.
insert('                } else {\n' + anchor,
       '                } else {\n                    PvsDiagnostics.full(depth, ply, i, alpha, beta, false);\n' + anchor)
insert('            if(score >= beta) {',
       '            if(score >= beta) {\n                PvsDiagnostics.cutoff(depth, ply, i);')
insert('        int type = score <= alpha ? TTable.TYPE_UPPER : score >= beta ? TTable.TYPE_LOWER : TTable.TYPE_EXACT;',
       '        PvsDiagnostics.store(ply, alpha, beta, score);\n'
       '        int type = score <= alpha ? TTable.TYPE_UPPER : score >= beta ? TTable.TYPE_LOWER : TTable.TYPE_EXACT;')

output = root / 'build/sr003-pvs-instrumented'
java = output / 'src' / relative
java.parent.mkdir(parents=True, exist_ok=True)
java.write_text(source, encoding='utf-8')
classes = output / 'classes'
classes.mkdir(parents=True, exist_ok=True)
classpath = os.pathsep.join(str(root / p) for p in ['app/build/classes/java/main', 'app/build/classes/java/test'])
subprocess.run([shutil.which('javac'), '-cp', classpath, '-d', str(classes), str(java)], check=True)
main = 'com.ohinteractive.seedv6.search.exact.PvsDiagnostics'
common = [shutil.which('java'), '-Xms256m', '-Xmx256m', '-Xbatch']
plain = subprocess.run([*common, '-cp', classpath, main, *sys.argv[1:]], check=True, capture_output=True, text=True)
shadow = subprocess.run([*common, '-Dsr003.instrumented=true', '-cp', str(classes) + os.pathsep + classpath,
                         main, *sys.argv[1:]], check=True, capture_output=True, text=True)
semantic = lambda output: [line for line in output.splitlines() if line.startswith('result ')]
if semantic(plain.stdout) != semantic(shadow.stdout):
    raise RuntimeError('Instrumentation changed Search semantics/tree')
print('plain/shadow score, best, PV and node identity verified; all counters UNTIMED')
print(shadow.stdout, end='')
