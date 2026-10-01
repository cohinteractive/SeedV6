# BRN qsearch stand-pat diagnostic — 2026-09-24

**Finding:** the hypothesis is supported in a qualified form. Current BRN-2 expands much larger Kiwipete qsearch trees, with more capture-neighbour sign reversals and poor stand-pat cutoff rates. The best-supported explanation is **both local capture-neighbour inconsistency and a shifted learned score distribution**, particularly negative side-to-move bias. **Excessive numeric gain and tanh saturation are not supported.** No BRN incremental-state defect was found.

The evidence depends on checkpoint and topology. t2 Best g127 is markedly worse than Latest Training g134. On g134, the fixed shallow capture corpus has smaller absolute deltas than NNUE, whereas the actual BRN-driven Kiwipete qsearch edges have much larger typical deltas. Statistical centering removes much of g127's excess. Output-bias calibration should be isolated before changing architecture or qsearch. This is diagnostic evidence, not an implemented or accepted correction.

## Scope and repository state

- Date: 2026-09-24 UTC and New Zealand local date. First successful clock observation: `2026-09-24T00:28:10Z`; the first PowerShell `Get-Date -AsUTC` attempt failed because that parameter is unavailable here. No earlier timestamp is inferred.
- Authoritative repository: `C:\projects\seed\java\seedv6`.
- Inspected HEAD: `95f4ff3205f9badbff06d581f731761d77e40195`.
- Initial worktree: 41 modified tracked files and 46 individually enumerated untracked files, including inherited `app/bin/` and GUI/training source/tests. Index initially empty. These changes are inherited and unattributed, not part of this work.
- No on-disk ancestor/repository `AGENTS.md` was found. Current user-supplied instructions apply. Exact root `CODEXLOG_CURRENT.md` and `VERSION_STATE.txt` are absent; neither was created or activated.
- Core, rules, search and the reused JSON diagnostic helper have no diff from HEAD. The harness compiled current source into OS temporary storage, without using existing `app/bin`, build classes, modified checkpoint services, training services or GUI paths.
- SHA-256 checks of all 538 initially tracked/non-ignored files verified unchanged bytes after investigation. Root-level `BRN_*.md` reports are the existing convention; this report is the only intended task-created repository file.
- The user confirmed `t2` was the recent slow run. Saved BRN and play preferences independently selected t2. This unit did not reproduce training or establish which position dominated the reported two-hour interval.

## Pinned networks

Best/Latest references were read only to select exact immutable payloads. Loading used direct codecs: format, dimensions, schema, finite values and payload CRC checks succeeded. SHA-256 was checked before/after each run. No training writer, reference update, optimizer update, recovery or model save ran. Full checkpoint/optimizer/lineage acceptance was not repeated.

| Role | Exact checkpoint under the store's `checkpoints` directory | Model SHA-256 |
| --- | --- | --- |
| Primary: `E:\SeedV6-Networks\BRN\BRN-2\t2`, Latest Training g134 | `g000134-s000218158-3612e6b9a83ebe1c6c23a62b929d0363a67a4340a637bab40078f07833fe616c/network.brn2` | `21daab52baa213bd167a8317bac177d03b95fc3f7837ceefaf836497dc618924` |
| Published Best: same t2 store, g127 | `g000127-s000206807-d42ee23437e9d3e1a9525b7425bd46df1ecfc36b114c20ccfe380621f1fbfb84/network.brn2` | `474a43ee06c8f86357d4d1d3afd0f68fbfcfd6086b3e976ba4458791bbb27b31` |
| Historical canonical Best: `E:\SeedV6-Networks\BRN\BRN-2\training001`, g121 | `g000121-s000196690-5cf3400fde8d0c61c024b0dac47a8db54e425f7899f62bd2f657f77cc5b6b315/network.brn2` | `cc2072fe57fcfbc8ca8f83111586e3bfff4fe0abb554998e8ca8b859d1786c47` |
| NNUE reference: `E:\SeedV6-Networks\NNUE\training`, Best g74 | `g000074-s000008942-2e22ccbfdeb18f5f26f91f0df183411ac1d01844ccd43e56e9005ee3f5837bc6/network.nnue` | `3279ff72654f56c8f5ce3cc89cc55d1f253de6ae9df00bd61a41293f57dd16a9` |

All three BRN payloads are canonical BRN-2/schema 2, width 32, and report `boundedIntermediates=true`. That flag selects a safe incremental implementation; it does not mean clipping or output saturation. Best and Latest are separate inference snapshots, not interchangeable claims about promotion or strength.

## Current implementation facts

Paths below are relative to `app/src/main/java/com/ohinteractive/seedv6/`.

| Area | Current behaviour/source |
| --- | --- |
| NNUE | `core/nnue/NnueEvaluator.java`: dual-perspective accumulator, float forward output, `StrictMath.tanh(raw)`, side-to-move value. Public raw/bounded inspection exists. |
| BRN-2 | `core/brn2/{Brn2Model,Brn2Workspace,Brn2Accumulator}.java`: canonical friendly/enemy inputs, endpoint relations, local ReLU, sum pooling, board ReLU, binary64 value head, `StrictMath.tanh(z)`. Public `raw()` exposes pre-tanh output. |
| Mapping | `search/evaluation/{BrnScoreMapping,NnueScoreMapping}.java`: identical default `sign(v)*max(1,round(32511*abs(v)))`, exact zero preserved. These are uncalibrated search units, **not centipawns**. Mate band reserved. |
| Incremental lifecycle | `SearchEvaluation`: per-ply accumulators maintain both canonical perspectives; `child` prepares another slot, restoration returns to the untouched parent. Fresh production rebuild and independent `evaluateReference` are available. |
| Comparable search | Both neural definitions use mate-distance bounds only; aspiration, razoring and futility disabled. One worker, fresh 262,144-entry TT per request, singleton root history, ordinary iterative deepening. |
| Stand-pat | `search/quiescence/QuiescenceSearch.java`: eligible non-check/nonterminal positions cut off if `standPat >= beta`, otherwise alpha becomes `max(alpha,standPat)`. Captures/promotions are searched; checks require complete legal evasions. SEE orders moves without rejecting captures. |
| Depth | Soft q-ply limit 16 returns a static score at non-check positions after legality/draw checks; checked nodes can continue beyond 16. Absolute capacity is separate. |
| Telemetry | `SearchDiagnosticsSnapshot` exposes main/q child entries, maximum q-ply, stand-pat cutoffs, tactical/evasion moves and soft-limit encounters. `QsearchDecisionTrace` exposes eligible attempts, exact paired shadow cutoff classes, q-ply and adjacent-delta histograms, child-return deltas and detached sampled boards/windows. No source instrumentation was needed. |

