package com.dhk.view;

import java.awt.Component;

/**
 * Interface for views.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2026 Jonathan Miller
 */
public interface IView {

    /**
     * Gets the default focus component in the view.
     * 
     * @return The default focus component in the view
     */
    public Component getDefaultFocusComponent();

}
