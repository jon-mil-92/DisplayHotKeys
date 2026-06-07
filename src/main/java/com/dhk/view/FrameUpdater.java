/*
 * The MIT License (MIT)
 *
 * Copyright © 2026 Jonathan R. Miller
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the “Software”), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.dhk.view;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Updates the view for Display Hot Keys. The frame components and UI are updated with this class.
 *
 * @author Jonathan R. Miller
 */
public class FrameUpdater {

    private JFrame frame;
    private JPanel mainPanel;

    /**
     * Constructor for the {@link FrameUpdater} class.
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
