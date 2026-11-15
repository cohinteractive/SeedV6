"""Re-envelope the accepted checkpoint without changing a single parameter byte.

Only the exact accepted Phase 2 artifact is eligible. This is not a general
experimental-format importer. The runtime accepts S6CNU003, never S6P2N002.
"""
import argparse
import hashlib
import json
from pathlib import Path
import struct

SOURCE_SHA256 = 'defaf21da465d6f63fc13a2299299e927029d82558d663c7bbc801e7c35e9d1c'
PAYLOAD_SHA256 = '969dd242ad58c5927d53d87b6c26d0f898a6e9cccc93b5ae0331cc7cabb239be'


def convert(source):
    if len(source) != 1970524 or hashlib.sha256(source).hexdigest() != SOURCE_SHA256:
        raise ValueError('Not the accepted 128-unit checkpoint-64 artifact')
    if source[:8] != b'S6P2N002' or struct.unpack_from('<9I', source, 8) != (2, 0, 7680, 128, 0, 255, 64, 400, 1970448):
        raise ValueError('Unexpected architecture or scaling')
    payload = source[76:]
    digest = hashlib.sha256(payload).digest()
    if source[44:76] != digest or digest.hex() != PAYLOAD_SHA256:
        raise ValueError('Payload checksum mismatch')
    output = struct.unpack_from('<2048h', payload, (7680 * 128 + 128) * 2)
    if any(abs(weight) > 127 for weight in output):
        raise ValueError('Unbounded output weights')
    return b'S6CNU003' + struct.pack('<8I', 3, 7680, 128, 10, 255, 64, 400, len(payload)) + digest + payload


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('source', type=Path)
    parser.add_argument('destination', type=Path)
    args = parser.parse_args()
    data = convert(args.source.read_bytes())
    with args.destination.open('xb') as stream:
        stream.write(data)
    assert args.destination.read_bytes() == data
    print(json.dumps(dict(source_sha256=SOURCE_SHA256, payload_sha256=PAYLOAD_SHA256,
                         production_sha256=hashlib.sha256(data).hexdigest(), bytes=len(data))))


if __name__ == '__main__':
    main()
