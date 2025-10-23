package com.dhk.controller.button;

import com.dhk.controller.IController;
import com.dhk.view.DhkView;

/**
 * Controls the Exit button. Listeners are added to the corresponding view component so that when a user hovers over or
 * clicks the Exit button, its icon changes, and when a user clicks on the button, the application is stopped.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class ExitButtonController extends AbstractButtonController implements IController {

    private DhkView view;

    /**
     * Constructor for the ExitButtonController class.
     *
     * @param view
     *            - The view for the application
     */
    public ExitButtonController(DhkView view) {
        this.view = view;
    }

    @Override
    public void initController() {
    }

    /**
     * Initializes the listeners for the exit button.
     */
    @Override
    public void initListeners() {
        view.getExitButton().addActionListener(e -> exitButtonAction());

        initStateChangeListeners(view.getExitButton(), view.getSelectedDisplayLabel());
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Exits the application.
     */
    private void exitButtonAction() {
        System.exit(0);
    }

}
