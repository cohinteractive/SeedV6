# MW-C4 W4 rolling production adoption - 5 November 2026

Revision 1. Owner-authorized production integration, separate from the accepted
MW-R001 research closeout and the TT-R001 accepted inconclusive closeout.
Starting state: clean main `09d415c1`, build95, P4-R027/P4-F045. TT-R001 closeout
is separately committed at `e2192625` (P4-R028/P4-F046). No root journal exists.

## Selection and mechanism

The shared `BaselineEngine` default is **MW_C4_W4** without a JVM property.
For frozen CP64 it resolves to **MW-C4 EAGER_MEMORY at W4**, flags1182007295,
EAGER_ROOT scheduling; **W1-W3 and W5-W16 retain exact TC-P1**, flags1177812991,
OWNER at W1 / ROOT_REUSE otherwise. Default worker count remains1, limits1-16,
and Max remains bounded. This is deliberately worker-dependent production.

MW-C4 is the existing TC-P1/HL-G1 foundation plus eager finite-window root work
and guarded current-generation heuristic TT scores. Full/unbounded windows and
shallow roots keep the existing owner-first fallback. No heuristic, TT eligibility,
parameter, clock policy or concurrent Search algorithm is changed. The current
ExactSearch, ParallelSearch, FlatSearchConfig and TTable source still matches the
confirmed build94 core3ba1daad. Later build95 lifecycle changes remain; the opt-in
C6 independent-iteration implementation is never selected by this production path.

ConstructionSearch now carries scheduling in its immutable configuration, factory,
equality and identity; its existing four-argument constructor retains STANDARD
behavior for historical/research/exact controls. Formulation names require both
flags/parameters and scheduling; same flags under ROOT_REUSE cannot claim MW-C4.
Bindings snapshot the profile and resolve worker-dependent flags and schedule on
every worker/hash reconstruction; clock rebinding preserves that profile.
GUI Play, built-in Arena and UCI consume this single shared engine-core factory.
HCE, BREP2-32 and unrelated conventional payloads keep their prior selections.

CP64 remains checkpoint64, S6CNU002/eight heads/BT4_Q_LOGIT400_V1, file SHA-256
`8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b`.
PVS/ADAPTIVE16, requested Hash64 (2,097,152 slots / 50,331,648 entry-payload bytes),
BOUNDARY2 with explicit FACTUAL fallback, private worker learning and history-rich
SearchKey semantics are unchanged. Score reuse remains an ordinary selective
estimate with legal hints, sufficient depth, eligible interior null windows and
current generation, excluding mate/synthetic/verifier domains. No aged retention,
TTF information layout or general TT-policy relaxation enters this adoption.

## Exact rollback and historical identities

Use **`-Dseedv6.searchProfile=TC_P1`** for exact previous TC-P1/ROOT_REUSE at W4
and TC-P1 at every other worker count. `MW_C4_W4` explicitly selects the new
worker-aware policy. `HL_G1` remains flags104071167; `LEGACY` remains flags3407871.
Invalid selectors fail. Research `probcut`, `guided`, H1/H2 and MW architecture
selectors retain their meanings and do not inherit the production property.
For core API rollback use `BaselineEngine.load(CONVENTIONAL_CP64, clocks,
SearchProfile.TC_P1)`. For generated Java UCI use process-local
`SEEDV6_UCI_OPTS=-Dseedv6.searchProfile=TC_P1`. For native GUI/UCI launchers use
process-local `JAVA_TOOL_OPTIONS` containing `-Dseedv6.searchProfile=TC_P1`, preserving
other existing options; restart without this override to restore the rolling policy.

Immutable baseline79, corrected control80, W1-qualified champion HL-G1/build84,
original TC-P1/build90, accepted About/build91, confirmed MW-C4/build94 and
build95 remain distinct historical identities. No champion advancement occurs.
This source adoption advances only the rolling production baseline.

## Reused strength evidence and limits

[MW-R001 RESULTS](RESULTS.md) and its frozen runtime/configuration manifests retain
160 fresh W4 pairs /320 games versus exact TC-P1:150W/95D/75L,61.71875%, paired-normal
95%57.5083-65.9292%, passing score>=53% and lower>50%. Frozen research source9d380c30,
core3ba1daad/build94, flags1182007295 versus1177812991. Domain: W4 CP64/Hash64,
PVS/ADAPTIVE16/BOUNDARY2,10+0.1, Windows Ryzen5500/Java21 and the documented fresh
opening/cold-JVM protocol. All39 files in the binding/runtime manifest were locally
available and rehashed. No strength tournament or renewed confirmation was run.

This integrated result does not isolate TT/scheduling strength or qualify other
worker counts, longer clocks, warmed persistent clients or other platforms.
Adverse fixed-work evidence remains. TT-R001's historical inconclusive outcome
and TTF-R001's limited foundation acceptance are not changed by MW-C4 success.

## Source qualification and commit boundary

**219 JUnit checks passed, zero failures/errors/skips**: first75 production routing,
identity, W1-W16 private-state/accounting/history/PV/reuse, GUI Play, built-in Arena,
UCI and eager/memory integration checks; then144 TT coherence, rules, independent
exact controls, driver/lifecycle, verified-null and stop/drain checks. The Arena
fixture is bounded shallow execution in temporary stores, not strength evidence.
**38 Python packaging checks passed.** An initial import failure after renaming
a verifier constant was corrected; the failed log remains. No engine defect was
found. `verifyProjectBoundaries` and research workspace compilation passed.

Windows GUI/UCI packaging, native launch checks and extracted ZIP checks passed
in2m15s using `packageWindows :app-uci:installDist :installResearchWorkspace
-PcandidateId=mw-c4-adoption-precommit-v1`. These provisional artifacts carry
build95/uncommitted-source metadata and are not historical build95 artifacts.
Compilation uses Java21; native packaging uses Temurin21.0.12.1+1 Windows x64.

Raw logs, JUnit XML, summaries, historical hash checks and version baseline are at
`raw/production-adoption-v1/`, ignored/local-only. Frozen evidence is untouched.
The following source commit carries the sole build95->96 token change. Clean
committed build96 packaging and further packaged consumer checks are pending at
this commit boundary; their exact identities/results will be recorded here and
in the compact qualification manifest after execution. Source qualification does
not claim those later results in advance.

No installation, running-bot replacement, push, release or deployment is authorized
or performed. macOS, interactive desktop acceptance and external live clients are
not established by headless checks. Project Source mirrors remain owner-maintained;
the coordinated final canon update and its revision pair follow artifact qualification.
