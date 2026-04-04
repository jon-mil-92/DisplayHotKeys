package com.dhk.view;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    private DhkView view;
    private GridBagLayout mainLayout;
    private GridBagConstraints mainConstraints;
    private JPanel buttonPanel;
    private GridBagLayout buttonPanelLayout;
    private GridBagConstraints buttonPanelConstraints;
    private JLabel infoLabel;
    private JButton licenseButton;
    private JButton releasesButton;
    private JButton closeButton;
    private ThemeableButton paypalDonateButton;
    private PaypalDonateButtonController paypalButtonController;
    private Component originalGlassPane;
    private Component darkeningGlassPane;
    private FrameUpdater frameUpdater;

    private static final String LICENSE_LINK = "https://mit-license.org";
    private static final String GITHUB_DOMAIN = "https://github.com";
    private static final String REALEASES_PATH = "/jon-mil-92/DisplayHotKeys/releases";
    private static final String RELEASES_LINK = GITHUB_DOMAIN + REALEASES_PATH;

    /**
     * Constructor for the {@link AboutDialog} class.
     * 
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     */
    public AboutDialog(DhkModel model, DhkView view) {
        super(view.getFrame());
        this.model = model;
        this.view = view;

        originalGlassPane = parentFrame.getGlassPane();
        darkeningGlassPane = createDarkeningGlassPane();
        frameUpdater = new FrameUpdater(view);
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

                initAboutPanels(aboutDialog);
                initAboutComponents(aboutDialog, systemTray);
                initAboutComponentListeners(aboutDialog, systemTray);
                addAboutComponents(aboutDialog);

                frameUpdater.updateUI();
                view.getDefaultFocusComponent().requestFocusInWindow();
                parentFrame.setGlassPane(darkeningGlassPane);
                darkeningGlassPane.setVisible(true);

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
        infoLabel = new JLabel(buildAboutInfoHtml());
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);

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
    private void initAboutComponentListeners(JDialog aboutDialog, SystemTray systemTray) {
        paypalButtonController.initListeners();

        licenseButton.addActionListener(createLicenseActionListener());
        releasesButton.addActionListener(createReleasesActionListener());
        closeButton.addActionListener(createCloseActionListener(aboutDialog, systemTray));

        licenseButton.addMouseListener(createMouseAdapter());
        releasesButton.addMouseListener(createMouseAdapter());
        closeButton.addMouseListener(createMouseAdapter());
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

        mainConstraints.gridx = 0;
        mainConstraints.gridy = 0;
        aboutDialog.add(infoLabel, mainConstraints);

        mainConstraints.gridx = 0;
        mainConstraints.gridy = 2;
        aboutDialog.add(paypalDonateButton, mainConstraints);

        mainConstraints.gridx = 0;
        mainConstraints.gridy = 3;
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

}
