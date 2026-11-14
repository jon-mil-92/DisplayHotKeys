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
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;

import com.dhk.io.SystemTrayIcon;
import com.formdev.flatlaf.fonts.roboto.FlatRobotoFont;
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
     * Whether the native layer holds the Roboto font, which it keeps for the life of the process, so the font file is
     * only read until then.
     */
    private static boolean robotoFontRegistered;

    /**
     * Text of each menu item, in the order the items are shown and their actions are indexed.
     */
    private static final String[] ITEM_TEXTS = {"Restore", "Settings", "About", "Exit"};

    /**
     * Roboto font file, read from beside the class that installs the font.
     */
    private static final String ROBOTO_FONT_FILE = "Roboto-Regular.ttf";

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
     * Hands the items and the current theme's menu look to the tray icon and routes chosen items to their actions. The
     * theme is only read here because it can only change while the application window is showing.
     */
    public void install() {
        systemTrayIcon.registerTrayIconListener(this::runItemAction);

        // Laid out by the theme's own menu items, so the native menu takes the size and insets the theme gives one
        JPopupMenu layoutMenu = new JPopupMenu();
        int itemWidth = 0;
        int itemHeight = 0;

        for (String itemText : ITEM_TEXTS) {
            Dimension itemSize = layoutMenu.add(itemText).getPreferredSize();

            itemWidth = Math.max(itemWidth, itemSize.width);
            itemHeight = Math.max(itemHeight, itemSize.height);
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

        int[] menuMetrics = new int[SystemTrayIcon.MENU_METRIC_COUNT];
        menuMetrics[SystemTrayIcon.MENU_METRIC_ITEM_WIDTH] = itemWidth;
        menuMetrics[SystemTrayIcon.MENU_METRIC_ITEM_HEIGHT] = itemHeight;
        menuMetrics[SystemTrayIcon.MENU_METRIC_TEXT_INSET] = textInset(firstItem);
        menuMetrics[SystemTrayIcon.MENU_METRIC_FONT_SIZE] = itemFont.getSize();

        byte[] fontData = fontData(itemFont);
        boolean accepted = systemTrayIcon.setMenu(ITEM_TEXTS, menuColors, menuMetrics, itemFont.getFamily(), fontData);

        if (accepted && fontData != null) {
            robotoFontRegistered = true;
        }
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

    /**
     * Returns the font file for a font the system does not have installed, so the native layer can draw in it.
     *
     * @param font
     *            - The font the items are drawn in
     *
     * @return The font file, or null when the font is not bundled or the native layer already registered it
     */
    private byte[] fontData(Font font) {
        if (robotoFontRegistered || !FlatRobotoFont.FAMILY.equals(font.getFamily())) {
            return null;
        }

        try (InputStream fontStream = FlatRobotoFont.class.getResourceAsStream(ROBOTO_FONT_FILE)) {
            return fontStream != null ? fontStream.readAllBytes() : null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
