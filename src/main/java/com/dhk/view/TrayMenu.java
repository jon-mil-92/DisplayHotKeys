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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;

import com.dhk.io.SystemTrayIcon;
import com.dhk.localization.Localizer;
import com.formdev.flatlaf.util.UIScale;

/**
 * Supplies the tray icon's menu to the native layer, which shows and dismisses it without waiting on the virtual
 * machine however busy the system is. The items take the current theme's look, and each runs its action.
 *
 * @author Jonathan R. Miller
 */
public class TrayMenu {

    private SystemTrayIcon systemTrayIcon;
    private Runnable[] itemActions;

    /**
     * Message key of each menu item's text, in the order the items are shown and their actions are indexed.
     */
    private static final String[] ITEM_TEXT_KEYS = {"tray.restore", "tray.settings", "tray.about", "tray.exit"};

    /**
     * Constructor for the {@link TrayMenu} class.
     *
     * @param systemTrayIcon
     *            - The tray icon the menu belongs to
     * @param restoreAction
     *            - The action to run when the restore item is selected
     * @param settingsAction
     *            - The action to run when the settings item is selected
     * @param aboutAction
     *            - The action to run when the about item is selected
     * @param exitAction
     *            - The action to run when the exit item is selected
     */
    public TrayMenu(SystemTrayIcon systemTrayIcon, Runnable restoreAction, Runnable settingsAction,
            Runnable aboutAction, Runnable exitAction) {
        this.systemTrayIcon = systemTrayIcon;
        this.itemActions = new Runnable[]{restoreAction, settingsAction, aboutAction, exitAction};
    }

    /**
     * Hands the items in the selected language and the current theme's menu look to the tray icon, and routes chosen
     * items to their actions. It is installed again whenever the language or the theme changes.
     */
    public void install() {
        systemTrayIcon.registerTrayIconListener(this::runItemAction);

        // Laid out by the theme's own menu items, so the native menu takes the height and insets the theme gives one
        JPopupMenu layoutMenu = new JPopupMenu();
        String[] itemTexts = new String[ITEM_TEXT_KEYS.length];
        int itemHeight = 0;

        for (int itemIndex = 0; itemIndex < ITEM_TEXT_KEYS.length; itemIndex++) {
            itemTexts[itemIndex] = Localizer.getText(ITEM_TEXT_KEYS[itemIndex]);
            itemHeight = Math.max(itemHeight, layoutMenu.add(itemTexts[itemIndex]).getPreferredSize().height);
        }

        JMenuItem firstItem = (JMenuItem) layoutMenu.getComponent(0);
        Color background = layoutMenu.getBackground();
        Color foreground = firstItem.getForeground();
        Font itemFont = firstItem.getFont();

        int[] menuColors = new int[SystemTrayIcon.MENU_COLOR_COUNT];
        menuColors[SystemTrayIcon.MENU_COLOR_BACKGROUND] = background.getRGB();
        menuColors[SystemTrayIcon.MENU_COLOR_FOREGROUND] = foreground.getRGB();
        menuColors[SystemTrayIcon.MENU_COLOR_SELECTION_BACKGROUND] = themeColor("MenuItem.selectionBackground",
                foreground);
        menuColors[SystemTrayIcon.MENU_COLOR_SELECTION_FOREGROUND] = themeColor("MenuItem.selectionForeground",
                background);
        menuColors[SystemTrayIcon.MENU_COLOR_BORDER] = themeColor("PopupMenu.borderColor", foreground);

        int textInset = textInset(firstItem);

        // Only the space around the text is sent, since the native layer measures the text at the display's scale
        int textWidth = firstItem.getFontMetrics(itemFont).stringWidth(firstItem.getText());
        int trailingInset = firstItem.getPreferredSize().width - textInset - textWidth;

        int[] menuMetrics = new int[SystemTrayIcon.MENU_METRIC_COUNT];
        menuMetrics[SystemTrayIcon.MENU_METRIC_TRAILING_INSET] = trailingInset;
        menuMetrics[SystemTrayIcon.MENU_METRIC_ITEM_HEIGHT] = itemHeight;
        menuMetrics[SystemTrayIcon.MENU_METRIC_TEXT_INSET] = textInset;
        menuMetrics[SystemTrayIcon.MENU_METRIC_FONT_SIZE] = itemFont.getSize();

        systemTrayIcon.setMenu(itemTexts, menuColors, menuMetrics, itemFont.getFamily());
    }

    /**
     * Runs the action of the chosen item.
     *
     * @param itemIndex
     *            - The index of the chosen item
     */
    private void runItemAction(int itemIndex) {
        if (itemIndex < 0 || itemIndex >= itemActions.length) {
            return;
        }

        itemActions[itemIndex].run();
    }

    /**
     * Returns a color of the current theme in packed ARGB order, or the fallback color when the theme lacks it.
     *
     * @param colorKey
     *            - The key of the theme color
     * @param fallbackColor
     *            - The color to use when the theme lacks the key
     *
     * @return The color in packed ARGB order
     */
    private int themeColor(String colorKey, Color fallbackColor) {
        Color color = UIManager.getColor(colorKey);

        return (color != null ? color : fallbackColor).getRGB();
    }

    /**
     * Returns where the theme starts an item's text, past the item's leading inset and the space the theme reserves for
     * an icon even when the item has none.
     *
     * @param menuItem
     *            - The laid out menu item to measure
     *
     * @return The distance from the item's leading edge to its text
     */
    private int textInset(JMenuItem menuItem) {
        Dimension minimumIconSize = UIManager.getDimension("MenuItem.minimumIconSize");
        int iconWidth = minimumIconSize != null ? minimumIconSize.width : 0;

        return menuItem.getInsets().left + UIScale.scale(iconWidth) + UIScale.scale(menuItem.getIconTextGap());
    }

}
