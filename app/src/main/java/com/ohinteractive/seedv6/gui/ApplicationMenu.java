package com.ohinteractive.seedv6.gui;

import java.awt.event.KeyEvent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

/** Small action-only menu, also constructible in headless tests. */
final class ApplicationMenu {
    static JMenuBar create(Runnable closeWindow, Runnable showAbout) {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File"); file.setMnemonic(KeyEvent.VK_F);
        JMenuItem exit = new JMenuItem("Exit"); exit.setMnemonic(KeyEvent.VK_X);
        exit.setName("fileExit");
        exit.addActionListener(event -> closeWindow.run());
        file.add(exit); bar.add(file);
        JMenu help = new JMenu("Help"); help.setMnemonic(KeyEvent.VK_H);
        JMenuItem about = new JMenuItem("About"); about.setMnemonic(KeyEvent.VK_A);
        about.setName("helpAbout");
        about.addActionListener(event -> showAbout.run());
        help.add(about); bar.add(help);
        return bar;
    }

    private ApplicationMenu() {}
}
