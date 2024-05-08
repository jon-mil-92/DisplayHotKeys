package com.dhk.controllers.buttons;

import java.awt.Frame;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import com.dhk.controllers.Controller;
import com.dhk.ui.DhkView;

/**
 * This class controls the Minimize button. Listeners are added to the corresponding view component so that when the
 * Minimize button is pressed, the application is minimized to the system tray.
 * 
 * @author Jonathan Miller
 * @version 1.3.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class MinimizeButtonController implements Controller {
    private DhkView view;

    /**
     * Constructor for the MinimizeButtonController class.
     *
     * @param view - The view for the application.
     */
    public MinimizeButtonController(DhkView view) {
        // Get the application's view.
        this.view = view;
    }

    @Override
    public void initController() {
    }

    /**
     * This method initializes the listeners for the minimize button.
     */
    @Override
    public void initListeners() {
        // Start the action listener for the minimize button action.
        view.getMinimizeButton().addActionListener(e -> minimizeButtonAction());

        // Set the state change listener for the minimize button.
        view.getMinimizeButton().addChangeListener(e -> minimizeButtonStateChangeAction());

        // Set the focus listener for the minimize button.
        view.getMinimizeButton().addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // Switch to the rollover state when the minimize button is focused.
                view.getMinimizeButton().getModel().setRollover(true);
            }

            @Override
            public void focusLost(FocusEvent e) {
                // Leave the rollover state when the minimize button is not focused.
                view.getMinimizeButton().getModel().setRollover(false);
            }
        });

        // Set the mouse listener for the minimize button.
        view.getMinimizeButton().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // Set the focus on the minimize button when the mouse hovers over it.
                view.getMinimizeButton().requestFocusInWindow();
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
     * Minimize the frame and put the focus on the first element in the frame.
     */
    private void minimizeButtonAction() {
        // Minimize the application.
        view.getFrame().setExtendedState(Frame.ICONIFIED);

        // Transfer focus to the first element in the frame so the icon is not focused upon leaving the ICONIFIED state.
        view.getDisplayIdsLabel().requestFocusInWindow();

        // Try to free up resources upon minimizing to the system tray to utilize minimal memory while minimized.
        Runtime.getRuntime().gc();
    }

    /**
     * This method changes the minimize button icon depending on the button's state.
     */
    private void minimizeButtonStateChangeAction() {
        // If the user is holding the action button on the minimize button...
        if (view.getMinimizeButton().getModel().isArmed()) {
            // Use the pressed icon for the minimize button.
            view.getMinimizeButton().setIcon(view.getMinimizeButton().getMinimizePressedIcon());
        }
        // If the user is hovering on the minimize button...
        else if (view.getMinimizeButton().getModel().isRollover()) {
            // Use the hover icon for the minimize button.
            view.getMinimizeButton().setIcon(view.getMinimizeButton().getMinimizeHoverIcon());
        }
        // Otherwise, if the user is not interacting with the minimize button...
        else {
            // Use the idle icon for the minimize button.
            view.getMinimizeButton().setIcon(view.getMinimizeButton().getMinimizeIdleIcon());
        }
    }
}
