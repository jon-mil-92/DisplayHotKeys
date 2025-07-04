package com.dhk.ui.buttons;

import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.JButton;

import com.formdev.flatlaf.extras.FlatSVGIcon;

/**
 * This class defines the Minimize To Tray button. The icons for the different states of the button are defined here.
 * 
 * @author Jonathan Miller
 * @version 1.5.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
 */
public class MinimizeToTrayButton extends JButton {
    private static final long serialVersionUID = 1L;
    private Icon minimizeToTrayEnabledIdleIcon;
    private Icon minimizeToTrayDisabledIdleIcon;
    private Icon minimizeToTrayDisabledDarkHoverIcon;
    private Icon minimizeToTrayDisabledLightHoverIcon;
    private Icon minimizeToTrayDisabledDarkPressedIcon;
    private Icon minimizeToTrayDisabledLightPressedIcon;
    private Icon minimizeToTrayEnabledDarkHoverIcon;
    private Icon minimizeToTrayEnabledLightHoverIcon;
    private Icon minimizeToTrayEnabledDarkPressedIcon;
    private Icon minimizeToTrayEnabledLightPressedIcon;

    // Set a fixed size for the button icon.
    private final Dimension BUTTON_ICON_SIZE = new Dimension(48, 50);

    /**
     * Constructor for the minimizeToTrayButton class.
     *
     * @param minimizeToTray                           - Whether or not the application will run when the user logs into
     *                                                 Windows.
     * @param minimizeToTrayEnabledIdleIconPath        - The resource path for the minimize to tray button enabled idle
     *                                                 icon.
     * @param minimizeToTrayDisabledIdleIconPath       - The resource path for the minimize to tray button disable idle
     *                                                 icon.
     * @param minimizeToTrayEnabledDarkHoverIconPath   - The resource path for the minimize to tray button enabled hover
     *                                                 dark icon.
     * @param minimizeToTrayEnabledLightHoverIconPath  - The resource path for the minimize to tray button enabled hover
     *                                                 light icon.
     * @param minimizeToTrayDisabledDarkHoverIconPath  - The resource path for the minimize to tray button disabled
     *                                                 hover dark icon.
     * @param minimizeToTrayDisabledLightHoverIconPath - The resource path for the minimize to tray button disabled
     *                                                 hover light icon.
     */
    public MinimizeToTrayButton(boolean minimizeToTray, String minimizeToTrayEnabledIdleIconPath,
            String minimizeToTrayDisabledIdleIconPath, String minimizeToTrayEnabledDarkHoverIconPath,
            String minimizeToTrayEnabledLightHoverIconPath, String minimizeToTrayDisabledDarkHoverIconPath,
            String minimizeToTrayDisabledLightHoverIconPath) {
        // Initialize minimize to tray enabled button icons.
        minimizeToTrayEnabledIdleIcon = new FlatSVGIcon(getClass().getResource(minimizeToTrayEnabledIdleIconPath))
                .derive(0.80f);
        minimizeToTrayEnabledDarkHoverIcon = new FlatSVGIcon(
                getClass().getResource(minimizeToTrayEnabledDarkHoverIconPath)).derive(0.80f);
        minimizeToTrayEnabledLightHoverIcon = new FlatSVGIcon(
                getClass().getResource(minimizeToTrayEnabledLightHoverIconPath)).derive(0.80f);
        minimizeToTrayEnabledDarkPressedIcon = new FlatSVGIcon(
                getClass().getResource(minimizeToTrayEnabledDarkHoverIconPath)).derive(0.68f);
        minimizeToTrayEnabledLightPressedIcon = new FlatSVGIcon(
                getClass().getResource(minimizeToTrayEnabledLightHoverIconPath)).derive(0.68f);

        // Initialize minimize to tray disabled button icons.
        minimizeToTrayDisabledIdleIcon = new FlatSVGIcon(getClass().getResource(minimizeToTrayDisabledIdleIconPath))
                .derive(0.80f);
        minimizeToTrayDisabledDarkHoverIcon = new FlatSVGIcon(
                getClass().getResource(minimizeToTrayDisabledDarkHoverIconPath)).derive(0.80f);
        minimizeToTrayDisabledLightHoverIcon = new FlatSVGIcon(
                getClass().getResource(minimizeToTrayDisabledLightHoverIconPath)).derive(0.80f);
        minimizeToTrayDisabledDarkPressedIcon = new FlatSVGIcon(
                getClass().getResource(minimizeToTrayDisabledDarkHoverIconPath)).derive(0.68f);
        minimizeToTrayDisabledLightPressedIcon = new FlatSVGIcon(
                getClass().getResource(minimizeToTrayDisabledLightHoverIconPath)).derive(0.68f);

        // If the application should minimize to tray...
        if (minimizeToTray) {
            // Initialize the minimize to tray button icon to enabled idle icon.
            this.setIcon(minimizeToTrayEnabledIdleIcon);
        } else {
            // Initialize the minimize to tray button icon to disabled idle icon.
            this.setIcon(minimizeToTrayDisabledIdleIcon);
        }

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
     * Getter for the minimize to tray button idle icon when it is enabled.
     * 
     * @return The minimize to tray enabled idle button icon.
     */
    public Icon getMinimizeToTrayEnabledIdleIcon() {
        return minimizeToTrayEnabledIdleIcon;
    }

    /**
     * Getter for the minimize to tray button idle icon when it is disabled.
     * 
     * @return The minimize to tray disabled idle button icon.
     */
    public Icon getMinimizeToTrayDisabledIdleIcon() {
        return minimizeToTrayDisabledIdleIcon;
    }

    /**
     * Getter for the minimize to tray enabled dark button icon when the cursor is over the button or the button is in
     * focus.
     * 
     * @return The minimize to tray enabled dark button hover icon.
     */
    public Icon getMinimizeToTrayEnabledDarkHoverIcon() {
        return minimizeToTrayEnabledDarkHoverIcon;
    }

    /**
     * Getter for the minimize to tray enabled light button icon when the cursor is over the button or the button is in
     * focus.
     * 
     * @return The minimize to tray enabled light button hover icon.
     */
    public Icon getMinimizeToTrayEnabledLightHoverIcon() {
        return minimizeToTrayEnabledLightHoverIcon;
    }

    /**
     * Getter for the minimize to tray enabled dark button icon when the button is held down.
     * 
     * @return The pressed minimize to tray enabled dark button icon.
     */
    public Icon getMinimizeToTrayEnabledDarkPressedIcon() {
        return minimizeToTrayEnabledDarkPressedIcon;
    }

    /**
     * Getter for the minimize to tray enabled light button icon when the button is held down.
     * 
     * @return The pressed minimize to tray enabled light button icon.
     */
    public Icon getMinimizeToTrayEnabledLightPressedIcon() {
        return minimizeToTrayEnabledLightPressedIcon;
    }

    /**
     * Getter for the minimize to tray disabled dark button icon when the cursor is over the button or the button is in
     * focus.
     * 
     * @return The minimize to tray disabled dark button hover icon.
     */
    public Icon getMinimizeToTrayDisabledDarkHoverIcon() {
        return minimizeToTrayDisabledDarkHoverIcon;
    }

    /**
     * Getter for the minimize to tray disabled light button icon when the cursor is over the button or the button is in
     * focus.
     * 
     * @return The minimize to tray disabled light button hover icon.
     */
    public Icon getMinimizeToTrayDisabledLightHoverIcon() {
        return minimizeToTrayDisabledLightHoverIcon;
    }

    /**
     * Getter for the minimize to tray disabled dark button icon when the button is held down.
     * 
     * @return The pressed minimize to tray disabled dark button icon.
     */
    public Icon getMinimizeToTrayDisabledDarkPressedIcon() {
        return minimizeToTrayDisabledDarkPressedIcon;
    }

    /**
     * Getter for the minimize to tray disabled light button icon when the button is held down.
     * 
     * @return The pressed minimize to tray disabled light button icon.
     */
    public Icon getMinimizeToTrayDisabledLightPressedIcon() {
        return minimizeToTrayDisabledLightPressedIcon;
    }
}
