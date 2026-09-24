# BRN capture-consistency search validation — 2026-09-24

**Primary classification: C — SEARCH VALIDATION POSITIVE. All five unchanged preregistered search gates PASS.** The exact fixed replication λ=2 checkpoint shows a broadly distributed, outlier-robust qsearch benefit over its exact λ=0 control on 24 newly selected positions from 24 independent source-game executions and 24 distinct random-opening groups. This resolves the preceding near-threshold mixed search result **for these fixed checkpoints and this bounded source distribution**. It does not establish universal transfer, playing strength, or production acceptance.

Trace-free primary qnodes fall **461,564→208,265 (54.88%)**, median λ2/λ0 ratio **0.756878**, with **17 improved / 7 regressed / 0 tied**. Removing the largest saving still gives **343,129→185,659 (45.89% lower)**. Every search completes depth 4, without a node/time cap. Exactly one position exceeds 2× control qnodes, within the unchanged guard. All three outcome aggregates and both capture strata improve.

**Recommend the NEXT work unit** to integrate the already-tested loss into the real BRN trainer behind an explicit experimental, default-off boundary, preserving exact λ=0 behavior and enabling a controlled multi-generation λ=0 versus λ=2 campaign. No integration is performed here. Playing-strength and retention testing must be part of future acceptance before considering λ=2 a default. This is **C, not D**: Black-win positions split 4/4, one fresh regression uses the guard allowance, and Fianchetto reproducibly remains severely adverse.

## Scope, governance and relationship to accepted reports

- Date: 2026-09-24 UTC / New Zealand local date. Authoritative repository: `C:\projects\seed\java\seedv6`. Inspected HEAD: **`0e1e1d5168b8d7c80699bb3a4bef42660b65a6ea`**. This is the seventh investigation unit, restricted to fixed-checkpoint SEARCH TRANSFER.
- User-supplied governance and `source/CHESS_SEARCH_CONTRACT.md` apply. No ancestor/repository on-disk `AGENTS.md` was found and no material conflict arose. Exact root `CODEXLOG_CURRENT.md` and `VERSION_STATE.txt` were independently absent; neither was created. First successful UTC observation: `2026-09-24T04:59:25Z`; the immediately preceding unsupported PowerShell `Get-Date -AsUTC` failed, so no earlier time is inferred.
- Initial index empty; **41 modified tracked files / 46 individually enumerated untracked files** were inherited GUI/training/document/build work. All **544 initially present tracked/non-ignored files** retained their captured SHA-256 hashes before report creation. The inherited changes remain unattributed, preserved and excluded from the report commit.
- Clean `git archive HEAD` isolation: `C:\Users\Central\AppData\Local\Temp\seedv6-brn-search-validation-20260924`. **No existing archived file was edited**, including trainer, BRN, NNUE, search, qsearch and mapping. Freshly compiled Java 21 classes use pristine archived code, never inherited `app/bin` or dirty services. Temporary harnesses, game pool, suite, checkpoint copies and outputs stay outside the authoritative worktree.
- The report is the only task-created authoritative file. No weights, optimizer state, calibration, score mapping, pruning, ordering, caps, depth semantics or checkpoint schema changed. No training/update path, promotion match, tournament or strength comparison ran.

| Accepted report / commit | Relationship to this unit |
| --- | --- |
| [Stand-pat diagnostic](BRN_QSEARCH_STAND_PAT_DIAGNOSTIC_2026-09-24.md) — `8ea5cb99b32330c1b85ef8a918dfb24ae5336109` | Local capture inconsistency and shifted score distributions, without an incremental BRN defect; supplies unchanged fixture/search semantics. |
| [Calibration diagnostic](BRN_QSEARCH_CALIBRATION_DIAGNOSTIC_2026-09-24.md) — `b1d224f3a51e5bb720a15e6339db6023291b6ff2` | Calibration effects depend on population/horizon; no calibration is used here. |
| [Initial capture training](BRN_CAPTURE_CONSISTENCY_TRAINING_DIAGNOSTIC_2026-09-24.md) — `339b0d763e12141b24918190fd5b9adc00450c5c` | Teacher-only synthetic training learned geometry but failed search transfer; its recorded positions are novelty exclusions. |
| [Campaign-target replay](BRN_CAMPAIGN_TARGET_CAPTURE_REPLAY_DIAGNOSTIC_2026-09-24.md) — `6eb0df1a1a48c646b1850ceaab5a397c5110eba1` | Authenticated the 50:50 campaign objective and exact saved optimizer/replay; its historical gains are bounded evidence, not substituted checkpoints. |
| [First fresh generation](BRN_FRESH_GENERATION_CAPTURE_REPLAY_DIAGNOSTIC_2026-09-24.md) — `eb46291fa350c9148218ec725bfb453870785979` | Mixed, clustered and outlier-dependent fresh search transfer; only three searched source games. Its full retained records are excluded. |
| [Independent replication](BRN_CAPTURE_CONSISTENCY_REPLICATION_DIAGNOSTIC_2026-09-24.md) — `0e1e1d5168b8d7c80699bb3a4bef42660b65a6ea` | C — REPLICATION MIXED: stable λ=0, improved held-out geometry/loss, but median and outlier gates narrowly failed. Supplies the exact fixed λ=0/2 bytes and unchanged five search gates. Six searched games then versus 24 here. |

All six current report files were independently byte-compared with their accepted report commits. Earlier classifications remain attached to their original protocols. This unit repeats no held-out loss evaluation and makes no new learning claim.

## Exact fixed checkpoint identities

All three inference artifacts were directly available. **No reconstruction and no substitute training were needed.** Source directory for the measured copies: `C:\Users\Central\AppData\Local\Temp\seedv6-brn-replication-20260924`. The following are exact filenames there and in the new external directory:

| Model | SHA-256 |
| --- | --- |
| g134.brn2 | `21daab52baa213bd167a8317bac177d03b95fc3f7837ceefaf836497dc618924` |
| lambda0.brn2 | `0e294b9d9f736b2070c1775935be1633d73b74287d71d86a2dc256435a1260de` |
| lambda2.brn2 | `e23a07e82fe3d3ceab6008f6a5ed362946c65f01e99936bf2adfb4cdebbc40d0` |

Untouched g134 also matches the original store payload at `E:\SeedV6-Networks\BRN\BRN-2\t2\checkpoints\g000134-s000218158-3612e6b9a83ebe1c6c23a62b929d0363a67a4340a637bab40078f07833fe616c\network.brn2`. These are canonical BRN-2/schema 2, width 32 inference payloads. The replication trained both continuation arms once from g134, at final optimizer step 219,785; this unit only decodes inference models.

SHA-256 was recorded before any search and rechecked afterward at both retained originals and new copies. Direct normal model codecs validate and round-trip each payload. Every search process asserts **byte-identical in-memory serialization before/after** and unchanged on-disk bytes. The retained optimizer states were never loaded by search and still match the accepted hashes:

| Optimizer payload (not decoded) | Verified SHA-256 |
| --- | --- |
| g134.state | `ea95c119ea4935f8a28acdf7d5cb854c1535cc2c9219c99c93bdeece369752e9` |
| lambda0.state | `6eed58a27d23e53fe8fd4a00d977d2cf68266aa19f26d1c4da4e3a1bd5e2a2de` |
| lambda2.state | `ad8e2b6631004391cccbaa188e8622436d57a16d60c4f9c1a297e1f9d8a4fff4` |

No NNUE checkpoint was loaded or evaluated in this unit. The secondary stand-pat observer uses the same immutable BRN as a sparse shadow; that shadow supplies no decision.

## Preregistration, source generation and deterministic selection

Protocol recorded **2026-09-24T05:01:50.467249+00:00**, SHA-256 `c601626fb3f4c0ad53efd06c1ea00644031ed62ea5bea391a9473abeca6037b8`, before generation or comparative results. Fixed root seed: **2026092403**. The sole secondary RNG seed is Java `new SplittableRandom(2026092403L ^ 0x6A09E667F3BCC909L).nextLong()` = **−5161967410789381145**. For original candidate index `i`, native `SelfPlayRunner.gameSeed(seed,i)` is `SplittableRandom(seed + 0x9E3779B97F4A7C15L*i).nextLong()` with signed 64-bit arithmetic. No RNG stream or seed was changed.

Before games, preview candidates 0–4,095 in index order with existing `ValidationArena.opening` from the standard starting board. Admit the first **96** active, distinct opening endpoint keys having **4–8 randomized plies**. The helper is used only to preview native random openings; no validation arena/match runs. Each selected original index then goes through unchanged `SelfPlayRunner.play(SearchEvaluation.handcrafted(), ...)`, depth 4, six root workers, maximum 1,024 plies, native 0–8 random-opening algorithm, no per-move node/time cap. Games execute sequentially with native private TT/order state per game. Neither compared BRN nor NNUE is involved in source generation/selection. This is source-game construction, not a strength comparison.

The bounded pool completed **96 games / 96 distinct opening groups**, **43 White wins / 10 draws / 43 Black wins**, with **10,828 pre-move positions**. One game per fine group; all full trajectory-key sequences are distinct. Admitted candidate indices end at **181**; opening lengths 4/5/6/7/8 occur **15/18/18/25/20** times. Every actual opening endpoint matches its preview. Full legal move replay reproduces every final board and real termination; no outcome is invented, relabeled, or assigned to an incomplete game. No additional pool was generated.

The suite draws from all recorded trajectory positions, rather than the previous 32-sample training grid. Eligibility is fixed board/source logic: pre-move ply ≥12, at least eight plies before actual game end, active root, ≥2 legal moves, ≥1 legal capture, and **every legal one-ply child remains active** under singleton-history rule adjudication. This excludes immediate terminal/draw options without an evaluator or deeper search oracle. It does not guarantee the absence of a forced ending beyond one ply. All 5,743 structurally eligible rows were novel to the exclusion union.

**Tactical/capture-rich (T): ≥3 legal captures. Quieter-capture (Q): 1–2 legal captures.** Capture counting uses the established native move flags and tactical en-passant handling. No evaluation magnitude, qnode count, preferred move, or score volatility is a selection input.

For each row, compute SHA-256 of UTF-8 `2026092403|position|GAME|PLY|KEY`, where GAME/PLY are decimal and KEY is native unsigned lowercase unpadded hex. Sort games by SHA-256 of `2026092403|game|GAME`. For each outcome in White-win / draw / Black-win order, deterministically match eight slots `T,Q,T,Q,T,Q,T,Q` to distinct eligible games using the hash-ordered augmenting-path procedure reproduced in the appendix; choose each assigned game/stratum’s smallest available position hash. No game or key can be selected twice. The preregistered shortage fallback minimizes deviations from 8/8/8 and 12/12 before hash order; **it was not needed**. Available games with T/Q candidates: White **39/39**, draw **10/10**, Black **41/41**.

The exact target was feasible: **24 selected positions, 24 distinct games, 24 distinct opening endpoint groups, one per group; 8/8/8 outcomes; 12 T / 12 Q**, with four T and four Q in each outcome. There are **15** higher-level families, defined before search by the game’s **first actual random-opening move coordinate**. These are descriptive first-move groups, not invented ECO classifications or proof of independent opening theory.

Suite frozen **2026-09-24T05:06:01.162990+00:00**. Full identity JSONL SHA-256: `c7aa27bb205d2a66d0d8f6be265bbc1d3fabe6017526e1f0f1cf07cc8380f7ee`; exact benchmark TSV SHA-256: `9aaad2d4cb46f4409ec3dfbe9d2af3732a5254a1ae18ea98482171197cf494b1`. A second independent execution of the selector reproduced the identity JSONL byte-for-byte. First compared search started **2026-09-24T05:07:32.825102500Z**; all 72 primary requests ended before any continuity request. No comparative result influenced inclusion.

Distinct indexed game seeds were verified against all 134 saved generation domains, the first fresh generation and replication: zero overlap. Bounded full regeneration of pool games **0 and 95** reproduced their entire trajectory-key sequences and outcomes exactly. This is verification of existing sources, not an extension of the candidate pool. The numeric root seed also occurred in an earlier synthetic-walk diagnostic, but its generation domain/algorithm differs; exact position-key exclusions, not the seed label alone, establish novelty.

## Novelty audit and limits

