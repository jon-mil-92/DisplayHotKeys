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

        // Fix text representation for the following keys due to a mismatch in key codes between System Hook and AWT
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
