package com.intro.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link GameConfig}.
 *
 * <p>
 *     Each of the four typed getters ({@link GameConfig#getString},
 *     {@link GameConfig#getInt}, {@link GameConfig#getBoolean},
 *     {@link GameConfig#getDouble}) is verified against two contracts:
 * </p>
 * <ul>
 *     <li>
 *         <strong>Key present</strong> — a known key from
 *         {@code config.properties} is read and returned as the correct type.
 *         This also confirms the properties file was loaded successfully.
 *     </li>
 *     <li>
 *         <strong>Key absent</strong> — a guaranteed-absent key returns the
 *         caller-supplied default unchanged.
 *     </li>
 * </ul>
 * <p>
 *     {@link GameConfig#getInt} and {@link GameConfig#getDouble} additionally
 *     verify the malformed-value path: when a present key holds a value that
 *     cannot be parsed as the target type, the getter must return the
 *     caller-supplied default rather than throwing.
 * </p>
 *
 * <p>
 *     {@link GameConfig} uses a {@code static} initialiser and a private
 *     constructor, so property injection is not possible without bytecode
 *     manipulation. Tests therefore probe known keys in {@code config.properties}
 *     (integration-style) and guaranteed-absent keys (unit-style).
 * </p>
 */
class GameConfigTest {

    private static final Logger logger = LogManager.getLogger(GameConfigTest.class);

    /** A key guaranteed not to exist in {@code config.properties}. */
    private static final String ABSENT_KEY = "test.key.that.does.not.exist.xyz";

    // -------------------------------------------------------------------------
    // getString
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link GameConfig#getString} returns the raw string value
     * for a key present in {@code config.properties}.
     */
    @Test
    @DisplayName("getString returns the configured value for a present key")
    void getString_presentKey_returnsConfiguredValue() {
        logger.debug("Testing getString with a present key");
        assertEquals("900", GameConfig.getString("ui.window.default.width", "default"),
                "getString should return '900' from config.properties");
    }

    /**
     * Verifies that {@link GameConfig#getString} returns the caller-supplied
     * default for any key that is absent from {@code config.properties}.
     *
     * @param key an absent key, injected by {@link ValueSource @ValueSource}.
     */
    @ParameterizedTest(name = "getString(\"{0}\", default) returns default")
    @ValueSource(strings = {
            "test.key.that.does.not.exist.xyz",
            "another.missing.key.abc",
            "ui.completely.unknown.setting"
    })
    @DisplayName("getString returns the caller-supplied default for absent keys")
    void getString_absentKey_returnsDefault(String key) {
        logger.debug("Testing getString with absent key: {}", key);
        assertEquals("sentinel", GameConfig.getString(key, "sentinel"),
                "getString should return the default for an absent key");
    }

    // -------------------------------------------------------------------------
    // getInt
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link GameConfig#getInt} correctly parses an integer value
     * for a key present in {@code config.properties}.
     */
    @Test
    @DisplayName("getInt returns the configured integer for a present key")
    void getInt_presentKey_returnsConfiguredValue() {
        logger.debug("Testing getInt with ui.window.default.width");
        assertEquals(900, GameConfig.getInt("ui.window.default.width", 0),
                "getInt should return 900 for ui.window.default.width");
    }

    /**
     * Verifies that {@link GameConfig#getInt} returns the caller-supplied
     * default when the key is absent.
     */
    @Test
    @DisplayName("getInt returns the caller-supplied default for an absent key")
    void getInt_absentKey_returnsDefault() {
        logger.debug("Testing getInt with absent key");
        assertEquals(42, GameConfig.getInt(ABSENT_KEY, 42),
                "getInt should return the default for an absent key");
    }

    /**
     * Verifies that {@link GameConfig#getInt} returns the caller-supplied
     * default when a present key's value cannot be parsed as an integer.
     *
     * <p>
     *     Uses {@code game.debug.enabled=false} as the probe: the key is
     *     present, so the file-loading path runs, but {@code "false"} is not a
     *     valid integer and must trigger the {@link NumberFormatException} fallback.
     * </p>
     */
    @Test
    @DisplayName("getInt returns the caller-supplied default when a value cannot be parsed as int")
    void getInt_malformedValue_returnsDefault() {
        logger.debug("Testing getInt fallback for non-numeric value");
        assertEquals(99, GameConfig.getInt("game.debug.enabled", 99),
                "getInt should return the default when the value is not a valid integer");
    }

    // -------------------------------------------------------------------------
    // getBoolean
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link GameConfig#getBoolean} correctly parses the boolean
     * value for a key present in {@code config.properties}.
     *
     * <p>
     *     The default is deliberately {@code true} so the test confirms the
     *     file value ({@code false}) overrides it rather than the default being
     *     returned.
     * </p>
     */
    @Test
    @DisplayName("getBoolean returns the configured boolean for a present key")
    void getBoolean_presentKey_returnsConfiguredValue() {
        logger.debug("Testing getBoolean with game.debug.enabled");
        assertFalse(GameConfig.getBoolean("game.debug.enabled", true),
                "getBoolean should return false for game.debug.enabled");
    }

    /**
     * Verifies that {@link GameConfig#getBoolean} returns the caller-supplied
     * default when the key is absent.
     */
    @Test
    @DisplayName("getBoolean returns the caller-supplied default for an absent key")
    void getBoolean_absentKey_returnsDefault() {
        logger.debug("Testing getBoolean with absent key");
        assertTrue(GameConfig.getBoolean(ABSENT_KEY, true),
                "getBoolean should return the default for an absent key");
    }

    // -------------------------------------------------------------------------
    // getDouble
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link GameConfig#getDouble} correctly parses a
     * floating-point value for a key present in {@code config.properties}.
     */
    @Test
    @DisplayName("getDouble returns the configured double for a present key")
    void getDouble_presentKey_returnsConfiguredValue() {
        logger.debug("Testing getDouble with ui.window.default.height");
        assertEquals(620.0, GameConfig.getDouble("ui.window.default.height", 0.0),
                "getDouble should return 620.0 for ui.window.default.height");
    }

    /**
     * Verifies that {@link GameConfig#getDouble} returns the caller-supplied
     * default when the key is absent.
     */
    @Test
    @DisplayName("getDouble returns the caller-supplied default for an absent key")
    void getDouble_absentKey_returnsDefault() {
        logger.debug("Testing getDouble with absent key");
        assertEquals(3.14, GameConfig.getDouble(ABSENT_KEY, 3.14),
                "getDouble should return the default for an absent key");
    }

    /**
     * Verifies that {@link GameConfig#getDouble} returns the caller-supplied
     * default when a present key's value cannot be parsed as a number.
     *
     * <p>
     *     Uses {@code game.debug.enabled=false} as the probe: the key is
     *     present, so the file-loading path runs, but {@code "false"} is not a
     *     valid double and must trigger the {@link NumberFormatException} fallback.
     * </p>
     */
    @Test
    @DisplayName("getDouble returns the caller-supplied default when a value cannot be parsed as double")
    void getDouble_malformedValue_returnsDefault() {
        logger.debug("Testing getDouble fallback for non-numeric value");
        assertEquals(9.9, GameConfig.getDouble("game.debug.enabled", 9.9),
                "getDouble should return the default when the value is not a valid double");
    }
}
