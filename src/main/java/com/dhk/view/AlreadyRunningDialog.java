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
 * Shows a modal, icon-free dialog reporting that another instance is already running.
 *
 * @author Jonathan R. Miller
 */
public class AlreadyRunningDialog {

    /**
     * Product name shown as the dialog's title, which is never translated.
     */
    private static final String DIALOG_TITLE = "Display Hot Keys";

    /**
     * Default constructor for the {@link AlreadyRunningDialog} class.
     */
    public AlreadyRunningDialog() {
    }

    /**
     * Shows the modal "already running" dialog with a centered message above a centered OK button.
     */
    public void showAlreadyRunningDialog() {
        final JDialog dialog = new JDialog((JFrame) null, DIALOG_TITLE, true);
        dialog.setResizable(false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new GridBagLayout());

        JLabel message = new JLabel(Localizer.getText("alreadyRunning.message"), SwingConstants.CENTER);
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
        constraints.insets = new Insets(28, 28, 28, 28);

        dialog.add(message, constraints);

        constraints.gridy = 1;
        constraints.insets = new Insets(0, 0, 18, 0);

        dialog.add(okButton, constraints);

        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

}
