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

import java.awt.Dimension;

/**
 * Properties for a custom button.
 *
 * @author Jonathan R. Miller
 */
public class ButtonProperties {

    private String tooltip;
    private Dimension size;
    private float idleScale;
    private float heldScale;

    /**
     * Default constructor for the {@link ButtonProperties} class.
     */
    public ButtonProperties() {
    }

    /**
     * Constructor for the {@link ButtonProperties} class.
     *
     * @param tooltip
     *            - The tooltip for the button (not required)
     * @param size
     *            - The size of the button
     * @param idleScale
     *            - The idle scale of the button
     * @param heldScale
     *            - The held scale of the button
     */
    public ButtonProperties(String tooltip, Dimension size, float idleScale, float heldScale) {
        this.tooltip = tooltip;
        this.size = size;
        this.idleScale = idleScale;
        this.heldScale = heldScale;
    }

    /**
     * Gets the tooltip property.
     *
     * @return The tooltip property
     */
    public String getTooltip() {
        return tooltip;
    }

    /**
     * Sets the tooltip property.
     *
     * @param tooltip
     *            - The tooltip to set
     */
    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }

    /**
     * Gets the size property.
     *
     * @return The size property
     */
    public Dimension getSize() {
        return size;
    }

    /**
     * Sets the size property.
     *
     * @param size
     *            - The size to set
     */
    public void setSize(Dimension size) {
        this.size = size;
    }

    /**
     * Gets the idle scale property.
     *
     * @return The idle scale property
     */
    public float getIdleScale() {
        return idleScale;
    }

    /**
     * Sets the idle scale property.
     *
     * @param idleScale
     *            - The idle scale to set
     */
    public void setIdleScale(float idleScale) {
        this.idleScale = idleScale;
    }

    /**
     * Gets the held scale property.
     *
     * @return The held scale property
     */
    public float getHeldScale() {
        return heldScale;
    }

    /**
     * Sets the held scale property.
     *
     * @param heldScale
     *            - The held scale to set
     */
    public void setHeldScale(float heldScale) {
        this.heldScale = heldScale;
    }

}
