"""Reproduce IC-C4 adjacent iterations with initial helper depth offsets from exact96."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
SOURCE = 'engine-core/src/main/java/com/ohinteractive/seedv6/search/exact/IndependentSearch.java'
TARGET = 'research/src/main/java/com/ohinteractive/seedv6/search/exact/P4AdjacentOffsetSearch.java'
BASE = '44fc2bf2fe54e87dee7721bdaa451f3b00f3b1d0'


def generate():
    original = subprocess.check_output(['git', 'show', BASE + ':' + SOURCE], cwd=ROOT).decode()
    assert (ROOT / SOURCE).read_text() == original
    source = original.replace('IndependentSearch', 'P4AdjacentOffsetSearch')
    a = source.index('/** MW-R001'); b = source.index('public final class', a)
    source = source[:a] + '''/** IC-C4 Initial helper depth offsets with adjacent subsequent iterations.
 * OWNER starts at1; helpers start at2/3/4 atW4, clamped to requested depth.
 * Every worker then advances by1, preserving existing adjacent aspiration/reuse
 * eligibility after its first completion. No completed depth is manufactured.
 * OWNER remains sole publisher/BOUNDARY2 decision maker. Exact96 shared TT,
 * private learning, global accounting, cancellation and full joins are unchanged.
 * Quiescent first-attempt observation is provenance, never worker confidence. */
''' + source[b:]
    replacements = {
        'int maximumPly, maximumQply, attemptedDepth, completedDepth;':
            'int maximumPly, maximumQply, attemptedDepth, completedDepth, firstAttemptedDepth;',
        'maximumPly=maximumQply=attemptedDepth=completedDepth=0;':
            'maximumPly=maximumQply=attemptedDepth=completedDepth=firstAttemptedDepth=0;',
        'for(int depth=1;depth<=request.depth();depth++) {':
            'for(int depth=Math.min(request.depth(),index+1);depth<=request.depth();depth++) {',
        '                    attemptedDepth=depth;':
            '                    if(firstAttemptedDepth==0) firstAttemptedDepth=depth;\n                    attemptedDepth=depth;',
        'public record WorkerProgress(int worker,long nodes,int attemptedDepth,int completedDepth,boolean incomplete,':
            'public record WorkerProgress(int worker,long nodes,int attemptedDepth,int completedDepth,int firstAttemptedDepth,boolean incomplete,',
        'new WorkerProgress(i,w.nodes,w.attemptedDepth,w.completedDepth,w.incomplete,w.completed)':
            'new WorkerProgress(i,w.nodes,w.attemptedDepth,w.completedDepth,w.firstAttemptedDepth,w.incomplete,w.completed)'
    }
    for before, after in replacements.items():
        assert source.count(before) == 1, before
        source = source.replace(before, after)
    return original.encode(), source.encode()


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true'); args = parser.parse_args()
    original, generated = generate(); path = ROOT / TARGET
    if args.check:
        assert path.read_bytes() == generated
    else:
        with path.open('xb') as stream: stream.write(generated)
    print(json.dumps(dict(status='verified' if args.check else 'created', base=BASE, source=SOURCE, target=TARGET,
                          originalLfSha256=hashlib.sha256(original).hexdigest(), targetSha256=hashlib.sha256(generated).hexdigest())))
