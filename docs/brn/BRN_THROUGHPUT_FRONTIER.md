# BRN-3 throughput frontier

Authority: [contract](BRN_THROUGHPUT_CONTRACT.md). Evidence and reproduction:
[research record](../research/brn/BRN_THROUGHPUT_RESEARCH.md), especially T04.
Status: practical bounded optimization complete, 2026-10-04. Owner acceptance and
release finalization remain separate. No current correctness failure or required
human action blocks this completed work unit.

| Question / measured cost | Final disposition | Evidence or reopening condition |
|---|---|---|
| Whole loading/training path | Retained implementation; 64k x 2 pipeline 12.9645 -> 6.9636s (1.86x), 16k 1.77x | Exact selection, statistics and complete optimizer-state hashes; three trials each |
| Loading/preparation | Parse-once FEN/direct planes, cached patterns and input-buffer slices retained | 128k + held-out 2.2311 -> 1.3666s; ~46% fewer allocated bytes; source/NNUE/line-boundary checks pass |
| JSON tree allocation | Selective decoder rejected; production strict decoder unchanged | Only 1.9% median gain; initial strictness failure fixed in prototype; complexity unjustified |
| Sparse forward/backward and Adam | Width-eight row stamps, contiguous updates and exact lane-wise SIMD retained | 64k compute 11.6804 -> 6.0931s; 18.4 MB less bookkeeping; eight-epoch exact state |
| Dense head | 48 KiB cached transpose retained; dense-backward SIMD prototype rejected | Transpose preserves sum order; final extra backward experiment only ~2-4% amid variability |
| Ordinary JIT SIMD / portability | SuperWord control showed no material pipeline advantage; explicit Vector API justified | Java 21 unchanged; scalar, preferred and forced 128-bit smoke match; ARM/native macOS unmeasured |
| Locality / precomputation | Row sorting rejected; full relation cache not selected | Sorting slower; full-board relation indices alone about 32 GB at 8M; relative map already cached |
| CPU parallelism | 2/4/6 workers tested and rejected for production | No further scaling, changed reduction state, ~24 MiB/worker; ~10% after exact transpose insufficient for added complexity |
| GPU | Actual RTX4060 FP64 optimizer probe; partial offload rejected | Transfers erase resident-kernel advantage over CPU SIMD before packing cost |
| Full device-resident backend | Explicitly deferred as disproportionate architecture work | New custom sparse forward/backward/reduction, checkpoint/cancellation and native runtime; reopen only with compelling whole-graph evidence |
| Efficacy / integration | 33 targeted tests pass, zero skipped; Windows native runtime smoke passes | Exact full state through 4,096 updates; BRN/NNUE service/resume and UCI stderr protected |
| Final dominant costs | Adam ~34%, sparse relation traversal ~19%, backward ~33% of sampled top frames | Further major gain needs changed memory/backend design; no demonstrated worthwhile local candidate remains |

Resume after completion: read contract, T04 and compact committed evidence; inspect
current Git and runtime before drawing new conclusions. Do not repeat closed
experiments or strength gates without new evidence. The next admissible work unit
requires a new representative profile/hardware result or an explicit backend or
learning-method scope decision. Optional owner-scale acceptance would establish
actual 8M loading, peak memory/GC and complete campaign time; it is not required for
this workstream's bounded completion and must not be run routinely by Codex.

Version finalization remains DEFERRED_FINALIZATION under the prior reservation
documented in the contract. Build33 is unchanged; no old token was recovered,
finished or bypassed. Separate attributable diffs/commit preserve this later work.
Only a future release requiring finalized provenance depends on resolving that
existing gate. No push or deployment occurred.
