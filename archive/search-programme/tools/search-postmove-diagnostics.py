"""SR-001I: extend H's guarded build-only diagnostic copy; never mutate shipped Search.
The child supplies its naturally evaluated stand-pat or authoritative terminal reason.
No extra evaluator call, pruning decision or Search control-flow change is introduced.
"""
import importlib.util
from pathlib import Path
import sys
sys.dont_write_bytecode = True

spec = importlib.util.spec_from_file_location('delta', Path(__file__).with_name('search-delta-diagnostics.py'))
delta = importlib.util.module_from_spec(spec)
spec.loader.exec_module(delta)
source = delta.instrumented_source()

def replace(before, after):
    global source
    if source.count(before) != 1:
        raise RuntimeError('SR-001I integration point changed: ' + before)
    source = source.replace(before, after)

replace('    private QuiescenceDeltaEvidence deltaEvidence;',
        '    private QuiescencePostMoveEvidence postEvidence;\n    private QuiescenceDeltaEvidence deltaEvidence;')
replace('    static ExactSearch deltaEvidenceResearch(', '''    static ExactSearch postMoveEvidenceResearch(ExactEvaluator evaluator, QuiescencePostMoveEvidence evidence) {
        ExactSearch search = deltaEvidenceResearch(evaluator, evidence.base);
        search.postEvidence = evidence;
        return search;
    }

    static ExactSearch deltaEvidenceResearch(''')
replace('        if(deltaEvidence != null) deltaEvidence.reset();',
        '        if(deltaEvidence != null) deltaEvidence.reset();\n        if(postEvidence != null) postEvidence.reset();')
replace('        if(deltaEvidence != null) deltaEvidence.enter(ply, qply, nodes);',
        '        if(deltaEvidence != null) deltaEvidence.enter(ply, qply, nodes);\n        if(postEvidence != null) postEvidence.enter(ply);')
for name in ['MATE', 'STALEMATE']:
    anchor = '                if(deltaEvidence != null) deltaEvidence.cause[ply] = QuiescenceDeltaEvidence.' + name + ';'
    replace(anchor, anchor + '\n                if(postEvidence != null) postEvidence.kind[ply] = QuiescenceDeltaEvidence.' + name + ';')
replace('''                default -> QuiescenceDeltaEvidence.NONE;
            };
            return 0;''', '''                default -> QuiescenceDeltaEvidence.NONE;
            };
            if(postEvidence != null) postEvidence.kind[ply] = deltaEvidence.cause[ply];
            return 0;''')
# Exactly one qsearch static call: use the following qsearch-specific comment as anchor.
replace('''            // SR-001C: nominal qhorizon, after legal-existence/draw resolution.''',
        '''            if(postEvidence != null) postEvidence.evaluated(ply, best, beta);
            // SR-001C: nominal qhorizon, after legal-existence/draw resolution.''')
replace('                deltaEvidence.previous[ply+1] = move;',
        '                deltaEvidence.previous[ply+1] = move;\n                if(postEvidence != null) postEvidence.before(observation, nodes);')
replace('            if(deltaEvidence != null) deltaEvidence.returned(observation, ply, score, best, alpha, beta);',
        '            if(deltaEvidence != null) deltaEvidence.returned(observation, ply, score, best, alpha, beta);\n            if(postEvidence != null) postEvidence.returned(observation, ply, nodes);')

delta.launch(source, 'sr001i', 'QuiescencePostMoveCorpus', '*Quiescence*EvidenceTest',
             ['sr001h.instrumented', 'sr001i.instrumented'], sys.argv[1:])
