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

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import com.dhk.model.DisplayMode;
import com.dhk.model.RefreshRate;
import com.dhk.model.Resolution;
import com.dhk.model.button.Button;
import com.dhk.model.button.ButtonProperties;
import com.dhk.utility.DisplayModeUtil;

/**
 * Defines the view components of a Slot.
 *
 * @author Jonathan R. Miller
 */
public class Slot {

    private JLabel slotIndicatorLabel;
    private Button applySlotButton;
    private DisplayMode[] supportedDisplayModes;
    private CenteredComboBox<Resolution> slotResolutions;
    private CenteredComboBox<RefreshRate> slotRefreshRates;
    private CenteredComboBox<String> slotScalingModes;
    private CenteredComboBox<Integer> slotDpiScalePercentages;
    private CenteredComboBox<String> slotOrientationModes;
    private JLabel slotHotKey;
    private Button clearHotKeyButton;
    private JButton slotChangeHotKeyButton;
    private Button clearSlotButton;
    private List<Button> buttons;

    /**
     * Constructor for the {@link Slot} class.
     *
     * @param slotIndex
     *            - The index of the slot
     * @param displayIndex
     *            - The index of the display the slot resides in
     * @param displayModes
     *            - The array of display modes for the slot
     * @param scalingModes
     *            - The array of scaling modes for the slot
     * @param dpiScalePercentages
     *            - The array of DPI scale percentages for the slot
     * @param orientationModes
     *            - The array of orientation modes for the slot
     */
    public Slot(int slotIndex, int displayIndex, DisplayMode[] displayModes, String[] scalingModes,
            Integer[] dpiScalePercentages, String[] orientationModes) {
        String slotId = Integer.toString(slotIndex + 1);

        slotIndicatorLabel = new JLabel("Slot " + slotId + " :", SwingConstants.CENTER);
        slotIndicatorLabel.setPreferredSize(new Dimension(52, 28));

        ButtonProperties applySlotButtonProps = new ButtonProperties("Apply Slot", new Dimension(20, 20), 0.80f, 0.68f);
        applySlotButton = new Button("/apply_slot_idle.svg", "/apply_slot_hover.svg", applySlotButtonProps, true);

        supportedDisplayModes = displayModes;

        Resolution[] resolutions = DisplayModeUtil.distinctResolutions(displayModes);
        slotResolutions = new CenteredComboBox<Resolution>(resolutions);
        slotResolutions.setPreferredSize(new Dimension(138, 28));

        // Start with the first resolution's refresh rates; the view sets the real selection right after construction
        RefreshRate[] refreshRates = resolutions.length > 0
                ? DisplayModeUtil.refreshRatesForResolution(displayModes, resolutions[0])
                : new RefreshRate[0];
        slotRefreshRates = new CenteredComboBox<RefreshRate>(refreshRates);
        slotRefreshRates.setPreferredSize(new Dimension(118, 28));

        slotScalingModes = new CenteredComboBox<String>(scalingModes);
        slotScalingModes.setPreferredSize(new Dimension(110, 28));

        slotDpiScalePercentages = new CenteredComboBox<Integer>(dpiScalePercentages);
        slotDpiScalePercentages.setPreferredSize(new Dimension(70, 28));

        slotOrientationModes = new CenteredComboBox<String>(orientationModes);
        slotOrientationModes.setPreferredSize(new Dimension(118, 28));

        slotHotKey = new JLabel("", SwingConstants.CENTER);

        ButtonProperties clearHotKeyButtonProps = new ButtonProperties("Clear Hot Key", new Dimension(18, 20), 0.70f,
                0.60f);
        clearHotKeyButton = new Button("/clear_hot_key_idle.svg", "/clear_hot_key_hover.svg", clearHotKeyButtonProps,
                false);

        slotChangeHotKeyButton = new JButton("Change Hot Key");
        slotChangeHotKeyButton.setPreferredSize(new Dimension(148, 28));
        slotChangeHotKeyButton.setFocusPainted(false);

        ButtonProperties clearSlotButtonProps = new ButtonProperties("Clear Slot", new Dimension(22, 20), 0.80f, 0.68f);
        clearSlotButton = new Button("/clear_slot_idle.svg", "/clear_slot_hover.svg", clearSlotButtonProps, true);

        buttons = new ArrayList<>();
        buttons.add(clearSlotButton);
        buttons.add(applySlotButton);
        buttons.add(clearHotKeyButton);
    }

