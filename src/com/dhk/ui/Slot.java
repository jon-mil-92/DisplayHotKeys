package com.dhk.ui;

import java.awt.Dimension;
import java.awt.DisplayMode;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * This class defines the view components of a Slot.
 * 
 * @author Jonathan Miller
 * @version 1.3.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class Slot {
    private JLabel slotIndicatorLabel;
    private JComboBox<DisplayMode> slotDisplayModes;
    private JComboBox<String> slotScalingModes;
    private JComboBox<Integer> slotDpiScalePercentages;
    private JButton slotChangeHotKeyButton;
    private JButton slotClearHotKeyButton;
    private JLabel slotHotKey;

    /**
     * Constructor for the Slot class.
     * 
     * @param slotIndex           - The index of the slot.
     * @param displayModes        - The array of display modes for the slot.
     * @param scalingModes        - The array of scaling modes for the slot.
     * @param dpiScalePercentages - The array of DPI scale percentages for the slot.
     */
    public Slot(String slotIndex, DisplayMode[] displayModes, String[] scalingModes, Integer[] dpiScalePercentages) {
        // Increment the slot ID string.
        String slotID = Integer.toString(Integer.parseInt(slotIndex) + 1);

        // Initialize the slot indicator label component.
        slotIndicatorLabel = new JLabel("Hot Key Slot " + slotID + " :", SwingConstants.CENTER);
        slotIndicatorLabel.setPreferredSize(new Dimension(105, 28));

        // Initialize the display modes combo box component.
        slotDisplayModes = new JComboBox<DisplayMode>(displayModes);
        slotDisplayModes.setPreferredSize(new Dimension(220, 28));

        // Initialize the scaling modes combo box component.
        slotScalingModes = new JComboBox<String>(scalingModes);
        slotScalingModes.setPreferredSize(new Dimension(110, 28));

        // Initialize the DPI scale percentages combo box component.
        slotDpiScalePercentages = new JComboBox<Integer>(dpiScalePercentages);
        slotDpiScalePercentages.setPreferredSize(new Dimension(70, 28));

        // Initialize the change hot key button component.
        slotChangeHotKeyButton = new JButton("Change Hot Key");
        slotChangeHotKeyButton.setPreferredSize(new Dimension(150, 28));

        // Initialize the clear hot key button component.
        slotClearHotKeyButton = new JButton("Clear Hot Key");
        slotClearHotKeyButton.setPreferredSize(new Dimension(135, 28));

        // Initialize the current hot key component.
        slotHotKey = new JLabel("", SwingConstants.CENTER);
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
     * Getter for the change hot key button of the slot.
     * 
     * @return The change hot key button of the slot.
     */
    public JButton getChangeHotKeyButton() {
        return slotChangeHotKeyButton;
    }

    /**
     * Getter for the clear hot key button of the slot.
     * 
     * @return The clear hot key button of the slot.
     */
    public JButton getClearHotKeyButton() {
        return slotClearHotKeyButton;
    }

    /**
     * Getter for the hot key of the slot.
     * 
     * @return The hot key of the slot.
     */
    public JLabel getHotKey() {
        return slotHotKey;
    }
}
