# E007 fixed-candidate prospective confirmation

Frozen before any confirmation games,2026-10-22 UTC /23 October Auckland.
E00764 plus exact optional SIMD completed the reused development screen8W/5D/3L.
This justifies a larger test, not competitive acceptance. Frozen candidate is
raw/e007-evaluation64/network.cnnue (payload5da0253642ab33b8e446b5b1c5764750be008cd3dbacc7cd1992a95bf4a881f6),
installed6213630/build64 runtime, unpruned NNUE search, vector module resolved.
Opponent is unchanged frozen Gen32/Policy4. The match protocol hashes exact files,
commands and jars before the first game. No runtime rebuild or candidate change
is permitted during this test; resumption verifies the same protocol.

One fixed endpoint:64 opening pairs/128 games,200ms per move,one worker per engine,
32MiB hash,no book/ponder,one game active. Color-reverse each opening;800-ply cap
is censoring, never a draw. Formal python-chess endings include claimable draws.
No evaluator-score adjudication. Complete all128 unless owner control, genuine
process/correctness failure or resource/safety boundary intervenes. Report progress
without treating interim scores as acceptance or repeatedly extending the same test.

Use e007-confirmation-openings-v2.json and its selection manifest,seed2026102301.
Read-only local legal16-v1.pgn input SHA256eaf3c706dc6038d18a05c8608417a5ee2e3e43438487d73bbf2c7191183db42f,
from the existing Stockfish books preparation documented in
../search/SR021_FUTILITY_PHASE2_2026-10-15.md (upstream65815ccdbc7727cd4f6aee252ba8f67fb740e92f).
Uniform shuffle of distinct legal standard-start16-ply nonterminal prefixes, with
no evaluator/outcome filter. Exclude inspected tracked selection/prior-root records
and the NNUE development roots; normalize en-passant to legally available captures.
The first unused selection with raw en-passant fields is preserved as superseded;
no games used it. Manifest lists every exclusion input hash and selected position.
This does not establish absence from unknown historical games or training data,
and related opening-family dependence may remain. No prior research is reactivated.

At the final endpoint report W/D/L,censoring,paired mean score and descriptive Elo,
paired-opening percentile bootstrap95% (fixed10000 resamples),and paired-normal95%
interval using1.96*sampleSD/sqrt(64). No independent-game binomial interval or
sequential significance claim. Competitive evidence requires completed uncensored
games,mean>0.5,and both stated lower score bounds>0.5,plus preserved correctness,
runtime/resource gates. A positive result is limited to this hardware,clock,worker
and opening distribution; it is a Search Phase3 candidate,not an automatic default
change,owner acceptance or transfer of Policy4's authority to NNUE. A negative or
inconclusive result triggers evidence-driven further work,not programme termination.

Run through owner compute control with --vector --confirmation. NORMAL persists
without expiry. Hold checks between moves preserve finished games and safely stop
owned processes; use identical --resume invocation to continue an interrupted run.
Active asynchronous monitoring supports user-facing reports at least every15min
(and the host's more frequent updates); log files alone are not a reporting fallback.
