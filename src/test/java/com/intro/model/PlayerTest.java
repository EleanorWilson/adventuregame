package com.intro.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerTest {

    private static final Logger logger = LogManager.getLogger(PlayerTest.class);

    private Player player;

    @BeforeEach
    void setUp() {
      player = new Player();
    }

    @Test
    @DisplayName("Player constructor creates instances of Player with empty string as name variable")
    void testNewPlayerNameIsEmptyString() {
        logger.debug("Testing new player instances have empty string Name variable");
        assertEquals("", player.getName(), "Get name should be an empty string");
    }

    @Test
    @DisplayName("Player getter and setter work as expected")
    void testPlayerGetSetName() {
        logger.debug("Testing Player name getters and setters work");
        String testName = "Test";
        player.setName(testName);
        assertEquals(testName, player.getName(), "getName() should return the name set by setName()");
    }

    @Test
    @DisplayName("Player setName overwrites previous names")
    void testPlayerSetNameOverwritesPrevious() {
        logger.debug("Testing Player setName overwrites previous names");
        player.setName("TestName1");
        player.setName("TestName2");
        assertEquals("TestName2", player.getName(), "setName() should overwrite previous name");
    }

    @Test
    @DisplayName("Player set name accepts empty strings")
    void testPlayerSetNameAcceptsEmptyStrings() {
        logger.debug("Testing Player setName accepts empty strings");
        player.setName("");
        assertEquals("", player.getName(), "setName() should not throw exception when setting name to empty string");
    }

    // test set name accepts special characters?
    @Test
    @DisplayName("Player set name accepts special characters")
    void testPlayerSetNameAcceptsSpecialCharacters() {
        logger.debug("Testing Player setName accepts special characters");
        String testName = "Testing space and !?";
        player.setName(testName);
        assertEquals(testName, player.getName(), "setName() should not throw exception when setting name with special characters");
    }

}
