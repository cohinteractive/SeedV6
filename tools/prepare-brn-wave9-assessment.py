"""Freeze G9 calibrated-control development comparisons before any games."""
import importlib.util
import json
from pathlib import Path

BASE=Path('docs/research/brn-successor-cglhw')
def load(p): return json.loads(p.read_text(encoding='utf-8-sig'))
def save(p,value):
    with p.open('x',encoding='utf-8',newline='\n') as f:
        json.dump(value,f,indent=2);f.write('\n')
def main():
    spec=importlib.util.spec_from_file_location('baseline',Path(__file__).with_name('search-baseline.py'))
    b=importlib.util.module_from_spec(spec);spec.loader.exec_module(b)
    prior=load(BASE/'wave8-development-openings.selection.json')
    assert b.digest(prior['source'])==prior['sourceSha256']
    exclusions=['wave3-used-openings.selection.json','wave3-fair-control-openings.selection.json']
    exclusions += [f'wave{w}-development-openings.selection.json' for w in range(4,9)]
    book=BASE/'wave9-development-openings.pgn'
    b.select_openings(prior['source'],book,32,2026102209,0,[BASE/n for n in exclusions])
    roots=b.opening_prefixes(book,32)
    reference,affine,quad,cubic,bre=load(BASE/'wave9-trained-model-plan.json')['models']
    protocols=[]
    for label,selected,openings,nodes,millis in [
        ('affine-node',[affine,quad,cubic],roots[:8],262144,0),
        ('degree-node',[quad,cubic],roots[:8],262144,0),
        ('affine-clock',[affine,quad,cubic],roots[:8],0,200),
        ('degree-clock',[quad,cubic],roots[:16],0,200),
        ('bre-clock',[bre,affine,quad,cubic],roots[16:24],0,200),
        ('reference-clock',[reference,cubic,bre],roots[24:32],0,200)]:
        plan=dict(id='wave9-'+label+'-v1',pairs=len(openings),seed=2026102209,
            maximumPlies=512,batchGames=12,batchSeconds=240,legalFallback=False,
            models=selected,nodes=nodes,moveMillis=millis,openingMoves=openings)
        path=BASE/('wave9-'+label+'-plan.json');save(path,plan)
        protocols.append(dict(label=label,plan=path.as_posix(),sha256=b.digest(path),games=2*len(openings)*(len(selected)-1)))
    save(BASE/'wave9-assessment-protocol.json',dict(id='wave9-development-assessment-v1',
        protocols=protocols,totalGames=sum(p['games'] for p in protocols),runtime='wave9-runtime.json',
        selection='Latest176 primary selected prospectively in WAVE9 before training. All head Best175; BRE Best176. No match-based substitution.',
        admissionEvidence={name:b.digest(BASE/name) for name in ['wave9-trained-checkpoints.json','wave9-trained-admission.json','wave9-runtime.json']},
        mapping='Common Policy4/QXRAY and material plus quarter raw residual. Native learned calibrated heads already include half anchor; no extra correction scaling. BRE uses its actual native quarter mapping, not the prior half-change variant.',
        purpose='Direct calibrated affine/quad/cubic comparisons at nodes and time; all three versus continued BRE at time; expressive cubic and BRE versus frozen Gen32 at time.',
        stopping='Complete all fixed budgets except correctness/resource failure. No favorable stopping, unchanged extension or post-hoc Best substitution.',
        statistics='Opening-pair descriptive and bounded intervals. Development roots reused across related protocols; unknown opening-family/training dependence, prior candidate selection, multiplicity. No independent confirmation.',
        resource='One game, one worker/engine, no pondering,64MiB TT,768MiB heap,ActiveProcessorCount1,BelowNormal;12games/240sec per launch.',
        confirmation='Partition1 untouched; no candidate admitted to confirmation.',
        causalLimits='Head arms share identical frozen Q4/data/new Adam. Quad and cubic eight-head feature spaces are not nested. BRE has distinct historical exposure, splits and raw training objective. Learned heads greatly alter score scale; common search does not itself prove calibration optimal.'))
    print('Frozen',sum(p['games'] for p in protocols),'development games')
if __name__=='__main__': main()
