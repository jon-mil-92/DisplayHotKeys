package com.dhk.controller;

/**
 * Defines the required methods for a controller.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2026 Jonathan Miller
 */
public interface IController {

    /**
     * Initializes the controller.
     */
    public void initController();

    /**
     * Initializes the listeners of the controller.
     */
    public void initListeners();

    /**
     * Cleans up after the controller before re-initialization.
     */
    public void cleanUp();

}
