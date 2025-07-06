package com.dhk.ui.buttons;

import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.Icon;
import javax.swing.JButton;
import com.formdev.flatlaf.extras.FlatSVGIcon;

/**
 * This class defines the Exit button. The icons for the different states of the button are defined here.
 * 
 * @author Jonathan Miller
 * @version 1.5.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
 */
public class ExitButton extends JButton {
    private static final long serialVersionUID = 1L;
    private Icon exitIdleIcon;
    private Icon exitHoverIcon;
    private Icon exitPressedIcon;

    // Set a fixed size for the button icon.
    private final Dimension BUTTON_ICON_SIZE = new Dimension(34, 50);

    /**
     * Constructor for the ExitButton class.
     * 
     * @param exitIdleIconPath  - The resource path for the exit button idle icon.
     * @param exitHoverIconPath - The resource path for the exit button hover icon.
     */
    public ExitButton(String exitIdleIconPath, String exitHoverIconPath) {
        // Initialize exit button icons.
        exitIdleIcon = new FlatSVGIcon(getClass().getResource(exitIdleIconPath)).derive(0.80f);
        exitHoverIcon = new FlatSVGIcon(getClass().getResource(exitHoverIconPath)).derive(0.80f);
        exitPressedIcon = new FlatSVGIcon(getClass().getResource(exitHoverIconPath)).derive(0.68f);

        // Initialize the exit button icon to the idle icon.
        this.setIcon(exitIdleIcon);

        // Set the tooltip for the button.
        this.setToolTipText("Exit App");

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
     * Getter for the exit button icon when it is idle.
     * 
     * @return The idle exit button icon.
     */
    public Icon getExitIdleIcon() {
        return exitIdleIcon;
    }

    /**
     * Getter for the exit button icon when the cursor is over the button or the button is in focus.
     * 
     * @return The exit button hover icon.
     */
    public Icon getExitHoverIcon() {
        return exitHoverIcon;
    }

    /**
     * Getter for the exit button icon when the button is held down.
     * 
     * @return The pressed exit button icon.
     */
    public Icon getExitPressedIcon() {
        return exitPressedIcon;
    }
}
