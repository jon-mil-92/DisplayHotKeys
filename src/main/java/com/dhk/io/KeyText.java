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

import java.awt.event.KeyEvent;

/**
 * Gets the text representation for a given key code.
 *
 * @author Jonathan R. Miller
 */
public class KeyText {

    /**
     * Default constructor for the {@link KeyText} class.
     */
    public KeyText() {
    }

    /**
     * Gets the proper key text from the specified key code.
     *
     * @param keyCode
     *            - The key code for the key to get the text for
     *
     * @return The text representation for the key
     */
    public static String getKeyCodeText(int keyCode) {
        String keyCodeText = "";

        // Fix text representation for the following keys due to a mismatch in key codes between the input hook and AWT
        switch (keyCode) {
            case 222 :
                keyCodeText = "Apostrophe";
                break;
            case 221 :
                keyCodeText = "Close Bracket";
                break;
            case 220 :
                keyCodeText = "Back Slash";
                break;
            case 219 :
                keyCodeText = "Open Bracket";
                break;
            case 191 :
                keyCodeText = "Forward Slash";
                break;
            case 190 :
                keyCodeText = "Period";
                break;
            case 189 :
                keyCodeText = "Minus";
                break;
            case 188 :
                keyCodeText = "Comma";
                break;
            case 187 :
                keyCodeText = "Equal";
                break;
            case 186 :
                keyCodeText = "Semicolon";
                break;
            case 93 :
                keyCodeText = "Menu";
                break;
            case 91 :
                keyCodeText = "Windows";
                break;
            case 46 :
                keyCodeText = "Delete";
                break;
            case 45 :
                keyCodeText = "Insert";
                break;
            case 44 :
                keyCodeText = "Print Screen";
                break;
            case 13 :
                keyCodeText = "Enter";
                break;
            default :
                keyCodeText = KeyEvent.getKeyText(keyCode);
                break;
        }

        return keyCodeText;
    }

}
