package com.ohinteractive.seedv6.gui;

import com.ohinteractive.seedv6.search.exact.SearchThreads;
import java.text.ParseException;
import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;

/** Same bounded numeric/Max editor for Play, Training and Arena. Stored Max remains zero. */
final class ThreadSelection {
    private ThreadSelection() {}
    static JSpinner spinner(int choice) {
        var spinner = new JSpinner(new SpinnerNumberModel(0, 0, SearchThreads.available(), 1));
        var editor = new JSpinner.DefaultEditor(spinner);
        editor.getTextField().setEditable(true); editor.getTextField().setColumns(8);
        editor.getTextField().setFormatterFactory(new DefaultFormatterFactory(new JFormattedTextField.AbstractFormatter() {
            public String valueToString(Object value) {
                int n = ((Number) value).intValue();
                return n == 0 ? "Max (" + SearchThreads.available() + ")" : Integer.toString(n);
            }
            public Object stringToValue(String text) throws ParseException {
                if (text.trim().equalsIgnoreCase("auto") || text.trim().toLowerCase(java.util.Locale.ROOT).startsWith("max")) return 0;
                try {
                    int n = Integer.parseInt(text.trim());
                    if (n < 0 || n > SearchThreads.available()) throw new NumberFormatException();
                    return n;
                } catch (NumberFormatException invalid) {
                    throw new ParseException("Choose Max or 1.." + SearchThreads.available(), 0);
                }
            }
        }));
        spinner.setEditor(editor); setChoice(spinner, choice);
        spinner.setToolTipText("Max follows this machine's available processors, up to the supported search limit. Games run sequentially.");
        return spinner;
    }
    static void setChoice(JSpinner spinner, int choice) {
        new SearchThreads(choice);
        spinner.setValue(choice == 0 ? 0 : Math.min(choice, SearchThreads.available()));
    }
    static int resolved(JSpinner spinner) { return new SearchThreads(((Number) spinner.getValue()).intValue()).resolve(); }
}
