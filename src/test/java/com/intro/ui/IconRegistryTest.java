package com.intro.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link IconRegistry}.
 *
 * <p>
 *     {@link IconRegistry#getPath} is verified against two contracts:
 * </p>
 * <ul>
 *     <li>
 *         <strong>Key present</strong> — a known key from
 *         {@code icons.properties} is read and returned verbatim, which also
 *         confirms the properties file was found and loaded successfully.
 *     </li>
 *     <li>
 *         <strong>Key absent</strong> — a guaranteed-absent key returns the
 *         caller-supplied default unchanged.
 *     </li>
 * </ul>
 * <p>
 *     {@link IconRegistry} uses a {@code static} initialiser and a private
 *     constructor, so property injection is not possible without bytecode
 *     manipulation. Tests therefore probe the real {@code icons.properties}
 *     file (integration-style) and guaranteed-absent keys (unit-style),
 *     mirroring the existing convention in {@code GameConfigTest}.
 * </p>
 */
class IconRegistryTest {

    private static final Logger logger = LogManager.getLogger(IconRegistryTest.class);

    /** A key guaranteed not to exist in {@code icons.properties}. */
    private static final String ABSENT_KEY = "test.icon.that.does.not.exist.xyz";

    /**
     * Verifies that {@link IconRegistry#getPath} returns the configured
     * SVGPath "d" content for a key present in {@code icons.properties},
     * confirming the file was loaded and this specific icon key resolves to
     * non-blank path data (rather than merely falling back to the default).
     */
    @Test
    @DisplayName("getPath returns the configured value for the moon icon key")
    void getPath_presentKey_returnsConfiguredValue() {
        logger.debug("Testing getPath with ui.icon.theme.moon");
        String result = IconRegistry.getPath("ui.icon.theme.moon", "SHOULD_NOT_BE_RETURNED");

        assertNotEquals("SHOULD_NOT_BE_RETURNED", result,
                "getPath should resolve ui.icon.theme.moon from icons.properties, not fall back");
        assertFalse(result.isBlank(), "The moon icon's path data should not be blank");
    }

    /**
     * Verifies that {@link IconRegistry#getPath} returns the configured
     * SVGPath "d" content for the sun icon key, and that it differs from the
     * moon icon's content (guarding against both keys accidentally pointing
     * at the same glyph).
     */
    @Test
    @DisplayName("getPath returns the configured value for the sun icon key, distinct from the moon icon")
    void getPath_presentKey_returnsDistinctConfiguredValue() {
        logger.debug("Testing getPath with ui.icon.theme.sun");
        String sun = IconRegistry.getPath("ui.icon.theme.sun", "SHOULD_NOT_BE_RETURNED");
        String moon = IconRegistry.getPath("ui.icon.theme.moon", "SHOULD_NOT_BE_RETURNED");

        assertNotEquals("SHOULD_NOT_BE_RETURNED", sun,
                "getPath should resolve ui.icon.theme.sun from icons.properties, not fall back");
        assertNotEquals(moon, sun, "The sun and moon icons must have distinct path data");
    }

    /**
     * Verifies that {@link IconRegistry#getPath} returns the caller-supplied
     * default for any key that is absent from {@code icons.properties}.
     *
     * @param key an absent key, injected by {@link ValueSource @ValueSource}.
     */
    @ParameterizedTest(name = "getPath(\"{0}\", default) returns default")
    @ValueSource(strings = {
            "test.icon.that.does.not.exist.xyz",
            "another.missing.icon.abc",
            "ui.icon.completely.unknown"
    })
    @DisplayName("getPath returns the caller-supplied default for absent keys")
    void getPath_absentKey_returnsDefault(String key) {
        logger.debug("Testing getPath with absent key: {}", key);
        assertEquals("sentinel", IconRegistry.getPath(key, "sentinel"),
                "getPath should return the default for an absent key");
    }

    // -------------------------------------------------------------------------
    // Window control icons (minimise / maximise / close)
    // -------------------------------------------------------------------------

    /**
     * Verifies that the three static window-control icon keys
     * ({@code ui.icon.window.minimise}, {@code ui.icon.window.maximise},
     * {@code ui.icon.window.close}) all resolve to non-blank path data from
     * {@code icons.properties}, and that all three — plus the theme icons —
     * are mutually distinct, guarding against a copy-paste mistake where two
     * keys accidentally point at the same glyph.
     *
     * @param key one of the three window-control icon keys, injected by
     *            {@link ValueSource @ValueSource}.
     */
    @ParameterizedTest(name = "getPath(\"{0}\", default) resolves a non-blank, distinct glyph")
    @ValueSource(strings = {
            "ui.icon.window.minimise",
            "ui.icon.window.maximise",
            "ui.icon.window.close"
    })
    @DisplayName("getPath resolves each window-control icon key to a distinct, non-blank glyph")
    void getPath_windowControlIconKeys_resolveToDistinctNonBlankGlyphs(String key) {
        logger.debug("Testing getPath with window-control icon key: {}", key);
        String result = IconRegistry.getPath(key, "SHOULD_NOT_BE_RETURNED");

        assertNotEquals("SHOULD_NOT_BE_RETURNED", result,
                "getPath should resolve " + key + " from icons.properties, not fall back");
        assertFalse(result.isBlank(), "The " + key + " path data should not be blank");

        for (String otherKey : new String[]{
                "ui.icon.theme.moon", "ui.icon.theme.sun",
                "ui.icon.window.minimise", "ui.icon.window.maximise", "ui.icon.window.close"}) {
            if (!otherKey.equals(key)) {
                assertNotEquals(IconRegistry.getPath(otherKey, ""), result,
                        key + " and " + otherKey + " must not share the same path data");
            }
        }
    }
}
