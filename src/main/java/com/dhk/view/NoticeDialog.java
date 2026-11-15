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
 * Tells the user something with a modal dialog that only offers an OK button, styled like the
 * {@link ConfirmationDialog} so both read alike when shown one after another.
 *
 * @author Jonathan R. Miller
 */
public class NoticeDialog {

    private Component parentComponent;
    private String title;
    private String message;

    /**
     * Constructor for the {@link NoticeDialog} class.
     *
     * @param parentComponent
     *            - The component whose window owns the dialog, or null to center the dialog on the screen
     * @param title
     *            - The title of the dialog, shown in its title bar
     * @param message
     *            - The message, where each line break starts a new centered line
     */
    public NoticeDialog(Component parentComponent, String title, String message) {
        this.parentComponent = parentComponent;
        this.title = title;
        this.message = message;
    }

    /**
     * Shows the dialog and waits for the user to close it.
     */
    public void showNoticeDialog() {
        // The option pane stretches each label across the message area, so a centered label centers its line
        String[] messageLines = message.split("\n");
        JLabel[] messageLabels = new JLabel[messageLines.length];

        for (int lineIndex = 0; lineIndex < messageLines.length; lineIndex++) {
            messageLabels[lineIndex] = new JLabel(messageLines[lineIndex], SwingConstants.CENTER);
        }

        String[] options = {Localizer.getText("dialog.ok")};

        JOptionPane optionPane = new JOptionPane(messageLabels, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION,
                null, options, options[0]);
        JDialog dialog = optionPane.createDialog(parentComponent, title);
        dialog.setModal(true);
        dialog.setResizable(false);
        dialog.setVisible(true);
    }

}