All six prior temporary evidence directories were retained. Read-only independent parsing verified checksummed frames and plan linkage for **134 saved historical data files**, reproducing the accepted **144,051 TRAIN keys / 176,859 train-or-held keys**. The exact g133-parent replay batch contributes **1,800** unique train-or-held keys. Prior fresh-generation and replication frozen manifests and recorded binary corpus hashes were authenticated. Their retained parent/selected-child endpoints, partitions, prior suite positions and replication trajectories were scanned alongside earlier synthetic, calibration and actual-search records.

| Exclusion population | Distinct collected keys | New-suite overlap |
| --- | --- | --- |
| historicalTrain | 144,051 | 0 |
| historicalAll | 176,859 | 0 |
| g133Replay | 1,800 | 0 |
| diagnostic | 2,487 | 0 |
| calibration | 85,486 | 0 |
| capture-consistency | 64,415 | 0 |
| campaign-replay | 16,916 | 0 |
| fresh-generation | 231,256 | 0 |
| replication | 208,857 | 0 |

The six prior directories supplied **112 JSONL files**, plus retained key lists. Recursive key/board extraction and retained endpoint-key lists deliberately exclude more than only prior TRAIN/HELD roots. Direct supplemental checks confirm that all 7,200 initial synthetic pair endpoints and 2,957 campaign replay pair endpoints were already present in the frozen exclusion union; none touches the selected suite. There are **386,292** keys before FEN expansion. Parsing **3,737 distinct recorded FEN strings** from prior records, all root BRN reports and tool-source fixtures yields a combined **386,327-key** exclusion union. Kiwipete, Fianchetto, Opening, en-passant and all discoverable previously named diagnostic benchmark FENs are included. The selected suite has **zero intersections with every individual exclusion set**, 24 unique native keys and 24 unique FEN first-four-field representations.

Native position keys ignore move counters and detect ordinary transpositions; native FEN round-trip equality was asserted for every recorded source board. No prior temporary corpus required for the declared audit was missing. Limits remain: saved historical datasets contain sampled training/held boards, not every historical game trajectory or search descendant; undocumented/unsaved exposure, symmetry-equivalence, and theoretical Zobrist collisions are not excluded. Fresh source executions and distinct opening endpoints are not a claim that all chess positions are statistically IID. Conditioning on 4–8 random opening plies, capture availability, terminal-distance filters and outcome quotas defines the tested distribution.

## Frozen 24-position suite

Game/group identifiers refer to this new 96-game pool, not earlier experiment IDs. The opening key identifies the fine source group. Each row below is one distinct source game/group; W/D/B mean White-win/draw/Black-win. Full deterministic selection hashes are retained without abbreviation.

| ID | Game / candidate | Outcome | Ply | T/Q; captures | Position key | Selection SHA-256 | FEN |
| --- | --- | --- | --- | --- | --- | --- | --- |
| sv-01 | 28 / 54 | W | 13 | T; 3 | `4ca9ac27af6b841d` | `05701225d998b3faee43f6051b8f8d302023d9a5191d434d3162be0012e3897d` | `r1bqkb1r/p3pppp/p1n5/2pp4/6n1/2N1PN2/1PPP1PPP/R1BQKB1R b KQkq - 1 7` |
| sv-02 | 11 / 29 | W | 59 | Q; 1 | `bf4e50db747ecd6b` | `03b590cea5c9b590891edd92936590a4f2c01b3dff6238f57714c74b4d4562c0` | `2r2k2/3bn3/p2B2p1/2P1R3/4p3/2P1P3/P5P1/4K3 b - - 1 30` |
| sv-03 | 6 / 16 | W | 77 | T; 3 | `8727a9242ec53994` | `0d2c4c4dbead712015f608fdf29db84dde59afd93d1429148401997568eaee81` | `RR6/5p1k/1b5p/1r1rP2P/KP4p1/6B1/5PP1/8 b - - 1 39` |
| sv-04 | 91 / 172 | W | 13 | Q; 2 | `a7569ae2cd8acf5` | `004b0916faa5fda8173ad99c8bc45b0317e5438b5184b159a7ab1b5f8064a9e9` | `rnbq1knr/ppppp1b1/8/5p1B/3P3B/2N1P3/PPP2PPP/R2QK1NR b KQ - 0 7` |
| sv-05 | 3 / 8 | W | 36 | T; 3 | `c8a691db24a49f91` | `1d2bb168daaaf6423635faa17ce06c74e1d52135c0fbf9b17e70423c9ba0fa10` | `rnb5/pp3kb1/3Bq1p1/5p2/4P3/1P1B2Q1/P1P2PP1/2KR4 w - - 1 19` |
| sv-06 | 0 / 0 | W | 24 | Q; 2 | `45e2775de559120` | `37c3db2d0fc179e7914160d6d1395471b718634e5c567a8bbb3ee92316e2f065` | `r4rk1/pp1q1pp1/2np1b1p/2p1p2P/8/2NBPQ2/PPPP1PP1/R1B2K2 w - - 0 13` |
| sv-07 | 21 / 45 | W | 15 | T; 4 | `205ed47f833630e5` | `17805fa3bc63861239b91b15f486647c6ef846d15ed6d69d63fe8b692639c6cf` | `r1b1kb1r/p3pppp/pqp5/3p1P2/4P1nP/8/PPPPQ3/RNB1K1NR b KQkq - 0 8` |
| sv-08 | 47 / 87 | W | 49 | Q; 2 | `cf1e8e38b239b4d2` | `00bad346a47aa9d36b4cd0aedf12a0dbbb725de5e2b2302d2da3b34d484d05da` | `r5n1/pp2p1B1/4P2p/2pp1k2/Rb1P4/5P2/1PP3P1/3K3R b - - 3 25` |
| sv-09 | 51 / 96 | D | 21 | T; 3 | `e50704ab5fe6df0f` | `03be9cc392f4a6dc631e7205d474e6a20fa905fa345ba6d3a713c574a22f336a` | `r2qk1nr/1pp2ppp/p3b3/8/P1Bn4/2PP3N/2P2PPP/R1BQ1RK1 b - - 0 11` |
| sv-10 | 53 / 98 | D | 120 | Q; 1 | `59ecc860ec9e9670` | `07503a30b0f71fa03f03f81a8b9ae92ee7cc69eb81b08e8afc3118d7fa82f9df` | `8/1k6/p3R3/2p5/2K5/1P6/5b2/8 w - - 12 61` |
| sv-11 | 25 / 50 | D | 56 | T; 3 | `fb9db59eef56972f` | `0d9b2fa683c93e9fe650a6cdb41603dbfad21a7ee9f074191b581346fa88c8e8` | `2r2r1k/4n1pp/pp3p2/4pP1R/4B3/3p4/PPP3PP/4R2K w - - 0 29` |
| sv-12 | 95 / 181 | D | 16 | Q; 1 | `13a31c19398c358d` | `000cb4bd9183a77c8ea8956ce1ca89c17665a50da6c480e6c0fe54db2e071b8d` | `r3kbnr/pp1n1ppp/2p5/3pP3/1q1Q1P2/8/P1P2PPP/RNB1KB1R w KQkq - 0 9` |
| sv-13 | 83 / 155 | D | 32 | T; 5 | `8e86dac36c8ec1e6` | `19b98ec9d2e44a729c1d2cbf261f923a856f2f840c0ef289b0cc8224fbfcacbb` | `2kr2nr/pbp3Rp/4p3/1q2pnN1/3p4/3P4/1PPBPP1P/1R1QKB2 w - - 5 17` |
| sv-14 | 84 / 157 | D | 43 | Q; 1 | `3eb85b642846f4f1` | `2afbda1975332cbef3f500c579db65a32c676dd4319e644074580ad235704cdb` | `1n3b2/2p1k1p1/5pQ1/4p3/qpn5/5P2/2P2RPP/2B1KN2 b - - 3 22` |
| sv-15 | 87 / 163 | D | 18 | T; 3 | `9da86b1335a4af99` | `0244c23f966a753d4bdbd3e0b62a84370a4b31ac65dc383259d1a0bd118aa621` | `r2qk1nr/pbpppp2/1pn3p1/4b1B1/2P5/2NP2pP/PP1QPPB1/R3K1NR w KQkq - 0 10` |
| sv-16 | 85 / 159 | D | 95 | Q; 1 | `8997ef1ce317ee66` | `003842fefd468dbed03d0b1bdeb08e8de1797dee86291c33efb65aa7a4bcfa89` | `8/8/7k/5pp1/3R2N1/2r1P1K1/8/8 b - - 0 48` |
| sv-17 | 39 / 70 | B | 25 | T; 4 | `e3c768cc3d8bde14` | `07b9b76b608c390bdd0f77dee3849a4019021e40f7c6cbb933cb6de652f9434e` | `r1bqkb1r/1p3pp1/2n5/p2pP1Pp/2p5/P1P1PN1P/1P6/R1BQKB1R b KQkq - 0 13` |
| sv-18 | 9 / 23 | B | 46 | Q; 2 | `52c986c15349444` | `04e7a57abb243b10fe3deaa16d891e453601f19b7e36b0e82e869920c58874e0` | `2k5/3nb3/2p1pp2/rp1p4/P2P2pr/2P1P3/2B2PR1/RN3K2 w - - 0 24` |
| sv-19 | 61 / 111 | B | 65 | T; 3 | `e82658c9fd8a7728` | `0e9de782123e24c2684fec01153cb17a9e91ff0af1bbbaaa9d52303f441f833b` | `6k1/1pp2ppp/4b3/p1P5/1r6/2K5/P1R3PP/2r2B1R b - - 13 33` |
| sv-20 | 60 / 110 | B | 111 | Q; 1 | `65b84ced89e0eb4b` | `01cd60f93fcb3c20b0d2ef29ac16204c8498562ff808f2bf643026767e3be158` | `8/8/8/3k4/6R1/5q2/7K/8 b - - 0 56` |
| sv-21 | 62 / 112 | B | 20 | T; 4 | `486c87cc57779ac8` | `02e79d249b00e41b9ccbac5884c8dba6429aa132bc038827342c985b9519f447` | `rn1qkb1r/2pp1p1p/pp3n2/4pNp1/Q1P1b3/5N1P/PP1PPPB1/R1B1K2R w KQkq - 1 11` |
| sv-22 | 66 / 122 | B | 52 | Q; 1 | `223292b3b30200f3` | `00a1ed189ac4b0699264d7e2b10f743bcb83477fc2ae4744ceb8c6f1758e145b` | `r4r2/pp5k/4b1p1/2R5/2p1P1n1/P3P3/1P4BP/4R1K1 w - - 4 27` |
| sv-23 | 69 / 126 | B | 29 | T; 4 | `ad448bd3577e4e63` | `06f978b7e00246622af5b5d8f3deed0068e0f8b838ced2488dee2e3b7873d073` | `rn2kb1r/p3pppp/1P6/q3P3/3Bp3/7N/P1P2PPP/1R1Q2KR b kq - 0 15` |
| sv-24 | 38 / 66 | B | 115 | Q; 1 | `9307f1db18a981ea` | `05e0d903100295e13af179b3a21079f7bbe8c283da8bbd55d5e50b7aabbc4fe8` | `8/1Rb5/p1r5/P7/KPk5/8/8/8 b - - 10 58` |

