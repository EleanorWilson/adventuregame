package com.intro.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.Properties;

/**
 * Centralised accessor for {@code SVGPath} "d" content loaded from
 * {@code com/intro/ui/icons.properties}.
 *
 * <p>
 *     Some icons — such as the theme-toggle button's moon/sun glyph — must
 *     change at runtime, based on application state. They cannot be inlined
 *     as static {@code SVGPath} content in {@code GamePanel.fxml} the way
 *     the minimise/maximise/close icons are, because FXML has no concept of
 *     "pick one of these two path strings depending on a boolean field".
 * </p>
 *
 * <p>
 *     Rather than hard-coding those path strings as {@code String} literals
 *     inside controller classes, they are kept in
 *     {@code icons.properties} and loaded here, mirroring the existing
 *     {@link com.intro.config.GameConfig} pattern used for game settings.
 *     This keeps visual/vector data out of Java source files, so a designer
 *     can update or replace an icon glyph by editing a properties file —
 *     no recompilation, and no risk of introducing a Java syntax error while
 *     hand-editing a long SVG path string.
 * </p>
 *
 * <p>
 *     Properties are loaded once at class initialisation. The single typed
 *     getter, {@link #getPath}, accepts a caller-supplied default that is
 *     returned when the key is absent, so a missing or misspelled icon key
 *     degrades gracefully (an empty/invalid glyph) rather than crashing the
 *     application.
 * </p>
 *
 * <p>
 *     Adding a new runtime-swappable icon requires only a new key/value pair
 *     in {@code icons.properties}; no change to this class is ever needed.
 * </p>
 */
public class IconRegistry {

    private static final Logger logger = LogManager.getLogger(IconRegistry.class);

    private static final Properties properties = new Properties();
    private static final String ICONS_FILE = "/com/intro/ui/icons.properties";

    static {
        try (InputStream input = IconRegistry.class.getResourceAsStream(ICONS_FILE)) {
            if (input == null) {
                logger.warn("'{}' not found on classpath — all icon lookups will use built-in defaults.",
                        ICONS_FILE);
            } else {
                properties.load(input);
                logger.info("Loaded {} icon(s) from '{}'.", properties.size(), ICONS_FILE);
            }
        } catch (Exception e) {
            logger.error("Failed to load '{}': {}", ICONS_FILE, e.getMessage());
        }
    }

    private IconRegistry() {}

    /**
     * Returns the SVGPath "d" content configured for {@code key}, or
     * {@code defaultValue} if the key is not present in
     * {@code icons.properties}.
     *
     * <p>
     *     No try-catch is needed here: {@link Properties#getProperty} stores
     *     and returns all values as {@link String} natively, so there is no
     *     type-conversion step that can fail.
     * </p>
     *
     * @param key          the property key; must not be {@code null}.
     * @param defaultValue returned when the key is absent.
     * @return the configured path data, or {@code defaultValue}.
     */
    public static String getPath(String key, String defaultValue) {
        String value = properties.getProperty(key, defaultValue);
        logger.debug("getPath('{}') = '{}'", key, value);
        return value;
    }
}
