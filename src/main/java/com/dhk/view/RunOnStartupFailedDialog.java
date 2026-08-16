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

/**
 * Shows a modal, icon-free dialog reporting why the run on startup setting could not be changed.
 *
 * @author Jonathan R. Miller
 */
public class RunOnStartupFailedDialog {

    /**
     * Message shown above the reason lines.
     */
    private static final String MESSAGE_TEXT = "Run On Startup could not be changed!";

    /**
     * First reason line shown when the setting could not be turned off.
     */
    private static final String DISABLE_REASON_LINE_1 = "A scheduled task still starts the app upon login,";

    /**
     * Second reason line shown when the setting could not be turned off.
     */
    private static final String DISABLE_REASON_LINE_2 = "and removing it requires administrator rights.";

    /**
     * First reason line shown when the setting could not be turned on.
     */
    private static final String ENABLE_REASON_LINE_1 = "The scheduled task could not be registered,";

    /**
     * Second reason line shown when the setting could not be turned on.
     */
    private static final String ENABLE_REASON_LINE_2 = "and the startup folder could not be written to.";

    /**
     * Default constructor for the {@link RunOnStartupFailedDialog} class.
     */
    public RunOnStartupFailedDialog() {
    }

    /**
     * Shows the modal "run on startup failed" dialog with centered message lines above a centered Close button.
     *
     * @param runOnStartup
     *            - The state the user asked for, which decides the reason shown
     */
    public void showRunOnStartupFailedDialog(boolean runOnStartup) {
        final JDialog dialog = new JDialog((JFrame) null, "Display Hot Keys", true);
        dialog.setResizable(false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new GridBagLayout());

        JLabel message = new JLabel(MESSAGE_TEXT, SwingConstants.CENTER);
        JLabel reasonLine1 = new JLabel(runOnStartup ? ENABLE_REASON_LINE_1 : DISABLE_REASON_LINE_1,
                SwingConstants.CENTER);
        JLabel reasonLine2 = new JLabel(runOnStartup ? ENABLE_REASON_LINE_2 : DISABLE_REASON_LINE_2,
                SwingConstants.CENTER);
        JButton closeButton = new JButton("Close");

        // Suppress the focus ring
        closeButton.setFocusPainted(false);
        closeButton.addActionListener(new ActionListener() {
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

        constraints.gridy = 1;
        constraints.insets = new Insets(0, 36, 2, 36);

        dialog.add(reasonLine1, constraints);

        constraints.gridy = 2;
        constraints.insets = new Insets(0, 36, 24, 36);

        dialog.add(reasonLine2, constraints);

        constraints.gridy = 3;
        constraints.insets = new Insets(0, 0, 22, 0);

        dialog.add(closeButton, constraints);

        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

}
