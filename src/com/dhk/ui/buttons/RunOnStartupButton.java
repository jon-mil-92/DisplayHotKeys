package com.dhk.ui.buttons;

import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.Icon;
import javax.swing.JButton;
import com.formdev.flatlaf.extras.FlatSVGIcon;

/**
 * This class defines the Run On Startup button. The icons for the different states of the button are defined here.
 * 
 * @author Jonathan Miller
 * @version 1.5.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
 */
public class RunOnStartupButton extends JButton {
    private static final long serialVersionUID = 1L;
    private Icon runOnStartupEnabledIdleIcon;
    private Icon runOnStartupDisabledIdleIcon;
    private Icon runOnStartupDisabledDarkHoverIcon;
    private Icon runOnStartupDisabledLightHoverIcon;
    private Icon runOnStartupDisabledDarkPressedIcon;
    private Icon runOnStartupDisabledLightPressedIcon;
    private Icon runOnStartupEnabledDarkHoverIcon;
    private Icon runOnStartupEnabledLightHoverIcon;
    private Icon runOnStartupEnabledDarkPressedIcon;
    private Icon runOnStartupEnabledLightPressedIcon;

    // Set a fixed size for the button icon.
    private final Dimension BUTTON_ICON_SIZE = new Dimension(48, 50);

