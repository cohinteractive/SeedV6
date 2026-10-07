# Retained final orchestration sources

These are byte-identical copies of the scripts used from the ignored research
directory. Original files remain at app/build/research/brn-architecture/ and are
the paths bound by the immutable plans/receipts. These copies preserve reviewable
source with the research record; they are not a new allocation or maintained
production entrypoint. Do not rerun scripts against existing exclusive outputs.

Reproduction needs the hashed data/models/raw artifacts at their recorded paths.
On a new checkout, restore those artifacts and these exact scripts to the original
paths without overwriting existing evidence. For a new experiment, first create a
new prospective plan/output allocation rather than silently replaying this one.
The final reconciliation script intentionally requires the original pre-commit
HEAD and writes a new exclusive receipt; its retained source explains the audited
historical check and is not an instruction to reset Git.

| Script | SHA-256 |
|---|---|
| [run-frozen-plan.py](run-frozen-plan.py) | 2bfdad9b56f31d2c3cb947e4a7d16ea95ceddfa3e52aaccb72a8d5ac138cd423 |
| [crosscheck-final-matches.py](crosscheck-final-matches.py) | 0e0cc3b6cd85511671e295a81f196436cb3961157f9241b63c553b6c5985aaec |
| [run-final-quality.py](run-final-quality.py) | e896223c0a2d7741be5adaeeec551dbc2ff64674aec2492d6d790ef7aa735aa2 |
| [record-final-results.py](record-final-results.py) | 397c6b52aaf3ec2dce281e134bacee75b6bc0bb849b72cea2e32eb9b296176b6 |
| [reconcile-final-evidence.py](reconcile-final-evidence.py) | 472db901262efe7f3899bbb30f3b0e37261f3e60d1917c5a547fdd0dd084531c |
