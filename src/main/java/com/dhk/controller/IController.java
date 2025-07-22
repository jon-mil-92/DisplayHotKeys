package com.dhk.controller;

/**
 * Defines the required methods for a controller.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public interface IController {

    /**
     * Initialize the controller.
     */
    public void initController();

    /**
     * Initialize the listeners of the controller.
     */
    public void initListeners();

    /**
     * General cleanup before re-initialization.
     */
    public void cleanUp();

}