## Methodology and fixtures

1. Compile the external harness in Appendix A against current engine source using Java 21. Every executable diagnostic script/class, JSONL file and test output stayed in `C:\Users\Central\AppData\Local\Temp\seedv6-brn-diagnostic-20260924`.
2. Search depth 4 with a cumulative **1,000,000 entered-node cap and 60-second time cap per request**. All requests ended by completion or node cap, never time cap. No warmup; timing is secondary and includes JIT/trace effects. g134 uses all six fixtures; g127/g121 use Kiwipete only.
3. Enumerate all legal root moves in coordinate order. Keep quiet root edges as controls; after each root capture recursively enumerate **only captures through four plies**. A 20,000-edge cap per fixture was never reached. There are **4,100 capture edges, 804 immediate same-target recaptures and 121 quiet root edges**, with 15 captures at the roots. Kiwipete supplies 4,055 of the capture edges. Paths/transpositions are retained, not deduplicated; observations are correlated.
4. Use `Gen.genAll(..., legal=true)`, `Board.makeMoveInto`, production `State.child` and normal per-ply restoration. Assert unchanged parent board and restored parent scores after each child. Both t2 checkpoints see exactly the same **4,221 edges**, board keys/status and NNUE scores.
5. Correct perspective: `delta = -childScoreSTM - parentScoreSTM`. Sign reversal means `parentScoreSTM * (-childScoreSTM) < 0`, not ordinary side-to-move alternation. Endpoint distributions count parent and child once per edge (8,200 observations for fixed captures); signed scores are STM-oriented. Quantiles interpolate at `(n-1)*p`; p99 values are descriptive, not confidence bounds.
6. Existing trace shadow stride is 1: the other evaluator freshly rebuilds at every actual static q-evaluation and never supplies decisions. Exact adjacent histograms require real statics at both ends; checked/terminal positions receive no fabricated statics. These adjacent edges include tactical captures/promotions. g134 is measured in both driver directions.
7. Sample every 101st observed q-position starting at ordinal 1, limit 12,000; the limit was never reached. Re-evaluate detached positions/parents after search. Capture-only samples require occupancy to decrease by one and actual stand-pat eligibility at the child. Margin, sign and saturation results on these samples are **sample estimates**. Full node/cutoff/adjacent histograms are exact for each completed or capped request.
8. Compare normal incremental BRN output at every fixed root/child with fresh production reconstruction and independent reference accumulation, including raw values. Compare sampled actual driver scores against fresh reconstruction. Record NNUE discrepancies without changing NNUE.
9. Replay g134 Kiwipete depth 4 without a trace: score, move, all emitted nontiming counters match exactly for both drivers. Also compare depth 3.

Qnodes count entered **qsearch children**; qsearch roots are main-search leaves and are not counted again. Q proportion is `q/(main+q)`. Stand-pat rates use actual eligible attempts, not qnodes. Typical q-ply and captures per q-position include qsearch roots.

| Fixture | FEN |
| --- | --- |
| middlegame-kiwipete | `r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1` |
| qsearch-exchanges | `4k3/8/2p1p3/3q4/2P1P3/3Q4/8/4K3 w - - 0 1` |
| tactical-queen | `4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1` |
| en-passant | `4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1` |
| quiet-fianchetto | `r1bq1rk1/ppp1bppp/2n2n2/3pp3/8/1P1P1NP1/PBP1PPBP/RN1Q1RK1 w - - 0 8` |
| opening-start | `rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1` |

All except fianchetto are established `SearchBenchmark` fixtures; fianchetto is from `Brn2DiagnosticCorpus`. Fianchetto is only relatively quiet and has some captures. Opening-start has zero root captures and is the stricter quiet control.

## A. Search-tree reproduction

Primary g134 versus NNUE at depth 4; all twelve requests completed.

| Fixture | Driver | Total nodes | Qnodes | Q % | Stand-pat attempts | Cutoffs | Cutoff % | Max q-ply | Seconds |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Kiwipete | NNUE | 41,435 | 32,300 | 77.95 | 36,191 | 29,281 | 80.91 | 17 | 1.673 |
| Kiwipete | BRN | 218,790 | 209,297 | 95.66 | 180,919 | 80,896 | 44.71 | 19 | 2.242 |
| Exchanges | NNUE | 3,197 | 1,419 | 44.39 | 2,362 | 1,441 | 61.01 | 5 | 0.0185 |
| Exchanges | BRN | 2,019 | 749 | 37.10 | 1,347 | 925 | 68.67 | 6 | 0.0105 |
| Tactical queen | NNUE | 229 | 10 | 4.37 | 154 | 109 | 70.78 | 1 | 0.0018 |
| Tactical queen | BRN | 189 | 8 | 4.23 | 115 | 72 | 62.61 | 1 | 0.0018 |
| En passant | NNUE | 638 | 40 | 6.27 | 427 | 264 | 61.83 | 2 | 0.0025 |
| En passant | BRN | 358 | 18 | 5.03 | 235 | 151 | 64.26 | 1 | 0.0028 |
| Fianchetto | NNUE | 8,793 | 3,928 | 44.67 | 7,437 | 5,744 | 77.24 | 9 | 0.319 |
| Fianchetto | BRN | 18,327 | 13,007 | 70.97 | 16,509 | 8,896 | 53.89 | 17 | 0.225 |
| Opening | NNUE | 2,343 | 126 | 5.38 | 1,684 | 1,136 | 67.46 | 2 | 0.0723 |
| Opening | BRN | 3,795 | 220 | 5.80 | 2,913 | 2,158 | 74.08 | 4 | 0.0483 |

