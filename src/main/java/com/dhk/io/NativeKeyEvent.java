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

/**
 * A key event reported by the native input hook, carrying the virtual key code of the key that changed state.
 *
 * @author Jonathan R. Miller
 */
public class NativeKeyEvent {

    private final int virtualKeyCode;

    /**
     * No key.
     */
    public static final int VK_UNDEFINED = 0x0;

    /**
     * Left mouse button.
     */
    public static final int VK_LBUTTON = 0x01;

    /**
     * Right mouse button.
     */
    public static final int VK_RBUTTON = 0x02;

    /**
     * Control-break processing.
     */
    public static final int VK_CANCEL = 0x03;

    /**
     * Middle mouse button (three-button mouse).
     */
    public static final int VK_MBUTTON = 0x04;

    /**
     * X1 mouse button.
     */
    public static final int VK_XBUTTON1 = 0x05;

    /**
     * X2 mouse button.
     */
    public static final int VK_XBUTTON2 = 0x06;

    /**
     * BACKSPACE key.
     */
    public static final int VK_BACK = 0x08;

    /**
     * TAB key.
     */
    public static final int VK_TAB = 0x09;

    /**
     * CLEAR key.
     */
    public static final int VK_CLEAR = 0x0C;

    /**
     * ENTER key.
     */
    public static final int VK_RETURN = 0x0D;

    /**
     * SHIFT key.
     */
    public static final int VK_SHIFT = 0x10;

    /**
     * CTRL key.
     */
    public static final int VK_CONTROL = 0x11;

    /**
     * ALT key.
     */
    public static final int VK_MENU = 0x12;

    /**
     * PAUSE key.
     */
    public static final int VK_PAUSE = 0x13;

    /**
     * CAPS LOCK key.
     */
    public static final int VK_CAPITAL = 0x14;

    /**
     * IME Kana mode.
     */
    public static final int VK_KANA = 0x15;

    /**
     * IME Hanguel mode (maintained for compatibility; use VK_HANGUL).
     */
    public static final int VK_HANGUEL = 0x15;

    /**
     * IME Hangul mode.
     */
    public static final int VK_HANGUL = 0x15;

    /**
     * IME Junja mode.
     */
    public static final int VK_JUNJA = 0x17;

    /**
     * IME final mode.
     */
    public static final int VK_FINAL = 0x18;

    /**
     * IME Hanja mode.
     */
    public static final int VK_HANJA = 0x19;

    /**
     * IME Kanji mode.
     */
    public static final int VK_KANJI = 0x19;

    /**
     * ESC key.
     */
    public static final int VK_ESCAPE = 0x1B;

    /**
     * IME convert.
     */
    public static final int VK_CONVERT = 0x1C;

    /**
     * IME nonconvert.
     */
    public static final int VK_NONCONVERT = 0x1D;

    /**
     * IME accept.
     */
    public static final int VK_ACCEPT = 0x1E;

    /**
     * IME mode change request.
     */
    public static final int VK_MODECHANGE = 0x1F;

    /**
     * SPACEBAR.
     */
    public static final int VK_SPACE = 0x20;

    /**
     * PAGE UP key.
     */
    public static final int VK_PRIOR = 0x21;

    /**
     * PAGE DOWN key.
     */
    public static final int VK_NEXT = 0x22;

    /**
     * END key.
     */
    public static final int VK_END = 0x23;

    /**
     * HOME key.
     */
    public static final int VK_HOME = 0x24;

    /**
     * LEFT ARROW key.
     */
    public static final int VK_LEFT = 0x25;

    /**
     * UP ARROW key.
     */
    public static final int VK_UP = 0x26;

    /**
     * RIGHT ARROW key.
     */
    public static final int VK_RIGHT = 0x27;

    /**
     * DOWN ARROW key.
     */
    public static final int VK_DOWN = 0x28;

    /**
     * SELECT key.
     */
    public static final int VK_SELECT = 0x29;

    /**
     * PRINT key.
     */
    public static final int VK_PRINT = 0x2A;

    /**
     * EXECUTE key.
     */
    public static final int VK_EXECUTE = 0x2B;

    /**
     * PRINT SCREEN key.
     */
    public static final int VK_SNAPSHOT = 0x2C;

    /**
     * INS key.
     */
    public static final int VK_INSERT = 0x2D;

