/*
 * Display Hot Keys
 * Copyright © 2024-2026 Jonathan R. Miller
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of version 3 of the GNU
 * General Public License as published by the Free Software Foundation, together with the additional terms under section
 * 7 of that License in ADDITIONAL_TERMS.txt.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License and the additional terms along with this program in
 * LICENSE.txt and ADDITIONAL_TERMS.txt. If not, see <https://www.gnu.org/licenses/>.
 */
package com.dhk.controller.button;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JButton;

import com.dhk.controller.IController;
import com.dhk.view.IView;

/**
 * Controls a Donate button, which opens the donation page in the user's default web browser. The button carries no
 * provider branding and the page is one link, so switching donation providers only means replacing that link.
 *
 * @author Jonathan R. Miller
 */
public class DonateButtonController extends AbstractButtonController implements IController {

    private IView view;
    private JButton donateButton;

    /**
     * Donation page the Donate button opens, currently a PayPal page. Replacing this link switches donation providers.
     */
    private static final String DONATION_LINK = "https://www.paypal.com/donate/?business=A6U7KG5BDZTRE"
            + "&no_recurring=0&item_name=I+appreciate+you+visiting+this+page%21+Thank+you%21&currency_code=USD";

    /**
     * Constructor for the {@link DonateButtonController} class.
     *
     * @param view
     *            - The view that holds the Donate button
     * @param donateButton
     *            - The Donate button to control
     */
    public DonateButtonController(IView view, JButton donateButton) {
        this.view = view;
        this.donateButton = donateButton;
    }

    @Override
    public void initController() {
    }

    @Override
    public void initListeners() {
        donateButton.addActionListener(e -> donateButtonAction());
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Opens the donation page in the user's default web browser.
     */
    private void donateButtonAction() {
        view.getDefaultFocusComponent().requestFocusInWindow();

        try {
            Desktop.getDesktop().browse(new URI(DONATION_LINK));
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

}
