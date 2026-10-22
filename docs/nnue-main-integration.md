# Checkpoint-64 NNUE release: main integration and source build

23 October 2026 (Auckland). **Main integration and Windows source-build gates
passed. Native M1 acceptance and live activation remain pending.** No push, Mac
installation change, BotLi configuration change or Lichess connection occurred.

## Git integration

The starting worktree was clean on `release/nnue-cp64-e008-rc1` at
`d87657665268ada8bdcdd6911eed3f92084bec38`. Local `main` was
`859a61976026b1143b12fee55841a28e9c43a7d1`, an ancestor with no divergent commits.
`git switch main` followed by `git merge --ff-only release/nnue-cp64-e008-rc1`
integrated exactly the two release commits, `4fa0f9f984ad3822ba5260e1a431374cdc1ac262`
and `d87657665268ada8bdcdd6911eed3f92084bec38`. The release branch is preserved.
No reset, rebase, history rewrite, unrelated-work merge or application edit was
needed. This integration's follow-up commit changes documentation only; build
**65** remains correct under root AGENTS.md. The existing finalizer is not used.

At inspection, both `origin/main` and read-only `git ls-remote --heads origin main`
reported `499685abc4a84fe31c5f0bac6a4230604a5f893a`. Main already contained 25
unpushed conventional-NNUE commits before this task; the fast-forward added two.
The documentation commit adds one more. Origin is
`git@github.com:cohinteractive/SeedV6.git`, with `main` tracking `origin/main`.
The owner command is **`git push origin main`**, without force. If GitHub advances
before that push, preserve its changes and resolve the divergence normally.

## Repository-contained inference input

The release already tracked exactly one inference export at
`build-assets/nnue-candidate/conventional-cp64.cnnue`, plus `manifest.json`.
No new binary, training data, optimizer checkpoint, duplicate export or raw
research output was added by this integration.

| Property | Verified value |
| --- | --- |
| File size | 3,940,952 bytes (about 3.76 MiB) |
| Ordinary Git blob | `6cfa4a26938ec6e2cf656a126f31abffa441e155` |
| Full-file SHA-256 | `8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b` |
| Payload SHA-256 | `5da0253642ab33b8e446b5b1c5764750be008cd3dbacc7cd1992a95bf4a881f6` |
| Format | S6CNU002, checkpoint 64, eight material heads |
| Network | 10 king buckets, 256 hidden units per perspective, SCReLU, QA255/QB64/scale400 |
| Runtime resource | `models/conventional-cp64.cnnue` inside `lib/app-uci.jar` |

