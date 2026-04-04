package com.dhk.utility;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Retrieves the current version for the application.
 * 
 * @author Jonathan R. Miller
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright © 2026 Jonathan R. Miller
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
