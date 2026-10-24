"""Hash-only historical audit. Never parse historical game results or run analyzers."""
import hashlib
import json
from pathlib import Path
import subprocess
from datetime import datetime, timezone

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
OLD = HERE.parent / 'p3-sr034-2026-10-24'


def digest(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def write_new(path, value):
    with Path(path).open('x', encoding='utf8', newline='\n') as stream:
        json.dump(value, stream, indent=2, allow_nan=False)
        stream.write('\n')


def audit():
    binding_path = OLD / 'confirmation-v1-binding.json'
    binding = json.loads(binding_path.read_text())
    checked = {}
    for category in ('files', 'externalFiles'):
        for name, expected in binding[category].items():
            path = ROOT / name
            actual = digest(path)
            assert actual == expected, 'Frozen input mismatch: ' + name
            checked[name] = actual
    evidence = {}
    for number in range(1, 5):
        # These operational receipts are deliberately outcome-blind. Do not read run.json or PGN.
        path = OLD / f'sr034-confirmation-v1-b{number:02}-summary.json'
        receipt = json.loads(path.read_text())
        assert receipt['bindingSha256'] == digest(binding_path)
        for name, expected in receipt['evidence'].items():
            actual = digest(OLD / name)
            assert actual == expected, 'Historical raw mismatch: ' + name
            evidence[name] = actual
    for name in ('confirmation-v1-disposition.json', 'confirmation-clock-tail-v1.json'):
        evidence[name] = digest(OLD / name)
    return {'status': 'passed', 'observedUtc': datetime.now(timezone.utc).isoformat(),
            'head': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip(),
            'historicalBindingSha256': digest(binding_path), 'boundInputCount': len(checked),
            'historicalRawAndReceiptCount': len(evidence), 'historicalEvidence': evidence,
            'runtimeJars': {p: sha for p, sha in checked.items()
                            if ('runtime-production-v2' in p or 'runtime-candidate-v2' in p)
                            and p.endswith(('engine-core.jar', 'app-uci.jar', 'research.jar'))},
            'historicalScores': 'Unopened; raw artifacts read only as bytes for hashing',
            'scriptSha256': digest(__file__)}


if __name__ == '__main__':
    result = audit()
    write_new(HERE / 'bootstrap-input-audit-v1.json', result)
    print(json.dumps({k: v for k, v in result.items() if k not in ('historicalEvidence', 'runtimeJars')}))
