"""Post-confirmation verification and correction of descriptive game-order labels.

Preserves the frozen analyzer/output. Its per-pair records are canonicalized by
candidate colour, so enumeration was not actual game order. Read raw sequential
PGNs for that supplementary breakdown and independently check every primary pair.
No exclusions, primary-gate changes, new games or claim attempts.
"""
import argparse
from collections import Counter
from fractions import Fraction
import hashlib
import json
import math
from pathlib import Path
import statistics
import sys
import chess.pgn

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'docs/research/search/p4-critical-line'


def digest(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def main(output):
    name='c1-confirm-w4-v2';directory=OUT/'raw'/name
    rp=OUT/(name+'-execution.json');ap=OUT/(name+'-analysis.json')
    receipt=json.loads(rp.read_text());analysis=json.loads(ap.read_text());protocol=json.loads((ROOT/receipt['protocol']).read_text())
    assert receipt['status']=='passed' and receipt['inputsStable'] and receipt['error'] is None
    assert receipt['initiatedGames']==receipt['completedGames']==640 and receipt['drain']['ownedProcessesRemaining']==0
    assert digest(ROOT/receipt['protocol'])==receipt['protocolSha256']
    for path,sha in receipt['files'].items():assert digest(ROOT/path)==sha,path
    for path,sha in protocol['inputs'].items():assert digest(ROOT/path)==sha,path
    selection=json.loads((ROOT/protocol['confirmation']['selection']).read_text())
    orders=[[],[]];colours={'White':[],'Black':[]};pairs=[];wdl=Counter();lengths=[];first=Counter();raw_files={}
    for index in range(320):
        sub=directory/f'pair-{index:03d}';path=sub/'games.pgn'
        with path.open(encoding='utf8') as f:
            games=[chess.pgn.read_game(f),chess.pgn.read_game(f)];assert chess.pgn.read_game(f) is None
        with (sub/'opening.pgn').open(encoding='utf8') as f:opening=chess.pgn.read_game(f)
        assert opening is not None and not opening.errors
        prefix=list(opening.mainline_moves());assert len(prefix)==14
        assert ' '.join(opening.end().board().fen().split()[:4])==selection['positionKeys'][index]
        expected=['candidate','baseline'] if index%2 else ['baseline','candidate']
        assert [g.headers['White'] for g in games]==expected
        first[expected[0]]+=1;total=Fraction(0)
        for order,game in enumerate(games):
            assert not game.errors and game.board().fen()==chess.STARTING_FEN
            moves=list(game.mainline_moves());assert moves[:14]==prefix
            result=game.headers['Result'];white={'1-0':Fraction(1),'0-1':Fraction(0),'1/2-1/2':Fraction(1,2)}[result]
            assert game.headers['Termination'].lower()=='normal'
            board=game.end().board()
            if board.is_checkmate():assert result==('0-1' if board.turn else '1-0')
            else:assert result=='1/2-1/2' and (board.is_stalemate() or board.is_insufficient_material() or board.is_repetition(3) or board.halfmove_clock>=100)
            score=white if game.headers['White']=='candidate' else 1-white
            total+=score;wdl[score]+=1;orders[order].append(score)
            colours['White' if game.headers['White']=='candidate' else 'Black'].append(score);lengths.append(len(moves))
        pairs.append(total);raw_files[str(path.relative_to(ROOT))]=digest(path)
    assert [float(x) for x in pairs]==analysis['pairs']
    assert [wdl[x] for x in (Fraction(1),Fraction(1,2),Fraction(0))]==analysis['wdl']
    values=[x/2 for x in pairs];mean=sum(values)/320;variance=sum((x-mean)**2 for x in values)/319
    se=math.sqrt(float(variance)/320);bounds=[float(mean)-1.96*se,float(mean)+1.96*se]
    primary=analysis['primary'];assert float(mean)==primary['score']
    assert math.isclose(float(variance),primary['pairVariance'],abs_tol=1e-15)
    assert all(math.isclose(x,y,abs_tol=1e-15) for x,y in zip(bounds,primary['primaryPairedNormal95']))
    assert primary['gate']==protocol['confirmation']['gate']
    gates=dict(scoreAtLeast53=float(mean)>=.53,pairedNormalLowerStrictlyAbove50=bounds[0]>.5)
    samples=[json.loads(x) for x in (directory/'environment.jsonl').read_text().splitlines()]
    result=dict(status='passed independent verification; both primary strength gates failed',protocolSha256=receipt['protocolSha256'],
        frozenAnalysisSha256=digest(ap),executionSha256=digest(rp),verifierSha256=digest(__file__),games=640,pairs=320,
        exactCandidatePoints=str(sum(pairs)),exactMean=str(mean),exactPairVariance=str(variance),score=float(mean),primaryPairedNormal95=bounds,
        primaryGateResults=gates,wdl=analysis['wdl'],allPairTotalsMatchFrozenAnalysis=True,allFrozenInputsAndRawHashesMatch=True,
        allGamesLegallyReplayed=True,normalTerminations=640,firstGameWhiteCounts=dict(first),
        correctedActualGameOrderScores=[float(statistics.mean(x)) for x in orders],
        candidateColourScores={k:float(statistics.mean(v)) for k,v in colours.items()},
        frozenOrderFieldCorrection='The frozen analyzer enumerated colour-canonicalized games returned by search-baseline.analyze(unordered=True). Its descriptiveGameOrderScores are therefore colour slots, not actual execution order. Corrected values above use raw sequential PGNs. All320 pair totals, WDL, primary score/variance/CI/gate and other resource/game validity are unchanged. Frozen code/output are preserved.',
        environment=dict(samples=len(samples),meanTotalCpuPercent=statistics.mean(s['totalCpuPercent'] for s in samples),
            maximumTotalCpuPercent=max(s['totalCpuPercent'] for s in samples),minimumFreeMemoryGiB=min(s['freeMemoryGiB'] for s in samples),
            peakOwnedCommittedGiB=receipt['peakOwnedCommittedGiB']),
        gamePlies=dict(minimum=min(lengths),maximum=max(lengths),mean=statistics.mean(lengths)),
        rawPgnHashes=raw_files,scope='Post-hoc implementation correction of a planned descriptive sensitivity; no change to frozen acceptance criteria, candidate, sample or historical evidence.')
    with Path(output).open('x',encoding='utf8',newline='\n') as f:json.dump(result,f,indent=2);f.write('\n')
    print(json.dumps({k:v for k,v in result.items() if k!='rawPgnHashes'}))


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--output',required=True)
    main(parser.parse_args().output)
