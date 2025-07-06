package com.dhk.ui.buttons;

import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.Icon;
import javax.swing.JButton;
import com.formdev.flatlaf.extras.FlatSVGIcon;

/**
 * This class defines the Clear Hot Key button. The icons for the different states of the button are defined here.
 * 
 * @author Jonathan Miller
 * @version 1.5.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
 */
public class ClearHotKeyButton extends JButton {
    private static final long serialVersionUID = 1L;
    private Icon clearHotKeyIdleIcon;
    private Icon clearHotKeyHoverIcon;
    private Icon clearHotKeyPressedIcon;

    // Set a fixed size for the button icon.
    private final Dimension BUTTON_ICON_SIZE = new Dimension(20, 24);

    /**
     * Constructor for the ClearHotKeyButton class.
     * 
     * @param clearHotKeyIdleIconPath  - The resource path for the clear hot key button idle icon.
     * @param clearHotKeyHoverIconPath - The resource path for the clear hot key button hover icon.
     */
    public ClearHotKeyButton(String clearHotKeyIdleIconPath, String clearHotKeyHoverIconPath) {
        // Initialize clear hot key button icons.
        clearHotKeyIdleIcon = new FlatSVGIcon(getClass().getResource(clearHotKeyIdleIconPath)).derive(0.70f);
        clearHotKeyHoverIcon = new FlatSVGIcon(getClass().getResource(clearHotKeyHoverIconPath)).derive(0.70f);
        clearHotKeyPressedIcon = new FlatSVGIcon(getClass().getResource(clearHotKeyHoverIconPath)).derive(0.60f);

        // Initialize the clear hot key button icon to idle icon.
        this.setIcon(clearHotKeyIdleIcon);

        // Set the tooltip for the button.
        this.setToolTipText("Clear Hot Key");

        // Set the initial button size.
        this.setPreferredSize(BUTTON_ICON_SIZE);

        // Remove hot key input mapping from the button.
        this.getInputMap().clear();

        // Only show the icon for the button.
        this.setBorderPainted(false);
        this.setContentAreaFilled(false);
        this.setFocusPainted(false);

        // Remove space around the icon.
        this.setMargin(new Insets(0, 0, 0, 0));

        // Disable the button by default.
        this.setEnabled(false);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Getters and Setters
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Getter for the clear hot key button icon when it is idle.
     * 
     * @return The idle clear hot key button icon.
     */
    public Icon getClearHotKeyIdleIcon() {
        return clearHotKeyIdleIcon;
    }

    /**
     * Getter for the clear hot key button icon when the cursor is over the button or the button is in focus.
     * 
     * @return The clear hot key button hover icon.
     */
    public Icon getClearHotKeyHoverIcon() {
        return clearHotKeyHoverIcon;
    }

    /**
     * Getter for the clear hot key button icon when the button is held down.
     * 
     * @return The pressed clear hot key button icon.
     */
    public Icon getClearHotKeyPressedIcon() {
        return clearHotKeyPressedIcon;
    }
}
