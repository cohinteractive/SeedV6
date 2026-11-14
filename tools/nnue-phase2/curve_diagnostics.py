"""Retain matched epoch loss curves and a midpoint diagnostic before interpreting failures."""
import json
from pathlib import Path
import re
import subprocess
import sys
from bootstrap import ROOT, BASE, write, sha
sys.path.insert(0,str(ROOT/'tools/conventional-nnue'))
import compute_gate


def main():
    compute_gate.require();curves={};diagnostics={}
    for name in ['replica','teacher','wdl50','narrow','late','deep16','pawnpairs']:
        stem='a001-'+name if name in ['replica','teacher','wdl50','narrow','late'] else 'b-'+name
        values=[];seconds=0.
        for stage in ['pilot','64']:
            for verified in sorted((BASE/f'raw/{stem}-{stage}').glob('segments/*/verified.json')):
                config=json.loads((verified.parent/'config.json').read_text());step=config['end_superbatch']
                text=re.sub(r'\x1b\[[0-9;]*[A-Za-z]','',(verified.parent/'console.log').read_text(encoding='utf-8'))
                losses=re.findall(r'running loss\s+([0-9.]+)',text)
                if not losses:raise ValueError('Missing reported training loss')
                seconds+=json.loads(verified.read_text())['wall_seconds']
                values.append(dict(superbatch=step,loss=float(losses[-1]),config_sha256=sha(verified.parent/'config.json')))
        values.sort(key=lambda v:v['superbatch']);assert [v['superbatch'] for v in values]==list(range(1,65))
        epochs=[sum(v['loss'] for v in values[i:i+16])/16 for i in range(0,64,16)]
        curves[name]=dict(superbatches=values,reported_16_superbatch_means=epochs,native_wall_seconds_upper_bound=seconds,
                          note='Losses with different WDL targets are not directly comparable; narrow data has two passes per16 superbatches, broad has one.')
        if name not in ['deep16','pawnpairs']:continue
        paths=list((BASE/f'raw/b-{name}-64').glob('segments/000032-*/run/seed-phase2-32'))
        assert len(paths)==1;output=BASE/f'raw/b-{name}-32-diagnostic'
        with (BASE/f'raw/b-{name}-32-diagnostic.log').open('xb') as log:
            subprocess.run([sys.executable,'-B',str(ROOT/'tools/nnue-phase2/assess_architecture.py'),str(paths[0]),str(output),'--pilot'],
                           stdout=log,stderr=subprocess.STDOUT,check=True,timeout=180)
        midpoint=json.loads((output/'summary.json').read_text());endpoint=json.loads((BASE/f'b-{name}-64-assessment.json').read_text())
        assert midpoint['parity']['status']=='PASS'
        diagnostics[name]=dict(midpoint=midpoint,endpoint_validation=endpoint['validation'],
            interpretation='Midpoint diagnostics assess training adequacy; selected initial strength endpoint remains64. No Elo inference or result-selected checkpoint.')
    result=dict(curves=curves,midpoint_diagnostics=diagnostics,
        policy='Before first Phase2 game, inspect optimization trajectory. Any continuation/revised recipe requires a separately fixed endpoint; these diagnostics cannot establish strength.')
    write(BASE/'training-curves.json',result)
    print(json.dumps({name:dict(loss_means=row['reported_16_superbatch_means'],seconds=row['native_wall_seconds_upper_bound']) for name,row in curves.items()},indent=2))


if __name__=='__main__':main()
