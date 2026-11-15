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

import com.dhk.localization.Localizer;

/**
 * Gets the text representation for a given key code in the selected language. Named keys take the name the language's
 * style guide gives them, while letters, digits, function keys, and punctuation read as keyboards print them.
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
     *            - The virtual-key code of the key to get the text for
     *
     * @return The text representation for the key
     */
    public static String getKeyCodeText(int keyCode) {
        // Keyboards print letters, digits, and function keys the same way in every language
        if ((keyCode >= NativeKeyEvent.VK_0 && keyCode <= NativeKeyEvent.VK_9)
                || (keyCode >= NativeKeyEvent.VK_A && keyCode <= NativeKeyEvent.VK_Z)) {
            return String.valueOf((char) keyCode);
        }

        if (keyCode >= NativeKeyEvent.VK_F1 && keyCode <= NativeKeyEvent.VK_F24) {
            return "F" + (keyCode - NativeKeyEvent.VK_F1 + 1);
        }

        if (keyCode >= NativeKeyEvent.VK_NUMPAD0 && keyCode <= NativeKeyEvent.VK_NUMPAD9) {
            return Localizer.format("key.numPad", String.valueOf(keyCode - NativeKeyEvent.VK_NUMPAD0));
        }

        String numPadSymbol = getNumPadSymbol(keyCode);

        if (numPadSymbol != null) {
            return Localizer.format("key.numPad", numPadSymbol);
        }

        String punctuationSymbol = getPunctuationSymbol(keyCode);

        if (punctuationSymbol != null) {
            return punctuationSymbol;
        }

        String nameKey = getNameKey(keyCode);

        return nameKey != null ? Localizer.getText(nameKey) : Localizer.format("key.unknown", keyCode);
    }

    /**
     * Gets the symbol printed on a number pad operator key.
     *
     * @param keyCode
     *            - The virtual-key code of the key
     *
     * @return The symbol of the number pad operator key, or null when the key is not one
     */
    private static String getNumPadSymbol(int keyCode) {
        return switch (keyCode) {
            case NativeKeyEvent.VK_MULTIPLY -> "*";
            case NativeKeyEvent.VK_ADD -> "+";
            case NativeKeyEvent.VK_SEPARATOR -> ",";
            case NativeKeyEvent.VK_SUBTRACT -> "-";
            case NativeKeyEvent.VK_DECIMAL -> ".";
            case NativeKeyEvent.VK_DIVIDE -> "/";
            default -> null;
        };
    }

    /**
     * Gets the symbol a punctuation key types on a US standard keyboard, which reads the same in every language.
     *
     * @param keyCode
     *            - The virtual-key code of the key
     *
     * @return The symbol of the punctuation key, or null when the key is not one
     */
    private static String getPunctuationSymbol(int keyCode) {
        return switch (keyCode) {
            case NativeKeyEvent.VK_OEM_1 -> ";";
            case NativeKeyEvent.VK_OEM_PLUS -> "=";
            case NativeKeyEvent.VK_OEM_COMMA -> ",";
            case NativeKeyEvent.VK_OEM_MINUS -> "-";
            case NativeKeyEvent.VK_OEM_PERIOD -> ".";
            case NativeKeyEvent.VK_OEM_2 -> "/";
            case NativeKeyEvent.VK_OEM_3 -> "`";
            case NativeKeyEvent.VK_OEM_4 -> "[";
            case NativeKeyEvent.VK_OEM_5 -> "\\";
            case NativeKeyEvent.VK_OEM_6 -> "]";
            case NativeKeyEvent.VK_OEM_7 -> "'";
            // A US standard keyboard lacks this key, so it takes the angle bracket other keyboards print on it
            case NativeKeyEvent.VK_OEM_102 -> "<";
            default -> null;
        };
    }

    /**
     * Gets the message key of a named key's name.
     *
     * @param keyCode
     *            - The virtual-key code of the key
     *
     * @return The message key of the key's name, or null when the key has no name of its own
     */
    private static String getNameKey(int keyCode) {
        return switch (keyCode) {
            case NativeKeyEvent.VK_CANCEL -> "key.break";
            case NativeKeyEvent.VK_BACK -> "key.backspace";
            case NativeKeyEvent.VK_TAB -> "key.tab";
            case NativeKeyEvent.VK_CLEAR -> "key.clear";
            case NativeKeyEvent.VK_RETURN -> "key.enter";
            case NativeKeyEvent.VK_SHIFT, NativeKeyEvent.VK_LSHIFT, NativeKeyEvent.VK_RSHIFT -> "key.shift";
            case NativeKeyEvent.VK_CONTROL, NativeKeyEvent.VK_LCONTROL, NativeKeyEvent.VK_RCONTROL -> "key.ctrl";
            case NativeKeyEvent.VK_MENU, NativeKeyEvent.VK_LMENU, NativeKeyEvent.VK_RMENU -> "key.alt";
            case NativeKeyEvent.VK_PAUSE -> "key.pause";
            case NativeKeyEvent.VK_CAPITAL -> "key.capsLock";
            case NativeKeyEvent.VK_KANA -> "key.kanaHangul";
            case NativeKeyEvent.VK_JUNJA -> "key.junja";
            case NativeKeyEvent.VK_FINAL -> "key.final";
            case NativeKeyEvent.VK_KANJI -> "key.hanjaKanji";
            case NativeKeyEvent.VK_ESCAPE -> "key.escape";
            case NativeKeyEvent.VK_CONVERT -> "key.convert";
            case NativeKeyEvent.VK_NONCONVERT -> "key.nonConvert";
            case NativeKeyEvent.VK_ACCEPT -> "key.accept";
            case NativeKeyEvent.VK_MODECHANGE -> "key.modeChange";
            case NativeKeyEvent.VK_SPACE -> "key.space";
            case NativeKeyEvent.VK_PRIOR -> "key.pageUp";
            case NativeKeyEvent.VK_NEXT -> "key.pageDown";
            case NativeKeyEvent.VK_END -> "key.end";
            case NativeKeyEvent.VK_HOME -> "key.home";
            case NativeKeyEvent.VK_LEFT -> "key.left";
            case NativeKeyEvent.VK_UP -> "key.up";
            case NativeKeyEvent.VK_RIGHT -> "key.right";
            case NativeKeyEvent.VK_DOWN -> "key.down";
            case NativeKeyEvent.VK_SELECT -> "key.select";
            case NativeKeyEvent.VK_PRINT -> "key.print";
            case NativeKeyEvent.VK_EXECUTE -> "key.execute";
            case NativeKeyEvent.VK_SNAPSHOT -> "key.printScreen";
            case NativeKeyEvent.VK_INSERT -> "key.insert";
            case NativeKeyEvent.VK_DELETE -> "key.delete";
            case NativeKeyEvent.VK_HELP -> "key.help";
            case NativeKeyEvent.VK_LWIN, NativeKeyEvent.VK_RWIN -> "key.windows";
            case NativeKeyEvent.VK_APPS -> "key.menu";
            case NativeKeyEvent.VK_SLEEP -> "key.sleep";
            case NativeKeyEvent.VK_NUMLOCK -> "key.numLock";
            case NativeKeyEvent.VK_SCROLL -> "key.scrollLock";
            case NativeKeyEvent.VK_BROWSER_BACK -> "key.browserBack";
            case NativeKeyEvent.VK_BROWSER_FORWARD -> "key.browserForward";
            case NativeKeyEvent.VK_BROWSER_REFRESH -> "key.browserRefresh";
            case NativeKeyEvent.VK_BROWSER_STOP -> "key.browserStop";
            case NativeKeyEvent.VK_BROWSER_SEARCH -> "key.browserSearch";
            case NativeKeyEvent.VK_BROWSER_FAVORITES -> "key.browserFavorites";
            case NativeKeyEvent.VK_BROWSER_HOME -> "key.browserHome";
            case NativeKeyEvent.VK_VOLUME_MUTE -> "key.volumeMute";
            case NativeKeyEvent.VK_VOLUME_DOWN -> "key.volumeDown";
            case NativeKeyEvent.VK_VOLUME_UP -> "key.volumeUp";
            case NativeKeyEvent.VK_MEDIA_NEXT_TRACK -> "key.mediaNext";
            case NativeKeyEvent.VK_MEDIA_PREV_TRACK -> "key.mediaPrevious";
            case NativeKeyEvent.VK_MEDIA_STOP -> "key.mediaStop";
            case NativeKeyEvent.VK_MEDIA_PLAY_PAUSE -> "key.mediaPlayPause";
            case NativeKeyEvent.VK_LAUNCH_MAIL -> "key.launchMail";
            case NativeKeyEvent.VK_LAUNCH_MEDIA_SELECT -> "key.launchMediaSelect";
            case NativeKeyEvent.VK_LAUNCH_APP1 -> "key.launchApp1";
            case NativeKeyEvent.VK_LAUNCH_APP2 -> "key.launchApp2";
            default -> null;
        };
    }

}
