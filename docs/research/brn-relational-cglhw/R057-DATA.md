Completed continuation, 29 October 2026: the supplement and logical corpus now
pass independent validation. All47,588 new records match upstream packing and
independently inverted labels, with no original/reserved/new alias collisions.
The final logical training corpus has33,554,432 samples and33,180,466 canonical
unique positions (373,966 duplicate samples). r057-supplement-audit.json and
r057-training-corpus.json bind exact identities. The plan/status language below
records the preceding resource-stop boundary and is now historical.

# R057 original-data and separation audit

The exact CP64 E006 training data, aligned provenance, original validation,
source descriptors and prepared BT4 chunks are available. Full-file hashes and
all33,554,432 stored labels/results/provenance ordinals passed the existing
independent verifier. All66,560 distributed proof records pack identically using
the upstream converter. This reuses the tested BINP decoder; it is not a second
independent decoding of6.67billion source positions. Evidence:
r057-data-discovery.json and raw/r057/data-integrity-retry/result.json.

The selected E007 CP64 endpoint has268,435,456 training exposures:
64superbatches*1024batches*4096examples. Its first4superbatches were preserved
control-validation training; the5..64 config resumes that optimizer. This is
eight corpus-equivalent passes through33,554,432 samples, not268million distinct
positions. CP64 has1,970,440 exported parameters; factorized training parameters
and optimizer methodology also differ from ONE and must be accounted for.

Original targets are BT4_Q_LOGIT400_V1: invert publisher encoded Q, map to
`p=clamp((Q+1)/2,.0001,.9999)`, round `400*logit(p)`, normalize side to move.
Bullet trains sigmoid squared error with WDL mixture0. CP64 teacher imitation
is not needed for this experiment. The original record's result is preserved
but does not become a newly invented outcome objective. Link units are not
original engine centipawns. Recorded maximum encoding probability error is
.00031125306735640956 over the label range.

Original selection shuffled prepared chunk indices, consumed validation chunks
first, then selected bounded qualifying prefixes: quiet, noncheck and ply>=16,
with exact placement/STM dedup. There are no game identifiers. Its sampled chunk
distribution is not uniform over the whole source; validation units are disjoint
but game/symmetry independence was not established. Material/ply/Q distribution
counts are retained in r057-data-discovery.json, not replaced by approximate
opening/middlegame/endgame labels. About one-third of training samples are
below12 pieces; very early openings were filtered. These facts constrain claims
of broad practical chess coverage.

## New exact canonical audit

The source32-byte Bullet format already normalizes STM to White. An exact24-byte
occupancy/piece key additionally folds horizontal reflection. This groups the
same placement/STM plus colour/rank/file aliases used by earlier BRN reservations;
no hash collision approximation is used. Thirty-two immutable1M-record sorted
shards and a bounded-memory merge check every source ID once and preserve labels.

| Quantity | Actual count |
|---|---:|
| Original samples | 33,554,432 |
| Distinct canonical aliases | 33,164,045 |
| Additional samples sharing a canonical alias | 390,387 |
| Reserved aliases (diagnostics plus full CP64 validation) | 588,429 |
| Original samples excluded by reservations | 39,396 |
| Distinct excluded training aliases | 22,975 |
| Remaining original samples | 33,515,036 |
| Remaining original canonical aliases | 33,141,070 |

Multiplicity distribution is32,880,042 singleton aliases,206,475 doubles,
48,672 triples and28,856 quadruples. This does not invalidate the original exact
physical-placement dedup; the new audit groups additional equivalent inputs.
R049's544game reservations include the old4096 panel, R0168192 and R008 quartets.
Both R049 parents/children, R050 opposite turns and every CP64 validation alias
are now excluded. Completed historical training/diagnostic records are unchanged.

Independent Python checks use a separate square encoder on2,560 frozen source
rows, including512 forced exclusions. They verify Java index identity, full merged
key membership and reservation decisions. Full mask bit counts, multiplicity
totals and original material-bucket counts reconcile exactly. See
[r057-corpus-audit.json](r057-corpus-audit.json). The Python check is sampled;
the Java sort/merge visits all33M rows. Unknown game correlations remain unknown.

## Supplement plan, not yet a training result

r057-supplement-config.json prospectively requests39,396 replacement positions
from unused prepared units of the same source and8,192 fresh validation positions.
Use the original target conversion and filters, validation units first,4,096-row
quotas, and exact exclusion against all original CP64 training, all reservations
and every newly selected alias. New validation is reserved until model selection
is frozen. Units remain disjoint but games cannot be proven independent.

If validated, retained original rows plus replacements will restore exactly
33,554,432 training samples with33,180,466 canonical aliases; this projected count
must be checked against the completed supplement, not assumed. Original training
duplicates are deliberately retained for methodological comparability. This is a
0.1174% replacement of CP64 samples, not a claim of a byte-identical corpus. New
quota/distribution and alias exclusions are explicit comparison differences.

The first data-verification attempt completed hashes/labels but its upstream
converter could not load DLLs. Documented process-local CUDA-library paths fixed
the CPU-only converter; no GPU training occurred. An exclusion-publication path
bug happened after reserved.keys was written. The failed runner/source remain
preserved; the corrected retry independently reconstructed and required exactly
the same key-file hash before publishing its manifest. No old evidence was erased.

No R057 candidate training or playing-strength test has run. Corpus construction,
capacity selection, optimizer/checkpoint tests, full-scale exposure and evaluation
remain pending. No small-data result has been used to waive full-scale training.
