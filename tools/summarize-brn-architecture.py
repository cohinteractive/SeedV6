"""Collect compact factual BRN architecture evidence without opening sealed test data."""
import argparse
import hashlib
import json
from pathlib import Path


def sha(path):
    with path.open('rb') as source:
        return hashlib.file_digest(source, 'sha256').hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('root', type=Path)
    parser.add_argument('output', type=Path)
    parser.add_argument('--compact', action='store_true',
                        help='Emit a hashed evidence index; full metrics remain in referenced raw results.')
    args = parser.parse_args()
    runs = []
    for path in sorted(args.root.glob('*.execution.json')):
        execution = json.loads(path.read_text(encoding='utf-8'))
        row = {k: execution[k] for k in ['command', 'head', 'status', 'elapsedSeconds',
               'startedUtc', 'endedUtc', 'timeoutSeconds', 'java', 'platform', 'runnerSha256',
               'processMemory', 'error', 'note'] if k in execution}
        row.update(receipt=str(path), receiptSha256=sha(path))
        for name in ['sourceSha256', 'compiledSha256']:
            hashes = execution[name]
            row[name + 'ManifestDigest'] = hashlib.sha256(
                json.dumps(hashes, sort_keys=True, separators=(',', ':')).encode()).hexdigest()
            row[name] = {k: v for k, v in hashes.items() if any(
                key in k for key in ['BrnArchitecture', 'BrnEnergy', 'BrnCubic', 'BrnSpline', 'BrnContext', 'BrnLogic', 'BrnTuple', 'BrnResearchData',
                                    'Brn3Trainer', 'NnueTrainer', 'NnueMaterialResidualTrainer'])}
        run = args.root / path.name.removesuffix('.execution.json')
        result = run / 'result.json'
        if result.exists():
            raw = json.loads(result.read_text(encoding='utf-8'))
            row['resultPath'] = str(result)
            row['resultSha256'] = sha(result)
            if 'datasetManifest' in raw:
                row['datasetPayloadSha256'] = raw.pop('datasetManifest')['payloadSha256']
            row['result'] = raw
            row['persistedBytes'] = sum(p.stat().st_size for p in run.rglob('*') if p.is_file())
            pairs = []
            for pair_path in sorted(run.glob('pair-*.json')):
                pair = json.loads(pair_path.read_text(encoding='utf-8'))
                pair['rawSha256'] = sha(pair_path)
                for color in ['white', 'black']:
                    # Full move lists remain in hashed raw records; retain all scoring/resource evidence.
                    pair[color].pop('moves', None)
                pairs.append(pair)
            if pairs:
                row['pairs'] = pairs
        if args.compact:
            # Avoid duplicating whole source maps, training traces and game records
            # in the repository index. Their immutable raw files remain authoritative.
            row.pop('sourceSha256', None)
            row.pop('compiledSha256', None)
            if 'result' in row:
                raw = row.pop('result')
                row['resultSummary'] = {
                    key: value for key, value in raw.items()
                    if value is None or isinstance(value, (str, int, float, bool))
                }
                for key in ['selectedValidation', 'lastValidation', 'gen0Validation']:
                    if key in raw:
                        row['resultSummary'][key] = raw[key]
            if 'pairs' in row:
                row['pairFileSha256'] = {
                    pair_path.name: sha(pair_path)
                    for pair_path in sorted(run.glob('pair-*.json'))
                }
                row.pop('pairs')
        runs.append(row)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    evidence = {'schema': 'brn-architecture-evidence-v2' if args.compact else 'brn-architecture-evidence-v1',
                'runs': runs}
    if args.compact:
        evidence.update(collectorSha256=sha(Path(__file__)),
                        scope='Compact index, not complete metrics. Referenced immutable receipts/results and '
                              'pair files retain full commands, source/compiled maps, curves and outcomes; '
                              'programme analysis documents retain comparative interpretation.')
    args.output.write_text(json.dumps(evidence,
                                     indent=2) + '\n', encoding='utf-8')
    if args.compact:
        print(f'Indexed {len(runs)} receipts; {args.output.stat().st_size} bytes')
        return
    for row in runs:
        result = row.get('result', {})
        if 'selectedValidation' in result:
            print(Path(row['receipt']).stem, row['status'], 'epoch', result['selectedEpoch'],
                  'loss', result['selectedValidation']['outcomeHalfMse'],
                  'trainingSeconds', result['events'][-1]['trainingSeconds'])


if __name__ == '__main__':
    main()