Kiwipete g134 has **6.48× qnodes and 5.28× total nodes**. Fianchetto has 3.31× qnodes. Conversely BRN searches fewer nodes on all three sparse tactical fixtures. This contradicts a universal claim that every capture position makes BRN explode.

| Kiwipete model | Requested/completed depth | Status | Total nodes | Qnodes | Q % | Attempts | Cutoffs | Cutoff % |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| t2 Best g127 | 4/2 | NODE_LIMIT | 1,000,000 | 999,635 | 99.96 | 773,302 | 286,120 | 37.00 |
| Historical g121 | 4/3 | NODE_LIMIT | 1,000,000 | 992,405 | 99.24 | 815,521 | 327,917 | 40.21 |
| t2 g134 | 3/3 | COMPLETED | 73,308 | 70,625 | 96.34 | 59,398 | 25,077 | 42.22 |
| NNUE g74 | 3/3 | COMPLETED | 10,844 | 8,217 | 75.77 | 9,767 | 7,372 | 75.48 |

Capped rows are **prefixes, not completed depth-4 counts**. g127 exceeds NNUE's completed depth-4 qnodes by 30.95× while completing only depth 2. g134's depth-3 qnodes are 8.60× NNUE's.

Historical supplied NNUE totals, 41,435 total/32,300 qnodes, reproduce exactly. Current cutoffs are 29,281, not the historical approximate 24,501. The old BRN 671,841/658,493/8,323 figures were not reproduced and are not used as current evidence; the exact old model/settings for those figures were not established. The pinned canonical g121 instead reaches the current million-node cap.

| Kiwipete driver | Observed q-positions | Mean / median q-ply | Max q-ply | Soft-limit encounters | Capture entries | Captures / q-position | Tactical / evasion moves |
| --- | --- | --- | --- | --- | --- | --- | --- |
| NNUE | 38,843 | 3.43 / 1 | 17 | 88 | 29,536 | 0.760 | 27,341 / 4,959 |
| g134 | 216,180 | 9.25 / 10 | 19 | 10,750 | 183,267 | 0.848 | 170,385 / 38,912 |
| g127 cap | 999,954 | 12.14 / 12 | 19 | 104,775 | 839,567 | 0.840 | 784,981 / 214,654 |
| g121 cap | 998,853 | 10.42 / 11 | 20 | 60,738 | 843,885 | 0.845 | 777,213 / 215,192 |

Capture entries include tactical captures and capture evasions. Tactical moves also include promotions; evasions may be quiet. This much deeper q-ply distribution establishes tree-shape pathology beyond evaluator cost. Trace-free g134 depth-4 timing was 0.311 seconds NNUE, 2.411 seconds BRN; single observations are not warmed performance estimates.

## B. Fixed capture-neighbour deltas

All 4,100 identical capture edges; units are search scores.

| Evaluator | Mean absolute delta | p50 | p75 | p90 | p95 | p99 | Maximum | Sign reversals % |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| NNUE | 9,605.73 | 7,700.5 | 14,123.75 | 20,846.3 | 24,939.8 | 32,834.52 | 39,943 | 23.56 |
| t2 g127 | 15,343.41 | 15,168 | 20,282 | 24,767.5 | 27,250.8 | 31,667.69 | 37,915 | 65.59 |
| t2 g134 | 6,908.00 | 6,566 | 9,908 | 12,722.1 | 14,495.95 | 17,299.56 | 25,326 | 46.20 |

g127's median is 1.97× NNUE and reversals are 2.78× as frequent. g134 has **smaller absolute median/tails**, but 1.96× as many reversals. Uniformly larger absolute jumps are not established for g134.

| Subset | Edges | NNUE median delta / reversal % | g127 median / reversal % | g134 median / reversal % |
| --- | --- | --- | --- | --- |
| Root captures only | 15 | 3,076 / 33.33 | 12,077 / 60.00 | 5,189 / 6.67 |
| Same-target recaptures | 804 | 8,366 / 26.87 | 16,366.5 / 66.67 | 6,219.5 / 48.51 |
| Quiet root moves | 121 | 3,263 / 15.70 | 16,244 / 54.55 | 5,719 / 22.31 |
| Kiwipete capture paths | 4,055 | 7,720 / 23.65 | 15,259 / 65.94 | 6,620 / 46.56 |
| Sparse exchange captures | 33 | 4,109 / 18.18 | 6,445 / 42.42 | 3,292 / 15.15 |
| Fianchetto captures | 10 | 1,071 / 0 | 15,351 / 0 | 3,996.5 / 0 |

Fifteen root captures alone are too small/unrepresentative to test the hypothesis. Opening-start contributes zero capture edges and 20 quiet edges; no nonexistent captures were fabricated for a control.

A data-derived large-jump threshold is **NNUE's fixed capture-delta p95, 24,939.8**. Exceedance frequencies: NNUE 5.00%, g127 9.63%, g134 0.0244% (1/4,100). Thus extreme absolute tails are not the primary g134 explanation. On the deeper g134-tree capture sample, the same definition yields threshold 24,816.2 and BRN exceedance 8.68% versus NNUE 5.05%. These are observed baseline quantiles, not centipawn heuristics.

### Actual Kiwipete qsearch topology

