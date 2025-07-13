package com.dhk.ui.button;

import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.Icon;
import javax.swing.JButton;
import com.formdev.flatlaf.extras.FlatSVGIcon;

/**
 * This class defines the Apply Display Mode button. The icons for the different states of the button are defined here
 * along with the slot and display that the button resides in.
 * 
 * @author Jonathan Miller
 * @version 1.5.1
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
 */
public class ApplyDisplayModeButton extends JButton {
    private static final long serialVersionUID = 1L;
    private Icon applyDisplayModeIdleIcon;
    private Icon applyDisplayModeHoverIcon;
    private Icon applyDisplayModePressedIcon;
    private int slotIndex;
    private int displayIndex;

    // Set a fixed size for the button icon.
    private final Dimension BUTTON_ICON_SIZE = new Dimension(28, 28);

    /**
     * Constructor for the ApplyDisplayModeButton class.
     * 
     * @param applyDisplayModeIdleIconPath  - The resource path for the apply display mode button idle icon.
     * @param applyDisplayModeHoverIconPath - The resource path for the apply display mode button hover icon.
     * @param slotIndex                     - The index of the slot that this button resides in.
     * @param displayIndex                  - The displayIndex of the slot that this button resides in.
     */
    public ApplyDisplayModeButton(String applyDisplayModeIdleIconPath, String applyDisplayModeHoverIconPath,
            int slotIndex, int displayIndex) {
        // Initialize apply display mode button icons.
        applyDisplayModeIdleIcon = new FlatSVGIcon(getClass().getResource(applyDisplayModeIdleIconPath)).derive(0.80f);
        applyDisplayModeHoverIcon = new FlatSVGIcon(getClass().getResource(applyDisplayModeHoverIconPath))
                .derive(0.80f);
        applyDisplayModePressedIcon = new FlatSVGIcon(getClass().getResource(applyDisplayModeHoverIconPath))
                .derive(0.68f);

        // Initialize the slot that this button resides in.
        this.slotIndex = slotIndex;
        this.displayIndex = displayIndex;

        // Initialize the apply display mode button icon to the idle icon.
        this.setIcon(applyDisplayModeIdleIcon);

        // Set the tooltip for the button.
        this.setToolTipText("Apply Display Mode");

        // Set the initial button size.
        this.setPreferredSize(BUTTON_ICON_SIZE);

        // Remove all input mapping from the button.
        this.getInputMap().clear();

        // Only show the icon for the button.
        this.setBorderPainted(false);
        this.setContentAreaFilled(false);
        this.setFocusPainted(false);

        // Remove space around the icon.
        this.setMargin(new Insets(0, 0, 0, 0));
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Getters and Setters
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Getter for the apply display mode button icon when it is idle.
     * 
     * @return The idle apply display mode button icon.
     */
    public Icon getApplyDisplayModeIdleIcon() {
        return applyDisplayModeIdleIcon;
    }

    /**
     * Getter for the apply display mode button icon when the cursor is over the button or the button is in focus.
     * 
     * @return The apply display mode button hover icon.
     */
    public Icon getApplyDisplayModeHoverIcon() {
        return applyDisplayModeHoverIcon;
    }

    /**
     * Getter for the apply display mode button icon when the button is held down.
     * 
     * @return The pressed apply display mode button icon.
     */
    public Icon getApplyDisplayModePressedIcon() {
        return applyDisplayModePressedIcon;
    }

    /**
     * Getter for the slot index that this button resides in.
     * 
     * @return The slot index that this button resides in.
     */
    public int getSlotIndex() {
        return slotIndex;
    }

    /**
     * Getter for the display index that this button resides in.
     * 
     * @return The display index that this button resides in.
     */
    public int getDisplayIndex() {
        return displayIndex;
    }
}
