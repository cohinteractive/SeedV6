package com.ohinteractive.seedv6.gui;

import java.awt.*;
import javax.swing.*;
import static com.ohinteractive.seedv6.gui.TrainingDashboardModel.*;

/** EDT-only operational cards over immutable trainer publications. */
final class TrainingDashboard extends JPanel implements Scrollable {
    private final JLabel title = label("Training", 21, SeedTheme.TEXT);
    private final JLabel subtitle = label("No active run", 12, SeedTheme.SECONDARY);
    private final JLabel state = label("IDLE", 12, SeedTheme.GREEN), elapsed = label("00:00:00", 17, SeedTheme.TEXT);
    private final JLabel generationElapsed = label("\u2014", 17, SeedTheme.TEXT);
    private final TrainingLoss losses = new TrainingLoss();
    private final ModelLoss latestLoss = new ModelLoss("latestCompletedLoss"), candidateLoss = new ModelLoss("candidateTrainingLoss"), bestLoss = new ModelLoss("bestTrainingLoss");
    private final JTextArea notice = text("", 12, SeedTheme.WARNING);
    private final CampaignDivider campaign = new CampaignDivider();
    private final JLabel runProgress = label("", 13, SeedTheme.TEXT), timeLimit = label("", 12, SeedTheme.SECONDARY);
    private final JLabel positionMethod = label("Positions: awaiting run", 12, SeedTheme.TEXT);
    private final JLabel validationMethod = label("Validation: awaiting run", 12, SeedTheme.TEXT);
    private final PhaseProgress selfPlay = new PhaseProgress("Position generation / self-play", "selfPlayProgress");
    private final PhaseProgress training = new PhaseProgress("Network training", "networkTrainingProgress");
    private final PhaseProgress validation = new PhaseProgress("Validation", "validationPairProgress");
    private final JLabel matchTitle = label("Candidate vs Best", 14, SeedTheme.TEXT);
    private final JLabel leftCaption = label("Candidate", 12, SeedTheme.SECONDARY), rightCaption = label("Best", 12, SeedTheme.SECONDARY);
    private final JLabel leftMetric = metric("\u2014", SeedTheme.GREEN), rightMetric = metric("\u2014", SeedTheme.TEXT);
    private final JLabel direction = label("Awaiting validation", 12, SeedTheme.SECONDARY);
    private final JLabel decision = label("Awaiting validation", 15, SeedTheme.TEXT);
    private final ValidationWinMetric candidateWins = new ValidationWinMetric("candidateWins", SeedTheme.GREEN);
    private final ValidationWinMetric draws = new ValidationWinMetric("validationDraws", SeedTheme.SECONDARY);
    private final ValidationWinMetric bestWins = new ValidationWinMetric("bestWins", SeedTheme.TEXT);
    private JPanel rightCounterColumn, winsColumn;
    private final JPanel winCounts = panel(new GridLayout(1, 3, SeedTheme.scale(12), 0));
    private final WinIncreases winIncreases = new WinIncreases();
    private final JLabel previousDuration = label("Previous generation \u00b7 unavailable", 11, SeedTheme.SECONDARY);
    private final JLabel[] comparison = new JLabel[5], configuration = new JLabel[6];
    private final JLabel recentNote = label("Last 25 plotted \u00b7 latest validation regime only \u00b7 green = promoted", 11, SeedTheme.SECONDARY);
    private final TrainingHistory.Records recent = new TrainingHistory.Records();
    private final TrainingHistory history = new TrainingHistory();
    private final HistoryChart recentScores = new HistoryChart(true, 230), recentDurations = new HistoryChart(false, 230);
    private com.ohinteractive.seedv6.training.history.HistoryRepository.Snapshot lastHistory;

