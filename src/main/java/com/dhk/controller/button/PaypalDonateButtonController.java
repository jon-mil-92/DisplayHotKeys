/*
 * The MIT License (MIT)
 *
 * Copyright © 2026 Jonathan R. Miller
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the “Software”), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
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
 */
public class PaypalDonateButtonController extends AbstractButtonController implements IController {

    private IView view;
    private ThemeableButton paypalDonateButton;

    /**
     * Domain of the PayPal donation link.
     */
    private static final String LINK_DOMAIN = "https://www.paypal.com";

    /**
     * Path of the PayPal donation page.
     */
    private static final String LINK_PATH = "/donate/";

    /**
     * Query parameter identifying the PayPal account that receives donations.
     */
    private static final String LINK_ID = "?business=A6U7KG5BDZTRE";

    /**
     * Query parameter that lets the donor choose a recurring donation.
     */
    private static final String LINK_RECURRING = "&no_recurring=0";

    /**
     * Query parameter holding the thank-you message shown on the donation page.
     */
    private static final String LINK_MESSAGE = "&item_name=I+appreciate+you+visiting+this+page%21+Thank+you%21";

    /**
     * Query parameter setting the donation currency to US dollars.
     */
    private static final String LINK_CURRENCY = "&currency_code=USD";

    /**
     * Full PayPal donation link opened by the donate button.
     */
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