| ID | Source game seed | Opening group key | First-move family | Opening endpoint FEN |
| --- | --- | --- | --- | --- |
| sv-01 | -6573887862863430247 | `53022f1489779694` | a2a3 | `rnbqkb1r/pp1ppppp/8/P1p5/6n1/5N2/1PPPPPPP/RNBQKB1R b KQkq - 0 4` |
| sv-02 | 9144067217637006120 | `bfd2ee09be9b0a08` | h2h4 | `rnbqkbnr/1ppp1p1p/p3p1p1/8/2P4P/8/PP1PPPPR/RNBQKBN1 w Qkq - 0 4` |
| sv-03 | -7230986688325518728 | `6d90dad75b46903` | c2c3 | `rnbqkbnr/pp3ppp/3p4/2p1p3/7P/1PPP4/P3PPP1/RNBQKBNR b KQkq - 0 4` |
| sv-04 | 7983792957654640737 | `c1570a96066b8304` | b1c3 | `rnbqkbnr/ppppp2p/8/5pp1/3P4/2N5/PPP1PPPP/R1BQKBNR w KQkq f6 0 3` |
| sv-05 | -4882489073770866068 | `590a105b3f824651` | h2h4 | `rnb1kbnr/pp1qpp1p/2pp2p1/6NP/8/8/PPPPPPP1/RNBQKB1R w KQkq - 2 5` |
| sv-06 | -304081875162657490 | `73741403cd3cb28e` | e2e3 | `rn1qkbnr/pp2pppp/3p4/2p2b2/7P/3BP2R/PPPP1PP1/RNBQK1N1 b Qkq - 1 4` |
| sv-07 | 2443105855311760161 | `7cbbdf4ad24e2210` | h2h4 | `r1bqkb1r/pppppppp/n6n/8/5PPP/8/PPPPP3/RNBQKBNR b KQkq g3 0 3` |
| sv-08 | 3619514664875295172 | `c6ee575714dbd9` | h2h3 | `rnbqk1nr/ppppp1bp/B4p2/6p1/4P2P/8/PPPP1PP1/RNBQK1NR b KQkq - 0 4` |
| sv-09 | 7940056922593790714 | `be320a43b57f80e4` | b1c3 | `rnbq1bnr/ppppkppp/4p3/8/8/2N4N/PPPPPPPP/R1BQKB1R w KQ - 2 3` |
| sv-10 | 4649594463566045455 | `e9c7681608eef601` | a2a4 | `rnbqkbnr/pppp2pp/5p2/4p3/P7/RPN5/2PPPPPP/2BQKBNR w Kkq - 3 5` |
| sv-11 | 3778178902768404334 | `b85b429c44d2f6b0` | g1h3 | `rnbqkbnr/p2ppppp/1p6/2p5/8/8/PPPPPPPP/RNBQKBNR w KQkq c6 0 3` |
| sv-12 | 7687618223822546151 | `197d5bf3c98b4182` | b2b4 | `rnbqkbnr/pp3ppp/2p5/3pp3/1P1P4/8/P1PQPPPP/RNB1KBNR w KQkq - 0 4` |
| sv-13 | 456061357349631602 | `951e8c4e94bb310d` | b1c3 | `rnbqk1nr/ppp1ppbp/8/3p2p1/P5P1/2N4N/1PPPPP1P/R1BQKB1R b KQkq g3 0 4` |
| sv-14 | 4669178009766154527 | `277233132db7299e` | b2b4 | `rnbqkbnr/2ppp1pp/5p2/pp6/1P5N/8/PBPPPPPP/RN1QKB1R b KQkq - 1 4` |
| sv-15 | 1584034164413357797 | `85c1ed673f716437` | g2g3 | `r1bqkbnr/p1ppppp1/1pn5/7p/2P3P1/3P4/PP2PP1P/RNBQKBNR b KQkq c3 0 4` |
| sv-16 | 1365634864079962138 | `f25b16f02bf823fe` | g1f3 | `r1bqkbnr/pp1p1ppp/n7/2p5/1P2p3/2P2N2/P2PPPPP/RNBQKBR1 w Qkq - 0 5` |
| sv-17 | -2764422686466391374 | `dfc7dce4f72e44fa` | c2c3 | `rnbqkbnr/1p1p1ppp/8/p1p1p3/6P1/P1P2N2/1P1PPP1P/RNBQKB1R b KQkq - 1 4` |
| sv-18 | -5851283663681334208 | `6f71608a1f229b22` | b2b4 | `rn1qkbnr/p1p1pppp/1p1p4/8/1P6/3BPN1b/P1PP1PPP/RNBQK2R b KQkq - 2 4` |
| sv-19 | 4994738086780591761 | `e7cbab484ce1d116` | b2b3 | `1nbqkb1r/rppppppp/5n2/p7/2P5/1P3P2/P2PP1PP/RNBQKBNR b KQk - 0 4` |
| sv-20 | 4916785283674306468 | `79f79ce0505a8110` | a2a3 | `rnbqkb1r/pp1ppppp/2p5/5n2/P2N4/8/1PPPPPPP/RNBQKB1R b KQkq - 1 4` |
| sv-21 | 7899088657059600772 | `aa344ff2e90750cd` | c2c4 | `rnbqkb1r/pppp1p1p/4pn2/6p1/2P3P1/N7/PP1PPP1P/R1BQKBNR w KQkq - 1 4` |
| sv-22 | -2189857773996801481 | `3fb1bd4056022260` | g1h3 | `rnbqkbnr/pppp2pp/8/4pp2/5NP1/8/PPPPPP1P/RNBQKB1R w KQkq f6 0 4` |
| sv-23 | 2096143671631157610 | `cfb0d6df952e248c` | e2e4 | `rnbqkbnr/p2ppppp/1pp5/8/4P3/8/PPPPKPPP/RNBQ1BNR w kq - 0 3` |
| sv-24 | -7241088357236057780 | `8302c04bbe485587` | f2f4 | `rnbqkbnr/pp1ppp2/2p3p1/7p/5PP1/1PP5/P2PP2P/RNBQKBNR b KQkq - 0 4` |

## Search conditions and trace-free primary results

Unchanged production `AlphaBetaPvsSearch(SearchEvaluation.brn2(model), 1<<18)` inside ordinary `IterativeDeepeningSearch`: requested **depth 4**, **one worker**, fresh **262,144-entry TT** and fresh ordering state per request, singleton root `GameHistory`, cumulative **1,000,000 entered-node / 60-second caps**, diagnostics enabled. Original mate-distance-only neural selectivity, aspiration disabled, soft q-ply limit 16 and check evasions remain unchanged. All models use the original BRN normal integer mapping. No tuning or raised cap follows a result.

**All node/depth/score/move/time values below come from normal trace-free runs.** Qnodes are entered qsearch children; main leaves reused as qsearch roots are not counted twice. Q%=q/total entered nodes. SP%=cutoffs/eligible attempts. Normal diagnostics expose cutoffs, but not eligible attempts. Therefore, after all primary/continuity runs and bounded trace-free replays, the unchanged `QsearchDecisionTrace` collects attempts in secondary replays. Each of all **84** traced requests exactly matches its normal request’s depth, status, total/main/q nodes, all worker diagnostics, score and move. The sparse same-BRN shadow uses static stride `Integer.MAX_VALUE`, retains no detached position samples and supplies no search decisions; attempt/cutoff counts are exact for all observed statics, not sparse estimates. No product instrumentation was added.

Every row completes depth 4, with **no node cap, time cap or failure**. Each run asserts unchanged root board and exact authoritative node accounting. Scores are native mapped search units, **not centipawns**. Timing is secondary, includes JVM/JIT and incidental load, and is not a warmed throughput comparison.

