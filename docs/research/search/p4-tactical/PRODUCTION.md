# TC-P1 rolling experimental production integration - 4 November 2026

This bounded integration implements the owner's explicit adoption of exact TC-P1
across W1-W16, accepting the unresolved W4 competitive warning. It is separate
from TC-R001's completed, unsuccessful strength-development execution. Starting
state: clean main1f55124b,0.0.0/build89, mastersP4-R015/P4-F031; no root journal.
Current canon is P4-R016/P4-F032; focused tactical CONTRACT revision2. No shared
behavioral governance, frozen experiment, model, clock policy or search parameter
is changed. The production source commit and final artifact receipts are recorded
below after qualification; build90 belongs to the committed runtime integration.

## Exact selection and reproducibility

Normal GUI Play, built-in Arena and standalone UCI use **TC_P1**, exact **HL-G1 +
bounded capture ProbCut**, **flags1177812991**. The one runtime edit is in shared
`BaselineEngine`: default selector plus TACTICAL_VERIFICATION over HL-G1. No new
Search implementation. The kernel/configuration/parameters from TC-P1 implementation
`a7c45dffeaffc362bc7c7026d03a2e6434472be2` are preserved: eligible noncheck interior
null-window depth4-8; raw+300 guard; at most3 SEE-nonnegative legal tacticals;
beta+150 qsearch then depth-3 child verification; selective original-beta return.
Probe isolation suppresses nested verification and learning/TT side effects while
real moves extend real history and all work consumes the request budget.

Unchanged: frozen CP64/checkpoint64/S6CNU002/eight heads/BT4_Q_LOGIT400_V1,
PVS/ADAPTIVE16, requested Hash64, BOUNDARY2 with explicit FACTUAL fallback, default
W1, supported W1-W16/bounded Max, OWNER at W1 and ROOT_REUSE at W2-W16. CP64 file
SHA2568431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b.
HCE, BREP2-32 and unrelated payloads retain flags262143. Explicit historical
Construction/Policy4/E008/research profiles remain independent.

| Selection | Meaning | Flags |
| --- | --- | ---: |
| No property, or `-Dseedv6.searchProfile=TC_P1` | Current rolling production: HL-G1 + exact TC-P1 | 1177812991 |
| `-Dseedv6.searchProfile=HL_G1` | Previous HL-G1-only production rollback | 104071167 |
| `-Dseedv6.searchProfile=LEGACY` | Established pre-HL rollback with current correctness fixes | 3407871 |
| Explicit research `--profile probcut` | Frozen TC-P1 formulation | 1177812991 |
| Explicit research `--profile guided` | Frozen HL-G1 formulation | 104071167 |

Invalid production or research selectors fail. Explicit core API selection wins;
bindings snapshot selection and preserve it across clock/hash/worker reconstruction,
reset and reuse. Research profiles do not inherit the production property. Exact
historical binaries still require pinned source/artifacts, not merely matching flags.
The37-file frozen89 and33-file frozen84 manifests were present and hash-verified.

For a native GUI or UCI rollback, launch from a separate PowerShell with a
process-local JVM option (preserve existing options if present):

```powershell
$previousOptions = $env:JAVA_TOOL_OPTIONS
$env:JAVA_TOOL_OPTIONS = "$previousOptions -Dseedv6.searchProfile=HL_G1".Trim()
& '<candidate>/SeedV6-UCI/SeedV6-UCI.exe'
# GUI: & '<candidate>/SeedV6-GUI/SeedV6-GUI.exe'
$env:JAVA_TOOL_OPTIONS = $previousOptions
```

Restart without the override to restore TC_P1. Java-only UCI supports
`SEEDV6_UCI_OPTS=-Dseedv6.searchProfile=HL_G1` with its generated launcher.
`BaselineEngine.load(CONVENTIONAL_CP64, clocks, SearchProfile.HL_G1)` is the explicit
API. The historical `LEGACY` meaning is unchanged.

## Research outcome and boundaries

TC-R001 completed640 independent W1 games:235W/202D/203L,52.50%, paired-normal95%
49.37-55.63%. Both original gates failed (>=53% observed and lower strictly>50%).
The sole attempt remains spent; no campaign or allowance is renewed. W4 completed
96 games:26W/32D/38L,43.75%, normal95%34.94-52.56%, an unresolved adverse warning,
neither proven regression nor neutrality. The owner explicitly accepted this risk
for adoption. Integration value and potential future combination value justify the
owner decision; adoption is not new competitive evidence or confirmed superiority.

**HL-G1/build84**, sourceb8ea8b2856eed3f6c774860ca540375054e4de30, remains research
champion. **P4-BASELINE-001/build79**, source753367614b8aa7e32dee337fd76b542737141ac1,
remains immutable. Future authorized research normally starts from the latest
owner-adopted rolling production baseline, while older controls/reverse ablations/
substitutions remain available. No About enhancement, concurrency programme,
active-bot change, push, release, installation replacement or deployment.

## Qualification and artifact record

Local raw output is retained under `raw/production-adoption-v1/`, ignored by Git;
fresh checkouts must check availability. Reused accepted TC-P1 evidence includes
W1/W4 probes, legal history/score/PV, isolated learning/TT effects, full-request
accounting and lifecycle checks linked from STATE and build89 qualification.
No competitive strength games or independent confirmation were run for adoption.
The shallow built-in Arena regression exercises routing/lifecycle only.

The initial integration regression had147 passing tests and one new overstrict
coverage assertion: a100000-node W2 request need not complete an eligible iteration
before aborted counters reset. This was a test-observation error, not evidence of a
kernel defect. The failed output/XML is preserved. The correction restricts the
deterministic completed-counter witness to W1; all16 actual production routes,
exact worker configs, private state, budgets and lifecycle remain checked. Frozen
W4 per-worker mechanism observations retain their original scope.

Final qualification, artifact identities and platform limits follow below.

- **148 JUnit tests pass**,0 failures/errors/skips:88 core Search/rules/book,
  26 UCI,5 GUI,1 built-in Arena,28 selected production/research/control regressions.
  Project boundaries and research-workspace build pass.22 Python packaging tests pass.
- Actual W1-W16 kernels match exact probcut flags with private board/PV/history/
  evaluator state. W1 production and HL_G1 rollback match their explicit research
  traversals on four real-history roots, Hash1/64, repeats/reset and100000-node caps.
  LEGACY also retains its separate frozen-control parity. Tests exercise terminal,
  repetition/mate, legal PV, board restoration, verified-null/full-request accounting,
  hard limits, cancellation/failure, drain/reset/reuse and shutdown.
- `gradlew.bat --no-daemon --max-workers=1 packageWindows
  -PcandidateId=tc-p1-adoption-precommit-v1` passed in2m17s. Both native images/ZIPs,
  extracted copies and shared core/model identity passed established packaging gates.
  These provisional artifacts are labelled build89/uncommitted source, **not** the
  historical research build89. Final committed build90 artifacts supersede them for use.
- Final committed-artifact UCI, packaged GUI and interactive qualification remains
  pending at the source commit boundary; the following evidence commit records it.
