"""Audit adverse clock requests without calculating or displaying game scores."""
import json
import re
from pathlib import Path
from run_trajectory_v1 import HERE, ROOT, digest, write_new, module


def main():
    b = module('baseline', ROOT/'tools/search-baseline.py'); tails = []; evidence = {}; counts = {}; actors = {}
    for block in range(1,5):
        name = f'sr034-confirmation-v1-b{block:02}'; path = HERE/'raw'/name/'games.pgn'
        receipt = json.loads((HERE/(name+'-summary.json')).read_text())
        assert receipt['status'] in ('passed','clock-unqualified')
        for p, sha in receipt['evidence'].items(): assert digest(HERE/p) == sha, p
        evidence[name+'-summary.json'] = digest(HERE/(name+'-summary.json'))
        evidence[str(path.relative_to(HERE))] = digest(path)
        game_index = 0; checked = 0
        with path.open(encoding='utf-8') as stream:
            while (game := b.chess.pgn.read_game(stream)) is not None:
                assert not game.errors; game_index += 1; board = game.board(); previous = {'baseline':10100,'candidate':10100}; history = []
                for ply,node in enumerate(game.mainline(),1):
                    actor = game.headers['White' if board.turn else 'Black']; fen = board.fen(); fullmove = board.fullmove_number
                    move = node.move; assert move in board.legal_moves
                    telemetry = re.search(r'info string request nodes=(\d+) elapsedNanos=(\d+) termination=(\w+)',node.comment)
                    if telemetry:
                        elapsed = int(telemetry[2])/1e6; before = previous[actor]
                        left = round(float(re.search(r'\btl=(\d+(?:\.\d+)?)s',node.comment)[1])*1000)
                        charged = round(float(re.search(r'^\S+/\d+ (\d+(?:\.\d+)?)s',node.comment)[1])*1000)
                        assert left == before+100-charged; checked += 1
                        base = min(max(0,before-50),max(1,before//30+65))
                        hard = base if actor == 'baseline' else min(max(0,before-50),2*base)
                        overrun = elapsed-hard
                        actors.setdefault(actor,{'requests':0,'maximumRequestOverHardMillis':-float('inf'),'minimumReserveBeforeIncrementMillis':float('inf'),'responsesBelow500Millis':0})
                        a = actors[actor]; a['requests'] += 1
                        a['maximumRequestOverHardMillis'] = max(a['maximumRequestOverHardMillis'],overrun)
                        a['minimumReserveBeforeIncrementMillis'] = min(a['minimumReserveBeforeIncrementMillis'],left-100)
                        a['responsesBelow500Millis'] += left < 500
                        if overrun > 25:
                            completed = re.search(r'\bn=(\d+)',node.comment)
                            tails.append({'block':block,'gameOrdinal':game_index,'ply':ply,'moveNumber':fullmove,'actor':actor,
                                'rootFen':fen,'historyUci':history.copy(),'playedMove':move.uci(),'clockBeforeMillis':before,
                                'baseAllocationMillis':base,'hardAllocationMillis':hard,'requestElapsedMillis':elapsed,
                                'runnerChargedMillis':charged,'postIncrementClockMillis':left,'reserveBeforeIncrementMillis':left-100,
                                'overrunMillis':overrun,'termination':telemetry[3],'wholeNodes':int(telemetry[1]),
                                'lastCompletedNodes':None if completed is None else int(completed[1])})
                        previous[actor] = left
                    else: assert 'book' in node.comment and ply <= 16
                    board.push(move); history.append(move.uci())
        assert game_index == 64; counts[name] = checked
    record = {'status':'clock_gate_failure_verified','scores':'Unopened; no WDL or strength analysis performed',
        'thresholdMillis':25,'requestsChecked':sum(counts.values()),'byBlock':counts,'actors':actors,'adverseRequests':tails,
        'evidence':evidence,'scriptSha256':digest(Path(__file__)),
        'limits':['Runner charge and clock conservation independently corroborate request elapsed. No request-level CPU, GC, JFR or scheduler trace was captured; exact cause cannot be assigned.',
            'The failed comparison remains unqualified even if the adverse request belongs to baseline. No exclusion, replay, substituted block or relaxed threshold.',
            'Game results were not analyzed. All four blocks retain their own original integrity/resource/clock status.']}
    write_new(HERE/'confirmation-clock-tail-v1.json',record)
    print(json.dumps({k:v for k,v in record.items() if k not in ('evidence','scriptSha256')},indent=2))


if __name__ == '__main__': main()
