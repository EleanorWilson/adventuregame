package com.intro.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.Properties;

/**
 * Centralised accessor for game settings loaded from {@code config.properties}.
 *
 * <p>
 *     Properties are loaded once at class initialisation. Values are accessed
 *     through four typed getters — {@link #getString}, {@link #getInt},
 *     {@link #getBoolean}, and {@link #getDouble} — each accepting a
 *     caller-supplied default that is returned when the key is absent or its
 *     value cannot be parsed as the requested type.
 * </p>
 *
 * <p>
 *     This class manages <em>settings</em>: numeric limits, feature flags, and
 *     tunable parameters. Player-visible display strings (window title, button
 *     labels, prompt text) belong in {@code messages.properties} instead, which
 *     is loaded as a {@link java.util.ResourceBundle} by
 *     {@link com.intro.ui.GameWindow} and consumed by FXML's {@code %key} syntax.
 * </p>
 *
 * <p>
 *     Adding a new property requires only a key/value pair in
 *     {@code config.properties}; no change to this class is ever needed.
 * </p>
 */
public class GameConfig {

    private static final Logger logger = LogManager.getLogger(GameConfig.class);

    private static final Properties properties = new Properties();
    private static final String CONFIG_FILE = "config.properties";

    static {
        try (InputStream input = GameConfig.class.getResourceAsStream("/" + CONFIG_FILE)) {
            if (input == null) {
                logger.warn("'{}' not found on classpath — all settings will use built-in defaults.",
                        CONFIG_FILE);
            } else {
                properties.load(input);
                logger.info("Loaded {} setting(s) from '{}'.", properties.size(), CONFIG_FILE);
            }
        } catch (Exception e) {
            logger.error("Failed to load '{}': {}", CONFIG_FILE, e.getMessage());
        }
    }

    private GameConfig() {}

    // -------------------------------------------------------------------------
    // Generic typed getters
    // -------------------------------------------------------------------------

    /**
     * Returns the value of {@code key} as a {@link String}, or
     * {@code defaultValue} if the key is not present.
     *
     * <p>
     *     No try-catch is needed here: {@link Properties#getProperty} stores and
     *     returns all values as {@link String} natively, so there is no type
     *     conversion step that can fail. This distinguishes {@code getString}
     *     from {@link #getInt} and {@link #getDouble}, which parse the raw
     *     string and must catch {@link NumberFormatException}.
     * </p>
     *
     * @param key          the property key; must not be {@code null}.
     * @param defaultValue returned when the key is absent.
     * @return the configured value, or {@code defaultValue}.
     */
    public static String getString(String key, String defaultValue) {
        String value = properties.getProperty(key, defaultValue);
        logger.debug("getString('{}') = '{}'", key, value);
        return value;
    }

    /**
     * Returns the value of {@code key} parsed as an {@code int}, or
     * {@code defaultValue} if the key is absent or its value is not a valid
     * integer.
     *
     * <p>
     *     {@link Integer#parseInt} throws {@link NumberFormatException} for
     *     any non-numeric string. When that happens, a warning is logged and
     *     {@code defaultValue} is returned so the application never crashes
     *     due to a misconfigured property.
     * </p>
     *
     * @param key          the property key; must not be {@code null}.
     * @param defaultValue returned when the key is absent or unparseable.
     * @return the configured value, or {@code defaultValue}.
     */
    public static int getInt(String key, int defaultValue) {
        String raw = properties.getProperty(key);
        if (raw == null) {
            logger.debug("getInt('{}') — key absent, using default: {}", key, defaultValue);
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            logger.debug("getInt('{}') = {}", key, value);
            return value;
        } catch (NumberFormatException e) {
            logger.warn("getInt('{}') — '{}' is not a valid integer, using default: {}",
                    key, raw.trim(), defaultValue);
            return defaultValue;
        }
    }

    /**
     * Returns the value of {@code key} parsed as a {@code boolean}, or
     * {@code defaultValue} if the key is absent.
     *
     * <p>
     *     No try-catch is needed here: {@link Boolean#parseBoolean} never
     *     throws — it returns {@code false} for any string that is not the
     *     exact case-insensitive value {@code "true"} (including {@code "yes"},
     *     {@code "1"}, and malformed values). This distinguishes
     *     {@code getBoolean} from {@link #getInt} and {@link #getDouble}.
     * </p>
     *
     * @param key          the property key; must not be {@code null}.
     * @param defaultValue returned when the key is absent.
     * @return the configured value, or {@code defaultValue}.
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        String raw = properties.getProperty(key);
        if (raw == null) {
            logger.debug("getBoolean('{}') — key absent, using default: {}", key, defaultValue);
            return defaultValue;
        }
        boolean value = Boolean.parseBoolean(raw.trim());
        logger.debug("getBoolean('{}') = {}", key, value);
        return value;
    }

    /**
     * Returns the value of {@code key} parsed as a {@code double}, or
     * {@code defaultValue} if the key is absent or its value is not a valid
     * floating-point number.
     *
     * <p>
     *     {@link Double#parseDouble} throws {@link NumberFormatException} for
     *     any non-numeric string. When that happens, a warning is logged and
     *     {@code defaultValue} is returned so the application never crashes
     *     due to a misconfigured property.
     * </p>
     *
     * @param key          the property key; must not be {@code null}.
     * @param defaultValue returned when the key is absent or unparseable.
     * @return the configured value, or {@code defaultValue}.
     */
    public static double getDouble(String key, double defaultValue) {
        String raw = properties.getProperty(key);
        if (raw == null) {
            logger.debug("getDouble('{}') — key absent, using default: {}", key, defaultValue);
            return defaultValue;
        }
        try {
            double value = Double.parseDouble(raw.trim());
            logger.debug("getDouble('{}') = {}", key, value);
            return value;
        } catch (NumberFormatException e) {
            logger.warn("getDouble('{}') — '{}' is not a valid number, using default: {}",
                    key, raw.trim(), defaultValue);
            return defaultValue;
        }
    }
}
