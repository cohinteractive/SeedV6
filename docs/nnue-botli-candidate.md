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
options itself stays quiet for BotLi/python-chess. `build-manifest.json`, `network-manifest.json`
and `SHA256SUMS` bind the frozen distribution. The source and runtime network are
also pinned by the tracked release inputs; no training store is consulted.

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

## Build and freeze

Use the established Java 21 Gradle portable-UCI convention, with one build worker:

```sh
./gradlew --gradle-user-home build/migration/gradle-user-home --no-daemon --max-workers=1 :app-uci:installNnueCandidateDist :app-uci:nnueCandidateDistTar :app-uci:nnueCandidateDistZip
python3 -B tools/package_nnue_candidate.py
```

On Windows use `gradlew.bat` and `python`; substantive local validation uses the
existing `nnue-control.cmd run` wrapper where that worktree's compute policy applies.
The freezing tool requires reviewed committed inputs and a fresh rebuild from that
clean commit. It makes a new, non-overwriting directory under
`releases/candidates/portable-java21/`, named for RC, build and source revision.
The archive contains two runtime jars, launchers, network and build manifests,
checksums, this guide and the offline verification script. It contains no training
data, optimizer state, GPU tools, GUI or training runtime dependencies.

## Native Mac acceptance (required before live trial)

Transfer the **complete `.tar.gz`** and its recorded SHA-256 through the owner's
normal file-transfer channel. There is no need to update/reset the Mac repository
or copy a jar from an uncertain checkout. Do not extract over the old bot engine.
Use a new directory under the home directory and substitute the actual delivered
archive path and SHA-256 from the release receipt:

```sh
shasum -a 256 /path/to/delivered-candidate.tar.gz
mkdir -p "$HOME/SeedV6-candidates"
# Choose a fresh subdirectory; mkdir must succeed, without -p.
mkdir "$HOME/SeedV6-candidates/nnue-cp64-e008-rc1"
tar -xzf /path/to/delivered-candidate.tar.gz -C "$HOME/SeedV6-candidates/nnue-cp64-e008-rc1"
RC="$HOME/SeedV6-candidates/nnue-cp64-e008-rc1/seedv6-nnue-cp64-e008-rc1"
cd "$RC"
shasum -a 256 -c SHA256SUMS
test -x "$RC/bin/seedv6-nnue-cp64"
uname -m
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
"$JAVA_HOME/bin/java" -version
file "$JAVA_HOME/bin/java" "$JAVA_HOME/lib/server/libjvm.dylib"
"$JAVA_HOME/bin/java" --list-modules
```

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
