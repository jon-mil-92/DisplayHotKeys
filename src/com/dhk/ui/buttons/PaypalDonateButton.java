package com.dhk.ui.buttons;

import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.Icon;
import javax.swing.JButton;
import com.formdev.flatlaf.extras.FlatSVGIcon;

/**
 * This class defines the Paypal Donate button. The icons for the different states of the button are defined here.
 * 
 * @author Jonathan Miller
 * @version 1.3.2
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class PaypalDonateButton extends JButton {
    private static final long serialVersionUID = 1L;
    private Icon paypalDonateDarkIdleIcon;
    private Icon paypalDonateDarkHoverIcon;
    private Icon paypalDonateDarkPressedIcon;
    private Icon paypalDonateLightIdleIcon;
    private Icon paypalDonateLightHoverIcon;
    private Icon paypalDonateLightPressedIcon;

    // Set a fixed size for the button icon.
    private final Dimension BUTTON_ICON_SIZE = new Dimension(134, 46);

    /**
     * Constructor for the PaypalDonateButton class.
     * 
     * @param darkMode                       - The initial dark mode state of the button.
     * @param paypalDonateDarkIdleIconPath   - The resource path for the paypal donate button dark mode idle icon.
     * @param paypalDonateDarkHoverIconPath  - The resource path for the paypal donate button dark mode hover icon.
     * @param paypalDonateLightIdleIconPath  - The resource path for the paypal donate button light mode idle icon.
     * @param paypalDonateLightHoverIconPath - The resource path for the paypal donate button light mode hover icon.
     */
    public PaypalDonateButton(boolean darkMode, String paypalDonateDarkIdleIconPath,
            String paypalDonateDarkHoverIconPath, String paypalDonateLightIdleIconPath,
            String paypalDonateLightHoverIconPath) {
        // Initialize paypal donate dark mode button icons.
        paypalDonateDarkIdleIcon = new FlatSVGIcon(getClass().getResource(paypalDonateDarkIdleIconPath)).derive(0.70f);
        paypalDonateDarkHoverIcon = new FlatSVGIcon(getClass().getResource(paypalDonateDarkHoverIconPath))
                .derive(0.70f);
        paypalDonateDarkPressedIcon = new FlatSVGIcon(getClass().getResource(paypalDonateDarkHoverIconPath))
                .derive(0.63f);

        // Initialize paypal donate light mode button icons.
        paypalDonateLightIdleIcon = new FlatSVGIcon(getClass().getResource(paypalDonateLightIdleIconPath))
                .derive(0.70f);
        paypalDonateLightHoverIcon = new FlatSVGIcon(getClass().getResource(paypalDonateLightHoverIconPath))
                .derive(0.70f);
        paypalDonateLightPressedIcon = new FlatSVGIcon(getClass().getResource(paypalDonateLightHoverIconPath))
                .derive(0.63f);

        // If the UI is in dark mode...
        if (darkMode) {
            // Initialize the paypal donate button icon to the dark mode icon.
            this.setIcon(paypalDonateDarkIdleIcon);
        }
        // Otherwise, if the UI is in light mode...
        else {
            // Initialize the paypal donate button icon to the light mode icon.
            this.setIcon(paypalDonateLightIdleIcon);
        }

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
     * Getter for the paypal donate button icon when it is idle and the theme is in dark mode.
     * 
     * @return The idle paypal donate button icon when the theme is in dark mode.
     */
    public Icon getPaypalDonateDarkIdleIcon() {
        return paypalDonateDarkIdleIcon;
    }

    /**
     * Getter for the paypal donate button icon when the cursor is over the button or the button is in focus when the
     * theme is in dark mode.
     * 
     * @return The paypal donate button hover icon when the theme is in dark mode.
     */
    public Icon getPaypalDonateDarkHoverIcon() {
        return paypalDonateDarkHoverIcon;
    }

    /**
     * Getter for the paypal donate button icon when the button is held down when the theme is in dark mode.
     * 
     * @return The pressed paypal donate button icon when the theme is in dark mode.
     */
    public Icon getPaypalDonateDarkPressedIcon() {
        return paypalDonateDarkPressedIcon;
    }

    /**
     * Getter for the paypal donate button icon when it is idle and the theme is in light mode.
     * 
     * @return The idle paypal donate button icon when the theme is in light mode.
     */
    public Icon getPaypalDonateLightIdleIcon() {
        return paypalDonateLightIdleIcon;
    }

    /**
     * Getter for the paypal donate button icon when the cursor is over the button or the button is in focus when the
     * theme is in light mode.
     * 
     * @return The paypal donate button hover icon when the theme is in light mode.
     */
    public Icon getPaypalDonateLightHoverIcon() {
        return paypalDonateLightHoverIcon;
    }

    /**
     * Getter for the paypal donate button icon when the button is held down when the theme is in light mode.
     * 
     * @return The pressed paypal donate button icon when the theme is in light mode.
     */
    public Icon getPaypalDonateLightPressedIcon() {
        return paypalDonateLightPressedIcon;
    }
}