Full paired adjacent-static histograms, same observed edges within each row. They include tactical captures/promotions with real statics at both endpoints; these are not the shallow fixed corpus.

| Driver tree | Paired edges | BRN p50 delta | NNUE p50 | BRN p95 | NNUE p95 |
| --- | --- | --- | --- | --- | --- |
| NNUE, g134 shadow | 25,403 | 5,501 | 4,445 | 17,814.7 | 21,191 |
| g134, NNUE shadow | 146,387 | 12,121 | 5,002 | 26,987 | 24,662 |
| g127 cap, NNUE shadow | 668,002 | 14,682 | 6,127 | 29,312 | 25,384 |

The g134-driven tree has **2.42× median delta** on 146,387 identical edges. Immediately below parents where NNUE alone would have cleared local beta, medians are 13,511 versus 4,982 (2.71×). In the NNUE-driven tree the median excess is only 1.24×. BRN decisions expose a different, deeper capture topology, so population selection matters.

| Capture-only systematic sample | Edges | BRN median delta | NNUE median | BRN reversals % | NNUE reversals % | BRN normalized median | NNUE normalized median |
| --- | --- | --- | --- | --- | --- | --- | --- |
| NNUE tree, g134 shadow | 261 | 5,871 | 4,917 | 30.27 | 15.71 | 0.274 | 0.178 |
| g134 tree | 1,405 | 11,959 | 5,087 | 48.61 | 17.58 | 0.653 | 0.180 |
| g127 tree cap | 5,992 | 14,901 | 6,320.5 | 63.37 | 20.43 | 0.795 | 0.228 |

Normalization divides each evaluator's deltas by its own p90 absolute endpoint score in the same population. g134 retains a **3.63× relative median** on its actual tree. Positive scale normalization does not eliminate the discrepancy.

## C. Scale, saturation and distribution bias

Fixed capture endpoints: 8,200 observations per evaluator, with edge/path weighting.

| Evaluator | Signed STM mean | Median absolute score | p90 absolute | p95 absolute | p99 absolute | Maximum absolute | ≥95% limit / ≥99% / at limit |
| --- | --- | --- | --- | --- | --- | --- | --- |
| NNUE | −2,803.52 | 13,862 | 23,319 | 24,709 | 27,678 | 29,146 | 0 / 0 / 0 |
| g127 | −7,594.73 | 8,167 | 17,388 | 19,656 | 22,820 | 26,152 | 0 / 0 / 0 |
| g134 | −3,044.61 | 4,597 | 10,809 | 13,385 | 17,073.69 | 23,113 | 0 / 0 / 0 |

BRN has smaller absolute scores than NNUE. For g134, signed STM endpoint median is −3,254 and range −23,113 to +20,453; NNUE median is −4,479 and range −29,146 to +28,847. Neither BRN checkpoint saturates on these captures.

Public raw values were available. Counting each root/child once (4,227 observations per BRN checkpoint), g134 raw pre-tanh range is −0.88905 to +1.44180; g127 −1.11099 to +1.39901. Neither reaches `|tanh(raw)| >= .95`. These populations differ from endpoint weighting because parents are not repeated for every outgoing edge.

| Systematic actual-search static sample | N | BRN median absolute score | NNUE median | BRN p95 absolute | BRN max | BRN ≥95% / ≥99% / at limit |
| --- | --- | --- | --- | --- | --- | --- |
| g134, both drivers/six fixtures | 2,624 | 9,073.5 | 20,423.5 | 23,964.2 | 31,239 | 1 / 0 / 0 |
| g127 capped Kiwipete | 8,707 | 8,002 | 17,232 | 20,636.7 | 29,586 | 0 / 0 / 0 |
| Historical g121 capped Kiwipete | 8,698 | 8,768.5 | 22,244 | 23,944.9 | 32,112 | 12 / 0 / 0 |

One g134 sample exceeds 95% of the limit (0.038%); none exceeds 99%. No sampled score from either architecture reaches ±32,511. Historical g121 has 12/8,698 above 95%, zero above 99%. Saturation is not a plausible primary explanation here; sampling cannot exclude every unobserved saturated position.

### Multiplicative normalization versus additive bias

On fixed captures, median `absoluteDelta / p90(absoluteScore)` is NNUE **0.330**, g127 **0.872**, g134 **0.607**. Dividing by endpoint score standard deviation also leaves a larger relative delta: NNUE 0.504, g134 1.103. A smaller positive multiplier cannot repair sign reversals or ordering. With neural aspiration/futility/delta margins absent, pure positive rescaling largely preserves ideal alpha-beta/stand-pat inequalities; integer quantization, PVS boundaries and mate handling prevent claiming exact search invariance. No rescaled search was run.

**Additive distribution bias is materially different.** Parent and child both report negative STM scores on 65.46% of g127 fixed capture edges, 42.88% of g134 edges, and 17.68% of NNUE edges. Both-positive frequencies are only 0.12%, 3.32%, and 5.88%. On g127's actual tree sample, 63.00% are both-negative; on g134's tree 47.26%. After perspective correction, this produces predominantly positive capture deltas. This is not a renewed colour-asymmetry diagnosis; both endpoints use canonical STM interpretation.

A **statistical-only** check subtracts each evaluator's mean STM endpoint score from the fixed capture corpus: NNUE −2,803.5234, g127 −7,594.7284, g134 −3,044.6098. For center `c`, the corrected delta is `delta + 2*c`. This changes each side's STM zero point, not merely the scale. Applying those fixed centers to the separately observed BRN-tree capture samples gives:

| Tree sample | Original BRN / NNUE median absolute delta | Centered BRN / NNUE median | Centered BRN / NNUE reversal % |
| --- | --- | --- | --- |
| g127 | 14,901 / 6,320.5 | 6,219 / 6,434.0 | 33.28 / 19.24 |
| g134 | 11,959 / 5,087 | 7,088.8 / 6,152.0 | 34.52 / 15.37 |

