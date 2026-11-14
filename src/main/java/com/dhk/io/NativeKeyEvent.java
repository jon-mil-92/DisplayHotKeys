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
 * A key event reported by the native input hook, carrying the virtual key code of the key that changed state.
 *
 * @author Jonathan R. Miller
 */
public class NativeKeyEvent {

    private final int virtualKeyCode;

    /**
     * Code that stands for no key, since Windows assigns no virtual key to zero.
     */
    public static final int VK_UNDEFINED = 0x0;

    /**
     * Virtual-key code of the left mouse button.
     */
    public static final int VK_LBUTTON = 0x01;

    /**
     * Virtual-key code of the right mouse button.
     */
    public static final int VK_RBUTTON = 0x02;

    /**
     * Virtual-key code of the control-break key.
     */
    public static final int VK_CANCEL = 0x03;

    /**
     * Virtual-key code of the middle mouse button.
     */
    public static final int VK_MBUTTON = 0x04;

    /**
     * Virtual-key code of the X1 mouse button.
     */
    public static final int VK_XBUTTON1 = 0x05;

    /**
     * Virtual-key code of the X2 mouse button.
     */
    public static final int VK_XBUTTON2 = 0x06;

    /**
     * Virtual-key code of the BACKSPACE key.
     */
    public static final int VK_BACK = 0x08;

    /**
     * Virtual-key code of the TAB key.
     */
    public static final int VK_TAB = 0x09;

    /**
     * Virtual-key code of the CLEAR key.
     */
    public static final int VK_CLEAR = 0x0C;

    /**
     * Virtual-key code of the ENTER key.
     */
    public static final int VK_RETURN = 0x0D;

    /**
     * Virtual-key code of the SHIFT key.
     */
    public static final int VK_SHIFT = 0x10;

    /**
     * Virtual-key code of the CTRL key.
     */
    public static final int VK_CONTROL = 0x11;

    /**
     * Virtual-key code of the ALT key.
     */
    public static final int VK_MENU = 0x12;

    /**
     * Virtual-key code of the PAUSE key.
     */
    public static final int VK_PAUSE = 0x13;

    /**
     * Virtual-key code of the CAPS LOCK key.
     */
    public static final int VK_CAPITAL = 0x14;

    /**
     * Virtual-key code of the IME Kana mode key.
     */
    public static final int VK_KANA = 0x15;

    /**
     * Older name for {@link #VK_HANGUL}, kept for compatibility.
     */
    public static final int VK_HANGUEL = 0x15;

    /**
     * Virtual-key code of the IME Hangul mode key.
     */
    public static final int VK_HANGUL = 0x15;

    /**
     * Virtual-key code of the IME Junja mode key.
     */
    public static final int VK_JUNJA = 0x17;

    /**
     * Virtual-key code of the IME final mode key.
     */
    public static final int VK_FINAL = 0x18;

    /**
     * Virtual-key code of the IME Hanja mode key.
     */
    public static final int VK_HANJA = 0x19;

    /**
     * Virtual-key code of the IME Kanji mode key.
     */
    public static final int VK_KANJI = 0x19;

    /**
     * Virtual-key code of the ESC key.
     */
    public static final int VK_ESCAPE = 0x1B;

    /**
     * Virtual-key code of the IME convert key.
     */
    public static final int VK_CONVERT = 0x1C;

    /**
     * Virtual-key code of the IME nonconvert key.
     */
    public static final int VK_NONCONVERT = 0x1D;

    /**
     * Virtual-key code of the IME accept key.
     */
    public static final int VK_ACCEPT = 0x1E;

    /**
     * Virtual-key code of the IME mode change request key.
     */
    public static final int VK_MODECHANGE = 0x1F;

    /**
     * Virtual-key code of the SPACEBAR.
     */
    public static final int VK_SPACE = 0x20;

    /**
     * Virtual-key code of the PAGE UP key.
     */
    public static final int VK_PRIOR = 0x21;

    /**
     * Virtual-key code of the PAGE DOWN key.
     */
    public static final int VK_NEXT = 0x22;

    /**
     * Virtual-key code of the END key.
     */
    public static final int VK_END = 0x23;

    /**
     * Virtual-key code of the HOME key.
     */
    public static final int VK_HOME = 0x24;

