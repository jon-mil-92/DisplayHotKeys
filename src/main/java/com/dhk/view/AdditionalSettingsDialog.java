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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import com.dhk.io.NativeInputHook;
import com.dhk.model.DhkModel;
import com.dhk.model.button.Button;
import com.dhk.model.button.ButtonProperties;
import com.dhk.utility.LightweightTooltip;

/**
 * Shows an "Additional Settings" dialog over the darkened main frame, holding the settings that affect all displays.
 *
 * @author Jonathan R. Miller
 */
public class AdditionalSettingsDialog extends OverlayDialog {

    private DhkModel model;
    private GridBagLayout mainLayout;
    private GridBagConstraints mainConstraints;
    private JPanel settingsPanel;
    private GridBagLayout settingsPanelLayout;
    private GridBagConstraints settingsPanelConstraints;
    private JPanel optionsPanel;
    private GridBagLayout optionsPanelLayout;
    private GridBagConstraints optionsPanelConstraints;
    private JPanel hotKeysPanel;
    private GridBagLayout hotKeysPanelLayout;
    private GridBagConstraints hotKeysPanelConstraints;
    private JLabel optionsSectionLabel;
    private JSeparator optionsSeparator;
    private JLabel runOnStartupLabel;
    private ToggleSwitch runOnStartupSwitch;
    private JLabel minimizeToTrayLabel;
    private ToggleSwitch minimizeToTraySwitch;
    private JLabel closeToTrayLabel;
    private ToggleSwitch closeToTraySwitch;
    private JLabel closeToTrayNoteLabel;
    private JLabel autoShowInfoOverlayLabel;
    private ToggleSwitch autoShowInfoOverlaySwitch;
    private JLabel hotKeysSectionLabel;
    private JSeparator hotKeysSeparator;
    private JLabel displayInfoHotKeyLabel;
    private HotKeyLabel displayInfoHotKey;
    private Button clearHotKeyButton;
    private JButton changeHotKeyButton;
    private boolean controlsEnabled;

    /**
     * Title shown in the dialog's title bar.
     */
    private static final String DIALOG_TITLE = "Additional Settings";

    /**
     * Visible space between neighboring components in each section's table, both across a row and between rows.
     */
    private static final int COMPONENT_GAP = 16;

    /**
     * Visible space above the first section, between sections, and below the last row.
     */
    private static final int SECTION_GAP = 24;

    /**
     * Space a label's text leaves inside the component, which already counts toward a visible gap and is subtracted
     * from the inset beside it.
     */
    private static final int INNER_PADDING = 2;

    /**
     * Constructor for the {@link AdditionalSettingsDialog} class.
     *
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     * @param nativeInputHook
     *            - The shared native input hook used to re-fit the dialog after a cross-display drag (may be null)
     */
    public AdditionalSettingsDialog(DhkModel model, DhkView view, NativeInputHook nativeInputHook) {
        super(view, nativeInputHook, DIALOG_TITLE);
        this.model = model;
        this.controlsEnabled = true;

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
        showDialog(onCloseAction);
    }

    @Override
    public Component getDefaultFocusComponent() {
        return optionsSectionLabel;
    }

    @Override
    protected void buildContent(JDialog dialog, JPanel contentPanel) {
        initPanels(contentPanel);
        addComponents(contentPanel);
    }

    @Override
    protected boolean canClose() {
        // Ignore the title-bar close while a hot key is being recorded, so the recording always finishes
        return controlsEnabled;
    }

    /**
     * Gets the switch that turns starting the application upon login on or off.
     *
     * @return The Run on Startup switch
     */
    public ToggleSwitch getRunOnStartupSwitch() {
        return runOnStartupSwitch;
    }

    /**
     * Gets the switch that turns minimizing the application to the system tray on or off.
     *
     * @return The Minimize to Tray switch
     */
    public ToggleSwitch getMinimizeToTraySwitch() {
        return minimizeToTraySwitch;
    }

