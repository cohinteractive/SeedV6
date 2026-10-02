"""Final internal NNUE calibration falsification; reuses removal-study statistics.

Usage: python -B tools/analyze-nnue-cp-replacement-calibration.py PAIRS.tsv NEW_OUTPUT
No nonlinear fit: incompatible class/phase responses cannot be rescued by curvature.
"""
import collections
import csv
import hashlib
import importlib.util
import json
import math
import pathlib
import statistics
import sys

SPEC = importlib.util.spec_from_file_location('removal', pathlib.Path(__file__).with_name('analyze-nnue-cp-calibration.py'))
REMOVAL = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(REMOVAL)
CORE = ('N-R', 'B-R', 'R-Q')
ANCHORS = {'N-B': 10, 'N-R': 180, 'B-R': 170, 'R-Q': 400,
           'P-N': 220, 'P-B': 230, 'P-R': 400, 'P-Q': 800}


def phase(r):
    return 'opening' if r['phase'] <= 7 else 'middlegame' if r['phase'] <= 16 else 'endgame'


def magnitude(r):
    a = abs(r['r0'])
    return '<0.25' if a < .25 else '0.25-0.75' if a < .75 else '0.75-1.5' if a < 1.5 else '>=1.5'


def describe(rows, k):
    result = REMOVAL.describe(rows, (k, 0))
    result['anchor_relative_rmse'] = math.sqrt(statistics.fmean(((k*r['x']-r['y'])/abs(r['y']))**2 for r in rows))
    result['anchor_relative_mae'] = statistics.fmean(abs(k*r['x']-r['y'])/abs(r['y']) for r in rows)
    return result


def groups(rows, k, getters, intervals=False):
    result = {}
    for name, getter in getters.items():
        buckets = collections.defaultdict(list)
        for r in rows:
            buckets[getter(r)].append(r)
        result[name] = {label: describe(q, k) for label, q in sorted(buckets.items())}
        if intervals and name in ('piece', 'phase', 'magnitude'):
            for label, q in buckets.items():
                result[name][label]['bootstrap'] = REMOVAL.bootstrap(q)
    return result


GETTERS = {
    'piece': lambda r: r['piece'], 'phase': phase, 'magnitude': magnitude,
    'direction': lambda r: r['direction'],
    'sign': lambda r: 'positive' if r['y'] > 0 else 'negative',
    'stm': lambda r: 'white' if r['stm'] == 0 else 'black',
    'replaced_side': lambda r: 'white' if r['replaced_side'] == 0 else 'black',
    'source': lambda r: r['source'],
    'piece_phase': lambda r: r['piece']+':'+phase(r),
    'piece_magnitude': lambda r: r['piece']+':'+magnitude(r),
    'piece_direction': lambda r: r['piece']+':'+r['direction'],
}


def analyze(rows):
    train = [r for r in rows if REMOVAL.fold(r) != 0]
    test = [r for r in rows if REMOVAL.fold(r) == 0]
    assert not ({(r['source'],r['cluster']) for r in train} & {(r['source'],r['cluster']) for r in test})
    k = REMOVAL.slope(train)
    result = dict(pairs=len(rows), train_pairs=len(train), test_pairs=len(test),
                  base_positions=len({(r['source'],r['index']) for r in rows}),
                  train_k=k, all_ls_k=REMOVAL.slope(rows), bootstrap=REMOVAL.bootstrap(train),
                  train=describe(train,k), test=describe(test,k),
                  robust_huber_k=REMOVAL.huber(train), robust_l1_k=REMOVAL.l1_slope(train),
                  raw_endpoints=REMOVAL.distribution(v for r in rows for v in (r['r0'],r['r1'])),
                  groups=groups(rows,k,GETTERS,True), heldout_groups=groups(test,k,GETTERS))
    counts = collections.Counter(r['piece'] for r in train)
    equal = REMOVAL.slope(train,[1/counts[r['piece']] for r in train])
    result['class_balanced'] = dict(k=equal,test=describe(test,equal))
    result['split_stability'] = []
    for salt in range(4):
        tr = [r for r in rows if REMOVAL.fold(r,salt) != 0]
        te = [r for r in rows if REMOVAL.fold(r,salt) == 0]
        fitted = REMOVAL.slope(tr)
        result['split_stability'].append(dict(salt=salt,k=fitted,train_pairs=len(tr),test=describe(te,fitted)))
    identities = sorted({(r['source'],r['cluster']) for r in train},key=lambda v:hashlib.sha256(str(v).encode()).digest())
    result['convergence'] = []
    for fraction in (.25,.5,.75,1):
        selected = set(identities[:max(1,int(len(identities)*fraction))])
        subset = [r for r in train if (r['source'],r['cluster']) in selected]
        result['convergence'].append(dict(fraction=fraction,clusters=len(selected),pairs=len(subset),k=REMOVAL.slope(subset)))
    reverse = [dict(r,x=r['reverse_r1']-r['reverse_r0'],y=-r['y'],z=0) for r in rows]
    result['symmetry'] = dict(reverse_ls_k=REMOVAL.slope(reverse),
                             raw_stm_sum=REMOVAL.distribution(r['r0']+r['reverse_r0'] for r in rows),
                             paired_delta_sum=REMOVAL.distribution(r['x']+q['x'] for r,q in zip(rows,reverse)),
                             reverse_wrong_fraction=sum(r['x']*r['y'] <= 0 for r in reverse)/len(reverse),
                             reverse_test=describe([r for r in reverse if REMOVAL.fold(r)==0],k))
    return result


