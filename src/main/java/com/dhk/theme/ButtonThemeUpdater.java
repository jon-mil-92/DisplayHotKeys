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
package com.dhk.theme;

import com.dhk.model.DhkModel;
import com.dhk.model.button.ThemeableButton;
import com.dhk.view.DhkView;

/**
 * Sets the theme for all of the themeable buttons in the view. It aids in setting the theme of the application.
 *
 * @author Jonathan R. Miller
 */
public class ButtonThemeUpdater {

    private DhkModel model;
    private DhkView view;

    /**
     * Constructor for the {@link ButtonThemeUpdater} class.
     *
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     */
    public ButtonThemeUpdater(DhkModel model, DhkView view) {
        this.model = model;
        this.view = view;
    }

    /**
     * Applies the current theme to all of the themeable buttons in the view.
     */
    public void updateButtonThemes() {
        for (ThemeableButton button : view.getThemeableButtons()) {
            button.setDarkMode(model.isDarkMode());
            button.updateIdleIcon();
        }
    }

}