    /**
     * Gets the switch that turns closing the application to the system tray on or off.
     *
     * @return The Close to Tray switch
     */
    public ToggleSwitch getCloseToTraySwitch() {
        return closeToTraySwitch;
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
     * Shows the given text as the Display Info hot key, resizing a showing dialog to fit the text within its display.
     *
     * @param hotKeyText
     *            - The text of the Display Info hot key
     */
    public void setDisplayInfoHotKeyText(String hotKeyText) {
        displayInfoHotKey.setText(hotKeyText);
        refitShowingDialog();
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
     * Enables the Close to Tray switch and hides its note only while minimizing to the tray is on, since closing to the
     * tray only applies then. A showing dialog is resized to fit the note as it appears or disappears.
     */
    public void updateCloseToTrayAvailability() {
        closeToTraySwitch.setEnabled(controlsEnabled && model.isMinimizeToTray());
        closeToTrayNoteLabel.setVisible(!model.isMinimizeToTray());
        refitShowingDialog();
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

        runOnStartupSwitch.setEnabled(enabled);
        minimizeToTraySwitch.setEnabled(enabled);
        closeToTraySwitch.setEnabled(enabled && model.isMinimizeToTray());
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
     * @param contentPanel
     *            - The dialog's scrolling content panel to lay the settings out in
     */
    private void initPanels(JPanel contentPanel) {
        mainLayout = new GridBagLayout();
        contentPanel.setLayout(mainLayout);

        mainConstraints = new GridBagConstraints();
        mainConstraints.insets = new Insets(0, 20, 0, 20);
        mainConstraints.anchor = GridBagConstraints.CENTER;
        mainConstraints.gridwidth = 1;

        settingsPanel = new JPanel();
        settingsPanelLayout = new GridBagLayout();
        settingsPanel.setLayout(settingsPanelLayout);

        settingsPanelConstraints = new GridBagConstraints();

        optionsPanel = new JPanel();
        optionsPanelLayout = new GridBagLayout();
        optionsPanel.setLayout(optionsPanelLayout);

        optionsPanelConstraints = new GridBagConstraints();

        hotKeysPanel = new JPanel();
        hotKeysPanelLayout = new GridBagLayout();
        hotKeysPanel.setLayout(hotKeysPanelLayout);

        hotKeysPanelConstraints = new GridBagConstraints();
    }

    /**
     * Initializes components for a additional settings dialog.
     */
    private void initComponents() {
        optionsSectionLabel = new JLabel("Options", SwingConstants.LEFT);
        optionsSectionLabel.putClientProperty("FlatLaf.styleClass", "h4");

        optionsSeparator = new JSeparator();

        runOnStartupLabel = new JLabel("Run on Startup :", SwingConstants.LEFT);
        new LightweightTooltip(runOnStartupLabel).setText("Start the app automatically upon login.");

        runOnStartupSwitch = new ToggleSwitch(model.isRunOnStartup());

        minimizeToTrayLabel = new JLabel("Minimize to Tray :", SwingConstants.LEFT);
        new LightweightTooltip(minimizeToTrayLabel).setText("Keep running in the system tray when minimized.");

        minimizeToTraySwitch = new ToggleSwitch(model.isMinimizeToTray());

        closeToTrayLabel = new JLabel("Close to Tray :", SwingConstants.LEFT);
        new LightweightTooltip(closeToTrayLabel).setText("Keep running in the system tray when closed.");

        closeToTraySwitch = new ToggleSwitch(model.isCloseToTray());

        closeToTrayNoteLabel = new JLabel("Requires Minimize to Tray", SwingConstants.LEFT);
        closeToTrayNoteLabel.putClientProperty("FlatLaf.style",
                "font: $small.font; foreground: $Label.disabledForeground");

        // Closing to the tray only applies while minimizing to the tray is on, so its switch and note follow that state
        updateCloseToTrayAvailability();

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
     * Adds components to a additional settings dialog.
     *
     * @param contentPanel
     *            - The dialog's scrolling content panel to add the settings to
     */
    private void addComponents(JPanel contentPanel) {
        // Each component takes part of the gap on each side, so neighboring components always sit the full gap apart
        int leadingInset = COMPONENT_GAP / 2;
        int trailingInset = COMPONENT_GAP - leadingInset;

        // Each switch already keeps an edge margin, so its insets shrink by it and its track stays aligned
        int switchLeadingInset = leadingInset - ToggleSwitch.EDGE_MARGIN;
        int switchTrailingInset = trailingInset - ToggleSwitch.EDGE_MARGIN;

        // An edge adds no padding of its own, so only the heading's text padding counts toward the gap above it
        int edgeInset = SECTION_GAP - INNER_PADDING;

        // The options end in switch rows, whose tracks already sit the trailing inset above the next heading
        int sectionInset = SECTION_GAP - trailingInset - INNER_PADDING;

        // The panels copy the insets of each component as it is added, so every label or switch can share one set
        Insets labelInsets = new Insets(leadingInset, leadingInset, trailingInset, trailingInset);
        Insets switchInsets = new Insets(switchLeadingInset, switchLeadingInset, switchTrailingInset,
                switchTrailingInset);

        // The option labels share a row with their switches, so each row is sized by the taller switch
        addSetting(optionsPanel, optionsPanelConstraints, runOnStartupLabel, 0, 0, 1, GridBagConstraints.WEST,
                GridBagConstraints.NONE, labelInsets);
        addSetting(optionsPanel, optionsPanelConstraints, runOnStartupSwitch, 1, 0, 1, GridBagConstraints.WEST,
                GridBagConstraints.NONE, switchInsets);
        addSetting(optionsPanel, optionsPanelConstraints, minimizeToTrayLabel, 2, 0, 1, GridBagConstraints.WEST,
                GridBagConstraints.NONE, labelInsets);
        addSetting(optionsPanel, optionsPanelConstraints, minimizeToTraySwitch, 3, 0, 1, GridBagConstraints.WEST,
                GridBagConstraints.NONE, switchInsets);

        addSetting(optionsPanel, optionsPanelConstraints, autoShowInfoOverlayLabel, 0, 1, 1, GridBagConstraints.WEST,
                GridBagConstraints.NONE, labelInsets);
        addSetting(optionsPanel, optionsPanelConstraints, autoShowInfoOverlaySwitch, 1, 1, 1, GridBagConstraints.WEST,
                GridBagConstraints.NONE, switchInsets);
        addSetting(optionsPanel, optionsPanelConstraints, closeToTrayLabel, 2, 1, 1, GridBagConstraints.WEST,
                GridBagConstraints.NONE, labelInsets);
        addSetting(optionsPanel, optionsPanelConstraints, closeToTraySwitch, 3, 1, 1, GridBagConstraints.WEST,
                GridBagConstraints.NONE, switchInsets);

        /*
         * A hidden note takes up no space, so the row collapses whenever minimizing to the tray is on. The note ends
         * the options the way a switch row does, so it keeps the same space below it
         */
        addSetting(optionsPanel, optionsPanelConstraints, closeToTrayNoteLabel, 2, 2, 2, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(0, leadingInset, switchTrailingInset, trailingInset));

        // The hot key outline is drawn at its own edge, so the whole gap below the last row comes from its inset
        Insets hotKeyRowInsets = new Insets(leadingInset, leadingInset, SECTION_GAP, trailingInset);

        addSetting(hotKeysPanel, hotKeysPanelConstraints, displayInfoHotKeyLabel, 0, 0, 1, GridBagConstraints.WEST,
                GridBagConstraints.NONE, hotKeyRowInsets);
        addSetting(hotKeysPanel, hotKeysPanelConstraints, displayInfoHotKey, 1, 0, 1, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, hotKeyRowInsets);
        addSetting(hotKeysPanel, hotKeysPanelConstraints, clearHotKeyButton, 2, 0, 1, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, hotKeyRowInsets);
        addSetting(hotKeysPanel, hotKeysPanelConstraints, changeHotKeyButton, 3, 0, 1, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, hotKeyRowInsets);

        /*
         * Each section lays out its own table, so a long hot key widens only the hot key table and never moves the
         * options columns. Both tables sit flush with the headings, so each table's labels line up under its heading
         */
        addSetting(settingsPanel, settingsPanelConstraints, optionsSectionLabel, 0, 0, 1, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(edgeInset, leadingInset, 8, trailingInset));
        addSetting(settingsPanel, settingsPanelConstraints, optionsSeparator, 0, 1, 1, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, leadingInset, 0, trailingInset));
        addSetting(settingsPanel, settingsPanelConstraints, optionsPanel, 0, 2, 1, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(0, 0, 0, 0));

        addSetting(settingsPanel, settingsPanelConstraints, hotKeysSectionLabel, 0, 3, 1, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(sectionInset, leadingInset, 8, trailingInset));
        addSetting(settingsPanel, settingsPanelConstraints, hotKeysSeparator, 0, 4, 1, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, leadingInset, 0, trailingInset));
        addSetting(settingsPanel, settingsPanelConstraints, hotKeysPanel, 0, 5, 1, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(0, 0, 0, 0));

        mainConstraints.gridx = 0;
        mainConstraints.gridy = 0;
        contentPanel.add(settingsPanel, mainConstraints);
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

}