Centering removes most of g127's absolute median excess and much of g134's; reversal differences remain. Centered BRN p95 is lower than NNUE in both samples. Signed-delta standard deviation is also smaller for BRN on the fixed corpus and these tree samples. Thus **“more random high-variance noise everywhere” is not supported**; directional/zero-point behaviour is substantial.

This decomposition is not a fitted probability calibration or a validated fix. The fixed corpus is capture-selected and overlaps the tree's domain; it cannot establish unbiased training-distribution bias or a safe optimum. Root scores vary strongly by position: opening-start is −10,909 for g127, −271 for g134 and −1,538 for NNUE; Kiwipete is −9,458, +2,337 and +16,685 respectively. No single offset is proven safe, no search score was changed, and no counterfactual tree savings are claimed.

## D. Connection to stand-pat

Exact paired local cutoffs at the **driver's actual beta**:

| Kiwipete tree / shadow | Eligible attempts | Both cutoff | Driver only | Shadow only | Neither |
| --- | --- | --- | --- | --- | --- |
| NNUE / g134 | 36,191 | 24,716 | 4,565 | 1,558 | 5,352 |
| g134 / NNUE | 180,919 | 64,682 | 16,214 | 41,052 | 58,971 |
| g127 / NNUE | 773,302 | 229,121 | 56,999 | 204,043 | 283,139 |

On g134's 100,023 stand-pat failures, NNUE clears that same local beta **41,052 times (41.04%)**. Actual descendants under at least one such ancestor total 191,857/216,180 observed q-positions (88.75%). This union counts nested ancestry once; **it is not predicted savings** from substituting NNUE, which would change downstream windows/moves. The inverse comparison also disagrees, and NNUE is a working reference rather than an oracle of chess truth.

Sampled `standPat - beta` margins:

| Driver | Eligible samples | Median margin | Failure samples | Median failed margin | Failures within 233.19 of zero |
| --- | --- | --- | --- | --- | --- |
| NNUE | 363 | +19,554 | 65 | −6,025 | 0/65 (0%) |
| g134 | 1,824 | −1,201.5 | 994 | −8,423 | 15/994 (1.51%) |
| g127 | 7,731 | −3,619 | 4,847 | −9,068 | 60/4,847 (1.24%) |

233.19 equals 1% of fixed-corpus NNUE p90 absolute score; it is not an arbitrary centipawn margin. No sampled failure lies within one score unit of beta. Most misses are substantial.

Below parents where only NNUE would cut off, g134's exact adjacent median is 13,511 versus NNUE 4,982; g127 is 16,187 versus 6,275. On g134's full tree, backed-up child-return changes from static parent scores have median 7,344, p95 22,054 and maximum 58,277 across 170,385 returned children. Those include mate returns and are not static-delta statistics. They show substantial value changes following failed stand-pat without proving which captures are objectively justified.

**Measurement limit:** the existing trace retains exact cutoff/adjacent histograms and bounded window samples, not the full `standPat-beta` population histogram or every child's delta conditioned on all failed parents. If exact conditioning is needed later, the smallest instrumentation is diagnostic-only histograms at existing `staticScore`/`moveReturned` hooks, keyed by failure, incoming capture and q-ply. No evaluator/search behaviour needs changing. No instrumentation was added here.

## E. Incremental versus fresh

- Across **8,454 fixed root/child evaluations** from t2 g127/g134, zero BRN mapped-score disagreements against fresh production or independent reference evaluation; maximum raw discrepancy ≤7.78e−16. Parent restoration passed on every edge.
- Actual BRN driver samples: 2,136 g134 depth-4, 8,707 g127 capped, 8,698 historical g121 capped, plus 630 g134 depth-3. All **20,171** actual incremental driver scores equal fresh reconstruction. Fresh/reference mapped scores also agree on every sampled position. These are observations, not distinct boards.
- On the identical NNUE fixed paths, six of 4,227 values differ from fresh NNUE by **one search unit**, repeated identically in both BRN comparisons. Maximum error one; NNUE uses float accumulation. None of 589 actual NNUE-driver q-samples disagreed. This rounding sensitivity was recorded and left unchanged.
- **11/11 existing focused tests passed**, zero failures/aborts/skips: `Brn2AccumulatorTest` (5), `Brn2SearchIntegrationTest` (2), `NnueScoreMappingTest` (4). Accumulator tests separately reported 27,987 equivalence comparisons, maximum output error 7.77e−16, zero mapped-score differences. They include legal make/unmake, special moves and bounded one/six-thread searches against full reference evaluation.

No BRN incremental-state defect is observed. This is finite sampling and focused testing, not proof for every network/board.

## Explicit answers and conclusion

| Question | Answer |
| --- | --- |
| 1. Larger current qsearch tree? | Yes on Kiwipete: g134 6.48× qnodes at completed depth 4; g127 about 30.95× in a capped prefix completing only depth 2. Not universal on sparse tactical controls. |
| 2. Larger identical-edge changes? | g127 yes in the fixed corpus. g134 no in aggregate fixed captures, but yes in typical changes on actual BRN-driven qsearch edges. Extreme tails are not consistently larger. |
| 3. More sign reversals? | Yes on meaningful multi-ply samples: fixed g127 65.59%, g134 46.20%, NNUE 23.56%; g134 actual-tree captures 48.61% versus NNUE 17.58%. Root-only sample is mixed. |
| 4. Saturated or differently scaled? | Saturation/oversized absolute scale unsupported; mapper identical and BRN scores usually smaller. Learned zero point/distribution differs substantially. |
| 5. After normalization? | Relative inconsistency remains under multiplicative normalization. Additive centering removes much absolute median excess, especially g127, but leaves more sign reversals. These are distinct operations. |
| 6. Stand-pat connection? | Yes observationally: poor eligible cutoff rates, deeper q-ply, exact same-window shadow cutoffs and large changes below them. No causal correction experiment ran. |
| 7. Incremental inconsistency? | No BRN mapped-score mismatch in fixed trained-model paths, actual sampled search states or focused tests. |
| 8. Best-supported cause? | **Both local capture-neighbour inconsistency and learned distribution/offset behaviour.** Distribution bias is a major contributor. Saturation, oversized multiplier and incremental corruption are unsupported. Precise causal shares remain unmeasured. |
| 9. Smallest next experiment? | Research-only bias-calibration ablation on frozen g127/g134, fitted and tested on disjoint samples, with NNUE and qsearch rules unchanged. Compare intercept-only, positive-gain control and combined calibration before architecture changes. |

