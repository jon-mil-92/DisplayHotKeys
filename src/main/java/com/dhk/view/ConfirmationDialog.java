/*
 * Display Hot Keys
 * Copyright © 2024-2026 Jonathan R. Miller
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of version 3 of the GNU
 * General Public License as published by the Free Software Foundation, together with the additional terms under section
 * 7 of that License in ADDITIONAL_TERMS.txt.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License and the additional terms along with this program in
 * LICENSE.txt and ADDITIONAL_TERMS.txt. If not, see <https://www.gnu.org/licenses/>.
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
