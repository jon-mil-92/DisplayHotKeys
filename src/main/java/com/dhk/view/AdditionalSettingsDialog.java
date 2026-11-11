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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.dhk.model.DhkModel;
import com.dhk.model.button.Button;
import com.dhk.model.button.ButtonProperties;
import com.dhk.utility.LightweightTooltip;

/**
 * Shows an "Additional Settings" dialog on the AWT event dispatching thread. Creates a semi-transparent darkening panel
 * to overlay the parent frame.
 *
 * @author Jonathan R. Miller
 */
public class AdditionalSettingsDialog implements IView {

    private DhkModel model;
    private DhkView view;
    private GridBagLayout mainLayout;
    private GridBagConstraints mainConstraints;
    private JPanel settingsPanel;
    private GridBagLayout settingsPanelLayout;
    private GridBagConstraints settingsPanelConstraints;
    private JLabel optionsSectionLabel;
    private JSeparator optionsSeparator;
    private JLabel autoShowInfoOverlayLabel;
    private ToggleSwitch autoShowInfoOverlaySwitch;
    private JLabel hotKeysSectionLabel;
    private JSeparator hotKeysSeparator;
    private JLabel displayInfoHotKeyLabel;
    private HotKeyLabel displayInfoHotKey;
    private Button clearHotKeyButton;
    private JButton changeHotKeyButton;
    private JDialog additionalSettingsDialog;
    private JFrame parentFrame;
    private Component originalGlassPane;
    private Component darkeningGlassPane;
    private boolean dialogClosed;
    private boolean controlsEnabled;

    /**
     * Title of the dialog, shown in its title bar.
     */
    private static final String DIALOG_TITLE = "Additional Settings";

    /**
     * Constructor for the {@link AdditionalSettingsDialog} class.
     *
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     */
    public AdditionalSettingsDialog(DhkModel model, DhkView view) {
        this.model = model;
        this.view = view;
        this.parentFrame = view.getFrame();
        this.controlsEnabled = true;

        originalGlassPane = parentFrame.getGlassPane();
        darkeningGlassPane = createDarkeningGlassPane();

        // Created up front so a controller can attach its listeners before the dialog is shown
        initComponents();
    }

    /**
     * Shows an "Additional Settings" dialog on the AWT event dispatching thread.
     */
    public void showAdditionalSettingsDialog() {
        showAdditionalSettingsDialog(null);
    }

    /**
     * Shows an "Additional Settings" dialog on the AWT event dispatching thread.
     *
     * @param onCloseAction
     *            - The action to run when the dialog closes (not required)
     */
    public void showAdditionalSettingsDialog(Runnable onCloseAction) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Create the dialog and lay out the components created with this object
                additionalSettingsDialog = new JDialog(parentFrame, DIALOG_TITLE, true);
                additionalSettingsDialog.setResizable(false);

                initPanels(additionalSettingsDialog);
                initComponentListeners(additionalSettingsDialog, onCloseAction);
                addComponents(additionalSettingsDialog);

                view.getDefaultFocusComponent().requestFocusInWindow();
                parentFrame.setGlassPane(darkeningGlassPane);
                darkeningGlassPane.setVisible(true);

