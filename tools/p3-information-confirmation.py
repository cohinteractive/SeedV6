"""Phase 3 protocol binding on the existing audited stage executor/statistical rule.

Only complete prospective 128/512-pair looks; no promotion or new statistical
method. Freeze this adapter and its shared dependencies before confirmation.
The initial two contrasts reserve family indices 1 (one worker) and 2 (four).
Later retuned confirmations must prospectively allocate unused family alpha.
"""
import argparse
import importlib.util
import json
from pathlib import Path

spec=importlib.util.spec_from_file_location('p3_shared_confirmation',Path(__file__).with_name('sr032-confirmation.py'))
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
t=m.t
BASELINE_CORE='9b41d6b174846cfaf3af9f3089d8c3d13b0614fbf0a4717a6fe46aec346c61d9'


def verify(path):
    p=t.read(path)
    t.require(p['design']=='p3-information-paired-stages-v1' and p['family']=='P3-O1O2', 'Unknown Phase 3 protocol')
    t.require(p['executorSha256']==t.digest(__file__), 'Confirmation adapter changed')
    required=[__file__,m.__file__,m.rules.__file__,m.t.__file__,m.b.__file__]
    t.require(all(str(Path(f).resolve())in p['frozenFiles']for f in required), 'Unbound shared executor or rule')
    t.require(all(t.digest(f)==sha for f,sha in p['frozenFiles'].items()), 'Frozen evidence changed')
    t.require(p['looks']==[128,512] and type(p['contrastIndex'])is int and p['contrastIndex']in (1,2), 'Unexpected prospective design')
    t.require(p['familyAlpha']==m.rules.FAMILY_ALPHA and p['minimumPairsEachSideForNormal']==m.rules.MIN_EACH_SIDE
              and p['requiredSensitivitySigma']==m.rules.SENSITIVITY_SIGMA
              and p['futilityPowerThreshold']==m.rules.FUTILITY_POWER, 'Statistical rule mismatch')
    t.require([stage['pairs']for stage in p['stages']]==[128,384], 'Wrong cumulative stages')
    t.require(p['expectedAdapter']=='p3-information-match.py', 'Wrong match adapter')
    a=p['expectedArguments'];workers=a['candidate_workers'];policy=int(a['candidate_policy'])
    t.require(a['baseline_policy']=='0' and type(workers)is int and a['baseline_workers']==workers
              and workers==(1 if p['contrastIndex']==1 else 4)
              and 1<=policy<=7 and (workers==1 or policy==4), 'Wrong policy/worker domain')
    t.require(a['tc']=='10+0.1' and a['concurrency']==1 and a['background']is True, 'Wrong clock/resource domain')
    t.require(set(p['runtimeTrees'])=={'baseline','candidate'}, 'Both binary identities required')
    baseline={k.replace('\\','/'):v for k,v in p['runtimeTrees']['baseline'].items()}
    t.require(baseline.get('lib/engine-core.jar')==BASELINE_CORE, 'Wrong pre-experiment champion binary')
    return p


# The shared executor calls this binding before every stage and assessment;
# its lifecycle, PGN audit and frozen SR-024 decision function are unchanged.
m.verify=verify
assess=m.assess
run=m.run


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);sub=parser.add_subparsers(dest='action',required=True)
    execute=sub.add_parser('run');execute.add_argument('--protocol',required=True)
    execute.add_argument('--stop-before',required=True);execute.add_argument('--max-stages',type=int,choices=(1,2),default=2)
    audit=sub.add_parser('assess');audit.add_argument('--protocol',required=True);audit.add_argument('--stages',type=int,required=True)
    a=parser.parse_args()
    print(json.dumps(run(a.protocol,a.stop_before,a.max_stages)if a.action=='run'else assess(a.protocol,a.stages),indent=2))
