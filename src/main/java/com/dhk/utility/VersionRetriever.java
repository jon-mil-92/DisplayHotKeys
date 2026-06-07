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
package com.dhk.utility;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Retrieves the current version for the application.
 *
 * @author Jonathan R. Miller
 */
public class VersionRetriever {

    /**
     * Default constructor for the {@link VersionRetriever} class.
     */
    public VersionRetriever() {
    }

    /**
     * Gets the current version of the app from the app properties resource.
     *
     * @return The current version of the application
     */
    public static String getVersion() {
        Properties props = new Properties();

        try (InputStream in = VersionRetriever.class.getResourceAsStream("/app.properties")) {
            if (in == null) {
                return "unknown";
            }

            props.load(in);

            return props.getProperty("app.version", "unknown");
        } catch (IOException ex) {
            return "unknown";
        }
    }

}