| Position | Model | Depth / cap | Total nodes | Qnodes | Q% | SP attempts | SP cutoffs | SP% | Root score / move | Seconds |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| sv-01 | g134 | 4 / none | 54,656 | 47,050 | 86.08% | 48,657 | 22,238 | 45.70% | 146 / g4f2 | 0.6274 |
| sv-01 | lambda0 | 4 / none | 130,960 | 118,435 | 90.44% | 115,852 | 35,496 | 30.64% | -8941 / g4h2 | 1.0512 |
| sv-01 | lambda2 | 4 / none | 31,534 | 22,606 | 71.69% | 28,764 | 16,167 | 56.21% | -1071 / g4f2 | 0.4266 |
| sv-02 | g134 | 4 / none | 5,615 | 2,631 | 46.86% | 4,439 | 2,599 | 58.55% | -13947 / d7b5 | 0.0317 |
| sv-02 | lambda0 | 4 / none | 4,190 | 1,729 | 41.26% | 3,289 | 2,304 | 70.05% | -12286 / d7b5 | 0.0179 |
| sv-02 | lambda2 | 4 / none | 3,097 | 1,222 | 39.46% | 2,386 | 1,726 | 72.34% | -10092 / c8e8 | 0.0138 |
| sv-03 | g134 | 4 / none | 8,307 | 4,499 | 54.16% | 6,923 | 3,534 | 51.05% | -5558 / b5b4 | 0.0456 |
| sv-03 | lambda0 | 4 / none | 9,170 | 5,376 | 58.63% | 7,671 | 3,719 | 48.48% | -5105 / b5b4 | 0.0389 |
| sv-03 | lambda2 | 4 / none | 5,342 | 2,911 | 54.49% | 4,310 | 2,327 | 53.99% | -1727 / h7g7 | 0.0270 |
| sv-04 | g134 | 4 / none | 57,666 | 39,357 | 68.25% | 50,848 | 26,899 | 52.90% | -10112 / h8h5 | 0.4744 |
| sv-04 | lambda0 | 4 / none | 46,060 | 30,233 | 65.64% | 41,008 | 19,662 | 47.95% | -12224 / b8c6 | 0.3349 |
| sv-04 | lambda2 | 4 / none | 12,855 | 6,734 | 52.38% | 11,020 | 7,143 | 64.82% | -9331 / h8h5 | 0.1095 |
| sv-05 | g134 | 4 / none | 24,842 | 16,386 | 65.96% | 20,260 | 13,189 | 65.10% | 5587 / g3g5 | 0.1266 |
| sv-05 | lambda0 | 4 / none | 23,850 | 15,167 | 63.59% | 19,590 | 13,108 | 66.91% | 8222 / g3f4 | 0.1232 |
| sv-05 | lambda2 | 4 / none | 24,920 | 12,793 | 51.34% | 20,646 | 15,822 | 76.63% | 6041 / g3f4 | 0.1510 |
| sv-06 | g134 | 4 / none | 20,075 | 10,165 | 50.64% | 15,994 | 9,249 | 57.83% | 2521 / c3d5 | 0.1416 |
| sv-06 | lambda0 | 4 / none | 17,435 | 7,212 | 41.37% | 14,323 | 9,539 | 66.60% | -407 / f3e4 | 0.1305 |
| sv-06 | lambda2 | 4 / none | 13,625 | 5,820 | 42.72% | 10,992 | 7,515 | 68.37% | 5130 / f1g1 | 0.1048 |
| sv-07 | g134 | 4 / none | 18,994 | 12,944 | 68.15% | 15,963 | 10,363 | 64.92% | -1113 / b6c7 | 0.1446 |
| sv-07 | lambda0 | 4 / none | 14,939 | 9,053 | 60.60% | 12,181 | 8,801 | 72.25% | 3904 / b6c7 | 0.1134 |
| sv-07 | lambda2 | 4 / none | 16,923 | 9,256 | 54.69% | 13,829 | 10,523 | 76.09% | 5602 / b6c7 | 0.1283 |
| sv-08 | g134 | 4 / none | 10,075 | 3,606 | 35.79% | 8,500 | 6,866 | 80.78% | -8463 / g8f6 | 0.0551 |
| sv-08 | lambda0 | 4 / none | 9,630 | 4,965 | 51.56% | 8,234 | 5,787 | 70.28% | -10227 / f5g6 | 0.0636 |
| sv-08 | lambda2 | 4 / none | 8,854 | 3,197 | 36.11% | 7,380 | 6,202 | 84.04% | -6305 / f5g6 | 0.0450 |
| sv-09 | g134 | 4 / none | 14,369 | 8,073 | 56.18% | 11,755 | 7,355 | 62.57% | 2766 / e6c4 | 0.0983 |
| sv-09 | lambda0 | 4 / none | 11,567 | 5,477 | 47.35% | 9,196 | 6,952 | 75.60% | 10291 / e6c4 | 0.0765 |
| sv-09 | lambda2 | 4 / none | 11,262 | 5,333 | 47.35% | 8,850 | 6,983 | 78.90% | 8162 / e6c4 | 0.0737 |
| sv-10 | g134 | 4 / none | 3,378 | 779 | 23.06% | 2,496 | 1,444 | 57.85% | 4818 / e6e7 | 0.0111 |
| sv-10 | lambda0 | 4 / none | 2,626 | 812 | 30.92% | 1,970 | 904 | 45.89% | 3417 / e6f6 | 0.0075 |
| sv-10 | lambda2 | 4 / none | 2,451 | 662 | 27.01% | 1,834 | 1,018 | 55.51% | 3174 / e6d6 | 0.0066 |
| sv-11 | g134 | 4 / none | 17,934 | 9,911 | 55.26% | 14,618 | 8,067 | 55.19% | -1202 / c2c3 | 0.1028 |
| sv-11 | lambda0 | 4 / none | 9,736 | 4,515 | 46.37% | 7,540 | 5,059 | 67.10% | -4365 / c2d3 | 0.0542 |
| sv-11 | lambda2 | 4 / none | 9,381 | 3,991 | 42.54% | 7,104 | 5,102 | 71.82% | 1076 / e4d3 | 0.0634 |
| sv-12 | g134 | 4 / none | 6,315 | 2,263 | 35.84% | 5,297 | 3,994 | 75.40% | 5568 / d4b4 | 0.0540 |
| sv-12 | lambda0 | 4 / none | 11,362 | 5,839 | 51.39% | 9,411 | 5,673 | 60.28% | -4810 / d4b4 | 0.0832 |
| sv-12 | lambda2 | 4 / none | 2,966 | 1,120 | 37.76% | 2,363 | 1,907 | 80.70% | 1797 / d4b4 | 0.0230 |
| sv-13 | g134 | 4 / none | 20,922 | 14,093 | 67.36% | 18,500 | 9,842 | 53.20% | 19534 / e2e4 | 0.1467 |
| sv-13 | lambda0 | 4 / none | 48,128 | 40,179 | 83.48% | 43,357 | 16,225 | 37.42% | 21717 / g7f7 | 0.3036 |
| sv-13 | lambda2 | 4 / none | 27,767 | 20,337 | 73.24% | 24,756 | 12,071 | 48.76% | 17746 / g7c7 | 0.1850 |
| sv-14 | g134 | 4 / none | 7,374 | 3,464 | 46.98% | 5,136 | 2,934 | 57.13% | 6266 / a4a1 | 0.0422 |
| sv-14 | lambda0 | 4 / none | 5,356 | 1,938 | 36.18% | 3,575 | 1,966 | 54.99% | 3941 / a4a1 | 0.0261 |
| sv-14 | lambda2 | 4 / none | 5,585 | 2,248 | 40.25% | 3,912 | 2,523 | 64.49% | 4585 / a4a1 | 0.0279 |
| sv-15 | g134 | 4 / none | 9,994 | 5,215 | 52.18% | 8,305 | 6,163 | 74.21% | -7569 / f2f4 | 0.0801 |
| sv-15 | lambda0 | 4 / none | 16,656 | 11,283 | 67.74% | 14,311 | 8,390 | 58.63% | -3523 / f2f4 | 0.1241 |
| sv-15 | lambda2 | 4 / none | 9,746 | 5,682 | 58.30% | 8,036 | 5,742 | 71.45% | -2992 / f2f4 | 0.0774 |
| sv-16 | g134 | 4 / none | 1,364 | 423 | 31.01% | 1,081 | 861 | 79.65% | 2497 / f5g4 | 0.0032 |
| sv-16 | lambda0 | 4 / none | 1,518 | 455 | 29.97% | 1,169 | 873 | 74.68% | 322 / f5g4 | 0.0045 |
| sv-16 | lambda2 | 4 / none | 1,557 | 512 | 32.88% | 1,229 | 978 | 79.58% | 1017 / f5g4 | 0.0036 |
| sv-17 | g134 | 4 / none | 65,154 | 55,182 | 84.69% | 56,716 | 29,431 | 51.89% | 6066 / d8c7 | 0.4847 |
| sv-17 | lambda0 | 4 / none | 116,405 | 107,783 | 92.59% | 103,322 | 47,236 | 45.72% | 7525 / g7g6 | 1.0669 |
| sv-17 | lambda2 | 4 / none | 31,325 | 20,424 | 65.20% | 27,584 | 19,592 | 71.03% | 7589 / g7g6 | 0.2277 |
| sv-18 | g134 | 4 / none | 17,996 | 10,221 | 56.80% | 15,986 | 10,166 | 63.59% | -10122 / g2g4 | 0.1425 |
| sv-18 | lambda0 | 4 / none | 38,686 | 27,550 | 71.21% | 35,094 | 17,966 | 51.19% | -11788 / g2g4 | 0.2798 |
| sv-18 | lambda2 | 4 / none | 16,135 | 9,280 | 57.51% | 14,399 | 9,503 | 66.00% | -7656 / f1e1 | 0.0818 |
| sv-19 | g134 | 4 / none | 13,719 | 6,085 | 44.35% | 11,145 | 6,183 | 55.48% | 10095 / c1d1 | 0.0940 |
| sv-19 | lambda0 | 4 / none | 9,955 | 3,975 | 39.93% | 7,735 | 4,369 | 56.48% | 7508 / c1d1 | 0.0609 |
| sv-19 | lambda2 | 4 / none | 11,087 | 4,379 | 39.50% | 8,770 | 5,847 | 66.67% | 8637 / c1a1 | 0.0482 |
| sv-20 | g134 | 4 / none | 3,046 | 1,165 | 38.25% | 1,922 | 1,288 | 67.01% | 17896 / f3g4 | 0.0087 |
| sv-20 | lambda0 | 4 / none | 2,399 | 492 | 20.51% | 1,318 | 554 | 42.03% | 18011 / f3g4 | 0.0084 |
| sv-20 | lambda2 | 4 / none | 3,048 | 1,167 | 38.29% | 1,925 | 1,289 | 66.96% | 16715 / f3g4 | 0.0075 |
| sv-21 | g134 | 4 / none | 24,533 | 16,486 | 67.20% | 20,883 | 13,303 | 63.70% | -2513 / f3e5 | 0.2783 |
| sv-21 | lambda0 | 4 / none | 41,648 | 34,833 | 83.64% | 36,184 | 18,978 | 52.45% | 1559 / d2d3 | 0.4276 |
| sv-21 | lambda2 | 4 / none | 67,056 | 52,246 | 77.91% | 58,803 | 33,411 | 56.82% | 1267 / d2d3 | 0.5063 |
| sv-22 | g134 | 4 / none | 7,354 | 3,297 | 44.83% | 5,989 | 3,538 | 59.07% | 8412 / g2h3 | 0.0512 |
| sv-22 | lambda0 | 4 / none | 10,075 | 4,766 | 47.31% | 8,311 | 4,824 | 58.04% | 4013 / g2h3 | 0.0716 |
| sv-22 | lambda2 | 4 / none | 7,851 | 3,103 | 39.52% | 6,396 | 4,046 | 63.26% | 3422 / g2h3 | 0.0369 |
| sv-23 | g134 | 4 / none | 27,394 | 22,559 | 82.35% | 23,831 | 10,659 | 44.73% | 99 / b8c6 | 0.2303 |
| sv-23 | lambda0 | 4 / none | 26,318 | 18,922 | 71.90% | 22,940 | 11,121 | 48.48% | -1784 / b8c6 | 0.2345 |
| sv-23 | lambda2 | 4 / none | 19,605 | 12,578 | 64.16% | 16,966 | 10,131 | 59.71% | 1103 / b8c6 | 0.1304 |
| sv-24 | g134 | 4 / none | 2,325 | 570 | 24.52% | 1,670 | 1,079 | 64.61% | 7904 / c7e5 | 0.0053 |
| sv-24 | lambda0 | 4 / none | 2,281 | 575 | 25.21% | 1,617 | 1,064 | 65.80% | 6611 / c7e5 | 0.0080 |
| sv-24 | lambda2 | 4 / none | 2,894 | 664 | 22.94% | 2,044 | 1,394 | 68.20% | 6878 / c7e5 | 0.0076 |

| Checkpoint | Total nodes | Qnodes | Q% | SP attempts | SP cutoffs | Aggregate SP% |
| --- | --- | --- | --- | --- | --- | --- |
| g134 | 443,401 | 296,424 | 66.85% | 376,914 | 211,244 | 56.05% |
| lambda0 | 610,950 | 461,564 | 75.55% | 529,198 | 250,570 | 47.35% |
| lambda2 | 346,866 | 208,265 | 60.04% | 294,298 | 188,962 | 64.21% |

Primary λ=2 versus λ=0: **54.88% fewer qnodes**, median ratio **0.756878**, **17/7/0** improved/regressed/tied. Total nodes fall **610,950→346,866 (43.23%)**. Against untouched g134, λ=0 aggregate qnodes are **55.71% higher**, but median per-position λ0/g134 ratio is only **1.025567**; continuation effects are heterogeneous. λ=2 is **29.74% below g134** in aggregate and median λ2/g134 ratio is **0.717359**. The principal causal comparison remains λ=2 versus its equally trained λ=0 control.

## Unchanged preregistered gates and outlier analysis

| Gate | Unchanged threshold | Observed | Verdict |
| --- | --- | --- | --- |
| 1 — Median ratio | ≤0.90 | 0.756878 | PASS |
| 2 — Position improvements | ≥60%; at least 15/24; ties excluded | 17/24 = 70.83%; 7 regress / 0 tie | PASS |
| 3 — Aggregate reduction | ≥15% | 461,564→208,265; 54.88% lower | PASS |
| 4 — Largest-saving removal | ≥10% reduction on remaining 23 | Remove sv-01: 343,129→185,659; 45.89% lower | PASS |
| 5 — Severe-regression guard | At most one >2×; no newly lost depth or cap | One >2× (sv-20); zero new depth losses/caps | PASS |

All five gates pass **without threshold changes**. Their population is exactly the 24 new positions; no continuity fixture enters these calculations. Outcomes/strata/family sensitivities below are descriptive, not additional post-hoc gates.

- Largest absolute saving: **sv-01, 118,435→22,606**, saving **95,829 (80.91%)**, 37.83% of net saving. Untouched g134 is 47,050, so λ=0 is adverse there and λ=2 improves below g134. After its removal, median ratio is **0.806988** and **16 improve / 7 regress / 0 tie**.
- Largest absolute regression: **sv-21, 34,833→52,246**, **+17,413 (+49.99%)**; untouched g134 is 16,486. This is a material individual weakness, not hidden by aggregate savings.
- Largest relative and sole >2× regression: **sv-20, 492→1,167**, **2.371951× / +675 (+137.20%)**. Untouched g134 is 1,165, so the auxiliary returns almost exactly to the reference workload, while still failing this individual relative comparison. No depth/cap loss accompanies it. The guard permits exactly one; it passes as written, with no reinterpretation.
- Additional descriptive sensitivity: remove both largest-saving positions sv-01 and sv-17, leaving **235,346→165,235 (29.79% lower)**. The global result does not depend on a single outlier or even those two positions. These extra removals do not replace the preregistered gate.

## Outcome and capture-stratum robustness

| Stratum | N / distinct games | g134 qnodes | λ=0 qnodes | λ=2 qnodes | Reduction | Median λ2/λ0 | I / R / T | SP% λ=0→2 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| White wins | 8 / 8 | 136,638 | 192,170 | 64,539 | 66.42% | 0.675337 | 7 / 1 / 0 | 44.30→67.88 |
| Draws | 8 / 8 | 44,221 | 70,498 | 39,885 | 43.42% | 0.849607 | 6 / 2 / 0 | 50.86→62.54 |
| Black wins | 8 / 8 | 115,565 | 198,896 | 103,841 | 47.79% | 0.883182 | 4 / 4 / 0 | 49.01→62.25 |
| Tactical/capture-rich | 12 / 12 | 218,483 | 374,998 | 172,536 | 53.99% | 0.754102 | 9 / 3 / 0 | 44.88→62.92 |
| Quieter-capture | 12 / 12 | 77,941 | 86,566 | 35,729 | 58.73% | 0.756878 | 8 / 4 / 0 | 54.99→68.68 |

White-win transfer is the most consistently favorable (7/8), draws improve 6/8, and both capture strata improve broadly (9/12 and 8/12). **Black-win transfer remains weaker: four improve and four regress**, despite a 47.79% aggregate reduction and median ratio 0.883182. Its largest benefit is sv-17 **107,783→20,424**. Remove only that Black-win saving and the seven remaining Black-win positions still improve **91,113→83,417 (8.45%)**, with three improvements/four regressions. This descriptive sensitivity exposes the weaker breadth of Black-win transfer; the preregistered outlier gate applies to the complete new suite.

