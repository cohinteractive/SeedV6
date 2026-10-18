# Phase 3: Policy 4 production integration and promotion

19 October 2026 (Pacific/Auckland). **Integrated, validated and promoted as the
normal Pair-2 production Search champion/default at 1-16 workers.** The owner
accepted the candidate before this launch and authorized promotion upon passing
the integration gates. No new CGLHW, strength study, release or deployment begins.
The [Contract](../../../source/CHESS_SEARCH_CONTRACT.md) P3-R007 and
[Frontier](../../../source/CHESS_SEARCH_RESEARCH_FRONTIER.md) P3-F005 remain the canon.

## Identity and behavior

Started on clean `main` at `674ca04` with the expected P3-R006/P3-F004 masters.
The owner confirmed their Project Source synchronization. The original
[O1/O2 handback](P3_INFORMATION_REUSE_2026-10-18.md), closed protocols, frozen
installations and results are preserved unchanged.

Before this integration, `ProductionSearch.create` selected the cumulative
SR-001 + SR-029 + SR-022 + SR-005 + SR-036 + SR-024 engine: QXRAY, protected quiet
LMR, WINDOW, EFFORT at one worker and ROOT_REUSE at 2-16. Completed legal TT hints
were an opt-in research addition. Production now enables exactly that existing
hint mechanism for Pair-2. Main history still resets per fixed-depth invocation;
ROOT_REUSE retains private seeding. Retained history, nominal-only learning and
Policy 7 are not selected.

The change is factory activation through `ExactSearch.withCompletedHints` and
`ParallelSearch.withCompletedHints`, with one adapter forwarding method. There is
no second implementation or production dependency on research. TT layout, locking,
replacement, SearchKey identity, generation/proof admission and evaluator arithmetic
are unchanged. Requested default Hash remains 64 MiB (2,097,152 existing entries).
Completed selective hints do not become nominal mathematical bounds. Restricted
root jobs retain their existing hint/proof restrictions.

The principal evaluator remains BRE-Pair 2 generation 32, lineage
`235d0688-e060-44e3-8f1e-119c39bc8017`, checkpoint
`g000032-s001753088-5837c7f7192aa3cf9a265388f93649327455e804541398ae3ef6b69e09014304`,
payload SHA-256 `5ab54abe6063422a78a70ba2901492640c65dcf18a731309ed487e6aac484eb7`,
mapping `BRN_PAIR2_MATERIAL_RESIDUAL_QUARTER_V1`. Its model and manifest are unchanged.

| Entry point/control | Resulting behavior |
| --- | --- |
| Standalone `UciMain` / `ProductionUciConfiguration` | Pinned generation-32 Policy 4; normal Threads/Hash changes recreate the same production composition. Identity reports completed hints and actual history/learning policy. |
| Legacy `Main` / `LegacyUciEvaluation` | Default HCE unchanged. Explicit Pair-2 selection and warmup use Policy 4 and report its hint/history identity. |
| GUI `EngineSearchAdapter`, `SearchLifecycleService`, `SearchDriver` | Existing shared factory routes activate Policy 4 for Pair-2. Evaluator selection and stores are unchanged; no GUI implementation change. |
| `ProductionSearch.createInformationControl(workers, evaluation[, hashMiB])` | Previous cumulative Phase 2 composition, completed hints off; other evaluators unchanged. |
| `InformationResearch` / `P3InformationUciMain` policy 0 versus 4 | Policy 0 explicitly selects the Phase 2 control; policy 4 retains its independent accepted construction. Existing launcher property `seedv6.uci.informationResearch` suffices. No additional production switch. |
| Historical research controls | `P3HistoryStudy`, `Sr021Study`, `Sr032Study`, `Sr033Study`, `Sr036Cost` and `UciEvaluation` pin their previous cumulative comparisons explicitly. Historical selector labels are preserved; frozen copies are untouched. |

HCE, NNUE incremental/full refresh, BRN, BRN-1, BRN-2 incremental/full refresh and
BRN-3 retain their previous policies with completed hints off. Pair-2 selection
continues to use the existing family boundary; other checkpoints have no new Elo
claim. No evaluator substitution, calibration or training change is made.

## Equivalence and validation

