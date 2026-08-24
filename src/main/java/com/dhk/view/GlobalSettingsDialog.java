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

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.dhk.model.DhkModel;

/**
 * Shows a "Global Settings" dialog on the AWT event dispatching thread. Creates a semi-transparent darkening panel to
 * overlay the parent frame.
 *
 * @author Jonathan R. Miller
 */
public class GlobalSettingsDialog implements IView {

    private DhkModel model;
    private DhkView view;
    private GridBagLayout mainLayout;
    private GridBagConstraints mainConstraints;
    private JPanel buttonPanel;
    private GridBagLayout buttonPanelLayout;
    private GridBagConstraints buttonPanelConstraints;
    private JPanel settingsPanel;
    private GridBagLayout settingsPanelLayout;
    private GridBagConstraints settingsPanelConstraints;
    private JLabel headerLabel;
    private JButton closeButton;
    private JFrame parentFrame;
    private Component originalGlassPane;
    private Component darkeningGlassPane;
    private boolean dialogClosed;

    private static final float HEADER_FONT_SCALE = 1.6f;

    /**
     * Constructor for the {@link GlobalSettingsDialog} class.
     *
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     */
    public GlobalSettingsDialog(DhkModel model, DhkView view) {
        this.model = model;
        this.view = view;
        this.parentFrame = view.getFrame();

        originalGlassPane = parentFrame.getGlassPane();
        darkeningGlassPane = createDarkeningGlassPane();
    }

    /**
     * Shows a "Global Settings" dialog on the AWT event dispatching thread.
     */
    public void showGlobalSettingsDialog() {
        showGlobalSettingsDialog(null);
    }

    /**
     * Shows a "Global Settings" dialog on the AWT event dispatching thread.
     *
     * @param onCloseAction
     *            - The action to run when the dialog closes (not required)
     */
    public void showGlobalSettingsDialog(Runnable onCloseAction) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Create the dialog and initialize components
                final JDialog globalSettingsDialog = new JDialog(parentFrame, true);
                globalSettingsDialog.setResizable(false);

                initPanels(globalSettingsDialog);
                initComponents(globalSettingsDialog);
                initComponentListeners(globalSettingsDialog, onCloseAction);
                addComponents(globalSettingsDialog);

                view.getDefaultFocusComponent().requestFocusInWindow();
                parentFrame.setGlassPane(darkeningGlassPane);
                darkeningGlassPane.setVisible(true);

                // Pack and position to fit the info text, then show
                globalSettingsDialog.pack();
                globalSettingsDialog.setLocationRelativeTo(parentFrame);
                globalSettingsDialog.setVisible(true);
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
     * Initializes panels for a global settings dialog.
     *
     * @param globalSettingsDialog
     *            - The global settings dialog to initialize panels for
     */
    private void initPanels(JDialog globalSettingsDialog) {
        mainLayout = new GridBagLayout();
        globalSettingsDialog.setLayout(mainLayout);

        mainConstraints = new GridBagConstraints();
        mainConstraints.insets = new Insets(8, 20, 8, 20);
        mainConstraints.anchor = GridBagConstraints.CENTER;
        mainConstraints.gridwidth = 1;

        buttonPanel = new JPanel();
        buttonPanelLayout = new GridBagLayout();
        buttonPanel.setLayout(buttonPanelLayout);

        buttonPanelConstraints = new GridBagConstraints();
        buttonPanelConstraints.insets = new Insets(2, 8, 10, 8);
        buttonPanelConstraints.anchor = GridBagConstraints.CENTER;
        buttonPanelConstraints.gridwidth = 1;

        settingsPanel = new JPanel();
        settingsPanelLayout = new GridBagLayout();
        settingsPanel.setLayout(settingsPanelLayout);

        settingsPanelConstraints = new GridBagConstraints();
        settingsPanelConstraints.insets = new Insets(4, 8, 4, 8);
        settingsPanelConstraints.anchor = GridBagConstraints.CENTER;
        settingsPanelConstraints.gridwidth = 1;
    }

    /**
     * Initializes components for a global settings dialog.
     *
     * @param globalSettingsDialog
     *            - The global settings dialog to initialize components for
     */
    private void initComponents(JDialog globalSettingsDialog) {
        headerLabel = new JLabel("Global Settings");
        headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerLabel.putClientProperty("FlatLaf.style", "font: bold " + Math.round(HEADER_FONT_SCALE * 100) + "%");
        closeButton = new JButton("Close");
        closeButton.setFocusPainted(false);
    }

    /**
     * Initializes listeners for the components in a global settings dialog.
     *
     * @param globalSettingsDialog
     *            - The global settings dialog to initialize listeners for
     * @param onCloseAction
     *            - The action to run when the dialog closes (not required)
     */
    private void initComponentListeners(final JDialog globalSettingsDialog, final Runnable onCloseAction) {
        closeButton.addActionListener(createCloseActionListener(globalSettingsDialog, onCloseAction));

        // Prevent default close and handle the title-bar close ourselves
        globalSettingsDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        globalSettingsDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Call the same method used by the Close button
                closeButtonAction(globalSettingsDialog, onCloseAction);
            }

            // A re-initialization disposes the parent frame, destroying this dialog without any close request
            @Override
            public void windowClosed(WindowEvent e) {
                closeButtonAction(globalSettingsDialog, onCloseAction);
            }
        });

        globalSettingsDialog.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent mousePressedEvent) {
                getDefaultFocusComponent().requestFocusInWindow();
            }
        });
    }

    /**
     * Adds components to a global settings dialog.
     *
     * @param globalSettingsDialog
     *            - The global settings dialog to add components to
     */
    private void addComponents(JDialog globalSettingsDialog) {
        buttonPanelConstraints.gridx = 0;
        buttonPanelConstraints.gridy = 0;
        buttonPanel.add(closeButton, buttonPanelConstraints);

        settingsPanelConstraints.gridx = 0;
        settingsPanelConstraints.gridy = 0;
        settingsPanel.add(headerLabel, settingsPanelConstraints);

        mainConstraints.gridx = 0;
        mainConstraints.gridy = 0;
        globalSettingsDialog.add(settingsPanel, mainConstraints);

        mainConstraints.gridx = 0;
        mainConstraints.gridy = 1;
        globalSettingsDialog.add(buttonPanel, mainConstraints);
    }

    /**
     * Creates an action listener that closes a global settings dialog.
     *
     * @param globalSettingsDialog
     *            - The global settings dialog to close
     * @param onCloseAction
     *            - The action to run when the dialog closes (not required)
     *
     * @return An action listener that closes a global settings dialog
     */
    private ActionListener createCloseActionListener(JDialog globalSettingsDialog, Runnable onCloseAction) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeButtonAction(globalSettingsDialog, onCloseAction);
            }
        };
    }

    /**
     * Closes a global settings dialog.
     *
     * @param globalSettingsDialog
     *            - The global settings dialog to close
     * @param onCloseAction
     *            - The action to run when the dialog closes (not required)
     */
    private void closeButtonAction(JDialog globalSettingsDialog, Runnable onCloseAction) {
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

        globalSettingsDialog.dispose();
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
