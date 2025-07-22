package com.dhk.window;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import com.dhk.view.DhkView;

/**
 * Updates the view for Display Hot Keys. The frame components and UI are updated with this class.
 * 
 * @author Jonathan Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2025 Jonathan Miller
 */
public class FrameUpdater {

    private JFrame frame;
    private JPanel mainPanel;

    /**
     * Constructor for the FrameUpdater class.
     * 
     * @param view
     *            - The view for the application
     */
    public FrameUpdater(DhkView view) {
        this.frame = view.getFrame();
        this.mainPanel = view.getMainPanel();
    }

    /**
     * Updates the frame components.
     */
    public void update() {
        mainPanel.revalidate();
        frame.pack();
        frame.repaint();
    }

    /**
     * Updates the UI for each component in the frame.
     */
    public void updateUI() {
        SwingUtilities.updateComponentTreeUI(frame);
        update();
    }

}
