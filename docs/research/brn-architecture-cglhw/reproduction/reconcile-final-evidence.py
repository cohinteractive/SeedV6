"""Read back retained evidence, current bindings and the compact final index."""
from collections import Counter
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET

root = Path('docs/research/brn-architecture-cglhw')
rawroot = Path('app/build/research/brn-architecture')
def read(path): return json.loads(Path(path).read_text(encoding='utf-8'))
def sha(path):
    with Path(path).open('rb') as f: return hashlib.file_digest(f, 'sha256').hexdigest()
def manifest(value): return hashlib.sha256(json.dumps(value, sort_keys=True, separators=(',', ':')).encode()).hexdigest()
def git(*args): return subprocess.check_output(['git', '-c', 'safe.directory=C:/projects/seed/java/seedv6', *args], text=True).strip()

index = read(root/'EXPERIMENTS.json')
assert index['schema'] == 'brn-architecture-evidence-v2'
assert index['collectorSha256'] == sha('tools/summarize-brn-architecture.py')
rows = {Path(v['receipt']).resolve(): v for v in index['runs']}
assert len(rows) == len(index['runs']) == 1707
assert set(rows) == {p.resolve() for p in rawroot.glob('*.execution.json')}
pair_count = 0
for path, row in rows.items():
    assert sha(path) == row['receiptSha256']
    receipt = read(path)
    for key in ['command', 'status', 'head', 'startedUtc', 'endedUtc', 'elapsedSeconds']:
        if key in row: assert row[key] == receipt[key]
    for key in ['sourceSha256', 'compiledSha256']:
        assert row[key+'ManifestDigest'] == manifest(receipt[key])
    if 'resultPath' in row:
        assert sha(row['resultPath']) == row['resultSha256']
        result = read(row['resultPath'])
        for key, value in row['resultSummary'].items(): assert value == result[key]
    if 'pairFileSha256' in row:
        folder = Path(row['resultPath']).parent
        assert set(row['pairFileSha256']) == {p.name for p in folder.glob('pair-*.json')}
        for name, digest in row['pairFileSha256'].items(): assert sha(folder/name) == digest
        pair_count += len(row['pairFileSha256'])
prior = read(rawroot/'final-index-prior-v1.json')
assert all(Path(r['receipt']).resolve() in rows and rows[Path(r['receipt']).resolve()]['receiptSha256'] == r['receiptSha256'] for r in prior['runs'])

frozen_count = 0
for name, expected in [('z01-final-match-plan.json', 'e90f0f04fb2bf33019946f93e8c85391f1d02ab5626e60d36347f76ef6858529'),
                       ('z01-final-quality-plan.json', '296e6b6ffbc5ed3a022952e854493cb0bf278dac8d4e018635673bf5429182cd')]:
    assert sha(root/name) == expected
    plan = read(root/name)
    for files in [plan['frozenFiles'], *plan['frozenCode'].values()]:
        for path, digest in files.items():
            assert sha(path) == digest, path
            frozen_count += 1

evidence_names = ['z01-long-results.json', 'z01-long-crosscheck.json', 'z01-nested-results.json',
                  'z01-engine-scaling-validation.json', 'z01-qsearch-crosscheck.json',
                  'z01-final-runtime-crosscheck.json', 'z01-final-match-crosscheck.json',
                  'z01-final-quality-results.json', 'c02-validation.json']
for name in evidence_names: assert read(root/name)['allChecksPassed'], name
match = read(root/'z01-final-match-results.json')
cross = read(root/'z01-final-match-crosscheck.json')
assert cross['scoresSha256'] == sha(root/'z01-final-match-results.json')
assert cross['resourcesSha256'] == sha(root/'z01-final-match-resources.json')
assert cross['recordedGames'] == 3072 and len(match['comparisons']) == 6
assert all(v['complete'] and v['recordedPairs'] == v['plannedPairs'] == 256 and v['missingOrIncompletePairs'] == 0 for v in match['comparisons'].values())
quality = read(root/'z01-final-quality-results.json')
assert len(quality['profiles']) == 22 and quality['totalPositionVisits'] == 720896
assert quality['pairRepresentationMaximumMetricDifferences'] == {'211': 0, '337': 0}
assert quality['executionPlanSha256'] == sha(root/'z01-final-quality-execution-plan.json')
for value in quality['profiles'].values():
    p = value['provenance']; folder = Path(p['run'])
    assert sha(folder/'result.json') == p['resultSha256']
    assert sha(str(folder)+'.execution.json') == p['receiptSha256']