### Recommended next implementation experiment — not performed

In a separately authorized unit, add only the **research-only BRN calibration seam** needed to compare original scores against a fitted STM intercept and positive-gain control. Preserve normal-score/mate separation and evaluator identity per TT. Fit on a small independent, balanced calibration set with accessible raw outputs and a chosen trusted target; evaluate on held-out legal capture/recapture pairs and the same bounded searches. Do not choose the intercept by minimizing qnodes or ship the capture-selected means above as constants. Check both sides, quiet controls, target meaning and tactical ordering.

Success requires improved held-out bias/reversal behaviour and smaller qsearch trees without degraded target calibration or tactical outcomes. If calibration fails, the next candidate is targeted training for capture-delta agreement with justified teacher/target changes, rather than suppressing all capture deltas. Legitimately winning captures must retain large changes. Generic smoothing, output-range changes and qsearch-specific score suppression are not justified as the first correction. No qsearch change is recommended to conceal a defective evaluator.

## Validation, limitations and skipped work

**Performed/passed:** fresh external engine compilation; direct codec loads and model SHA stability; legal capture traversal/restoration; identical edges/NNUE scores across t2 snapshots; fresh/reference comparisons; full trace node/reason/cutoff accounting; trace-free Kiwipete score/move/counter equality; 11 focused tests; inherited-file SHA checks; `git diff --check`.

**Limits:** six fixtures, mostly Kiwipete-weighted correlated edges; no true-value/best-move labels for most captures; no causal calibration/search intervention; capped Best/historical searches; sampled margins/saturation/sign counts; single JVM timing; full checkpoint/optimizer/lineage validation not repeated. The four-generation elapsed time was not reproduced with training. Results do not establish which checkpoint dominated every historical slow game or whether a calibration change would preserve strength.

**Deliberately skipped:** full/long suites, GUI/browser verification, training, multi-generation runs, self-play datasets, broad strength benchmarks, candidate matches and release/package/build acceptance. These are outside this diagnostic and would add cost or mutation without resolving its hypothesis. No engine/source/config/test/build/existing-document edits, network writes, or fix were made. No web/browser access was needed.

Diagnostic completion does not establish implementation acceptance, playing-strength acceptance, deployment or operational resolution.

## Reproduction and evidence

Temporary workspace: `C:\Users\Central\AppData\Local\Temp\seedv6-brn-diagnostic-20260924`. Runtime: OpenJDK `21+35-2513`, Python 3.13.4. Commands ran from the repository root:

```powershell
# Extract Appendix A into $diag/CaptureDiagnostic.java if recreating the run.
javac -d (Join-Path $diag 'classes') -sourcepath app/src/main/java (Join-Path $diag 'CaptureDiagnostic.java')
# $brn and $nnue are exact payload paths from the identity table.
java -Xms256m -Xmx1024m -cp (Join-Path $diag 'classes') com.ohinteractive.seedv6.tools.search.CaptureDiagnostic $brn $nnue (Join-Path $diag 't2-g134-primary.jsonl') t2-g134 primary
# Repeat with a fresh output filename, these model/label/mode combinations:
# g127 -> t2-g127-kiwi.jsonl        t2-g127          kiwi
# g127 -> t2-g127-edges.jsonl       t2-g127          edges
# g121 -> historical-g121-kiwi.jsonl historical-g121 kiwi
# g134 -> t2-g134-replay.jsonl      t2-g134          replay
```

External Python scripts `analyze.py`, `offset_analysis.py`, `verify.py`, `report_numbers.py` and `provenance.py` only read JSONL/files. The formulas, populations, quantiles and denominators are specified above. Raw temp output is disposable; conclusions and executable harness are retained here so future work need not depend on temp-file survival.

Focused tests were compiled with `javac -sourcepath 'app/src/main/java;app/src/test/java'` to `$diag/test-classes`, selecting the three existing test source files named above and a temporary JUnit Launcher runner. It selected those classes only, called `LauncherFactory.create().execute(request)`, printed the summary/failures, and failed on nonzero failures or zero discovery. `java -Xmx1024m ... FocusedTests` ran it. Classpath used cached Jupiter 5.10.3, Platform 1.10.3, OpenTest4J 1.3.0 and API Guardian 1.1.2. No Gradle/build/test output was created in the repository.

| Temporary raw output | Bytes | SHA-256 |
| --- | --- | --- |
| historical-g121-kiwi.jsonl | 6,329,946 | `665feb8d69a9bfd2a716f5c7f3dcbcbe2031ba30449c962facc835aa80510a2f` |
| t2-g127-edges.jsonl | 2,537,150 | `3c3f482a223c416a4b98b164e3f2c5098744365cbab3e664ad28bc5d8e4c7cc0` |
| t2-g127-kiwi.jsonl | 6,364,216 | `0b48b12380c681544a25565671dac81d110b1b8094bfae3d83ac11a904e888a9` |
| t2-g134-primary.jsonl | 5,313,271 | `075915243f994614c683b06426a5488c39f26bc735d6d4fb3bb0cb66b9613c89` |
| t2-g134-replay.jsonl | 738,145 | `7a8dc14983c7daf378572326d054621e68522a452bebb1e059a44fa21bbbea2b` |

