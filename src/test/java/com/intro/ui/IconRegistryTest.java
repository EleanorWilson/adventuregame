package com.intro.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.*;

class IconRegistryTest {

    private static final Logger logger = LogManager.getLogger(IconRegistryTest.class);

    @Test
    @DisplayName("getPath returns the configured value for the moon key")
    void getPath_presentKey_returnsConfiguredValue() {
        logger.debug("Testing getPath with ui.icon.theme.moon");
        String result = IconRegistry.getPath("ui.icon.theme.moon", "SHOULD_NOT_BE_RETURNED");
        assertNotEquals("SHOULD_NOT_BE_RETURNED", result, "getPath should resolve ui.icon.theme.moon from icons.properties");
        assertFalse(result.isBlank(), "The moon icon's path data should not be blank");
    }

    @Test
    @DisplayName("getPath returns the configured value for the sun icon key, distinct from the moon key")
    void getPath_presentKey_returnsDistinctValue() {
        logger.debug("Testing getPath with ui.icon.theme.sun");
        String sun = IconRegistry.getPath("ui.icon.theme.sun", "SHOULD_NOT_BE_RETURNED");
        logger.debug("Testing getPath with ui.icon.theme.moon");
        String moon = IconRegistry.getPath("ui.icon.theme.moon", "SHOULD_NOT_BE_RETURNED");
        assertNotEquals("SHOULD_NOT_BE_RETURNED", sun, "getPath should resolve ui.icon.theme.sun from icons.properties");
        assertFalse(sun.isBlank(), "The sun icon's path data should not be blank");
        assertNotEquals(moon, sun, "The sun and moon icons must have distinct path data");
    }

    @ParameterizedTest(name = "getPath(\"{0}\", default) returns default")
    @ValueSource(strings = {
            "test.icon.that.does.not.exist",
            "test.another.missing.icon",
            "ui.icon.completely.invalid"
    })
    @DisplayName("getPath returns the caller-supplied default for absent keys")
    void getPath_absentKey_returnsDefault(String key) {
        logger.debug("Testing getPath with absent key: {}", key);
        assertEquals("DefaultValue", IconRegistry.getPath(key, "DefaultValue"), "getPath should return the default for an absent key");
    }
}
