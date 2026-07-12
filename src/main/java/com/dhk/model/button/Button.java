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

import java.awt.Insets;

import javax.swing.Icon;

/**
 * Defines a button with an idle, hover, and held icon.
 *
 * @author Jonathan R. Miller
 */
public class Button extends AbstractButton {

    private static final long serialVersionUID = 7244979033277293460L;

    private Icon idleIcon;
    private Icon hoverIcon;
    private Icon heldIcon;

    /**
     * Constructor for the {@link Button} class.
     *
     * @param idleIconPath
     *            - The resource path for the idle icon
     * @param hoverIconPath
     *            - The resource path for the hover icon
     * @param properties
     *            - The properties of the button
     * @param enabled
     *            - The initial enabled state of the button
     */
    public Button(String idleIconPath, String hoverIconPath, ButtonProperties properties, boolean enabled) {
        setButtonProperties(properties);
        this.idleIcon = getSvgIcon(idleIconPath, properties.getIdleScale());
        this.hoverIcon = getSvgIcon(hoverIconPath, properties.getIdleScale());
        this.heldIcon = getSvgIcon(hoverIconPath, properties.getHeldScale());

        updateIdleIcon();
        getInputMap().clear();
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setMargin(new Insets(0, 0, 0, 0));
        setToolTipText(properties.getTooltip());
        setPreferredSize(properties.getSize());
        setEnabled(enabled);

        addChangeListener(e -> super.iconChangeAction());
    }

    /**
     * Gets the icon when the button is idle.
     *
     * @return The idle icon
     */
    public Icon getIdleIcon() {
        return idleIcon;
    }

    /**
     * Gets the icon when the cursor is hovering over the button.
     *
     * @return The hover icon
     */
    public Icon getHoverIcon() {
        return hoverIcon;
    }

    /**
     * Gets the icon when the button is held down.
     *
     * @return The held down icon
     */
    public Icon getHeldIcon() {
        return heldIcon;
    }

    /**
     * Sets the icon when the button is idle.
     *
     * @param idleIcon
     *            - The icon when the button is idle
     */
    public void setIdleIcon(Icon idleIcon) {
        this.idleIcon = idleIcon;
    }

    /**
     * Sets the icon when the cursor is hovering over the button.
     *
     * @param hoverIcon
     *            - The icon when the cursor is hovering over the button
     */
    public void setHoverIcon(Icon hoverIcon) {
        this.hoverIcon = hoverIcon;
    }

    /**
     * Sets the icon when the button is held down.
     *
     * @param heldIcon
     *            - The icon when the button is held down.
     */
    public void setHeldIcon(Icon heldIcon) {
        this.heldIcon = heldIcon;
    }

    @Override
    public void updateIdleIcon() {
        setIcon(idleIcon);
    }

    @Override
    public void updateHoverIcon() {
        setIcon(hoverIcon);
    }

    @Override
    public void updateHeldIcon() {
        setIcon(heldIcon);
    }

}
