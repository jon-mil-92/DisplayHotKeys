package com.dhk.ui.buttons;

import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.JButton;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import javax.swing.Icon;

/**
 * This class defines the Theme button. The icons for the different states of the button are defined here.
 * 
 * @author Jonathan Miller
 * @version 1.3.2
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class ThemeButton extends JButton {
    private static final long serialVersionUID = 1L;
    private Icon darkModeIdleIcon;
    private Icon lightModeIdleIcon;
    private Icon darkModeHoverIcon;
    private Icon lightModeHoverIcon;
    private Icon darkModePressedIcon;
    private Icon lightModePressedIcon;

    // Set a fixed size for the button icon.
    private final Dimension BUTTON_ICON_SIZE = new Dimension(50, 50);

    /**
     * Constructor for the ThemeButton class.
     * 
     * @param darkMode               - The initial dark mode state of the button.
     * @param darkModeIdleIconPath   - The resource path for the dark mode idle icon.
     * @param darkModeHoverIconPath  - The resource path for the dark mode hover icon.
     * @param lightModeIdleIconPath  - The resource path for the light mode idle icon.
     * @param lightModeHoverIconPath - The resource path for the light mode hover icon.
     */
    public ThemeButton(boolean darkMode, String darkModeIdleIconPath, String darkModeHoverIconPath,
            String lightModeIdleIconPath, String lightModeHoverIconPath) {
        // Initialize dark theme button icons.
        darkModeIdleIcon = new FlatSVGIcon(getClass().getResource(darkModeIdleIconPath)).derive(0.80f);
        darkModeHoverIcon = new FlatSVGIcon(getClass().getResource(darkModeHoverIconPath)).derive(0.80f);
        darkModePressedIcon = new FlatSVGIcon(getClass().getResource(darkModeHoverIconPath)).derive(0.68f);

        // Initialize light theme button icons.
        lightModeIdleIcon = new FlatSVGIcon(getClass().getResource(lightModeIdleIconPath)).derive(0.80f);
        lightModeHoverIcon = new FlatSVGIcon(getClass().getResource(lightModeHoverIconPath)).derive(0.80f);
        lightModePressedIcon = new FlatSVGIcon(getClass().getResource(lightModeHoverIconPath)).derive(0.68f);

        // If the UI is in dark mode...
        if (darkMode) {
            // Initialize the theme button icon to the dark mode icon.
            this.setIcon(darkModeIdleIcon);
        }
        // Otherwise, if the UI is in light mode...
        else {
            // Initialize the theme button icon to the light mode icon.
            this.setIcon(lightModeIdleIcon);
        }

        // Set the tooltip for the button.
        this.setToolTipText("Change Theme");

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
     * Getter for the dark mode theme button icon when it is idle.
     * 
     * @return The idle dark mode theme button icon.
     */
    public Icon getDarkModeIdleIcon() {
        return darkModeIdleIcon;
    }

    /**
     * Getter for the light mode theme button icon when it is idle.
     * 
     * @return The idle light mode theme button icon.
     */
    public Icon getLightModeIdleIcon() {
        return lightModeIdleIcon;
    }

    /**
     * Getter for the dark mode theme button icon when the cursor is over the button or the button is in focus.
     * 
     * @return The dark mode theme button hover icon.
     */
    public Icon getDarkModeHoverIcon() {
        return darkModeHoverIcon;
    }

    /**
     * Getter for the light mode theme button icon when the cursor is over the button or the button is in focus.
     * 
     * @return The light mode theme button hover icon.
     */
    public Icon getLightModeHoverIcon() {
        return lightModeHoverIcon;
    }

    /**
     * Getter for the dark mode theme button icon when the button is held down.
     * 
     * @return The pressed dark mode theme button icon.
     */
    public Icon getDarkModePressedIcon() {
        return darkModePressedIcon;
    }

    /**
     * Getter for the light mode theme button icon when the button is held down.
     * 
     * @return The pressed light mode theme button icon.
     */
    public Icon getLightModePressedIcon() {
        return lightModePressedIcon;
    }
}
