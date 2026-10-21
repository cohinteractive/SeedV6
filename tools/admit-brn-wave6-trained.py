"""Verify retained G6 bootstrap/attempt bytes and freeze audited owner stores.

Exclusive evidence destinations; never writes into an owner store. Requires the
complete Java codec audit and audit-brn-wave6-training.py report first.
"""
import hashlib
import importlib.util
import io
import json
import shutil
import struct
from pathlib import Path

BASE = Path('docs/research/brn-successor-cglhw')
RAW = BASE/'raw/wave6'
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def read(p): return json.loads(p.read_text(encoding='utf-8-sig'))
def write(p, obj):
    with p.open('x', encoding='utf-8') as f: json.dump(obj, f, indent=2); f.write('\n')

def attempt(path):
    data = path.read_bytes()
    assert hashlib.sha256(data[:-32]).digest() == data[-32:]
    stream = io.BytesIO(data[:-32])
    def integer(fmt): return struct.unpack(fmt, stream.read(struct.calcsize(fmt)))[0]
    def utf():
        value = stream.read(integer('>H'))
        # These actual records are ASCII; refuse unsupported modified-UTF cases.
        return value.decode('ascii')
    assert integer('>I') == 0x53364350 and integer('>I') == 1
    assert utf() == 'generation-attempt-v1'
    length = integer('>I'); assert length == len(data)-32-stream.tell()
    result = dict(parent=utf(), incumbent=utf(), generation=integer('>q'),
                  sourceMode=utf(), sourceRoot=utf(), settings=utf(), format=utf(), notice=utf())
    assert not stream.read()
    return result

def main():
    spec = importlib.util.spec_from_file_location('projection', Path(__file__).with_name('audit-brn-degree-projection.py'))
    projection = importlib.util.module_from_spec(spec); spec.loader.exec_module(projection)
    report = read(BASE/'wave6-trained-checkpoints.json')
    inventory = read(RAW/'trained-before-01.json')['files']
    prep = read(RAW/'owner-preparation-01.json')
    initial_inventory = read(RAW/'owner-before-gui-01.json')['files']
    stores = read(RAW/'trained-audit-01/stores.json')
    destination = RAW/'trained-frozen-01'
    assert not destination.exists()
    checks = []
    for row, store, prepared in zip(report['lineages'], stores[1:], prep['entries']):
        root = Path(row['root']); origin = row['origin']; source = origin['arm']['source']
        assert row['name'] == prepared['candidate'] == store['name']
        assert read(root/'degree-continuation-origin.json') == prepared['origin']
        for is_state, pattern, key in [(False,'network.*','networkSha256'),(True,'training.state','trainingSha256')]:
            before = projection.payload(source['root'],source['checkpoint'],pattern)
            after = projection.payload(root,origin['bootstrap'],pattern)
            assert projection.sha(before) == source[key] and projection.sha(after) == origin[key]
            if origin['arm']['projection'] == 'quadratic':
                old, new = projection.decode(before,is_state,5),projection.decode(after,is_state,7)
                assert all(n == o[:13073*8] for n,o in zip(new,old))
                if is_state: assert before[68:84] == after[68:84]
            else: assert before == after
        for relative in ['training-data/sources.json','training-data/configuration.json']:
            assert (root/relative).read_bytes() == (Path(source['root'])/relative).read_bytes()
        assert sha(root/'training-data/configuration.json') == source['corpusConfigurationSha256']
        # Training extends seek indexes; require the inherited points unchanged.
        old_ledger = read(Path(source['root'])/'training-data/cursors.json')
        ledger = read(root/'training-data/cursors.json')
        assert ledger['reservations'][:96] == old_ledger['reservations']
        seek = {}
        for p in (root/'training-data/seek').glob('*.json'):
            old = read(Path(source['root'])/'training-data/seek'/p.name); new = read(p)
            assert new['identity'] == old['identity'] and new['count'] == old['count'] == -1
            assert new['points'][:len(old['points'])] == old['points']
            text = new['identity']+'['+', '.join('Point[record='+str(v['record'])+', offset='+str(v['offset'])+', within='+str(v['within'])+']' for v in new['points'])+']'+str(new['count'])
            assert hashlib.sha256(text.encode()).hexdigest() == new['checksum']
            assert all(a['record']<b['record'] for a,b in zip(new['points'],new['points'][1:]))
            seek[p.name] = dict(sha256=sha(p),inheritedPoints=len(old['points']),currentPoints=len(new['points']))
        assert seek
        a = attempt(root/'generation-attempt.bin')
        assert a['generation'] == 128 and a['parent'] == row['latest']['parentId']
        assert a['incumbent'] == store['history']['records'][-1]['incumbent']
        # TrainerService's normal incremental path persists this format even
        # after settlement; the attempt is not itself unfinished model state.
        assert a['format'] == 'INCREMENTAL' and a['notice'] == ''
        assert not list(root.rglob('*partial*'))
        assert not list((root/'staging').iterdir())
        checks.append(dict(name=row['name'],retainedAttempt=a,seek=seek,
                           initialProjectionVerified=True,sourcePrefixReservations=96))
    for path, expected in inventory.items():
        p=Path(path); assert p.stat().st_size==expected['bytes'] and sha(p)==expected['sha256'],path
    destination.mkdir()
    models = [read(BASE/'wave6-model-plan.json')['models'][0]]
    for row in report['lineages']:
        root = Path(row['root']); frozen = destination/row['name']
        shutil.copytree(root,frozen)
        for p in root.rglob('*'):
            if p.is_file(): assert sha(p)==sha(frozen/p.relative_to(root))
        model = dict(name=row['name'],root=frozen.as_posix(),checkpoint=row['latest']['id'],networkSha256=row['latest']['networkSha256'])
        if 'RESCUBIC' in row['name']: model['cubicDeltaCache']=True
        models.append(model)
    write(BASE/'wave6-trained-model-plan.json',dict(id='wave6-trained-latest128-v1',models=models))
    write(BASE/'wave6-trained-admission.json',dict(status='passed',ownerInventory=str(RAW/'trained-before-01.json'),
        ownerFilesUnchanged=len(inventory),freeze=str(destination),checks=checks,
        selection='Latest128 prospectively selected in WAVE6; Best equals Latest in all four stores.',
        limitations=['Retained final attempt describes settled Gen128; no partial optimizer state exists.',
          'No observed persisted restart notice or incomplete generation; uninterrupted wall-clock execution cannot be proved.',
          'Quadratic inherits cubic-trained embedding/moments; shared retained values exact at Gen96, no fresh initialization.',
          'BRE has distinct historical exposure, source cursors/split and sparse optimizer semantics.',
          'Initial admission script assumed immutable seek files and INDEPENDENT attempts; both checks failed before freeze. Corrected against SourceReaders/TrainerService: indexes append, normal attempts are INCREMENTAL. No product defect or owner mutation.']))
    print('Verified and froze',len(models)-1,'trained lineages;',len(inventory),'owner files unchanged')

if __name__=='__main__': main()
