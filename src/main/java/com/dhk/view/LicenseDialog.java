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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Window;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.dhk.io.NativeInputHook;
import com.dhk.utility.WindowDragRefitter;
import com.dhk.utility.WindowUtil;

/**
 * Shows the bundled additional terms and license in a modal dialog owned by a parent window. The dialog opens as tall
 * as its owner with the text scrolling, and it is capped to its display's working area as the main frame is.
 *
 * @author Jonathan R. Miller
 */
public class LicenseDialog {

    private Window owner;
    private NativeInputHook nativeInputHook;

    /**
     * Resource path of the GNU General Public License text shown in the dialog.
     */
    private static final String LICENSE_RESOURCE = "/LICENSE.txt";

    /**
     * Resource path of the additional terms, shown ahead of the license because they are short and specific to the app.
     */
    private static final String ADDITIONAL_TERMS_RESOURCE = "/ADDITIONAL_TERMS.txt";

    /**
     * Blank lines placed between the additional terms and the license, so the start of the license stands apart.
     */
    private static final String TEXT_SEPARATOR = "\n\n\n";

    /**
     * Margin in pixels between the dialog's edges and the scrolling text.
     */
    private static final int TEXT_MARGIN = 28;

    /**
     * Text shown in place of a license document when its resource cannot be read.
     */
    private static final String UNAVAILABLE_TEXT = "License text is unavailable.";

    /**
     * Constructor for the {@link LicenseDialog} class.
     *
     * @param owner
     *            - The window that owns the license dialog and whose height it opens at
     * @param nativeInputHook
     *            - The shared native input hook used to re-fit the dialog after a cross-display drag (may be null)
     */
    public LicenseDialog(Window owner, NativeInputHook nativeInputHook) {
        this.owner = owner;
        this.nativeInputHook = nativeInputHook;
    }

    /**
     * Shows the bundled additional terms followed by the license in a modal dialog owned by the owner window.
     */
    public void showLicenseDialog() {
        JDialog licenseDialog = new JDialog(owner, "License", JDialog.ModalityType.APPLICATION_MODAL);
        licenseDialog.setResizable(false);
        licenseDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        String additionalTerms = readLicenseText(ADDITIONAL_TERMS_RESOURCE);
        String licenseText = additionalTerms + TEXT_SEPARATOR + readLicenseText(LICENSE_RESOURCE);

        // The bundled text is hard-wrapped, so the text area sizes its width to the content for a DPI-correct fit
        JTextArea licenseTextArea = new JTextArea(licenseText);
        licenseTextArea.setEditable(false);

        // The read-only text is display-only, so drop focusability to suppress the leading caret
        licenseTextArea.setFocusable(false);
        licenseTextArea.setMargin(new Insets(0, 0, 0, 0));
        licenseTextArea.setCaretPosition(0);

        // The license is far taller than a display, so the viewport shows part of it and the rest scrolls
        JScrollPane licenseScrollPane = new JScrollPane(licenseTextArea);
        licenseScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // A viewport shorter than the text makes the pack count the vertical scroll bar's width alongside the text
        Dimension textSize = licenseTextArea.getPreferredSize();
        int viewportHeight = Math.min(textSize.height, owner.getHeight());
        licenseScrollPane.getViewport().setPreferredSize(new Dimension(textSize.width, viewportHeight));

        // The margin panel lets the scroll pane absorb any size cut, so a capped dialog shrinks the text view only
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(TEXT_MARGIN, TEXT_MARGIN, TEXT_MARGIN, TEXT_MARGIN));
        contentPanel.add(licenseScrollPane, BorderLayout.CENTER);
        licenseDialog.setContentPane(contentPanel);

        licenseDialog.pack();

        // Shift the viewport by the pack's height gap, so this and every later pack land exactly at the owner's height
        int heightGap = owner.getHeight() - licenseDialog.getHeight();
        licenseScrollPane.getViewport().setPreferredSize(new Dimension(textSize.width, viewportHeight + heightGap));
        licenseDialog.pack();

        // The text scrolls on its own, so the shared fit only caps the dialog and reserves no outer scroll bars
        WindowUtil.fitDialogToWorkingArea(licenseDialog);
        updateHorizontalScrolling(licenseDialog, licenseScrollPane);

        // A drag onto another display refreshes the dialog there once the drag ends, matching the main frame
        Runnable refreshAction = () -> refreshForCurrentDisplay(licenseDialog, licenseScrollPane);
        new WindowDragRefitter(licenseDialog, nativeInputHook, refreshAction).install();

        licenseDialog.setVisible(true);
    }

    /**
     * Refreshes the dialog for the display it now occupies through the shared window refresh, then re-checks whether
     * its text has to scroll sideways there.
     *
     * @param licenseDialog
     *            - The showing license dialog to refresh
     * @param licenseScrollPane
     *            - The scroll pane holding the license text
     */
    private void refreshForCurrentDisplay(JDialog licenseDialog, JScrollPane licenseScrollPane) {
        WindowUtil.refreshWindow(licenseDialog);
        updateHorizontalScrolling(licenseDialog, licenseScrollPane);
    }

    /**
     * Lets the text scroll sideways only while the dialog is narrower than its preferred width, so a dialog that fits
     * its display never shows a horizontal scroll bar.
     *
     * @param licenseDialog
     *            - The license dialog whose width to check
     * @param licenseScrollPane
     *            - The scroll pane holding the license text
     */
    private void updateHorizontalScrolling(JDialog licenseDialog, JScrollPane licenseScrollPane) {
        if (licenseDialog.getWidth() < licenseDialog.getPreferredSize().width) {
            licenseScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        } else {
            licenseScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        }
    }

    /**
     * Reads a bundled license document from the classpath, normalizing CRLF endings so the text area renders no stray
     * carriage returns.
     *
     * @param resource
     *            - The classpath resource path of the license document to read
     *
     * @return The document text, or a short fallback message if the resource cannot be read
     */
    private String readLicenseText(String resource) {
        try (InputStream in = LicenseDialog.class.getResourceAsStream(resource)) {
            if (in == null) {
                return UNAVAILABLE_TEXT;
            }

            // Trailing line breaks are dropped so the separator alone sets the gap between the two documents
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n").stripTrailing();
        } catch (IOException e) {
            e.printStackTrace();
            return UNAVAILABLE_TEXT;
        }
    }

}
