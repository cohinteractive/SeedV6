"""Read-only independent audit of a prepared degree-control continuation.

Checks exact retained parameter/Adam bytes, step/rate, source bindings and data
inheritance. Run before owner training; output is exclusive and never rewrites stores.
Java's separate lineage audit verifies codecs, references and runtime/optimizer parity.
"""
import argparse
import hashlib
import json
import struct
import zlib
from pathlib import Path


def sha(data):
    return hashlib.sha256(data).hexdigest()


def payload(root, checkpoint, pattern):
    files = list((Path(root) / 'checkpoints' / checkpoint).glob(pattern))
    assert len(files) == 1, files
    return files[0].read_bytes()


def decode(data, state, wire):
    count = {5: 13089, 6: 3849, 7: 13073, 8: 3845, 9: 3845, 10: 3849, 11: 3849, 12: 3853, 13: 3853}[wire]
    width = 4 if wire in (6, 8, 9, 10, 11, 12, 13) else 16
    assert struct.unpack('>Q7i', data[:36]) == (
        0x53364E5853543031 if state else 0x53364E5852543031,
        1, wire, count, width, 1, 1, 64)
    assert data[36:68].hex() == '5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7'
    offset, arrays = (84, 3) if state else (68, 1)
    assert len(data) == offset + arrays * count * 8 + 4
    assert zlib.crc32(data[:-4]) == struct.unpack('>I', data[-4:])[0]
    return [data[offset+a*count*8:offset+(a+1)*count*8] for a in range(arrays)]


