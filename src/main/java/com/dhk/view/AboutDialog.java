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

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.dhk.controller.button.PaypalDonateButtonController;
import com.dhk.model.DhkModel;
import com.dhk.model.button.ButtonProperties;
import com.dhk.model.button.ThemeableButton;
import com.dhk.utility.VersionRetriever;

/**
 * Shows an "About Display Hot Keys" dialog on the AWT event dispatching thread. Creates a semi-transparent darkening
 * panel to overlay the parent frame.
 *
 * @author Jonathan R. Miller
 */
public class AboutDialog implements IView {

    private DhkModel model;
    private DhkView view;
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
    private JLabel donateSectionLabel;
    private JSeparator donateSeparator;
    private JButton licenseButton;
    private JButton releasesButton;
    private ThemeableButton paypalDonateButton;
    private PaypalDonateButtonController paypalButtonController;
    private JFrame parentFrame;
    private Component originalGlassPane;
    private Component darkeningGlassPane;
    private boolean dialogClosed;

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
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     */
    public AboutDialog(DhkModel model, DhkView view) {
        this.model = model;
        this.view = view;
        this.parentFrame = view.getFrame();

        originalGlassPane = parentFrame.getGlassPane();
        darkeningGlassPane = createDarkeningGlassPane();
    }

    /**
     * Shows an "About Display Hot Keys" dialog on the AWT event dispatching thread.
     */
    public void showAboutDialog() {
        showAboutDialog(null);
    }

    /**
     * Shows an "About Display Hot Keys" dialog on the AWT event dispatching thread.
     *
     * @param onCloseAction
     *            - The action to run when the dialog closes (not required)
     */
    public void showAboutDialog(Runnable onCloseAction) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Create the dialog and initialize components
                final JDialog aboutDialog = new JDialog(parentFrame, DIALOG_TITLE, true);
                aboutDialog.setResizable(false);

                initAboutPanels(aboutDialog);
                initAboutComponents(aboutDialog);
                initAboutComponentListeners(aboutDialog, onCloseAction);
                addAboutComponents(aboutDialog);

                view.getDefaultFocusComponent().requestFocusInWindow();
                parentFrame.setGlassPane(darkeningGlassPane);
                darkeningGlassPane.setVisible(true);

                // Start off-EDT before the modal setVisible blocks, so the label updates while the dialog is shown
                fetchLatestVersion();

