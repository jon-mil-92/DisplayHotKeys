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

import javax.swing.Icon;
import javax.swing.event.ChangeListener;

/**
 * Defines a themeable toggle button with a light and dark hover and held icon.
 *
 * @author Jonathan R. Miller
 */
public class ThemeableToggleButton extends ThemeableButton {

    private static final long serialVersionUID = -6579393126960989482L;

    private Icon onIdleIcon;
    private Icon offIdleIcon;
    private Icon onHoverIcon;
    private Icon offHoverIcon;
    private Icon onHeldIcon;
    private Icon offHeldIcon;
    private Icon onDarkHoverIcon;
    private Icon offDarkHoverIcon;
    private Icon onDarkHeldIcon;
    private Icon offDarkHeldIcon;
    private boolean on;

    /**
     * Constructor for the {@link ThemeableToggleButton} button class.
     *
     * @param onIdleIconPath
     *            - The resource path for the idle icon in the on state
     * @param offIdleIconPath
     *            - The resource path for the idle icon in the off state
     * @param onHoverIconPath
     *            - The resource path for the hover icon in the on state
     * @param offHoverIconPath
     *            - The resource path for the hover icon in the off state
     * @param onDarkHoverIconPath
     *            - The resource path for the dark mode hover icon in the on state
     * @param offDarkHoverIconPath
     *            - The resource path for the dark mode hover icon in the off state
     * @param properties
     *            - The properties of the button
     * @param enabled
     *            - The initial enabled state of the button
     * @param darkMode
     *            - The initial dark mode state of the button
     * @param on
     *            - The initial on state of the button
     */
    public ThemeableToggleButton(String onIdleIconPath, String offIdleIconPath, String onHoverIconPath,
            String offHoverIconPath, String onDarkHoverIconPath, String offDarkHoverIconPath,
            ButtonProperties properties, boolean enabled, boolean darkMode, boolean on) {
        super(onIdleIconPath, onHoverIconPath, onIdleIconPath, onDarkHoverIconPath, properties, enabled, darkMode);
        setButtonProperties(properties);

        this.onIdleIcon = getSvgIcon(onIdleIconPath, properties.getIdleScale());
        this.offIdleIcon = getSvgIcon(offIdleIconPath, properties.getIdleScale());

        this.onHoverIcon = getSvgIcon(onHoverIconPath, properties.getIdleScale());
        this.offHoverIcon = getSvgIcon(offHoverIconPath, properties.getIdleScale());

        this.onHeldIcon = getSvgIcon(onHoverIconPath, properties.getHeldScale());
        this.offHeldIcon = getSvgIcon(offHoverIconPath, properties.getHeldScale());

        this.onDarkHoverIcon = getSvgIcon(onDarkHoverIconPath, properties.getIdleScale());
        this.offDarkHoverIcon = getSvgIcon(offDarkHoverIconPath, properties.getIdleScale());

        this.onDarkHeldIcon = getSvgIcon(onDarkHoverIconPath, properties.getHeldScale());
        this.offDarkHeldIcon = getSvgIcon(offDarkHoverIconPath, properties.getHeldScale());

        this.on = on;

        updateIdleIcon();

        // Remove the change listeners from the parent button
        for (ChangeListener changeListener : super.getChangeListeners()) {
            super.removeChangeListener(changeListener);
        }

        addChangeListener(_ -> super.iconChangeAction());
    }

    /**
     * Sets the on state of the button.
     *
     * @param on
     *            - The on state of the button.
     */
    public void setOn(boolean on) {
        this.on = on;
    }

    @Override
    public void updateIdleIcon() {
        // Currently only one theme for the idle button state
        if (on) {
            setIdleIcon(onIdleIcon);
            setDarkIdleIcon(onIdleIcon);
        } else {
            setIdleIcon(offIdleIcon);
            setDarkIdleIcon(offIdleIcon);
        }

        super.updateIdleIcon();
    }

    @Override
    public void updateHoverIcon() {
        if (on) {
            setHoverIcon(onHoverIcon);
            setDarkHoverIcon(onDarkHoverIcon);
        } else {
            setHoverIcon(offHoverIcon);
            setDarkHoverIcon(offDarkHoverIcon);
        }

        super.updateHoverIcon();
    }

    @Override
    public void updateHeldIcon() {
        if (on) {
            setHoverIcon(onHeldIcon);
            setDarkHoverIcon(onDarkHeldIcon);
        } else {
            setHoverIcon(offHeldIcon);
            setDarkHoverIcon(offDarkHeldIcon);
        }

        super.updateHoverIcon();
    }

}
