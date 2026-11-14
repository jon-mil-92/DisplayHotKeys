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

import javax.swing.Icon;
import javax.swing.event.ChangeListener;

/**
 * Defines a themeable button with a light and dark idle, hover, and held icon.
 *
 * @author Jonathan R. Miller
 */
public class ThemeableButton extends Button {

    private Icon darkIdleIcon;
    private Icon darkHoverIcon;
    private Icon darkHeldIcon;
    private boolean darkMode;

    /**
     * Serialization version of the {@link ThemeableButton} class.
     */
    private static final long serialVersionUID = 4378247830510346232L;

    /**
     * Constructor for the {@link ThemeableButton} class.
     *
     * @param idleIconPath
     *            - The resource path for the idle icon
     * @param hoverIconPath
     *            - The resource path for the hover icon
     * @param darkIdleIconPath
     *            - The resource path for the dark mode idle icon
     * @param darkHoverIconPath
     *            - The resource path for the dark mode hover icon
     * @param properties
     *            - The properties of the button
     * @param enabled
     *            - The initial enabled state of the button
     * @param darkMode
     *            - The initial dark mode state of the button
     */
    public ThemeableButton(String idleIconPath, String hoverIconPath, String darkIdleIconPath, String darkHoverIconPath,
            ButtonProperties properties, boolean enabled, boolean darkMode) {
        super(idleIconPath, hoverIconPath, properties, enabled);
        setButtonProperties(properties);

        this.darkIdleIcon = getSvgIcon(darkIdleIconPath, properties.getIdleScale());
        this.darkHoverIcon = getSvgIcon(darkHoverIconPath, properties.getIdleScale());
        this.darkHeldIcon = getSvgIcon(darkHoverIconPath, properties.getHeldScale());
        this.darkMode = darkMode;

        updateIdleIcon();

        // Remove the change listeners from the parent button
        for (ChangeListener changeListener : super.getChangeListeners()) {
            super.removeChangeListener(changeListener);
        }

        addChangeListener(e -> super.iconChangeAction());
    }

    /**
     * Sets the dark mode icon when the button is idle.
     *
     * @param darkIdleIcon
     *            - The dark mode idle icon
     */
    public void setDarkIdleIcon(Icon darkIdleIcon) {
        this.darkIdleIcon = darkIdleIcon;
    }

    /**
     * Sets the dark mode icon when the cursor is hovering over the button.
     *
     * @param darkHoverIcon
     *            - The dark mode hover icon
     */
    public void setDarkHoverIcon(Icon darkHoverIcon) {
        this.darkHoverIcon = darkHoverIcon;
    }

    /**
     * Sets the dark mode icon when the button is held down.
     *
     * @param darkHeldIcon
     *            - The dark mode held down icon
     */
    public void setDarkHeldIcon(Icon darkHeldIcon) {
        this.darkHeldIcon = darkHeldIcon;
    }

    /**
     * Sets the dark mode state of the button.
     *
     * @param darkMode
     *            - The dark mode state of the button
     */
    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }

    @Override
    public void updateIdleIcon() {
        if (darkMode) {
            setIcon(darkIdleIcon);
        } else {
            setIcon(getIdleIcon());
        }
    }

    @Override
    public void updateHoverIcon() {
        if (darkMode) {
            setIcon(darkHoverIcon);
        } else {
            setIcon(getHoverIcon());
        }
    }

    @Override
    public void updateHeldIcon() {
        if (darkMode) {
            setIcon(darkHeldIcon);
        } else {
            setIcon(getHeldIcon());
        }
    }

}
