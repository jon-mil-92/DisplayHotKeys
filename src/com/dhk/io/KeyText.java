package com.dhk.io;

import java.awt.event.KeyEvent;

/**
 * This class gets the text representation for a given key code.
 * 
 * @author Jonathan Miller
 * @version 1.2.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
public class KeyText {

    /**
     * Default constructor for the KeyText class.
     */
    public KeyText() {
    }

    /**
     * This method gets the proper key text from the specified key code.
     * 
     * @param keyCode - The key code for the key to get the text for.
     * @return The text representation for the key.
     */
    public String getKeyCodeText(int keyCode) {
        String keyCodeText = "";

        // Fix text representation for the following keys due to a mismatch in key codes between System Hook and AWT.
        switch (keyCode) {
        case 222:
            keyCodeText = "Apostrophe";
            break;
        case 221:
            keyCodeText = "Close Bracket";
            break;
        case 220:
            keyCodeText = "Back Slash";
            break;
        case 219:
            keyCodeText = "Open Bracket";
            break;
        case 191:
            keyCodeText = "Forward Slash";
            break;
        case 190:
            keyCodeText = "Period";
            break;
        case 189:
            keyCodeText = "Minus";
            break;
        case 188:
            keyCodeText = "Comma";
            break;
        case 187:
            keyCodeText = "Equal";
            break;
        case 186:
            keyCodeText = "Semicolon";
            break;
        case 93:
            keyCodeText = "Menu";
            break;
        case 91:
            keyCodeText = "Windows";
            break;
        case 46:
            keyCodeText = "Delete";
            break;
        case 45:
            keyCodeText = "Insert";
            break;
        case 44:
            keyCodeText = "Print Screen";
            break;
        default:
            keyCodeText = KeyEvent.getKeyText(keyCode);
            break;
        }

        return keyCodeText;
    }
}