The public GitHub repository accepts ordinary files of this size; no LFS is
required. The [GitHub file limits](https://docs.github.com/en/repositories/working-with-files/managing-large-files/about-large-files-on-github)
are well above this payload. The complete unpushed object range was also checked:
no newly pushed blob exceeds 50,000,000 bytes. No NAS or Windows archive is used
by the source build. Old ignored experiment paths in the manifest are historical
provenance references, not build/runtime inputs.

License/provenance inspection followed E005, E006 and E007: this model was freshly
initialized and trained on the E006 sample of
[xushawn/test80-bt4-relabel](https://huggingface.co/datasets/xushawn/test80-bt4-relabel).
The publisher's ODbL declaration was rechecked; it is not a CC0 dataset. The
packaged candidate guide now carries source attribution and the
[ODbL link](https://opendatacommons.org/licenses/odbl/1-0/). The pinned Bullet
trainer's MIT notice remains tracked. No dataset or teacher network is packaged,
and this task assigns no new license to the application's code or own weights.

## Fresh tracked-source verification

An independent local clone was made with:

```text
git clone --no-local --single-branch --branch main . build/nnue-main-check/checkout
```

Its clean HEAD was `d87657665268ada8bdcdd6911eed3f92084bec38`. Before building,
neither `build/` nor conventional research `raw/` existed in the clone. Its own
new Gradle home was `build/migration/gradle-user-home`; no existing application
outputs or weight files were copied into it. Java was installed OpenJDK 21+35-2513
on Windows 11 AMD64. Substantial checks ran sequentially through the existing
owner controller in NORMAL, one Gradle worker and two active JVM processors.

The wrapper built `:app-uci:installNnueCandidateDist`, `:app-uci:nnueCandidateDistTar`,
`:app-uci:nnueCandidateDistZip`, and ran `:app-uci:test --tests '*NnueCandidateTest'
-PconventionalVectorTests`. Build succeeded in 42 seconds; all four selected tests
passed (zero failures/skips): 259 independent reference positions across eight
heads, actual E008 service settings after hash reconstruction, five depth-5
incremental/full-rebuild comparisons, and strict asset rejection.

The clone's tracked `tools/verify_nnue_candidate.py` tested its actual installed
executable, relocated to a directory with spaces and launched from an unrelated
working directory. It passed:

- Embedded-byte SHA verification; checkpoint, evaluator, search and clean source
  identity; one-worker enforcement; all advertised option names.
- Exact scalar/vector scores, nodes and PVs at depth 5 on four roots, plus actual
  `java.base`-only scalar fallback. Windows reported `vector-256-long` normally.
- UCI handshake/readiness, startpos/FEN, 160-ply move sequence, depth, movetime,
  clocks/increments, repeated searches, clear-hash/game resets, infinite/stop,
  terminal positions, quit and EOF; legal moves/PVs and clean termination.
- 35 searches in the stability process; 150 ms commands completed in 141..149 ms,
  1000 ms commands in approximately 991 ms; observed stop response 0.64 ms.
  Peak sampled process-tree RSS at Hash256 was **442.3 MiB**. These short Windows
  observations are not M1 speed, thermal, concurrency or long-run guarantees.
- Offline BotLi-compatible `chess.engine.popen_uci`, 16 plies with two game
  identities, Threads1/Hash256/ponder false, no unsolicited protocol messages.
- Missing, truncated, corrupt, incompatible and different valid-checksum weights
  rejected with nonzero exit and no stdout; no evaluator fallback.
- No client-directory writes or runtime JAR mutation; no training-controller
  environment requirement for the actual engine.

Additional checks passed for the POSIX launcher under Windows Git Bash, the TAR's
0755 launcher permissions, clean embedded build/source metadata, and legal depth-4
Gen32 search through ordinary `UciMain` with the clone's tracked production model.
The first ad hoc TAR assertion compared type bits as permissions; the corrected
permission-mask assertion passed. Its failed log is retained; no engine fix was
needed. Git Bash testing is not native macOS execution.

Local ignored evidence is under `build/nnue-main-check/`: `build.log`,
`validation.log`, `validation/result.json`, `extra.log`, `extra-2.log`,
`extra-result.json`, and the clone's JUnit XML reports. The clone remained clean
after all tests. The current integration edits only the guide and evidence docs;
the verified application, network and build logic are unchanged from that commit.

Hashes of the clean-clone build (source d876576, build65):

| Artifact | SHA-256 |
| --- | --- |
| app-uci.jar | `07b3ed204a2f4f6a90fca95a9df9fdf498b1636cae9c66192ac28d26d39b080e` |
| engine-core.jar | `c974fe22b751d72181442d7fd191ecdc96f212b61eec18c2e6641771ff386fad` |
| seedv6-nnue-cp64-e008-rc1.tar | `39fbbefbada18043554a225a58f05d237e86fc7b0bfd6ffb47cdc05614bdc594` |
| seedv6-nnue-cp64-e008-rc1.zip | `c5e609c1439f54f3ea190f73f3f8800021c9b1357ccbeee077bb0a99cebced21` |

These live under the clone's `app-uci/build/install/` and `build/distributions/`.
Subsequent documentation-commit builds have new embedded source metadata and an
updated README, so their archive/core hashes can differ without a weight change.
The original release archive and its separate receipt remain preserved.

## Mac handoff and remaining gates

Follow [the candidate guide](nnue-botli-candidate.md#normal-windows-to-mac-source-workflow)
for exact push, pull, build, executable, identity, offline native validation and
later activation commands. The Mac builds directly from main using Java 21; the
output is `app-uci/build/install/seedv6-nnue-cp64-e008-rc1/bin/seedv6-nnue-cp64`.
No network argument, manual weight copy, experimental flag, JNI or bundled JRE
is required. Keep the two jars and launcher together. General Gen32 and the
existing bot installation remain the fallback.

Effective candidate: quantized conventional checkpoint64/eight heads, recursive
PVS, QXRAY, QUIET LMR, WINDOW LMP, EFFORT, completed same-key TT hints, invocation
history/all-searched learning, one worker. Aspiration, futility, reverse futility,
null move and extensions are OFF. Construction-v1 is not mixed into this qualified
composition. Optional exact vector inference depends on the target JVM; scalar
fallback was tested here, while the M1's actual backend remains unmeasured.

No native M1 execution, installed Mac BotLi configuration inspection, full test
suite, new training or tournament was performed. Historical Mac Java21 ARM64
packaging evidence does not qualify this candidate. The native offline verifier
must pass on arm64 with the actual Java21 runtime; inspect identity, scalar/vector
agreement, memory, latency, permissions and clean shutdown before activation.

Human actions required after this prompt:

1. **Blocking Mac synchronization:** owner runs `git push origin main` on Windows,
   then preserves any Mac local work and pulls main with `--ff-only`.
2. **Blocking live acceptance:** build on the Mac and pass the guide's offline
   native gate. Confirm the installed BotLi engine selector/options, one thread,
   actual hash allocation, ponder false and practical concurrency/memory; retain
   the old executable and private config backup.
3. **Blocking live activation:** explicit owner authorization and controlled BotLi
   activation after native acceptance. Preserve books, matchmaking and time-control
   behavior; rollback restores the previous selector/options and old executable.