    /**
     * Constructor for the RunOnStartupButton class.
     *
     * @param runOnStartup                           - Whether or not the application will run when the user logs into
     *                                               Windows.
     * @param runOnStartupEnabledIdleIconPath        - The resource path for the run on startup button enabled idle icon.
     * @param runOnStartupDisabledIdleIconPath       - The resource path for the run on startup button disable idle icon.
     * @param runOnStartupEnabledDarkHoverIconPath   - The resource path for the run on startup button enabled hover dark icon.
     * @param runOnStartupEnabledLightHoverIconPath  - The resource path for the run on startup button enabled hover light
     *                                               icon.
     * @param runOnStartupDisabledDarkHoverIconPath  - The resource path for the run on startup button disabled hover dark
     *                                               icon.
     * @param runOnStartupDisabledLightHoverIconPath - The resource path for the run on startup button disabled hover light
     *                                               icon.
     */
    public RunOnStartupButton(boolean runOnStartup, String runOnStartupEnabledIdleIconPath,
            String runOnStartupDisabledIdleIconPath, String runOnStartupEnabledDarkHoverIconPath,
            String runOnStartupEnabledLightHoverIconPath, String runOnStartupDisabledDarkHoverIconPath,
            String runOnStartupDisabledLightHoverIconPath) {
        // Initialize run on startup enabled button icons.
        runOnStartupEnabledIdleIcon = new FlatSVGIcon(getClass().getResource(runOnStartupEnabledIdleIconPath))
                .derive(0.80f);
        runOnStartupEnabledDarkHoverIcon = new FlatSVGIcon(getClass().getResource(runOnStartupEnabledDarkHoverIconPath))
                .derive(0.80f);
        runOnStartupEnabledLightHoverIcon = new FlatSVGIcon(
                getClass().getResource(runOnStartupEnabledLightHoverIconPath)).derive(0.80f);
        runOnStartupEnabledDarkPressedIcon = new FlatSVGIcon(
                getClass().getResource(runOnStartupEnabledDarkHoverIconPath)).derive(0.68f);
        runOnStartupEnabledLightPressedIcon = new FlatSVGIcon(
                getClass().getResource(runOnStartupEnabledLightHoverIconPath)).derive(0.68f);

        // Initialize run on startup disabled button icons.
        runOnStartupDisabledIdleIcon = new FlatSVGIcon(getClass().getResource(runOnStartupDisabledIdleIconPath))
                .derive(0.80f);
        runOnStartupDisabledDarkHoverIcon = new FlatSVGIcon(
                getClass().getResource(runOnStartupDisabledDarkHoverIconPath)).derive(0.80f);
        runOnStartupDisabledLightHoverIcon = new FlatSVGIcon(
                getClass().getResource(runOnStartupDisabledLightHoverIconPath)).derive(0.80f);
        runOnStartupDisabledDarkPressedIcon = new FlatSVGIcon(
                getClass().getResource(runOnStartupDisabledDarkHoverIconPath)).derive(0.68f);
        runOnStartupDisabledLightPressedIcon = new FlatSVGIcon(
                getClass().getResource(runOnStartupDisabledLightHoverIconPath)).derive(0.68f);

        // If the application should run on startup...
        if (runOnStartup) {
            // Initialize the run on startup button icon to enabled idle icon.
            this.setIcon(runOnStartupEnabledIdleIcon);
        } else {
            // Initialize the run on startup button icon to disabled idle icon.
            this.setIcon(runOnStartupDisabledIdleIcon);
        }

        // Set the tooltip for the button.
        this.setToolTipText("Run On Startup");

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
     * Getter for the run on startup button idle icon when it is enabled.
     * 
     * @return The run on startup enabled idle button icon.
     */
    public Icon getRunOnStartupEnabledIdleIcon() {
        return runOnStartupEnabledIdleIcon;
    }

    /**
     * Getter for the run on startup button idle icon when it is disabled.
     * 
     * @return The run on startup disabled idle button icon.
     */
    public Icon getRunOnStartupDisabledIdleIcon() {
        return runOnStartupDisabledIdleIcon;
    }

    /**
     * Getter for the run on startup enabled dark button icon when the cursor is over the button or the button is in
     * focus.
     * 
     * @return The run on startup enabled dark button hover icon.
     */
    public Icon getRunOnStartupEnabledDarkHoverIcon() {
        return runOnStartupEnabledDarkHoverIcon;
    }

    /**
     * Getter for the run on startup enabled light button icon when the cursor is over the button or the button is in
     * focus.
     * 
     * @return The run on startup enabled light button hover icon.
     */
    public Icon getRunOnStartupEnabledLightHoverIcon() {
        return runOnStartupEnabledLightHoverIcon;
    }

    /**
     * Getter for the run on startup enabled dark button icon when the button is held down.
     * 
     * @return The pressed run on startup enabled dark button icon.
     */
    public Icon getRunOnStartupEnabledDarkPressedIcon() {
        return runOnStartupEnabledDarkPressedIcon;
    }

    /**
     * Getter for the run on startup enabled light button icon when the button is held down.
     * 
     * @return The pressed run on startup enabled light button icon.
     */
    public Icon getRunOnStartupEnabledLightPressedIcon() {
        return runOnStartupEnabledLightPressedIcon;
    }

    /**
     * Getter for the run on startup disabled dark button icon when the cursor is over the button or the button is in
     * focus.
     * 
     * @return The run on startup disabled dark button hover icon.
     */
    public Icon getRunOnStartupDisabledDarkHoverIcon() {
        return runOnStartupDisabledDarkHoverIcon;
    }

    /**
     * Getter for the run on startup disabled light button icon when the cursor is over the button or the button is in
     * focus.
     * 
     * @return The run on startup disabled light button hover icon.
     */
    public Icon getRunOnStartupDisabledLightHoverIcon() {
        return runOnStartupDisabledLightHoverIcon;
    }

    /**
     * Getter for the run on startup disabled dark button icon when the button is held down.
     * 
     * @return The pressed run on startup Disabled dark button icon.
     */
    public Icon getRunOnStartupDisabledDarkPressedIcon() {
        return runOnStartupDisabledDarkPressedIcon;
    }

    /**
     * Getter for the run on startup disabled light button icon when the button is held down.
     * 
     * @return The pressed run on startup disabled light button icon.
     */
    public Icon getRunOnStartupDisabledLightPressedIcon() {
        return runOnStartupDisabledLightPressedIcon;
    }
}
