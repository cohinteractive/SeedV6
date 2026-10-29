"""R050 deterministic opposite-turn copies; no new sampling or labels."""
import json
from pathlib import Path
import time
import chess
from baseline import ROOT, admit, sha
from compute_control import atomic


def main():
    resource=admit(initial=True); started=time.monotonic()
    folder=ROOT/'docs/research/brn-relational-cglhw';out=folder/'raw/r050-data'
    source_path=folder/'r049-selection.json';source=json.loads(source_path.read_text())
    inputs={};files={};plans=[]
    for name,digest in source['files'].items():assert sha(ROOT/name)==digest
    out.mkdir(exist_ok=True)
    for shard,name in enumerate(source['plans']):
        parent=ROOT/name;plan=json.loads(parent.read_text());cases_path=ROOT/plan['cases']
        inputs[name]=sha(parent);inputs[plan['cases']]=sha(cases_path)
        positions=json.loads(cases_path.read_text())['positions'];copies=[]
        for position in positions:
            board=chess.Board(position['fen']);assert board.is_valid() and not board.is_check()
            before=board.board_fen();board.turn=not board.turn;board.ep_square=None
            assert board.is_valid() and not board.is_check() and board.board_fen()==before
            copies.append(dict(fen=board.fen(),pieces=len(board.piece_map()),in_check=False))
        cases=out/f'cases-{shard:02d}.json';value=dict(positions=copies)
        if cases.exists():assert json.loads(cases.read_text())==value
        else:atomic(cases,value)
        newplan=plan|dict(cases=str(cases.relative_to(ROOT)),cases_sha256=sha(cases),
                          output=str((out/f'prediction-{shard:02d}.json').relative_to(ROOT)))
        planpath=folder/f'r050-{shard:02d}-plan.json'
        if planpath.exists():assert json.loads(planpath.read_text())==newplan
        else:atomic(planpath,newplan)
        for path in (cases,planpath):files[str(path.relative_to(ROOT))]=sha(path)
        plans.append(str(planpath.relative_to(ROOT)))
    result=dict(selector_sha256=sha(Path(__file__)),protocol_sha256=sha(folder/'R050.md'),
                source_sha256=sha(source_path),inputs=inputs,files=files,plans=plans,
                positions=8192,resource=resource,seconds=time.monotonic()-started,
                scope='Opposite-turn copies of R049,not played roots or independent validation;no fit.')
    target=folder/'r050-selection.json'
    if target.exists():
        existing=json.loads(target.read_text())
        for key in ('selector_sha256','protocol_sha256','source_sha256','inputs','files','plans','positions'):assert existing[key]==result[key]
        print('Verified completed R050 preparation')
    else:atomic(target,result);print('Prepared8192 valid noncheck opposite-turn positions')


if __name__=='__main__':main()