    TrainingDashboard() {
        super(new GridBagLayout()); setOpaque(false); setName("trainingDashboard");
        title.setFont(SeedTheme.font(21, Font.BOLD)); title.setName("trainingHeadline"); state.setName("trainingState");
        state.setOpaque(true); state.setBackground(SeedTheme.SELECTED); SeedTheme.padding(state, 8, 12, 8, 12);
        JPanel heading = new SeedTheme.Card(); heading.setLayout(new BorderLayout());
        heading.setName("trainingStatusArea");
        JPanel headingContent = padded(new GridBagLayout(), 8); heading.add(headingContent);
        JPanel headingBody = panel(new BorderLayout(SeedTheme.scale(10), 0));
        JPanel titles = panel(new BorderLayout(0, SeedTheme.scale(3))); titles.add(title, BorderLayout.NORTH); titles.add(subtitle);
        headingBody.add(titles); JPanel badges = panel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        badges.add(state);
        headingBody.add(badges, BorderLayout.EAST); stack(headingContent, headingBody, 0, 6);
        JPanel models = panel(new GridBagLayout());
        GridBagConstraints modelCell = new GridBagConstraints(); modelCell.weightx = 1; modelCell.fill = GridBagConstraints.BOTH;
        modelCell.insets = new Insets(0, 0, 0, SeedTheme.scale(12));
        for (var model : new ModelLoss[]{candidateLoss, latestLoss, bestLoss}) models.add(model, modelCell);
        stack(headingContent, models, 1, 6);
        JPanel run = panel(new GridLayout(1, 3, SeedTheme.scale(12), 0));
        run.add(runProgress); run.add(value("Run", elapsed)); run.add(value("Generation", generationElapsed));
        elapsed.setFont(SeedTheme.font(12, Font.PLAIN)); generationElapsed.setFont(SeedTheme.font(12, Font.PLAIN));
        stack(headingContent, campaign, 4, 4); stack(headingContent, run, 5, 0);
        notice.setName("trainingDashboardNotice"); stack(headingContent, notice, 6, 0);
        elapsed.setName("campaignElapsed"); generationElapsed.setName("generationElapsed"); previousDuration.setName("previousGenerationDuration");
        runProgress.setName("activeRunProgress"); timeLimit.setName("activeRunTimeLimit");
        positionMethod.setName("effectivePositionMethod"); validationMethod.setName("effectiveValidationMethod");

        JPanel phases = padded(new GridBagLayout(), 0);
        GridBagConstraints phaseCell = new GridBagConstraints(); phaseCell.weightx = 1; phaseCell.fill = GridBagConstraints.HORIZONTAL;
        phaseCell.insets = new Insets(0, 0, 0, SeedTheme.scale(12));
        for (var phase : new PhaseProgress[]{selfPlay, training, validation}) phases.add(phase, phaseCell);
        stack(headingContent, phases, 2, 4);
        JPanel comparisonBody = padded(new BorderLayout(0, SeedTheme.scale(3)), 6);
        winCounts.add(winCounter("Candidate", candidateWins)); winCounts.add(winCounter("Draws", draws)); winCounts.add(winCounter("Best", bestWins));
        JPanel mainMetrics = panel(new GridBagLayout());
        addCell(mainMetrics, counter(leftCaption, leftMetric, ""), 0, .25);
        addCell(mainMetrics, counter(rightCaption, rightMetric, ""), 1, .25);
        rightCounterColumn = (JPanel) mainMetrics.getComponent(1);
        addCell(mainMetrics, winCounts, 2, .75); winsColumn = (JPanel) mainMetrics.getComponent(2);
        comparisonBody.add(mainMetrics, BorderLayout.NORTH);
        JPanel evidence = panel(new GridBagLayout());
        JPanel evidenceDetails = panel(new GridBagLayout()); evidenceDetails.setVisible(false);
        evidenceDetails.setName("trainingValidationEvidence");
        JPanel verdict = panel(new BorderLayout(SeedTheme.scale(8), 0));
        verdict.add(decision, BorderLayout.WEST); verdict.add(direction);
        JToggleButton evidenceToggle = new JToggleButton("Evidence"); evidenceToggle.setName("trainingValidationDetails");
        evidenceToggle.setFont(SeedTheme.font(11, Font.PLAIN));
        evidenceToggle.addActionListener(e -> { evidenceDetails.setVisible(evidenceToggle.isSelected()); revalidate(); });
        verdict.add(evidenceToggle, BorderLayout.EAST); stack(evidence, verdict, 0, 0); stack(evidence, evidenceDetails, 1, 0);
        for (int i = 0; i < comparison.length; i++) {
            comparison[i] = label("", 11, SeedTheme.SECONDARY); stack(evidenceDetails, comparison[i], i, 0);
        }
        stack(evidenceDetails, previousDuration, 5, 0);
        comparisonBody.add(evidence);
        leftMetric.setName("candidateScore"); decision.setName("promotionDecision");
        JPanel config = padded(new BorderLayout(), 8);
        JPanel configRows = panel(new GridBagLayout()); config.add(configRows, BorderLayout.NORTH);
        config.setName("effectiveTrainingConfiguration");
        for (int i = 0; i < configuration.length; i++) { configuration[i] = label("\u2014", 12, SeedTheme.TEXT); stack(configRows, configuration[i], i, 4); }
        JPanel comparisonSummary = panel(new BorderLayout()); comparisonSummary.add(matchTitle, BorderLayout.NORTH); comparisonSummary.add(comparisonBody);
        stack(headingContent, comparisonSummary, 3, 4);
        JPanel methods = panel(new GridLayout(0, 1, 0, SeedTheme.scale(3)));
        methods.add(positionMethod); methods.add(validationMethod); methods.add(timeLimit); config.add(methods, BorderLayout.SOUTH);
        JPanel expanded = panel(new BorderLayout()); expanded.add(config); expanded.setVisible(false);
        JToggleButton details = new JToggleButton("Effective configuration"); details.setName("trainingConfigurationDetails");
        details.addActionListener(e -> { expanded.setVisible(details.isSelected()); revalidate(); });
        JPanel configurationDetails = panel(new BorderLayout()); configurationDetails.add(details, BorderLayout.NORTH); configurationDetails.add(expanded);

        JPanel previews = panel(new GridBagLayout()); previews.setName("runtimeGraphs");
        recentScores.setName("comparisonTrend"); recentDurations.setName("generationDuration");
        addCell(previews, card("Comparison trend \u00b7 latest validation regime", null, recentScores), 0, .62);
        addCell(previews, card("Generation duration", null, recentDurations), 1, .38);
        JPanel latest = padded(new BorderLayout(0, SeedTheme.scale(6)), 8); latest.add(recentNote, BorderLayout.NORTH);
        JScrollPane recentScroll = scroll(TrainingHistory.table(recent, "recentTrainingHistory"));
        recentScroll.setPreferredSize(new Dimension(1, SeedTheme.scale(190))); latest.add(recentScroll);
        JComponent[] cards = {heading, previews, card("Recent History", null, latest), configurationDetails};
        GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.weightx = 1; c.fill = GridBagConstraints.BOTH;
        for (int i = 0; i < cards.length; i++) {
            c.gridy = i; c.insets = new Insets(0, 0, SeedTheme.scale(6), 0);
            c.weighty = i == 1 ? 1 : 0;
            add(widthIndependent(cards[i]), c);
        }
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & java.awt.event.HierarchyEvent.SHOWING_CHANGED) != 0) {
                winIncreases.reset(); candidateWins.stopPulse(); bestWins.stopPulse();
            }
        });
    }
    JScrollPane historyView() { return scroll(history); }

    void showState(TrainingController.ViewState view) {
        var s = view.loading() ? null : view.snapshot(); var r = s == null ? null : s.run().orElse(null);
        var displayedHistory = view.loading() ? com.ohinteractive.seedv6.training.history.HistoryRepository.Snapshot.EMPTY : view.history();
        title.setText(generationTitle(s));
        if (!view.active() && view.phase() != TrainingController.Phase.FAILED && view.startAction().equals("Start Training"))
            title.setText("Generation " + (s == null ? 1 : s.generation() + (s.run().isPresent() ? 1 : 0)) + " \u00b7 Ready");
        if (view.loading()) title.setText("Loading lineage");
        title.setToolTipText(title.getText());
        subtitle.setText(view.settings().architecture() + " \u00b7 " + (view.lineage() == null ? "No lineage selected" : view.lineage().lineage().name()));
        if (view.loading()) subtitle.setText("Waiting for selected lineage metadata");
        subtitle.setToolTipText(subtitle.getText() + " \u00b7 " + view.settings().root());
        subtitle.setName("trainingLineageIdentity");
        state.setText(phase(view)); state.setForeground(view.phase() == TrainingController.Phase.FAILED ? SeedTheme.ERROR : SeedTheme.GREEN);
        state.setToolTipText(phase(view));
        if (view.scheduledStopGeneration() > 0 && view.phase() == TrainingController.Phase.RUNNING) state.setText("STOP SCHEDULED");
        elapsed.setText(timer(s == null ? 0 : s.elapsed().toSeconds()));
        generationElapsed.setText(s == null ? "\u2014" : s.generationElapsed(System.nanoTime()).map(d -> timer(d.toSeconds())).orElse("\u2014"));
        runProgress.setText(runLabel(s)); timeLimit.setText(timeLabel(s));
        campaign.showProgress(campaignFraction(s), runLabel(s));
        positionMethod.setText("Positions: " + (r == null ? "awaiting effective configuration" : TrainingComparison.positionMethod(r)));
        validationMethod.setText("Validation: " + (r == null ? "awaiting effective configuration" : TrainingComparison.method(r)));
        selfPlay.show(selfPlay(s, view.settings())); training.show(training(s)); validation.show(validation(s, view.settings()));
        selfPlay.setVisible(positionGenerationActive(view));
        selfPlay.bar.setVisible(positionGenerationActive(view));
        showLosses(view);
        String warning = view.phase() == TrainingController.Phase.FAILED ? view.message()
                : !view.historyWarning().isBlank() ? view.historyWarning()
                : !view.history().warnings().isEmpty() ? "History has warnings; see History / Diagnostics"
                : view.message().contains("Could not") || view.message().contains("Restarted") ? view.message() : "";
        notice.setForeground(view.phase() == TrainingController.Phase.FAILED ? SeedTheme.ERROR : SeedTheme.WARNING);
        if (!notice.getText().equals(warning)) notice.setText(warning);
        notice.setVisible(!warning.isBlank());
        showComparison(s);
        var prior = previousGeneration(displayedHistory.records(), s == null ? 0 : s.generation());
        previousDuration.setText("Previous generation \u00b7 " + (prior == null ? "unavailable" : "Gen " + prior.generation()
                + " \u00b7 " + (prior.totalNanos() == null ? "duration unavailable" : timer(prior.totalNanos() / 1_000_000_000))));
        var pulse = winIncreases.update(s, view.active() && isShowing());
        var m = match(s);
        candidateWins.showCount(m == null ? null : m.wins(), pulse.candidate());
        draws.showCount(m == null ? null : m.draws(), false); bestWins.showCount(m == null ? null : m.losses(), pulse.best());
        String[] config = r == null ? new String[]{"Effective settings appear when the generation starts"}
                : r.session() != null && !view.active() && r.action().equals("Recovered session") ? new String[]{
                r.effective().architecture().displayName() + " | Saved session: " + r.session().state(),
                r.session().description(), "Review lineage settings before Start; loading never starts training"}
                : r.source().corpus() && !r.generationSettingsKnown() ? new String[]{
                r.effective().architecture().displayName() + " Training Data",
                "Checking Training Data source identities",
                "Source identity checks precede bounded generation acquisition",
                view.message(), "Durable generation ranges and progress are preserved", ""}
                : !r.generationSettingsKnown() ? new String[]{"Recovering a legacy Candidate", NetworkArchitecture.valueOf(r.effective().architecture().name()) + " \u00b7 Recorded depth: " + s.trainingDepth(), "Original generation settings were not recorded",
                "Current validation: " + r.effective().validation().openingPairs() + " pairs",
                "Validation: " + r.effective().validation().searchDescription() + " \u00b7 Threads: " + r.effective().validation().threads()}
                : r.source().corpus() ? new String[]{
                r.effective().architecture().displayName() + " Training Data training",
                count(r.effective().corpusTraining().positionsPerGeneration()) + " positions / generation; no generated games",
                (r.effective().architecture().nnueFamily() || r.effective().architecture().corpusOnly())
                        ? "Epochs: " + r.effective().training().epochs() + " \u00b7 Batch: " + r.effective().training().minibatchSize() : "One online pass per generation",
                r.effective().heldOut(r.source()) ? "Holdout: separately reserved source positions"
                        : "Validation: " + r.effective().validation().openingPairs() + " game pairs | " + r.effective().validation().searchDescription(),
                r.effective().heldOut(r.source()) ? "Strictly lower loss wins; ties keep Best"
                        : "Promotion margin: " + score(r.effective().validation().policy().requiredMargin()),
                r.effective().architecture().nnueFamily()
                        ? "Targets: STOCKFISH_WDL_V1: side-to-move expected outcome"
                        : r.effective().architecture().corpusOnly()
                        ? "Targets: side-to-move outcome, using each source's label profile" : "Targets: side-to-move CP / 32,511 (BASIC_V1)"}
                : new String[]{
                NetworkArchitecture.valueOf(r.effective().architecture().name()) + " \u00b7 Depth: " + r.effective().selfPlay().depth() + " \u00b7 Workers: " + r.effective().selfPlay().threads(),
                count(r.effective().selfPlay().games()) + " games \u00b7 up to " + r.effective().selfPlay().maximumSamplesPerGame() + " samples/game",
                "Epochs: " + r.effective().training().epochs() + " \u00b7 Batch: " + r.effective().training().minibatchSize() + " \u00b7 Openings: " + r.effective().selfPlay().minimumOpeningPlies() + "\u2014" + r.effective().selfPlay().maximumOpeningPlies(),
                r.effective().heldOut(r.source()) ? "Holdout: whole games, about 20% (at least 2)" : "Validation: " + r.effective().validation().openingPairs()
                        + " pairs \u00b7 " + r.effective().validation().searchDescription() + " \u00b7 Threads " + r.effective().validation().threads(),
                r.effective().heldOut(r.source()) ? "Strictly lower loss wins \u00b7 Cap " + r.effective().selfPlay().maximumPlies() : "Promotion margin: " + score(r.effective().validation().policy().requiredMargin()) + " \u00b7 Cap " + r.effective().selfPlay().maximumPlies(),
                r.supervision().blended() ? "Targets: NNUE " + score(r.supervision().teacherWeight()) + " \u00b7 WDL " + score(1 - r.supervision().teacherWeight())
                        : r.effective().heldOut(r.source()) ? "Targets: terminal WDL" : ""};
        if (r != null && r.generationSettingsKnown()
                && r.effective().architecture() == com.ohinteractive.seedv6.training.model.TrainingArchitecture.BRN2)
            config[5] += " | " + r.effective().effectiveCaptureConsistency().description();
        for (int i = 0; i < configuration.length; i++) set(configuration[i], i < config.length ? config[i] : "");
        history.showHistory(displayedHistory, view.loading() ? "" : view.historyWarning());
        if (lastHistory != displayedHistory) {
            lastHistory = displayedHistory; var records = lastHistory.records();
            var preview = records.subList(Math.max(0, records.size() - 25), records.size());
            recentScores.showRecords(preview); recentDurations.showRecords(preview);
            recent.show(records.subList(Math.max(0, records.size() - 5), records.size()));
        }
        recentScores.showLive(view.active() ? liveComparison(s) : null);
        recentNote.setText(view.historyWarning().isBlank() && view.history().warnings().isEmpty()
                ? "C = candidate · B = incumbent Best · Δ = C − B · ↓ lower loss is better · ↑ higher score is better"
                : "History warning \u00b7 see History / Diagnostics");
    }
    private void showLosses(TrainingController.ViewState view) {
        losses.update(view.loading() ? com.ohinteractive.seedv6.training.history.HistoryRepository.Snapshot.EMPTY : view.history());
        var s = view.loading() ? null : view.snapshot();
        var latest = losses.latest();
        latestLoss.showModel("Latest completed", latest == null ? "" : latest.candidate(),
                latest == null ? null : latest.loss(), "Training loss \u00b7 final", latest == null ? "No completed history" : losses.summary(latest.candidate(), null));
        String candidate = s == null ? "" : s.candidateId(), best = s == null ? "" : s.bestId();
        var held = losses.validation(best, s);
        Double bestTraining = losses.finalLoss(best, s);
        bestLoss.showModel("Best", best, bestTraining != null ? bestTraining : held == null ? null : held.value(),
                bestTraining != null || held == null ? "Training loss \u00b7 final" : "Validation loss \u00b7 held-out", losses.summary(best, s));
        Double finalLoss = losses.finalLoss(candidate, s);
        boolean unpublishedFit = s != null && candidate.isEmpty() && s.training().filter(t -> !t.cancelled() && TrainingLoss.valid(t.finalLoss())).isPresent();
        if (unpublishedFit) finalLoss = s.training().orElseThrow().finalLoss();
        boolean mean = finalLoss == null && s != null && s.generationSamplesTrained() > 0 && TrainingLoss.valid(s.meanTrainingLoss())
                && (candidate.isEmpty() || TrainingLoss.checkpointGeneration(candidate) == s.generation())
                && switch (s.state()) {
                    case TRAINING, PUBLISHING_CANDIDATE, VALIDATING, RECORDING_DECISION, STOPPING, STOPPED, FAILED -> true;
                    default -> false;
                };
        var candidateHeld = losses.validation(candidate, s);
        Double candidateValue = finalLoss;
        if (candidateValue == null && candidateHeld != null) candidateValue = candidateHeld.value();
        if (mean) candidateValue = s.meanTrainingLoss();
        candidateLoss.showModel((mean || unpublishedFit) && candidate.isEmpty() ? "Training \u00b7 Gen " + s.generation() : "Candidate", candidate,
                candidateValue,
                mean ? "Training loss \u00b7 mean so far" : finalLoss != null || candidateHeld == null ? "Training loss \u00b7 final" : "Validation loss \u00b7 held-out",
                mean ? "Mean over observed optimizer sample visits; not a final-model evaluation. " + TrainingLoss.EXPLANATION
                        : unpublishedFit ? "Final fit on this generation's training samples, before checkpoint publication. " + TrainingLoss.EXPLANATION : losses.summary(candidate, s));
        // Combine roles only for the exact same model and final measurement, never for merely equal losses.
        boolean latestCandidate = latest != null && latest.candidate().equals(candidate) && TrainingLoss.valid(latest.loss())
                && latest.loss().equals(finalLoss);
        boolean latestBest = latest != null && latest.candidate().equals(best) && TrainingLoss.valid(latest.loss())
                && latest.loss().equals(bestTraining);
        boolean candidateBest = !candidate.isEmpty() && candidate.equals(best) && TrainingLoss.valid(finalLoss) && finalLoss.equals(bestTraining);
        candidateLoss.setVisible(!latestCandidate); bestLoss.setVisible(!latestBest && !candidateBest);
        if (latestCandidate || latestBest) latestLoss.showModel("Latest completed" + (latestCandidate ? " / Candidate" : "") + (latestBest ? " / Best" : ""),
                latest.candidate(), latest.loss(), "Training loss \u00b7 final", losses.summary(latest.candidate(), s));
        else if (candidateBest) candidateLoss.showModel("Candidate / Best", candidate, finalLoss, "Training loss \u00b7 final", losses.summary(candidate, s));
    }
    private static final class ModelLoss extends JPanel {
        private final JLabel identity = label("", 12, SeedTheme.TEXT);
        private final JLabel metric = TrainingDashboard.metric(TrainingLoss.UNAVAILABLE, SeedTheme.GREEN);
        private final JLabel caption = label("Training loss", 11, SeedTheme.SECONDARY);
        ModelLoss(String name) {
            super(new BorderLayout(0, SeedTheme.scale(2))); setOpaque(false); setMinimumSize(new Dimension(0, 0));
            metric.setName(name); identity.setName(name + "Identity"); caption.setName(name + "Kind");
            add(identity, BorderLayout.NORTH); add(metric); add(caption, BorderLayout.SOUTH);
        }
        void showModel(String role, String id, Double loss, String kind, String evidence) {
            identity.setText(role + (id.isEmpty() ? "" : " \u00b7 " + network(id)));
            identity.setToolTipText(id.isEmpty() ? role : role + ": " + id);
            metric.setText(TrainingLoss.format(loss)); metric.setToolTipText(evidence);
            metric.getAccessibleContext().setAccessibleName(identity.getText() + ": " + kind + " " + metric.getText());
            caption.setText(kind); caption.setToolTipText(evidence);
        }
    }
    private void showComparison(com.ohinteractive.seedv6.training.service.TrainerSnapshot s) {
        for (var line : comparison) set(line, "");
        var b = s == null ? null : s.bootstrapValidation().orElse(null);
        var m = match(s);
        boolean heldOut = b != null || s != null && s.run().map(r -> r.effective().heldOut(r.source())).orElse(false);
        winCounts.setVisible(!heldOut); winsColumn.setVisible(!heldOut); rightCounterColumn.setVisible(heldOut);
        matchTitle.setText("Candidate vs Best" + (s != null && ((b != null && !b.candidateId().equals(s.candidateId())) || m != null && !m.current()) ? " \u00b7 latest result" : ""));
        if (heldOut) {
            leftCaption.setText("Candidate validation loss"); rightCaption.setText("Incumbent validation loss");
            leftMetric.setText(b == null ? "\u2014" : TrainingComparison.loss(b.evidence().comparison().candidateLoss()));
            rightMetric.setText(b == null ? "\u2014" : TrainingComparison.loss(b.evidence().comparison().bestLoss()));
            direction.setText(b == null ? "↓ Lower loss is better" : TrainingComparison.lossName(b.evidence()) + " ↓ · Δ " + TrainingComparison.delta(b.evidence().comparison()));
            decision.setText(b == null ? "Awaiting validation" : b.evidence().comparison().decision() == com.ohinteractive.seedv6.training.validation.PromotionPolicy.Decision.PROMOTE
                    ? s.bestId().equals(b.candidateId()) ? "PROMOTED" : "Promotion publication pending" : "BEST RETAINED");
            if (b != null) {
                var e = b.evidence();
                set(comparison[0], e.comparison().samples() + " held-out samples \u00b7 strict lower configured loss wins; ties retain Best");
                if (e.wdlLoss() != null) {
                    set(comparison[1], TrainingComparison.pair("WDL", e.wdlLoss()));
                    set(comparison[2], TrainingComparison.pair("NNUE", e.teacherLoss()));
                    set(comparison[3], "Decision uses configured target loss above \u00b7 " + (e.corpus() == null ? e.supervision().description() : e.corpus().targetAdapter() == null ? "Training Data CP" : e.corpus().adapterIdentity()));
                }
                set(comparison[4], "Candidate " + network(b.candidateId()) + " \u00b7 Incumbent " + network(b.incumbentId()));
                comparison[4].setToolTipText(b.candidateId() + " / " + b.incumbentId());
            }
        } else {
            leftCaption.setText("Score"); rightCaption.setText("Promotion lower bound");
            leftMetric.setText(m == null ? "\u2014" : m.score());
            rightMetric.setText(s == null || s.assessment().isEmpty() ? "\u2014" : score(s.assessment().get().lowerBound()));
            direction.setText("\u2191 Higher is better \u00b7 valid pairs only");
            decision.setText(m == null ? "Awaiting validation" : m.decision());
            if (m != null) {
                long finishedGames = s.validation().map(v -> v.terminations().entrySet().stream()
                        .filter(e -> e.getKey().completed() || e.getKey() == com.ohinteractive.seedv6.training.selfplay.GameTermination.PLY_CAP)
                        .mapToLong(java.util.Map.Entry::getValue).sum()).orElseGet(() -> (long) s.validationProgress().orElseThrow().completedGames());
                set(comparison[1], "Games: " + finishedGames + " / " + (2 * m.configuredPairs()) + " \u00b7 Valid pairs: " + m.pairs() + " / " + m.configuredPairs());
                set(comparison[2], m.detail());
                set(comparison[0], "Candidate Gen " + m.candidate() + " \u00b7 Best Gen " + m.incumbent());
                comparison[0].setToolTipText(m.candidateId() + " / " + m.incumbentId());
            }
        }
        decision.setForeground(decision.getText().equals("PROMOTED") ? SeedTheme.GREEN : SeedTheme.TEXT);
        direction.setToolTipText(direction.getText()); decision.setToolTipText(decision.getText());
    }
    private static void set(JLabel label, String text) { label.setText(text); label.setToolTipText(text); label.setVisible(!text.isEmpty()); }
    private static void stack(JPanel parent, JComponent child, int row, int gap) {
        GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.gridy = row; c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL; c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(0, 0, SeedTheme.scale(gap), 0); parent.add(child, c);
    }
    static JPanel card(String title, JLabel heading, JComponent body) {
        JPanel card = new SeedTheme.Card(); card.setLayout(new BorderLayout());
        JPanel header = panel(new BorderLayout());
        JLabel label = heading == null ? label(title, 14, SeedTheme.TEXT) : heading;
        label.setFont(SeedTheme.font(14, Font.BOLD)); header.add(label);
        header.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, SeedTheme.LINE),
                BorderFactory.createEmptyBorder(SeedTheme.scale(3), SeedTheme.scale(12), SeedTheme.scale(3), SeedTheme.scale(12))));
        card.add(header, BorderLayout.NORTH); card.add(body); return card;
    }
    static JPanel padded(LayoutManager layout, int pad) { JPanel p = panel(layout); SeedTheme.padding(p, pad, pad, pad, pad); return p; }
    static JPanel panel(LayoutManager layout) { return SeedTheme.panel(layout); }
    static JLabel label(String text, int size, Color color) { return SeedTheme.label(text, size, color); }
    static JTextArea text(String value, int size, Color color) {
        JTextArea text = new JTextArea(value); text.setEditable(false); text.setOpaque(false); text.setLineWrap(true); text.setWrapStyleWord(true);
        text.setFont(SeedTheme.font(size, Font.PLAIN)); text.setForeground(color); text.setFocusable(false); text.setBorder(null);
        // These are passive labels, not log editors. Caret updates must never scroll the dashboard.
        ((javax.swing.text.DefaultCaret) text.getCaret()).setUpdatePolicy(javax.swing.text.DefaultCaret.NEVER_UPDATE);
        return text;
    }
    static JScrollPane scroll(JComponent body) {
        JScrollPane scroll = new JScrollPane(body); scroll.setBorder(BorderFactory.createEmptyBorder());
        if (body instanceof JTable table) scroll.setColumnHeaderView(table.getTableHeader());
        scroll.setOpaque(false); scroll.getViewport().setOpaque(false); scroll.getVerticalScrollBar().setUnitIncrement(SeedTheme.scale(24)); return scroll;
    }
    private static JPanel value(String caption, JLabel value) {
        JPanel p = panel(new BorderLayout(SeedTheme.scale(6), 0)); p.add(label(caption, 11, SeedTheme.SECONDARY), BorderLayout.WEST); p.add(value); return p;
    }
    private static JPanel winCounter(String caption, JLabel metric) {
        JLabel title = label(caption, 12, metric.getForeground()); title.setHorizontalAlignment(SwingConstants.CENTER);
        metric.getAccessibleContext().setAccessibleName(caption + (caption.equals("Draws") ? "" : " wins"));
        JPanel p = panel(new BorderLayout()); p.add(title, BorderLayout.NORTH); p.add(metric); return p;
    }
    private static JLabel metric(String value, Color color) { JLabel l = label(value, 24, color); l.setFont(SeedTheme.font(24, Font.BOLD)); return l; }
    private static JPanel counter(JLabel title, JLabel metric, String caption) {
        JPanel p = panel(new BorderLayout(0, SeedTheme.scale(2))); p.add(title, BorderLayout.NORTH); p.add(metric); if (!caption.isEmpty()) p.add(label(caption, 10, SeedTheme.SECONDARY), BorderLayout.SOUTH); return p;
    }
    private static void addCell(JPanel parent, JComponent child, int column, double weight) {
        GridBagConstraints c = new GridBagConstraints(); c.gridx = column; c.weightx = weight; c.weighty = 1; c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, column == 0 ? 0 : SeedTheme.scale(10), 0, 0); child.setMinimumSize(new Dimension(0, 0));
        parent.add(widthIndependent(child), c);
    }
    // Long labels must not force GridBagLayout to shrink vertical content at enlarged display scales.
    private static JPanel widthIndependent(JComponent child) {
        JPanel columnPanel = new JPanel(new BorderLayout()) {
            @Override public Dimension getPreferredSize() { return new Dimension(1, super.getPreferredSize().height); }
            @Override public Dimension getMinimumSize() { return new Dimension(0, getPreferredSize().height); }
        };
        columnPanel.setOpaque(false); columnPanel.add(child); return columnPanel;
    }
    private static final class PhaseProgress extends JPanel {
        private final JLabel value = label("—", 12, SeedTheme.TEXT), detail = label("—", 10, SeedTheme.SECONDARY);
        private final JProgressBar bar = new JProgressBar(0, 100);
        private final JLabel percent = label("—", 10, SeedTheme.SECONDARY);
        PhaseProgress(String title, String name) {
            super(new BorderLayout(0, SeedTheme.scale(3))); setOpaque(false);
            JPanel labels = panel(new GridBagLayout());
            stack(labels, label(title, 12, SeedTheme.TEXT), 0, 2); stack(labels, value, 1, 2); stack(labels, detail, 2, 0); add(labels);
            JPanel progress = panel(new BorderLayout(SeedTheme.scale(8), 0)); bar.setName(name); bar.setForeground(SeedTheme.GREEN);
            bar.setPreferredSize(new Dimension(1, SeedTheme.scale(8))); progress.add(bar); progress.add(percent, BorderLayout.EAST); add(progress, BorderLayout.SOUTH);
        }
        void show(Progress p) {
            value.setText(p.value()); detail.setText(p.detail()); value.setToolTipText(p.value()); detail.setToolTipText(p.detail());
            bar.setValue(Math.max(0, p.percent())); bar.setVisible(p.percent() >= 0); percent.setText(p.percent() < 0 ? "Counted at optimizer boundaries" : p.percent() + "%");
            bar.getAccessibleContext().setAccessibleName(p.value());
        }
    }
    public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return SeedTheme.scale(24); }
    public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return Math.max(1, r.height - SeedTheme.scale(24)); }
    public boolean getScrollableTracksViewportWidth() { return true; }
    public boolean getScrollableTracksViewportHeight() { return getParent() != null && getParent().getHeight() >= getPreferredSize().height; }
}