Black-win sv-21 supplies the largest absolute regression; sv-20 supplies the sole >2× relative regression. They differ in STM, capture class and material context, so these data do not establish a simple colour/perspective defect. Outcome is the source game’s actual eventual result, not the correctness of a root move. All five prespecified strata have favorable aggregate qnodes, favorable median ratios and higher aggregate SP rates; no complete outcome/capture class reverses the aggregate finding. That supports C, while the uneven Black-win distribution prevents a strong/universal claim.

## Source-group and family analysis

Fine groups each contribute exactly one position: **17 of 24 improve**. Across the 15 preregistered first-move families, **11 improve aggregate qnodes / 4 regress**. All six families with multiple selected positions have lower aggregate qnodes, but their within-family consistency varies. These sparse aggregates are descriptive; there is no IID significance claim.

| First-move family | N | Positions | λ=0 qnodes | λ=2 qnodes | Reduction | Median ratio | I / R / T | SP% λ=0→2 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| a2a3 | 2 | sv-01, sv-20 | 118,927 | 23,773 | 80.01% | 1.281412 | 1 / 1 / 0 | 30.77→56.88 |
| a2a4 | 1 | sv-10 | 812 | 662 | 18.47% | 0.815271 | 1 / 0 / 0 | 45.89→55.51 |
| b1c3 | 3 | sv-04, sv-09, sv-13 | 75,889 | 32,404 | 57.30% | 0.506160 | 3 / 0 / 0 | 45.79→58.70 |
| b2b3 | 1 | sv-19 | 3,975 | 4,379 | -10.16% | 1.101635 | 0 / 1 / 0 | 56.48→66.67 |
| b2b4 | 3 | sv-12, sv-14, sv-18 | 35,327 | 12,648 | 64.20% | 0.336842 | 2 / 1 / 0 | 53.25→67.39 |
| c2c3 | 2 | sv-03, sv-17 | 113,159 | 23,335 | 79.38% | 0.365486 | 2 / 0 / 0 | 45.91→68.72 |
| c2c4 | 1 | sv-21 | 34,833 | 52,246 | -49.99% | 1.499900 | 0 / 1 / 0 | 52.45→56.82 |
| e2e3 | 1 | sv-06 | 7,212 | 5,820 | 19.30% | 0.806988 | 1 / 0 / 0 | 66.60→68.37 |
| e2e4 | 1 | sv-23 | 18,922 | 12,578 | 33.53% | 0.664729 | 1 / 0 / 0 | 48.48→59.71 |
| f2f4 | 1 | sv-24 | 575 | 664 | -15.48% | 1.154783 | 0 / 1 / 0 | 65.80→68.20 |
| g1f3 | 1 | sv-16 | 455 | 512 | -12.53% | 1.125275 | 0 / 1 / 0 | 74.68→79.58 |
| g1h3 | 2 | sv-11, sv-22 | 9,281 | 7,094 | 23.56% | 0.767506 | 2 / 0 / 0 | 62.35→67.76 |
| g2g3 | 1 | sv-15 | 11,283 | 5,682 | 49.64% | 0.503589 | 1 / 0 / 0 | 58.63→71.45 |
| h2h3 | 1 | sv-08 | 4,965 | 3,197 | 35.61% | 0.643907 | 1 / 0 / 0 | 70.28→84.04 |
| h2h4 | 3 | sv-02, sv-05, sv-07 | 25,949 | 23,271 | 10.32% | 0.843476 | 2 / 1 / 0 | 69.06→76.15 |

The a2a3 family is mixed (one large saving, one small absolute >2× regression); its mean-of-two median ratio exceeds one even though its aggregate improves. The two largest-saving families, **a2a3 and c2c3**, contribute **184,978 / 253,299 = 73.03%** of net savings, so magnitude is concentrated. Yet removing both entire families leaves **20 positions, 229,478→161,157 qnodes (29.77% lower)**. Improvement therefore extends beyond one or two source families. First-move families are coarse and share later tactical patterns; 24 distinct opening keys are not 24 statistically independent engine distributions.

## Stand-pat behavior and root stability

Aggregate SP rates: **g134 56.05%; λ=0 47.35%; λ=2 64.21%**. The paired median per-position λ0→λ2 change is **+9.67 percentage points**. Rates rise on **24/24**, fall on **0**, and tie on **0**. The per-position absolute rates appear in the complete search matrix; paired changes and qnode ratios follow.

| ID | λ2/λ0 q ratio | SP change (pp) | Root score change | Best move λ=0→2 |
| --- | --- | --- | --- | --- |
| sv-01 | 0.190873 | +25.57 | +7870 | g4h2→g4f2 |
| sv-02 | 0.706767 | +2.29 | +2194 | d7b5→c8e8 |
| sv-03 | 0.541481 | +5.51 | +3378 | b5b4→h7g7 |
| sv-04 | 0.222737 | +16.87 | +2893 | b8c6→h8h5 |
| sv-05 | 0.843476 | +9.72 | -2181 | g3f4→g3f4 |
| sv-06 | 0.806988 | +1.77 | +5537 | f3e4→f1g1 |
| sv-07 | 1.022424 | +3.84 | +1698 | b6c7→b6c7 |
| sv-08 | 0.643907 | +13.76 | +3922 | f5g6→f5g6 |
| sv-09 | 0.973708 | +3.31 | -2129 | e6c4→e6c4 |
| sv-10 | 0.815271 | +9.62 | -243 | e6f6→e6d6 |
| sv-11 | 0.883942 | +4.72 | +5441 | c2d3→e4d3 |
| sv-12 | 0.191814 | +20.42 | +6607 | d4b4→d4b4 |
| sv-13 | 0.506160 | +11.34 | -3971 | g7f7→g7c7 |
| sv-14 | 1.159959 | +9.50 | +644 | a4a1→a4a1 |
| sv-15 | 0.503589 | +12.83 | +531 | f2f4→f2f4 |
| sv-16 | 1.125275 | +4.90 | +695 | f5g4→f5g4 |
| sv-17 | 0.189492 | +25.31 | +64 | g7g6→g7g6 |
| sv-18 | 0.336842 | +14.80 | +4132 | g2g4→f1e1 |
| sv-19 | 1.101635 | +10.19 | +1129 | c1d1→c1a1 |
| sv-20 | 2.371951 | +24.93 | -1296 | f3g4→f3g4 |
| sv-21 | 1.499900 | +4.37 | -292 | d2d3→d2d3 |
| sv-22 | 0.651070 | +5.21 | -591 | g2h3→g2h3 |
| sv-23 | 0.664729 | +11.23 | +2887 | b8c6→b8c6 |
| sv-24 | 1.154783 | +2.40 | +267 | c7e5→c7e5 |

All 17 qnode improvements coincide with higher SP rates; **none** improves qnodes despite a lower cutoff rate. Conversely, higher cutoff rates accompany qnode regressions on **sv-07, sv-14, sv-16, sv-19, sv-20, sv-21 and sv-24**. In particular, sv-20 gains +24.93 SP points but qnodes grow 2.37×; sv-21 gains +4.37 points but adds 17,413 qnodes. A rate is conditional on the realized tree and eligible opportunities, so better stand-pat percentages are corroboration rather than a sufficient explanation or guaranteed smaller tree. No causal mediation claim follows from these aggregates.

Root best moves change on **10/24**: sv-01, 02, 03, 04, 06, 10, 11, 13, 18, 19. Signed score changes have mean **+1,632.75**, median **+912**, range **−3,971 to +7,870**; 17 rise / 7 fall / 0 tie. Absolute changes: median **2,155**, p90 **5,508.2**, p95 **6,446.5**, maximum **7,870** (sv-01). Quantiles interpolate at `(n−1)*p`. These are original mapped search units, not centipawns. Move changes are not classified as good/bad, and neither score changes nor smaller qtrees establish playing strength.

## Secondary continuity fixtures

Run only after all 72 NEW-suite primary searches. **All 12** model/fixture combinations exactly reproduce the accepted replication’s depth/status, node counters, worker diagnostics, root score and best move; no reproduction discrepancy needed interpretation or a rerun under different settings.

| Position | Model | Depth / cap | Total nodes | Qnodes | Q% | SP attempts | SP cutoffs | SP% | Root score / move | Seconds |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| middlegame-kiwipete | g134 | 4 / none | 218,790 | 209,297 | 95.66% | 180,919 | 80,896 | 44.71% | 10036 / e1g1 | 1.5765 |
| middlegame-kiwipete | lambda0 | 4 / none | 91,786 | 76,376 | 83.21% | 80,712 | 52,061 | 64.50% | 10670 / a2a3 | 0.8793 |
| middlegame-kiwipete | lambda2 | 4 / none | 58,535 | 47,699 | 81.49% | 51,231 | 36,400 | 71.05% | 6669 / e1g1 | 0.5989 |
| quiet-fianchetto | g134 | 4 / none | 18,327 | 13,007 | 70.97% | 16,509 | 8,896 | 53.89% | -18683 / f3e5 | 0.1494 |
| quiet-fianchetto | lambda0 | 4 / none | 11,564 | 4,881 | 42.21% | 9,881 | 7,984 | 80.80% | -12156 / f3e5 | 0.0927 |
| quiet-fianchetto | lambda2 | 4 / none | 24,816 | 18,864 | 76.02% | 22,705 | 10,524 | 46.35% | -12867 / f3e5 | 0.2063 |
| opening-start | g134 | 4 / none | 3,795 | 220 | 5.80% | 2,913 | 2,158 | 74.08% | -872 / b1c3 | 0.0309 |
| opening-start | lambda0 | 4 / none | 7,541 | 1,287 | 17.07% | 6,216 | 4,314 | 69.40% | -13826 / c2c4 | 0.0633 |
| opening-start | lambda2 | 4 / none | 6,600 | 802 | 12.15% | 5,340 | 3,900 | 73.03% | -371 / b1c3 | 0.0658 |
| en-passant | g134 | 4 / none | 358 | 18 | 5.03% | 235 | 151 | 64.26% | 0 / e5d6 | 0.0017 |
| en-passant | lambda0 | 4 / none | 392 | 11 | 2.81% | 258 | 166 | 64.34% | 0 / e5d6 | 0.0018 |
| en-passant | lambda2 | 4 / none | 341 | 19 | 5.57% | 221 | 139 | 62.90% | 0 / e5d6 | 0.0012 |

- **Kiwipete reproduces the benefit:** g134 209,297; λ=0 **76,376**; λ=2 **47,699 qnodes**, **37.55% fewer** versus control, SP **64.50%→71.05%**. Root choices a2a3→e1g1.
- **Fianchetto reproduces the regression:** g134 13,007; λ=0 **4,881**; λ=2 **18,864**, **3.8648× / +286.48%**; SP **80.80%→46.35%**, root f3e5 unchanged. λ=2 remains 45.03% above g134. This unresolved fixture is secondary, not silently added to or removed from primary gates.
- Opening **1,287→802**; en-passant **11→19**, both exactly as previously recorded. Opening root c2c4→b1c3, en-passant remains e5d6/0. Neither enters the fresh-suite statistics.

| Fixture | Exact FEN |
| --- | --- |
| Kiwipete | `r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1` |
| Fianchetto | `r1bq1rk1/ppp1bppp/2n2n2/3pp3/8/1P1P1NP1/PBP1PPBP/RN1Q1RK1 w - - 0 8` |
| Opening | `rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1` |
| En-passant | `4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1` |

## Classification, next work unit and unresolved uncertainty

**C — SEARCH VALIDATION POSITIVE** is the single primary classification. All unchanged gates pass with substantial median/aggregate/outlier margins. Benefit spans both capture classes and all outcome aggregates, 17 distinct source games and 11 first-move families, and survives removal of the two largest-saving families. This rejects the narrow explanation that the earlier fixed-checkpoint improvement existed only on its original fixtures or one/two source groups.

The result is not D: the position improvement count is 17 rather than near-unanimous; the severe guard uses its one allowed exception; Black wins are only 4/8 favorable; Fianchetto remains a severe continuity regression. These measured weaknesses constrain the recommendation. They do not overturn the positive new-suite gate outcome or establish an adverse entire source/outcome stratum. No extra secret subgroup threshold is used.

