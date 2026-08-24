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

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Retrieves the current version for the application and the latest released version from GitHub.
 *
 * @author Jonathan R. Miller
 */
public class VersionRetriever {

    /**
     * Sentinel returned when a version cannot be resolved.
     */
    private static final String UNKNOWN_VERSION = "Unknown";

    /**
     * GitHub REST endpoint that returns the latest release for the DisplayHotKeys repository.
     */
    private static final URI LATEST_RELEASE_URI = URI
            .create("https://api.github.com/repos/jon-mil-92/DisplayHotKeys/releases/latest");

    /**
     * Timeout applied to both establishing the connection and completing the request. The connect and request timeouts
     * compose, so this is kept short enough that an unreachable endpoint still resolves the label quickly.
     */
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Client reused across lookups, since building one per call orphans its selector thread and connection pool.
     */
    private static HttpClient httpClient;

    /**
     * Lifetime of a cached version. Unauthenticated GitHub API requests are limited to 60 per hour per address, so
     * repeat lookups are served from the cache rather than spending that budget.
     */
    private static final long CACHE_TTL_NANOS = Duration.ofMinutes(5).toNanos();

    /**
     * Last successfully resolved version, or null when nothing has resolved yet.
     */
    private static volatile String cachedLatestVersion;

    /**
     * Monotonic timestamp of the cached version, immune to the wall clock jumping under a time zone or NTP change.
     */
    private static volatile long cachedAtNanos;

    /**
     * Extracts the untrusted tag_name value from the release JSON, bounded to a safe length.
     */
    private static final Pattern TAG_NAME_PATTERN = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"\\\\]{1,32})\"");

    /**
     * Strict allow-list for a valid version tag, guaranteeing the returned value is a sanitized "vMAJOR.MINOR.PATCH".
     */
    private static final Pattern VERSION_PATTERN = Pattern.compile("v\\d{1,4}\\.\\d{1,4}\\.\\d{1,4}");

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
                return UNKNOWN_VERSION;
            }

            props.load(in);

            return props.getProperty("app.version", UNKNOWN_VERSION);
        } catch (Throwable t) {
            return UNKNOWN_VERSION;
        }
    }

    /**
     * Gets the latest released version from the GitHub API, reusing a recently resolved result. Every failure resolves
     * to the unknown sentinel rather than throwing, so a caller never needs to guard the call. A lookup that reaches
     * the network blocks, so this must not run on the AWT event dispatching thread.
     *
     * @return The latest released version, or the unknown sentinel if it cannot be resolved
     */
    public static String getLatestVersion() {
        String cachedVersion = cachedLatestVersion;

        if (cachedVersion != null && System.nanoTime() - cachedAtNanos < CACHE_TTL_NANOS) {
            return cachedVersion;
        }

        // GitHub requires a User-Agent; the versioned Accept header pins a stable response shape
        HttpRequest request = HttpRequest.newBuilder(LATEST_RELEASE_URI).timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/vnd.github+json").header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "DisplayHotKeys").GET().build();

        try {
            HttpResponse<String> response = httpClient().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return UNKNOWN_VERSION;
            }

            String latestVersion = sanitizeVersion(extractTagName(response.body()));

            // Only a resolved version is cached, so a transient failure is retried on the next lookup
            if (!UNKNOWN_VERSION.equals(latestVersion)) {
                cachedAtNanos = System.nanoTime();
                cachedLatestVersion = latestVersion;
            }

            return latestVersion;
        } catch (Throwable t) {
            // Sending clears the interrupt flag when it throws, so restore it rather than swallowing the interrupt
            if (t instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }

            return UNKNOWN_VERSION;
        }
    }

    /**
     * Gets the shared client, creating it on first use so no connection resources are established until a lookup runs.
     *
     * @return The client used for every version lookup
     */
    private static synchronized HttpClient httpClient() {
        if (httpClient == null) {
            httpClient = HttpClient.newBuilder().connectTimeout(REQUEST_TIMEOUT)
                    .followRedirects(HttpClient.Redirect.NORMAL).build();
        }

        return httpClient;
    }

    /**
     * Extracts the raw, still-untrusted tag_name value from the release JSON without a JSON parser dependency.
     *
     * @param responseBody
     *            - The GitHub release JSON response body
     *
     * @return The captured tag_name value, or the unknown sentinel if none is present
     */
    private static String extractTagName(String responseBody) {
        Matcher matcher = TAG_NAME_PATTERN.matcher(responseBody);
        return matcher.find() ? matcher.group(1) : UNKNOWN_VERSION;
    }

    /**
     * Sanitizes an untrusted version string by admitting it only if it matches the strict "vMAJOR.MINOR.PATCH" form,
     * then strips the leading "v" so the result matches the app properties version format.
     *
     * @param version
     *            - The untrusted version string to validate
     *
     * @return The well-formed version without its leading "v", otherwise the unknown sentinel
     */
    private static String sanitizeVersion(String version) {
        return VERSION_PATTERN.matcher(version).matches() ? version.substring(1) : UNKNOWN_VERSION;
    }

}
