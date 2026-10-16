package com.ohinteractive.seedv6.training.checkpoint;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import com.ohinteractive.seedv6.core.Board;
import com.ohinteractive.seedv6.core.nnue.NnueNetwork;
import com.ohinteractive.seedv6.rules.GameHistory;
import com.ohinteractive.seedv6.training.nnue.NnueTrainer;
import com.ohinteractive.seedv6.training.validation.*;

/** Exactly one publish/validate/record/optional-promote operation. The caller decides whether training continues. */
public final class CandidateLifecycle {
    public record Result(CheckpointStore.Checkpoint candidate, ValidationRecord validation,
                         Optional<PromotionRecord> promotion, CheckpointStore.Recovery references) {}

    public static Result run(CheckpointStore store, NnueTrainer trainer, CheckpointManifest.Metadata metadata,
                             ValidationConfig config, PromotionPolicy policy, ValidationControl control) throws IOException {
        long[] board = Board.startingPosition();
        return run(store, trainer, metadata, config, policy, board, GameHistory.initial(board), control);
    }
    public static Result run(CheckpointStore store, NnueTrainer trainer, CheckpointManifest.Metadata metadata,
                             ValidationConfig config, PromotionPolicy policy, long[] board, GameHistory history,
                             ValidationControl control) throws IOException {
        return run(store, trainer, metadata, config, policy, board, history, control,
                (candidate, incumbent) -> new ValidationArena().validate(candidate, incumbent, config, board, history, control));
    }
    @FunctionalInterface interface Match { ValidationResult play(NnueNetwork candidate, NnueNetwork incumbent); }
    static Result run(CheckpointStore store, NnueTrainer trainer, CheckpointManifest.Metadata metadata,
                      ValidationConfig config, PromotionPolicy policy, long[] board, GameHistory history,
                      ValidationControl control, Match match) throws IOException {
        Objects.requireNonNull(config); Objects.requireNonNull(policy); Objects.requireNonNull(control);
        history.requireCurrent(board);
        var incumbent = store.recover().best().orElseThrow(() -> new IOException("Initialize an incumbent before validation."));
        var published = store.publish(trainer, metadata);
        // Both actors are independently decoded from durable artifacts; no live training arrays enter search.
        var candidate = store.load(published.manifest().id());
        incumbent = store.load(incumbent.manifest().id());
        ValidationResult result = match.play(candidate.network(), incumbent.network());
        if (!result.config().equals(config) || !result.startingStateHash().equals(ValidationArena.stateHash(board, history))) {
            throw new IOException("Validation does not match the predeclared experiment.");
        }
        return recordDecision(store, candidate.manifest().id(), incumbent.manifest().id(), result, policy);
    }

    /** Persist validation, then finish the non-interruptible evidence/reference boundary. */
    public static Result recordDecision(CheckpointStore store, String candidateId, String incumbentId,
                                        ValidationResult result, PromotionPolicy policy) throws IOException {
        return completeDecision(store, store.recordValidation(candidateId, incumbentId, result, policy));
    }

    /** Reuses stored assessment/policy on restart; never reruns or duplicates a resolved experiment. */
    public static Result completeDecision(CheckpointStore store, ValidationRecord evidence) throws IOException {
        ValidationRecord record = store.readValidation(evidence.id());
        Optional<PromotionRecord> promotion = Optional.empty();
        if (record.decision() == PromotionPolicy.Decision.PROMOTE) {
            promotion = Optional.of(store.completePromotion(record.id()));
        } else {
            var best = store.recover().best().orElseThrow(() -> new IOException("No accepted incumbent."));
            if (!best.manifest().id().equals(record.incumbentId())) throw new IOException("Stale incumbent validation.");
        }
        var result = new Result(store.load(record.candidateId()), record, promotion, store.recover());
        // Decision/evidence/reference publication has completed under the existing store owner.
        // Maintenance is non-fatal and never changes the successful generation's result.
        CheckpointPruner.prune(store, record.candidateId());
        return result;
    }
    private CandidateLifecycle() {}
}
