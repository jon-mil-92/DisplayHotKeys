package com.dhk.controllers;

/**
 * This interface defines a controller. Controllers must have an initListeners method.
 * 
 * @version 1.0.0
 * @author Jonathan Miller
 */
public interface Controller {
	
	/**
	 * Initialize the listeners for the controller.
	 */
	public void initListeners();
}
