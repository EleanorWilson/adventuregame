package com.intro.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link Player} class.
 *
 * <p>
 *     Covers default construction, name getter, and name setter behaviours.
 *     All tests use a fresh {@link Player} instance created in {@link #setUp()}.
 * </p>
 */
class PlayerTest {

    private static final Logger logger = LogManager.getLogger(PlayerTest.class);

    /** The {@link Player} instance under test, recreated before each test. */
    private Player player;

    /**
     * Creates a fresh {@link Player} before every test to ensure isolation.
     */
    @BeforeEach
    void setUp() {
        logger.debug("Creating new Player instance for test");
        player = new Player();
    }

    /**
     * Verifies that a newly constructed {@link Player} has a name equal to an
     * empty string and not {@code null}.
     */
    @Test
    @DisplayName("Constructor initialises name to an empty string")
    void testDefaultNameIsEmptyString() {
        logger.debug("Testing default name is empty string");
        assertNotNull(player.getName(), "getName() should never return null after construction");
        assertEquals("", player.getName(), "Default name should be an empty string");
    }

    /**
     * Verifies that {@link Player#setName(String)} correctly stores the supplied name
     * and that {@link Player#getName()} returns that same name.
     */
    @Test
    @DisplayName("setName stores the name and getName returns it")
    void testSetAndGetName() {
        logger.debug("Testing setName / getName round-trip");
        player.setName("Aragorn");
        assertEquals("Aragorn", player.getName(), "getName() should return the name set by setName()");
    }

    /**
     * Verifies that {@link Player#setName(String)} can be called multiple times
     * and each call replaces the previous name.
     */
    @Test
    @DisplayName("setName can update name more than once")
    void testSetNameOverwritesPreviousName() {
        logger.debug("Testing that setName overwrites a previous name");
        player.setName("First");
        player.setName("Second");
        assertEquals("Second", player.getName(), "setName() should overwrite the previous name");
    }

    /**
     * Verifies that {@link Player#setName(String)} accepts an empty string without
     * throwing an exception, as empty names are handled at the scene level.
     */
    @Test
    @DisplayName("setName accepts an empty string without throwing")
    void testSetNameWithEmptyString() {
        logger.debug("Testing that setName accepts an empty string");
        assertDoesNotThrow(() -> player.setName(""),
                "setName() should not throw when given an empty string");
        assertEquals("", player.getName());
    }

    /**
     * Verifies that {@link Player#setName(String)} stores names containing
     * spaces and special characters correctly.
     */
    @Test
    @DisplayName("setName stores names with spaces and special characters")
    void testSetNameWithSpecialCharacters() {
        logger.debug("Testing setName with special characters");
        String specialName = "Sir Lancelot du Lac!";
        player.setName(specialName);
        assertEquals(specialName, player.getName(),
                "getName() should return the name exactly as set, including special characters");
    }
}