                // Pack and position to fit the info text, then show
                aboutDialog.pack();
                aboutDialog.setLocationRelativeTo(parentFrame);
                aboutDialog.setVisible(true);
            }
        });

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                getDefaultFocusComponent().requestFocusInWindow();
            }
        });
    }

    @Override
    public Component getDefaultFocusComponent() {
        return versionLabel;
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
     * @param aboutDialog
     *            - The about dialog to initialize panels for
     */
    private void initAboutPanels(JDialog aboutDialog) {
        mainLayout = new GridBagLayout();
        aboutDialog.setLayout(mainLayout);

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
     *
     * @param aboutDialog
     *            - The about dialog to initialize components for
     */
    private void initAboutComponents(JDialog aboutDialog) {
        versionSectionLabel = new JLabel("Version", SwingConstants.CENTER);
        versionSectionLabel.putClientProperty("FlatLaf.styleClass", "h4");
        versionSeparator = new JSeparator();

        versionLabel = new JLabel("Current: " + VersionRetriever.getVersion(), SwingConstants.CENTER);

        // Placeholder text is the widest state, so the async result never clips in the non-resizable dialog
        latestVersionLabel = new JLabel("Latest: checking…", SwingConstants.CENTER);

        developerSectionLabel = new JLabel("Developer", SwingConstants.CENTER);
        developerSectionLabel.putClientProperty("FlatLaf.styleClass", "h4");
        developerSeparator = new JSeparator();

        developedByLabel = new JLabel("Jonathan R. Miller", SwingConstants.CENTER);

        donateSectionLabel = new JLabel("Donate", SwingConstants.CENTER);
        donateSectionLabel.putClientProperty("FlatLaf.styleClass", "h4");
        donateSeparator = new JSeparator();

        ButtonProperties paypalButtonProperties = new ButtonProperties(null, new Dimension(134, 46), 0.70f, 0.63f);
        paypalDonateButton = new ThemeableButton("/paypal_donate_light_idle.svg", "/paypal_donate_light_hover.svg",
                "/paypal_donate_dark_idle.svg", "/paypal_donate_dark_hover.svg", paypalButtonProperties, true,
                model.isDarkMode());

        paypalButtonController = new PaypalDonateButtonController(AboutDialog.this, paypalDonateButton);
        licenseButton = new JButton("License");
        licenseButton.setFocusPainted(false);
        releasesButton = new JButton("Releases");
        releasesButton.setFocusPainted(false);
    }

    /**
     * Initializes listeners for the components in an about dialog.
     *
     * @param aboutDialog
     *            - The about dialog to initialize listeners for
     * @param onCloseAction
     *            - The action to run when the dialog closes (not required)
     */
    private void initAboutComponentListeners(final JDialog aboutDialog, final Runnable onCloseAction) {
        paypalButtonController.initListeners();

        licenseButton.addActionListener(createLicenseActionListener(aboutDialog));
        releasesButton.addActionListener(createReleasesActionListener());

        // Prevent default close and handle the title-bar close ourselves
        aboutDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        aboutDialog.addWindowListener(new WindowAdapter() {
            // Ensure the dialog takes the foreground after opening from the tray
            @Override
            public void windowOpened(WindowEvent e) {
                aboutDialog.toFront();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                closeDialog(aboutDialog, onCloseAction);
            }

            // A re-initialization disposes the parent frame, destroying this dialog without any close request
            @Override
            public void windowClosed(WindowEvent e) {
                closeDialog(aboutDialog, onCloseAction);
            }
        });

        aboutDialog.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent mousePressedEvent) {
                getDefaultFocusComponent().requestFocusInWindow();
            }
        });
    }

    /**
     * Adds components to an about dialog.
     *
     * @param aboutDialog
     *            - The about dialog to add components to
     */
    private void addAboutComponents(JDialog aboutDialog) {
        // An edge adds no padding of its own, so only the padding of the text or button beside it counts toward the gap
        int edgeInset = SECTION_GAP - INNER_PADDING;

        // A heading below text has padding on both sides of its gap, so both are taken out of its inset
        int sectionInset = SECTION_GAP - 2 * INNER_PADDING;

        // The PayPal icon is drawn smaller than its button, so the blank margin around it counts toward the gap below
        int paypalButtonHeight = paypalDonateButton.getPreferredSize().height;
        int paypalIconMargin = (paypalButtonHeight - paypalDonateButton.getIcon().getIconHeight()) / 2;

        buttonPanelConstraints.insets = new Insets(SECTION_GAP - INNER_PADDING - paypalIconMargin, 8, edgeInset, 8);

        buttonPanelConstraints.gridx = 0;
        buttonPanelConstraints.gridy = 0;
        buttonPanel.add(licenseButton, buttonPanelConstraints);

        buttonPanelConstraints.gridx = 1;
        buttonPanelConstraints.gridy = 0;
        buttonPanel.add(releasesButton, buttonPanelConstraints);

        addInfo(infoPanel, infoPanelConstraints, versionSectionLabel, 0, 0, 1, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(edgeInset, 8, 4, 8));
        addInfo(infoPanel, infoPanelConstraints, versionSeparator, 0, 1, 1, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, 8, 4, 8));
        addInfo(infoPanel, infoPanelConstraints, versionLabel, 0, 2, 1, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(4, 8, 2, 8));
        addInfo(infoPanel, infoPanelConstraints, latestVersionLabel, 0, 3, 1, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(2, 8, 0, 8));

        addInfo(infoPanel, infoPanelConstraints, developerSectionLabel, 0, 4, 1, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(sectionInset, 8, 4, 8));
        addInfo(infoPanel, infoPanelConstraints, developerSeparator, 0, 5, 1, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, 8, 4, 8));
        addInfo(infoPanel, infoPanelConstraints, developedByLabel, 0, 6, 1, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(4, 8, 0, 8));

        addInfo(infoPanel, infoPanelConstraints, donateSectionLabel, 0, 7, 1, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(sectionInset, 8, 4, 8));
        addInfo(infoPanel, infoPanelConstraints, donateSeparator, 0, 8, 1, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, 8, 4, 8));
        addInfo(infoPanel, infoPanelConstraints, paypalDonateButton, 0, 9, 1, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(4, 8, 0, 8));

        mainConstraints.gridx = 0;
        mainConstraints.gridy = 0;
        mainConstraints.fill = GridBagConstraints.HORIZONTAL;
        aboutDialog.add(infoPanel, mainConstraints);

        mainConstraints.gridx = 0;
        mainConstraints.gridy = 1;
        mainConstraints.fill = GridBagConstraints.NONE;
        aboutDialog.add(buttonPanel, mainConstraints);
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
                        new LicenseDialog(aboutDialog).showLicenseDialog();
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

    /**
     * Closes an about dialog.
     *
     * @param aboutDialog
     *            - The about dialog to close
     * @param onCloseAction
     *            - The action to run when the dialog closes (not required)
     */
    private void closeDialog(JDialog aboutDialog, Runnable onCloseAction) {
        // Disposing fires the close path again, and the title-bar close can land after it has already run
        if (dialogClosed) {
            return;
        }

        dialogClosed = true;

        getDefaultFocusComponent().requestFocusInWindow();

        if (onCloseAction != null) {
            onCloseAction.run();
        }

        darkeningGlassPane.setVisible(false);
        parentFrame.setGlassPane(originalGlassPane);

        aboutDialog.dispose();
    }

    /**
     * Creates a semi-transparent darkening panel to overlay the parent frame.
     *
     * @return A semi-transparent darkening panel to overlay the parent frame
     */
    private Component createDarkeningGlassPane() {
        JPanel darkeningPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphics) {
                graphics.setColor(new Color(0, 0, 0, 110));
                graphics.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        darkeningPanel.setOpaque(false);
        darkeningPanel.addMouseListener(new MouseAdapter() {
        });

        return darkeningPanel;
    }

}
