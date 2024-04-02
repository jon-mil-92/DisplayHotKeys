package com.dhk.controllers;

/**
 * This interface defines a controller. Controllers must have an initListeners method.
 * 
 * @author Jonathan Miller
 * @version 1.1.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public interface Controller {
	
	/**
	 * Initialize the listeners for the controller.
	 */
	public void initListeners();
}
