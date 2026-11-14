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
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.dhk.controller.button.DonateButtonController;
import com.dhk.io.NativeInputHook;
import com.dhk.utility.VersionRetriever;

/**
 * Shows an "About Display Hot Keys" dialog over the darkened main frame, with the version, the original developer, and
 * the legal notices.
 *
 * @author Jonathan R. Miller
 */
public class AboutDialog extends OverlayDialog {

    private GridBagLayout mainLayout;
    private GridBagConstraints mainConstraints;
    private JPanel buttonPanel;
    private GridBagLayout buttonPanelLayout;
    private GridBagConstraints buttonPanelConstraints;
    private JPanel infoPanel;
    private GridBagLayout infoPanelLayout;
    private GridBagConstraints infoPanelConstraints;
    private JLabel versionSectionLabel;
    private JSeparator versionSeparator;
    private JLabel versionLabel;
    private JLabel latestVersionLabel;
    private JLabel developerSectionLabel;
    private JSeparator developerSeparator;
    private JLabel developedByLabel;
    private JLabel legalSectionLabel;
    private JSeparator legalSeparator;
    private JLabel legalNoticesLabel;
    private JButton licenseButton;
    private JButton donateButton;
    private JButton releasesButton;

    /**
     * Title shown in the dialog's title bar.
     */
    private static final String DIALOG_TITLE = "About App";

    /**
     * Domain of the GitHub releases link.
     */
    private static final String GITHUB_DOMAIN = "https://github.com";

    /**
     * Path of the application's GitHub releases page.
     */
    private static final String RELEASES_PATH = "/jon-mil-92/DisplayHotKeys/releases";

    /**
     * Link the releases button opens.
     */
    private static final String RELEASES_LINK = GITHUB_DOMAIN + RELEASES_PATH;

    /**
     * Appropriate Legal Notices the GNU General Public License requires the app to display, including the notice the
     * additional terms require every modified version to keep word for word. The body width wraps them into a column.
     */
    private static final String LEGAL_NOTICES = "<html><body style='width: 320px'>Copyright © 2024-2026 Jonathan R. "
            + "Miller<br><br>Display Hot Keys is free software. You may redistribute it and modify it under the terms "
            + "of version 3 of the GNU General Public License, together with the additional terms under section 7 of "
            + "that License.<br><br>Display Hot Keys comes with ABSOLUTELY NO WARRANTY, to the extent permitted by "
            + "applicable law.<br><br>Modified versions are not endorsed by Jonathan R. Miller, who is not responsible "
            + "or liable for them.<br><br>Select License to view the GNU General Public License and the additional "
            + "terms.</body></html>";

    /**
     * Visible space above the first section, between sections, and below the buttons.
     */
    private static final int SECTION_GAP = 24;

    /**
     * Space a label's text or a button's border leaves inside the component, which already counts toward a visible gap
     * and is subtracted from the inset beside it.
     */
    private static final int INNER_PADDING = 2;

    /**
     * Constructor for the {@link AboutDialog} class.
     *
     * @param view
     *            - The view for the application
     * @param nativeInputHook
     *            - The shared native input hook used to re-fit the dialog after a cross-display drag (may be null)
     */
    public AboutDialog(DhkView view, NativeInputHook nativeInputHook) {
        super(view, nativeInputHook, DIALOG_TITLE);
    }

    /**
     * Shows an "About Display Hot Keys" dialog on the AWT event dispatching thread.
     *
     * @param onCloseAction
     *            - The action to run when the dialog closes (not required)
     */
    public void showAboutDialog(Runnable onCloseAction) {
        showDialog(onCloseAction);
    }

    @Override
    public Component getDefaultFocusComponent() {
        return versionLabel;
    }

    @Override
    protected void buildContent(JDialog aboutDialog, JPanel contentPanel) {
        initAboutPanels(contentPanel);
        initAboutComponents();
        initAboutComponentListeners(aboutDialog);
        addAboutComponents(contentPanel);
    }

    @Override
    protected void beforeShow() {
        // Start off-EDT before the modal setVisible blocks, so the label updates while the dialog is shown
        fetchLatestVersion();
    }

