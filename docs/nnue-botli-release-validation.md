# NNUE UCI release candidate handoff — build 65

23 October 2026 (Auckland). **Windows offline release gates passed. Native Mac
acceptance remains pending.** No BotLi configuration, existing executable, Lichess
connection, matchmaking, remote repository or production default was changed.

## Delivered artifact

Implementation commit: `4fa0f9f984ad3822ba5260e1a431374cdc1ac262`, build **65**,
on `release/nnue-cp64-e008-rc1`. The artifact was built from that clean commit.
The subsequent validation-document commit does not change its build or bytes.
Local `main` remains at the starting `859a619`; nothing was pushed.

Directory, relative to the repository root:

```text
releases/candidates/portable-java21/seedv6-nnue-cp64-e008-rc1-build65-4fa0f9f984ad/
```

Transfer archive:
`seedv6-nnue-cp64-e008-rc1-build65-4fa0f9f984ad.tar.gz`.
SHA-256: `12622ab91395a0c85e851ee3e853e251eefb0c952d3cfc672bf17e4d05947bf2`.

The contained application directory is `seedv6-nnue-cp64-e008-rc1`, executable
`bin/seedv6-nnue-cp64`. Its network is embedded in `lib/app-uci.jar`; the only
other runtime jar is `lib/engine-core.jar`. Java **21 is required and not bundled**.
Keep the entire directory together. The TAR preserves POSIX executable mode 0755;
all extracted bytes and the internal `SHA256SUMS` were independently checked.
The source-tree development install is at
`app-uci/build/install/seedv6-nnue-cp64-e008-rc1`, but transfer the frozen archive.

## Verified configuration

The exact frozen conventional Bullet **checkpoint 64**, S6CNU002, eight material
heads, 256 hidden units per perspective, SCReLU, full-file SHA-256
`8431626c2fca81e0a1e9ccd5e45ddd8829e6e3282c756c21fad75b06d075bb2b` is active.
No evaluator flags are needed. Missing/corrupt/incompatible/different valid-checksum
assets fail before `uciok`; no other evaluator takes over.

The active profile is **E008_QXRAY_LMR_WINDOW_EFFORT_HINTS**, recursive PVS,
QUIET LMR, WINDOW LMP, QXRAY quiescence, EFFORT/PV reuse, completed same-key legal
TT hints, invocation history and all-searched learning. Aspiration, null move,
futility, reverse futility and selective extensions remain off. Threads is fixed
at **1**. Requested Hash defaults to **64 MiB**; 32 and 256 MiB were also tested.
UseBook/OwnBook are optional aliases, default false. BotLi must keep pondering off.

This preserves the composition directly qualified with the NNUE. Construction-v1
is a distinct, richer flattened Gen32 configuration, technically verified but
not accepted as champion or qualified with this NNUE. It was not combined with
the candidate. The [full guide](nnue-botli-candidate.md) records the feature matrix,
source-to-runtime path, exact options and acceptance boundaries.

`uci` reports actual checkpoint, hashes, head backend, search settings, build and
source. Query `uci` again after changing Hash; option changes themselves are quiet
for python-chess. Windows used **vector-256-long**. Exact scalar inference passed
with `--scalar`, with only java.base resolved, and through both packaged launchers
using a physically module-absent jlink runtime. **M1 backend and speed are unknown.**

## Evidence and results

The [compact evidence manifest](nnue-botli-release-validation.json) records hashes,
commands/log paths, suite counts, measurements, corrections and limitations.
Raw output remains local/ignored under `build/nnue-release/` and is not available
in a fresh checkout. The prior CGLHW evidence and checkpoint 256 remain untouched.

| Gate | Result |
| --- | --- |
| Qualification provenance | Frozen network/runtime/launcher hashes and 12 handoff evidence hashes match. All 212 final engine-core class payloads match the qualified build-64 core exactly. |
| Java tests | 19 UCI + 13 conventional NNUE tests pass in scalar and vector modes; 19 UCI tests rerun against clean build 65. Zero failures/skips in these selections. |
| Real weights / all heads | 259 independent Python integer reference positions match packaged incremental and full-rebuild inference, covering all eight heads. |
| Selective integration | Five depth-5 candidate searches match independent full-rebuild scores, moves, nodes, qnodes, PV and selective status. |
| Frozen executable equivalence | Four actual packaged depth-5 searches match frozen E008 runtime depth, score, nodes, PV and bestmove; scalar/vector outputs also match. |
| Actual archive UCI | 44 candidate searches plus four frozen-reference searches; handshake, readiness, options, startpos/FEN, 160-ply input, legal PV/moves, special/terminal positions, clocks/increments, movetime, stop, reset/reuse, quit and EOF pass. |
| BotLi-compatible client | Current upstream executable-selection mechanism inspected; python-chess 1.11.2 popen/configure/play/quit passes 16 plies over two game identities, Threads=1, Hash=256, ponder=false, with no unsolicited configuration output. No BotLi/Lichess service started. |
| Asset failures / independence | Missing/truncated/corrupt/incompatible/different-checksummed assets rejected. Relocated paths with spaces and unrelated cwd pass. No controller variable, training service, training store, GUI module or JNI library required; no client-directory writes or jar mutation. |
| Shutdown | Stop approximately 0.59 ms in this sample; quit/EOF during search terminate cleanly with no owned process remaining. |
| Launchers | Frozen Windows launcher and actual POSIX launcher under Windows Git Bash pass, including automatic scalar selection without the vector module. Not native Mac validation. |
| Baseline preservation | General UCI still selects verified Gen32/Policy 4 with its established baseline asset; production factories/core class bytes unchanged. |

