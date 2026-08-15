package com.periut.retroapi.text;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Looks up a translation key, from whichever source can actually answer.
 *
 * <p>Beta's translation table lives in {@code I18n}, a client-only class backed by a lang file that
 * ships in the client jar and not the server one. Calling it from common code works in development
 * and then throws {@link NoClassDefFoundError} on a dedicated server, which is why nothing here
 * depends on it being there. Three sources are tried in order:
 *
 * <ol>
 *   <li>the game's own table, if a client installed one via {@link #setResolver} - this is the only
 *       source that honours the player's chosen language;</li>
 *   <li>{@code lang/en_US.lang} read straight off the classpath, which covers a client whose table
 *       has not been built yet and any environment that happens to ship the file;</li>
 *   <li>nothing - the caller is told so and falls back to a name it can derive itself.</li>
 * </ol>
 *
 * <p>{@link #find} returns null rather than echoing the key back, so callers can tell a real
 * translation from a missing one; beta's own lookup returns the key and hides the difference.
 */
public final class Translations {
    private static final String BUNDLED_PATH = "/lang/en_US.lang";

    private static Function<String, String> resolver = key -> null;
    private static Map<String, String> bundled;

    private Translations() {
    }

    /**
     * Points lookups at the game's translation table.
     *
     * @param resolver returns the translation, or null when the key is unknown
     */
    public static void setResolver(final Function<String, String> resolver) {
        Translations.resolver = resolver == null ? key -> null : resolver;
    }

    /** @return the translation, or null if no source knows this key */
    public static String find(final String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }

        try {
            final String resolved = resolver.apply(key);
            if (resolved != null && !resolved.isEmpty() && !resolved.equals(key)) {
                return resolved;
            }
        } catch (final RuntimeException | LinkageError ignored) {
            // A resolver that blows up is a resolver that does not know the key.
        }

        final String fromFile = bundled().get(key);
        return fromFile == null || fromFile.isEmpty() ? null : fromFile;
    }

    public static String get(final String key, final String fallback) {
        final String found = find(key);
        return found == null ? fallback : found;
    }

    /** True when some source could answer at all; useful for diagnostics, not for correctness. */
    public static boolean hasTable() {
        return !bundled().isEmpty() || resolver.apply("") != null;
    }

    private static synchronized Map<String, String> bundled() {
        if (bundled != null) {
            return bundled;
        }

        bundled = new HashMap<>();
        try (InputStream stream = Translations.class.getResourceAsStream(BUNDLED_PATH)) {
            if (stream != null) {
                read(stream);
            }
        } catch (final Exception | LinkageError ignored) {
            // No lang file on this side; the derived names take over.
        }
        return bundled;
    }

    private static void read(final InputStream stream) throws java.io.IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                final int separator = line.indexOf('=');
                if (separator > 0) {
                    bundled.put(line.substring(0, separator).trim(), line.substring(separator + 1).trim());
                }
            }
        }
    }
}