    /**
     * Virtual-key code of the LEFT ARROW key.
     */
    public static final int VK_LEFT = 0x25;

    /**
     * Virtual-key code of the UP ARROW key.
     */
    public static final int VK_UP = 0x26;

    /**
     * Virtual-key code of the RIGHT ARROW key.
     */
    public static final int VK_RIGHT = 0x27;

    /**
     * Virtual-key code of the DOWN ARROW key.
     */
    public static final int VK_DOWN = 0x28;

    /**
     * Virtual-key code of the SELECT key.
     */
    public static final int VK_SELECT = 0x29;

    /**
     * Virtual-key code of the PRINT key.
     */
    public static final int VK_PRINT = 0x2A;

    /**
     * Virtual-key code of the EXECUTE key.
     */
    public static final int VK_EXECUTE = 0x2B;

    /**
     * Virtual-key code of the PRINT SCREEN key.
     */
    public static final int VK_SNAPSHOT = 0x2C;

    /**
     * Virtual-key code of the INS key.
     */
    public static final int VK_INSERT = 0x2D;

    /**
     * Virtual-key code of the DEL key.
     */
    public static final int VK_DELETE = 0x2E;

    /**
     * Virtual-key code of the HELP key.
     */
    public static final int VK_HELP = 0x2F;

    /**
     * Virtual-key code of the 0 key.
     */
    public static final int VK_0 = 0x30;

    /**
     * Virtual-key code of the 1 key.
     */
    public static final int VK_1 = 0x31;

    /**
     * Virtual-key code of the 2 key.
     */
    public static final int VK_2 = 0x32;

    /**
     * Virtual-key code of the 3 key.
     */
    public static final int VK_3 = 0x33;

    /**
     * Virtual-key code of the 4 key.
     */
    public static final int VK_4 = 0x34;

    /**
     * Virtual-key code of the 5 key.
     */
    public static final int VK_5 = 0x35;

    /**
     * Virtual-key code of the 6 key.
     */
    public static final int VK_6 = 0x36;

    /**
     * Virtual-key code of the 7 key.
     */
    public static final int VK_7 = 0x37;

    /**
     * Virtual-key code of the 8 key.
     */
    public static final int VK_8 = 0x38;

    /**
     * Virtual-key code of the 9 key.
     */
    public static final int VK_9 = 0x39;

    /**
     * Virtual-key code of the A key.
     */
    public static final int VK_A = 0x41;

    /**
     * Virtual-key code of the B key.
     */
    public static final int VK_B = 0x42;

    /**
     * Virtual-key code of the C key.
     */
    public static final int VK_C = 0x43;

    /**
     * Virtual-key code of the D key.
     */
    public static final int VK_D = 0x44;

    /**
     * Virtual-key code of the E key.
     */
    public static final int VK_E = 0x45;

    /**
     * Virtual-key code of the F key.
     */
    public static final int VK_F = 0x46;

    /**
     * Virtual-key code of the G key.
     */
    public static final int VK_G = 0x47;

    /**
     * Virtual-key code of the H key.
     */
    public static final int VK_H = 0x48;

    /**
     * Virtual-key code of the I key.
     */
    public static final int VK_I = 0x49;

    /**
     * Virtual-key code of the J key.
     */
    public static final int VK_J = 0x4A;

    /**
     * Virtual-key code of the K key.
     */
    public static final int VK_K = 0x4B;

    /**
     * Virtual-key code of the L key.
     */
    public static final int VK_L = 0x4C;

    /**
     * Virtual-key code of the M key.
     */
    public static final int VK_M = 0x4D;

    /**
     * Virtual-key code of the N key.
     */
    public static final int VK_N = 0x4E;

    /**
     * Virtual-key code of the O key.
     */
    public static final int VK_O = 0x4F;

    /**
     * Virtual-key code of the P key.
     */
    public static final int VK_P = 0x50;

    /**
     * Virtual-key code of the Q key.
     */
    public static final int VK_Q = 0x51;

    /**
     * Virtual-key code of the R key.
     */
    public static final int VK_R = 0x52;

    /**
     * Virtual-key code of the S key.
     */
    public static final int VK_S = 0x53;