def self_test():
    REMOVAL.self_test()
    synthetic = [dict(x=1.0,y=1000,z=0),dict(x=-2.0,y=-2000,z=0)]
    assert REMOVAL.slope(synthetic)==1000
    assert REMOVAL.metrics(synthetic,(1000,0))['rmse']==0
    # Difference intercept is zero because equal endpoints have zero difference.
    assert REMOVAL.prediction(dict(x=0,z=0),(1000,0))==0
    r = dict(source='handcrafted',cluster='game-0')
    expected = int.from_bytes(hashlib.sha256(b'nnue-cp-v1:0:handcrafted:game-0').digest()[:8],'big')%5
    assert REMOVAL.fold(r)==expected


def load(path):
    rows=[]
    with path.open(encoding='utf-8') as stream:
        for r in csv.DictReader(stream,delimiter='\t'):
            for key in ('r0','r1','reverse_r0','reverse_r1'):
                r[key]=float(r[key]); assert math.isfinite(r[key])
            for key in ('phase','stm','replaced_side','cp','material','index','square','max_see0','max_see1'):
                r[key]=int(r[key])
            r['quiet']=r['quiet']=='true'
            r['x'],r['y'],r['z']=r['r1']-r['r0'],r['cp'],0.0
            assert abs(r['y'])==ANCHORS[r['piece']]
            expected=(1 if r['direction']=='increase' else -1)*(1 if r['stm']==r['replaced_side'] else -1)*ANCHORS[r['piece']]
            assert r['y']==expected
            assert r['quiet']==(r['max_see0']<100 and r['max_see1']<100 and r['mate_in_one0']=='false' and r['mate_in_one1']=='false')
            rows.append(r)
    assert len({(r['source'],r['index'],r['square'],r['to_piece']) for r in rows})==len(rows)
    return rows


def main():
    self_test()
    path,output=pathlib.Path(sys.argv[1]),pathlib.Path(sys.argv[2])
    output.mkdir()
    rows=load(path)
    quiet=[r for r in rows if r['quiet']]
    core=[r for r in quiet if r['piece'] in CORE]
    result=dict(schema='seedv6.nnue-cp-replacement-calibration.research.v1',
                pairs_sha256=hashlib.sha256(path.read_bytes()).hexdigest(),
                split='Unchanged removal-study SHA256(nnue-cp-v1:{salt}:{source}:{cluster}); first 8 bytes big endian mod 5; 0 held out',
                bootstrap='1000 within-source whole-game/root resamples, random.Random(20261002)',
                primary_classes=CORE, small_delta_probe='N-B excluded from candidate fit',
                nonlinear='Not fitted: only permitted after class and phase consistency',self_tests='passed')
    for label,subset in [('broad_core',[r for r in rows if r['piece'] in CORE]),('quiet_core',core),
                         ('quiet_core_abs_raw_le_1',[r for r in core if max(abs(r['r0']),abs(r['r1']))<=1])]:
        result[label]=analyze(subset)
        q=result[label]
        print(label,'N',q['pairs'],'K',q['train_k'],'CI',q['bootstrap'],'test MAE/RMSE',q['test']['metrics']['mae'],q['test']['metrics']['rmse'])
    k=result['quiet_core']['train_k']
    result['all_valid_classes']=groups(rows,k,{'piece':GETTERS['piece']})
    result['all_quiet_classes']=groups(quiet,k,GETTERS,True)
    result['all_quiet_heldout']=groups([r for r in quiet if REMOVAL.fold(r)==0],k,GETTERS)
    without_small=[r for r in quiet if r['piece']!='N-B' and REMOVAL.fold(r)!=0]
    optional_k=REMOVAL.slope(without_small)
    result['optional_pawn_inclusive']=dict(train_pairs=len(without_small),k=optional_k,
        bootstrap=REMOVAL.bootstrap(without_small),test=describe([r for r in quiet if r['piece']!='N-B' and REMOVAL.fold(r)==0],optional_k))
    examples=[]
    for piece in ANCHORS:
        subset=[r for r in quiet if REMOVAL.fold(r)==0 and r['piece']==piece]
        subset.sort(key=lambda r:math.copysign(1,r['y'])*k*r['x'])
        for percentile in (.1,.5,.9):
            r=subset[int((len(subset)-1)*percentile)]
            examples.append(dict(r,percentile=percentile,calibrated_delta=k*r['x']))
    with (output/'heldout-examples.tsv').open('w',encoding='utf-8',newline='') as stream:
        writer=csv.DictWriter(stream,fieldnames=list(examples[0]),delimiter='\t'); writer.writeheader(); writer.writerows(examples)
    previous_path=pathlib.Path(__file__).resolve().parents[1]/'docs/research/brn/evidence/nnue-cp-calibration-2026-10-02/results.json'
    previous=json.loads(previous_path.read_text(encoding='utf-8'))['quiet']
    result['removal_comparison']=dict(results_sha256=hashlib.sha256(previous_path.read_bytes()).hexdigest(),
        k=previous['train_k'],bootstrap=previous['bootstrap'],test=previous['test'],
        piece=previous['groups']['piece'],phase=previous['groups']['phase'],magnitude=previous['groups']['magnitude'],
        heldout_piece=previous['heldout_groups']['piece'],split_stability=previous['split_stability'])
    (output/'results.json').write_text(json.dumps(result,indent=2,allow_nan=False)+'\n',encoding='utf-8')


if __name__=='__main__':
    main()
