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

import java.awt.Component;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.SwingConstants;

/**
 * A {@link CenteredComboBox} that centers text for both the selected item and all popup list entries.
 *
 * @param <E>
 *            - Item type
 */
public class CenteredComboBox<E> extends JComboBox<E> {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an empty centered combo box.
     */
    public CenteredComboBox() {
        super();
        installCenteredRenderer();
    }

    /**
     * Creates a centered combo box with the given items.
     *
     * @param items
     *            - Items to display
     */
    public CenteredComboBox(E[] items) {
        super(items);
        installCenteredRenderer();
    }

    /**
     * Creates a centered combo box backed by the given model.
     *
     * @param model
     *            - Combo box model
     */
    public CenteredComboBox(ComboBoxModel<E> model) {
        super(model);
        installCenteredRenderer();
    }

    /**
     * Installs a renderer that centers all item text.
     */
    private void installCenteredRenderer() {
        setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {

                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (c instanceof JLabel lbl) {
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                }

                return c;
            }
        });
    }

}