The [configuration receipt](p3-policy4-production-2026-10-19/configuration-equivalence.json)
binds both confirmation protocols' frozen candidate core JARs. **179 of 182 core
classes are byte-identical**, including ExactSearch, ParallelSearch, TTable,
SearchKey and history/evaluator implementations. Only ProductionSearch,
ExactSearchAdapter and its observer class differ. The observer's `javap -p -c`
output is identical; its class difference is debug line metadata. Source review
confirms the adapter adds only configuration forwarding and comments.

The new `Policy4ProductionTest` inspects actual factory state before adding
observation-only counters. It checks all 1-16 worker constructions against research
Policy 4, shared TT/private board and history ownership, 64 MiB capacity, and absence
of the O2 alternatives. Single-worker scores, best moves, PVs and node counts match
across iterative, repeated and changed-history requests, including EP, promotion,
mate and stalemate roots. Runtime tests at 1/2/4/8/16 workers and Hash 1/64 exercise
actual hint stores/hits, the real hints-off path, proof separation, repeated reuse,
new-game clearing, exact shared node caps, interruption, draining and recovery.
Other evaluator fixtures complete depth-three searches at 1/4/16 workers with
unchanged policies, exercising the multiworker coordinator as well as owner paths.

The [validation manifest](p3-policy4-production-2026-10-19/validation.json) binds
test suites, logs, local result paths, JVM identity and artifact hashes:

| Validation group | Result |
| --- | --- |
| Focused Policy 4, completed hints, information lifecycle and Phase 2 baseline tests | 20 passed; final run includes depth-three evaluator compatibility. |
| Core TT/proof/score, rules/history/draw and Search support | 47 passed. |
| Research ExactSearch/TT-off/PVS, history/iteration reuse, production SEE, concurrency, driver/lifecycle and NNUE integration | 164 passed. |
| Standalone UCI module | 14 passed, including pinned production composition at 1/4/16 workers. |
| Legacy application production/boundary tests | 13 passed; shared-core packaging contains no research classes or duplicate implementation. |
| Selected UCI process and headless GUI integration | 51 passed, one pre-existing Training-panel widget failure, one existing native/headless skip (details below). |
| Compilation and Java packaging | Passed project dependency guard, module/research-launcher compilation, standalone install/ZIP, legacy GUI-containing distribution ZIP and research workspace assembly. |
| Standalone production UCI runtime | **159 searches passed**, including all 52 frozen Policy 4 reference positions, all workers 1-16, exact budgets, independently legal PVs, clocks/stop/reset/EOF, mate distances, Hash minimum/non-power-of-two/default, allocation-failure fallback, model rejection and relocated launcher with spaces/unrelated cwd. |
| Explicit Phase 2 control UCI runtime | **All 52 original Phase 2 positions matched score, node count and PV**, using current policy 0. |
| Historical research UCI configuration/lifecycle | 14 passed, including book-on bypass and book-off ordinary Search at 1/4 workers for HCE and Pair-2; optional book/tablebase composition is preserved. |

The old standalone verifier initially rejected the changed default's node counts
(start position D3: Phase 2 477, Policy 4 479). This is the intended ordering change,
not a mismatch with the accepted candidate. Its original vectors remain intact.
The existing verifier now accepts an explicit reference-vector receipt while
requiring exactly the original position/depth/history list and retaining its exact
score/node/PV and lifecycle assertions. The new
[52-position fixture](p3-policy4-production-2026-10-19/policy4-uci-vectors.json)
was captured from the **frozen accepted Policy 4 binary**, after verifying every
declared runtime-file hash, before comparison with integrated production. It is a
deliberate compact regression fixture, not a regenerated independent oracle. The
[control receipt](p3-policy4-production-2026-10-19/phase2-control-equivalence.json)
separately preserves the old behavior. No raw-output ignore exception was added.

All Gradle invocations used the existing worktree-local
`build/migration/gradle-user-home`, `--no-daemon --max-workers=1`; no concurrent
tournament or broad performance experiment was launched. Exact commands are in
the preserved logs under `p3-policy4-production-2026-10-19/raw/`. Reproduction of
the production process check uses the existing Python environment:

