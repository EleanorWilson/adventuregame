package com.intro.scene;

import com.intro.io.GameIO;
import com.intro.model.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AcceptQuestSceneTest}.
 *
 * <p>
 *     Uses Mockito to mock {@link GameIO} and {@link Player} so that tests are
 *     fully isolated from IO operations and player state.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class AcceptQuestSceneTest {

    private static final Logger logger = LogManager.getLogger(AcceptQuestSceneTest.class);

    /** Mock {@link GameIO} injected by Mockito. */
    @Mock
    private GameIO mockIO;

    /** Mock {@link Player} injected by Mockito. */
    @Mock
    private Player mockPlayer;

    /** The scene under test. */
    private AcceptQuestSceneTest scene;

    /**
     * Creates a fresh {@link AcceptQuestSceneTest} before each test.
     */
    @BeforeEach
    void setUp() {
        logger.debug("Setting up AcceptQuestSceneTest");
        scene = new AcceptQuestSceneTest();
    }

    /**
     * Verifies that {@link AcceptQuestSceneTest#getID()} returns {@link SceneID#ACCEPT}.
     */
    @Test
    @DisplayName("getID() returns SceneID.ACCEPT")
    void testGetIDReturnsAccept() {
        logger.debug("Testing getID returns ACCEPT");
        Assertions.assertEquals(SceneID.ACCEPT, scene.getID(),
                "getID() should return SceneID.ACCEPT");
    }

    /**
     * Verifies that {@link AcceptQuestSceneTest#play(Player, GameIO)} returns
     * {@code null}, which signals the game loop to end.
     */
    @Test
    @DisplayName("play() returns null to end the game")
    void testPlayReturnsNull() {
        logger.debug("Testing play returns null");
        when(mockPlayer.getName()).thenReturn("Hero");

        SceneID result = scene.play(mockPlayer, mockIO);

        assertNull(result, "play() should return null to trigger game over");
    }

    /**
     * Verifies that {@link AcceptQuestSceneTest#play(Player, GameIO)} retrieves the
     * player's name and includes it in the victory narrative printed to the IO.
     */
    @Test
    @DisplayName("play() uses the player name in the victory message")
    void testPlayUsesPlayerName() {
        logger.debug("Testing play uses player name");
        when(mockPlayer.getName()).thenReturn("Gandalf");

        scene.play(mockPlayer, mockIO);

        verify(mockPlayer, atLeastOnce()).getName();
        verify(mockIO).println("Bards forever more sing the praises of Gandalf, the hero of the realm!");
    }

    /**
     * Verifies that {@link AcceptQuestSceneTest#play(Player, GameIO)} calls
     * {@link GameIO#println(String)} at least once to print narrative text.
     */
    @Test
    @DisplayName("play() prints narrative text via io.println")
    void testPlayPrintsNarrativeText() {
        logger.debug("Testing play prints narrative text");
        when(mockPlayer.getName()).thenReturn("Merlin");

        scene.play(mockPlayer, mockIO);

        verify(mockIO, atLeastOnce()).println(anyString());
    }
}
