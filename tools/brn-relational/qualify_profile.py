"""Frozen evaluator/search-profile request-budget qualification, with no chess clocks."""
import argparse
import json
import os
from pathlib import Path
import re
import time

import chess
from baseline import ROOT, Engine, admit, sha
from compute_control import atomic


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('plan',type=Path);args=parser.parse_args()
    plan=json.loads(args.plan.read_text());runtime=ROOT/plan['runtime'];out=ROOT/plan['qualification']
    out.mkdir(parents=True,exist_ok=True);resource=admit(initial=True)
    binding=dict(plan_sha256=sha(args.plan),runtime={str(f.relative_to(runtime)):sha(f) for f in runtime.rglob('*') if f.is_file()},
                 runner_sha256=sha(Path(__file__)),engine_helper_sha256=sha(ROOT/'tools/brn-relational/baseline.py'))
    if (out/'binding.json').exists():
        if json.loads((out/'binding.json').read_text())!=binding:raise ValueError('Qualification binding changed')
    else:atomic(out/'binding.json',binding)
    if (out/'result.json').exists():
        result=json.loads((out/'result.json').read_text())
        if result['status']!='PASS':raise ValueError('Prior qualification failed; inspect attempt')
        print('Verified completed profile qualification');return
    specs={json.dumps(side,sort_keys=True) for c in plan['comparisons'] for side in (c['left'],c['right'])}
    results=[]
    for encoded in sorted(specs):
        spec=json.loads(encoded);model=ROOT/spec['path'];kind=spec.get('evaluator','pair');profile=plan['search_profile']
        if kind not in ('pair','cp64') or profile not in ('common','cp64-profile') or sha(model)!=spec['sha256']:raise ValueError('Model/profile identity')
        command=['C:/java/jdk-21/bin/java.exe','--add-modules=jdk.incubator.vector','-XX:ActiveProcessorCount=1','-XX:+UseSerialGC','-Xmx384m',
                 '-cp',str(runtime/'classes')+os.pathsep+str(runtime/'lib/*'),
                 'com.ohinteractive.seedv6.integration.BrnRelationalUciMain',kind,str(model),profile]
        engine=Engine(command,out/(kind+'-'+str(time.time_ns())+'.log'))
        try:
            identity='\n'.join(engine.identity)
            if spec['sha256'] not in identity or 'profile='+profile not in identity:raise ValueError('Runtime identity')
            if profile=='cp64-profile' and ('NOMINAL_FUTILITY' not in identity or 'VERIFIED_NULL' not in identity):raise ValueError('Missing deployed mechanisms')
            for fen in plan['probe_fens']:
                for budget in (1,100,plan['nodes']):
                    observed=[]
                    for _ in range(2):engine.reset();_,receipt=engine.search(chess.Board(fen),budget);observed.append(receipt)
                    comparable=lambda r:(r['nodes'],r['termination'],r['move'],re.sub(r' time \d+| nps \d+','',r['info'] or ''))
                    if comparable(observed[0])!=comparable(observed[1]):raise ValueError('Nondeterministic whole-request result')
                    results.append(dict(model=spec,fen=fen,budget=budget,repeats=observed))
            admit()
        finally:
            engine.close()
            atomic(out/('release-'+str(time.time_ns())+'.json'),dict(remaining=int(engine.p.poll() is None)))
    atomic(out/'result.json',dict(status='PASS',binding=binding,resource=resource,probes=results))
    print(json.dumps(dict(status='PASS',searches=len(results)*2,profile=plan['search_profile'])))


if __name__=='__main__':main()