    /**
     * Virtual-key code of the T key.
     */
    public static final int VK_T = 0x54;

    /**
     * Virtual-key code of the U key.
     */
    public static final int VK_U = 0x55;

    /**
     * Virtual-key code of the V key.
     */
    public static final int VK_V = 0x56;

    /**
     * Virtual-key code of the W key.
     */
    public static final int VK_W = 0x57;

    /**
     * Virtual-key code of the X key.
     */
    public static final int VK_X = 0x58;

    /**
     * Virtual-key code of the Y key.
     */
    public static final int VK_Y = 0x59;

    /**
     * Virtual-key code of the Z key.
     */
    public static final int VK_Z = 0x5A;

    /**
     * Virtual-key code of the left Windows key.
     */
    public static final int VK_LWIN = 0x5B;

    /**
     * Virtual-key code of the right Windows key.
     */
    public static final int VK_RWIN = 0x5C;

    /**
     * Virtual-key code of the Applications key.
     */
    public static final int VK_APPS = 0x5D;

    /**
     * Virtual-key code of the computer Sleep key.
     */
    public static final int VK_SLEEP = 0x5F;

    /**
     * Virtual-key code of the numeric keypad 0 key.
     */
    public static final int VK_NUMPAD0 = 0x60;

    /**
     * Virtual-key code of the numeric keypad 1 key.
     */
    public static final int VK_NUMPAD1 = 0x61;

    /**
     * Virtual-key code of the numeric keypad 2 key.
     */
    public static final int VK_NUMPAD2 = 0x62;

    /**
     * Virtual-key code of the numeric keypad 3 key.
     */
    public static final int VK_NUMPAD3 = 0x63;

    /**
     * Virtual-key code of the numeric keypad 4 key.
     */
    public static final int VK_NUMPAD4 = 0x64;

    /**
     * Virtual-key code of the numeric keypad 5 key.
     */
    public static final int VK_NUMPAD5 = 0x65;

    /**
     * Virtual-key code of the numeric keypad 6 key.
     */
    public static final int VK_NUMPAD6 = 0x66;

    /**
     * Virtual-key code of the numeric keypad 7 key.
     */
    public static final int VK_NUMPAD7 = 0x67;

    /**
     * Virtual-key code of the numeric keypad 8 key.
     */
    public static final int VK_NUMPAD8 = 0x68;

    /**
     * Virtual-key code of the numeric keypad 9 key.
     */
    public static final int VK_NUMPAD9 = 0x69;

    /**
     * Virtual-key code of the numeric keypad multiply key.
     */
    public static final int VK_MULTIPLY = 0x6A;

    /**
     * Virtual-key code of the numeric keypad add key.
     */
    public static final int VK_ADD = 0x6B;

    /**
     * Virtual-key code of the numeric keypad separator key.
     */
    public static final int VK_SEPARATOR = 0x6C;

    /**
     * Virtual-key code of the numeric keypad subtract key.
     */
    public static final int VK_SUBTRACT = 0x6D;

    /**
     * Virtual-key code of the numeric keypad decimal key.
     */
    public static final int VK_DECIMAL = 0x6E;

    /**
     * Virtual-key code of the numeric keypad divide key.
     */
    public static final int VK_DIVIDE = 0x6F;

    /**
     * Virtual-key code of the F1 key.
     */
    public static final int VK_F1 = 0x70;

    /**
     * Virtual-key code of the F2 key.
     */
    public static final int VK_F2 = 0x71;

    /**
     * Virtual-key code of the F3 key.
     */
    public static final int VK_F3 = 0x72;

    /**
     * Virtual-key code of the F4 key.
     */
    public static final int VK_F4 = 0x73;

    /**
     * Virtual-key code of the F5 key.
     */
    public static final int VK_F5 = 0x74;

    /**
     * Virtual-key code of the F6 key.
     */
    public static final int VK_F6 = 0x75;

    /**
     * Virtual-key code of the F7 key.
     */
    public static final int VK_F7 = 0x76;

    /**
     * Virtual-key code of the F8 key.
     */
    public static final int VK_F8 = 0x77;

    /**
     * Virtual-key code of the F9 key.
     */
    public static final int VK_F9 = 0x78;

