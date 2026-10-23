"""Evidence-backed admission judgment; retain adverse selective interactions."""
import json
from run_observation_v1 import HERE,digest,write_new

matched=json.loads((HERE/'root-witness-v1-summary.json').read_text());reference=json.loads((HERE/'root-reference-v1-summary.json').read_text())
assert matched['status']==reference['status']=='passed'
assert all(x['targetDepthCompleted'] for rows in matched['depths'].values() for x in rows.values())
assert all(x['outcome']['completed'] and not x['outcome']['selective'] and x['reusePassed'] for x in reference['references'])
scores={(x['depth'],x['rootMove']):x['outcome']['score'] for x in reference['references']}
gaps={str(depth):scores[depth,'a4c4']-scores[depth,'a4b4'] for depth in [7,9]}
assert gaps=={'7':80,'9':156}
assert all(matched['depths']['9'][p]['lastCompleted']['bestMove']==722584 for p in ['baseline','nominal-futility','verified-null'])
assert matched['depths']['9']['nominal-verified-null']['lastCompleted']['bestMove']==722520
names=['root-witness-v1.json','root-witness-v1-binding.json','root-witness-v1-summary.json','root-witness-v1-selection.json',
       'root-reference-v1.json','root-reference-v1-binding.json','root-reference-v1-summary.json','root-reference-v1-compile.json',
       'fixed-node-move-witness-v1.json','flat-null-validation-v3-summary.json','flat-null-composition-v2-summary.json','assess_root_witness_v1.py']
write_new(HERE/'root-witness-v1-assessment.json',{
    'status':'completed','decision':'eligible_for_confirmation','correctnessBlocker':False,'knownAdverseSelectiveInteraction':True,
    'evidence':{name:digest(HERE/name) for name in names},'referenceScoreAdvantageA4c4':gaps,
    'observations':['All eight matched-depth requests and all four independent restricted-root references completed within frozen caps.',
        'AtD9 full, nominal alone and null alone choose a4c4; composition chooses a4b4. Independent unpruned reference ranks a4c4 higher atD7/D9.',
        'Every returned PV/legal root identity/root-preservation/reuse check passes. Selective outcomes are labeled selective; independent references use no TT or selective pruning.',
        'The original unequal-horizon explanation is insufficient: same-depth selective disagreement persists, with an adverse composition interaction.'],
    'judgment':'Retain this adverse finite-horizon witness. The reference scores are CP64 restricted-root values, not proof of actual game outcome or full-root optimality. This identifies a selective accuracy tradeoff, not an observed illegal move, false mathematical proof, corrupted history, dishonest completion or ownership violation. Existing correctness/lifecycle and independent cutoff checks remain applicable. Continue the pre-selected complete-configuration clock confirmation without parameter changes; do not claim every active mechanism helps.',
    'limitations':['No universal safety/error-rate claim from earlier96 conditional cutoff replays.','No root-score or depth result is strength confirmation.','The independent reference omits Construction extensions/heuristics by design.','No retuning, extra-budget retry, production promotion or external deployment.']})
print('Adverse interaction retained; confirmation admission has no observed correctness blocker.')
