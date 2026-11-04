"""Prospective MW-R001 confirmation checks; never selects a candidate or starts work.

The match supervisor owns execution, deadlines and accounting. This small module
checks the additional frozen claim boundary and reports sensitivity after a full
audited sample. It does not authorize replay, segmented continuation or adoption.
"""
import hashlib
import json
import math
import random
import statistics


GATE = dict(minimumScore=.53, lowerBoundExclusive=.50, confidence=.95,
            method='paired-normal', z=1.9599639845400536,
            zeroVariance='inconclusive')
FAILURE_RULE = 'incomplete-attempt-no-pass-no-replay'


def validate(root, protocol, ledger):
    p = protocol
    c = p['confirmation']
    reserve = ledger['confirmation']
    n = len(p['openingIndices'])
    if (p['kind'] != 'timed' or p['mode'] != 'B' or p['workers'] != 4
            or p['baseline'] != 'ROOT_REUSE' or p['candidate'] == p['baseline']
            or p.get('firstColorPolicy') != 'alternate'):
        raise ValueError('Confirmation requires the declared primary paired W4 timed domain')
    if (reserve['attemptsStarted'] != 0 or reserve['attemptsMaximum'] != 1
            or not 30 <= n <= min(480, reserve['pairsMaximum'])
            or p['openingIndices'] != list(range(n))):
        raise ValueError('One unspent fixed sample in original randomized reserve order')
    if (c['status'] != 'prospectively-frozen' or c['gate'] != GATE
            or c['failureRule'] != FAILURE_RULE or c['interimScoreAccess'] != 'prohibited'
            or c['resume'] != 'none-after-first-game'
            or c['pairs'] != n or c['bootstrap'] != dict(samples=10000, seed=2026110417)):
        raise ValueError('Changed claim, sample, sensitivity or failure boundary')
    if 2*n > reserve['protectedTimedGames'] or p['timeoutSeconds']+60 > reserve['protectedTimedWallSeconds']:
        raise ValueError('Confirmation exceeds its protected allocation')

    def evidence(name):
        path = root/name
        if hashlib.sha256(path.read_bytes()).hexdigest() != p['inputs'].get(name):
            raise ValueError('Unbound or changed confirmation evidence '+name)
        return json.loads(path.read_text(encoding='utf-8'))

    selection = evidence(c['reserveSelection'])
    freshness = evidence(c['freshnessAudit'])
    if (selection['count'] < n or selection['count'] != len(selection['positionKeys'])
            or len(set(selection['positionKeys'])) != selection['count']
            or selection['outputSha256'] != p['inputs'].get(p['openings'])
            or freshness['status'] != 'passed' or freshness['overlaps'] != 0):
        raise ValueError('Unqualified protected opening population')
    development = evidence(c['developmentSelection'])
    if set(selection['positionKeys']) & set(development['positionKeys']):
        raise ValueError('Development overlap')
    # Verify legal roots/history and exact selection ordering, without evaluating
    # or playing any reserve position. No outcome-dependent opening filtering.
    import chess.pgn
    with (root/p['openings']).open(encoding='utf-8') as stream:
        keys = []
        while (game := chess.pgn.read_game(stream)) is not None:
            if game.errors or game.end().board().is_game_over(claim_draw=True):
                raise ValueError('Invalid or terminal reserve opening')
            keys.append(' '.join(game.end().board().fen().split()[:4]))
    if keys != selection['positionKeys']:
        raise ValueError('Reserve PGN/selection mismatch')
    if hashlib.sha256((root/p['openings']).read_bytes()).hexdigest() != selection['outputSha256']:
        raise ValueError('Changed reserve PGN')

    basis = evidence(c['timingVarianceBasis'])
    if (basis['status'] != 'complete exploratory analysis' or basis['kind'] != 'timed'
            or basis['workers'] != 4 or basis['candidate'] != p['candidate']
            or basis['baseline'] != p['baseline'] or basis['runtime'] != p['runtime']
            or basis['statistics']['pairedNormal95Score'] is None):
        raise ValueError('Need matching completed exploratory throughput/variance evidence')
    basis_protocol = evidence(basis['protocol'])
    if basis_protocol['tc'] != p['tc'] or basis_protocol['maxMoves'] != p['maxMoves']:
        raise ValueError('Changed clock or game cap invalidates throughput basis')
    minimum_wall = math.ceil(1.25*basis['wallSeconds']*n/basis['statistics']['pairs']+120)
    if p['timeoutSeconds'] < minimum_wall:
        raise ValueError('Insufficient prospective throughput margin')
    # These files are technical evidence, not a magical Boolean proof. The
    # prospective selection record must explain their relevance and limitations.
    if not c['qualificationEvidence'] or not c['selectionRationale'] or not c['precisionAssessment']:
        raise ValueError('Missing technical or inferential selection assessment')
    for name in c['qualificationEvidence']:
        evidence(name)
    return dict(pairs=n, minimumTimeoutSeconds=minimum_wall,
                exploratoryPairSd=basis['statistics']['pairScoreStandardError']
                    *math.sqrt(basis['statistics']['pairs']))


def consume_attempt(ledger, protocol_path, protocol_hash, initiated, utc):
    """Called only with factual first-game journal evidence or conservative loss.

    Parent persists this while the match runs; the fsynced first-pair journal is
    authoritative across a crash before that ledger update. Same protocol is
    idempotent reconciliation, never permission to resume/replay it.
    """
    if initiated <= 0:
        return False
    reserve = ledger['confirmation']
    if reserve['attemptsStarted']:
        if reserve['protocol'] != protocol_path or reserve['protocolSha256'] != protocol_hash:
            raise ValueError('Confirmation attempt already consumed by another protocol')
        return False
    reserve.update(attemptsStarted=1, protocol=protocol_path,
                   protocolSha256=protocol_hash, firstInitiatedObservedUtc=utc)
    return True


def assess(statistics_record):
    """Only call after complete immutable evidence/audits are independently checked."""
    s = statistics_record
    interval = s['pairedNormal95Score']
    passed = (interval is not None and s['score'] >= GATE['minimumScore']
              and interval[0] > GATE['lowerBoundExclusive'])
    values = [i/4 for i, count in enumerate(s['pentanomial']) for _ in range(count)]
    if len(values) != s['pairs']:
        raise ValueError('Pair statistics mismatch')
    rng = random.Random(2026110417)
    samples = sorted(statistics.mean(rng.choices(values, k=len(values))) for _ in range(10000))
    return dict(primaryStatisticalGate='passed' if passed else 'not-passed', gate=GATE,
                pairedBootstrapPercentile95=[samples[249], samples[9749]],
                sensitivity='Fixed-seed opening-pair percentile bootstrap; no game-level independence. '
                    'Normal, bounded and bootstrap assumptions differ; none removes opening-population '
                    'dependence or supports another clock/platform. Technical/operational qualification '
                    'and owner acceptance remain separate; no adoption is inferred.')
