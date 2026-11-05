## Build Version Management

Owner instruction, 2026-10-19: this repository has no finalizer dependency. These
rules supersede obsolete shared finalizer invocation, reservation, validation and
completion requirements for this repository. Do not invoke, recreate, reinstall
or replace a finalizer for version management, validation, commits or other work.
Retained finalizer metadata and historical reports are evidence, not operational
gates; preserve them without requiring reconciliation before new work.

- The repository-root `VERSION_STATE.txt` file maintains the application's version information.
- When Codex commits changes that warrant a new application build, increment the `build` value by exactly 1.
- Include the updated `VERSION_STATE.txt` in the same commit as the qualifying changes.
- Never modify `major`, `minor`, or `patch`. These values are managed manually by the user.
- The `build` value increases monotonically without an upper limit. Never reset, wrap, or derive it from the other version components.
- Do not increment the build number for uncommitted work, investigations, or documentation-only changes that do not affect the application.
- Do not create a separate commit solely to increment the build number.
- Preserve the existing JSON structure and all other values.

# Local research evidence

Raw experimental output is local-only evidence under `.gitignore`, not a Git
deliverable. Preserve it at its recorded paths, including failed, negative,
inconclusive and frozen experiments. Git exclusion never authorizes deletion,
rewriting completed protocols or changing research conclusions.

Keep reports, compact analytical summaries, configurations, run/selection/evidence
manifests, reproducibility metadata and intentional reusable fixtures in Git.
Frozen opening PGNs are inputs and have narrow ignore exceptions; tournament PGNs
are output. Review new fixture exceptions explicitly. Do not force-add raw output.
For new bulk formats not covered by the named patterns, use `raw/` inside the
existing experiment directory and keep its tracked manifest/report outside it.

Before staging research work, inspect the exact paths and verify bulk output is
ignored. Tracked reports/manifests must identify local evidence paths, experiment
and configuration identities, results and limitations. Check local availability
before analysis: a fresh checkout does not contain ignored evidence. This storage
policy preserves all Search Contract evidence, reproducibility, frozen-experiment,
negative-result and independent-confirmation requirements.

## Production Search and Evaluation feature inventory

`docs/SEARCH_AND_EVALUATION_FEATURES.md` is a concise, human-readable snapshot of
the Search and Evaluation features actually active in the normal production
SeedV6 engine. It describes what production currently does, not how it evolved.

- Whenever implementation or configuration changes add, remove, enable, disable,
  replace or materially alter a production Search or Evaluation feature, Codex
  must update the inventory as part of the same work unit. Revise descriptions
  when behavior changes, add newly adopted production features, and remove
  features no longer used in normal production.
- Maintain descriptions in place. Related features may be grouped for readability;
  descriptions must remain concise, technically accurate and understandable to
  chess-engine developers. Do not append changelogs, historical entries, dated
  updates, research narratives or obsolete descriptions.
- Routine refactoring, parameter tuning and implementation corrections require
  no inventory change unless they materially affect a feature description.
  Experimental changes require no update until adopted into the normal production
  formulation. If no inventory update is needed, leave the file unchanged.
- For relevant production feature changes, verify the inventory against the actual
  implementation and effective resolved production configuration before completing
  the work unit.

This is repository product documentation, not a competing research authority or
production configuration selector. Actual implementation and resolved production
configuration establish feature behavior; the applicable Search canon,
`source/CHESS_SEARCH_CONTRACT.md` and `source/CHESS_SEARCH_RESEARCH_FRONTIER.md`,
establishes adopted production policy. Historical implementations, research results,
experimental candidates or configurations, and planned features do not belong in
this inventory.
