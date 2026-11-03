package com.ohinteractive.seedv6.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.*;
import com.ohinteractive.seedv6.ApplicationVersion;
import com.ohinteractive.seedv6.search.driver.BaselineEngine;

/** Presentation only: consumes resolved Play bindings, never discovers or loads an engine. */
final class AboutEnginePanel extends JPanel {
    AboutEnginePanel(ApplicationVersion version, String engines, String details) {
        super(new BorderLayout());
        var tabs = new JTabbedPane();
        tabs.addTab("About", text(version.aboutText() + "\n\nEngine formulation\nProduction default (CP64):\n"
                + BaselineEngine.productionFormulation() + "\n\n" + engines
                + "\n\nScope: Play bindings only. Arena selects its engines independently;"
                + " its current configuration is not observed here.", "aboutSummary"));
        tabs.addTab("Configuration", text(version.aboutText() + "\n\n" + details, "aboutConfiguration"));
        add(tabs);
        setPreferredSize(new Dimension(660, 490));
    }

    private static JScrollPane text(String value, String name) {
        var text = new JTextArea(value);
        text.setName(name);
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(UIManager.getFont("Label.font"));
        text.setBackground(UIManager.getColor("Panel.background"));
        text.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        text.setCaretPosition(0);
        var scroll = new JScrollPane(text);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        return scroll;
    }

    static String play(PlayParticipants bindings, boolean started, boolean changing,
                       GameController.GameMode mode, GameController.HumanSide humanSide, int workers, boolean details) {
        if (changing) return "Play: engine configuration is changing. Reopen About after loading completes.";
        if (!started) return "Play: no game started. The production default above is not an active runtime."
                + " Select an engine and press Start Game to inspect its effective binding.";
        if (mode == GameController.GameMode.HUMAN_VS_HUMAN) return "Play: Human vs Human; no playing engine.";
        var result = new StringBuilder("Play — current game bindings (including paused games)\n");
        if (mode == GameController.GameMode.ENGINE_VS_ENGINE || humanSide == GameController.HumanSide.BLACK)
            result.append("White: ").append(binding(bindings.white(), workers, details)).append("\n");
        if (mode == GameController.GameMode.ENGINE_VS_ENGINE || humanSide == GameController.HumanSide.WHITE)
            result.append("Black: ").append(binding(bindings.black(), workers, details));
        return result.toString().stripTrailing();
    }

    private static String binding(PlayEvaluator binding, int workers, boolean details) {
        if (binding.baseline() != null) {
            var formulation = binding.baseline().formulation(workers, 64);
            return details ? formulation.summary() + "\n" + binding.baseline().identity(workers, 64)
                    : formulation.summary();
        }
        // Explicit historical bindings keep their existing identities, without borrowing CP64's default.
        return (binding.pinned() == null ? "Historical ProductionSearch" : binding.pinned().policy().toString())
                + "\nEvaluator: " + binding.description() + "\n" + binding.identity(workers)
                + "\nWorkers: " + workers + "; Hash: 64 MiB";
    }
}