    /**
     * Virtual-key code of the F10 key.
     */
    public static final int VK_F10 = 0x79;

    /**
     * Virtual-key code of the F11 key.
     */
    public static final int VK_F11 = 0x7A;

    /**
     * Virtual-key code of the F12 key.
     */
    public static final int VK_F12 = 0x7B;

    /**
     * Virtual-key code of the F13 key.
     */
    public static final int VK_F13 = 0x7C;

    /**
     * Virtual-key code of the F14 key.
     */
    public static final int VK_F14 = 0x7D;

    /**
     * Virtual-key code of the F15 key.
     */
    public static final int VK_F15 = 0x7E;

    /**
     * Virtual-key code of the F16 key.
     */
    public static final int VK_F16 = 0x7F;

    /**
     * Virtual-key code of the F17 key.
     */
    public static final int VK_F17 = 0x80;

    /**
     * Virtual-key code of the F18 key.
     */
    public static final int VK_F18 = 0x81;

    /**
     * Virtual-key code of the F19 key.
     */
    public static final int VK_F19 = 0x82;

    /**
     * Virtual-key code of the F20 key.
     */
    public static final int VK_F20 = 0x83;

    /**
     * Virtual-key code of the F21 key.
     */
    public static final int VK_F21 = 0x84;

    /**
     * Virtual-key code of the F22 key.
     */
    public static final int VK_F22 = 0x85;

    /**
     * Virtual-key code of the F23 key.
     */
    public static final int VK_F23 = 0x86;

    /**
     * Virtual-key code of the F24 key.
     */
    public static final int VK_F24 = 0x87;

    /**
     * Virtual-key code of the NUM LOCK key.
     */
    public static final int VK_NUMLOCK = 0x90;

    /**
     * Virtual-key code of the SCROLL LOCK key.
     */
    public static final int VK_SCROLL = 0x91;

    /**
     * Virtual-key code of the left SHIFT key.
     */
    public static final int VK_LSHIFT = 0xA0;

    /**
     * Virtual-key code of the right SHIFT key.
     */
    public static final int VK_RSHIFT = 0xA1;

    /**
     * Virtual-key code of the left CONTROL key.
     */
    public static final int VK_LCONTROL = 0xA2;

    /**
     * Virtual-key code of the right CONTROL key.
     */
    public static final int VK_RCONTROL = 0xA3;

    /**
     * Virtual-key code of the left ALT key.
     */
    public static final int VK_LMENU = 0xA4;

    /**
     * Virtual-key code of the right ALT key.
     */
    public static final int VK_RMENU = 0xA5;

    /**
     * Virtual-key code of the Browser Back key.
     */
    public static final int VK_BROWSER_BACK = 0xA6;

    /**
     * Virtual-key code of the Browser Forward key.
     */
    public static final int VK_BROWSER_FORWARD = 0xA7;

    /**
     * Virtual-key code of the Browser Refresh key.
     */
    public static final int VK_BROWSER_REFRESH = 0xA8;

    /**
     * Virtual-key code of the Browser Stop key.
     */
    public static final int VK_BROWSER_STOP = 0xA9;

    /**
     * Virtual-key code of the Browser Search key.
     */
    public static final int VK_BROWSER_SEARCH = 0xAA;

    /**
     * Virtual-key code of the Browser Favorites key.
     */
    public static final int VK_BROWSER_FAVORITES = 0xAB;

    /**
     * Virtual-key code of the Browser Start and Home key.
     */
    public static final int VK_BROWSER_HOME = 0xAC;

    /**
     * Virtual-key code of the Volume Mute key.
     */
    public static final int VK_VOLUME_MUTE = 0xAD;

    /**
     * Virtual-key code of the Volume Down key.
     */
    public static final int VK_VOLUME_DOWN = 0xAE;

    /**
     * Virtual-key code of the Volume Up key.
     */
    public static final int VK_VOLUME_UP = 0xAF;

    /**
     * Virtual-key code of the Next Track key.
     */
    public static final int VK_MEDIA_NEXT_TRACK = 0xB0;

    /**
     * Virtual-key code of the Previous Track key.
     */
    public static final int VK_MEDIA_PREV_TRACK = 0xB1;

