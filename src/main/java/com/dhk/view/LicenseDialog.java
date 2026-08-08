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
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JTextArea;

/**
 * Shows the bundled license text in a modal dialog owned by a parent window. The text area sizes to its content, so the
 * dialog stays fitted to the license at any DPI scale.
 *
 * @author Jonathan R. Miller
 */
public class LicenseDialog {

    private Window owner;

    private static final String LICENSE_RESOURCE = "/LICENSE.txt";
    private static final String UNAVAILABLE_TEXT = "License text is unavailable.";

    /**
     * Constructor for the {@link LicenseDialog} class.
     *
     * @param owner
     *            - The window that owns the license dialog
     */
    public LicenseDialog(Window owner) {
        this.owner = owner;
    }

    /**
     * Shows the bundled license text in a modal dialog owned by the owner window.
     */
    public void showLicenseDialog() {
        JDialog licenseDialog = new JDialog(owner, "License", JDialog.ModalityType.APPLICATION_MODAL);
        licenseDialog.setResizable(false);
        licenseDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        licenseDialog.setLayout(new GridBagLayout());

        // The bundled text is hard-wrapped, so the text area sizes to its content for a DPI-correct fitted dialog
        JTextArea licenseTextArea = new JTextArea(readLicenseText());
        licenseTextArea.setEditable(false);

        // The read-only text is display-only, so drop focusability to suppress the leading caret
        licenseTextArea.setFocusable(false);
        licenseTextArea.setMargin(new Insets(10, 12, 10, 12));

        JButton licenseCloseButton = new JButton("Close");

        // Suppress the focus ring so the initially-focused Close button matches the about dialog's ring-free buttons
        licenseCloseButton.setFocusPainted(false);
        licenseCloseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                licenseDialog.dispose();
            }
        });

        GridBagConstraints licenseConstraints = new GridBagConstraints();
        licenseConstraints.gridx = 0;
        licenseConstraints.gridy = 0;
        licenseConstraints.insets = new Insets(12, 12, 6, 12);
        licenseDialog.add(licenseTextArea, licenseConstraints);

        licenseConstraints.gridy = 1;
        licenseConstraints.insets = new Insets(2, 12, 12, 12);
        licenseDialog.add(licenseCloseButton, licenseConstraints);

        licenseDialog.pack();
        licenseDialog.setLocationRelativeTo(owner);
        licenseDialog.setVisible(true);
    }

    /**
     * Reads the bundled license text from the classpath, normalizing CRLF endings so the text area renders no stray
     * carriage returns.
     *
     * @return The license text, or a short fallback message if the resource cannot be read
     */
    private String readLicenseText() {
        try (InputStream in = LicenseDialog.class.getResourceAsStream(LICENSE_RESOURCE)) {
            if (in == null) {
                return UNAVAILABLE_TEXT;
            }

            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        } catch (IOException e) {
            e.printStackTrace();
            return UNAVAILABLE_TEXT;
        }
    }

}