                // Pack and position to fit the info text, then show
                additionalSettingsDialog.pack();
                additionalSettingsDialog.setLocationRelativeTo(parentFrame);
                additionalSettingsDialog.setVisible(true);
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
        return optionsSectionLabel;
    }

    /**
     * Gets the switch that turns showing the info overlay automatically on or off.
     *
     * @return The Auto Show Info Overlay switch
     */
    public ToggleSwitch getAutoShowInfoOverlaySwitch() {
        return autoShowInfoOverlaySwitch;
    }

    /**
     * Gets the button that starts recording a new Display Info hot key.
     *
     * @return The Change Hot Key button
     */
    public JButton getChangeHotKeyButton() {
        return changeHotKeyButton;
    }

    /**
     * Gets the button that clears the Display Info hot key.
     *
     * @return The Clear Hot Key button
     */
    public Button getClearHotKeyButton() {
        return clearHotKeyButton;
    }

    /**
     * Shows the given text as the Display Info hot key, resizing a showing dialog so the text always fits. The dialog
     * keeps its location, so a dialog the user moved is not pulled back while a hot key is typed.
     *
     * @param hotKeyText
     *            - The text of the Display Info hot key
     */
    public void setDisplayInfoHotKeyText(String hotKeyText) {
        displayInfoHotKey.setText(hotKeyText);

        if (additionalSettingsDialog != null && additionalSettingsDialog.isShowing()) {
            additionalSettingsDialog.pack();
        }
    }

    /**
     * Sets whether the Display Info hot key is being recorded, which outlines it in the theme's focus color.
     *
     * @param recording
     *            - Whether the Display Info hot key is being recorded
     */
    public void setDisplayInfoHotKeyRecording(boolean recording) {
        displayInfoHotKey.setRecording(recording);
    }

    /**
     * Enables or disables every control of the dialog, which is done while a hot key is being recorded so nothing can
     * be pressed or closed partway through. The Clear Hot Key button is only enabled while a hot key is set.
     *
     * @param enabled
     *            - Whether the controls should be enabled
     */
    public void setControlsEnabled(boolean enabled) {
        controlsEnabled = enabled;

        autoShowInfoOverlaySwitch.setEnabled(enabled);
        changeHotKeyButton.setEnabled(enabled);
        clearHotKeyButton.setEnabled(enabled && model.getDisplayInfoHotKey().getKeys().size() > 0);

        // A button re-enabled under the cursor gets no mouseEntered, so restore rollover from the pointer
        if (enabled && changeHotKeyButton.isShowing()) {
            changeHotKeyButton.getModel().setRollover(changeHotKeyButton.getMousePosition() != null);
        }
    }

    /**
     * Initializes panels for a additional settings dialog.
     *
     * @param additionalSettingsDialog
     *            - The additional settings dialog to initialize panels for
     */
    private void initPanels(JDialog additionalSettingsDialog) {
        mainLayout = new GridBagLayout();
        additionalSettingsDialog.setLayout(mainLayout);

        mainConstraints = new GridBagConstraints();
        mainConstraints.insets = new Insets(8, 20, 8, 20);
        mainConstraints.anchor = GridBagConstraints.CENTER;
        mainConstraints.gridwidth = 1;

        settingsPanel = new JPanel();
        settingsPanelLayout = new GridBagLayout();
        settingsPanel.setLayout(settingsPanelLayout);

        settingsPanelConstraints = new GridBagConstraints();
    }

    /**
     * Initializes components for a additional settings dialog.
     */
    private void initComponents() {
        optionsSectionLabel = new JLabel("Options", SwingConstants.LEFT);
        optionsSectionLabel.putClientProperty("FlatLaf.styleClass", "h4");

        optionsSeparator = new JSeparator();

        autoShowInfoOverlayLabel = new JLabel("Autoshow Info Overlay :", SwingConstants.LEFT);
        new LightweightTooltip(autoShowInfoOverlayLabel).setText("Automatically show each display's applied settings.");

        autoShowInfoOverlaySwitch = new ToggleSwitch(model.isAutoShowInfoOverlay());

        hotKeysSectionLabel = new JLabel("Hot Keys", SwingConstants.LEFT);
        hotKeysSectionLabel.putClientProperty("FlatLaf.styleClass", "h4");

        hotKeysSeparator = new JSeparator();

        displayInfoHotKeyLabel = new JLabel("Show Info Overlay :", SwingConstants.LEFT);
        new LightweightTooltip(displayInfoHotKeyLabel).setText("Shows each display's applied settings.");

        displayInfoHotKey = new HotKeyLabel(model.getDisplayInfoHotKey().getHotKeyString());

        ButtonProperties clearHotKeyButtonProps = new ButtonProperties("Clear Hot Key", new Dimension(18, 20), 0.70f,
                0.60f);
        clearHotKeyButton = new Button("/clear_hot_key_idle.svg", "/clear_hot_key_hover.svg", clearHotKeyButtonProps,
                model.getDisplayInfoHotKey().getKeys().size() > 0);

        changeHotKeyButton = new JButton("Change Hot Key");
        changeHotKeyButton.setPreferredSize(new Dimension(148, 28));
        changeHotKeyButton.setFocusPainted(false);
    }

    /**
     * Initializes listeners for the components in a additional settings dialog.
     *
     * @param additionalSettingsDialog
     *            - The additional settings dialog to initialize listeners for
     * @param onCloseAction
     *            - The action to run when the dialog closes (not required)
     */
    private void initComponentListeners(final JDialog additionalSettingsDialog, final Runnable onCloseAction) {
        // Prevent default close and handle the title-bar close ourselves
        additionalSettingsDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        additionalSettingsDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // Ignore the title-bar close while a hot key is being recorded, so the recording always finishes
                if (!controlsEnabled) {
                    return;
                }

                closeDialog(additionalSettingsDialog, onCloseAction);
            }

            // A re-initialization disposes the parent frame, destroying this dialog without any close request
            @Override
            public void windowClosed(WindowEvent e) {
                closeDialog(additionalSettingsDialog, onCloseAction);
            }
        });

        additionalSettingsDialog.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent mousePressedEvent) {
                getDefaultFocusComponent().requestFocusInWindow();
            }
        });
    }

    /**
     * Adds components to a additional settings dialog.
     *
     * @param additionalSettingsDialog
     *            - The additional settings dialog to add components to
     */
    private void addComponents(JDialog additionalSettingsDialog) {
        addSetting(settingsPanel, settingsPanelConstraints, optionsSectionLabel, 0, 0, 4, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(8, 8, 8, 8));
        addSetting(settingsPanel, settingsPanelConstraints, optionsSeparator, 0, 1, 4, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 8));

        addSetting(settingsPanel, settingsPanelConstraints, autoShowInfoOverlayLabel, 0, 2, 1, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(8, 8, 8, 8));
        addSetting(settingsPanel, settingsPanelConstraints, autoShowInfoOverlaySwitch, 1, 2, 1, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(8, 8, 8, 8));

        addSetting(settingsPanel, settingsPanelConstraints, hotKeysSectionLabel, 0, 3, 4, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(8, 8, 8, 8));
        addSetting(settingsPanel, settingsPanelConstraints, hotKeysSeparator, 0, 4, 4, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 8));

        addSetting(settingsPanel, settingsPanelConstraints, displayInfoHotKeyLabel, 0, 5, 1, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(8, 8, 8, 8));
        addSetting(settingsPanel, settingsPanelConstraints, displayInfoHotKey, 1, 5, 1, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(8, 8, 8, 8));
        addSetting(settingsPanel, settingsPanelConstraints, clearHotKeyButton, 2, 5, 1, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(8, 8, 8, 8));
        addSetting(settingsPanel, settingsPanelConstraints, changeHotKeyButton, 3, 5, 1, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(8, 8, 8, 8));

        mainConstraints.gridx = 0;
        mainConstraints.gridy = 0;
        additionalSettingsDialog.add(settingsPanel, mainConstraints);
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
    private void addSetting(JPanel panel, GridBagConstraints constraints, Component component, int gridx, int gridy,
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
     * Closes a additional settings dialog.
     *
     * @param additionalSettingsDialog
     *            - The additional settings dialog to close
     * @param onCloseAction
     *            - The action to run when the dialog closes (not required)
     */
    private void closeDialog(JDialog additionalSettingsDialog, Runnable onCloseAction) {
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

        additionalSettingsDialog.dispose();
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
