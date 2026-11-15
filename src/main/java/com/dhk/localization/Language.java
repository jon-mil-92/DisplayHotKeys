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

import java.util.Locale;

/**
 * Lists the languages the application is translated into, in the order the language selector shows them. Each language
 * names the Windows font that draws its script, since the Chinese, Japanese, and Korean scripts each need their own
 * font.
 *
 * @author Jonathan R. Miller
 */
public enum Language {

    /**
     * German, drawn in the Windows user interface font.
     */
    GERMAN("de", "Deutsch", "Segoe UI"),

    /**
     * English, which is also the base language every other language falls back to for a missing message.
     */
    ENGLISH("en", "English", "Segoe UI"),

    /**
     * Spanish, drawn in the Windows user interface font.
     */
    SPANISH("es", "Español", "Segoe UI"),

    /**
     * French, drawn in the Windows user interface font.
     */
    FRENCH("fr", "Français", "Segoe UI"),

    /**
     * Italian, drawn in the Windows user interface font.
     */
    ITALIAN("it", "Italiano", "Segoe UI"),

    /**
     * Brazilian Portuguese, listed simply as Português because it also serves every other Portuguese locale.
     */
    PORTUGUESE_BRAZIL("pt-BR", "Português", "Segoe UI"),

    /**
     * Russian, drawn in the Windows user interface font, which covers the Cyrillic script.
     */
    RUSSIAN("ru", "Русский", "Segoe UI"),

    /**
     * Japanese, drawn in the Windows Japanese user interface font so Kanji take their Japanese shapes.
     */
    JAPANESE("ja", "日本語", "Yu Gothic UI"),

    /**
     * Korean, drawn in the Windows Korean user interface font.
     */
    KOREAN("ko", "한국어", "Malgun Gothic"),

    /**
     * Simplified Chinese, drawn in the Windows Simplified Chinese user interface font so Hanzi take their Chinese
     * shapes.
     */
    CHINESE_SIMPLIFIED("zh-CN", "简体中文", "Microsoft YaHei UI");

    private String tag;
    private Locale locale;
    private String nativeName;
    private String fontFamily;

    /**
     * Constructor for the {@link Language} enum.
     *
     * @param tag
     *            - The IETF language tag that names the language in the settings file and the installer
     * @param nativeName
     *            - The name of the language written in that language
     * @param fontFamily
     *            - The family of the installed Windows font that draws the language's script
     */
    Language(String tag, String nativeName, String fontFamily) {
        this.tag = tag;
        this.locale = Locale.forLanguageTag(tag);
        this.nativeName = nativeName;
        this.fontFamily = fontFamily;
    }

    /**
     * Gets the IETF language tag that names the language in the settings file and the installer.
     *
     * @return The language tag
     */
    public String getTag() {
        return tag;
    }

    /**
     * Gets the locale whose messages the language is shown in.
     *
     * @return The locale of the language
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Gets the family of the installed Windows font that draws the language's script.
     *
     * @return The font family of the language
     */
    public String getFontFamily() {
        return fontFamily;
    }

    /**
     * Gets the name of the language written in that language, so every reader can find their own language in the
     * language selector whatever language the application is showing.
     *
     * @return The name of the language written in that language
     */
    @Override
    public String toString() {
        return nativeName;
    }

    /**
     * Finds the language with the given tag.
     *
     * @param tag
     *            - The IETF language tag to look up, which may be null
     *
     * @return The language with the given tag, or null when no language has it
     */
    public static Language fromTag(String tag) {
        for (Language language : values()) {
            if (language.tag.equalsIgnoreCase(tag)) {
                return language;
            }
        }

        return null;
    }

    /**
     * Finds the language that best serves a reader of the given locale, falling back to English when the application is
     * not translated into the locale's language.
     *
     * @param locale
     *            - The locale of the reader, such as the Windows display language
     *
     * @return The language that best serves the locale
     */
    public static Language fromLocale(Locale locale) {
        String languageCode = locale.getLanguage();
        String country = locale.getCountry();

        // Readers of the traditional script are not served by the simplified one, so they are left to English
        if (languageCode.equals("zh")) {
            boolean traditionalScript = locale.getScript().equals("Hant") || country.equals("TW")
                    || country.equals("HK") || country.equals("MO");

            return traditionalScript ? ENGLISH : CHINESE_SIMPLIFIED;
        }

        for (Language language : values()) {
            if (language.locale.getLanguage().equals(languageCode)) {
                return language;
            }
        }

        return ENGLISH;
    }

}
