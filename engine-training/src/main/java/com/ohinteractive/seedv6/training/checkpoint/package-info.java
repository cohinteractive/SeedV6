/**
 * Atomic NNUE checkpoint publication and logarithmic payload retention.
 *
 * <p>Identity remains the immutable V1 manifest, including parent, generation, optimizer step,
 * training depth and both payload hashes. Validation, promotion and analytics records are never
 * pruned. New publications also contain a checksummed {@code training-metadata.bin} with the four
 * Adam hyperparameters, bound to checkpoint identity/step. Legacy payloads supply this sidecar
 * only after full independent validation, immediately before their first retirement.
 *
 * <p>After {@code CandidateLifecycle.completeDecision} has durably settled validation and any
 * promotion, retention keeps ages 0..99, then generation multiples of 10 for ages 100..999,
 * multiples of 100 for ages 1000..9999, and so on. Current Best, its immediately previous accepted
 * Best, latest training and unresolved/pending dependencies bypass sampling. Earlier Bests and
 * bootstrap have no permanent exemption. Only decided checkpoints in the completed lineage qualify.
 *
 * <p>Before deleting either large file, maintenance forces/publishes the configuration and then
 * atomically publishes {@code payload-pruned.bin}, bound to the manifest identity and configuration
 * hash. From that point the generation is explicitly not resumable, even if a crash leaves either
 * payload behind. Later boundaries retry deletion of matching leftover files. An absent/invalid
 * marker never excuses missing payloads. Metadata records, unknown files and stages stay intact.
 * Directory-force limitations (notably Windows) are the same as publication: corruption is detected,
 * but universal power-loss durability is not promised.
 *
 * <p>{@code store.lock} still grants one lifetime trainer/writer owner, including validation.
 * The separate {@code payload.lock} gives short-lived OS-exclusive ownership to payload readers
 * and pruning across processes, with reentrant JVM coordination to avoid overlapping OS locks.
 * Readers intentionally serialize rather than depend on provider-specific shared-lock support.
 * Best reference selection and payload loading share that ownership. Loaded immutable arrays need
 * no filesystem lease during play. A read of a legacy store may create this empty coordination file;
 * readers need permission to open it for writing. Older SeedV6 binaries and manual filesystem tools
 * do not participate in this protocol and must not access a store concurrently with retention.
 *
 * <p>Best ancestry uses checksummed manifest/validation/promotion identities and ordered links;
 * it no longer decodes ancestors. Explicit materialized loads still check both hashes/codecs/model
 * equality. Historical inspection fully validates materialized payloads, or validates the compact
 * pruning/configuration records. Research measurements requiring a pruned network are unavailable.
 * Maintenance scans lightweight lineage/evidence and attempts at most eight payload retirements
 * per completion. Failures are reported on stderr and do not invalidate the generation. No work
 * occurs in search, evaluation, move generation or a background maintenance service.
 */
package com.ohinteractive.seedv6.training.checkpoint;
