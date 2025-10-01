package utils;

import io.github.cdimascio.dotenv.Dotenv;

public final class Env {
    private static final Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing() // non esplode se manca il file .env
            .load();

    private Env() {
        // utility class, no instances
    }

    /**
     * Get a required environment variable as String.
     * First tries System.getenv(), then falls back to .env if present.
     * Throws IllegalStateException if not found.
     */
    public static String getRequired(String key) {
        String value = System.getenv(key);
        if (value == null || value.isEmpty()) {
            value = dotenv.get(key);
        }
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException("Missing required environment variable: " + key);
        }
        return value;
    }

    /**
     * Get an optional environment variable as String.
     * Returns defaultValue if not found.
     */
    public static String getOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isEmpty()) {
            value = dotenv.get(key);
        }
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    /**
     * Get a required environment variable as Long.
     * Throws IllegalStateException if missing or not a valid number.
     */
    public static Long getRequiredLong(String key) {
        String val = getRequired(key);
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Environment variable " + key + " must be a number, got: " + val, e);
        }
    }

    /**
     * Get an optional environment variable as Long.
     * Returns defaultValue if not found or invalid.
     */
    public static Long getOrDefaultLong(String key, Long defaultValue) {
        String val = System.getenv(key);
        if (val == null || val.isEmpty()) {
            val = dotenv.get(key);
        }
        if (val == null || val.isEmpty()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
