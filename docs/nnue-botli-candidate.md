# SeedV6 checkpoint-64 NNUE / E008 UCI candidate

23 October 2026 (Auckland). This is the dedicated `nnue-cp64-e008-rc1`
candidate for an owner-controlled BotLi trial. General production remains Gen32 /
Policy 4. Native Apple Silicon and the installed BotLi configuration require the
acceptance steps below. This document does not authorize live activation.

## Identity and selection

The executable is `bin/seedv6-nnue-cp64` (`.bat` on Windows). It needs no evaluator
or experimental flags. It loads the embedded, frozen conventional Bullet
checkpoint 64, S6CNU002, 10 mirrored king buckets, 256 units per perspective,
SCReLU and eight material heads. File SHA-256:
`8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b`.
Payload SHA-256: `5da0253642ab33b8e446b5b1c5764750be008cd3dbacc7cd1992a95bf4a881f6`.
The entire header and payload must match before decoding. Missing, incompatible,
altered or different valid-checksum weights fail startup with a nonzero exit and
stderr diagnostic. There is no fallback evaluator. The optional `--network FILE`
accepts only identical bytes, for verification or recovery of the same asset.

Material head is `(piece_count - 2) / 4`, covering all eight heads. Worker/ply-owned
integer accumulators refresh on king bucket/mirror changes; otherwise they apply
placement deltas. SCReLU products are summed in long integers with the frozen
quantization/division order. No material residual, old NNUE or HCE is added.

The launcher enables `jdk.incubator.vector` when installed. The loader uses its
exact SIMD head when the preferred vector geometry is supported, otherwise exact
scalar long inference. `--scalar` explicitly selects the scalar diagnostic control.
The runtime `head=vector-...-long` or `head=scalar-long` field is authoritative;
M1 speed or vector width is not inferred from Windows measurements. The normal
JVM incubator warning goes to stderr, never the UCI stream.

On `uci`, `info string` reports evaluator, checkpoint, file/payload hashes, head
backend, search profile, settings, application build and embedded source revision.
Send `uci` again after resource changes to inspect the effective identity; setting
options itself stays quiet for BotLi/python-chess. Every source distribution includes
`network-manifest.json`; the optional archive-freezing step also adds
`build-manifest.json` and `SHA256SUMS`. The source and runtime network are pinned
by the tracked release inputs; no training store is consulted.

## Search configuration and qualification boundary

The dedicated factory exactly preserves E008/E009's **recursive PVS** composition:

| Feature | Candidate setting / qualification |
| --- | --- |
| Quiescence / SEE | QXRAY with the existing check, promotion, en-passant and clearance safeguards |
| Quiet reductions | QUIET LMR and nominal verification |
| Quiet pruning | Protected narrow-window WINDOW LMP |
| Iteration guidance | EFFORT and completed PV reuse |
| TT guidance | Completed legal hints at the same key; ordinary proof restrictions retained |
| Main history | Invocation lifetime, all searched observations; no retained-history/nominal-only alteration |
| Execution | Single worker, recursive PVS, iterative deepening, full windows; aspiration OFF |
| Other pruning/extensions | Futility, reverse futility, null move, IID/IIR and selective extensions OFF |
| Resources | Threads default/min/max **1**; Hash default **64 MiB**, supports 1..1024 requested MiB |
| Optional repertoire | `UseBook` / `OwnBook` aliases, default false; BotLi books remain external |

The strength qualification used one worker and 32 MiB requested hash, not 256 MiB.
The 256 MiB BotLi setting receives lifecycle/memory tests, not a new strength
claim. TTable rounds capacity down to a power of two: 256 MiB requested allocates
192 MiB of entry arrays, plus locks/objects. Heap limit is 768 MiB; the launcher
sets `ActiveProcessorCount=2` to bound JVM auxiliary parallelism, not two search
workers. No Ponder, MultiPV, Chess960, SyzygyPath or evaluator-selection UCI option
is advertised. Pondering must remain disabled in BotLi.