    /**
     * Gets the indicator label of the slot.
     *
     * @return The indicator label of the slot
     */
    public JLabel getIndicatorLabel() {
        return slotIndicatorLabel;
    }

    /**
     * Gets the apply slot button.
     *
     * @return The apply slot button
     */
    public Button getApplySlotButton() {
        return applySlotButton;
    }

    /**
     * Gets the supported display modes the slot's resolution and refresh rate selections are derived from.
     *
     * @return The supported display modes of the slot
     */
    public DisplayMode[] getSupportedDisplayModes() {
        return supportedDisplayModes;
    }

    /**
     * Gets the resolutions combo box of the slot.
     *
     * @return The resolutions combo box of the slot
     */
    public CenteredComboBox<Resolution> getResolutions() {
        return slotResolutions;
    }

    /**
     * Gets the refresh rates combo box of the slot.
     *
     * @return The refresh rates combo box of the slot
     */
    public CenteredComboBox<RefreshRate> getRefreshRates() {
        return slotRefreshRates;
    }

    /**
     * Replaces the items in the refresh rates combo box with the given rates. The previously selected rate is preserved
     * when it is still supported; otherwise the combo box falls back to the first (highest) rate. This is used to offer
     * only the refresh rates the slot's currently selected resolution supports.
     *
     * @param refreshRates
     *            - The array of refresh rates to populate the combo box with
     */
    public void setRefreshRates(RefreshRate[] refreshRates) {
        RefreshRate previouslySelected = (RefreshRate) slotRefreshRates.getSelectedItem();

        // Replacing the model leaves the registered action listeners attached so model updates still fire
        slotRefreshRates.setModel(new DefaultComboBoxModel<RefreshRate>(refreshRates));

        if (previouslySelected != null && Arrays.asList(refreshRates).contains(previouslySelected)) {
            slotRefreshRates.setSelectedItem(previouslySelected);
        }
    }

    /**
     * Gets the scaling modes combo box of the slot.
     *
     * @return The scaling modes combo box of the slot
     */
    public CenteredComboBox<String> getScalingModes() {
        return slotScalingModes;
    }

    /**
     * Gets the DPI scale percentages combo box of the slot.
     *
     * @return The DPI scale percentages combo box of the slot
     */
    public CenteredComboBox<Integer> getDpiScalePercentages() {
        return slotDpiScalePercentages;
    }

    /**
     * Replaces the items in the DPI scale percentages combo box with the given supported percentages. The previously
     * selected percentage is preserved when it is still supported; otherwise the combo box falls back to the first
     * (lowest) supported percentage. This is used to reflect the variable set of DPI scale percentages Windows supports
     * for the slot's currently selected resolution.
     *
     * @param dpiScalePercentages
     *            - The array of supported DPI scale percentages to populate the combo box with
     */
    public void setDpiScalePercentages(Integer[] dpiScalePercentages) {
        Integer previouslySelected = (Integer) slotDpiScalePercentages.getSelectedItem();

        // Replacing the model leaves the registered action listeners attached so model updates still fire
        slotDpiScalePercentages.setModel(new DefaultComboBoxModel<Integer>(dpiScalePercentages));

        if (previouslySelected != null && Arrays.asList(dpiScalePercentages).contains(previouslySelected)) {
            slotDpiScalePercentages.setSelectedItem(previouslySelected);
        }
    }

    /**
     * Gets the orientation modes combo box of the slot.
     *
     * @return The orientation modes combo box of the slot
     */
    public CenteredComboBox<String> getOrientationModes() {
        return slotOrientationModes;
    }

    /**
     * Gets the hot key of the slot.
     *
     * @return The hot key of the slot
     */
    public JLabel getHotKey() {
        return slotHotKey;
    }

    /**
     * Gets the clear hot key button of the slot.
     *
     * @return The clear hot key button of the slot
     */
    public Button getClearHotKeyButton() {
        return clearHotKeyButton;
    }

    /**
     * Gets the change hot key button of the slot.
     *
     * @return The change hot key button of the slot
     */
    public JButton getChangeHotKeyButton() {
        return slotChangeHotKeyButton;
    }

    /**
     * Gets the clear slot button of the slot.
     *
     * @return The clear slot button of the slot
     */
    public Button getClearSlotButton() {
        return clearSlotButton;
    }

    /**
     * Gets a list of buttons in the slot.
     *
     * @return A list of buttons in the slot
     */
    public List<Button> getButtons() {
        return buttons;
    }

}
