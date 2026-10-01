"""Compile an explicitly UNTIMED, instrumented ExactSearch copy under build/.

Production classes/sources are untouched. Exact substitutions fail closed if the
diagnostic integration points change. Run after :app:compileTestJava.
"""
from pathlib import Path
import os
import shutil
import subprocess
import sys

root = Path(__file__).resolve().parents[1]
relative = Path('com/ohinteractive/seedv6/search/exact/ExactSearch.java')
source = (root / 'app/src/main/java' / relative).read_text(encoding='utf-8')
start = source.index('    private int negamax(')
opening = source.index('{', start)
end, nesting = opening + 1, 1
while nesting:
    nesting += (source[end] == '{') - (source[end] == '}')
    end += 1
method = source[start:end]
anchor = '        long[] board = boards[ply];'
assert method.count(anchor) == 1
method = method[:-1].replace(anchor, anchor + '\n        StagedGenerationDiagnostics.enter(depth, ply, board);\n        try {')
method += '} finally { StagedGenerationDiagnostics.exit(ply); }\n    }'
source = source[:start] + method + source[end:]
anchor = '            int score = evaluator.evaluate(board, ply);'
assert source.count(anchor) == 1
source = source.replace(anchor, '            StagedGenerationDiagnostics.staticLeaf();\n' + anchor)
for name, expected in [('genAll', 1), ('genTactical', 1), ('genQuiet', 2), ('genEvasion', 1)]:
    anchor = 'Gen.' + name + '('
    assert source.count(anchor) == expected, (name, 'generation call sites changed')
    source = source.replace(anchor, 'StagedGenerationDiagnostics.' + name + '(')
anchor = 'QuietHistory.recordCutoff('
assert source.count(anchor) == 1
source = source.replace(anchor, 'StagedGenerationDiagnostics.recordCutoff(')
output = root / 'build/sr017-staging-instrumented'
java = output / 'src' / relative
java.parent.mkdir(parents=True, exist_ok=True)
java.write_text(source, encoding='utf-8')
classes = output / 'classes'
classes.mkdir(parents=True, exist_ok=True)
classpath = os.pathsep.join(str(root / p) for p in ['app/build/classes/java/main', 'app/build/classes/java/test'])
subprocess.run([shutil.which('javac'), '-cp', classpath, '-d', str(classes), str(java)], check=True)
subprocess.run([shutil.which('java'), '-Xms256m', '-Xmx256m', '-Xbatch', '-cp', str(classes) + os.pathsep + classpath,
                'com.ohinteractive.seedv6.search.exact.StagedGenerationDiagnostics', *sys.argv[1:]], check=True)