    /**
     * Fetches the latest released version on a background daemon thread and updates the latest-version label on the AWT
     * event dispatching thread, keeping the blocking network request off the AWT event dispatching thread.
     */
    private void fetchLatestVersion() {
        Thread fetchThread = new Thread(new Runnable() {
            @Override
            public void run() {
                final String latestVersion = VersionRetriever.getLatestVersion();

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        // The fetch outlives the dialog, which a close or re-initialization may already have released
                        if (latestVersionLabel == null || !latestVersionLabel.isDisplayable()) {
                            return;
                        }

                        latestVersionLabel.setText("Latest: " + latestVersion);

                        // Emphasize the label when a newer release is available than the current version
                        if (isNewerVersion(latestVersion, VersionRetriever.getVersion())) {
                            latestVersionLabel.putClientProperty("FlatLaf.style", "font: bold; foreground: #00993f");
                        }
                    }
                });
            }
        }, "dhk-latest-version-fetch");

        fetchThread.setDaemon(true);
        fetchThread.start();
    }

    /**
     * Determines whether the latest released version is greater than the current version. An unknown or malformed
     * version on either side is treated as not newer, so the label is emphasized only on a confident comparison.
     *
     * @param latestVersion
     *            - The latest released version in "MAJOR.MINOR.PATCH" form
     * @param currentVersion
     *            - The current version in "MAJOR.MINOR.PATCH" form
     *
     * @return True if the latest version is greater than the current version, otherwise false
     */
    private boolean isNewerVersion(String latestVersion, String currentVersion) {
        String[] latestParts = latestVersion.split("\\.");
        String[] currentParts = currentVersion.split("\\.");

        if (latestParts.length != 3 || currentParts.length != 3) {
            return false;
        }

        try {
            for (int componentIndex = 0; componentIndex < 3; componentIndex++) {
                int latestComponent = Integer.parseInt(latestParts[componentIndex]);
                int currentComponent = Integer.parseInt(currentParts[componentIndex]);

                if (latestComponent != currentComponent) {
                    return latestComponent > currentComponent;
                }
            }
        } catch (NumberFormatException ex) {
            return false;
        }

        return false;
    }

    /**
     * Initializes panels for an about dialog.
     *
     * @param contentPanel
     *            - The dialog's scrolling content panel to lay the sections out in
     */
    private void initAboutPanels(JPanel contentPanel) {
        mainLayout = new GridBagLayout();
        contentPanel.setLayout(mainLayout);

        mainConstraints = new GridBagConstraints();
        mainConstraints.insets = new Insets(0, 20, 0, 20);
        mainConstraints.anchor = GridBagConstraints.CENTER;
        mainConstraints.gridwidth = 1;

        buttonPanel = new JPanel();
        buttonPanelLayout = new GridBagLayout();
        buttonPanel.setLayout(buttonPanelLayout);

        buttonPanelConstraints = new GridBagConstraints();
        buttonPanelConstraints.anchor = GridBagConstraints.CENTER;
        buttonPanelConstraints.gridwidth = 1;

        infoPanel = new JPanel();
        infoPanelLayout = new GridBagLayout();
        infoPanel.setLayout(infoPanelLayout);

        // The single column takes the panel's full width, so each section's separator spans the dialog
        infoPanelConstraints = new GridBagConstraints();
        infoPanelConstraints.weightx = 1;
    }

    /**
     * Initializes components for an about dialog.
     */
    private void initAboutComponents() {
        versionSectionLabel = new JLabel("Version", SwingConstants.LEFT);
        versionSectionLabel.putClientProperty("FlatLaf.styleClass", "h4");
        versionSeparator = new JSeparator();

        versionLabel = new JLabel("Current: " + VersionRetriever.getVersion(), SwingConstants.LEFT);

        // Placeholder text is the widest state, so the async result never clips in the non-resizable dialog
        latestVersionLabel = new JLabel("Latest: checking…", SwingConstants.LEFT);

        developerSectionLabel = new JLabel("Original Developer", SwingConstants.LEFT);
        developerSectionLabel.putClientProperty("FlatLaf.styleClass", "h4");
        developerSeparator = new JSeparator();

        developedByLabel = new JLabel("Jonathan R. Miller", SwingConstants.LEFT);

        legalSectionLabel = new JLabel("Legal Notices", SwingConstants.LEFT);
        legalSectionLabel.putClientProperty("FlatLaf.styleClass", "h4");
        legalSeparator = new JSeparator();

        legalNoticesLabel = new JLabel(LEGAL_NOTICES, SwingConstants.LEFT);

        licenseButton = new JButton("License");
        licenseButton.setFocusPainted(false);
        donateButton = new JButton("Donate");
        donateButton.setFocusPainted(false);
        releasesButton = new JButton("Releases");
        releasesButton.setFocusPainted(false);
    }

    /**
     * Initializes listeners for the components in an about dialog.
     *
     * @param aboutDialog
     *            - The about dialog that owns the license dialog its License button opens
     */
    private void initAboutComponentListeners(JDialog aboutDialog) {
        licenseButton.addActionListener(createLicenseActionListener(aboutDialog));
        releasesButton.addActionListener(createReleasesActionListener());

        // The donation provider lives entirely in this controller, so switching providers never touches the dialog
        DonateButtonController donateButtonController = new DonateButtonController(this, donateButton);
        donateButtonController.initController();
        donateButtonController.initListeners();
    }

    /**
     * Adds components to an about dialog.
     *
     * @param contentPanel
     *            - The dialog's scrolling content panel to add the sections to
     */
    private void addAboutComponents(JPanel contentPanel) {
        // An edge adds no padding of its own, so only the padding of the text or button beside it counts toward the gap
        int edgeInset = SECTION_GAP - INNER_PADDING;

        // A heading below text has padding on both sides of its gap, so both are taken out of its inset
        int sectionInset = SECTION_GAP - 2 * INNER_PADDING;

        // The buttons sit below text, so the text's padding and the buttons' border padding both count toward the gap
        buttonPanelConstraints.insets = new Insets(sectionInset, 8, edgeInset, 8);

        buttonPanelConstraints.gridx = 0;
        buttonPanelConstraints.gridy = 0;
        buttonPanel.add(licenseButton, buttonPanelConstraints);

        buttonPanelConstraints.gridx = 1;
        buttonPanelConstraints.gridy = 0;
        buttonPanel.add(donateButton, buttonPanelConstraints);

        buttonPanelConstraints.gridx = 2;
        buttonPanelConstraints.gridy = 0;
        buttonPanel.add(releasesButton, buttonPanelConstraints);

        addInfo(infoPanel, infoPanelConstraints, versionSectionLabel, 0, 0, 1, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(edgeInset, 8, 4, 8));
        addInfo(infoPanel, infoPanelConstraints, versionSeparator, 0, 1, 1, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, 8, 4, 8));
        addInfo(infoPanel, infoPanelConstraints, versionLabel, 0, 2, 1, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(4, 8, 2, 8));
        addInfo(infoPanel, infoPanelConstraints, latestVersionLabel, 0, 3, 1, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(2, 8, 0, 8));

        addInfo(infoPanel, infoPanelConstraints, developerSectionLabel, 0, 4, 1, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(sectionInset, 8, 4, 8));
        addInfo(infoPanel, infoPanelConstraints, developerSeparator, 0, 5, 1, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, 8, 4, 8));
        addInfo(infoPanel, infoPanelConstraints, developedByLabel, 0, 6, 1, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(4, 8, 0, 8));

        addInfo(infoPanel, infoPanelConstraints, legalSectionLabel, 0, 7, 1, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(sectionInset, 8, 4, 8));
        addInfo(infoPanel, infoPanelConstraints, legalSeparator, 0, 8, 1, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, 8, 4, 8));
        addInfo(infoPanel, infoPanelConstraints, legalNoticesLabel, 0, 9, 1, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(4, 8, 0, 8));

        mainConstraints.gridx = 0;
        mainConstraints.gridy = 0;
        mainConstraints.fill = GridBagConstraints.HORIZONTAL;
        contentPanel.add(infoPanel, mainConstraints);

        mainConstraints.gridx = 0;
        mainConstraints.gridy = 1;
        mainConstraints.fill = GridBagConstraints.NONE;
        contentPanel.add(buttonPanel, mainConstraints);
    }

    /**
     * Adds a component to the given panel at the given grid cell, with its own span, alignment, fill, and spacing.
     *
     * @param panel
     *            - The panel to add the component to
     * @param constraints
     *            - The constraints to fill in for the component, which the panel copies when the component is added
     * @param component
     *            - The component to add
     * @param gridx
     *            - The column the component starts in
     * @param gridy
     *            - The row the component is in
     * @param gridwidth
     *            - The number of columns the component spans
     * @param anchor
     *            - Where the component sits within its cell
     * @param fill
     *            - How the component stretches to fill its cell
     * @param insets
     *            - The space around the component
     */
    private void addInfo(JPanel panel, GridBagConstraints constraints, Component component, int gridx, int gridy,
            int gridwidth, int anchor, int fill, Insets insets) {
        constraints.gridx = gridx;
        constraints.gridy = gridy;
        constraints.gridwidth = gridwidth;
        constraints.anchor = anchor;
        constraints.fill = fill;
        constraints.insets = insets;

        panel.add(component, constraints);
    }

    /**
     * Creates an action listener that shows the bundled license text in its own dialog.
     *
     * @param aboutDialog
     *            - The about dialog that owns the license dialog
     *
     * @return An action listener that shows the license dialog
     */
    private ActionListener createLicenseActionListener(JDialog aboutDialog) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // The modal grabs input before a mouse-exit fires, so clear the button's rollover highlight first
                licenseButton.getModel().setRollover(false);
                getDefaultFocusComponent().requestFocusInWindow();

                // Defer the modal show so the button finishes its mouse-release repaint before the modal blocks
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        new LicenseDialog(aboutDialog, getNativeInputHook()).showLicenseDialog();
                    }
                });
            }
        };
    }

    /**
     * Creates an action listener that opens the releases link.
     *
     * @return An action listener that opens the releases link
     */
    private ActionListener createReleasesActionListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openLinkButtonAction(RELEASES_LINK);
            }
        };
    }

    /**
     * Opens up a link in the user's default web browser.
     *
     * @param link
     *            - The link to open
     */
    private void openLinkButtonAction(String link) {
        getDefaultFocusComponent().requestFocusInWindow();

        try {
            Desktop.getDesktop().browse(new URI(link));
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

}
