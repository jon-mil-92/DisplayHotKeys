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

import com.dhk.controller.IController;
import com.dhk.view.DhkView;

/**
 * Controls the Exit button. Listeners are added to the corresponding view component so that when a user hovers over or
 * clicks the Exit button, its icon changes, and when a user clicks on the button, the application is stopped.
 *
 * @author Jonathan R. Miller
 */
public class ExitButtonController extends AbstractButtonController implements IController {

    private DhkView view;

    /**
     * Constructor for the {@link ExitButtonController} class.
     *
     * @param view
     *            - The view for the application
     */
    public ExitButtonController(DhkView view) {
        this.view = view;
    }

    @Override
    public void initController() {
    }

    @Override
    public void initListeners() {
        view.getExitButton().addActionListener(_ -> exitButtonAction());

        initStateChangeListeners(view.getExitButton(), view.getDefaultFocusComponent());
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Exits the application.
     */
    private void exitButtonAction() {
        System.exit(0);
    }

}
