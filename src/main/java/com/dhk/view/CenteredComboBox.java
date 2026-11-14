/*
 * Display Hot Keys
 * Copyright © 2024-2026 Jonathan R. Miller
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of version 3 of the GNU
 * General Public License as published by the Free Software Foundation, together with the additional terms under section
 * 7 of that License in ADDITIONAL_TERMS.txt.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License and the additional terms along with this program in
 * LICENSE.txt and ADDITIONAL_TERMS.txt. If not, see <https://www.gnu.org/licenses/>.
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

    private String itemTextSuffix = "";

    /**
     * Serialization version of the {@link CenteredComboBox} class.
     */
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
     * Sets the text appended to each item wherever it is shown, which leaves the items themselves unchanged.
     *
     * @param itemTextSuffix
     *            - The text to append to each shown item
     */
    public void setItemTextSuffix(String itemTextSuffix) {
        this.itemTextSuffix = itemTextSuffix;
        repaint();
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

                    // An empty selection has no item to suffix, so it keeps showing no text
                    if (value != null && !itemTextSuffix.isEmpty()) {
                        lbl.setText(value + itemTextSuffix);
                    }
                }

                return c;
            }
        });
    }

}