    /**
     * Virtual-key code of the Stop Media key.
     */
    public static final int VK_MEDIA_STOP = 0xB2;

    /**
     * Virtual-key code of the Play/Pause Media key.
     */
    public static final int VK_MEDIA_PLAY_PAUSE = 0xB3;

    /**
     * Virtual-key code of the Start Mail key.
     */
    public static final int VK_LAUNCH_MAIL = 0xB4;

    /**
     * Virtual-key code of the Select Media key.
     */
    public static final int VK_LAUNCH_MEDIA_SELECT = 0xB5;

    /**
     * Virtual-key code of the Start Application 1 key.
     */
    public static final int VK_LAUNCH_APP1 = 0xB6;

    /**
     * Virtual-key code of the Start Application 2 key.
     */
    public static final int VK_LAUNCH_APP2 = 0xB7;

    /**
     * Virtual-key code of a key that varies by keyboard, which is the semicolon and colon key on a US standard
     * keyboard.
     */
    public static final int VK_OEM_1 = 0xBA;

    /**
     * Virtual-key code of the plus key in every country or region.
     */
    public static final int VK_OEM_PLUS = 0xBB;

    /**
     * Virtual-key code of the comma key in every country or region.
     */
    public static final int VK_OEM_COMMA = 0xBC;

    /**
     * Virtual-key code of the minus key in every country or region.
     */
    public static final int VK_OEM_MINUS = 0xBD;

    /**
     * Virtual-key code of the period key in every country or region.
     */
    public static final int VK_OEM_PERIOD = 0xBE;

    /**
     * Virtual-key code of a key that varies by keyboard, which is the slash and question mark key on a US standard
     * keyboard.
     */
    public static final int VK_OEM_2 = 0xBF;

    /**
     * Virtual-key code of a key that varies by keyboard, which is the grave accent and tilde key on a US standard
     * keyboard.
     */
    public static final int VK_OEM_3 = 0xC0;

    /**
     * Virtual-key code of a key that varies by keyboard, which is the opening bracket and brace key on a US standard
     * keyboard.
     */
    public static final int VK_OEM_4 = 0xDB;

    /**
     * Virtual-key code of a key that varies by keyboard, which is the backslash and vertical bar key on a US standard
     * keyboard.
     */
    public static final int VK_OEM_5 = 0xDC;

    /**
     * Virtual-key code of a key that varies by keyboard, which is the closing bracket and brace key on a US standard
     * keyboard.
     */
    public static final int VK_OEM_6 = 0xDD;

    /**
     * Virtual-key code of a key that varies by keyboard, which is the single and double quote key on a US standard
     * keyboard.
     */
    public static final int VK_OEM_7 = 0xDE;

    /**
     * Virtual-key code of a key that varies by keyboard.
     */
    public static final int VK_OEM_8 = 0xDF;

    /**
     * Virtual-key code of the angle bracket key or the backslash key on the RT 102-key keyboard.
     */
    public static final int VK_OEM_102 = 0xE2;

    /**
     * Virtual-key code of the IME PROCESS key.
     */
    public static final int VK_PROCESSKEY = 0xE5;

    /**
     * Virtual-key code used to pass Unicode characters as if they were keystrokes.
     */
    public static final int VK_PACKET = 0xE7;

    /**
     * Virtual-key code of the Attn key.
     */
    public static final int VK_ATTN = 0xF6;

    /**
     * Virtual-key code of the CrSel key.
     */
    public static final int VK_CRSEL = 0xF7;

    /**
     * Virtual-key code of the ExSel key.
     */
    public static final int VK_EXSEL = 0xF8;

    /**
     * Virtual-key code of the Erase EOF key.
     */
    public static final int VK_EREOF = 0xF9;

    /**
     * Virtual-key code of the Play key.
     */
    public static final int VK_PLAY = 0xFA;

    /**
     * Virtual-key code of the Zoom key.
     */
    public static final int VK_ZOOM = 0xFB;

    /**
     * Reserved virtual-key code.
     */
    public static final int VK_NONAME = 0xFC;

    /**
     * Virtual-key code of the PA1 key.
     */
    public static final int VK_PA1 = 0xFD;

    /**
     * Virtual-key code of the OEM CLEAR key.
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
