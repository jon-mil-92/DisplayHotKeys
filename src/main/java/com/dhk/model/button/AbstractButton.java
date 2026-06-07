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
package com.dhk.model.button;

import javax.swing.JButton;

import com.formdev.flatlaf.extras.FlatSVGIcon;

/**
 * Defines an abstract button that implements the icon change action and defines the update methods for a button.
 *
 * @author Jonathan R. Miller
 */
public abstract class AbstractButton extends JButton {

    private static final long serialVersionUID = -8040714399831618945L;

    private ButtonProperties buttonProperties;

    /**
     * Default constructor for the {@link AbstractButton} class
     */
    public AbstractButton() {
    }

    /**
     * Gets an SVG icon for the resource at the given path with the given image scale percentage.
     *
     * @param path
     *            - The path to the icon resource
     * @param scale
     *            - The image scale percentage
     *
     * @return The SVG icon for the resource at the given path with the given image scale percentage
     */
    protected FlatSVGIcon getSvgIcon(String path, float scale) {
        return new FlatSVGIcon(getClass().getResource(path)).derive(scale);
    }

    /**
     * Changes the icon based on the state of the button.
     */
    protected void iconChangeAction() {
        if (getModel().isArmed()) {
            updateHeldIcon();
        } else if (getModel().isRollover()) {
            updateHoverIcon();
        } else {
            updateIdleIcon();
        }
    }

    /**
     * Updates the icon when the button is idle.
     */
    public abstract void updateIdleIcon();

    /**
     * Updates the icon when the cursor is hovering over the button.
     */
    public abstract void updateHoverIcon();

    /**
     * Updates the icon when the button is held down.
     */
    public abstract void updateHeldIcon();

    /**
     * Gets the button properties.
     *
     * @return The button properties
     */
    public ButtonProperties getButtonProperties() {
        return buttonProperties;
    }

    /**
     * Sets the button properties.
     *
     * @param buttonProperties
     *            - The button properties to set
     */
    public void setButtonProperties(ButtonProperties buttonProperties) {
        this.buttonProperties = buttonProperties;
    }

}
