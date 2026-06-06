package com.dhk.controller.button;

import java.awt.Frame;
import com.dhk.controller.IController;
import com.dhk.view.DhkView;

/**
 * Controls the Minimize button. Listeners are added to the corresponding view component so that when the Minimize
 * button is pressed, the application is minimized to the system tray.
 * 
 * @author Jonathan R. Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2026 Jonathan R. Miller
 */
public class MinimizeButtonController extends AbstractButtonController implements IController {

    private DhkView view;

    /**
     * Constructor for the {@link MinimizeButtonController} class.
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

    @Override
    public void initListeners() {
        view.getMinimizeButton().addActionListener(_ -> minimizeButtonAction());

        initStateChangeListeners(view.getMinimizeButton(), view.getDefaultFocusComponent());
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
        view.getDefaultFocusComponent().requestFocusInWindow();

        // Try to free up resources upon minimizing to the system tray to utilize minimal memory while minimized
        System.gc();
    }

}
