"""Freeze the prospective G6 development protocol; no matches or model writes."""
import importlib.util
import json
from pathlib import Path

BASE=Path('docs/research/brn-successor-cglhw')
def load(p): return json.loads(p.read_text(encoding='utf-8-sig'))
def save(p,obj):
    with p.open('x',encoding='utf-8') as f: json.dump(obj,f,indent=2); f.write('\n')

def main():
    spec=importlib.util.spec_from_file_location('baseline',Path(__file__).with_name('search-baseline.py'))
    b=importlib.util.module_from_spec(spec);spec.loader.exec_module(b)
    prior=load(BASE/'wave5-development-openings.selection.json')
    assert b.digest(prior['source']) == prior['sourceSha256']
    book=BASE/'wave6-development-openings.pgn'
    b.select_openings(prior['source'],book,32,2026102106,0,
        [BASE/n for n in ['wave3-used-openings.selection.json','wave3-fair-control-openings.selection.json',
                         'wave4-development-openings.selection.json','wave5-development-openings.selection.json']])
    openings=b.opening_prefixes(book,32)
    models=load(BASE/'wave6-trained-model-plan.json')['models']
    for m in models[1:]: m['correctionScale']=.5; m['name']+='-g128-half'
    reference,q16,c16,c4,bre=models
    protocols=[]
    for label, selected, roots, nodes, millis in [
        ('degree-node',[q16,c16],openings[:16],262144,0),
        ('degree-clock',[q16,c16],openings[:16],0,200),
        ('compact-node',[c16,c4],openings[16:24],262144,0),
        ('compact-clock',[c16,c4],openings[16:24],0,200),
        ('bre-clock',[bre,q16,c16,c4],openings[16:24],0,200),
        ('reference-clock',[reference,q16,c16,c4,bre],openings[24:32],0,200)]:
        plan=dict(id='wave6-'+label+'-v1',pairs=len(roots),seed=2026102106,maximumPlies=512,
                  batchGames=12,batchSeconds=240,legalFallback=False,models=selected,nodes=nodes,
                  moveMillis=millis,openingMoves=roots)
        path=BASE/('wave6-'+label+'-plan.json');save(path,plan)
        protocols.append(dict(label=label,plan=path.as_posix(),sha256=b.digest(path),
                              games=2*len(roots)*(len(selected)-1)))
    save(BASE/'wave6-assessment-protocol.json',dict(id='wave6-development-assessment-v1',protocols=protocols,
        totalGames=sum(p['games'] for p in protocols),runtime='wave6-runtime.json',
        selection='Latest Gen128 prospectively specified before games; all four Best pointers equal Latest.',
        mapping='Common Policy4/QXRAY; existing material plus quarter residual; half learned change from Gen32 for every trained arm.',
        purpose='Trained degree comparison at practical-depth equal nodes and equal time, compact tradeoff, continued BRE and frozen reference.',
        stopping='Complete each fixed budget; stop for correctness/resource failure, never for favorable significance. Further protocols require a separately recorded question/selection.',
        statistics='Pair-level descriptive intervals and bounded-pair intervals; reused roots within this programme and unknown opening family dependence. No independent superiority claim.',
        resource='One game, one search worker, no ponder,64MiB TT,768MiB heap,ActiveProcessorCount1,BelowNormal;12games/240sec per launch.',
        confirmation='Partition1 remains reserved. No confirmation candidate selected.'))
    print('Frozen',sum(p['games'] for p in protocols),'development games')

if __name__=='__main__':main()
