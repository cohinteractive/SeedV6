"""New confirmation only: original arithmetic plus complete runner-charge gate."""
import json
import re
import sys
from verify_inputs import HERE, ROOT, OLD, digest, write_new
from run_diagnostic_v1 import module


def audit(name,diagnostic_validation=False):
    assert name.startswith('boundary2-fresh-v1-b') or (diagnostic_validation and name=='game-diagnostic-v1-a01'), 'New study or named completed diagnostic only'
    sys.path.insert(0,str(OLD))
    old=module('original_clock_arithmetic',OLD/'audit_confirmation_clock_v1.py'); captured=[]
    old.write_new=lambda path,value:captured.append(value)
    folder=HERE/'raw'/name
    if diagnostic_validation: folder=folder/'match'
    result=old.audit(str(folder.resolve())); assert len(captured)==1
    b=module('baseline',ROOT/'tools/search-baseline.py'); counts={'baseline':0,'candidate':0}; rows=[]
    with (folder/'games.pgn').open(encoding='utf8') as stream:
        games=0
        while (game:=b.chess.pgn.read_game(stream)) is not None:
            assert not game.errors; games+=1; board=game.board(); previous={'baseline':10100,'candidate':10100}
            history=[]
            for ply,node in enumerate(game.mainline(),1):
                actor=game.headers['White' if board.turn else 'Black']; fen=board.fen()
                reported=re.search(r'info string request nodes=(\d+) elapsedNanos=(\d+) termination=(\w+)',node.comment)
                move=node.move; assert move in board.legal_moves
                if reported:
                    before=previous[actor]
                    charged=round(float(re.search(r'^\S+/\d+ (\d+(?:\.\d+)?)s',node.comment)[1])*1000)
                    left=round(float(re.search(r'\btl=(\d+(?:\.\d+)?)s',node.comment)[1])*1000)
                    assert left==before+100-charged
                    base=min(max(0,before-50),max(1,before//30+65))
                    hard=base if actor=='baseline' else min(max(0,before-50),2*base)
                    row={'actor':actor,'gameOrdinal':games,'ply':ply,'beforeMillis':before,'hardMillis':hard,
                        'chargedMillis':charged,'chargedOverHardMillis':charged-hard,
                        'reportedOverHardMillis':int(reported[2])/1e6-hard,'reserveBeforeIncrementMillis':left-100}
                    if row['reportedOverHardMillis']>25 or charged-hard>25 or left<100:
                        row.update(rootFen=fen,historyUci=list(history))
                    rows.append(row); counts[actor]+=1; previous[actor]=left
                else: assert ply<=16 and 'book' in node.comment
                board.push(move); history.append(move.uci())
    assert games==64 and all(counts[a]==result['actors'][a]['requests'] for a in counts)
    result['completeClockPassed']=all(r['chargedOverHardMillis']<=25 and r['reserveBeforeIncrementMillis']>=0 for r in rows)
    result['maximumRunnerChargeOverHardMillis']=max(r['chargedOverHardMillis'] for r in rows)
    result['adverseRequests']=[r for r in rows if 'rootFen' in r]
    result['newAuditSha256']=digest(__file__)
    result['context']=('Validation on completed development diagnostic only; no strength evidence. ' if diagnostic_validation else 'Fresh BOUNDARY2 confirmation; scores unopened. ')+ 'Both original reported<=25ms and complete runner-charge<=25ms gates apply prospectively.'
    write_new(HERE/(name+('-fresh-clock-validation.json' if diagnostic_validation else '-clock-audit.json')),result)
    return result


if __name__=='__main__':
    value=audit(sys.argv[1]); print(json.dumps({k:value[k] for k in ('clockReliabilityPassed','completeClockPassed','maximumRequestOverHardMillis','maximumRunnerChargeOverHardMillis')}))