`construction-v1` is implemented and technically verified with **Gen32**, has
18 flat-PVS controls (including richer histories/order, evaluation trend, reduction
relief, shallow pruning and capped extensions), and remains separately unaccepted
by the Search Phase 3 programme. Its gameplay launcher defaults aspiration OFF;
the frozen construction study used WIDEN16. Its combination with this NNUE is
unqualified. More enabled controls do not establish greater strength. The E008
composition has direct independent complete-engine NNUE evidence, so this release
does not combine or promote construction-v1. Ordinary `seedv6-uci --nnue` remains
the unpruned research control and is not this candidate.

Starting evidence was verified at source `859a619`, build 64: E008 84W/26D/18L
at 200 ms, E009 86W/33D/9L at 1000 ms, each 64 fresh opening pairs / one worker /
32 MiB. Frozen runtime source is `6213630`; the later handoff commit does not
change those jars. The prior cross-language parity and independent selective
search evidence remain under `docs/research/conventional-nnue-cglhw/`. No training
or tournament is repeated. These results are platform/clock/sample specific,
not a Lichess rating estimate or owner acceptance.

## Normal Windows-to-Mac source workflow

### Native Mac package with bundled Java

On an Apple Silicon Mac with a supported Java 21 packaging JDK:

```sh
./gradlew :app-uci:packageMac
```

The reported `releases/candidates/macos-arm64/<candidate>/SeedV6-UCI/` image
contains two executables. `SeedV6-UCI` retains the Gen32 production default;
`SeedV6-NNUE-CP64` runs this pinned checkpoint-64 / E008 candidate without extra
arguments. Keep the complete image together. The NNUE launcher uses only its
bundled runtime and explicitly resolves `jdk.incubator.vector`; no system Java,
external weight file or checkout path is needed. The runtime contains exactly
`java.base` and `jdk.incubator.vector`. `--scalar` remains available.

Packaging verifies both executables and the independently extracted ZIP with an
empty PATH and unrelated working directory. The NNUE checks cover the full
checkpoint/search identity, legal short searches, equal normal/scalar/module-absent
results, and clean quit/EOF. Windows native packaging uses the same module policy
and adds `SeedV6-NNUE-CP64.exe` beside `SeedV6-UCI.exe`.

This is local packaging validation, not live BotLi activation or a new strength
qualification. The Java source distribution below is unchanged.

### Source distribution using installed Java

The release implementation and evidence are integrated into `main`. The original
release branch and frozen build-65 archive remain preserved. See
[integration verification](nnue-main-integration.md) for the tested commit and
clean-checkout evidence. Nothing is pushed automatically.

After reviewing the completed Windows work, the owner pushes normally:

```powershell
git push origin main
```

On the Mac, enter the existing SeedV6 repository. Check for local work before
switching or pulling; preserve it and resolve any divergence without reset/rebase.
Use the established Java 21 Gradle portable-UCI convention:

```sh
git status --short
git switch main
git pull --ff-only origin main
git log -1 --format='%H %s'
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
"$JAVA_HOME/bin/java" -version
./gradlew --gradle-user-home build/migration/gradle-user-home --no-daemon --max-workers=1 :app-uci:installNnueCandidateDist :app-uci:nnueCandidateDistTar
RC="$(pwd -P)/app-uci/build/install/seedv6-nnue-cp64-e008-rc1"
test -x "$RC/bin/seedv6-nnue-cp64"
```

The exact executable is **`$RC/bin/seedv6-nnue-cp64`**. Keep the complete
distribution directory together. This task builds only the dedicated UCI target;
it does not change the ordinary Gen32 target or install/activate BotLi. Gradle and
Maven downloads may be needed on a fresh machine; Java 21 must already be installed.
On Windows use `gradlew.bat`; local substantial validation uses
`nnue-control.cmd run` where that worktree's compute policy applies.

The sole required inference payload is tracked at
`build-assets/nnue-candidate/conventional-cp64.cnnue` (3,940,952 bytes), alongside
its versioned manifest. Gradle embeds it as `models/conventional-cp64.cnnue` in
`lib/app-uci.jar`. There is no Git LFS, NAS, ignored research file, Windows archive,
or manual post-build network-copy dependency. The distribution has two runtime
jars, launchers, network manifest, this guide and the offline verifier. It has no
training data, optimizer state, GPU tools, GUI or training-service dependencies.

