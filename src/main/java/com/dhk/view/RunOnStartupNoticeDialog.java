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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import com.dhk.localization.Localizer;

/**
 * Shows a modal, icon-free dialog centered on the application frame, reporting why the run on startup setting could not
 * be changed, or why it was corrected on launch to match what really starts the application.
 *
 * @author Jonathan R. Miller
 */
public class RunOnStartupNoticeDialog {

    private JFrame parentFrame;

    /**
     * Product name shown as the dialog's title, which is never translated.
     */
    private static final String DIALOG_TITLE = "Display Hot Keys";

    /**
     * Constructor for the {@link RunOnStartupNoticeDialog} class.
     *
     * @param parentFrame
     *            - The application frame the dialog is centered on
     */
    public RunOnStartupNoticeDialog(JFrame parentFrame) {
        this.parentFrame = parentFrame;
    }

    /**
     * Shows the modal notice reporting that a requested change to the setting could not be made.
     *
     * @param runOnStartup
     *            - The state the user asked for, which decides the reason shown
     */
    public void showFailedNotice(boolean runOnStartup) {
        showDialog(Localizer.getText("runOnStartup.failed"),
                Localizer.getText(runOnStartup ? "runOnStartup.failedOn" : "runOnStartup.failedOff"));
    }

    /**
     * Shows the modal notice reporting that the saved setting was corrected on launch to match what really starts the
     * application, since the account could not change it.
     *
     * @param startsOnLogon
     *            - The state the setting was corrected to, which decides the reason shown
     */
    public void showChangedNotice(boolean startsOnLogon) {
        showDialog(Localizer.getText("runOnStartup.changed"),
                Localizer.getText(startsOnLogon ? "runOnStartup.changedOn" : "runOnStartup.changedOff"));
    }

    /**
     * Shows the modal dialog, centered on the parent frame, with centered message lines above a centered OK button.
     *
     * @param messageText
     *            - The message shown above the reason
     * @param reasonText
     *            - The reason, where each line break starts a new centered line
     */
    private void showDialog(String messageText, String reasonText) {
        /*
         * A frame hidden for the tray or minimized has no on-screen bounds to center on, which can happen to the notice
         * shown on launch, so the dialog then stands alone at the center of the screen
         */
        boolean frameShowing = parentFrame.isShowing() && (parentFrame.getExtendedState() & JFrame.ICONIFIED) == 0;
        JFrame owner = frameShowing ? parentFrame : null;

        final JDialog dialog = new JDialog(owner, DIALOG_TITLE, true);
        dialog.setResizable(false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new GridBagLayout());

        JLabel message = new JLabel(messageText, SwingConstants.CENTER);
        String[] reasonLines = reasonText.split("\n");
        JButton okButton = new JButton(Localizer.getText("dialog.ok"));

        // Suppress the focus ring
        okButton.setFocusPainted(false);
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(28, 36, 16, 36);

        dialog.add(message, constraints);

        // Each reason line sits close below the one before it, and the last one keeps the gap above the button
        for (int lineIndex = 0; lineIndex < reasonLines.length; lineIndex++) {
            int bottomInset = lineIndex == reasonLines.length - 1 ? 24 : 2;

            constraints.gridy = lineIndex + 1;
            constraints.insets = new Insets(0, 36, bottomInset, 36);

            dialog.add(new JLabel(reasonLines[lineIndex], SwingConstants.CENTER), constraints);
        }

        constraints.gridy = reasonLines.length + 1;
        constraints.insets = new Insets(0, 0, 22, 0);

        dialog.add(okButton, constraints);

        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

}
