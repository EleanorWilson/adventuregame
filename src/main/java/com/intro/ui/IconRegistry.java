package com.intro.ui;

import com.intro.config.GameConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.InputStream;
import java.util.Properties;

/**
 * Accessor for {@code SVGPath} 'd' content loaded from {@code com/intro/ui/icons.properties}.
 * <p>
 *     Some icons, for example the moon and sun, will change at runtime depending on which mode
 *     is selected.
 * </p>
 * <p>
 *     Similarly to the {@link GameConfig} class, the icon properties are loaded once at class
 *     initialisation. The getPath caller-supplied default is returned when the key is absent, so
 *     that a missing or misspelled icon key does not cause the application to crash.
 * </p>
 * <p>
 *     Additional icons added to {@code icons.properties} should not require any changes to this
 *     class, only to the class(es) that call(s) the {@link #getPath(String, String)} method.
 * </p>
 * @see GameConfig
 */
public class IconRegistry {

    private static final Logger logger = LogManager.getLogger(IconRegistry.class);

    private static final Properties properties = new Properties();

    private static final String ICONS_FILE = "/com/intro/ui/icons.properties";

    static {
        try (InputStream input = IconRegistry.class.getResourceAsStream(ICONS_FILE)) {
            if (input == null) {
                logger.warn("Unable to find '{}' on the classpath. Using default icons.", ICONS_FILE);
            } else {
                properties.load(input);
                logger.info("Successfully loaded {} icons from '{}'", properties.size(), ICONS_FILE);
            }
        } catch (Exception e) {
            logger.error("Failed to load '{}': {}", ICONS_FILE, e);
        }
    }

    private IconRegistry() {}

    /**
     * Returns value of {@code key} as a {@link String}, or {@code defaultValue} if the key is not present.
     * @param key the property key; must not be {@code null}
     * @param defaultValue returned when the key is absent.
     * @return the path data, or the {@code defaultValue}
     */
    public static String getPath(String key, String defaultValue) {
        String value = properties.getProperty(key, defaultValue);
        logger.debug("getPath('{}') = '{}'", key, value);
        return value;
    }

}
