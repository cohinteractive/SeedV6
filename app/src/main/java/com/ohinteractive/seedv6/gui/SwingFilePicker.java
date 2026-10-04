package com.ohinteractive.seedv6.gui;

import java.awt.Window;
import java.nio.file.Path;
import java.util.Optional;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Bounded fallback for platforms other than Windows and macOS. */
final class SwingFilePicker implements FilePickers.Backend {
    @Override public Optional<Path> show(Window owner, FilePickers.Request request) {
        JFileChooser chooser = new JFileChooser(request.directory() == null ? null : request.directory().toFile());
        chooser.setDialogTitle(request.title());
        chooser.setMultiSelectionEnabled(false);
        chooser.setFileSelectionMode(switch (request.kind()) {
            case FILE -> JFileChooser.FILES_ONLY;
            case DIRECTORY -> JFileChooser.DIRECTORIES_ONLY;
            case FILE_OR_DIRECTORY -> JFileChooser.FILES_AND_DIRECTORIES;
        });
        if (request.purpose() == FilePickers.Purpose.CORPUS_ARCHIVE) {
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.setFileFilter(new FileNameExtensionFilter("Lichess JSONL Zstandard archive (*.jsonl.zst)", "zst"));
        }
        return chooser.showOpenDialog(owner) == JFileChooser.APPROVE_OPTION
                ? Optional.of(chooser.getSelectedFile().toPath()) : Optional.empty();
    }
}