Host: Windows 11 AMD64, Ryzen 5 5500, approximately 32 GiB RAM, OpenJDK
21+35-2513. The one-worker 256-MiB-hash process tree peaked at **444.5 MiB RSS**.
Twenty-four requested 150 ms searches returned in **140.5–149.9 ms**. The two
1000 ms searches returned in **990.7 / 997.3 ms**; last completed-iteration NPS
was approximately **439k / 571k** on those two positions. Startup handshake samples
were **491–796 ms**, including Java process/module probing. These short measurements
include JIT/state effects and establish neither M1 performance nor an Elo estimate.

Initial checks exposed and corrected unsolicited resource-identity output for
python-chess and Git Bash classpath conversion. Failed/incomplete-check evidence
was retained; final checks use the corrected frozen archive. No search algorithm
or trained weights changed. No broad legacy/GUI suite, new strength tournament,
native Mac execution, installed BotLi configuration, sustained M1 thermal test,
concurrent-game capacity test or live Lichess trial was performed.

## Exact remaining Mac steps

Copy the archive named above to the Mac's `~/Downloads` using the owner's usual
transfer method. This avoids reliance on whether the Mac repository has received
the Windows commits. Then run locally:

```sh
ARCHIVE="$HOME/Downloads/seedv6-nnue-cp64-e008-rc1-build65-4fa0f9f984ad.tar.gz"
printf '%s  %s\n' 12622ab91395a0c85e851ee3e853e251eefb0c952d3cfc672bf17e4d05947bf2 "$ARCHIVE" | shasum -a 256 -c -
mkdir -p "$HOME/SeedV6-candidates"
mkdir "$HOME/SeedV6-candidates/nnue-cp64-e008-rc1"
tar -xzf "$ARCHIVE" -C "$HOME/SeedV6-candidates/nnue-cp64-e008-rc1"
RC="$HOME/SeedV6-candidates/nnue-cp64-e008-rc1/seedv6-nnue-cp64-e008-rc1"
cd "$RC"
shasum -a 256 -c SHA256SUMS
test -x bin/seedv6-nnue-cp64
uname -m
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
"$JAVA_HOME/bin/java" -version
file "$JAVA_HOME/bin/java" "$JAVA_HOME/lib/server/libjvm.dylib"
"$JAVA_HOME/bin/java" --list-modules
cd /tmp
# Replace only this path with the installed BotLi Python path; do not start BotLi.
BOTLI_PYTHON=/absolute/path/to/BotLi/.venv/bin/python
"$BOTLI_PYTHON" -B "$RC/validation/verify_nnue_candidate.py" \
  --install "$RC" --out "$HOME/SeedV6-candidates/native-check-01"
```

Stop if the destination already exists or any checksum/runtime check fails.
Require native arm64/aarch64 Java 21 and `result.json` with `status=passed` and
`native_mac=true`. Inspect actual head backend, timings, RSS and Activity Monitor
memory pressure. Existing native Mac packaging was historically verified with
Temurin 21.0.12.1+1; it does not certify this archive. No Mac access was attempted,
as the owner selected a native-Mac handoff.

The installed BotLi path/version and current engine configuration are unknown.
Read only its relevant engine fields locally, without sharing the token or full
config. Confirm actual selected `dir/name` (including per-clock/color overrides),
Threads=1, intended Hash, ponder=false, books and concurrency. Follow the
[activation/rollback section](nnue-botli-candidate.md#botli-configuration-review-and-later-activation)
only after acceptance and explicit owner authorization. Preserve the complete old
engine and a private config backup. Starting BotLi itself may accept live games;
it is outside this handoff, even without a matchmaking command.

Human actions required after this prompt:

- **Blocking Mac acceptance/live trial:** transfer and run the native gate above;
  retain and reconcile the resulting platform/runtime/performance evidence.
- **Blocking activation:** verify the installed BotLi settings and memory fit,
  preserving current book/time-control/concurrency behavior and rollback files.
- **Blocking deployment/Lichess connection:** explicitly authorize candidate
  activation after those checks. No activation is inferred from this handoff.
- **Non-blocking:** no additional action required for the completed Windows
  preparation. Git changes are local; publication is optional and not performed.
