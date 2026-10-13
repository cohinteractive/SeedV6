# Retained final orchestration sources

Availability note (2026-10-13): the owner intentionally emptied
`app/build/research/`, including the original models, continuation states, datasets,
raw outputs and original orchestration copies. The older
[retention inventory](../../brn/BRN_ARTIFACT_RETENTION.md) is historical, not a
requirement to restore them. Exact replay may no longer be possible.

These byte-identical source copies preserve the original methodology and receipt
bindings. They are historical evidence, excluded from application source sets and
not maintained executable entry points. Do not rerun them against existing outputs
or substitute current production lineages for their deleted frozen inputs. A new
experiment needs its own authorized inputs and output allocation. The final
reconciliation script's original pre-commit HEAD is provenance, not an instruction
to reset Git. The source bytes and recorded hashes below remain unchanged.

| Script | SHA-256 |
|---|---|
| [run-frozen-plan.py](run-frozen-plan.py) | 2bfdad9b56f31d2c3cb947e4a7d16ea95ceddfa3e52aaccb72a8d5ac138cd423 |
| [crosscheck-final-matches.py](crosscheck-final-matches.py) | 0e0cc3b6cd85511671e295a81f196436cb3961157f9241b63c553b6c5985aaec |
| [run-final-quality.py](run-final-quality.py) | e896223c0a2d7741be5adaeeec551dbc2ff64674aec2492d6d790ef7aa735aa2 |
| [record-final-results.py](record-final-results.py) | 397c6b52aaf3ec2dce281e134bacee75b6bc0bb849b72cea2e32eb9b296176b6 |
| [reconcile-final-evidence.py](reconcile-final-evidence.py) | 472db901262efe7f3899bbb30f3b0e37261f3e60d1917c5a547fdd0dd084531c |
