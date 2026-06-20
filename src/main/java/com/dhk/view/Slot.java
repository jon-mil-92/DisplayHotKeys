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
import java.awt.DisplayMode;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import com.dhk.model.button.Button;
import com.dhk.model.button.ButtonProperties;

/**
 * Defines the view components of a Slot.
 *
 * @author Jonathan R. Miller
 */
public class Slot {

    private JLabel slotIndicatorLabel;
    private Button applyDisplayModeButton;
    private JComboBox<DisplayMode> slotDisplayModes;
    private JComboBox<String> slotScalingModes;
    private JComboBox<Integer> slotDpiScalePercentages;
    private JComboBox<String> slotOrientationModes;
    private JLabel slotHotKey;
    private Button slotClearHotKeyButton;
    private JButton slotChangeHotKeyButton;
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
        String slotID = Integer.toString(slotIndex + 1);

        slotIndicatorLabel = new JLabel("Slot " + slotID + " :", SwingConstants.CENTER);
        slotIndicatorLabel.setPreferredSize(new Dimension(50, 28));

        ButtonProperties applyDisplayModeButtonProps = new ButtonProperties("Apply Display Mode", new Dimension(20, 20),
                0.80f, 0.68f);
        applyDisplayModeButton = new Button("/apply_idle.svg", "/apply_hover.svg", applyDisplayModeButtonProps, true);

        slotDisplayModes = new JComboBox<DisplayMode>(displayModes);
        slotDisplayModes.setPreferredSize(new Dimension(220, 28));

        slotScalingModes = new JComboBox<String>(scalingModes);
        slotScalingModes.setPreferredSize(new Dimension(110, 28));

        slotDpiScalePercentages = new JComboBox<Integer>(dpiScalePercentages);
        slotDpiScalePercentages.setPreferredSize(new Dimension(70, 28));

        slotOrientationModes = new JComboBox<String>(orientationModes);
        slotOrientationModes.setPreferredSize(new Dimension(115, 28));

        slotHotKey = new JLabel("", SwingConstants.CENTER);

        ButtonProperties slotClearHotKeyButtonProps = new ButtonProperties("Clear Hot Key", new Dimension(17, 20),
                0.70f, 0.60f);
        slotClearHotKeyButton = new Button("/clear_hot_key_idle.svg", "/clear_hot_key_hover.svg",
                slotClearHotKeyButtonProps, false);

        slotChangeHotKeyButton = new JButton("Change Hot Key");
        slotChangeHotKeyButton.setPreferredSize(new Dimension(150, 28));

        buttons = new ArrayList<>();
        buttons.add(applyDisplayModeButton);
        buttons.add(slotClearHotKeyButton);
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
     * Gets the apply display mode button.
     *
     * @return The apply display mode button
     */
    public Button getApplyDisplayModeButton() {
        return applyDisplayModeButton;
    }

    /**
     * Gets the display modes combo box of the slot.
     *
     * @return The display modes combo box of the slot
     */
    public JComboBox<DisplayMode> getDisplayModes() {
        return slotDisplayModes;
    }

    /**
     * Gets the scaling modes combo box of the slot.
     *
     * @return The scaling modes combo box of the slot
     */
    public JComboBox<String> getScalingModes() {
        return slotScalingModes;
    }

    /**
     * Gets the DPI scale percentages combo box of the slot.
     *
     * @return The DPI scale percentages combo box of the slot
     */
    public JComboBox<Integer> getDpiScalePercentages() {
        return slotDpiScalePercentages;
    }

    /**
     * Gets the orientation modes combo box of the slot.
     *
     * @return The orientation modes combo box of the slot
     */
    public JComboBox<String> getOrientationModes() {
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
        return slotClearHotKeyButton;
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
     * Gets a list of buttons in the slot.
     *
     * @return A list of buttons in the slot
     */
    public List<Button> getButtons() {
        return buttons;
    }

}
