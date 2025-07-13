package com.dhk.ui;

import java.awt.Dimension;
import java.awt.DisplayMode;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import com.dhk.ui.button.ApplyDisplayModeButton;
import com.dhk.ui.button.ClearHotKeyButton;

/**
 * This class defines the view components of a Slot.
 * 
 * @author Jonathan Miller
 * @version 1.5.1
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
 */
public class Slot {
    private JLabel slotIndicatorLabel;
    private ApplyDisplayModeButton applyDisplayModeButton;
    private JComboBox<DisplayMode> slotDisplayModes;
    private JComboBox<String> slotScalingModes;
    private JComboBox<Integer> slotDpiScalePercentages;
    private JLabel slotHotKey;
    private ClearHotKeyButton slotClearHotKeyButton;
    private JButton slotChangeHotKeyButton;

    /**
     * Constructor for the Slot class.
     * 
     * @param slotIndex           - The index of the slot.
     * @param displayIndex        - The index of the display the slot resides in.
     * @param displayModes        - The array of display modes for the slot.
     * @param scalingModes        - The array of scaling modes for the slot.
     * @param dpiScalePercentages - The array of DPI scale percentages for the slot.
     */
    public Slot(int slotIndex, int displayIndex, DisplayMode[] displayModes, String[] scalingModes,
            Integer[] dpiScalePercentages) {
        // Increment the slot ID string.
        String slotID = Integer.toString(slotIndex + 1);

        // Initialize the slot indicator label component.
        slotIndicatorLabel = new JLabel("Slot " + slotID + " :", SwingConstants.CENTER);
        slotIndicatorLabel.setPreferredSize(new Dimension(50, 28));

        // Initialize the button used to immediately apply the slot's display settings.
        applyDisplayModeButton = new ApplyDisplayModeButton("/apply_idle.svg", "/apply_hover.svg", slotIndex,
                displayIndex);

        // Initialize the display modes combo box component.
        slotDisplayModes = new JComboBox<DisplayMode>(displayModes);
        slotDisplayModes.setPreferredSize(new Dimension(220, 28));

        // Initialize the scaling modes combo box component.
        slotScalingModes = new JComboBox<String>(scalingModes);
        slotScalingModes.setPreferredSize(new Dimension(110, 28));

        // Initialize the DPI scale percentages combo box component.
        slotDpiScalePercentages = new JComboBox<Integer>(dpiScalePercentages);
        slotDpiScalePercentages.setPreferredSize(new Dimension(70, 28));

        // Initialize the current hot key component.
        slotHotKey = new JLabel("", SwingConstants.CENTER);

        // Initialize the clear hot key button component.
        slotClearHotKeyButton = new ClearHotKeyButton("/clear_hot_key_idle.svg", "/clear_hot_key_hover.svg");

        // Initialize the change hot key button component.
        slotChangeHotKeyButton = new JButton("Change Hot Key");
        slotChangeHotKeyButton.setPreferredSize(new Dimension(150, 28));
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Getters and Setters
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Getter for the indicator label of the slot.
     * 
     * @return The indicator label of the slot.
     */
    public JLabel getIndicatorLabel() {
        return slotIndicatorLabel;
    }

    /**
     * Getter for the apply display mode button.
     * 
     * @return The apply display mode button.
     */
    public ApplyDisplayModeButton getApplyDisplayModeButton() {
        return applyDisplayModeButton;
    }

    /**
     * Getter for the display modes combo box of the slot.
     * 
     * @return The display modes combo box of the slot.
     */
    public JComboBox<DisplayMode> getDisplayModes() {
        return slotDisplayModes;
    }

    /**
     * Getter for the scaling modes combo box of the slot.
     * 
     * @return The scaling modes combo box of the slot.
     */
    public JComboBox<String> getScalingModes() {
        return slotScalingModes;
    }

    /**
     * Getter for the DPI scale percentages combo box of the slot.
     * 
     * @return The DPI scale percentages combo box of the slot.
     */
    public JComboBox<Integer> getDpiScalePercentages() {
        return slotDpiScalePercentages;
    }

    /**
     * Getter for the hot key of the slot.
     * 
     * @return The hot key of the slot.
     */
    public JLabel getHotKey() {
        return slotHotKey;
    }

    /**
     * Getter for the clear hot key button of the slot.
     * 
     * @return The clear hot key button of the slot.
     */
    public ClearHotKeyButton getClearHotKeyButton() {
        return slotClearHotKeyButton;
    }

    /**
     * Getter for the change hot key button of the slot.
     * 
     * @return The change hot key button of the slot.
     */
    public JButton getChangeHotKeyButton() {
        return slotChangeHotKeyButton;
    }
}
