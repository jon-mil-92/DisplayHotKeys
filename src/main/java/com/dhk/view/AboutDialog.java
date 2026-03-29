package com.dhk.view;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.*;
import com.dhk.controller.button.PaypalDonateButtonController;
import com.dhk.model.DhkModel;
import com.dhk.model.button.ButtonProperties;
import com.dhk.model.button.ThemeableButton;
import com.dhk.utility.VersionRetriever;
import dorkbox.systemTray.SystemTray;

/**
 * Shows an "About Display Hot Keys" dialog on the AWT event dispatching thread.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2026 Jonathan Miller
 */
public class AboutDialog extends AbstractDraggableDialog {

    private DhkModel model;
    private GridBagLayout mainLayout;
    private GridBagConstraints mainConstraints;
    private JPanel buttonPanel;
    private GridBagLayout buttonPanelLayout;
    private GridBagConstraints buttonPanelConstraints;
    private JLabel infoLabel;
    private JButton releasesButton;
    private JButton okButton;
    private ThemeableButton paypalDonateButton;
    private PaypalDonateButtonController paypalButtonController;
    private JPanel darkeningGlassPane;

    private static final String LINK_DOMAIN = "https://github.com";
    private static final String LINK_PATH = "/jon-mil-92/DisplayHotKeys/releases";
    private static final String RELEASES_LINK = LINK_DOMAIN + LINK_PATH;

    /**
     * Constructor for the {@link AboutDialog} class.
     * 
     * @param parentFrame
     *            - The parent frame for the about dialog
     * @param model
     *            - The model for the application
     */
    public AboutDialog(JFrame parentFrame, DhkModel model) {
        super(parentFrame);
        this.model = model;
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
                JDialog aboutDialog = new JDialog(parentFrame, "About Display Hot Keys", true);
                initMouseListeners(aboutDialog);
                aboutDialog.setUndecorated(true);
                aboutDialog.setResizable(false);

                initPanels(aboutDialog);
                initComponents(aboutDialog, systemTray);
                addComponents(aboutDialog);

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
        return infoLabel;
    }

    /**
     * Creates a semi-transparent darkening panel to overlay the parent frame.
     * 
     * @return A semi-transparent darkening panel to overlay the parent frame
     */
    private JPanel createDarkeningGlassPane() {
        JPanel gp = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(0, 0, 0, 120));
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };

        gp.setOpaque(false);
        gp.addMouseListener(new MouseAdapter() {
        });

        return gp;
    }

    /**
     * Initializes all of the panels that will hold the view components.
     * 
     * @param aboutDialog
     *            - The about dialog to initialize panels for
     */
    private void initPanels(JDialog aboutDialog) {
        mainLayout = new GridBagLayout();
        aboutDialog.setLayout(mainLayout);

        mainConstraints = new GridBagConstraints();
        mainConstraints.insets = new Insets(10, 20, 10, 20);
        mainConstraints.anchor = GridBagConstraints.CENTER;
        mainConstraints.gridwidth = 1;

        buttonPanel = new JPanel();
        buttonPanelLayout = new GridBagLayout();
        buttonPanel.setLayout(buttonPanelLayout);

        buttonPanelConstraints = new GridBagConstraints();
        buttonPanelConstraints.insets = new Insets(0, 8, 0, 8);
        buttonPanelConstraints.anchor = GridBagConstraints.CENTER;
        buttonPanelConstraints.gridwidth = 1;
    }

    /**
     * Initializes the components for an about dialog.
     * 
     * @param aboutDialog
     *            - The about dialog to initialize components for
     * @param systemTray
     *            - The system tray (not required)
     */
    private void initComponents(JDialog aboutDialog, SystemTray systemTray) {
        Component originalGlassPane = parentFrame.getGlassPane();
        parentFrame.setGlassPane(darkeningGlassPane);
        darkeningGlassPane.setVisible(true);

        infoLabel = new JLabel(buildAboutInfoHtml());
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        ButtonProperties paypalButtonProperties = new ButtonProperties(null, new Dimension(134, 46), 0.70f, 0.63f);
        paypalDonateButton = new ThemeableButton("/paypal_donate_light_idle.svg", "/paypal_donate_light_hover.svg",
                "/paypal_donate_dark_idle.svg", "/paypal_donate_dark_hover.svg", paypalButtonProperties, true,
                model.isDarkMode());

        paypalButtonController = new PaypalDonateButtonController(AboutDialog.this, paypalDonateButton);
        paypalButtonController.initListeners();

        releasesButton = new JButton("Releases");
        releasesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                releasesButtonAction();
            }
        });

        okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                okButtonAction(aboutDialog, systemTray, originalGlassPane, darkeningGlassPane);
            }
        });
    }

    /**
     * Adds the components to an about dialog.
     * 
     * @param aboutDialog
     *            - The about dialog to add components to
     */
    private void addComponents(JDialog aboutDialog) {
        buttonPanelConstraints.gridx = 0;
        buttonPanelConstraints.gridy = 0;
        buttonPanel.add(releasesButton, buttonPanelConstraints);

        buttonPanelConstraints.gridx = 1;
        buttonPanelConstraints.gridy = 0;
        buttonPanel.add(okButton, buttonPanelConstraints);

        mainConstraints.gridx = 0;
        mainConstraints.gridy = 0;
        aboutDialog.add(infoLabel, mainConstraints);

        mainConstraints.gridx = 0;
        mainConstraints.gridy = 1;
        aboutDialog.add(paypalDonateButton, mainConstraints);

        mainConstraints.gridx = 0;
        mainConstraints.gridy = 2;
        aboutDialog.add(buttonPanel, mainConstraints);
    }

    /**
     * Builds an HTML string for information about the application.
     * 
     * @return An HTML string for information about the application
     */
    private String buildAboutInfoHtml() {
        String htmlBegin = "<html><div style=\"text-align: center;\">";
        String header = "<h1>About Display Hot Keys</h1>";
        String version = "<p>Version: " + VersionRetriever.getVersion() + "</p>";
        String developedBy = "<p>Developed by Jonathan R. Miller</p>";
        String htmlEnd = "</div></html>";

        return htmlBegin + header + version + developedBy + htmlEnd;
    }

    /**
     * Opens up the GitHub Releases page for Display Hot Keys in the user's default web browser.
     */
    private void releasesButtonAction() {
        try {
            Desktop.getDesktop().browse(new URI(RELEASES_LINK));
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                getDefaultFocusComponent().requestFocusInWindow();
            }
        });
    }

    /**
     * Re-enables the system tray, removes the darkening glass pane from the parent frame, and disposes this dialog.
     * 
     * @param aboutDialog
     *            - The about dialog to dispose of
     * @param systemTray
     *            - The system tray to re-enable
     * @param originalGlassPane
     *            - The original glass pane for the parent frame
     * @param glassPan
     *            - The darkening glass pane for the parent frame
     */
    private void okButtonAction(JDialog aboutDialog, SystemTray systemTray, Component originalGlassPane,
            JPanel darkeningGlassPane) {
        if (systemTray != null) {
            systemTray.setEnabled(true);
        }

        darkeningGlassPane.setVisible(false);
        parentFrame.setGlassPane(originalGlassPane);

        aboutDialog.dispose();
    }

}
