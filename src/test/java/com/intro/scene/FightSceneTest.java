package com.intro.scene;

import com.intro.io.GameIO;
import com.intro.model.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link FightScene}.
 *
 * <p>D
 *     Uses Mockito to mock {@link GameIO} and {@link Player}, isolating the scene
 *     from all IO operations and player state.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class FightSceneTest {

    private static final Logger logger = LogManager.getLogger(FightSceneTest.class);

    /** Mock {@link GameIO} injected by Mockito. */
    @Mock
    private GameIO mockIO;

    /** Mock {@link Player} injected by Mockito. */
    @Mock
    private Player mockPlayer;

    /** The scene under test. */
    private FightScene scene;

    /**
     * Creates a fresh {@link FightScene} before each test.
     */
    @BeforeEach
    void setUp() {
        logger.debug("Setting up FightSceneTest");
        scene = new FightScene();
    }

    /**
     * Verifies that {@link FightScene#getID()} returns {@link SceneID#FIGHT}.
     */
    @Test
    @DisplayName("getID() returns SceneID.FIGHT")
    void testGetIDReturnsFight() {
        logger.debug("Testing getID returns FIGHT");
        assertEquals(SceneID.FIGHT, scene.getID(),
                "getID() should return SceneID.FIGHT");
    }

    /**
     * Verifies that {@link FightScene#play(Player, GameIO)} returns {@code null},
     * which signals the game loop to end (defeat ending).
     */
    @Test
    @DisplayName("play() returns null to end the game")
    void testPlayReturnsNull() {
        logger.debug("Testing play returns null");
        SceneID result = scene.play(mockPlayer, mockIO);
        assertNull(result, "play() should return null to trigger game over");
    }

    /**
     * Verifies that {@link FightScene#play(Player, GameIO)} calls
     * {@link GameIO#println(String)} at least once to display the defeat narrative.
     */
    @Test
    @DisplayName("play() prints the defeat narrative via io.println")
    void testPlayPrintsDefeatNarrative() {
        logger.debug("Testing play prints defeat narrative");
        scene.play(mockPlayer, mockIO);
        verify(mockIO, atLeastOnce()).println(anyString());
    }

    /**
     * Verifies that the defeat narrative contains the text {@code "killed"} to
     * convey the outcome clearly to the player.
     */
    @Test
    @DisplayName("play() prints a message containing 'killed'")
    void testPlayPrintsKilledMessage() {
        logger.debug("Testing play prints 'killed' message");
        scene.play(mockPlayer, mockIO);
        verify(mockIO).println("You are killed...");
    }

    /**
     * Verifies that {@link FightScene#play(Player, GameIO)} never calls
     * {@link GameIO#prompt(String)}, since the fight scene has no player input.
     */
    @Test
    @DisplayName("play() never prompts for player input")
    void testPlayNeverPrompts() {
        logger.debug("Testing play never calls prompt");
        scene.play(mockPlayer, mockIO);
        verify(mockIO, never()).prompt(anyString());
    }
}
