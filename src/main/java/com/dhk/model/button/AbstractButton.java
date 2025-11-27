package com.dhk.model.button;

import javax.swing.JButton;
import com.formdev.flatlaf.extras.FlatSVGIcon;

/**
 * Defines an abstract button that implements the icon change action and defines the update methods for a button.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public abstract class AbstractButton extends JButton {

    private static final long serialVersionUID = -8040714399831618945L;

    /**
     * Default constructor for the AbstractButton class
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
    abstract void updateIdleIcon();

    /**
     * Updates the icon when the cursor is hovering over the button.
     */
    abstract void updateHoverIcon();

    /**
     * Updates the icon when the button is held down.
     */
    abstract void updateHeldIcon();

}
