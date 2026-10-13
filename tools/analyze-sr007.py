"""Independently re-audit frozen SR-007 exploratory screens; never adoption evidence."""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path


def digest(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def analyze(root, protocol_name='screen-protocol-v1.json'):
    root = Path(root)
    protocol = json.loads((root/protocol_name).read_text())
    assert all(digest(p) == h for p,h in protocol['files'].items())
    spec = importlib.util.spec_from_file_location('frozen_baseline',root/protocol['adapter'])
    b = importlib.util.module_from_spec(spec); spec.loader.exec_module(b)
    result = {'status':'passed','type':'exploratory only; no independent strength acceptance',
              'protocolSha256':digest(root/protocol_name),'screens':{}}
    for policy in protocol['policiesInOrder']:
        run_dir = root/(protocol['outputPrefix']+policy)
        run = json.loads((run_dir/'run.json').read_text())
        assert run['status']=='passed' and run['inputsStable']
        args = run['arguments']
        for key,value in {'candidate_internal':policy,'baseline_internal':'off','candidate_reuse':'default','baseline_reuse':'default',
                'pairs':protocol['pairsPerPolicy'],'tc':protocol['tc'],
                'workers':1,'concurrency':4,'seed':protocol['seed'],
                'baseline':protocol['baseline'],'candidate':protocol['candidate'],
                'baseline_qpolicy':'production','candidate_qpolicy':'production',
                'baseline_lmr':'production','candidate_lmr':'production',
                'baseline_lmp':'default','candidate_lmp':'default',
                'baseline_check':'off','candidate_check':'off',
                'baseline_aspiration':'off','candidate_aspiration':'off'}.items():
            if key in ('baseline','candidate'):
                assert Path(args[key]).resolve()==Path(value).resolve(),(policy,key,args[key],value)
            else:
                assert args[key]==value,(policy,key,args[key],value)
        openings = root/protocol['openings']
        assert Path(args['openings']).resolve()==openings.resolve()
        assert all(digest(p)==h for p,h in run['filesBefore'].items())
        for side,source in [('baseline','450988455af3'),('candidate',protocol['source'][:12])]:
            identity = run[side+'Preflight']['identity']
            for fact in ('source='+source,'checkpoint='+b.CHECKPOINT,'sha256='+b.NETWORK_SHA256,
                         'mapping=BRN_PAIR2_MATERIAL_RESIDUAL_QUARTER_V1','workers=1',
                         'leaves=unpruned','lmr=quiet','aspiration=off'):
                assert fact in identity,(policy,side,fact)
            b.check_lmp_identity(identity,'default',reuse='default', internal=policy if side=='candidate' else 'off')
        audit = b.analyze(run_dir/'games.pgn',openings,protocol['pairsPerPolicy'],True,True)
        assert audit['terminations']=={'normal':2*protocol['pairsPerPolicy']}
        for key in ('wdl','statistics','wholeRequestResources','terminations'):
            assert audit[key]==run['results'][key],(policy,key)
        result['screens'][policy]={k:audit[k] for k in ('wdl','statistics','wholeRequestResources','terminations')}
        result['screens'][policy].update(wallSeconds=run['wallSeconds'],
            receiptSha256=digest(run_dir/'run.json'),pgnSha256=digest(run_dir/'games.pgn'))
    result['games']=2*protocol['pairsPerPolicy']*len(result['screens'])
    return result


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('root');parser.add_argument('output')
    parser.add_argument('--protocol',default='screen-protocol-v1.json');args=parser.parse_args()
    output=Path(args.output)
    if output.exists():raise ValueError('New output required')
    result=analyze(args.root,args.protocol)
    output.write_bytes((json.dumps(result,indent=2,allow_nan=False)+'\n').encode())
    for policy,run in result['screens'].items():
        print(policy,run['wdl'],round(run['statistics']['logisticElo'],2),round(run['wallSeconds'],1))
