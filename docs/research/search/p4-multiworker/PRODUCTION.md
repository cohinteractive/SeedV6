# MW-C4 W4 rolling production adoption - 5 November 2026

Revision 1, final qualification complete. Owner-authorized production integration, separate from the accepted
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
The source commit **44fc2bf2fe54e87dee7721bdaa451f3b00f3b1d0** carries the sole
build95->96 change. (The full authoritative commit is also in the manifest.) Clean
committed build96 qualification below supersedes the pending artifact boundary
recorded in that source commit. No second build increment follows this evidence.

## Committed build96 artifacts and executed consumers

[Compact qualification manifest](production-build96-qualification.json) pins full
source, configuration, runtime, artifact/evidence hashes and the changed-file list.
`packageWindows :app-uci:installDist :installResearchWorkspace
-PcandidateId=mw-c4-build96-v1` passed from the clean source in1m28s. Native images,
ZIP integrity and independently extracted copies passed established gates. The
GUI/UCI JAR entries match the qualified precommit packages except application
version/source metadata and the desktop build timestamp; all executable class
bytes match. An initial overstrict comparison rejected the expected desktop
build timestamp; inspection confirmed that sole additional metadata difference.

| Artifact | Repository-relative path |
| --- | --- |
| Native GUI | `dist/windows/candidates/mw-c4-build96-v1/SeedV6-GUI/SeedV6-GUI.exe` |
| GUI ZIP | `dist/windows/candidates/mw-c4-build96-v1/SeedV6-GUI-windows-x64.zip` |
| Native UCI | `releases/candidates/windows-x64/mw-c4-build96-v1/SeedV6-UCI/SeedV6-UCI.exe` |
| UCI ZIP | `releases/candidates/windows-x64/mw-c4-build96-v1/SeedV6-UCI-windows-x64.zip` |
| Java UCI | `app-uci/build/install/seedv6-uci/bin/seedv6-uci.bat` |

- **Native UCI:**184 search responses,46 per profile (normal MW_C4_W4, exact TC_P1,
  HL_G1, LEGACY), two sessions/profile. All W1-W16, Hash1/64, worker/hash/clock
  reconstruction, BOUNDARY2/FACTUAL, node caps, movetime, stop, Clear Hash,
  repetition/mate/stalemate, legal PVs, stale-result suppression, reuse, active quit
  and EOF passed. Terminal/history cases explicitly ran at W4 after leaving and
  re-entering it. Invalid selector exited nonzero before uciok.
- **Packaged GUI Play:**264 requests,66 per profile, on the actual GUI JARs and
  bundled Java21 runtime, headless EDT/services. Worker sequence1,4,8,16,4,1,
  depth/movetime/stop, twofold/threefold, mate/stalemate, ordinary score domains,
  legal PVs, board restoration, reset/reuse and shutdown drain passed.
- **Packaged built-in Arena:**four four-ply depth4 games, one bounded color pair
  each for normal W4 MW-C4 and explicit W4 TC_P1. Actual telemetry identifies the
  expected flags/scheduling/resources; completion, persisted resume and close
  passed in new local fixture stores. This is technical execution, not a tournament
  or competitive evidence. Source Arena checks also covered other evaluator routes.
- **Generated Java UCI launcher:**12 depth3 requests across default, explicit
  MW_C4_W4 and TC_P1 via SEEDV6_UCI_OPTS; W4->W1->W4->W16 reconstruction, legal
  moves, build96/source identity and active-search quit passed.
- **About identity:**actual packaged idle/active components were rendered offscreen
  and visually inspected. Version96/source44fc2bf2fe54, worker-dependent production
  defaults, active MW-C4, flags and EAGER_ROOT are visible. This is not interactive
  desktop acceptance. W4 CP64/Hash64/BOUNDARY2/vector-256 configuration fingerprint
  is `cfg-965d2f2609b383f7`; the full reproducible text is retained in the manifest's
  referenced active.txt and UCI identities.
- **Confirmed mechanism bridge:**all11 ExactSearch/ParallelSearch/FlatSearchConfig/
  TTable class entries match the frozen build94 core byte-for-byte. Shared core
  JAR SHA is identical in native GUI, native UCI, Java UCI and research workspace.
  The C6 source remains separate and unselected; no C6 runtime/strength claim follows.

No failing validation remains. Source JUnit results were not represented as a new
219-test rerun after the metadata-only commit. No full unrelated training suite,
macOS/mobile execution, interactive Windows play or external persistent-client
acceptance was performed. Bounded persistent service correctness is not warmed
persistent-client competitive qualification. Longer clocks, worker-count strength
transfer and historical BOUNDARY2 clock studies remain unqualified/unopened.

## Final authority and handback

Production source integration and bounded Windows qualification are complete.
Final masters **P4-R029/P4-F047** reflect W4 adoption and TT-R001's separately
accepted inconclusive closeout. MW-R001 remains accepted and closed; no new
research objective is created. The raw evidence paths above must be checked for
availability in a fresh checkout. The version-byte SHA-256 is
`a63ad996f079321df2f279f1447bfdda5da71c89e80c9fbd59be84a94ab4b942`.

No installation, running-bot replacement, push, release or deployment is authorized
or performed. macOS, interactive desktop acceptance and external live clients are
not established by headless checks. Project Source mirrors remain owner-maintained;
the final pair requiring refresh is P4-R029/P4-F047.

Human actions required after this prompt:

- Blocking for completed repository adoption and TT-R001 closeout: **None**.
- Owner refresh of Phase4 Project Sources to **P4-R029/P4-F047**: non-blocking for
  this completed work, blocking only before mirror-dependent planning.
- Deployment, if desired: separate owner authorization and installed-client/
  platform qualification; non-blocking for source integration. No installed or
  running bot was changed. There is no autonomous follow-on research command.