**Experimental production-trainer integration is justified as the NEXT separately authorized work unit only.** Integrate the exact accepted capture-consistency formulation behind a clearly experimental/default-off configuration boundary. Preserve λ=0 current behavior exactly and enable a controlled, paired multi-generation λ=0 versus λ=2 campaign. Carry the recorded suite, Black-win split, sv-20/sv-21 and Fianchetto into acceptance planning, without tuning this completed experiment. Playing-strength/retention evaluation belongs to that future campaign’s acceptance before any default decision. No implementation, campaign, promotion, tournament or strength test is authorized by this report or performed here.

The larger search-only dataset resolves the previous median/outlier uncertainty positively for these fixed bytes. Remaining uncertainties: one starting checkpoint and one pair of trained arms; one new source seed/generator, deliberately balanced outcome/capture selection, random-opening-conditioned distribution, modest 24-game sample, shared chess motifs, uneven Black-win transfer, Fianchetto and sv-21 regressions, sizable root-score/move changes, no multi-generation stability, no unsaved-history exclusion proof, and no playing-strength evidence. Aggregate stand-pat improvement cannot identify whether one-ply loss, regularization, target mismatch or deeper tactical context explains an exception. This unit does not diagnose those causes from source outcomes alone.

## Validation performed, skipped work and Git boundary

Passed focused validation:

1. All six report bytes match accepted commits; canonical search contract and current Git state inspected. Exact checkpoint SHA-256 before/after, normal codec load/round-trip and per-process in-memory serialization equality; retained optimizer and original store bytes unchanged.
2. Root/derived/indexed seeds verified, no historical/prior-generation seed collision; all 96 native opening previews and all full legal game/outcome replays verified; two full source-game regeneration equalities. Outcome and terminal labels remain native.
3. Exact position-key/FEN novelty to retained historical/prior corpora; zero per-set intersections, source/group/key uniqueness and native FEN round trips. Deterministic selection replay is byte-identical; timestamps and frozen hashes prove suite freeze preceded all comparative searches.
4. **72 primary trace-free searches + 12 continuity searches + 18 bounded trace-free replay equalities + 84 secondary trace/no-trace equalities = 186 benchmark requests**, all depth 4 with no caps/failures. Bounded replay covers the first T/Q position in each outcome for all three models. Twelve continuity equalities also match the prior replication. All node accounting/root restoration assertions pass; trace eligible attempts/cutoffs agree with normal diagnostic cutoffs.
5. Identical fixed settings and pristine search classes across arms; independent arithmetic recomputation from raw trace-free outputs confirms aggregates/median and unchanged gates. All 84 normal requests precede secondary tracing. No node cap was raised and no failed position replaced.
6. Executed harness entrypoints were inspected: only source generation, board/rule replay, inference codec/search and analysis are invoked; no trainer, training-state decoder, target builder or update call. No network initialization/tuning/training test was used to create a replacement. Static call-path inspection is distinguished from runtime model-byte immutability checks.
7. Every original archived file remains byte-identical to the clean HEAD archive; authoritative core/rules/search/selfplay have no diff from HEAD. **All 544 initial authoritative file hashes and exact pre-report porcelain status remain unchanged**, index empty. All recorded old-corpus input hashes, frozen new inputs and original/copy checkpoint hashes remain unchanged. Final report-only diff/staging/commit and preservation checks are recorded in the completion response.

Deliberately skipped: training and all optimizer-update/gradient/training tests; calibration; full regression/JUnit suites; GUI/browser checks (headless, no UI change); promotion/validation matches, tournaments, Elo and self-play strength comparisons; alternative λ/checkpoints, extra generation pool, search tuning, higher caps/depth, NNUE evaluation, multi-generation campaigns, full live-store recovery/operational acceptance, release/package/deployment checks. Focused actual searches, deterministic replays and byte/provenance assertions supplied the required validation. No browser interaction was required.

Minor external tooling corrections occurred before their dependent evidence: unsupported PowerShell UTC syntax was replaced by `[DateTime]::UtcNow`; report reads used Python UTF-8; the novelty reader was corrected to recognize the older frozen manifest’s `files` field as well as the later `sha256` field. These changed no generation/selection rule, source/checkpoint bytes, gate, or completed benchmark.

The sole intended authoritative change is this report. No checkpoint, generated pool, separate benchmark corpus or temporary tooling is committed. A report-only commit is allowed if final attribution checks remain clean; its SHA is supplied in the completion response because a report cannot contain its own commit hash. No push or deployment. A completed diagnostic/report is not user acceptance, production acceptance or operational integration.

**Human actions required after this prompt: None.** The recommended integration is a future authorization decision, not a blocking action required to complete this diagnostic.

## External evidence and reproduction

External evidence root is the isolation directory above. Use Java `21+35-2513`, Python 3.13.4, a pristine archive of inspected HEAD and the three exact inference bytes. Compile the appended Java harnesses with `javac -encoding UTF-8 -d CLASSES -sourcepath ARCHIVE/app/src/main/java`. `SearchSources DIR` writes the bounded source plan/games/positions; the novelty/selection procedure freezes the suite. To replay measurements directly, reconstruct `search-suite.tsv` as UTF-8 ID, tab, exact FEN, CRLF from the frozen table, verify its hash, then run `FixedSearch DIR MODEL MODE`, MODEL in g134/lambda0/lambda2. Run all models with MODE `primary`, then all with `continuity`, then `replay`, then `trace`. Use `java -Xmx1024m`; source generation uses `-Xmx1536m`. Every result file uses CREATE_NEW. Compare all non-timing worker counters, scores and moves before joining secondary attempts into primary rows. Analysis is simple per-position ratios and summed counters; no weighting or confidence interval is hidden.

Retained files are disposable research evidence and may not survive OS temporary cleanup. The exact suite/FENs/source identities and complete measured tables persist here; no future work may substitute different checkpoint bytes if originals disappear. All source generation/selection/search code needed to repeat the bounded procedure is included below. The retained `novelty.py`, `analyze.py` and `verify.py` hashes identify the executed audit/analysis code; their algorithm and validation contracts are documented above.

| External artifact | Bytes | SHA-256 |
| --- | --- | --- |
| preregistration.json | 2704 | `c601626fb3f4c0ad53efd06c1ea00644031ed62ea5bea391a9473abeca6037b8` |
| checkpoint-pins.json | 654 | `28086a4b5f2648653c9f7933b3b8ef08b052e5ca481e3807feeb2c6190c1af63` |
| source-plan.jsonl | 20040 | `1ada313758cc54318b3bd429d20e8c4d847643c77f201e993c9c10a203618389` |
| games.jsonl | 17957 | `ebb3a202e488487a917102f32de08bf20ee7d84847daab79dd368d8da6a140f2` |
| positions.jsonl | 4426338 | `d36fb54fa36b603a6dc868c4d0fa98c08bfa374e983d141e7f1eb7b56e7475a7` |
| novelty-audit.json | 977 | `6397d703c90444726a8649ae75c8304da2380d47db211be3574b5cf8aa46e6e0` |
| novelty-input-hashes.json | 49941 | `d33e127db060112d682238e169f374de3d72bc05eb48917564b87b875aeed1ee` |
| excluded-keys.txt | 6927466 | `fbbe6f8fd5a08b96d9789c4bb8654c9a27ebb9069847fe2096331ceca2a2039c` |
| banned-fens.txt | 201241 | `a4166ecc6544608cbd1b7e1258ebbe5d79aabda4b2d99088d813a8c3e746311c` |
| frozen-manifest.json | 2237 | `044712c4e5b3ffcf462d6af2cdd4489800a432ab44a82d41e270efc50b944b7c` |
| search-suite.jsonl | 16688 | `c7aa27bb205d2a66d0d8f6be265bbc1d3fabe6017526e1f0f1cf07cc8380f7ee` |
| search-suite.tsv | 1546 | `9aaad2d4cb46f4409ec3dfbe9d2af3732a5254a1ae18ea98482171197cf494b1` |
| source-checks.txt | 261 | `69b98ec07c01013c7fe2f0d6628bc07907124ed1845a6d5c24fc379aa8e1055a` |
| analysis.json | 151450 | `82645eef4b968387d14657c1d121a165a9a4490d4aef4e557d2edd719f8e68c2` |
| search-verification.json | 260 | `8e6b20fe4278c77ecb9b2c5a547491d4bd57ec8bf0c795849c722465eaefd8c5` |
| verification.json | 2557 | `937d7f4b113db0d6a0b1d17db6fae12ef8bad1b2cfa8636da8f14ddaebb99237` |
| novelty.py | 3922 | `6f09269c6b277d7f2af435b81950c5e688db22744441a6988b7039a15a42e9a1` |
| analyze.py | 4533 | `017d1d12812ac9f293b3b9877286eb9eeb42bd95bc876ae0785fe34c2a2db528` |
| verify.py | 5067 | `f323ad352e78169121aa46448cba351e9a69b96b032bb2fd827df6119500896e` |
| run_search.py | 2238 | `4d1c75b7aa1a38633a75425304f1c9913dbe0a6db0b6c9aae84708c86980707e` |
| g134-primary.jsonl | 30764 | `8446df6be943f6876f5405f77f4983ad8a15f21bfe8085d4b375f37d8c26d7f4` |
| lambda0-primary.jsonl | 30864 | `40de96f1d9fe383a6f924f96bfa256df8d298edf2d4e36c93960f3d24cb5cfa5` |
| lambda2-primary.jsonl | 30771 | `50444a8e49d7cd4882870d4c73610fb265c4754ae7a047a8f6054ba030c1a269` |
| g134-continuity.jsonl | 5541 | `d9fdf9c8d7498fd1bb9c9e305ccd764f99ae571183c7e767d6781c68479a1ab1` |
| lambda0-continuity.jsonl | 5563 | `a2f002cce9f93b7c57db028f8855e01584d22f5b5d86e880b4d7f388b411c3e3` |
| lambda2-continuity.jsonl | 5560 | `7e256f878871a1d51f80f37193256df65fb04d120496d0a0f18836a92f0967b1` |
| g134-replay.jsonl | 8077 | `d81837bc8096856c7a307ed20382410e09c3131bf53e987cfbef8c895f61e42f` |
| lambda0-replay.jsonl | 8115 | `2f62ee30cf5e9537f2e676a9290c5e466a0a005cbba0e03f8b84b217e0075d49` |
| lambda2-replay.jsonl | 8079 | `ad19ad08d6317e718c774f957304724c0c34256862b1485f53a13ba0d639f613` |
| g134-trace.jsonl | 35796 | `ea89935adb81ba2e5a7096b2a69865f6f2e264df3ce7c8779417ab75f98c0db3` |
| lambda0-trace.jsonl | 35908 | `885e697a0a205e981beb32bdf743f2bfdc68011fb94e293fdf429f19eebd071e` |
| lambda2-trace.jsonl | 35811 | `9960509cd018987d2e41491b84732565d4869b33f416e46a973f59530da98c5c` |

## Appendix — exact source-generation, selection and search harnesses

### SourceUtil.java

```java
package com.ohinteractive.seedv6.tools.search;
import java.util.*;import com.ohinteractive.seedv6.core.*;import com.ohinteractive.seedv6.core.move.Move;import com.ohinteractive.seedv6.search.order.MoveOrdering;
public class SourceUtil {
 static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
 static String fen(long[] b){StringBuilder s=new StringBuilder();String pieces=" KQRBNP  kqrbnp";for(int rank=7;rank>=0;rank--){int empty=0;for(int file=0;file<8;file++){int square=rank*8+file,p=0;for(int j=0;j<4;j++)p|=((b[j]>>>square)&1)<<j;if((p&7)==0){empty++;continue;}if(empty>0){s.append(empty);empty=0;}s.append(pieces.charAt(p));}if(empty>0)s.append(empty);if(rank>0)s.append('/');}int st=(int)b[4];s.append(Board.player(st)==0?" w ":" b ");int cast=(st>>>1)&15;if(cast==0)s.append('-');else for(int j=0;j<4;j++)if((cast&(1<<j))!=0)s.append("KQkq".charAt(j));int ep=Board.enPassantSquare(st);s.append(' ').append(ep<0?"-":"abcdefgh".charAt(ep%8)+Integer.toString(ep/8+1));s.append(' ').append(Board.halfMoveClock(st)).append(' ').append(Board.fullMoveNumber(st));String f=s.toString();require(Arrays.equals(b,Board.fromFen(f)),"FEN roundtrip");return f;}
 static long[] legal(long[] b){long[] m=new long[256],scratch=new long[256]; int n=Gen.genAll(b[0],b[1],b[2],b[3],(int)b[4],b[5],true,m,scratch);return Arrays.stream(Arrays.copyOf(m,n)).boxed().sorted(Comparator.comparing(Move::coordinate)).mapToLong(Long::longValue).toArray();}
 static boolean capture(long[] b,long m){return ((m>>>Board.TARGET_PIECE_SHIFT)&Board.PIECE_BITS)!=0 || ((m>>>Board.PROMOTE_PIECE_SHIFT)&Board.PIECE_BITS)==0&&MoveOrdering.isTactical(b,m);}
 static long[] play(long[] b,long m){long[] c=new long[Board.MAX_BITBOARDS];Board.makeMoveInto(b[0],b[1],b[2],b[3],(int)b[4],b[5],m,c);return c;}
}
```

