package com.dhk.controller.button;

import com.dhk.controller.IController;
import com.dhk.model.DhkModel;
import com.dhk.view.AboutDialog;
import com.dhk.view.DhkView;

/**
 * Controls the About button. Listeners are added to the corresponding view component so that when the About button is
 * pressed, an "About Display Hot Keys" dialog is shown with application information.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2026 Jonathan Miller
 */
public class AboutButtonController extends AbstractButtonController implements IController {

    private DhkModel model;
    private DhkView view;
    private AboutDialog aboutDialog;

    /**
     * Constructor for the {@link AboutButtonController} class.
     *
     * @param model
     *            - The model for the application
     * @param view
     *            - The view for the application
     */
    public AboutButtonController(DhkModel model, DhkView view) {
        this.model = model;
        this.view = view;
    }

    @Override
    public void initController() {
    }

    @Override
    public void initListeners() {
        view.getAboutButton().addActionListener(e -> aboutButtonAction());

        initStateChangeListeners(view.getAboutButton(), view.getDefaultFocusComponent());
    }

    @Override
    public void cleanUp() {
    }

    /**
     * Shows an "About Display Hot Keys" dialog with application information.
     */
    private void aboutButtonAction() {
        aboutDialog = new AboutDialog(view.getFrame(), model);
        aboutDialog.showAboutDialog();
    }

}
