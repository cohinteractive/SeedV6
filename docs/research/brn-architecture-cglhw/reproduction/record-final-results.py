"""Record the completed frozen final allocations without changing their inputs."""
import hashlib
import json
from pathlib import Path

root = Path('docs/research/brn-architecture-cglhw')
def read(name): return json.loads((root / name).read_text(encoding='utf-8'))
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def pct(value): return f'{100 * value:.2f}%'
def interval(values): return f'[{pct(values[0])}, {pct(values[1])}]'
matches = read('z01-final-match-results.json')
resources = read('z01-final-match-resources.json')
cross = read('z01-final-match-crosscheck.json')
quality = read('z01-final-quality-results.json')
assert cross['allChecksPassed'] and quality['allChecksPassed']
assert resources['allBindingChecksPassed'] and resources['allPairFilesPresent']
assert cross['scoresSha256'] == sha(root / 'z01-final-match-results.json')
assert cross['resourcesSha256'] == sha(root / 'z01-final-match-resources.json')
assert all(v['complete'] and v['missingOrIncompletePairs'] == 0 for v in matches['comparisons'].values())
names = {'compiled-600-vs-brn': 'Fresh BRN', 'compiled-600-vs-nnue-v2': 'Current NNUE v2',
         'compiled-600-vs-nnue-strict': 'Strict NNUE', 'compiled-600-vs-material': 'Cheap material',
         'compiled-600-vs-native': 'Captured native BRN', 'compiled-600-vs-compiled-gen0': 'Compiled own Gen0'}
lines = ['# Final confirmation results', '', '2026-10-07. All frozen allocations and their audits completed. '
         'This is research evidence, not production adoption or user acceptance.', '',
         '## Fixed-time complete-engine strength', '',
         'All 768 batches / 3,072 games completed with scored outcomes: no caps, missing games or failures. '
         'Maximum observed game length was 654 plies. The independent 128-opening sample was used for both '
         'source/order runs and both colours, giving 512 games per comparison. Search used the unchanged '
         'production exact/PVS static-leaf path, one worker, private 4 MiB TT and 100 ms per move. '
         'No interim outcomes were inspected; selection remained frozen.', '',
         '| Opponent | W / D / L | Score | Run A / B | Two-way bootstrap 95% | Opening-only 95% | Simultaneous six 95% lower |',
         '|---|---:|---:|---:|---:|---:|---:|']
for key, v in matches['comparisons'].items():
    scores = ' / '.join(pct(v['perInitialization'][s]['completedPairPointScore']) for s in ['211', '337'])
    lines.append(f"| {names[key]} | {' / '.join(map(str, v['winsDrawsLossesCompletePairs']))} | {pct(v['completedPairPointScore'])} | {scores} | {interval(v['initializationAndOpeningBootstrap95'])} | {interval(v['openingClusterBootstrap95'])} | {pct(cross['comparisons'][key]['conditionalBoundedPairSimultaneousSix95LowerWithMissingAdverse'])} |")
lines += ['', 'All six comparisons pass the frozen primary bootstrap strength criterion (lower > 50%). '
          'Those intervals are approximate and unadjusted, with only two source/order runs. The conservative '
          'simultaneous bounded-pair sensitivity check clears 50% for five controls, including cheap material '
          '(50.58%), but not captured native BRN (41.89%). Its single-comparison bound is 44.75%, also just '
          'below the 45% noninferiority threshold. Thus the native result is promising under the primary '
          'analysis, not robustly established under the conservative sensitivity analysis. Do not claim '
          'an unconditional all-controls win. Bounded checks condition on the fixed models and independent '
          'generated opening clusters; neither they nor two source intervals establish corpus-game independence.', '',
          'All-planned adverse score bounds equal the point estimates because every game was scored. '
          'No extension or outcome-dependent rerun was added.', '', '## Actual search and process resources', '',
          '| Opponent | Candidate / opponent NPS (millions) | Candidate / opponent ms per played move |',
          '|---|---:|---:|']
for key in names:
    nps, ms = [], []
    for actor in ['candidate', 'opponent']:
        rows = [g['actors'][actor] for g in resources['groups'].values() if g['comparison'] == key]
        nps.append(sum(r['nodesAllRecordedGames'] for r in rows) / sum(r['secondsAllRecordedGames'] for r in rows) / 1e6)
        ms.append(1000 * sum(r['secondsInGamesWithUnambiguousMoveAccounting'] for r in rows) / sum(r['movesInGamesWithUnambiguousMoveAccounting'] for r in rows))
    lines.append(f'| {names[key]} | {nps[0]:.3f} / {nps[1]:.3f} | {ms[0]:.3f} / {ms[1]:.3f} |')
