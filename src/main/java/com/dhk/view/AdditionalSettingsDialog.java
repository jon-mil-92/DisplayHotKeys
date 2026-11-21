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
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import com.dhk.io.NativeInputHook;
import com.dhk.io.SettingsManager;
import com.dhk.localization.Language;
import com.dhk.localization.Localizer;
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
    private JPanel customizationPanel;
    private GridBagLayout customizationPanelLayout;
    private GridBagConstraints customizationPanelConstraints;
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
    private JLabel customizationSectionLabel;
    private JSeparator customizationSeparator;
    private JLabel languageLabel;
    private CenteredComboBox<Language> languageComboBox;
    private JLabel infoOverlayPositionLabel;
    private CenteredComboBox<String> infoOverlayPositionComboBox;
    private JLabel infoOverlayScaleLabel;
    private JSlider infoOverlayScaleSlider;
    private JLabel infoOverlayScaleValueLabel;
    private JLabel infoOverlayOpacityLabel;
    private JSlider infoOverlayOpacitySlider;
    private JLabel infoOverlayOpacityValueLabel;
    private JLabel infoOverlayDurationLabel;
    private JSlider infoOverlayDurationSlider;
    private JLabel infoOverlayDurationValueLabel;
    private JLabel hotKeysSectionLabel;
    private JSeparator hotKeysSeparator;
    private JLabel displayInfoHotKeyLabel;
    private HotKeyLabel displayInfoHotKey;
    private Button clearHotKeyButton;
    private JButton changeHotKeyButton;
    private boolean controlsEnabled;

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
     * Message key of each info overlay position's name, in the order of the overlay's positions, so a combo box index
     * is the position itself.
     */
    private static final String[] OVERLAY_POSITION_KEYS = {"overlayPosition.topLeft", "overlayPosition.topRight",
            "overlayPosition.center", "overlayPosition.bottomLeft", "overlayPosition.bottomRight"};

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
        super(view, nativeInputHook, Localizer.getText("settings.title"));
        this.model = model;
        this.controlsEnabled = true;

        // Created up front so a controller can attach its listeners before the dialog is shown
        initComponents();
    }

    /**
     * Shows an "Additional Settings" dialog on the AWT event dispatching thread.
     *
     * @param onCloseAction
     *            - The action to run when the dialog closes (not required)
     * @param reopenAction
     *            - The action that shows the dialog again after a re-initialization destroys it with the main frame
     */
    public void showAdditionalSettingsDialog(Runnable onCloseAction, Runnable reopenAction) {
        showDialog(onCloseAction, reopenAction);
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
     * Gets the combo box that selects the language the application is shown in.
     *
     * @return The Language combo box
     */
    public CenteredComboBox<Language> getLanguageComboBox() {
        return languageComboBox;
    }

    /**
     * Gets the combo box that selects where the info overlay sits on each display.
     *
     * @return The Info Overlay Position combo box
     */
    public CenteredComboBox<String> getInfoOverlayPositionComboBox() {
        return infoOverlayPositionComboBox;
    }

    /**
     * Gets the slider that selects the percentage the info overlay is scaled by.
     *
     * @return The Info Overlay Scale slider
     */
    public JSlider getInfoOverlayScaleSlider() {
        return infoOverlayScaleSlider;
    }

    /**
     * Gets the percentage the Info Overlay Scale slider is set to.
     *
     * @return The selected info overlay scale, as a percentage
     */
    public int getInfoOverlayScale() {
        return infoOverlayScaleSlider.getValue() * SettingsManager.INFO_OVERLAY_SCALE_STEP;
    }

    /**
     * Gets the slider that selects the opacity percentage of the info overlay.
     *
     * @return The Info Overlay Opacity slider
     */
    public JSlider getInfoOverlayOpacitySlider() {
        return infoOverlayOpacitySlider;
    }

    /**
     * Gets the percentage the Info Overlay Opacity slider is set to.
     *
     * @return The selected info overlay opacity, as a percentage
     */
    public int getInfoOverlayOpacity() {
        return infoOverlayOpacitySlider.getValue() * SettingsManager.INFO_OVERLAY_OPACITY_STEP;
    }

    /**
     * Gets the slider that selects the number of seconds the info overlay stays visible, which is its value.
     *
     * @return The Info Overlay Duration slider
     */
    public JSlider getInfoOverlayDurationSlider() {
        return infoOverlayDurationSlider;
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
        languageComboBox.setEnabled(enabled);
        infoOverlayPositionComboBox.setEnabled(enabled);
        infoOverlayScaleSlider.setEnabled(enabled);
        infoOverlayOpacitySlider.setEnabled(enabled);
        infoOverlayDurationSlider.setEnabled(enabled);
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

        customizationPanel = new JPanel();
        customizationPanelLayout = new GridBagLayout();
        customizationPanel.setLayout(customizationPanelLayout);

        customizationPanelConstraints = new GridBagConstraints();

        hotKeysPanel = new JPanel();
        hotKeysPanelLayout = new GridBagLayout();
        hotKeysPanel.setLayout(hotKeysPanelLayout);

        hotKeysPanelConstraints = new GridBagConstraints();
    }

    /**
     * Initializes components for a additional settings dialog.
     */
    private void initComponents() {
        optionsSectionLabel = new JLabel(Localizer.getText("settings.options"), SwingConstants.LEFT);
        optionsSectionLabel.putClientProperty("FlatLaf.styleClass", "h4");

        optionsSeparator = new JSeparator();

        runOnStartupLabel = new JLabel(Localizer.getText("settings.runOnStartup"), SwingConstants.LEFT);
        new LightweightTooltip(runOnStartupLabel).setText(Localizer.getText("settings.runOnStartup.tooltip"));

        runOnStartupSwitch = new ToggleSwitch(model.isRunOnStartup());

        minimizeToTrayLabel = new JLabel(Localizer.getText("settings.minimizeToTray"), SwingConstants.LEFT);
        new LightweightTooltip(minimizeToTrayLabel).setText(Localizer.getText("settings.minimizeToTray.tooltip"));

        minimizeToTraySwitch = new ToggleSwitch(model.isMinimizeToTray());

        closeToTrayLabel = new JLabel(Localizer.getText("settings.closeToTray"), SwingConstants.LEFT);
        new LightweightTooltip(closeToTrayLabel).setText(Localizer.getText("settings.closeToTray.tooltip"));

        closeToTraySwitch = new ToggleSwitch(model.isCloseToTray());

        closeToTrayNoteLabel = new JLabel(Localizer.getText("settings.closeToTray.note"), SwingConstants.LEFT);
        closeToTrayNoteLabel.putClientProperty("FlatLaf.style",
                "font: $small.font; foreground: $Label.disabledForeground");

        // Closing to the tray only applies while minimizing to the tray is on, so its switch and note follow that state
        updateCloseToTrayAvailability();

        autoShowInfoOverlayLabel = new JLabel(Localizer.getText("settings.autoShowInfoOverlay"), SwingConstants.LEFT);
        new LightweightTooltip(autoShowInfoOverlayLabel)
                .setText(Localizer.getText("settings.autoShowInfoOverlay.tooltip"));

        autoShowInfoOverlaySwitch = new ToggleSwitch(model.isAutoShowInfoOverlay());

        customizationSectionLabel = new JLabel(Localizer.getText("settings.customization"), SwingConstants.LEFT);
        customizationSectionLabel.putClientProperty("FlatLaf.styleClass", "h4");

        customizationSeparator = new JSeparator();

        languageLabel = new JLabel(Localizer.getText("settings.language"), SwingConstants.LEFT);
        new LightweightTooltip(languageLabel).setText(Localizer.getText("settings.language.tooltip"));

        // Each language is listed by its own name, so every reader can find their language whatever is shown
        languageComboBox = new CenteredComboBox<Language>(Language.values());
        languageComboBox.setSelectedItem(Localizer.getLanguage());

        infoOverlayPositionLabel = new JLabel(Localizer.getText("settings.infoOverlayPosition"), SwingConstants.LEFT);
        new LightweightTooltip(infoOverlayPositionLabel)
                .setText(Localizer.getText("settings.infoOverlayPosition.tooltip"));

        infoOverlayPositionComboBox = new CenteredComboBox<String>(Localizer.getTexts(OVERLAY_POSITION_KEYS));
        infoOverlayPositionComboBox.setSelectedIndex(model.getInfoOverlayPosition());

        // The combo boxes share a column, so each takes the width of the widest combo box in it like the slot table's
        List<CenteredComboBox<?>> customizationColumn = new ArrayList<>();
        languageComboBox.joinColumn(customizationColumn);
        infoOverlayPositionComboBox.joinColumn(customizationColumn);

        infoOverlayScaleLabel = new JLabel(Localizer.getText("settings.infoOverlayScale"), SwingConstants.LEFT);
        new LightweightTooltip(infoOverlayScaleLabel).setText(Localizer.getText("settings.infoOverlayScale.tooltip"));

        int scaleStep = SettingsManager.INFO_OVERLAY_SCALE_STEP;

        // The slider counts steps rather than percentages, so a drag can only ever land on a valid scale
        infoOverlayScaleSlider = new JSlider(SettingsManager.MIN_INFO_OVERLAY_SCALE / scaleStep,
                SettingsManager.MAX_INFO_OVERLAY_SCALE / scaleStep, model.getInfoOverlayScale() / scaleStep);

        infoOverlayScaleValueLabel = createSliderValueLabel(infoOverlayScaleSlider,
                step -> step * scaleStep + Localizer.getText("dpiScale.suffix"));

        infoOverlayOpacityLabel = new JLabel(Localizer.getText("settings.infoOverlayOpacity"), SwingConstants.LEFT);
        new LightweightTooltip(infoOverlayOpacityLabel)
                .setText(Localizer.getText("settings.infoOverlayOpacity.tooltip"));

        int opacityStep = SettingsManager.INFO_OVERLAY_OPACITY_STEP;

        // The slider counts steps rather than percentages, so a drag can only ever land on a valid opacity
        infoOverlayOpacitySlider = new JSlider(SettingsManager.MIN_INFO_OVERLAY_OPACITY / opacityStep,
                SettingsManager.MAX_INFO_OVERLAY_OPACITY / opacityStep, model.getInfoOverlayOpacity() / opacityStep);

        infoOverlayOpacityValueLabel = createSliderValueLabel(infoOverlayOpacitySlider,
                step -> step * opacityStep + Localizer.getText("dpiScale.suffix"));

        infoOverlayDurationLabel = new JLabel(Localizer.getText("settings.infoOverlayDuration"), SwingConstants.LEFT);
        new LightweightTooltip(infoOverlayDurationLabel)
                .setText(Localizer.getText("settings.infoOverlayDuration.tooltip"));

        infoOverlayDurationSlider = new JSlider(SettingsManager.MIN_INFO_OVERLAY_DURATION,
                SettingsManager.MAX_INFO_OVERLAY_DURATION, model.getInfoOverlayDuration());
        infoOverlayDurationValueLabel = createSliderValueLabel(infoOverlayDurationSlider,
                seconds -> Localizer.format("settings.infoOverlayDuration.value", seconds));

        hotKeysSectionLabel = new JLabel(Localizer.getText("settings.hotKeys"), SwingConstants.LEFT);
        hotKeysSectionLabel.putClientProperty("FlatLaf.styleClass", "h4");

        hotKeysSeparator = new JSeparator();

        displayInfoHotKeyLabel = new JLabel(Localizer.getText("settings.showInfoOverlay"), SwingConstants.LEFT);
        new LightweightTooltip(displayInfoHotKeyLabel).setText(Localizer.getText("settings.showInfoOverlay.tooltip"));

        displayInfoHotKey = new HotKeyLabel(model.getDisplayInfoHotKey().getHotKeyString());

        ButtonProperties clearHotKeyButtonProps = new ButtonProperties(Localizer.getText("hotKey.clear"),
                new Dimension(18, 20), 0.70f, 0.60f);
        clearHotKeyButton = new Button("/clear_hot_key_idle.svg", "/clear_hot_key_hover.svg", clearHotKeyButtonProps,
                model.getDisplayInfoHotKey().getKeys().size() > 0);

        changeHotKeyButton = new ChangeHotKeyButton();
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

        addSetting(customizationPanel, customizationPanelConstraints, languageLabel, 0, 0, 1, GridBagConstraints.WEST,
                GridBagConstraints.NONE, labelInsets);
        addSetting(customizationPanel, customizationPanelConstraints, languageComboBox, 1, 0, 1,
                GridBagConstraints.WEST, GridBagConstraints.NONE, labelInsets);
        addSetting(customizationPanel, customizationPanelConstraints, infoOverlayPositionLabel, 0, 1, 1,
                GridBagConstraints.WEST, GridBagConstraints.NONE, labelInsets);
        addSetting(customizationPanel, customizationPanelConstraints, infoOverlayPositionComboBox, 1, 1, 1,
                GridBagConstraints.WEST, GridBagConstraints.NONE, labelInsets);
        addSetting(customizationPanel, customizationPanelConstraints, infoOverlayScaleLabel, 0, 2, 1,
                GridBagConstraints.WEST, GridBagConstraints.NONE, labelInsets);
        addSetting(customizationPanel, customizationPanelConstraints, infoOverlayScaleSlider, 1, 2, 1,
                GridBagConstraints.WEST, GridBagConstraints.NONE, labelInsets);
        addSetting(customizationPanel, customizationPanelConstraints, infoOverlayScaleValueLabel, 2, 2, 1,
                GridBagConstraints.WEST, GridBagConstraints.NONE, labelInsets);
        addSetting(customizationPanel, customizationPanelConstraints, infoOverlayOpacityLabel, 0, 3, 1,
                GridBagConstraints.WEST, GridBagConstraints.NONE, labelInsets);
        addSetting(customizationPanel, customizationPanelConstraints, infoOverlayOpacitySlider, 1, 3, 1,
                GridBagConstraints.WEST, GridBagConstraints.NONE, labelInsets);
        addSetting(customizationPanel, customizationPanelConstraints, infoOverlayOpacityValueLabel, 2, 3, 1,
                GridBagConstraints.WEST, GridBagConstraints.NONE, labelInsets);
        addSetting(customizationPanel, customizationPanelConstraints, infoOverlayDurationLabel, 0, 4, 1,
                GridBagConstraints.WEST, GridBagConstraints.NONE, labelInsets);
        addSetting(customizationPanel, customizationPanelConstraints, infoOverlayDurationSlider, 1, 4, 1,
                GridBagConstraints.WEST, GridBagConstraints.NONE, labelInsets);
        addSetting(customizationPanel, customizationPanelConstraints, infoOverlayDurationValueLabel, 2, 4, 1,
                GridBagConstraints.WEST, GridBagConstraints.NONE, labelInsets);

        // The hot key outline is drawn at its own edge, so the whole gap below the last row comes from its inset
        Insets hotKeyRowInsets = new Insets(leadingInset, leadingInset, SECTION_GAP, trailingInset);

        addSetting(hotKeysPanel, hotKeysPanelConstraints, displayInfoHotKeyLabel, 0, 0, 1, GridBagConstraints.WEST,
                GridBagConstraints.NONE, hotKeyRowInsets);
        addSetting(hotKeysPanel, hotKeysPanelConstraints, displayInfoHotKey, 1, 0, 1, GridBagConstraints.CENTER,
                GridBagConstraints.VERTICAL, hotKeyRowInsets);
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

        addSetting(settingsPanel, settingsPanelConstraints, customizationSectionLabel, 0, 3, 1, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(sectionInset, leadingInset, 8, trailingInset));
        addSetting(settingsPanel, settingsPanelConstraints, customizationSeparator, 0, 4, 1, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, leadingInset, 0, trailingInset));
        addSetting(settingsPanel, settingsPanelConstraints, customizationPanel, 0, 5, 1, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(0, 0, 0, 0));

        // The slider row sits the trailing inset above the next heading like a switch track, so the gap matches
        addSetting(settingsPanel, settingsPanelConstraints, hotKeysSectionLabel, 0, 6, 1, GridBagConstraints.WEST,
                GridBagConstraints.NONE, new Insets(sectionInset, leadingInset, 8, trailingInset));
        addSetting(settingsPanel, settingsPanelConstraints, hotKeysSeparator, 0, 7, 1, GridBagConstraints.CENTER,
                GridBagConstraints.HORIZONTAL, new Insets(0, leadingInset, 0, trailingInset));
        addSetting(settingsPanel, settingsPanelConstraints, hotKeysPanel, 0, 8, 1, GridBagConstraints.WEST,
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

    /**
     * Creates a label showing the text of a slider's value, which follows the slider as it moves. The label is as wide
     * as the widest of its texts, measured whenever the layout asks, so moving the slider never resizes the dialog.
     *
     * @param slider
     *            - The slider whose value the label shows
     * @param formatValue
     *            - Formats the text of a slider value
     *
     * @return The label showing the slider's value
     */
    private JLabel createSliderValueLabel(JSlider slider, IntFunction<String> formatValue) {
        int minValue = slider.getMinimum();
        String[] valueTexts = new String[slider.getMaximum() - minValue + 1];

        // Each value's text is formatted once, so every layout can measure the widest of them
        for (int textIndex = 0; textIndex < valueTexts.length; textIndex++) {
            valueTexts[textIndex] = formatValue.apply(minValue + textIndex);
        }

        JLabel valueLabel = new JLabel(valueTexts[slider.getValue() - minValue], SwingConstants.LEFT) {
            @Override
            public Dimension getPreferredSize() {
                FontMetrics fontMetrics = getFontMetrics(getFont());
                int widestTextWidth = 0;

                for (String valueText : valueTexts) {
                    widestTextWidth = Math.max(widestTextWidth, fontMetrics.stringWidth(valueText));
                }

                // The padding around the shown text stays the same whichever value is shown
                Dimension shownTextSize = super.getPreferredSize();
                int paddingWidth = shownTextSize.width - fontMetrics.stringWidth(getText());

                return new Dimension(paddingWidth + widestTextWidth, shownTextSize.height);
            }
        };

        slider.addChangeListener(e -> valueLabel.setText(valueTexts[slider.getValue() - minValue]));

        return valueLabel;
    }

}