def audit(report):
    assert report['wave'] in ('brn-successor-2026-10-21-wave6', 'brn-successor-2026-10-21-wave7', 'brn-successor-2026-10-22-wave8', 'brn-successor-2026-10-22-wave9')
    calibrated = report['wave'].endswith('wave9')
    fixed = report['wave'].endswith('wave8')
    compact = report['wave'].endswith('wave7')
    generation, step_expected = (168, 65536) if calibrated else (160, 1310720) if fixed else (128, 1048576) if compact else (96, 786432)
    additional = 8 if fixed or calibrated else 32
    source_wire, target_wire, retained = (6, 8, 3845) if compact else (5, 7, 13073)
    architecture = 'BRN_GEN32_QUADRATIC_R4' if compact else 'BRN_GEN32_QUADRATIC'
    rows = []
    for entry in report['entries']:
        origin = entry['origin']; arm = origin['arm']; source = arm['source']
        root, old_root = Path(entry['root']), Path(source['root'])
        assert root.resolve() != old_root.resolve()
        assert entry['latest'] == origin['bootstrap'] and entry['latestGeneration'] == source['generation'] == generation
        assert entry['additionalGenerations'] == additional
        row = dict(candidate=entry['candidate'], root=str(root), checkpoint=entry['latest'],
                   architecture=entry['architecture'], projection=arm['projection'])
        for state, pattern, key in [(False, 'network.*', 'networkSha256'),
                                    (True, 'training.state', 'trainingSha256')]:
            before = payload(old_root, source['checkpoint'], pattern)
            after = payload(root, entry['latest'], pattern)
            assert sha(before) == source[key] and sha(after) == origin[key]
            if calibrated and arm['projection'].startswith('calibrated-'):
                kind = arm['projection'].removeprefix('calibrated-')
                wire = dict(affine=11, quadratic=12, cubic=13)[kind]
                assert entry['architecture'] == 'BRN_GEN32_CAL_'+kind.upper()+'_R4'
                old, new = decode(before, state, 10), decode(after, state, wire)
                assert new[0] == old[0][:3845*8] + bytes((4 if kind=='affine' else 8)*8)
                if state:
                    assert all(a == bytes(len(a)) for a in new[1:])
                    source_step, source_rate = struct.unpack('>qd', before[68:84])
                    step, rate = struct.unpack('>qd', after[68:84])
                    assert source_step == origin['inheritedStep'] == step_expected
                    assert step == 0 and source_rate == rate == arm['rate'] == .001
                    row.update(step=step, sourceStep=source_step, rate=rate, exactRetainedWeights=3845,
                               optimizer='New zero calibrated-head Adam; source cubic heads omitted and source store preserved',
                               zeroMomentValues=sum(len(a)//8 for a in new[1:]))
            elif fixed and arm['projection'].startswith('fixed-'):
                cubic = arm['projection'] == 'fixed-cubic-heads'
                assert arm['projection'] in ('fixed-cubic-heads', 'fixed-quadratic-heads')
                assert entry['architecture'] == ('BRN_GEN32_FIXED_CUBIC_R4' if cubic else 'BRN_GEN32_FIXED_QUADRATIC_R4')
                old, new = decode(before, state, 8), decode(after, state, 10 if cubic else 9)
                assert new[0] == old[0] + (bytes(32) if cubic else b'')
                if state:
                    assert all(a == bytes(len(a)) for a in new[1:])
                    source_step, source_rate = struct.unpack('>qd', before[68:84])
                    step, rate = struct.unpack('>qd', after[68:84])
                    assert source_step == origin['inheritedStep'] == step_expected
                    assert step == 0 and source_rate == rate == arm['rate'] == .001
                    row.update(step=step, sourceStep=source_step, rate=rate, exactRetainedWeights=3845,
                               optimizer='New zero head-only Adam; source state preserved',
                               zeroMomentValues=sum(len(a)//8 for a in new[1:]))
            elif arm['projection'] == 'quadratic':
                assert entry['architecture'] == architecture
                old, new = decode(before, state, source_wire), decode(after, state, target_wire)
                assert all(n == o[:retained*8] for n, o in zip(new, old))
                if state:
                    assert before[68:84] == after[68:84]
                    step, rate = struct.unpack('>qd', after[68:84])
                    assert step == origin['inheritedStep'] == step_expected and rate == arm['rate'] == .001
                    row.update(step=step, rate=rate, exactRetainedWeightAndMomentValues=3*retained)
            else:
                assert arm['projection'] == 'exact' and before == after
            row[key] = sha(after)
            row['stateBytes' if state else 'modelBytes'] = len(after)
        inherited = {}
        for name in ['sources.json', 'cursors.json', 'configuration.json']:
            old = (old_root/'training-data'/name).read_bytes()
            new = (root/'training-data'/name).read_bytes()
            assert old == new
            inherited[name] = sha(new)
        assert inherited['cursors.json'] == origin['sourceLedgerSha256']
        assert inherited['sources.json'] == origin['selectionSha256']
        assert inherited['configuration.json'] == source['corpusConfigurationSha256']
        selection = json.loads((root/'training-data/sources.json').read_text(encoding='utf-8-sig'))
        ledger = json.loads((root/'training-data/cursors.json').read_text(encoding='utf-8-sig'))
        assert ledger['next'] == entry['sourceOffsets']
        assert len(ledger['reservations']) == generation and all(r['status'] == 'COMPLETED' for r in ledger['reservations'])
        seek = {}
        for item in selection['sources']:
            if item['format'] == 'LICHESS_PZSTD' and ledger['next'].get(item['identity'], 0) > 0:
                relative = Path('training-data/seek') / (item['identity']+'.json')
                old, new = (old_root/relative).read_bytes(), (root/relative).read_bytes()
                assert old == new
                seek[str(relative)] = sha(new)
        assert seek, 'The admitted source mix must inherit its CP seek index'
        assert not (root/'training-session.json').exists()
        assert not (root/'generation-attempt.bin').exists()
        assert not list(root.rglob('*partial*'))
        assert not list((root/'history').glob('*'))
        assert len(list((root/'checkpoints').iterdir())) == 1
        row.update(inheritedMetadata=inherited, inheritedSeek=seek, generation=generation,
                   addedGenerations=0, nextGeneration=generation+1, targetGeneration=generation+additional)
        rows.append(row)
    return dict(status='passed', scope='Independent exact byte/CRC/layout projection and source/data inheritance audit',
                entries=rows, limitations=['No substantive training or strength verification.',
                    'GUI settings, checkpoint references and source readiness are verified separately by Java/native checks.'])


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('report', type=Path); parser.add_argument('new_output', type=Path)
    args = parser.parse_args()
    result = audit(json.loads(args.report.read_text(encoding='utf-8-sig')))
    result['preparationReport'] = str(args.report)
    result['preparationReportSha256'] = sha(args.report.read_bytes())
    with args.new_output.open('x', encoding='utf-8', newline='\n') as out:
        json.dump(result, out, indent=2); out.write('\n')
    print(result['status'])
