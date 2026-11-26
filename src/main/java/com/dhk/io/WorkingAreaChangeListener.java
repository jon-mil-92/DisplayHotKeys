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
 * Listener for working area changes delivered from native code. Moving, resizing, or auto-hiding the task bar changes a
 * display's working area without changing the display itself, so it needs only a re-fit, not a full re-initialization.
 *
 * @author Jonathan R. Miller
 */
public interface WorkingAreaChangeListener {

    /**
     * Called when the native layer detects that a display's working area changed.
     */
    void workingAreaChanged();

}
