"""Post-hoc, read-only search-domain diagnostic; no fitting or policy change.

Strict quiet means a valid nonterminal position, not in check, with no legal
capture or promotion. This is a conservative board property, not the engine's
actual quiescence leaf distribution or a claim that every capture is searched.
"""
import importlib.util
import json
import math
from pathlib import Path
import statistics as st
import sys
import chess

s=importlib.util.spec_from_file_location('pilot',Path(__file__).with_name('probe-brn-continued-base-heads.py'))
pilot=importlib.util.module_from_spec(s);s.loader.exec_module(pilot);basis=pilot.basis

def main(admission_path,output):
    admission_path,output=map(Path,[admission_path,output]);assert not output.exists()
    a=pilot.read(admission_path);assert a['id']=='wave9-quiet-strata-diagnostic-v1'
    assert pilot.sha(Path(__file__))==a['scriptSha256'];reports=[];bindings={}
    names=['W9-CAL-'+k+'-R4-LR001-g176' for k in ['AFFINE','QUAD','CUBIC']]
    pairs=[(names[1],names[0]),(names[2],names[0]),(names[2],names[1]),
           (names[1],names[1]+'-affine-only'),(names[2],names[2]+'-affine-only')]
    for panel,folder in a['predictions'].items():
        for source in ['bt4','cp']:
            p=Path(folder)/(source+'-predictions.jsonl');bindings[str(p)]=pilot.sha(p)
            assert bindings[str(p)]==a['inputs'][str(p)]
            groups={};invalid=0;selected=0
            for i,line in enumerate(p.read_text(encoding='utf-8').splitlines()):
                if panel!='new' and i<1024:continue
                r=json.loads(line);board=chess.Board(r['fen']);selected+=1
                if not board.is_valid():invalid+=1;continue
                legal=list(board.legal_moves)
                category='terminal' if not legal else 'in-check' if board.is_check() else 'capture-or-promotion' if any(board.is_capture(m) or m.promotion for m in legal) else 'strict-quiet'
                pieces=board.piece_map();material=sum((1 if v.color==board.turn else -1)*{1:1,2:3.2,3:3.3,4:5,5:9}.get(v.piece_type,0) for v in pieces.values())
                phase=sum({1:1,2:3,3:3,4:5,5:9}.get(v.piece_type,0) for v in pieces.values())
                values={n:material+.25*(r['predictions'][n]-material) for pair in pairs for n in pair}
                losses={n:basis.fitlib.objective(v,phase,r['target'])[0] for n,v in values.items()}
                groups.setdefault(category,[]).append((values,losses,len(pieces),r['target']))
            cells=[]
            for category,rows in sorted(groups.items()):
                contrasts=[]
                for left,right in pairs:
                    delta=[r[1][left]-r[1][right] for r in rows]
                    contrasts.append(dict(left=left,right=right,meanCrossEntropyDifference=st.mean(delta),
                        descriptiveRowSE=st.stdev(delta)/math.sqrt(len(delta)) if len(delta)>1 else None,
                        scoreDifferenceRmsPawns=math.sqrt(st.mean((r[0][left]-r[0][right])**2 for r in rows))))
                cells.append(dict(category=category,count=len(rows),meanPieces=st.mean(r[2] for r in rows),
                    decisiveFraction=st.mean(abs(r[3])>.9 for r in rows),contrasts=contrasts))
            assert sum(c['count'] for c in cells)+invalid==selected
            reports.append(dict(panel=panel,source=source,selectedRows=selected,invalidRowsExcluded=invalid,cells=cells))
    report=dict(id=a['id']+'-results',admission=str(admission_path),admissionSha256=pilot.sha(admission_path),inputs=bindings,panels=reports,
        limitations=['Post-hoc source/board stratification, not independent hypothesis confirmation or strength.',
        'Strict-quiet subset is not actual search-leaf sampling; captures may be pruned and search policies differ by context.',
        'Row errors ignore source/game clustering; small strata and multiple comparisons are explicitly exploratory.',
        'Degree masks are unadapted. No fitting, evaluator gating, search changes or owner writes.'])
    with output.open('x',encoding='utf-8',newline='\n') as f:json.dump(report,f,indent=2);f.write('\n')
    print('Completed fixed board-property strata; no training or search-policy intervention')
if __name__=='__main__':main(*sys.argv[1:])