validation = read(root/'c02-validation.json')
for family in ['java', 'python']:
    item = validation[family]
    assert sha(item['path']) == item['sha256']
    receipt = read(item['path'])
    assert receipt['allChecksPassed']
    for path, digest in receipt['sourceSha256'].items(): assert sha(path) == digest, path
java = read(validation['java']['path'])
assert java['returnCode'] == 0 and sum(s['tests'] for s in java['suites']) == 28
assert all(s['failures'] == s['errors'] == s['skipped'] == 0 for s in java['suites'])
assert sha(java['stdout']) == java['stdoutSha256']
xmls = list(Path(validation['java']['path']).parent.rglob('TEST-*.xml'))
assert len(xmls) == len(java['suites']) == 11
assert {sha(p) for p in xmls} == {s['xmlSha256'] for s in java['suites']}
assert sum(int(ET.parse(p).getroot().attrib['tests']) for p in xmls) == 28
scaling = read(root/'z01-engine-scaling-validation.json')
for item in scaling['newTestReceipts']:
    assert sha(item['path']) == item['sha256']
    assert read(item['path'])['allChecksPassed']
assert scaling['cumulativeDistinctTests'] == {'java': 84, 'python': 28}

assert git('rev-parse', 'HEAD') == '121e4a607367903d7c93af5a098da791a71bc44f'
assert not git('diff', 'HEAD', '--', 'app/src/main', 'VERSION_STATE.txt', 'app/build.gradle')
assert not git('ls-files', '--others', '--exclude-standard', 'app/src/main')
assert read('VERSION_STATE.txt') == {'major': 0, 'minor': 0, 'patch': 0, 'build': 34}
subprocess.run(['git', '-c', 'safe.directory=C:/projects/seed/java/seedv6', 'diff', '--check'], check=True)
for name in ['CONTRACT.md','E000.md','E001.md','E002.md','B01.md','C01.md','C02.md','C02-RESULTS.md',
             'D01.md','E01.md','F01.md','G01.md','Z01.md','SCORECARD.md','Z01-FINAL-RESULTS.md','RECOMMENDATION.md']:
    assert (root/name).is_file() and (root/name).stat().st_size > 0

report = {'schema': 'brn-architecture-final-reconciliation-v1', 'allChecksPassed': True,
          'observedUtc': datetime.now(timezone.utc).isoformat(), 'auditorSha256': sha(__file__),
          'indexedReceipts': len(rows), 'priorIndexedReceiptsPreserved': len(prior['runs']),
          'hashedPairFiles': pair_count, 'frozenFileBindingChecks': frozen_count,
          'executionStatuses': dict(Counter(r['status'] for r in rows.values())),
          'indexSha256': sha(root/'EXPERIMENTS.json'), 'indexBytes': (root/'EXPERIMENTS.json').stat().st_size,
          'retainedPriorIndex': str(rawroot/'final-index-prior-v1.json'),
          'retainedPriorIndexSha256': sha(rawroot/'final-index-prior-v1.json'),
          'currentTargetedJavaSourceBindingsVerified': len(java['sourceSha256']),
          'latestFocusedJavaTests': 28, 'latestFocusedJavaXmlSuites': 11,
          'recordedCumulativeDistinctTests': scaling['cumulativeDistinctTests'],
          'evidenceSha256': {name: sha(root/name) for name in evidence_names + ['z01-final-match-results.json',
                                'z01-final-match-resources.json','Z01-FINAL-RESULTS.md','RECOMMENDATION.md']},
          'preCommitHead': git('rev-parse','HEAD'), 'preCommitStatus': git('status','--short'),
          'productionSourceAndBuildDefinitionUnchanged': True, 'build': 34,
          'versionFinalization': 'Non-blocking initial invocation warning; no active token or verified bump; no replacement invocation.',
          'rootJournalPresent': Path('CODEXLOG_CURRENT.md').exists(),
          'scope': 'Read-back integrity/coverage checks and recorded test evidence, not a new unit-test or full-suite run. '
                   'Scientific interpretation and numbered completion decisions are in COMPLETION-AUDIT and RECOMMENDATION. '
                   'Commit packaging follows this pre-commit evidence boundary; historical receipts retain their original HEAD and source hashes.'}
out = root/'z01-final-reconciliation.json'
with out.open('x',encoding='utf-8',newline='\n') as f: json.dump(report,f,indent=2);f.write('\n')
print(json.dumps({k:v for k,v in report.items() if k not in ['preCommitStatus','evidenceSha256']},indent=2))
