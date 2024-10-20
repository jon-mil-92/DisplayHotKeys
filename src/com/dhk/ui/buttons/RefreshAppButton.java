package com.dhk.ui.buttons;

import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.Icon;
import javax.swing.JButton;
import com.formdev.flatlaf.extras.FlatSVGIcon;

/**
 * This class defines the Refresh App button. The icons for the different states of the button are defined here.
 * 
 * @author Jonathan Miller
 * @version 1.4.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class RefreshAppButton extends JButton {
    private static final long serialVersionUID = 1L;
    private Icon refreshAppIdleIcon;
    private Icon refreshAppHoverIcon;
    private Icon refreshAppPressedIcon;

    // Set a fixed size for the button icon.
    private final Dimension BUTTON_ICON_SIZE = new Dimension(40, 50);

    /**
     * Constructor for the RefreshAppButton class.
     * 
     * @param refreshAppIdleIconPath  - The resource path for the refresh app button idle icon.
     * @param refreshAppHoverIconPath - The resource path for the refresh app button hover icon.
     */
    public RefreshAppButton(String refreshAppIdleIconPath, String refreshAppHoverIconPath) {
        // Initialize refresh app button icons.
        refreshAppIdleIcon = new FlatSVGIcon(getClass().getResource(refreshAppIdleIconPath)).derive(0.80f);
        refreshAppHoverIcon = new FlatSVGIcon(getClass().getResource(refreshAppHoverIconPath)).derive(0.80f);
        refreshAppPressedIcon = new FlatSVGIcon(getClass().getResource(refreshAppHoverIconPath)).derive(0.68f);

        // Initialize the refresh app button icon to the idle icon.
        this.setIcon(refreshAppIdleIcon);

        // Set the tooltip for the button.
        this.setToolTipText("Refresh App");

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
     * Getter for the refresh app button icon when it is idle.
     * 
     * @return The idle refresh app button icon.
     */
    public Icon getRefreshAppIdleIcon() {
        return refreshAppIdleIcon;
    }

    /**
     * Getter for the refresh app button icon when the cursor is over the button or the button is in focus.
     * 
     * @return The refresh app button hover icon.
     */
    public Icon getRefreshAppHoverIcon() {
        return refreshAppHoverIcon;
    }

    /**
     * Getter for the refresh app button icon when the button is held down.
     * 
     * @return The pressed refresh app button icon.
     */
    public Icon getRefreshAppPressedIcon() {
        return refreshAppPressedIcon;
    }
}
