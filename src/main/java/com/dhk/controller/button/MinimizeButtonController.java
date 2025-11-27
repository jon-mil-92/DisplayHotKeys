package com.dhk.controller.button;

import java.awt.Frame;
import com.dhk.controller.IController;
import com.dhk.view.DhkView;

/**
 * Controls the Minimize button. Listeners are added to the corresponding view component so that when the Minimize
 * button is pressed, the application is minimized to the system tray.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class MinimizeButtonController extends AbstractButtonController implements IController {

    private DhkView view;

    /**
     * Constructor for the MinimizeButtonController class.
     *
     * @param view
     *            - The view for the application
     */
    public MinimizeButtonController(DhkView view) {
        this.view = view;
    }

    @Override
    public void initController() {
    }

    /**
     * Initializes the listeners for the minimize button.
     */
    @Override
    public void initListeners() {
        view.getMinimizeButton().addActionListener(e -> minimizeButtonAction());

        initStateChangeListeners(view.getMinimizeButton(), view.getSelectedDisplayLabel());
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Minimizes the frame and put the focus on the first element in the frame.
     */
    private void minimizeButtonAction() {
        // Minimize the application
        view.getFrame().setExtendedState(Frame.ICONIFIED);

        // Focus on the selected display label so the button is not focused on upon leaving the ICONIFIED state
        view.getSelectedDisplayLabel().requestFocusInWindow();

        // Try to free up resources upon minimizing to the system tray to utilize minimal memory while minimized
        System.gc();
    }

}
