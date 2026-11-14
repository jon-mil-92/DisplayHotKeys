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
package com.dhk.io;

/**
 * Listener for notification area icon interactions delivered from native code, always called on the EDT.
 *
 * @author Jonathan R. Miller
 */
public interface TrayIconListener {

    /**
     * Called when an item of the tray icon's menu is chosen.
     *
     * @param itemIndex
     *            - The index of the chosen item, in the order the items were supplied
     */
    void menuItemSelected(int itemIndex);

}
