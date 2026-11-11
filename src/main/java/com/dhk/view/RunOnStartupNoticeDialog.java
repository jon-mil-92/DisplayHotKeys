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
 * Shows a modal, icon-free dialog centered on the application frame, reporting why the run on startup setting could not
 * be changed, or why it was corrected on launch to match what really starts the application.
 *
 * @author Jonathan R. Miller
 */
public class RunOnStartupNoticeDialog {

    private JFrame parentFrame;

    /**
     * Message shown above the reason lines when a requested change could not be made.
     */
    private static final String FAILED_MESSAGE_TEXT = "Run On Startup could not be changed!";

    /**
     * Message shown above the reason lines when the saved setting was corrected on launch rather than by a click.
     */
    private static final String CHANGED_MESSAGE_TEXT = "Run On Startup was changed to match the scheduled task!";

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
     * First reason line shown when the setting was corrected on launch, which both corrections open with.
     */
    private static final String CHANGED_REASON_LINE_1 = "The task that starts the app upon login could not be";

    /**
     * Second reason line shown when the setting was turned on because that task could not be turned off.
     */
    private static final String CHANGED_ON_REASON_LINE_2 = "turned off without administrator rights.";

    /**
     * Second reason line shown when the setting was turned off because that task could not be created.
     */
    private static final String CHANGED_OFF_REASON_LINE_2 = "created without administrator rights.";

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
        showDialog(FAILED_MESSAGE_TEXT, runOnStartup ? ENABLE_REASON_LINE_1 : DISABLE_REASON_LINE_1,
                runOnStartup ? ENABLE_REASON_LINE_2 : DISABLE_REASON_LINE_2);
    }

    /**
     * Shows the modal notice reporting that the saved setting was corrected on launch to match what really starts the
     * application, since the account could not change it.
     *
     * @param startsOnLogon
     *            - The state the setting was corrected to, which decides the reason shown
     */
    public void showChangedNotice(boolean startsOnLogon) {
        showDialog(CHANGED_MESSAGE_TEXT, CHANGED_REASON_LINE_1,
                startsOnLogon ? CHANGED_ON_REASON_LINE_2 : CHANGED_OFF_REASON_LINE_2);
    }

    /**
     * Shows the modal dialog, centered on the parent frame, with centered message lines above a centered OK button.
     *
     * @param messageText
     *            - The message shown above the reason lines
     * @param reasonText1
     *            - The first reason line
     * @param reasonText2
     *            - The second reason line
     */
    private void showDialog(String messageText, String reasonText1, String reasonText2) {
        /*
         * A frame hidden for the tray or minimized has no on-screen bounds to center on, which can happen to the notice
         * shown on launch, so the dialog then stands alone at the center of the screen
         */
        boolean frameShowing = parentFrame.isShowing() && (parentFrame.getExtendedState() & JFrame.ICONIFIED) == 0;
        JFrame owner = frameShowing ? parentFrame : null;

        final JDialog dialog = new JDialog(owner, "Display Hot Keys", true);
        dialog.setResizable(false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new GridBagLayout());

        JLabel message = new JLabel(messageText, SwingConstants.CENTER);
        JLabel reasonLine1 = new JLabel(reasonText1, SwingConstants.CENTER);
        JLabel reasonLine2 = new JLabel(reasonText2, SwingConstants.CENTER);
        JButton okButton = new JButton("OK");

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

        constraints.gridy = 1;
        constraints.insets = new Insets(0, 36, 2, 36);

        dialog.add(reasonLine1, constraints);

        constraints.gridy = 2;
        constraints.insets = new Insets(0, 36, 24, 36);

        dialog.add(reasonLine2, constraints);

        constraints.gridy = 3;
        constraints.insets = new Insets(0, 0, 22, 0);

        dialog.add(okButton, constraints);

        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

}
