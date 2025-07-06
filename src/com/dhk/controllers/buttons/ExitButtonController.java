package com.dhk.controllers.buttons;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import com.dhk.controllers.Controller;
import com.dhk.ui.DhkView;

/**
 * This class controls the Exit button. Listeners are added to the corresponding view component so that when a user
 * hovers over or clicks the Exit button, its icon changes, and when a user clicks on the button, the application is
 * stopped.
 * 
 * @author Jonathan Miller
 * @version 1.3.1
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class ExitButtonController implements Controller {
    private DhkView view;

    /**
     * Constructor for the ExitButtonController class.
     *
     * @param view - The view for the application.
     */
    public ExitButtonController(DhkView view) {
        // Get the application's view.
        this.view = view;
    }

    @Override
    public void initController() {
    }

    /**
     * This method initializes the listeners for the exit button.
     */
    @Override
    public void initListeners() {
        // Start the action listener for the exit button action.
        view.getExitButton().addActionListener(e -> exitButtonAction());

        // Set the state change listener for the exit button.
        view.getExitButton().addChangeListener(e -> exitButtonStateChangeAction());

        // Set the focus listener for the exit button.
        view.getExitButton().addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // Switch to the rollover state when the exit button is focused.
                view.getExitButton().getModel().setRollover(true);
            }

            @Override
            public void focusLost(FocusEvent e) {
                // Leave the rollover state when the exit button is not focused.
                view.getExitButton().getModel().setRollover(false);
            }
        });

        // Set the mouse listener for the exit button.
        view.getExitButton().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // Set the focus on the exit button when the mouse hovers over it.
                view.getExitButton().requestFocusInWindow();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Set the focus on the display IDs label when the mouse leaves the button.
                view.getDisplayIdsLabel().requestFocusInWindow();
            }
        });
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Simply exit the application.
     */
    private void exitButtonAction() {
        System.exit(0);
    }

    /**
     * This method changes the exit button icon depending on the button's state.
     */
    private void exitButtonStateChangeAction() {
        // If the user is holding the action button on the exit button...
        if (view.getExitButton().getModel().isArmed()) {
            // Use the pressed icon for the exit button.
            view.getExitButton().setIcon(view.getExitButton().getExitPressedIcon());
        }
        // If the user is hovering on the exit button...
        else if (view.getExitButton().getModel().isRollover()) {
            // Use the hover icon for the exit button.
            view.getExitButton().setIcon(view.getExitButton().getExitHoverIcon());
        }
        // Otherwise, if the user is not interacting with the exit button...
        else {
            // Use the idle icon for the exit button.
            view.getExitButton().setIcon(view.getExitButton().getExitIdleIcon());
        }
    }
}
