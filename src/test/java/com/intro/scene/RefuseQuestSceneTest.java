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
 * Unit tests for {@link RefuseQuestScene}.
 *
 * <p>
 *     Uses Mockito to mock {@link GameIO} and {@link Player} so tests run in
 *     full isolation from IO and player state.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class RefuseQuestSceneTest {

    private static final Logger logger = LogManager.getLogger(RefuseQuestSceneTest.class);

    /** Mock {@link GameIO} injected by Mockito. */
    @Mock
    private GameIO mockIO;

    /** Mock {@link Player} injected by Mockito. */
    @Mock
    private Player mockPlayer;

    /** The scene under test. */
    private RefuseQuestScene scene;

    /**
     * Creates a fresh {@link RefuseQuestScene} before each test.
     */
    @BeforeEach
    void setUp() {
        logger.debug("Setting up RefuseQuestSceneTest");
        scene = new RefuseQuestScene();
    }

    /**
     * Verifies that {@link RefuseQuestScene#getID()} returns {@link SceneID#REFUSE}.
     */
    @Test
    @DisplayName("getID() returns SceneID.REFUSE")
    void testGetIDReturnsRefuse() {
        logger.debug("Testing getID returns REFUSE");
        assertEquals(SceneID.REFUSE, scene.getID(),
                "getID() should return SceneID.REFUSE");
    }

    /**
     * Verifies that {@link RefuseQuestScene#play(Player, GameIO)} returns
     * {@code null}, which signals the game loop to end.
     */
    @Test
    @DisplayName("play() returns null to end the game")
    void testPlayReturnsNull() {
        logger.debug("Testing play returns null");
        SceneID result = scene.play(mockPlayer, mockIO);
        assertNull(result, "play() should return null to trigger game over");
    }

    /**
     * Verifies that {@link RefuseQuestScene#play(Player, GameIO)} prints
     * narrative text describing the quest refusal.
     */
    @Test
    @DisplayName("play() prints the quest refusal narrative")
    void testPlayPrintsRefusalNarrative() {
        logger.debug("Testing play prints refusal narrative");
        scene.play(mockPlayer, mockIO);
        verify(mockIO).println("You reject the quest and the wizard bows their head in sorrow, letting you pass.");
    }

    /**
     * Verifies that {@link RefuseQuestScene#play(Player, GameIO)} never calls
     * {@link GameIO#prompt(String)}, since this is a terminal narrative scene
     * with no player input.
     */
    @Test
    @DisplayName("play() never prompts for player input")
    void testPlayNeverPrompts() {
        logger.debug("Testing play never calls prompt");
        scene.play(mockPlayer, mockIO);
        verify(mockIO, never()).prompt(anyString());
    }
}
