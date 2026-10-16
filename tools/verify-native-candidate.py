"""Reverify a candidate and relocated copies without starting any GUI window.

--work must be a NEW disposable directory; native Mac execution is required on Mac.
--peer compares the actual core/model/accepted configuration with another platform's
manifest. A mismatch fails and records the comparison, never infers equivalence.
"""
import argparse
import json
from pathlib import Path
import shutil
import sys

import native_packaging as p


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--candidate', type=Path, required=True)
    parser.add_argument('--work', type=Path, required=True)
    parser.add_argument('--jdk', type=Path, required=True, help='Native Java 21 used only to compile the headless verifier')
    parser.add_argument('--peer', type=Path, help='Other candidate build-manifest.json')
    args = parser.parse_args()
    work = args.work.resolve(); work.mkdir(parents=True, exist_ok=False)
    output = args.candidate.resolve()
    result = {'status': 'FAILED', 'candidate': str(output), 'work': str(work), 'copies': {}}
    try:
        receipt = json.loads((output / 'candidate.json').read_text())
        p.require(receipt['status'] == 'VERIFIED_LOCAL_CANDIDATE' and not receipt['officialRelease'], 'Unverified candidate')
        image = output / receipt['image']; archive = output / receipt['archive']
        p.require(p.sha(archive) == receipt['archiveSha256'], 'Archive receipt hash differs')
        p.require(p.sha(image / 'build-manifest.json') == receipt['manifestSha256'], 'Manifest receipt hash differs')
        manifest = p.verify_integrity(image, archive)
        p.host_check(manifest['platform'], args.jdk.resolve())
        context = {'platform': manifest['platform'], 'jdk': str(args.jdk.resolve())}
        copied = work / 'copied image with spaces' / image.name
        copied.parent.mkdir(); shutil.copytree(image, copied)
        p.extract_verified(archive, work / 'archive extracted with spaces')
        extracted = work / 'archive extracted with spaces' / image.name
        for label, current in [('copied', copied), ('extracted', extracted)]:
            p.verify_integrity(current)
            result['copies'][label] = p.verify_runtime(context, manifest['application'], current, work)
            p.verify_integrity(current)
        result['source'] = manifest['source']
        result['engine'] = manifest['engine']
        result['archiveSha256'] = p.sha(archive)
        if args.peer:
            peer = json.loads(args.peer.read_text())
            fields = ('coreJarSha256', 'model', 'productionSearch', 'coreInputs')
            result['peerComparison'] = {key: {'same': peer['engine'][key] == manifest['engine'][key],
                'current': manifest['engine'][key], 'peer': peer['engine'][key]} for key in fields}
            result['peerComparison']['sourceRevision'] = {
                'same': peer['source']['revision'] == manifest['source']['revision'],
                'current': manifest['source']['revision'], 'peer': peer['source']['revision']}
            p.require(all(v['same'] for v in result['peerComparison'].values()),
                      'Cross-candidate engine identity differs; investigate recorded hashes and build inputs. No parity claimed.')
        result['status'] = 'passed'
    finally:
        p.write_json(work / 'verification.json', result)
    print(json.dumps({'status': result['status'], 'evidence': str(work / 'verification.json')}))


if __name__ == '__main__':
    main()