## Git and human-action boundary

The report is the sole task-created repository change. Inherited modifications/untracked files remain outside its isolated commit. No product changes are attributed to this unit. The report-only staged diff and final worktree are checked before completion; the final report commit SHA is given in the completion response because a report cannot contain its own final commit SHA. No push/deployment is part of this unit.

**Human actions required after this prompt: None.** The proposed future experiment is not an action required to complete this diagnostic.

## Appendix A — exact temporary diagnostic harness

The source below is report evidence only. To rerun, extract it outside the repository and compile with the command above. It uses existing board/evaluator/search APIs and telemetry.

```java
package com.ohinteractive.seedv6.tools.search;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import com.ohinteractive.seedv6.core.*;
import com.ohinteractive.seedv6.core.brn2.*;
import com.ohinteractive.seedv6.core.nnue.*;
import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.search.alphabeta.AlphaBetaPvsSearch;
import com.ohinteractive.seedv6.search.common.*;
import com.ohinteractive.seedv6.search.diagnostics.*;
import com.ohinteractive.seedv6.search.evaluation.*;
import com.ohinteractive.seedv6.search.iterative.*;
import com.ohinteractive.seedv6.search.order.MoveOrdering;
import static com.ohinteractive.seedv6.tools.search.DiagnosticReport.*;
public class CaptureDiagnostic {
 static final String[][] POSITIONS={
 {"middlegame-kiwipete","r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1"},
 {"qsearch-exchanges","4k3/8/2p1p3/3q4/2P1P3/3Q4/8/4K3 w - - 0 1"},
 {"tactical-queen","4k3/8/8/3q4/4P3/8/8/4K3 w - - 0 1"},
 {"en-passant","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1"},
 {"quiet-fianchetto","r1bq1rk1/ppp1bppp/2n2n2/3pp3/8/1P1P1NP1/PBP1PPBP/RN1Q1RK1 w - - 0 8"},
 {"opening-start","rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"}};
 static PrintWriter out; static Brn2Model brn; static NnueNetwork nnue;
 static SearchEvaluation bdef,ndef; static SearchEvaluation.State bs,ns,bfresh,nfresh;
 static Brn2Accumulator[] acc=new Brn2Accumulator[6]; static Brn2Accumulator fresh;
 static Brn2Workspace ref=new Brn2Workspace(); static String label; static int edgeCount; static long checks;
 record V(int b,int n,double raw,double value,int freshB,int referenceB,double freshRaw,double referenceRaw,int freshN){}
 static V val(long[] board,int ply, boolean incremental){
  if(!incremental){bs.initialize(board,ply); ns.initialize(board,ply); acc[ply].rebuild(board);}
  int b=bs.evaluate(board,ply),n=ns.evaluate(board,ply); double value=acc[ply].evaluate(board),raw=acc[ply].raw();
  if(b!=BrnScoreMapping.map(value))throw new IllegalStateException("Direct/production BRN mismatch");
  fresh.rebuild(board); double fv=fresh.evaluate(board); double rv=brn.evaluateReference(board,ref);
  bfresh.initialize(board,0); nfresh.initialize(board,0);
  int fb=bfresh.evaluate(board,0),fn=nfresh.evaluate(board,0);
  if(fb!=BrnScoreMapping.map(fv))throw new IllegalStateException("Fresh production mismatch");
  checks++; return new V(b,n,raw,value,fb,BrnScoreMapping.map(rv),fresh.raw(),ref.raw(),fn);
 }
 static Map<String,Object> values(V v){return fields("brn",v.b,"nnue",v.n,"raw",v.raw,"tanh",v.value,"freshBrn",v.freshB,"referenceBrn",v.referenceB,"freshRaw",v.freshRaw,"referenceRaw",v.referenceRaw,"freshNnue",v.freshN);}
 static long[] legal(long[] b){long[] m=new long[256],scratch=new long[256]; int n=Gen.genAll(b[0],b[1],b[2],b[3],(int)b[4],b[5],true,m,scratch);return Arrays.stream(Arrays.copyOf(m,n)).boxed().sorted(Comparator.comparing(Move::coordinate)).mapToLong(Long::longValue).toArray();}
 static boolean capture(long[] b,long m){return ((m>>>Board.TARGET_PIECE_SHIFT)&Board.PIECE_BITS)!=0 || ((m>>>Board.PROMOTE_PIECE_SHIFT)&Board.PIECE_BITS)==0&&MoveOrdering.isTactical(b,m);}
 static long[] play(long[] b,long m){long[] c=new long[Board.MAX_BITBOARDS];Board.makeMoveInto(b[0],b[1],b[2],b[3],(int)b[4],b[5],m,c);return c;}
 static void walk(String id,long[] b,int ply,V parent,int previousTarget){
  if(ply>=4)return; long[] before=b.clone();
  for(long move:legal(b)){
   boolean cap=capture(b,move); if(!cap&&ply!=0)continue; if(edgeCount>=20000) return;
   long[] child=play(b,move); bs.child(b,child,ply); ns.child(b,child,ply); acc[ply+1].update(b,child,acc[ply]);
   V v=val(child,ply+1,true); String next=id+"/"+Move.coordinate(move);
   write(out,"type","edge","id",next,"ply",ply+1,"capture",cap,"recapture",cap&&Move.toSquare(move)==previousTarget,"whiteParent",Board.player((int)b[4])==0,"parent",values(parent),"child",values(v),"childKey",Long.toHexString(child[Board.KEY]),"childStatus",child[Board.STATUS]);
   edgeCount++; if(cap)walk(next,child,ply+1,v,Move.toSquare(move));
   if(!Arrays.equals(b,before)||bs.evaluate(b,ply)!=parent.b||ns.evaluate(b,ply)!=parent.n||BrnScoreMapping.map(acc[ply].evaluate(b))!=parent.b)throw new IllegalStateException("Parent restoration failed");
  }
 }
 static void search(String[] p,boolean driveBrn,int depth,long limit,boolean traceEnabled){
  long[] b=Board.fromFen(p[1]),before=b.clone(); String driver=driveBrn?"BRN":"NNUE";
  QsearchDecisionTrace trace=traceEnabled?new QsearchDecisionTrace(driveBrn?ndef:bdef,1,101,12000):null;
  var worker=trace==null?new AlphaBetaPvsSearch(driveBrn?bdef:ndef,1<<18):new AlphaBetaPvsSearch(driveBrn?bdef:ndef,1<<18,trace);
  try(var search=new IterativeDeepeningSearch(worker)){
   long start=System.nanoTime();var control=SearchControl.controlled(limit,start,60_000_000_000L,TimeSource.SYSTEM);
   var result=search.search(new SearchRequest(b,GameHistory.initial(b),depth,SearchObserver.NONE,control,true));
   long elapsed=System.nanoTime()-start;var d=result.diagnostics();var n=d.worker().nodes();var q=d.worker().qsearch();var r=result.lastCompletedResult();
   if(control.nodes()!=d.totalEnteredNodes()||!Arrays.equals(b,before))throw new IllegalStateException("Search accounting/restoration failed");
   String status=result.targetDepthCompleted()?"COMPLETED":control.termination().toString();
   write(out,"type","search","position",p[0],"driver",driver,"depth",depth,"trace",traceEnabled,"status",status,"completedDepth",r==null?0:r.depth(),"score",r==null?null:r.score(),"move",r==null?null:Move.coordinate(r.bestMove()),"nodes",control.nodes(),"main",n.mainNodes(),"q",n.qNodes(),"maxQply",n.maximumQply(),"evaluations",n.evaluationCalls(),"standPatCutoffs",q.standPatCutoffs(),"checkedQ",q.checkedQNodes(),"tacticalMoves",q.tacticalMovesSearched(),"evasionMoves",q.evasionMovesSearched(),"softLimit",q.softDepthLimitEncounters(),"elapsedNs",elapsed);
   System.err.println(label+" "+p[0]+" "+driver+" depth="+depth+" "+status+" nodes="+control.nodes()+" q="+n.qNodes());
   if(trace!=null){
    write(out,"type","trace","position",p[0],"driver",driver,"depth",depth,"metrics",trace.summary());
    for(var s:trace.positionSamples()){
     V v=s.driver()==null?null:val(s.board(),0,false),pv=s.parentBoard()==null?null:val(s.parentBoard(),0,false);
     boolean cap=s.parentBoard()!=null&&Long.bitCount(s.parentBoard()[0]|s.parentBoard()[1]|s.parentBoard()[2])==Long.bitCount(s.board()[0]|s.board()[1]|s.board()[2])+1;
     write(out,"type","qsample","position",p[0],"driver",driver,"depth",depth,"ordinal",s.ordinal(),"qply",s.qply(),"check",s.check(),"alpha",s.alpha(),"beta",s.beta(),"static",s.driver(),"shadow",s.shadow(),"allowed",s.standPatAllowed(),"reason",s.reason().toString(),"capture",cap,"values",v==null?null:values(v),"parentValues",pv==null?null:values(pv),"parentDriver",s.parentDriver());
    }
    for(var s:trace.samples())write(out,"type","traceExample","position",p[0],"driver",driver,"depth",depth,"node",s);
   }
  }
 }
 static String sha(Path p)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p)));}
 public static void main(String[] args)throws Exception{
  Path bp=Path.of(args[0]),np=Path.of(args[1]);label=args[3];String mode=args[4];
  try(var input=Files.newInputStream(bp)){brn=Brn2Codec.readModel(input);}try(var input=Files.newInputStream(np)){nnue=NnueNetworkCodec.read(input);}
  bdef=SearchEvaluation.brn2(brn);ndef=SearchEvaluation.incremental(nnue);bs=bdef.newState(6);ns=ndef.newState(6);bfresh=bdef.newState(1);nfresh=ndef.newState(1);fresh=new Brn2Accumulator(brn);for(int i=0;i<acc.length;i++)acc[i]=new Brn2Accumulator(brn);
  try(PrintWriter writer=new PrintWriter(Files.newBufferedWriter(Path.of(args[2]),java.nio.charset.StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW))){out=writer;
   write(out,"type","run","label",label,"mode",mode,"brnPath",bp.toString(),"brnSha",sha(bp),"nnuePath",np.toString(),"nnueSha",sha(np),"boundedIntermediates",brn.boundedIntermediates(),"java",System.getProperty("java.runtime.version"));
   if(mode.equals("primary")||mode.equals("edges"))for(String[] p:POSITIONS){long[] board=Board.fromFen(p[1]);V v=val(board,0,false);write(out,"type","root","position",p[0],"fen",p[1],"values",values(v));edgeCount=0;walk(p[0],board,0,v,-1);write(out,"type","walkEnd","position",p[0],"edgeCount",edgeCount,"capReached",edgeCount>=20000);}
   if(mode.equals("primary"))for(String[] p:POSITIONS){search(p,false,4,1_000_000,true);search(p,true,4,1_000_000,true);}
   if(mode.equals("kiwi")){search(POSITIONS[0],true,4,1_000_000,true);}
   if(mode.equals("replay")){search(POSITIONS[0],false,4,1_000_000,false);search(POSITIONS[0],true,4,1_000_000,false);search(POSITIONS[0],false,3,1_000_000,true);search(POSITIONS[0],true,3,1_000_000,true);}
   write(out,"type","end","consistencyChecks",checks,"brnShaAfter",sha(bp),"nnueShaAfter",sha(np));
  }
 }
}
```
