"""Reproduce the isolated IC root-job observation copy from exact build96 source.
No production source is edited. New outputs only; --check verifies existing bytes.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
SOURCE = 'engine-core/src/main/java/com/ohinteractive/seedv6/search/exact/ParallelSearch.java'
TARGET = 'research/src/main/java/com/ohinteractive/seedv6/search/exact/P4ObservedRootSearch.java'
BASE = '44fc2bf2fe54e87dee7721bdaa451f3b00f3b1d0'


def generate():
    original = subprocess.check_output(['git', 'show', BASE + ':' + SOURCE], cwd=ROOT).decode('utf-8')
    assert (ROOT / SOURCE).read_text() == original, 'Current coordinator differs from accepted source'
    source = original.replace('ParallelSearch', 'P4ObservedRootSearch')
    source = source.replace('public final class P4ObservedRootSearch implements SingleDepthSearch {', '''/** Mechanical research-only copy of build96 ParallelSearch. Only class identity and
 * drained root-job observations differ; no observation participates in Search policy.
 * Worker writes are confined to its unique job slot; Future joins publish them. */
public final class P4ObservedRootSearch implements SingleDepthSearch {
    public record RootJobObservation(int job, int invocation, int worker, long move,
            int depth, int alpha, int beta, boolean scout, boolean completed,
            int score, long nodes) {}
    private RootJobObservation[][] rootJobFacts = new RootJobObservation[0][];
    public java.util.List<RootJobObservation> rootJobObservations() {
        ensureIdle();
        var result = new java.util.ArrayList<RootJobObservation>();
        for (var job : rootJobFacts) for (var item : job) if (item != null) result.add(item);
        return java.util.List.copyOf(result);
    }''')
    replacements = {
        '            splitEffort[job]+=nodes-before;': '''            splitEffort[job]+=nodes-before;
            int slot = rootJobFacts[job][0] == null ? 0 : 1;
            if (rootJobFacts[job][slot] != null) throw new IllegalStateException("Unexpected third root invocation");
            rootJobFacts[job][slot] = new RootJobObservation(job, slot, index, splitMoves[job],
                    request.depth(), alpha, beta, scout, result.completed(), result.score(), nodes-before);''',
        '        preferred=0; previousMoves=previousEffort=new long[0];': '''        preferred=0; previousMoves=previousEffort=new long[0];
        rootJobFacts = new RootJobObservation[0][];''',
        '        table.clear();': '''        table.clear();
        rootJobFacts = new RootJobObservation[0][];''',
        '        eagerActive=false;': '''        eagerActive=false;
        rootJobFacts = new RootJobObservation[0][];''',
        '                    splitEffort=new long[splitMoves.length];splitIncomplete.set(false);': '''                    splitEffort=new long[splitMoves.length];splitIncomplete.set(false);
                    rootJobFacts = new RootJobObservation[splitMoves.length][2];'''
    }
    for before, after in replacements.items():
        assert source.count(before) == 1, before
        source = source.replace(before, after)
    return original.encode(), source.encode()


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    original, generated = generate()
    path = ROOT / TARGET
    if args.check:
        assert path.read_bytes() == generated, 'Observation copy changed outside the declared transformation'
    else:
        with path.open('xb') as stream:
            stream.write(generated)
    print(json.dumps(dict(status='verified' if args.check else 'created', base=BASE,
                          source=SOURCE, originalLfSha256=hashlib.sha256(original).hexdigest(),
                          target=TARGET, targetSha256=hashlib.sha256(generated).hexdigest())))