For a stable BotLi trial location, copy the **whole built distribution** into a
fresh directory after building, then run native acceptance against that copy:

```sh
mkdir -p "$HOME/SeedV6-candidates"
# Pick a new name if this directory already exists. Do not replace the old engine.
DEST="$HOME/SeedV6-candidates/nnue-cp64-e008-rc1-build65-$(git rev-parse --short=12 HEAD)"
mkdir "$DEST"
cp -R "$RC/." "$DEST/"
RC="$DEST"
```

This optional local copy avoids later source builds replacing a running candidate.
It is not a separate weight transfer. Alternatively, `python3 -B
tools/package_nnue_candidate.py` freezes a fresh clean-commit build into a new
non-overwriting directory and `.tar.gz` under `releases/candidates/portable-java21/`,
with build manifest and checksums. Freezing or transferring an archive is **not
required** for the normal push/pull/build workflow.

### Network provenance and notices

This is SeedV6's freshly initialized E007 model, trained with the E006 sample and
exported at checkpoint 64; it is not a copy of a teacher network. Training data
was obtained from [xushawn/test80-bt4-relabel](https://huggingface.co/datasets/xushawn/test80-bt4-relabel),
which the publisher makes available under the
[Open Database License (ODbL) 1.0](https://opendatacommons.org/licenses/odbl/1-0/).
Retain this source attribution with the candidate. E005/E006/E007 records and
their tracked manifests preserve sampling, transformations and training provenance;
the dataset and intermediate checkpoints are not distributed here. The Bullet
trainer's MIT notice is retained at
`tools/conventional-nnue/trainer/UPSTREAM-NOTICE.txt`; the trainer is not a UCI
runtime dependency. These notices do not assign a new license to SeedV6 code or
the frozen weights, or claim that the source data is CC0.

## Native Mac acceptance (required before live trial)

Use `RC` from the source build (or complete local copy) above. No network transfer
is required. For an optional frozen archive, first verify its recorded SHA-256,
extract into a fresh directory, set `RC` to its application directory, and run
`shasum -a 256 -c SHA256SUMS` there. Ordinary Gradle installs do not contain that
additional checksum file. In either case:

```sh
test -x "$RC/bin/seedv6-nnue-cp64"
uname -m
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
"$JAVA_HOME/bin/java" -version
file "$JAVA_HOME/bin/java" "$JAVA_HOME/lib/server/libjvm.dylib"
"$JAVA_HOME/bin/java" --list-modules
printf 'uci\nisready\nquit\n' | "$RC/bin/seedv6-nnue-cp64"
```

The UCI identity must show `checkpoint=64 schema=S6CNU002 heads=8`, the file hash
above, `search=E008_QXRAY_LMR_WINDOW_EFFORT_HINTS`, `execution=recursive-pvs`,
`workers=1`, build 65 and the source commit built on the Mac. Expect `uciok` and
`readyok`; record the actual `head=` backend. The full offline gate below also
checks the embedded network bytes independently of that identity string.

Require native arm64/aarch64 Java 21 (not Rosetta), and retain those outputs.
Historical Mac packaging passed on Temurin 21.0.12.1+1 / arm64 / macOS 26.4;
that is evidence for the packaging convention, not this candidate's execution.
This portable distribution **does not bundle a JRE**. Java 21 is the sole engine
runtime prerequisite; no JNI library is required on this route. The launcher
uses an optional `runtime/bin/java`, otherwise `JAVA_HOME`, then the Mac's Java 21
discovery. All assets resolve relative to the launcher/jars, independent of cwd.
The existing complete native Gen32 distribution remains a separate fallback.

Run the bundled offline gate from an unrelated working directory with the Mac's
BotLi Python environment (which already has python-chess and psutil). Do not start
BotLi's user interface, which connects and accepts challenges. If using a separate
validation venv, install `chess==1.11.2 psutil==7.2.2` there. Python 3.9+ suffices
for this verifier; current upstream BotLi itself requires Python 3.11+.

```sh
cd /tmp
# Set this to the existing BotLi venv's Python executable; do not copy config.yml.
BOTLI_PYTHON=/absolute/path/to/BotLi/.venv/bin/python
"$BOTLI_PYTHON" -B "$RC/validation/verify_nnue_candidate.py" \
  --install "$RC" --out "$HOME/SeedV6-candidates/native-check-01"
```

Use a fresh evidence directory each run. The script relocates a copy with spaces,
checks exact network identity, scalar/vector equality, a java.base-only fallback,
depth/movetime/clocks/increments, long move history, special/terminal positions,
stop/readiness/reset/reuse/quit/EOF, negative assets, legal moves/PVs, process
termination, RSS and BotLi's `chess.engine.popen_uci` launch API without network
access. Inspect `result.json`: status must be `passed`, `native_mac=true`, and
machine arm64. Record the actual `head=` field, startup/search/stop times and RSS.
Observe Activity Monitor memory pressure during the bounded test; no sustained
thermal or multi-game concurrency capacity is certified by short validation.

Native scalar/vector depth-5 scores/nodes/PVs must agree. A vector backend label
alone is not a speed measurement; compare recorded times after warmup on the M1
before attributing any SIMD advantage. A module-absent runtime must identify
`scalar-long` and pass. Stop and retain evidence if any gate fails.

## BotLi configuration review and later activation

Upstream was inspected at `6aa64359abb629039ba455605734fd11a2639432`:
[README](https://github.com/Torom/BotLi/blob/6aa64359abb629039ba455605734fd11a2639432/README.md),
[config.py](https://github.com/Torom/BotLi/blob/6aa64359abb629039ba455605734fd11a2639432/config.py),
[engine.py](https://github.com/Torom/BotLi/blob/6aa64359abb629039ba455605734fd11a2639432/engine.py).
Current configuration uses `engines`, potentially with per-clock/color overrides.
It checks `dir/name` existence/executable permission and launches the resulting
path directly through python-chess. Unsupported UCI options are ignored with a
message; this candidate's exact advertised names are `Threads`, `Hash`,
`Clear Hash`, `UseBook`, `OwnBook`.

The installed BotLi version, selected executable(s), current Threads/Hash, ponder,
concurrency and books have **not** been inspected here. On the Mac, inspect only
the necessary engine fields locally; do not print/copy/share the API token or the
whole config. Preserve all opening books, online move sources, time controls,
matchmaking, account and concurrency settings. If concurrency exceeds practical
memory, resolve that before live activation; do not silently change it.

After native acceptance and **explicit owner authorization to activate**, save a
private local backup of the existing config and retain the complete old engine
directory. Change only the appropriate existing engine selector(s), following
the installed version's schema. For current upstream the relevant values are:

```yaml
engines:
  standard:  # Use the actual existing selector; preserve other engine keys.
    dir: "/absolute/path/to/seedv6-nnue-cp64-e008-rc1/bin"
    name: "seedv6-nnue-cp64"
    ponder: false
    uci_options:
      Threads: 1
      Hash: 256
      UseBook: false
```

This is a field example, **not a replacement config**. Keep other required fields
and preserve the existing engine-book choice if intentionally enabled; the
offline validation uses false to ensure search is exercised. Preserve 256 MiB
only after verifying the installed setting and native memory results. Do not add
`Ponder` to `uci_options`; BotLi owns pondering. Do not add unsupported Syzygy,
MultiPV, evaluator or experimental options. No command arguments are required.

When authorized, let current games finish and use the existing safe BotLi stop/
start procedure. Starting BotLi can accept live challenges even without issuing
`matchmaking`; therefore even that start remains at the deployment boundary.
Rollback restores the previous `dir/name` and saved engine options and restarts
using the same safe procedure. The old executable and its dependencies must remain
untouched. No live configuration change, executable replacement, bot start,
matchmaking, remote push or Lichess connection is part of candidate preparation.
