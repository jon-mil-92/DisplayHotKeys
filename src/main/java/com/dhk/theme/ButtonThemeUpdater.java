package com.dhk.theme;

import com.dhk.model.DhkModel;
import com.dhk.ui.DhkView;

/**
 * This class sets the theme for all of the themeable buttons in the view. It aids in setting the theme of the
 * application.
 * 
 * @author Jonathan Miller
 * @version 1.5.1
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
 */
public class ButtonThemeUpdater {
    private DhkModel model;
    private DhkView view;

    /**
     * Constructor for the ButtonThemesUpdater class.
     * 
     * @param model - The model for the application.
     * @param view  - The view for the application.
     */
    public ButtonThemeUpdater(DhkModel model, DhkView view) {
        // Get the application's model and view.
        this.model = model;
        this.view = view;
    }

    /**
     * This method applies the current theme to all of the idle themeable buttons in the view.
     */
    public void updateIdleButtonThemes() {
        // If the UI is in dark mode...
        if (model.isDarkMode()) {
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
