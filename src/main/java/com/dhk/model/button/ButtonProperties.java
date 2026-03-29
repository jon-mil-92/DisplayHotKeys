package com.dhk.model.button;

import java.awt.Dimension;

/**
 * Properties for a custom button.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2026 Jonathan Miller
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
