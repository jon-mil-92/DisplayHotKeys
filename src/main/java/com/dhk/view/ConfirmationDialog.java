/*
 * The MIT License (MIT)
 *
 * Copyright © 2026 Jonathan R. Miller
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the “Software”), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.dhk.view;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

/**
 * Asks the user to confirm an action with a modal Yes/No dialog owned by the application frame. Each line of the
 * message is centered, so the text lines up with the centered Yes and No buttons.
 *
 * @author Jonathan R. Miller
 */
public class ConfirmationDialog {

    private JFrame parentFrame;
    private String title;
    private String[] messageLines;

    /**
     * Constructor for the {@link ConfirmationDialog} class.
     *
     * @param parentFrame
     *            - The frame that owns the dialog
     * @param title
     *            - The title of the dialog, shown in its title bar
     * @param messageLines
     *            - The lines of the message, each shown centered on its own line
     */
    public ConfirmationDialog(JFrame parentFrame, String title, String... messageLines) {
        this.parentFrame = parentFrame;
        this.title = title;
        this.messageLines = messageLines;
    }

    /**
     * Shows the dialog and waits for the user to answer it.
     *
     * @return The pressed button's option (YES_OPTION or NO_OPTION), or CLOSED_OPTION when the dialog closes unanswered
     */
    public int showConfirmationDialog() {
        // The option pane stretches each label across the message area, so a centered label centers its line
        JLabel[] messageLabels = new JLabel[messageLines.length];

        for (int lineIndex = 0; lineIndex < messageLines.length; lineIndex++) {
            messageLabels[lineIndex] = new JLabel(messageLines[lineIndex], SwingConstants.CENTER);
        }

        JOptionPane optionPane = new JOptionPane(messageLabels, JOptionPane.PLAIN_MESSAGE, JOptionPane.YES_NO_OPTION);
        JDialog dialog = optionPane.createDialog(parentFrame, title);
        dialog.setModal(true);
        dialog.setResizable(false);
        dialog.setVisible(true);

        Object selectedValue = optionPane.getValue();

        return selectedValue instanceof Integer ? (Integer) selectedValue : JOptionPane.CLOSED_OPTION;
    }

}
