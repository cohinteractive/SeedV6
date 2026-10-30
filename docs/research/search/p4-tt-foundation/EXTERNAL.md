# Source-informed architectural investigation

Examined 30 October 2026. This extends the accepted map for a concrete information
dependency; it does not repeat a general engine survey. Local source snapshots are
under `raw/external/`, with hashes/URLs in `bootstrap-manifest.json`. No external engine
was executed. All statements below are implementation observations unless labelled.

| Pinned source | Relevant observation | SeedV6 adaptation hypothesis |
| --- | --- | --- |
| [Stockfish19 search.cpp](https://github.com/official-stockfish/Stockfish/blob/edb0d9db6731067ec50ce619ff372b463bc4dd5d/src/search.cpp), [tt.h](https://github.com/official-stockfish/Stockfish/blob/edb0d9db6731067ec50ce619ff372b463bc4dd5d/src/tt.h), [tt.cpp](https://github.com/official-stockfish/Stockfish/blob/edb0d9db6731067ec50ce619ff372b463bc4dd5d/src/tt.cpp) | The copied record distinguishes move, value, eval, depth, bound and PV. Raw eval is stored before correction; Search can reuse it, compute corrected static value, and use a direction-compatible searched bound as a different estimate. Improving, pruning, ProbCut and singular extensions have distinct conditions. Eval-only admission is possible. Replacement considers depth/age/PV/exactness and preserves same-position moves. | Preserve meanings independently and demonstrate actual consumers; do not transplant correction history, race-tolerant access, compressed keys, bound substitution or external margins automatically. |
| [Ethereal14 search.c](https://github.com/AndyGrant/Ethereal/blob/96cde8d2f82a3ab0536bf33572405fda96f6e236/src/search.c), [transposition.c](https://github.com/AndyGrant/Ethereal/blob/96cde8d2f82a3ab0536bf33572405fda96f6e236/src/transposition.c) | TT exposes eval separately from searched value/depth/bound/move. Main Search and qsearch reuse eval. Null admission vetoes an upper-bound record below beta; other material/check/PV/exclusion/static-eval conditions still apply. Singular/ProbCut consumers have their own depth/eligibility. Bucket replacement uses depth/age and near-depth same-key retention. | A conservative qualified upper-bound veto can avoid speculative null work while falling through to legal Search. Raw-eval reuse can reduce repeated NNUE work without altering scores. Neither hypothesis is proven by these sources. |
| [Bagatur5.5c Search_PVS_NWS.java](https://github.com/bagaturchess/Bagatur/blob/56d1b3651f62bc770c7cde5f4c1b0360890b9bd5/Search/src/bagaturchess/search/impl/alg/impl1/Search_PVS_NWS.java), [TTEntry_BaseImpl.java](https://github.com/bagaturchess/Bagatur/blob/56d1b3651f62bc770c7cde5f4c1b0360890b9bd5/Search/src/bagaturchess/search/impl/tpt/TTEntry_BaseImpl.java), [EvalCache_Impl2.java](https://github.com/bagaturchess/Bagatur/blob/56d1b3651f62bc770c7cde5f4c1b0360890b9bd5/Search/src/bagaturchess/search/impl/eval/cache/EvalCache_Impl2.java) | A separate evaluation cache exists. Search captures rawStaticEval before correction, computes improving, and may substitute a direction-compatible TT value only at comparable depth. Its divergence reduction uses the raw-vs-searched difference. Verified null has explicit same-ply scratch restoration. Cache implementation uses key/score XOR with optimistic reads and treats zero as a miss. | Identity, zero validity, lifetime and concurrency guarantees must be explicit. Keep SeedV6's coherent locking and explicit zero-valid marker. Do not transfer Bagatur's combined correction/reduction assumptions or treat a separate cache class as proof every evaluator route uses it. |

External tags/commits are exactly those in the accepted map: sf_19 /
edb0d9db6731067ec50ce619ff372b463bc4dd5d; v5.5c /
56d1b3651f62bc770c7cde5f4c1b0360890b9bd5; v14.00 /
96cde8d2f82a3ab0536bf33572405fda96f6e236. Focused code ranges include Stockfish
search 814-920, 988-1105, 1240-1305, 1695-1760 and TT save/replacement; Ethereal
search 353-378, 416-505 and qsearch, and TT probe/store; Bagatur search 643-782 and
cache get/put. Snapshot hashes identify the bytes actually retrieved.

No individual-mechanism empirical result was independently validated. Ethereal's
inline approximate Elo attributions lack a protocol/raw-evidence basis in these files;
they are author annotations, not a SeedV6 forecast. The bounded historical SeedV6
H1/H2 findings establish feasibility/availability, not downstream benefit here.
Selected hypotheses need new causal, correctness and cost evidence. Clustering,
qsearch score storage, correction history, singular extensions and ProbCut remain
unselected unless a measured dependency makes them proportionate.
