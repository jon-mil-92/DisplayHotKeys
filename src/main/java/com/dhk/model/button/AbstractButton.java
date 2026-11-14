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
package com.dhk.model.button;

import javax.swing.JButton;

import com.dhk.utility.LightweightTooltip;
import com.formdev.flatlaf.extras.FlatSVGIcon;

/**
 * Defines an abstract button that implements the icon change action and defines the update methods for a button. Its
 * tooltip is a lightweight tooltip drawn in the window's layered pane, which avoids heavyweight popup windows.
 *
 * @author Jonathan R. Miller
 */
public abstract class AbstractButton extends JButton {

    private ButtonProperties buttonProperties;
    private transient LightweightTooltip lightweightTooltip;

    /**
     * Serialization version of the {@link AbstractButton} class.
     */
    private static final long serialVersionUID = -8040714399831618945L;

    /**
     * Default constructor for the {@link AbstractButton} class.
     */
    public AbstractButton() {
        super();
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

    /**
     * Shows the given text in a lightweight tooltip drawn in the window's layered pane instead of the default Swing
     * tooltip, which would open a heavyweight popup window that can persist as a ghost.
     *
     * @param text
     *            - The tooltip text to show for this button, or null to remove the tooltip
     */
    @Override
    public void setToolTipText(String text) {
        super.setToolTipText(null);

        // Created on first use, since a superclass constructor can call this before the fields are initialized
        if (lightweightTooltip == null) {
            lightweightTooltip = new LightweightTooltip(this);
        }

        lightweightTooltip.setText(text);
    }

}
