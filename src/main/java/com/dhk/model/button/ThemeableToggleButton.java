package com.dhk.model.button;

import java.awt.Dimension;
import javax.swing.Icon;
import javax.swing.event.ChangeListener;

/**
 * Defines a themeable toggle button with a light and dark hover and held icon.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
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
     * Constructor for the ThemeableToggle button class.
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
     * @param on
     *            - The initial on state of the button
     */
    public ThemeableToggleButton(String onIdleIconPath, String offIdleIconPath, String onHoverIconPath,
            String offHoverIconPath, String onDarkHoverIconPath, String offDarkHoverIconPath, String tooltip,
            Dimension size, float idleScale, float heldScale, boolean enabled, boolean darkMode, boolean on) {
        super(onIdleIconPath, onHoverIconPath, onIdleIconPath, onDarkHoverIconPath, tooltip, size, idleScale, heldScale,
                enabled, darkMode);

        this.onIdleIcon = getSvgIcon(onIdleIconPath, idleScale);
        this.offIdleIcon = getSvgIcon(offIdleIconPath, idleScale);

        this.onHoverIcon = getSvgIcon(onHoverIconPath, idleScale);
        this.offHoverIcon = getSvgIcon(offHoverIconPath, idleScale);

        this.onHeldIcon = getSvgIcon(onHoverIconPath, heldScale);
        this.offHeldIcon = getSvgIcon(offHoverIconPath, heldScale);

        this.onDarkHoverIcon = getSvgIcon(onDarkHoverIconPath, idleScale);
        this.offDarkHoverIcon = getSvgIcon(offDarkHoverIconPath, idleScale);

        this.onDarkHeldIcon = getSvgIcon(onDarkHoverIconPath, heldScale);
        this.offDarkHeldIcon = getSvgIcon(offDarkHoverIconPath, heldScale);

        this.on = on;

        updateIdleIcon();

        // Remove the change listeners from the parent button
        for (ChangeListener changeListener : super.getChangeListeners()) {
            super.removeChangeListener(changeListener);
        }

        addChangeListener(e -> super.iconChangeAction());
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
