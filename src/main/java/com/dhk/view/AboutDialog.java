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
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.dhk.controller.button.PaypalDonateButtonController;
import com.dhk.model.DhkModel;
import com.dhk.model.button.ButtonProperties;
import com.dhk.model.button.ThemeableButton;
import com.dhk.utility.FrameUtil;
import com.dhk.utility.VersionRetriever;

import dorkbox.systemTray.SystemTray;

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
    private JLabel headerLabel;
    private JLabel versionLabel;
    private JLabel developedByLabel;
    private JButton licenseButton;
    private JButton releasesButton;
    private JButton closeButton;
    private ThemeableButton paypalDonateButton;
    private PaypalDonateButtonController paypalButtonController;
    private JFrame parentFrame;
    private Component originalGlassPane;
    private Component darkeningGlassPane;

    private static final String GITHUB_DOMAIN = "https://github.com";
    private static final String GITHUB_CONTENT_DOMAIN = "https://raw.githubusercontent.com";
    private static final String LICENSE_PATH = "/jon-mil-92/DisplayHotKeys/refs/heads/main/LICENSE.txt";
    private static final String REALEASES_PATH = "/jon-mil-92/DisplayHotKeys/releases";
    private static final String LICENSE_LINK = GITHUB_CONTENT_DOMAIN + LICENSE_PATH;
    private static final String RELEASES_LINK = GITHUB_DOMAIN + REALEASES_PATH;
    private static final float HEADER_FONT_SCALE = 1.6f;

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
     * @param systemTray
     *            - The system tray (not required)
     */
    public void showAboutDialog(SystemTray systemTray) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Create the dialog and initialize components
                final JDialog aboutDialog = new JDialog(parentFrame, true);
                aboutDialog.setResizable(false);

                initAboutPanels(aboutDialog);
                initAboutComponents(aboutDialog, systemTray);
                initAboutComponentListeners(aboutDialog, systemTray);
                addAboutComponents(aboutDialog);

                // Update UI and show darkening glass pane on parent
                FrameUtil.refreshFrame(parentFrame);
                view.getDefaultFocusComponent().requestFocusInWindow();
                parentFrame.setGlassPane(darkeningGlassPane);
                darkeningGlassPane.setVisible(true);

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
        return headerLabel;
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
        mainConstraints.insets = new Insets(7, 19, 7, 19);
        mainConstraints.anchor = GridBagConstraints.CENTER;
        mainConstraints.gridwidth = 1;

        buttonPanel = new JPanel();
        buttonPanelLayout = new GridBagLayout();
        buttonPanel.setLayout(buttonPanelLayout);

        buttonPanelConstraints = new GridBagConstraints();
        buttonPanelConstraints.insets = new Insets(2, 7, 10, 7);
        buttonPanelConstraints.anchor = GridBagConstraints.CENTER;
        buttonPanelConstraints.gridwidth = 1;

        infoPanel = new JPanel();
        infoPanelLayout = new GridBagLayout();
        infoPanel.setLayout(infoPanelLayout);

        infoPanelConstraints = new GridBagConstraints();
        infoPanelConstraints.insets = new Insets(3, 7, 3, 7);
        infoPanelConstraints.anchor = GridBagConstraints.CENTER;
        infoPanelConstraints.gridwidth = 1;
    }

    /**
     * Initializes components for an about dialog.
     *
     * @param aboutDialog
     *            - The about dialog to initialize components for
     * @param systemTray
     *            - The system tray (not required)
     */
    private void initAboutComponents(JDialog aboutDialog, SystemTray systemTray) {
        headerLabel = new JLabel("About Display Hot Keys");
        headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerLabel.putClientProperty("FlatLaf.style", "font: bold " + Math.round(HEADER_FONT_SCALE * 100) + "%");

        versionLabel = new JLabel("Version: " + VersionRetriever.getVersion());
        versionLabel.setHorizontalAlignment(SwingConstants.CENTER);

        developedByLabel = new JLabel("Developed by Jonathan R. Miller");
        developedByLabel.setHorizontalAlignment(SwingConstants.CENTER);

        ButtonProperties paypalButtonProperties = new ButtonProperties(null, new Dimension(134, 46), 0.70f, 0.63f);
        paypalDonateButton = new ThemeableButton("/paypal_donate_light_idle.svg", "/paypal_donate_light_hover.svg",
                "/paypal_donate_dark_idle.svg", "/paypal_donate_dark_hover.svg", paypalButtonProperties, true,
                model.isDarkMode());

        paypalButtonController = new PaypalDonateButtonController(AboutDialog.this, paypalDonateButton);
        licenseButton = new JButton("License");
        releasesButton = new JButton("Releases");
        closeButton = new JButton("Close");
    }

    /**
     * Initializes listeners for the components in an about dialog.
     *
     * @param aboutDialog
     *            - The about dialog to initialize listeners for
     * @param systemTray
     *            - The system tray (not required)
     */
    private void initAboutComponentListeners(final JDialog aboutDialog, final SystemTray systemTray) {
        paypalButtonController.initListeners();

        licenseButton.addActionListener(createLicenseActionListener());
        releasesButton.addActionListener(createReleasesActionListener());
        closeButton.addActionListener(createCloseActionListener(aboutDialog, systemTray));

        licenseButton.addMouseListener(createMouseAdapter());
        releasesButton.addMouseListener(createMouseAdapter());
        closeButton.addMouseListener(createMouseAdapter());

        // Prevent default close and handle the title-bar close ourselves
        aboutDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        aboutDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Call the same method used by the Close button
                closeButtonAction(aboutDialog, systemTray);
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
        buttonPanelConstraints.gridx = 0;
        buttonPanelConstraints.gridy = 0;
        buttonPanel.add(licenseButton, buttonPanelConstraints);

        buttonPanelConstraints.gridx = 1;
        buttonPanelConstraints.gridy = 0;
        buttonPanel.add(releasesButton, buttonPanelConstraints);

        buttonPanelConstraints.gridx = 2;
        buttonPanelConstraints.gridy = 0;
        buttonPanel.add(closeButton, buttonPanelConstraints);

        infoPanelConstraints.gridx = 0;
        infoPanelConstraints.gridy = 0;
        infoPanel.add(headerLabel, infoPanelConstraints);

        infoPanelConstraints.gridx = 0;
        infoPanelConstraints.gridy = 1;
        infoPanel.add(versionLabel, infoPanelConstraints);

        infoPanelConstraints.gridx = 0;
        infoPanelConstraints.gridy = 2;
        infoPanel.add(developedByLabel, infoPanelConstraints);

        mainConstraints.gridx = 0;
        mainConstraints.gridy = 0;
        aboutDialog.add(infoPanel, mainConstraints);

        mainConstraints.gridx = 0;
        mainConstraints.gridy = 1;
        aboutDialog.add(paypalDonateButton, mainConstraints);

        mainConstraints.gridx = 0;
        mainConstraints.gridy = 2;
        aboutDialog.add(buttonPanel, mainConstraints);
    }

    /**
     * Creates an action listener that opens the license link.
     *
     * @return An action listener that opens the license link
     */
    private ActionListener createLicenseActionListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openLinkButtonAction(LICENSE_LINK);
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
     * Creates an action listener that closes an about dialog.
     *
     * @param aboutDialog
     *            - The about dialog to close
     * @param systemTray
     *            - The system tray to re-enable (not required)
     *
     * @return An action listener that closes an about dialog
     */
    private ActionListener createCloseActionListener(JDialog aboutDialog, SystemTray systemTray) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeButtonAction(aboutDialog, systemTray);
            }
        };
    }

    /**
     * Creates a mouse adapter that gives focus to the default focus component upon mouse exit.
     *
     * @return A mouse adapter that gives focus to the default focus component upon mouse exit
     */
    private MouseAdapter createMouseAdapter() {
        return new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                getDefaultFocusComponent().requestFocusInWindow();
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
     * @param systemTray
     *            - The system tray to re-enable (not required)
     */
    private void closeButtonAction(JDialog aboutDialog, SystemTray systemTray) {
        getDefaultFocusComponent().requestFocusInWindow();

        if (systemTray != null) {
            systemTray.setEnabled(true);
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
