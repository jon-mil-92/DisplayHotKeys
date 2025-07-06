package com.dhk.ui.buttons;

import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.Icon;
import javax.swing.JButton;
import com.formdev.flatlaf.extras.FlatSVGIcon;

/**
 * This class defines the Minimize button. The icons for the different states of the button are defined here.
 * 
 * @author Jonathan Miller
 * @version 1.5.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
 */
public class MinimizeButton extends JButton {
    private static final long serialVersionUID = 1L;
    private Icon minimizeIdleIcon;
    private Icon minimizeHoverIcon;
    private Icon minimizePressedIcon;

    // Set a fixed size for the button icon.
    private final Dimension BUTTON_ICON_SIZE = new Dimension(32, 50);

    /**
     * Constructor for the MinimizeButton class.
     * 
     * @param minimizeIdleIconPath   - The resource path for the minimize button idle icon.
     * @param minimimzeHoverIconPath - The resource path for the minimize button hover icon.
     */
    public MinimizeButton(String minimizeIdleIconPath, String minimimzeHoverIconPath) {
        // Initialize minimize button icons.
        minimizeIdleIcon = new FlatSVGIcon(getClass().getResource(minimizeIdleIconPath)).derive(0.80f);
        minimizeHoverIcon = new FlatSVGIcon(getClass().getResource(minimimzeHoverIconPath)).derive(0.80f);
        minimizePressedIcon = new FlatSVGIcon(getClass().getResource(minimimzeHoverIconPath)).derive(0.68f);

        // Initialize the minimize button icon to the idle icon.
        this.setIcon(minimizeIdleIcon);

        // Set the tooltip for the button.
        this.setToolTipText("Minimize To Tray");

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
     * Getter for the minimize button icon when it is idle.
     * 
     * @return The idle minimize button icon.
     */
    public Icon getMinimizeIdleIcon() {
        return minimizeIdleIcon;
    }

    /**
     * Getter for the minimize button icon when the cursor is over the button or the button is in focus.
     * 
     * @return The minimize button hover icon.
     */
    public Icon getMinimizeHoverIcon() {
        return minimizeHoverIcon;
    }

    /**
     * Getter for the minimize button icon when the button is held down.
     * 
     * @return The pressed minimize button icon.
     */
    public Icon getMinimizePressedIcon() {
        return minimizePressedIcon;
    }
}