    /**
     * DEL key.
     */
    public static final int VK_DELETE = 0x2E;

    /**
     * HELP key.
     */
    public static final int VK_HELP = 0x2F;

    /**
     * 0 key.
     */
    public static final int VK_0 = 0x30;

    /**
     * 1 key.
     */
    public static final int VK_1 = 0x31;

    /**
     * 2 key.
     */
    public static final int VK_2 = 0x32;

    /**
     * 3 key.
     */
    public static final int VK_3 = 0x33;

    /**
     * 4 key.
     */
    public static final int VK_4 = 0x34;

    /**
     * 5 key.
     */
    public static final int VK_5 = 0x35;

    /**
     * 6 key.
     */
    public static final int VK_6 = 0x36;

    /**
     * 7 key.
     */
    public static final int VK_7 = 0x37;

    /**
     * 8 key.
     */
    public static final int VK_8 = 0x38;

    /**
     * 9 key.
     */
    public static final int VK_9 = 0x39;

    /**
     * A key.
     */
    public static final int VK_A = 0x41;

    /**
     * B key.
     */
    public static final int VK_B = 0x42;

    /**
     * C key.
     */
    public static final int VK_C = 0x43;

    /**
     * D key.
     */
    public static final int VK_D = 0x44;

    /**
     * E key.
     */
    public static final int VK_E = 0x45;

    /**
     * F key.
     */
    public static final int VK_F = 0x46;

    /**
     * G key.
     */
    public static final int VK_G = 0x47;

    /**
     * H key.
     */
    public static final int VK_H = 0x48;

    /**
     * I key.
     */
    public static final int VK_I = 0x49;

    /**
     * J key.
     */
    public static final int VK_J = 0x4A;

    /**
     * K key.
     */
    public static final int VK_K = 0x4B;

    /**
     * L key.
     */
    public static final int VK_L = 0x4C;

    /**
     * M key.
     */
    public static final int VK_M = 0x4D;

    /**
     * N key.
     */
    public static final int VK_N = 0x4E;

    /**
     * O key.
     */
    public static final int VK_O = 0x4F;

    /**
     * P key.
     */
    public static final int VK_P = 0x50;

    /**
     * Q key.
     */
    public static final int VK_Q = 0x51;

    /**
     * R key.
     */
    public static final int VK_R = 0x52;

    /**
     * S key.
     */
    public static final int VK_S = 0x53;

    /**
     * T key.
     */
    public static final int VK_T = 0x54;

    /**
     * U key.
     */
    public static final int VK_U = 0x55;

    /**
     * V key.
     */
    public static final int VK_V = 0x56;

    /**
     * W key.
     */
    public static final int VK_W = 0x57;

    /**
     * X key.
     */
    public static final int VK_X = 0x58;

    /**
     * Y key.
     */
    public static final int VK_Y = 0x59;

    /**
     * Z key.
     */
    public static final int VK_Z = 0x5A;

    /**
     * Left Windows key (Natural keyboard).
     */
    public static final int VK_LWIN = 0x5B;

    /**
     * Right Windows key (Natural keyboard).
     */
    public static final int VK_RWIN = 0x5C;

    /**
     * Applications key (Natural keyboard).
     */
    public static final int VK_APPS = 0x5D;

    /**
     * Computer Sleep key.
     */
    public static final int VK_SLEEP = 0x5F;

    /**
     * Numeric keypad 0 key.
     */
    public static final int VK_NUMPAD0 = 0x60;

    /**
     * Numeric keypad 1 key.
     */
    public static final int VK_NUMPAD1 = 0x61;

    /**
     * Numeric keypad 2 key.
     */
    public static final int VK_NUMPAD2 = 0x62;

    /**
     * Numeric keypad 3 key.
     */
    public static final int VK_NUMPAD3 = 0x63;

    /**
     * Numeric keypad 4 key.
     */
    public static final int VK_NUMPAD4 = 0x64;

    /**
     * Numeric keypad 5 key.
     */
    public static final int VK_NUMPAD5 = 0x65;

    /**
     * Numeric keypad 6 key.
     */
    public static final int VK_NUMPAD6 = 0x66;

    /**
     * Numeric keypad 7 key.
     */
    public static final int VK_NUMPAD7 = 0x67;

    /**
     * Numeric keypad 8 key.
     */
    public static final int VK_NUMPAD8 = 0x68;

