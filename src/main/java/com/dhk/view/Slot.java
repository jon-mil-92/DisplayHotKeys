package com.dhk.view;

import java.awt.Dimension;
import java.awt.DisplayMode;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import com.dhk.model.button.Button;

/**
 * Defines the view components of a Slot.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
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

    private final String APPLY_DISPLAY_MODE_BUTTON_TOOLTIP = "Apply Display Mode";
    private final Dimension APPLY_DISPLAY_MODE_BUTTON_SIZE = new Dimension(28, 28);
    private final float APPLY_DISPLAY_MODE_BUTTON_IDLE_SCALE = 0.80f;
    private final float APPLY_DISPLAY_MODE_BUTTON_HELD_SCALE = 0.68f;

    private final String CLEAR_HOT_KEY_BUTTON_TOOLTIP = "Clear Hot Key";
    private final Dimension CLEAR_HOT_KEY_BUTTON_SIZE = new Dimension(20, 24);
    private final float CLEAR_HOT_KEY_BUTTON_IDLE_SCALE = 0.70f;
    private final float CLEAR_HOT_KEY_BUTTON_HELD_SCALE = 0.60f;

    /**
     * Constructor for the Slot class.
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

        applyDisplayModeButton = new Button("/apply_idle.svg", "/apply_hover.svg", APPLY_DISPLAY_MODE_BUTTON_TOOLTIP,
                APPLY_DISPLAY_MODE_BUTTON_SIZE, APPLY_DISPLAY_MODE_BUTTON_IDLE_SCALE,
                APPLY_DISPLAY_MODE_BUTTON_HELD_SCALE, true);

        slotDisplayModes = new JComboBox<DisplayMode>(displayModes);
        slotDisplayModes.setPreferredSize(new Dimension(220, 28));

        slotScalingModes = new JComboBox<String>(scalingModes);
        slotScalingModes.setPreferredSize(new Dimension(110, 28));

        slotDpiScalePercentages = new JComboBox<Integer>(dpiScalePercentages);
        slotDpiScalePercentages.setPreferredSize(new Dimension(70, 28));

        slotOrientationModes = new JComboBox<String>(orientationModes);
        slotOrientationModes.setPreferredSize(new Dimension(115, 28));

        slotHotKey = new JLabel("", SwingConstants.CENTER);

        slotClearHotKeyButton = new Button("/clear_hot_key_idle.svg", "/clear_hot_key_hover.svg",
                CLEAR_HOT_KEY_BUTTON_TOOLTIP, CLEAR_HOT_KEY_BUTTON_SIZE, CLEAR_HOT_KEY_BUTTON_IDLE_SCALE,
                CLEAR_HOT_KEY_BUTTON_HELD_SCALE, false);

        slotChangeHotKeyButton = new JButton("Change Hot Key");
        slotChangeHotKeyButton.setPreferredSize(new Dimension(150, 28));
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

}