```text
build/research/venv/Scripts/python.exe -B tools/verify-standalone-uci.py --java C:/java/jdk-21/bin/java.exe --install app-uci/build/install/seedv6-uci --model build-assets/production/bre-pair2-g32.brn-pair2 --ue-repo C:/projects/seed/java/seedv6-ue --reference-vectors docs/research/search/p3-policy4-production-2026-10-19/policy4-uci-vectors.json --out build/migration/NEW-UNUSED-DIRECTORY
```

The process report/transcripts remain at
`build/migration/policy4-production-2026-10-19-uci-r3/result.json`; the manifest
records its digest. Raw logs/XML/transcripts and capture scripts remain local and
ignored. A fresh checkout must check availability. Gradle tests and the tracked
Policy 4 vector fixture do not require the old confirmation runtime; recapturing
that fixture does. Local packages carry the starting revision plus dirty state;
their hashes, not the unchanged displayed build number, identify the tested bytes.

## Limitations and completion boundaries

The broad integration task had one failure:
`BrnPair2GuiTest.capabilitiesAndRecipeEditorsAppearInExistingTrainingPanel` cannot
find the named architecture widget (NPE at line 61). The same test/source fails
identically with the hash-verified frozen pre-integration core substituted on the
test classpath. No GUI source changed. It is a pre-existing Training UI/test issue,
outside the passed Search and GUI Search-adapter gates, left for separate work.
`ThreadAndTerminationTest.nativeRunPolicyShowsOnlyItsActiveBoundAndRendersMax`
was skipped by its existing headless assumption. Native GUI interaction, real
Arena/BotLi client acceptance, native package images and macOS execution were not
newly tested or certified. They are not changed by this factory-only integration.

Failed setup attempts are preserved: the new test initially omitted NNUE's mapping
argument (corrected); a temporary baseline-classpath assertion compared a Set to a
List (corrected in a new script); system Python lacked chess (the existing research
environment supplies chess 1.11.2); the old Phase 2 vector assertion failed as
described above. No failed evidence or closed protocol was rewritten.
Final review also corrected the historical control's lifecycle construction to
preserve its optional book/tablebase facilities; the additional 14-test run passed
after that correction. The normal production runtime was unchanged by this repair.

Strength evidence remains the accepted separate one-/four-worker, generation-32,
10+0.1 confirmations: +54.7 Elo [+4.8,+107.0] and +88.7 [+34.3,+147.9] under the
adjusted approximate paired-normal rule. Conservative bounded intervals include
zero; four-worker CPU was about 0.98% higher under equal clocks. Compatibility
does not prove gains at other workers, evaluators, checkpoints, clocks, Hash
pressure, hosts or platforms. No tournament was rerun or extended. Longer-game
hint pressure and the unadopted history/filter combinations remain opportunities,
with no next objective activated.

VERSION_STATE.txt remains byte-identical at **0.0.0 build 35**, SHA-256
`92a26d4e187edc6c47ed88100d355f77b4597d98524d21d005a3c164986b4bca`.
Per the task boundary, no finalizer invocation, investigation, state edit or bypass
occurred. The owner is handling that inherited issue separately. No finalized
version, build advancement, external synchronization, push, release or deployment
is claimed. The root journal was absent and was not created.

The implementation, tests, control pinning, verifier extension, compact evidence
and two masters form one attributable commit atop the clean starting HEAD. Raw
evidence is excluded. Production promotion is recorded under the owner's prior
authorization; this handback still returns to GPT/owner for reconciliation.

**PROJECT_SOURCE_REFRESH_REQUIRED: YES** — replace both masters' Project Source
mirrors: `# SeedV6 Search Contract - Phase 3`, **P3-R007**; and
`# SeedV6 Search Research Frontier - Phase 3`, **P3-F005**.

Human actions required after this prompt:
- **Blocking dependent GPT planning/next launch:** refresh both mirrors and confirm
  synchronization. The completed repository integration did not depend on this new refresh.
- **Blocking finalized-version/release operations only:** complete the owner's
  separately handled finalizer reconciliation before operations requiring finalized identity.
- **Non-blocking required actions: None.** The unrelated Training-widget test can
  be addressed in a separate authorized task; no additional Policy 4 acceptance
  decision or fresh strength confirmation is required for this equivalent integration.
