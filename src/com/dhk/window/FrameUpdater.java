package com.dhk.window;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.dhk.ui.DhkView;

/**
 * This class updates the view for Display Hot Keys. The frame components and UI are updated with this class.
 * 
 * @author Jonathan Miller
 * @version 1.5.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2025
 */
public class FrameUpdater {
    private JFrame frame;
    private JPanel mainPanel;

    /**
     * Constructor for the FrameUpdater class.
     * 
     * @param view - The view for the application.
     */
    public FrameUpdater(DhkView view) {
        // Get the application view's frame and main panel.
        this.frame = view.getFrame();
        this.mainPanel = view.getMainPanel();
    }

    /**
     * This method updates the frame components.
     */
    public void update() {
        // Revalidate the main panel components.
        mainPanel.revalidate();

        // Pack the panel components into the frame and automatically size the window.
        frame.pack();

        // Repaint the frame.
        frame.repaint();
    }

    /**
     * This method updates the UI for each component in the frame.
     */
    public void updateUI() {
        // Update the UI for each component in the frame.
        SwingUtilities.updateComponentTreeUI(frame);

        // Update the frame components.
        update();
    }
}
