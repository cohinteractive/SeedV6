"""Candidate routing and unchanged predecessor checks against frozen artifacts."""
import argparse
import importlib.util
import json
from pathlib import Path
import sys

sys.dont_write_bytecode=True
spec=importlib.util.spec_from_file_location('p4_memory',Path(__file__).with_name('p4-tt-memory.py'))
p4=importlib.util.module_from_spec(spec);spec.loader.exec_module(p4)
b=p4.b


def main(args):
    out=p4.OUT/'raw'/args.name;out.mkdir(exist_ok=False)
    candidate=p4.OUT/'raw'/args.candidate
    profiles={
        'frozen80':(p4.OUT/'raw/runtime80','control','control'),
        'candidateProduction':(candidate,'control','control'),
        'candidateResearchControl':(candidate,'research-control','control'),
        'frozenH1':(p4.OUT/'raw/runtimeh1-v1','h1','h1'),
        'candidateH1':(candidate,'h1','h1'),
        'candidateH2':(candidate,'h2','h2')}
    commands={name:(p4.command(path,'h2')[:-1]+['control'] if launcher=='research-control'
                    else p4.command(path,launcher)) for name,(path,launcher,policy) in profiles.items()}
    frozen={str(Path(p).resolve()):b.digest(p) for p in
            [__file__,p4.__file__,b.__file__,p4.JAVA,p4.MODEL,p4.OUT/args.protocol,p4.OUT/'bridge-v1-openings.pgn']}
    for name in ('runtime80','runtimeh1-v1',args.candidate):
        manifest=p4.OUT/(name+'-manifest.json');frozen[str(manifest.resolve())]=b.digest(manifest)
        values=json.loads(manifest.read_text())['files']
        if any(b.digest(p)!=h for p,h in values.items()):raise ValueError('Frozen artifact changed')
        frozen.update(values)
    positions=['startpos']+['startpos moves '+' '.join(moves) for moves in
                           b.opening_prefixes(p4.OUT/'bridge-v1-openings.pgn',32)[:8]]
    report={'status':'running','protocol':args.protocol,'filesBefore':frozen,'commands':commands,'rows':[],'identities':{}}
    try:
        for name,command in commands.items():
            engine=b.Uci(command,out/(name+'.txt'))
            try:
                for workers in (1,4):
                    report['identities'][f'{name}-w{workers}']=p4.configure(engine,workers,profile=profiles[name][2])
                    requests=[(position,'depth 5') for position in positions] if workers==1 else []
                    requests+=[('startpos','nodes 20000'),('startpos','nodes 100000')]
                    for position,request in requests:
                        board=b.chess.Board()
                        if ' moves ' in position:
                            for move in position.split(' moves ')[1].split():board.push_uci(move)
                        engine.send('ucinewgame');engine.send('isready');engine.until('readyok')
                        engine.send('position '+position);engine.send('go '+request)
                        result=b.inspect_result(board,engine.until('bestmove ',60))
                        if request.startswith('nodes ') and result['nodes']!=int(request.split()[1]):
                            raise ValueError('Whole-request node limit')
                        report['rows'].append(dict(profile=name,workers=workers,position=position,request=request,result=result))
            finally:engine.close()
        for group in (('frozen80','candidateProduction','candidateResearchControl'),('frozenH1','candidateH1')):
            for position,request in [(p,'depth 5') for p in positions]+[('startpos','nodes 20000'),('startpos','nodes 100000')]:
                values=[p4.stable(r['result']) for r in report['rows'] if r['profile'] in group and r['workers']==1
                        and r['position']==position and r['request']==request]
                if len(values)!=len(group) or any(v!=values[0] for v in values):
                    raise ValueError('Unchanged predecessor/routing mismatch: '+str(group)+' / '+position)
        if any(b.digest(p)!=h for p,h in frozen.items()):raise ValueError('Frozen inputs changed')
        report.update(status='passed',inputsStable=True)
    except BaseException as error:
        report.update(status='failed',failure=repr(error));raise
    finally:b.save(out/'observations.json',report)
    summary={'status':'passed','protocol':args.protocol,'candidate':args.candidate,'requests':len(report['rows']),
             'identities':report['identities'],'raw':str(out),'inputsStable':True,
             'interpretation':'All11 W1 outcomes/nodes/PVs match in each unchanged-policy group: frozen80/ordinary/research-control and frozenH1/candidate-H1. H2 is separately qualified for legality/accounting; no H2 equivalence or strength claim. W4 qualifies limits/PVs without asserting scheduling equality.'}
    (p4.OUT/(args.name+'-summary.json')).write_text(json.dumps(summary,indent=2)+'\n',encoding='utf-8',newline='\n')


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    for name in ('name','candidate','protocol'):parser.add_argument('--'+name,required=True)
    main(parser.parse_args())