### SearchSources.java

```java
package com.ohinteractive.seedv6.tools.search;
import java.io.*;import java.nio.file.*;import java.util.*;
import com.ohinteractive.seedv6.core.*;import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.training.selfplay.*;import com.ohinteractive.seedv6.training.validation.*;
import com.ohinteractive.seedv6.rules.GameHistory;import com.ohinteractive.seedv6.search.evaluation.*;
import static com.ohinteractive.seedv6.tools.search.DiagnosticReport.*;
public class SearchSources {
 static final long ROOT=2026092403L,GEN=new SplittableRandom(ROOT^0x6A09E667F3BCC909L).nextLong();
 public static void main(String[] a)throws Exception{
  Path d=Path.of(a[0]);var config=new SelfPlayConfig(96,4,6,GEN,0,8,32,1024,NnueScoreMapping.V1,-1,-1);
  var oc=new ValidationConfig(96,GEN,0,8,4,6,NnueScoreMapping.V1,1024);long[] start=Board.startingPosition();
  var ids=new ArrayList<Integer>();var roots=new ArrayList<ValidationArena.Opening>();var keys=new HashSet<Long>();
  for(int c=0;c<4096&&ids.size()<96;c++){var o=ValidationArena.opening(start,GameHistory.initial(start),oc,c);if(o.randomizedPlies()>=4&&new HeadlessGame(o.board(),1024).active()&&keys.add(o.board()[5])){ids.add(c);roots.add(o);}}
  SourceUtil.require(ids.size()==96,"Opening shortage");
  try(var o=new PrintWriter(Files.newBufferedWriter(d.resolve("source-plan.jsonl"),StandardOpenOption.CREATE_NEW))){
   write(o,"type","plan","utc",java.time.Instant.now().toString(),"rootSeed",ROOT,"generationSeed",GEN,"config",config.toString());
   for(int i=0;i<96;i++){var r=roots.get(i);write(o,"type","source","game",i,"candidateIndex",ids.get(i),"seed",SelfPlayRunner.gameSeed(GEN,ids.get(i)),"openingPlies",r.randomizedPlies(),"openingKey",Long.toUnsignedString(r.board()[5],16),"openingFen",SourceUtil.fen(r.board()));}
  }
  try(var out=new PrintWriter(Files.newBufferedWriter(d.resolve("positions.jsonl"),StandardOpenOption.CREATE_NEW));var progress=new PrintWriter(Files.newBufferedWriter(d.resolve("games.jsonl"),StandardOpenOption.CREATE_NEW))){
   for(int i=0;i<96;i++){
    var game=SelfPlayRunner.play(SearchEvaluation.handcrafted(),config,ids.get(i),start,new SelfPlayControl());
    SourceUtil.require(game.termination().completed(),"Incomplete game "+i+" "+game.termination());
    SourceUtil.require(Arrays.equals(game.positions().get(roots.get(i).randomizedPlies()).board(),roots.get(i).board()),"Opening preview");
    var replay=new HeadlessGame(start,1024);var family=Move.coordinate(game.positions().getFirst().playedMove());
    for(var p:game.positions()){
     long[] b=p.board();SourceUtil.require(Arrays.equals(replay.boardSnapshot(),b),"Trajectory replay");replay.play(p.playedMove());
     var legal=SourceUtil.legal(b);int caps=0;boolean immediateEnd=false;
     for(long m:legal){if(SourceUtil.capture(b,m))caps++;if(!new HeadlessGame(SourceUtil.play(b,m),1024).active())immediateEnd=true;}
     boolean eligible=p.ply()>=12&&p.ply()<=game.playedPlies()-8&&legal.length>=2&&caps>0&&!immediateEnd&&new HeadlessGame(b,1024).active();
     write(out,"game",i,"candidateIndex",ids.get(i),"ply",p.ply(),"key",Long.toUnsignedString(b[5],16),"fen",SourceUtil.fen(b),"board",Arrays.stream(b).boxed().toList(),"move",Move.coordinate(p.playedMove()),"moveEncoded",p.playedMove(),"captures",caps,"legalMoves",legal.length,"immediateEnd",immediateEnd,"eligible",eligible,"outcome",game.result().orElseThrow().toString(),"family",family,"openingKey",Long.toUnsignedString(roots.get(i).board()[5],16));
    }
    SourceUtil.require(replay.termination()==game.termination()&&Arrays.equals(replay.boardSnapshot(),game.finalBoard()),"Outcome replay");out.flush();
    write(progress,"game",i,"candidateIndex",ids.get(i),"seed",SelfPlayRunner.gameSeed(GEN,ids.get(i)),"outcome",game.result().orElseThrow().toString(),"termination",game.termination().toString(),"plies",game.playedPlies(),"family",family,"utc",java.time.Instant.now().toString());progress.flush();
    System.out.println("game="+i+" "+game.termination()+" plies="+game.playedPlies());
   }
  }
 }
}
```

### select_suite.py

```python
from pathlib import Path
import json,hashlib,collections,datetime,subprocess,sys
D=Path(__file__).parent
def load(n):return [json.loads(l) for l in (D/n).read_text().splitlines()]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def H(s):return hashlib.sha256(s.encode()).hexdigest()
def save(n,v):(D/n).write_text(json.dumps(v,indent=2)+'\n')
if not (D/'named-fen-keys.txt').exists():
 subprocess.run(['javac','-cp',str(D/'classes'),'-d',str(D/'classes'),str(D/'FenKeys.java')],check=True)
 subprocess.run(['java','-cp',str(D/'classes'),'com.ohinteractive.seedv6.tools.search.FenKeys',str(D/'banned-fens.txt'),str(D/'named-fen-keys.txt')],check=True)
old={int(k,16) for f in ['excluded-keys.txt','named-fen-keys.txt'] for k in (D/f).read_text().split()}
rows=load('positions.jsonl');games=load('games.jsonl');sources=load('source-plan.jsonl')
assert len(games)==96 and len({g['seed'] for g in games})==96 and len({s['openingKey'] for s in sources[1:]})==96
assert sources[0]['rootSeed']==2026092403
candidates=[]
for r in rows:
 if r['eligible'] and int(r['key'],16) not in old:
  r=dict(r);r['stratum']='T' if r['captures']>=3 else 'Q';r['selectionHash']=H(f"2026092403|position|{r['game']}|{r['ply']}|{r['key']}");candidates.append(r)
order=sorted(range(96),key=lambda g:H(f'2026092403|game|{g}'))
by={(g,t):sorted([r for r in candidates if r['game']==g and r['stratum']==t],key=lambda r:r['selectionHash']) for g in order for t in ['T','Q']}
selected=[];used=set();keys=set();availability={}
for outcome in ['WHITE_WIN','DRAW','BLACK_WIN']:
 slots=['T','Q']*4; matching={}
 def augment(slot,seen):
  for g in order:
   if g in used or g in seen or games[g]['outcome']!=outcome or not any(r['key'] not in keys for r in by[g,slots[slot]]):continue
   seen.add(g)
   if g not in matching or augment(matching[g],seen):matching[g]=slot;return True
  return False
 successes=[augment(s,set()) for s in range(8)]
 availability[outcome]={t:sum(bool(by[g,t]) for g in order if games[g]['outcome']==outcome) for t in ['T','Q']}
 for g,slot in sorted(matching.items(),key=lambda item:item[1]):
  r=next(r for r in by[g,slots[slot]] if r['key'] not in keys);selected.append(r);used.add(g);keys.add(r['key'])
if len(selected)<24:
 while len(selected)<24:
  oc=collections.Counter(r['outcome'] for r in selected);tc=collections.Counter(r['stratum'] for r in selected)
  available=[r for r in candidates if r['game'] not in used and r['key'] not in keys]
  if not available:break
  def rank(r):
   o=oc.copy();o[r['outcome']]+=1;t=tc.copy();t[r['stratum']]+=1
   return sum(abs(o[k]-8) for k in ['WHITE_WIN','DRAW','BLACK_WIN']),sum(abs(t[k]-12) for k in ['T','Q']),H(f"2026092403|game|{r['game']}"),r['selectionHash']
  r=min(available,key=rank);selected.append(r);used.add(r['game']);keys.add(r['key'])
assert len(selected)==24 and len(used)==24 and len(keys)==24
for i,r in enumerate(selected):r['id']=f'sv-{i+1:02d}';r['group']=r['game'];r['sourceSeed']=games[r['game']]['seed'];r['openingFen']=sources[r['game']+1]['openingFen']
assert not {int(r['key'],16) for r in selected}&old
content=''.join(json.dumps(r,sort_keys=True)+'\n' for r in selected)
if len(sys.argv)>1:
 assert (D/'search-suite.jsonl').read_text()==content
 print('DETERMINISTIC SELECTION REPLAY EXACT',sha(D/'search-suite.jsonl'));sys.exit()
with (D/'search-suite.jsonl').open('x') as f:f.write(content)
with (D/'search-suite.tsv').open('x') as f:f.write(''.join(r['id']+'\t'+r['fen']+'\n' for r in selected))
summary=dict(utc=datetime.datetime.now(datetime.timezone.utc).isoformat(),poolGames=96,poolOutcomes=dict(collections.Counter(g['outcome'] for g in games)),poolPositions=len(rows),eligibleBeforeNovelty=sum(r['eligible'] for r in rows),eligibleNovel=len(candidates),availability=availability,
 suiteGames=len(used),suiteGroups=len({r['openingKey'] for r in selected}),suiteOutcomes=dict(collections.Counter(r['outcome'] for r in selected)),suiteStrata=dict(collections.Counter(r['stratum'] for r in selected)),families=dict(collections.Counter(r['family'] for r in selected)),rootSeed=2026092403,generationSeed=sources[0]['generationSeed'],noveltyUnion=len(old),noveltyOverlaps=0,
 artifacts={f:sha(D/f) for f in ['preregistration.json','source-plan.jsonl','games.jsonl','positions.jsonl','excluded-keys.txt','banned-fens.txt','named-fen-keys.txt','search-suite.jsonl','search-suite.tsv','SearchSources.java','SourceUtil.java','select_suite.py','novelty.py']})
save('frozen-manifest.json',summary);print(json.dumps(summary,indent=2))
```

### FixedSearch.java

