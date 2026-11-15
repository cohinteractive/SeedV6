
# Build Version Management

- `VERSION_STATE.txt` at the repository root maintains application version information.
- When committing changes that warrant a new application build, increment `build` by exactly 1 and include the updated file in the same commit.
- Never change `major`, `minor`, or `patch`; the owner manages these manually.
- `build` increases monotonically without limit. Never reset, wrap, or derive it from other version components.
- Do not increment for uncommitted work, investigations, or documentation-only changes that do not affect the application.
- Never create a separate commit solely for a build increment.
- Preserve the existing JSON structure and unrelated values.

# Local Research Evidence

Raw experimental output is local-only evidence excluded from Git through `.gitignore`. Preserve it at its recorded paths, including failed, negative, inconclusive, and frozen experiments. Never delete evidence, rewrite completed protocols, or alter conclusions merely because output is ignored.

Track reports, analytical summaries, configurations, manifests, reproducibility metadata, and intentional reusable fixtures in Git.

- Frozen opening PGNs are inputs with narrow ignore exceptions; tournament PGNs are output.
- Review new fixture exceptions explicitly.
- Store new bulk formats under `raw/` within the experiment directory, keeping tracked reports and manifests outside it.
- Before staging, verify that bulk output is ignored. Never force-add raw output.
- Reports and manifests must identify evidence paths, experiment/configuration identities, results, and limitations.
- Verify local evidence availability before analysis; a fresh checkout will not contain ignored output.

Preserve applicable Search Contract requirements for evidence, reproducibility, frozen experiments, negative results, and independent confirmation.

# Production Search and Evaluation Inventory

`docs/SEARCH_AND_EVALUATION_FEATURES.md` describes Search and Evaluation features active in the normal production engine.

- Update it in the same work unit whenever a production feature is added, removed, enabled, disabled, replaced, or materially changed.
- Verify descriptions against the implementation and effective resolved production configuration.
- Maintain concise, technically accurate descriptions in place. Group related features where useful.
- Exclude changelogs, historical narratives, experiments, planned features, and obsolete descriptions.
- Leave it unchanged for routine refactoring, parameter tuning, or corrections that do not materially affect feature descriptions.
- Experimental work requires an update only when adopted into production.

The implementation and resolved configuration determine actual behavior. `source/CHESS_SEARCH_CONTRACT.md` and `source/CHESS_SEARCH_RESEARCH_FRONTIER.md` govern adopted Search policy. The inventory documents production behavior; it does not determine policy or configuration.

# Public README

Root `README.md` is owner-controlled public documentation.

Codex may modify it only when:

1. The owner explicitly requests a README update; or
2. A feature is being promoted into the normal production baseline, and the README needs updating to reflect that promotion.

For promotion-driven updates:

- Document only the promoted feature and its resulting production behavior.
- Changes may span multiple README sections where necessary for accuracy and consistency.
- Update or remove existing descriptions made inaccurate by the promotion.
- Do not perform unrelated rewrites, cleanup, restructuring, or documentation updates.
- Verify that the feature is actually adopted into the production baseline, not merely experimental or proposed.
- Leave the README unchanged if the promotion requires no public documentation change.

Outside these permissions, do not modify `README.md` during routine development, research, refactoring, testing, benchmarking, releases, or documentation maintenance.

Maintain other technical documentation in its established locations.
