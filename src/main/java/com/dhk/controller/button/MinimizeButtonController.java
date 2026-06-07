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

import java.awt.Frame;

import com.dhk.controller.IController;
import com.dhk.view.DhkView;

/**
 * Controls the Minimize button. Listeners are added to the corresponding view component so that when the Minimize
 * button is pressed, the application is minimized to the system tray.
 *
 * @author Jonathan R. Miller
 */
public class MinimizeButtonController extends AbstractButtonController implements IController {

    private DhkView view;

    /**
     * Constructor for the {@link MinimizeButtonController} class.
     *
     * @param view
     *            - The view for the application
     */
    public MinimizeButtonController(DhkView view) {
        this.view = view;
    }

    @Override
    public void initController() {
    }

    @Override
    public void initListeners() {
        view.getMinimizeButton().addActionListener(_ -> minimizeButtonAction());

        initStateChangeListeners(view.getMinimizeButton(), view.getDefaultFocusComponent());
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Minimizes the frame and put the focus on the first element in the frame.
     */
    private void minimizeButtonAction() {
        // Minimize the application
        view.getFrame().setExtendedState(Frame.ICONIFIED);
        view.getDefaultFocusComponent().requestFocusInWindow();

        // Try to free up resources upon minimizing to the system tray to utilize minimal memory while minimized
        System.gc();
    }

}
