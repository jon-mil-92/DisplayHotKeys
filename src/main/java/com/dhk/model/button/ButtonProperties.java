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