```java
package com.ohinteractive.seedv6.tools.search;
import java.io.*;import java.nio.file.*;import java.security.*;import java.util.*;
import com.ohinteractive.seedv6.core.*;import com.ohinteractive.seedv6.core.brn2.*;import com.ohinteractive.seedv6.core.move.Move;
import com.ohinteractive.seedv6.rules.GameHistory;import com.ohinteractive.seedv6.search.alphabeta.AlphaBetaPvsSearch;
import com.ohinteractive.seedv6.search.common.*;import com.ohinteractive.seedv6.search.diagnostics.*;
import com.ohinteractive.seedv6.search.evaluation.*;import com.ohinteractive.seedv6.search.iterative.*;
import static com.ohinteractive.seedv6.tools.search.DiagnosticReport.*;
public class FixedSearch {
 static String sha(byte[] b)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
 static final String[][] CONTINUITY={
 {"middlegame-kiwipete","r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1"},
 {"quiet-fianchetto","r1bq1rk1/ppp1bppp/2n2n2/3pp3/8/1P1P1NP1/PBP1PPBP/RN1Q1RK1 w - - 0 8"},
 {"opening-start","rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"},
 {"en-passant","4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1"}};
 public static void main(String[] a)throws Exception{
  Path d=Path.of(a[0]);String name=a[1],mode=a[2];byte[] bytes=Files.readAllBytes(d.resolve(name+".brn2"));var model=Brn2Codec.decodeModel(bytes);var eval=SearchEvaluation.brn2(model);
  SourceUtil.require(Arrays.equals(bytes,Brn2Codec.encodeModel(model)),"Codec identity");
  List<String[]> suite=new ArrayList<>();for(var l:Files.readAllLines(d.resolve("search-suite.tsv")))suite.add(l.split("\t"));
  boolean traced=mode.equals("trace");if(mode.equals("continuity"))suite=new ArrayList<>(Arrays.asList(CONTINUITY));
  else if(traced)suite.addAll(Arrays.asList(CONTINUITY));
  else if(mode.equals("replay"))suite=new ArrayList<>(List.of(suite.get(0),suite.get(1),suite.get(8),suite.get(9),suite.get(16),suite.get(17)));
  else SourceUtil.require(mode.equals("primary"),"Unknown mode");
  try(var o=new PrintWriter(Files.newBufferedWriter(d.resolve(name+"-"+mode+".jsonl"),StandardOpenOption.CREATE_NEW))){
   write(o,"type","run","model",name,"mode",mode,"utc",java.time.Instant.now().toString(),"checkpointHash",sha(bytes),"suiteHash",sha(Files.readAllBytes(d.resolve("search-suite.tsv"))),"depth",4,"threads",1,"nodeCap",1000000,"timeCapNs",60000000000L,"ttEntries",262144);
   for(var p:suite){
    long[] b=Board.fromFen(p[1]),before=b.clone();var trace=traced?new QsearchDecisionTrace(eval,Integer.MAX_VALUE,1,0):null;
    var worker=traced?new AlphaBetaPvsSearch(eval,1<<18,trace):new AlphaBetaPvsSearch(eval,1<<18);
    try(var engine=new IterativeDeepeningSearch(worker)){
     long start=System.nanoTime();var control=SearchControl.controlled(1000000,start,60000000000L,TimeSource.SYSTEM);
     var result=engine.search(new SearchRequest(b,GameHistory.initial(b),4,SearchObserver.NONE,control,true));long elapsed=System.nanoTime()-start;
     var diag=result.diagnostics();var n=diag.worker().nodes();var q=diag.worker().qsearch();var r=result.lastCompletedResult();
     SourceUtil.require(Arrays.equals(before,b)&&control.nodes()==diag.totalEnteredNodes(),"Accounting/restoration");
     Long attempts=null;
     if(traced){var summary=trace.summary();var metrics=(Map<?,?>)summary.get("all");attempts=((Number)metrics.get("standPatAllowed")).longValue();SourceUtil.require(((Number)summary.get("observedCountedQChildren")).longValue()==n.qNodes(),"Trace qnodes");SourceUtil.require(((Number)metrics.get("driverStandPatBetaCutoffs")).longValue()==q.standPatCutoffs(),"Trace cutoffs");}
     write(o,"type","search","model",name,"position",p[0],"trace",traced,"completedDepth",r==null?0:r.depth(),"status",result.targetDepthCompleted()?"COMPLETED":control.termination().toString(),"nodes",control.nodes(),"main",n.mainNodes(),"q",n.qNodes(),"maxQply",n.maximumQply(),"evaluations",n.evaluationCalls(),"spAttempts",attempts,"spCutoffs",q.standPatCutoffs(),"score",r==null?null:r.score(),"move",r==null?null:Move.coordinate(r.bestMove()),"elapsedNs",elapsed,"workerDiagnostics",diag.worker().toString());o.flush();
     System.out.println(name+" "+mode+" "+p[0]+" depth="+(r==null?0:r.depth())+" q="+n.qNodes());
    }
   }
   SourceUtil.require(Arrays.equals(bytes,Brn2Codec.encodeModel(model)),"In-memory model changed");
   SourceUtil.require(Arrays.equals(bytes,Files.readAllBytes(d.resolve(name+".brn2"))),"Disk model changed");
   write(o,"type","end","utc",java.time.Instant.now().toString(),"checkpointHashAfter",sha(Brn2Codec.encodeModel(model)),"immutable",true);
  }
 }
}
```

### SourceChecks.java

```java
package com.ohinteractive.seedv6.tools.search;
import java.nio.file.*;import java.util.*;
import com.ohinteractive.seedv6.core.*;import com.ohinteractive.seedv6.search.evaluation.*;import com.ohinteractive.seedv6.training.selfplay.*;
public class SourceChecks {
 public static void main(String[] a)throws Exception{
  Path d=Path.of(a[0]);SourceUtil.require(SearchSources.ROOT==2026092403L&&SearchSources.GEN==-5161967410789381145L,"Fixed seed");
  var old=new HashSet<Long>();for(var s:Files.readAllLines(d.resolve("historical-generation-seeds.txt")))for(int i=0;i<64;i++)old.add(SelfPlayRunner.gameSeed(Long.parseLong(s),i));
  for(int i=0;i<64;i++)old.add(SelfPlayRunner.gameSeed(-8204741532265061904L,i));
  for(var s:Files.readAllLines(d.resolve("replication-seeds.txt")))old.add(Long.parseLong(s));
  var seen=new HashSet<Long>();int count=0;
  for(var l:Files.readAllLines(d.resolve("source-check.tsv"))){var s=l.split("\t");int game=Integer.parseInt(s[0]),index=Integer.parseInt(s[1]);long seed=Long.parseLong(s[2]);SourceUtil.require(seed==SelfPlayRunner.gameSeed(SearchSources.GEN,index)&&!old.contains(seed)&&seen.add(seed),"seed identity/overlap");count++;
   if(game==0||game==95){
    var config=new SelfPlayConfig(96,4,6,SearchSources.GEN,0,8,32,1024,NnueScoreMapping.V1,-1,-1);
    var trajectory=SelfPlayRunner.play(SearchEvaluation.handcrafted(),config,index,Board.startingPosition(),new SelfPlayControl());
    String actual=String.join(",",trajectory.positions().stream().map(p->Long.toUnsignedString(p.board()[5],16)).toList());
    SourceUtil.require(actual.equals(s[4])&&trajectory.termination().toString().equals(s[3]),"Bounded generation deterministic replay");
    System.out.println("PASS exact full-trajectory regeneration game="+game+" plies="+trajectory.playedPlies());
   }
  }
  SourceUtil.require(count==96,"Source count");System.out.println("PASS 96 indexed seeds; no overlap with 134 saved generations, first fresh generation, replication; root=2026092403 derived=-5161967410789381145");
 }
}
```

### FenKeys.java

```java
package com.ohinteractive.seedv6.tools.search;
import java.nio.file.*;import java.io.*;import com.ohinteractive.seedv6.core.Board;
public class FenKeys {public static void main(String[] a)throws Exception{try(var o=new PrintWriter(Files.newBufferedWriter(Path.of(a[1]),StandardOpenOption.CREATE_NEW))){for(var f:Files.readAllLines(Path.of(a[0])))o.println(Long.toUnsignedString(Board.fromFen(f)[5],16));}}}
```

### novelty.py

```python
from pathlib import Path
import json,hashlib,re,sys,datetime,shutil
D=Path(__file__).parent;P=D.parent/'seedv6-brn-replication-20260924';sys.path.insert(0,str(P))
from inspect_store import frame,plan,STORE
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def save(n,v): (D/n).write_text(json.dumps(v,indent=2)+'\n')
sets={};hashes={};train=set();history=set();g133=set();hseeds=[]
for p in sorted((STORE/'bootstrap').glob('*.data')):
 k,r,h=frame(p);assert k=='brn-bootstrap-data-v1';ph=r.utf();r.fmt('q');pl=plan(p.with_suffix('.plan'));assert ph==pl['sha256']
 hseeds.append(int(re.search(r'SelfPlayConfig\[.*?seed=(-?\d+)',pl['settings']).group(1)))
 for split in ['train','held']:
  for _ in range(r.fmt('i')):
   b=[r.fmt('q') for _ in range(6)];w=r.fmt('d');assert w in (-1,0,1);key=b[5]&((1<<64)-1);history.add(key)
   if split=='train':train.add(key)
   if pl['generation']==134:g133.add(key)
 hashes[str(p)]=h
assert len(hashes)==134
assert history=={int(s,16) for s in (P/'history-all-keys.txt').read_text().split()}
assert train=={int(s,16) for s in (P/'history-train-keys.txt').read_text().split()}
sets.update(historicalTrain=train,historicalAll=history,g133Replay=g133)
roots=['diagnostic','calibration','capture-consistency','campaign-replay','fresh-generation','replication'];fens=set();scanned={}
pattern=r'[rnbqkpRNBQKP1-8]+(?:/[rnbqkpRNBQKP1-8]+){7} [wb] [KQkq-]+ (?:[a-h][36]|-) \d+ \d+'
def collect(v,keys):
 if isinstance(v,dict):
  for k,x in v.items():
   if k in ('key','childKey','parentKey','openingKey') and isinstance(x,str) and re.fullmatch('[0-9a-fA-F]{1,16}',x): keys.add(int(x,16))
   if k=='keys' and isinstance(x,list):
    keys.update(int(a,16) for a in x if isinstance(a,str) and re.fullmatch('[0-9a-fA-F]{1,16}',a))
   if k.lower().endswith('board') and isinstance(x,list) and len(x)==6 and all(isinstance(a,int) for a in x):keys.add(x[5]&((1<<64)-1))
   if isinstance(x,str):fens.update(re.findall(pattern,x))
   collect(x,keys)
 elif isinstance(v,list):
  for x in v:collect(x,keys)
for name in roots:
 p=D.parent/('seedv6-brn-'+name+'-20260924');assert p.is_dir();keys=set();count=0
 for f in sorted(p.glob('*.jsonl')):
  for l in f.read_text(encoding='utf-8-sig').splitlines():
   if l.strip():collect(json.loads(l),keys)
  hashes[str(f)]=sha(f);count+=1
 for f in p.glob('*keys.txt'):
  values=f.read_text().split()
  if all(re.fullmatch('[0-9a-fA-F]{1,16}',v) for v in values):keys.update(int(v,16) for v in values);hashes[str(f)]=sha(f)
 sets[name]=keys;scanned[name]=dict(jsonlFiles=count,keys=len(keys))
for f in list((D/'repo').glob('BRN*.md'))+list((D/'repo/app/src/main/java/com/ohinteractive/seedv6/tools').rglob('*.java')):
 fens.update(re.findall(pattern,f.read_text(encoding='utf-8')))
for name,expected in [('fresh-generation','5f805f70d4e0d701d607e8d9d50c52ca644ac4e7c0bfce79f4e112726d5e38d9'),('replication','fcfe1a52b4f90bf222357c1a7426b025d274587c232c986201f87c250a0ebd1d')]:
 p=D.parent/('seedv6-brn-'+name+'-20260924');assert sha(p/'fresh-rows.bin')==expected
 manifest=json.loads((p/'frozen-manifest.json').read_text()); entries=manifest.get('sha256',manifest.get('files'))
 for f,h in entries.items():assert sha(p/f)==h,(p,f)
allkeys=set().union(*sets.values())
(D/'excluded-keys.txt').write_text('\n'.join(format(k,'x') for k in sorted(allkeys))+'\n')
(D/'banned-fens.txt').write_text('\n'.join(sorted(fens))+'\n')
(D/'historical-generation-seeds.txt').write_text('\n'.join(map(str,hseeds))+'\n')
save('exclusion-sets.json',{k:sorted(format(x,'x') for x in v) for k,v in sets.items()})
save('novelty-input-hashes.json',hashes)
save('novelty-audit.json',dict(utc=datetime.datetime.now(datetime.timezone.utc).isoformat(),historicalFrames=134,sets={k:len(v) for k,v in sets.items()},scanned=scanned,namedFens=len(fens),unionKeys=len(allkeys),allPriorDirsAvailable=True,priorFrozenManifestsVerified=True))
print((D/'novelty-audit.json').read_text())
```
