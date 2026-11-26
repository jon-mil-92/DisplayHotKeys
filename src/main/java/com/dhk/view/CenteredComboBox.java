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
import java.awt.Dimension;
import java.util.List;

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
    private List<CenteredComboBox<?>> columnComboBoxes = List.of();

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
     * Joins the given column of combo boxes, so this combo box takes the width of the column's widest combo box.
     *
     * @param columnComboBoxes
     *            - The combo boxes of the column, which this combo box adds itself to
     */
    public void joinColumn(List<CenteredComboBox<?>> columnComboBoxes) {
        this.columnComboBoxes = columnComboBoxes;
        columnComboBoxes.add(this);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension preferredSize = getItemsPreferredSize();

        /*
         * Hidden combo boxes count too, so showing one never resizes the column. Each width comes from the look and
         * feel's cached size, so the scan is cheap and still follows any change to a combo box's items
         */
        for (CenteredComboBox<?> columnComboBox : columnComboBoxes) {
            preferredSize.width = Math.max(preferredSize.width, columnComboBox.getItemsPreferredSize().width);
        }

        return preferredSize;
    }

    /**
     * Gets the preferred size that fits this combo box's own items, which ignores the rest of its column.
     *
     * @return The preferred size that fits this combo box's own items
     */
    private Dimension getItemsPreferredSize() {
        return super.getPreferredSize();
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
