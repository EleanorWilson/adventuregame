package com.intro.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.Properties;


/**
 * Handles configuration loading and property management for the game.
 *<p>
 *     This class acts as a centralised config manager that reads settings from the
 *     {@code config.properties} file. Properties are loaded when the class is first
 *     initialised, ensuring that configuration data is instantly available to other
 *     components.
 *</p>
 * <p>
 *     Class contains a static initialisation block responsible for loading, opening
 *     and parsing the configuration file. This will initialise static field when the
 *     class is first loaded into memory.
 * </p>
 * <h2>Adding new Properties</h2>
 * <p>
 *     Add the key/value pair to {@code config.properties}. No change to this class is required.
 *     Call the appropriate getter in the class using the property, for example {@link #getInt(String, int)}
 *     and {@link #getBoolean(String, boolean)}:
 * </p>
 * <pre>{@code
 * int maxHP = GameConfig.getInt("game.player.max.health", 100);
 * boolean debugMode = GameConfig.getBoolean("game.debug.enabled", false);
 * }</pre>
 * <p>
 *     The default value in each call should provide a safe fallback if the properties file is absent
 *     or the key is later removed from {@code config.properties}.
 * </p>
 * <h2>Error Handling</h2>
 * <p>
 *     If the properties file is missing, a warning is logged and getters return the caller defaults. If
 *     a value is present but cannot be parsed properly (e.g. {@link #getInt} called on String), a warning
 *     is logged and the default is returned. This ensures the game never crashes due to a missing key.
 * </p>
 * <h2>{@code config.properties} vs {@code messages.properties}</h2>
 * <p>
 *     Config.properties file is for <em>settings</em> like default window sizes, etc and is
 *     handled by this {@code GameConfig} class. Whereas text displayed to the player on the
 *     UI is stored in {@code messages.properties}, which is loaded as a
 *     {@link java.util.ResourceBundle ResourceBundle} by
 *     {@link com.intro.ui.GameWindow GameWindow} and read by FXML {@code %key} syntax.
 * </p>
 * @see java.util.Properties
 * @see java.util.ResourceBundle
 */
@SuppressWarnings("unused")
public class GameConfig {

    private static final Logger logger = LogManager.getLogger(GameConfig.class);

    /**
     * Internal key-value repository for storing configuration properties.
     */
    private static final Properties properties = new Properties();

    /**
     * The target filename of the configuration properties file.
     */
    private static final String CONFIG_FILE = "config.properties";

    static {
        try (InputStream input = GameConfig.class.getResourceAsStream("/" + CONFIG_FILE)) {
            if (input == null) {
                logger.warn("Unable to find '{}' on the classpath. Using default configurations.", CONFIG_FILE);
            } else {
                properties.load(input);
                logger.info("Successfully loaded {} settings configuration from '{}'", properties.size(), CONFIG_FILE);
            }
        } catch (Exception e) {
            logger.error("Critical exception occurred while loading configuration file: {}", CONFIG_FILE, e);
        }
    }

    /**
     * Private constructor to prevent instantiation. This class is designed to be shared and reusable,
     * multiple copies of the class is not necessary.
     */
    private GameConfig(){}

    // -----------------------------------------------------
    // Methods for retrieving values from config.properties
    // -----------------------------------------------------

    /**
     * Returns value of {@code key} as a {@link String}, or {@code defaultValue} if the key is not present.
     * @param key the property key; must not be {@code null}
     * @param defaultValue returned when the key is absent.
     * @return the properties value, or the {@code defaultValue}
     */
    public static String getString(String key, String defaultValue) {
        String value = properties.getProperty(key, defaultValue);
        logger.debug("getString('{}') = '{}'", key, value);
        return value;
    }

    /**
     * Returns value of {@code key} as a {@link Integer int}, or {@code defaultValue} if the key is not present, or
     * is an invalid integer.
     * <p>
     *     Throws {@link NumberFormatException} for any non-numeric string. Warning is logged and the default
     *     value is returned.
     * </p>
     * @param key the property key; must not be {@code null}
     * @param defaultValue returned when the key is absent or cannot be parsed.
     * @return the properties value, or the {@code defaultValue}
     */
    public static int getInt(String key, int defaultValue) {
        String raw = properties.getProperty(key);
        if (raw == null) {
            logger.debug("getInt('{}') key absent, using default: {}", key, defaultValue);
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            logger.debug("getInt('{}') = {}", key, value);
            return value;
        } catch (NumberFormatException e) {
            logger.warn("getInt('{}'), '{}' is not a valid integer, using default: {}", key, raw.trim(), defaultValue);
            return defaultValue;
        }
    }

    /**
     * Returns value of {@code key} as a {@link Boolean boolean}, or {@code defaultValue} if the key is not present.
     * @param key the property key; must not be {@code null}
     * @param defaultValue returned when the key is absent.
     * @return the properties value, or the {@code defaultValue}
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        String raw = properties.getProperty(key);
        if (raw == null) {
            logger.debug("getBoolean('{}') key absent, using default: {}", key, defaultValue);
            return defaultValue;
        }
        boolean value = Boolean.parseBoolean(raw.trim());
        logger.debug("getBoolean('{}') = {}", key, value);
        return value;
    }

    /**
     * Returns value of {@code key} as a {@link Double double}, or {@code defaultValue} if the key is not present, or
     * is an invalid double.
     * <p>
     *     Throws {@link NumberFormatException} for any non-numeric string. Warning is logged and the default
     *     value is returned.
     * </p>
     * @param key the property key; must not be {@code null}
     * @param defaultValue returned when the key is absent or cannot be parsed.
     * @return the properties value, or the {@code defaultValue}
     */
    public static double getDouble(String key, double defaultValue) {
        String raw = properties.getProperty(key);
        if (raw == null) {
            logger.debug("getDouble('{}') key absent, using default: {}", key, defaultValue);
            return defaultValue;
        }
        try {
            double value = Double.parseDouble(raw.trim());
            logger.debug("getDouble('{}') = {}", key, value);
            return value;
        } catch (NumberFormatException e) {
            logger.warn("getDouble('{}'), '{}' is not a valid double, using default: {}", key, raw.trim(), defaultValue);
            return defaultValue;
        }
    }

}
