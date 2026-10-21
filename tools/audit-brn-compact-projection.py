"""Independently verify actual compact continuation bytes; read-only owner stores."""
import argparse
import hashlib
import json
import struct
import zlib
from pathlib import Path


def digest(data):
    return hashlib.sha256(data).hexdigest()


def payload(root, checkpoint, pattern):
    files = list((Path(root) / 'checkpoints' / checkpoint).glob(pattern))
    assert len(files) == 1, files
    return files[0].read_bytes()


def decode(data, state, width, wire):
    count = 769 + 770 * width
    assert struct.unpack('>Q7i', data[:36]) == (
        0x53364E5853543031 if state else 0x53364E5852543031,
        1, wire, count, width, 1, 1, 64)
    assert data[36:68].hex() == '5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7'
    offset, arrays = (84, 3) if state else (68, 1)
    assert len(data) == offset + arrays * count * 8 + 4
    assert zlib.crc32(data[:-4]) == struct.unpack('>I', data[-4:])[0]
    return [[data[offset+(a*count+i)*8:offset+(a*count+i+1)*8]
             for i in range(count)] for a in range(arrays)]


def audit(report):
    results = []
    for entry in report['entries']:
        origin = entry['origin']; source = origin['arm']['source']
        row = {'candidate': entry['candidate'], 'checkpoint': entry['latest']}
        for state, pattern, key in [(False, 'network.*', 'networkSha256'),
                                    (True, 'training.state', 'trainingSha256')]:
            before = payload(source['root'], source['checkpoint'], pattern)
            after = payload(entry['root'], entry['latest'], pattern)
            assert digest(before) == source[key] and digest(after) == origin[key]
            components = origin['arm']['components']
            if components:
                assert components == [2, 3, 5, 9]
                old = decode(before, state, 16, 5); new = decode(after, state, 4, 6)
                mapping = list(range(769))
                mapping += [769 + feature*16+r for feature in range(768) for r in components]
                mapping += [769+768*16+degree*16+r for degree in range(2) for r in components]
                assert len(mapping) == 3849
                for a in range(len(new)):
                    assert new[a] == [old[a][i] for i in mapping]
                if state:
                    assert before[68:84] == after[68:84]
                    step, rate = struct.unpack('>qd', after[68:84])
                    assert step == origin['inheritedStep'] and rate == origin['arm']['rate']
                    row.update(step=step, rate=rate, exactMappedValues=3*3849)
            else:
                assert before == after
            row[key] = digest(after); row['stateBytes' if state else 'modelBytes'] = len(after)
        results.append(row)
    return {'status': 'passed', 'scope': 'Independent byte/CRC/layout and exact parameter/moment projection; unchanged controls', 'entries': results}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('report', type=Path); parser.add_argument('new_output', type=Path)
    args = parser.parse_args()
    result = audit(json.loads(args.report.read_text(encoding='utf-8-sig')))
    with args.new_output.open('x', encoding='utf-8') as out:
        json.dump(result, out, indent=2); out.write('\n')
    print(result['status'])
