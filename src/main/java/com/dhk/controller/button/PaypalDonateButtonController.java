package com.dhk.controller.button;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import com.dhk.controller.IController;
import com.dhk.view.DhkView;

/**
 * Controls the PayPal Donate button. Listeners are added to the corresponding view component so that when the PayPal
 * Donate button is pressed, the paypal donation page is opened up in the user's default web browser.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class PaypalDonateButtonController extends AbstractButtonController implements IController {

    private DhkView view;

    private final String LINK_DOMAIN = "https://www.paypal.com/donate/";
    private final String LINK_ID = "?business=A6U7KG5BDZTRE";
    private final String LINK_RECURRING = "&no_recurring=0";
    private final String LINK_MESSAGE = "&item_name=I+appreciate+you+visiting+this+page%21+Thank+you%21";
    private final String LINK_CURRENCY = "&currency_code=USD";
    private final String PAYPAL_DONATE_LINK = LINK_DOMAIN + LINK_ID + LINK_RECURRING + LINK_MESSAGE + LINK_CURRENCY;

    /**
     * Constructor for the PaypalDonateButtonController class.
     *
     * @param view
     *            - The view for the application
     */
    public PaypalDonateButtonController(DhkView view) {
        this.view = view;
    }

    @Override
    public void initController() {
    }

    /**
     * Initializes the listeners for the paypal donate button.
     */
    @Override
    public void initListeners() {
        view.getPaypalDonateButton().addActionListener(e -> paypalDonateButtonAction());

        initStateChangeListeners(view.getPaypalDonateButton(), view.getSelectedDisplayLabel());
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Opens up the paypal donation page in the user's default web browser.
     */
    private void paypalDonateButtonAction() {
        try {
            Desktop.getDesktop().browse(new URI(PAYPAL_DONATE_LINK));
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

}
