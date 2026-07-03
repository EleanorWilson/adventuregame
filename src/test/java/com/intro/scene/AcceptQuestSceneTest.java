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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcceptQuestSceneTest {

    private static final Logger logger = LogManager.getLogger(AcceptQuestSceneTest.class);

    @Mock
    private GameIO io;

    @Mock
    private Player player;

    private AcceptQuestScene scene;

    @BeforeEach
    void setUp() {
        logger.debug("Setting up AcceptQuestSceneTest");
        scene = new AcceptQuestScene();
    }

    @Test
    @DisplayName("getID() returns correct enum")
    void testGetIDReturnsEnum() {
        logger.debug("Testing getID() returns correct enum");
        assertEquals(SceneID.ACCEPT, scene.getID(), "getID() should return SceneID.ACCEPT");
    }

    @Test
    @DisplayName("play() returns null and triggers game over")
    void testPlayReturnsNull() {
        logger.debug("Testing play returns null and triggers game over");
        SceneID result = scene.play(player, io);
        assertNull(result, "play() should return null to trigger game over");
    }

    @Test
    @DisplayName("play() uses player's name in final message")
    void testPlayUsesPlayerName() {
        logger.debug("Testing play uses player's name in final message");
        String testName = "JoeBloggs";
        when(player.getName()).thenReturn(testName);
        scene.play(player, io);
        verify(player, atLeast(1)).getName();
        verify(io).println("Bards forever more sing the praises of " + player.getName() + ", hero of the realm!");
    }

    @Test
    @DisplayName("play() prints text via io.println()")
    void testPlayPrintsViaGameIO() {
        logger.debug("Testing play prints text via io.println()");
        scene.play(player, io);
        verify(io, atLeast(1)).println(anyString());
    }

    @Test
    @DisplayName("play() never prompts player for input")
    void testPlayNeverPromptsInput() {
        logger.debug("Testing play() never prompts the user for any input");
        scene.play(player, io);
        verify(io, never()).prompt(anyString());
    }
}
