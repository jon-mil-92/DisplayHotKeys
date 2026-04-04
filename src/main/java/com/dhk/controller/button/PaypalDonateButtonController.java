package com.dhk.controller.button;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import com.dhk.controller.IController;
import com.dhk.model.button.ThemeableButton;
import com.dhk.view.IView;

/**
 * Controls the PayPal Donate button. Listeners are added to the corresponding view component so that when the PayPal
 * Donate button is pressed, the paypal donation page is opened up in the user's default web browser.
 * 
 * @author Jonathan R. Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2026 Jonathan R. Miller
 */
public class PaypalDonateButtonController extends AbstractButtonController implements IController {

    private IView view;
    private ThemeableButton paypalDonateButton;

    private static final String LINK_DOMAIN = "https://www.paypal.com";
    private static final String LINK_PATH = "/donate/";
    private static final String LINK_ID = "?business=A6U7KG5BDZTRE";
    private static final String LINK_RECURRING = "&no_recurring=0";
    private static final String LINK_MESSAGE = "&item_name=I+appreciate+you+visiting+this+page%21+Thank+you%21";
    private static final String LINK_CURRENCY = "&currency_code=USD";
    private static final String PAYPAL_DONATE_LINK = LINK_DOMAIN + LINK_PATH + LINK_ID + LINK_RECURRING + LINK_MESSAGE
            + LINK_CURRENCY;

    /**
     * Constructor for the {@link PaypalDonateButtonController} class.
     *
     * @param view
     *            - The view that holds the PayPal donate button
     * @param paypalDonateButton
     *            - The PayPal donate button to control
     */
    public PaypalDonateButtonController(IView view, ThemeableButton paypalDonateButton) {
        this.view = view;
        this.paypalDonateButton = paypalDonateButton;
    }

    @Override
    public void initController() {
    }

    @Override
    public void initListeners() {
        paypalDonateButton.addActionListener(e -> paypalDonateButtonAction());

        initStateChangeListeners(paypalDonateButton, view.getDefaultFocusComponent());
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
