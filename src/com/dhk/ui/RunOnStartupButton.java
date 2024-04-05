package com.dhk.ui;

import java.awt.Dimension;
import javax.swing.Icon;
import javax.swing.JButton;
import com.formdev.flatlaf.extras.FlatSVGIcon;

/**
 * This class defines the Run On Startup button. The icons for the different states of the button are defined here.
 * 
 * @author Jonathan Miller
 * @version 1.2.1
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class RunOnStartupButton extends JButton {
    private static final long serialVersionUID = 1L;
    private Icon runOnStartupEnabledIdleIcon;
    private Icon runOnStartupDisabledIdleIcon;
    private Icon runOnStartupEnabledHoverIcon;
    private Icon runOnStartupDisabledHoverIcon;
    private Icon runOnStartupEnabledPressedIcon;
    private Icon runOnStartupDisabledPressedIcon;
    private boolean runOnStartup;

    // Set a fixed size for the button icon.
    private final Dimension BUTTON_ICON_SIZE = new Dimension(48, 48);

    /**
     * Constructor for the RunOnStartupButton class.
     *
     * @param runOnStartup                      - Whether or not the application will run when the user logs into
     *                                            Windows.
     * @param runOnStartupEnabledIdleIconPath   - The resource path for the startup button enabled idle icon.
     * @param runOnStartupDisabledIdleIconPath  - The resource path for the startup button disable idle icon.
     * @param runOnStartupEnabledHoverIconPath  - The resource path for the startup button enabled hover icon.
     * @param runOnStartupDisabledHoverIconPath - The resource path for the startup button disabled hover icon.
     */
    public RunOnStartupButton(boolean runOnStartup, String runOnStartupEnabledIdleIconPath,
            String runOnStartupDisabledIdleIconPath, String runOnStartupEnabledHoverIconPath,
            String runOnStartupDisabledHoverIconPath) {
        // Initialize the "run on startup" state of the button.
        this.runOnStartup = runOnStartup;

        // Initialize run on startup enabled button icons.
        runOnStartupEnabledIdleIcon = new FlatSVGIcon(getClass().getResource(runOnStartupEnabledIdleIconPath))
                .derive(0.75f);
        runOnStartupEnabledHoverIcon = new FlatSVGIcon(getClass().getResource(runOnStartupEnabledHoverIconPath))
                .derive(0.75f);
        runOnStartupEnabledPressedIcon = new FlatSVGIcon(getClass().getResource(runOnStartupEnabledHoverIconPath))
                .derive(0.60f);

        // Initialize run on startup disabled button icons.
        runOnStartupDisabledIdleIcon = new FlatSVGIcon(getClass().getResource(runOnStartupDisabledIdleIconPath))
                .derive(0.75f);
        runOnStartupDisabledHoverIcon = new FlatSVGIcon(getClass().getResource(runOnStartupDisabledHoverIconPath))
                .derive(0.75f);
        runOnStartupDisabledPressedIcon = new FlatSVGIcon(getClass().getResource(runOnStartupDisabledHoverIconPath))
                .derive(0.60f);

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

        // Only show the icon for the run on startup button.
        this.setBorderPainted(false);
        this.setContentAreaFilled(false);
        this.setFocusPainted(false);
    }

    /**
     * Toggle the "run on startup" state.
     */
    public void toggleRunOnStartup() {
        runOnStartup = !runOnStartup;
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Getters and Setters
    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Getter for the "run on startup" state.
     * 
     * @return The "run on startup" state.
     */
    public boolean isRunOnStartup() {
        return runOnStartup;
    }

    /**
     * Setter for the "run on startup" state.
     * 
     * @param runOnStartup - Whether or not the application should run on startup.
     */
    public void setRunOnStartup(boolean runOnStartup) {
        this.runOnStartup = runOnStartup;
    }

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
     * Getter for the run on startup enabled button icon when the cursor is over the button or the button is in focus.
     * 
     * @return The run on startup enabled button hover icon.
     */
    public Icon getRunOnStartupEnabledHoverIcon() {
        return runOnStartupEnabledHoverIcon;
    }

    /**
     * Getter for the run on startup disabled button icon when the cursor is over the button or the button is in focus.
     * 
     * @return The run on startup disabled button hover icon.
     */
    public Icon getRunOnStartupDisabledHoverIcon() {
        return runOnStartupDisabledHoverIcon;
    }

    /**
     * Getter for the run on startup enabled button icon when the button is held down.
     * 
     * @return The pressed run on startup enabled button icon.
     */
    public Icon getRunOnStartupEnabledPressedIcon() {
        return runOnStartupEnabledPressedIcon;
    }

    /**
     * Getter for the run on startup disabled button icon when the button is held down.
     * 
     * @return The pressed run on startup disabled button icon.
     */
    public Icon getRunOnStartupDisabledPressedIcon() {
        return runOnStartupDisabledPressedIcon;
    }
}
