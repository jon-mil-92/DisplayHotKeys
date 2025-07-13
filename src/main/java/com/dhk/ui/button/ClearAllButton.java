package com.dhk.ui.button;

import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.Icon;
import javax.swing.JButton;
import com.formdev.flatlaf.extras.FlatSVGIcon;

/**
 * This class defines the Clear All button. The icons for the different states of the button are defined here.
 * 
 * @author Jonathan Miller
 * @version 1.5.1
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
 */
public class ClearAllButton extends JButton {
    private static final long serialVersionUID = 1L;
    private Icon clearAllIdleIcon;
    private Icon clearAllHoverIcon;
    private Icon clearAllPressedIcon;

    // Set a fixed size for the button icon.
    private final Dimension BUTTON_ICON_SIZE = new Dimension(44, 50);

    /**
     * Constructor for the ClearAllButton class.
     * 
     * @param clearAllIdleIconPath  - The resource path for the clear all button idle icon.
     * @param clearAllHoverIconPath - The resource path for the clear all button hover icon.
     */
    public ClearAllButton(String clearAllIdleIconPath, String clearAllHoverIconPath) {
        // Initialize clear all button icons.
        clearAllIdleIcon = new FlatSVGIcon(getClass().getResource(clearAllIdleIconPath)).derive(0.80f);
        clearAllHoverIcon = new FlatSVGIcon(getClass().getResource(clearAllHoverIconPath)).derive(0.80f);
        clearAllPressedIcon = new FlatSVGIcon(getClass().getResource(clearAllHoverIconPath)).derive(0.68f);

        // Initialize the clear all button icon to idle icon.
        this.setIcon(clearAllIdleIcon);

        // Set the tooltip for the button.
        this.setToolTipText("Clear All Slots");

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
     * Getter for the clear all button icon when it is idle.
     * 
     * @return The idle clear all button icon.
     */
    public Icon getClearAllIdleIcon() {
        return clearAllIdleIcon;
    }

    /**
     * Getter for the clear all button icon when the cursor is over the button or the button is in focus.
     * 
     * @return The clear all button hover icon.
     */
    public Icon getClearAllHoverIcon() {
        return clearAllHoverIcon;
    }

    /**
     * Getter for the clear all button icon when the button is held down.
     * 
     * @return The pressed clear all button icon.
     */
    public Icon getClearAllPressedIcon() {
        return clearAllPressedIcon;
    }
}