    /**
     * Numeric keypad 9 key.
     */
    public static final int VK_NUMPAD9 = 0x69;

    /**
     * Multiply key.
     */
    public static final int VK_MULTIPLY = 0x6A;

    /**
     * Add key.
     */
    public static final int VK_ADD = 0x6B;

    /**
     * Separator key.
     */
    public static final int VK_SEPARATOR = 0x6C;

    /**
     * Subtract key.
     */
    public static final int VK_SUBTRACT = 0x6D;

    /**
     * Decimal key.
     */
    public static final int VK_DECIMAL = 0x6E;

    /**
     * Divide key.
     */
    public static final int VK_DIVIDE = 0x6F;

    /**
     * F1 key.
     */
    public static final int VK_F1 = 0x70;

    /**
     * F2 key.
     */
    public static final int VK_F2 = 0x71;

    /**
     * F3 key.
     */
    public static final int VK_F3 = 0x72;

    /**
     * F4 key.
     */
    public static final int VK_F4 = 0x73;

    /**
     * F5 key.
     */
    public static final int VK_F5 = 0x74;

    /**
     * F6 key.
     */
    public static final int VK_F6 = 0x75;

    /**
     * F7 key.
     */
    public static final int VK_F7 = 0x76;

    /**
     * F8 key.
     */
    public static final int VK_F8 = 0x77;

    /**
     * F9 key.
     */
    public static final int VK_F9 = 0x78;

    /**
     * F10 key.
     */
    public static final int VK_F10 = 0x79;

    /**
     * F11 key.
     */
    public static final int VK_F11 = 0x7A;

    /**
     * F12 key.
     */
    public static final int VK_F12 = 0x7B;

    /**
     * F13 key.
     */
    public static final int VK_F13 = 0x7C;

    /**
     * F14 key.
     */
    public static final int VK_F14 = 0x7D;

    /**
     * F15 key.
     */
    public static final int VK_F15 = 0x7E;

    /**
     * F16 key.
     */
    public static final int VK_F16 = 0x7F;

    /**
     * F17 key.
     */
    public static final int VK_F17 = 0x80;

    /**
     * F18 key.
     */
    public static final int VK_F18 = 0x81;

    /**
     * F19 key.
     */
    public static final int VK_F19 = 0x82;

    /**
     * F20 key.
     */
    public static final int VK_F20 = 0x83;

    /**
     * F21 key.
     */
    public static final int VK_F21 = 0x84;

    /**
     * F22 key.
     */
    public static final int VK_F22 = 0x85;

    /**
     * F23 key.
     */
    public static final int VK_F23 = 0x86;

    /**
     * F24 key.
     */
    public static final int VK_F24 = 0x87;

    /**
     * NUM LOCK key.
     */
    public static final int VK_NUMLOCK = 0x90;

    /**
     * SCROLL LOCK key.
     */
    public static final int VK_SCROLL = 0x91;

    /**
     * Left SHIFT key.
     */
    public static final int VK_LSHIFT = 0xA0;

    /**
     * Right SHIFT key.
     */
    public static final int VK_RSHIFT = 0xA1;

    /**
     * Left CONTROL key.
     */
    public static final int VK_LCONTROL = 0xA2;

    /**
     * Right CONTROL key.
     */
    public static final int VK_RCONTROL = 0xA3;

    /**
     * Left MENU key.
     */
    public static final int VK_LMENU = 0xA4;

    /**
     * Right MENU key.
     */
    public static final int VK_RMENU = 0xA5;

    /**
     * Browser Back key.
     */
    public static final int VK_BROWSER_BACK = 0xA6;

    /**
     * Browser Forward key.
     */
    public static final int VK_BROWSER_FORWARD = 0xA7;

    /**
     * Browser Refresh key.
     */
    public static final int VK_BROWSER_REFRESH = 0xA8;

    /**
     * Browser Stop key.
     */
    public static final int VK_BROWSER_STOP = 0xA9;

    /**
     * Browser Search key.
     */
    public static final int VK_BROWSER_SEARCH = 0xAA;

    /**
     * Browser Favorites key.
     */
    public static final int VK_BROWSER_FAVORITES = 0xAB;

    /**
     * Browser Start and Home key.
     */
    public static final int VK_BROWSER_HOME = 0xAC;

