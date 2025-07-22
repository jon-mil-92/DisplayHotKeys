package com.dhk.model.button;

import java.awt.Dimension;
import javax.swing.Icon;
import javax.swing.event.ChangeListener;

/**
 * Defines a themeable button with a light and dark idle, hover, and held icon.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class ThemeableButton extends Button {

    private static final long serialVersionUID = 4378247830510346232L;

    private Icon darkIdleIcon;
    private Icon darkHoverIcon;
    private Icon darkHeldIcon;
    private boolean darkMode;

    /**
     * Constructor for the ThemeableButton class.
     * 
     * @param idleIconPath
     *            - The resource path for the idle icon
     * @param hoverIconPath
     *            - The resource path for the hover icon
     * @param darkIdleIconPath
     *            - The resource path for the dark mode idle icon
     * @param darkHoverIconPath
     *            - The resource path for the dark mode hover icon
     * @param tooltip
     *            - The text for the button tooltip
     * @param size
     *            - The size of the button
     * @param idleScale
     *            - The image scale percentage when the button is idle
     * @param heldScale
     *            - The image scale percentage when the button is held down
     * @param enabled
     *            - The initial enabled state of the button
     * @param darkMode
     *            - The initial dark mode state of the button
     */
    public ThemeableButton(String idleIconPath, String hoverIconPath, String darkIdleIconPath, String darkHoverIconPath,
            String tooltip, Dimension size, float idleScale, float heldScale, boolean enabled, boolean darkMode) {
        super(idleIconPath, hoverIconPath, tooltip, size, idleScale, heldScale, enabled);

        this.darkIdleIcon = getSvgIcon(darkIdleIconPath, idleScale);
        this.darkHoverIcon = getSvgIcon(darkHoverIconPath, idleScale);
        this.darkHeldIcon = getSvgIcon(darkHoverIconPath, heldScale);
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