memory = [p['processMemory'] for p in resources['provenance']]
assert all(m['readFailures'] == 0 and m['postExitSampleSucceeded'] for m in memory)
lines += ['', f"Java-process time was {cross['processSeconds']:.6f} seconds ({cross['processSeconds']/3600:.4f} hours), excluding outer-controller overhead. "
          f"Observed whole-JVM peak working set ranged {min(m['peakWorkingSetBytes'] for m in memory):,}–{max(m['peakWorkingSetBytes'] for m in memory):,} bytes; "
          f"private commit {min(m['peakPrivateCommitBytes'] for m in memory):,}–{max(m['peakPrivateCommitBytes'] for m in memory):,} bytes. "
          'All post-exit counter reads succeeded and no memory read failed. Per-run/actor mean move times '
          'ranged 95.777–97.278 ms; early search completion is permitted. Per-game timing distributions '
          'remain in the resource JSON and are not per-move percentiles. Different evaluators visit different '
          'trees, so these are observed engine throughputs, not identical-node microbenchmarks. Process '
          'memory includes JVM/data/checkpoint allocations and is not isolated model memory.', '',
          '## Sealed prediction and calibration', '',
          'All 22 profiles evaluated the full 32,768-position held-out population on each range: '
          f"{quality['totalPositionVisits']:,} visits, {quality['distinctSealedPositions']:,} distinct positions, "
          f"{quality['processSeconds']:.6f} process seconds. All frozen source/class/runner, command, Java/platform, "
          'model/metadata, finite-metric, population, strata and reliability checks passed. Original versus '
          f"compiled pair metrics and reliability differed by exactly {quality['pairRepresentationMaximumMetricDifferences']['211']} "
          f"on A and {quality['pairRepresentationMaximumMetricDifferences']['337']} on B in this sample. No models or gains were selected from these results.", '',
          '| Profile | Raw half-MSE | Balanced half-MSE | Search half-MSE | Raw / search bin gap | CP MAE (pawns) | Residual RMS (pawns) | Sign mistakes / population |',
          '|---|---:|---:|---:|---:|---:|---:|---:|']
for key, v in quality['profiles'].items():
    m, rel = v['metrics'], v['reliability']
    assert m['saturatedSearchScores'] == 0
    lines.append(f"| {key} | {m['outcomeHalfMse']:.6f} | {m['balancedHalfMse']:.6f} | {m['searchHalfMse']:.6f} | {rel['raw']['weightedAbsoluteBinGap']:.6f} / {rel['calibratedSearch']['weightedAbsoluteBinGap']:.6f} | {m['cpMaePawns']:.5f} | {m['residualRmsPawns']:.5f} | {m['signMistakes']} / {m['signPopulation']} |")
qm = [p['processMemory'] for p in quality['profiles'].values()]
lines += ['', 'All profiles reported zero saturated search scores. Balanced results use the existing '
          '|teacher CP| <= 200 subset. Full material and occupied-piece-count strata and ten-bin reliability '
          'tables remain in the quality JSON. Piece count is a phase proxy, not known game phase; reliability '
          'compares teacher-derived expected score, not empirical win probability. Native historical exposure '
          'is unknown. Sealed raw loss is lowest for edge functions on A and BRN on B, rather than a universal '
          'edge advantage. Compiled pairs have worse raw loss than BRN/current NNUE but stronger fixed-time '
          'play, confirming that teacher loss alone cannot select the practical architecture.', '',
          f"Sealed-profile whole-JVM peak working set: {min(m['peakWorkingSetBytes'] for m in qm):,}–{max(m['peakWorkingSetBytes'] for m in qm):,} bytes; "
          f"private commit: {min(m['peakPrivateCommitBytes'] for m in qm):,}–{max(m['peakPrivateCommitBytes'] for m in qm):,} bytes. "
          'Zero counter-read failures. No isolated training-memory inference follows.', '',
          '## Evidence identities and reproduction', '',
          'Plans and raw outputs are immutable. Reproduce into new output paths; do not overwrite completed runs. '
          'The match controller, analyzer, resource auditor and independent cross-check are recorded in STATE '
          'and the plans. The sealed controller is bound by the match plan and its separate execution plan.', '',
          '| Artifact | SHA-256 |', '|---|---|']
for name in ['z01-final-match-plan.json', 'z01-final-match-results.json', 'z01-final-match-resources.json',
             'z01-final-match-crosscheck.json', 'z01-final-quality-plan.json', 'z01-final-quality-execution-plan.json',
             'z01-final-quality-results.json', 'z01-final-runtime-results.json', 'z01-final-runtime-crosscheck.json']:
    lines.append(f'| [{name}]({name}) | `{sha(root/name)}` |')
lines += ['', 'The source/class/JVM/Java/platform manifests match the earlier scaling campaign. '
          'The first rejected opening sample and the fully replaced, accepted geometry-only sample remain '
          'in Z01-FINAL-OPENINGS-02 and its receipts. All earlier positive and negative evidence remains in '
          'the family files and Z01. No production source, default evaluator, native model store, release '
          'or deployment was changed.', '']
out = root / 'Z01-FINAL-RESULTS.md'
with out.open('x', encoding='utf-8', newline='\n') as f: f.write('\n'.join(lines))
summary = '\n## Completed final confirmation and sealed evaluation — 2026-10-07\n\n' \
          'All 3,072 final games completed with no missing, capped or failed outcome; all statistical, ' \
          'resource and provenance audits passed. Compiled pairs scored 80.18% versus fresh BRN, 73.73% ' \
          'versus current NNUE, 74.90% versus strict NNUE, 64.26% versus material, 55.57% versus captured ' \
          'native BRN and 73.93% versus compiled own Gen0. All primary bootstrap gates pass; the ' \
          'conservative simultaneous bound does not establish native superiority. All 22 sealed profiles ' \
          'passed, including exact observed original/compiled metric parity. No selection was reopened.\n\n' \
          'See [final results](Z01-FINAL-RESULTS.md) for WDL, per-run scores, both interval conventions, ' \
          'simultaneous bounds, actual search resources, complete sealed metrics and hashes. The ' \
          'comparative recommendation and final completion reconciliation follow; no further empirical ' \
          'allocation or production adoption is scheduled.\n'
for name in ['Z01.md', 'SCORECARD.md']:
    p = root / name
    with p.open('a', encoding='utf-8', newline='\n') as f: f.write(summary)
print('Recorded final evidence:', out, sha(out))