    /**
     * Volume Mute key.
     */
    public static final int VK_VOLUME_MUTE = 0xAD;

    /**
     * Volume Down key.
     */
    public static final int VK_VOLUME_DOWN = 0xAE;

    /**
     * Volume Up key.
     */
    public static final int VK_VOLUME_UP = 0xAF;

    /**
     * Next Track key.
     */
    public static final int VK_MEDIA_NEXT_TRACK = 0xB0;

    /**
     * Previous Track key.
     */
    public static final int VK_MEDIA_PREV_TRACK = 0xB1;

    /**
     * Stop Media key.
     */
    public static final int VK_MEDIA_STOP = 0xB2;

    /**
     * Play/Pause Media key.
     */
    public static final int VK_MEDIA_PLAY_PAUSE = 0xB3;

    /**
     * Start Mail key.
     */
    public static final int VK_LAUNCH_MAIL = 0xB4;

    /**
     * Select Media key.
     */
    public static final int VK_LAUNCH_MEDIA_SELECT = 0xB5;

    /**
     * Start Application 1 key.
     */
    public static final int VK_LAUNCH_APP1 = 0xB6;

    /**
     * Start Application 2 key.
     */
    public static final int VK_LAUNCH_APP2 = 0xB7;

    /**
     * Used for miscellaneous characters.
     */
    public static final int VK_OEM_1 = 0xBA;

    /**
     * For any country/region, the '+' key.
     */
    public static final int VK_OEM_PLUS = 0xBB;

    /**
     * For any country/region, the ',' key.
     */
    public static final int VK_OEM_COMMA = 0xBC;

    /**
     * For any country/region, the '-' key.
     */
    public static final int VK_OEM_MINUS = 0xBD;

    /**
     * For any country/region, the '.' key.
     */
    public static final int VK_OEM_PERIOD = 0xBE;

    /**
     * Used for miscellaneous characters.
     */
    public static final int VK_OEM_2 = 0xBF;

    /**
     * Used for miscellaneous characters.
     */
    public static final int VK_OEM_3 = 0xC0;

    /**
     * Used for miscellaneous characters.
     */
    public static final int VK_OEM_4 = 0xDB;

    /**
     * Used for miscellaneous characters.
     */
    public static final int VK_OEM_5 = 0xDC;

    /**
     * Used for miscellaneous characters.
     */
    public static final int VK_OEM_6 = 0xDD;

    /**
     * Used for miscellaneous characters.
     */
    public static final int VK_OEM_7 = 0xDE;

    /**
     * Used for miscellaneous characters; it can vary by keyboard..
     */
    public static final int VK_OEM_8 = 0xDF;

    /**
     * Either the angle bracket key or the backslash key on the RT 102-key keyboard.
     */
    public static final int VK_OEM_102 = 0xE2;

    /**
     * IME PROCESS key.
     */
    public static final int VK_PROCESSKEY = 0xE5;

    /**
     * Used to pass Unicode characters as if they were keystrokes.
     */
    public static final int VK_PACKET = 0xE7;

    /**
     * Attn key.
     */
    public static final int VK_ATTN = 0xF6;

    /**
     * CrSel key.
     */
    public static final int VK_CRSEL = 0xF7;

    /**
     * ExSel key.
     */
    public static final int VK_EXSEL = 0xF8;

    /**
     * Erase EOF key.
     */
    public static final int VK_EREOF = 0xF9;

    /**
     * Play key.
     */
    public static final int VK_PLAY = 0xFA;

    /**
     * Zoom key.
     */
    public static final int VK_ZOOM = 0xFB;

    /**
     * Reserved.
     */
    public static final int VK_NONAME = 0xFC;

    /**
     * PA1 key.
     */
    public static final int VK_PA1 = 0xFD;

    /**
     * VK_OEM_CLEAR.
     */
    public static final int VK_OEM_CLEAR = 0xFE;

    /**
     * Constructor for the {@link NativeKeyEvent} class.
     *
     * @param virtualKeyCode
     *            - The virtual key code of the key that changed state
     */
    public NativeKeyEvent(int virtualKeyCode) {
        this.virtualKeyCode = virtualKeyCode;
    }

    /**
     * Gets the virtual key code of the key that changed state.
     *
     * @return The virtual key code
     */
    public int getVirtualKeyCode() {
        return virtualKeyCode;
    }

}
