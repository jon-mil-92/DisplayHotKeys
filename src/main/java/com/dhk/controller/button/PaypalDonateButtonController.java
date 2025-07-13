package com.dhk.controller.button;

import java.awt.Desktop;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import com.dhk.controller.Controller;
import com.dhk.model.DhkModel;
import com.dhk.ui.DhkView;

/**
 * This class controls the PayPal Donate button. Listeners are added to the corresponding view component so that when
 * the PayPal Donate button is pressed, the paypal donation page is opened up in the user's default web browser.
 * 
 * @author Jonathan Miller
 * @version 1.5.1
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
 */
public class PaypalDonateButtonController implements Controller {
    private DhkModel model;
    private DhkView view;

    // Define a string for the paypal donate web link.
    private final String LINK_DOMAIN = "https://www.paypal.com/donate/";
    private final String LINK_ID = "?business=A6U7KG5BDZTRE";
    private final String LINK_RECURRING = "&no_recurring=0";
    private final String LINK_MESSAGE = "&item_name=I+appreciate+you+visiting+this+page%21+Thank+you%21";
    private final String LINK_CURRENCY = "&currency_code=USD";
    private final String PAYPAL_DONATE_LINK = LINK_DOMAIN + LINK_ID + LINK_RECURRING + LINK_MESSAGE + LINK_CURRENCY;

    /**
     * Constructor for the PaypalDonateButtonController class.
     *
     * @param model - The model for the application.
     * @param view  - The view for the application.
     */
    public PaypalDonateButtonController(DhkModel model, DhkView view) {
        // Get the application's model and view.
        this.model = model;
        this.view = view;
    }

    @Override
    public void initController() {
    }

    /**
     * This method initializes the listeners for the paypal donate button.
     */
    @Override
    public void initListeners() {
        // Start the action listener for the paypal donate button action.
        view.getPaypalDonateButton().addActionListener(e -> paypalDonateButtonAction());

        // Set the state change listener for the paypal donate button.
        view.getPaypalDonateButton().addChangeListener(e -> paypalDonateButtonStateChangeAction());

        // Set the focus listener for the paypal donate button.
        view.getPaypalDonateButton().addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // Switch to the rollover state when the paypal donate button is focused.
                view.getPaypalDonateButton().getModel().setRollover(true);
            }

            @Override
            public void focusLost(FocusEvent e) {
                // Leave the rollover state when the paypal donate button is not focused.
                view.getPaypalDonateButton().getModel().setRollover(false);
            }
        });

        // Set the mouse listener for the paypal donate button.
        view.getPaypalDonateButton().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // Set the focus on the paypal donate button when the mouse hovers over it.
                view.getPaypalDonateButton().requestFocusInWindow();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Set the focus on the display IDs label when the mouse leaves the button.
                view.getDisplayIdsLabel().requestFocusInWindow();
            }
        });
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Open up the paypal donation page in the user's default web browser.
     */
    private void paypalDonateButtonAction() {
        try {
            Desktop.getDesktop().browse(new URI(PAYPAL_DONATE_LINK));
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method changes the paypal donate button icon depending on the button's state.
     */
    private void paypalDonateButtonStateChangeAction() {
        // If the user is holding the action button on the paypal donate button...
        if (view.getPaypalDonateButton().getModel().isArmed()) {
            // Use the corresponding pressed icon.
            setPressedIcon();
        }
        // If the user is hovering on the paypal donate button...
        else if (view.getPaypalDonateButton().getModel().isRollover()) {
            // Use the corresponding hover icon.
            setHoverIcon();
        }
        // Otherwise, if the user is not interacting with the paypal donate button...
        else {
            // Use the corresponding idle icon.
            setIdleIcon();
        }
    }

    /**
     * This method sets the pressed icon corresponding to the "dark mode" state.
     */
    private void setPressedIcon() {
        // If the UI is in dark mode...
        if (model.isDarkMode()) {
            // Use the pressed icon for dark mode.
            view.getPaypalDonateButton().setIcon(view.getPaypalDonateButton().getPaypalDonateDarkPressedIcon());
        } else {
            // Use the pressed icon for light mode.
            view.getPaypalDonateButton().setIcon(view.getPaypalDonateButton().getPaypalDonateLightPressedIcon());
        }
    }

    /**
     * This method sets the hover icon corresponding to the "dark mode" state.
     */
    private void setHoverIcon() {
        // If the UI is in dark mode...
        if (model.isDarkMode()) {
            // Use the hover icon for dark mode.
            view.getPaypalDonateButton().setIcon(view.getPaypalDonateButton().getPaypalDonateDarkHoverIcon());
        } else {
            // Use the hover icon for light mode.
            view.getPaypalDonateButton().setIcon(view.getPaypalDonateButton().getPaypalDonateLightHoverIcon());
        }
    }

    /**
     * This method sets the idle icon corresponding to the "dark mode" state.
     */
    private void setIdleIcon() {
        // If the UI is in dark mode...
        if (model.isDarkMode()) {
            // Use the idle icon for dark mode.
            view.getPaypalDonateButton().setIcon(view.getPaypalDonateButton().getPaypalDonateDarkIdleIcon());
        } else {
            // Use the idle icon for light mode.
            view.getPaypalDonateButton().setIcon(view.getPaypalDonateButton().getPaypalDonateLightIdleIcon());
        }
    }
}
