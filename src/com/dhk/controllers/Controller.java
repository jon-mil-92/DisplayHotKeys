package com.dhk.controllers;

/**
 * This interface defines a controller. Controllers must have an initListeners method.
 * 
 * @author Jonathan Miller
 * @version 1.3.1
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public interface Controller {
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
