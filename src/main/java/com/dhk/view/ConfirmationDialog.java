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

import java.awt.Component;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

import com.dhk.localization.Localizer;

/**
 * Asks the user to confirm an action with a modal Yes/No dialog owned by the given component's window. Each line of the
 * message is centered, so the text lines up with the centered Yes and No buttons.
 *
 * @author Jonathan R. Miller
 */
public class ConfirmationDialog {

    private Component parentComponent;
    private String title;
    private String message;

    /**
     * Constructor for the {@link ConfirmationDialog} class.
     *
     * @param parentComponent
     *            - The component whose window owns the dialog, or null to center the dialog on the screen
     * @param title
     *            - The title of the dialog, shown in its title bar
     * @param message
     *            - The message, where each line break starts a new centered line
     */
    public ConfirmationDialog(Component parentComponent, String title, String message) {
        this.parentComponent = parentComponent;
        this.title = title;
        this.message = message;
    }

    /**
     * Shows the dialog and waits for the user to answer it.
     *
     * @return The pressed button's option (YES_OPTION or NO_OPTION), or CLOSED_OPTION when the dialog closes unanswered
     */
    public int showConfirmationDialog() {
        // The option pane stretches each label across the message area, so a centered label centers its line
        String[] messageLines = message.split("\n");
        JLabel[] messageLabels = new JLabel[messageLines.length];

        for (int lineIndex = 0; lineIndex < messageLines.length; lineIndex++) {
            messageLabels[lineIndex] = new JLabel(messageLines[lineIndex], SwingConstants.CENTER);
        }

        // The buttons take the selected language's text, since the built-in text only follows the Windows language
        String yesText = Localizer.getText("dialog.yes");
        String[] options = {yesText, Localizer.getText("dialog.no")};

        JOptionPane optionPane = new JOptionPane(messageLabels, JOptionPane.PLAIN_MESSAGE, JOptionPane.YES_NO_OPTION,
                null, options, yesText);
        JDialog dialog = optionPane.createDialog(parentComponent, title);
        dialog.setModal(true);
        dialog.setResizable(false);
        dialog.setVisible(true);

        Object selectedValue = optionPane.getValue();

        if (selectedValue == options[0]) {
            return JOptionPane.YES_OPTION;
        }

        return selectedValue == options[1] ? JOptionPane.NO_OPTION : JOptionPane.CLOSED_OPTION;
    }

}
