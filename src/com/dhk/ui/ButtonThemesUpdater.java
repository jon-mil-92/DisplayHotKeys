package com.dhk.ui;

/**
 * This class sets the theme for all of the themeable buttons in the view. It aids in setting the theme of the
 * application.
 * 
 * @author Jonathan Miller
 * @version 1.3.1
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class ButtonThemesUpdater {
    private DhkView view;

    /**
     * Constructor for the ButtonThemesUpdater class.
     * 
     * @param view - The view for the application.
     */
    public ButtonThemesUpdater(DhkView view) {
        // Get the application's view.
        this.view = view;
    }

    /**
     * This method applies the current theme to all of the idle themeable buttons in the view.
     */
    public void updateIdleButtonThemes() {
        // If the UI is in dark mode...
        if (view.isDarkMode()) {
            // Change the paypal donate button to the dark idle icon.
            view.getPaypalDonateButton().setIcon(view.getPaypalDonateButton().getPaypalDonateDarkIdleIcon());
        }
        // Otherwise, if the UI is in light mode...
        else {
            // Change the paypal donate button to the light idle icon.
            view.getPaypalDonateButton().setIcon(view.getPaypalDonateButton().getPaypalDonateLightIdleIcon());
        }
    }
}
