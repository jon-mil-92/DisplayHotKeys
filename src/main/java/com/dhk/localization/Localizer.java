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
package com.dhk.localization;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import com.formdev.flatlaf.FlatLaf;

/**
 * Supplies the application's text in the selected language. A language's messages are loaded once when it is selected,
 * so every lookup afterwards is a single map read that adds nothing noticeable to rebuilding the view.
 *
 * @author Jonathan R. Miller
 */
public class Localizer {

    /**
     * Language whose text is supplied, which is null only until the language is selected at launch.
     */
    private static Language selectedLanguage;

    /**
     * Messages of the selected language, keyed by the names the application looks its text up by.
     */
    private static ResourceBundle messages;

    /**
     * Base name of the message bundles, where the English messages are the base and each other language has its own.
     */
    private static final String MESSAGES_BASE_NAME = "com.dhk.localization.Messages";

    /**
     * Bundle lookup that never consults the Windows language, so a language only ever falls back to the English base.
     */
    private static final ResourceBundle.Control MESSAGES_CONTROL = ResourceBundle.Control
            .getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES);

    /**
     * Default constructor for the {@link Localizer} class.
     */
    public Localizer() {
    }

    /**
     * Selects the language the application's text is supplied in, along with the font that draws its script. The font
     * takes effect the next time the theme is set up, which every rebuild of the application does.
     *
     * @param language
     *            - The language to select
     */
    public static void setLanguage(Language language) {
        if (language == selectedLanguage) {
            return;
        }

        messages = ResourceBundle.getBundle(MESSAGES_BASE_NAME, language.getLocale(), MESSAGES_CONTROL);
        selectedLanguage = language;

        FlatLaf.setPreferredFontFamily(language.getFontFamily());
    }

    /**
     * Gets the language the application's text is supplied in.
     *
     * @return The selected language
     */
    public static Language getLanguage() {
        return selectedLanguage;
    }

    /**
     * Gets a message in the selected language.
     *
     * @param key
     *            - The key of the message
     *
     * @return The message in the selected language
     */
    public static String getText(String key) {
        return messages.getString(key);
    }

    /**
     * Gets the messages of the given keys in the selected language.
     *
     * @param keys
     *            - The keys of the messages
     *
     * @return The messages in the selected language, in the same order as their keys
     */
    public static String[] getTexts(String[] keys) {
        String[] texts = new String[keys.length];

        for (int keyIndex = 0; keyIndex < keys.length; keyIndex++) {
            texts[keyIndex] = messages.getString(keys[keyIndex]);
        }

        return texts;
    }

    /**
     * Gets a message in the selected language with its numbered placeholders filled in, where {0} takes the first
     * argument.
     *
     * @param key
     *            - The key of the message
     * @param arguments
     *            - The values to fill the message's placeholders with, in placeholder order
     *
     * @return The message in the selected language with its placeholders filled in
     */
    public static String format(String key, Object... arguments) {
        return new MessageFormat(messages.getString(key), selectedLanguage.getLocale()).format(arguments);
    }

}
