package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.core.util.Value;

/** One EDT-owned workspace publication drives both tab treatment and global activity text. */
record WorkspaceActivity(boolean active, String detail) {
    static WorkspaceActivity idle() { return new WorkspaceActivity(false, ""); }

    static WorkspaceActivity play(boolean searching, GameController.SearchInfo info) {
        if (!searching) return idle();
        return new WorkspaceActivity(true, (info.scoreSide() == Value.WHITE ? "White" : "Black")
                + " searching" + (info.depth() > 0 ? " · Depth " + info.depth() : ""));
    }

    static WorkspaceActivity training(TrainingController.ViewState view) {
        boolean working = view.loading() || switch (view.phase()) {
            case STARTING, RUNNING, STOPPING, CLOSING -> view.active();
            default -> false;
        };
        if (!working) return idle();
        var snapshot = view.snapshot();
        String text = TrainingDashboardModel.phase(view);
        if (snapshot != null) {
            text = "Gen " + snapshot.generation() + " · " + text;
            var game = snapshot.activeGame().orElse(null);
            if (game != null) {
                int total = game.phase() == com.ohinteractive.seedv6.training.telemetry.ActiveGameSnapshot.Phase.VALIDATION
                        ? snapshot.validationProgress().map(p -> p.configuredPairs() * 2).orElse(0)
                        : view.settings().games();
                text += " · Game " + game.gameOrdinal() + (total > 0 ? "/" + total : "");
            }
        }
        return new WorkspaceActivity(true, text);
    }
}
