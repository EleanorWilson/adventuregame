package com.intro.scene;

import com.intro.io.GameIO;
import com.intro.model.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestSceneTest {

    private static final Logger logger = LogManager.getLogger(QuestSceneTest.class);

    @Mock
    private GameIO io;

    @Mock
    private Player player;

    private QuestScene scene;

    @BeforeEach
    void setUp() {
        logger.debug("Setting up QuestSceneTest");
        scene = new QuestScene();
        // TODO remove if lenient does not work
        lenient().when(player.getName()).thenReturn("Player");
    }

    @Test
    @DisplayName("getID() returns correct enum")
    void testGetIDReturnsEnum() {
        logger.debug("Testing getID() returns correct enum");
        assertEquals(SceneID.QUEST, scene.getID(), "getID() should return SceneID.QUEST");
    }

    /**
     * Testing ACCEPT path works as intended.
     * @param input valid inputs that lead to {@link SceneID#ACCEPT}, from {@code @ValueSource}
     */
    @ParameterizedTest(name="input ''{0}'' returns ACCEPT")
    @ValueSource(strings = {"1", "accept", "a", "Accept"})
    void testPlayAcceptPath(String input) {
        logger.debug("Testing ACCEPT path with input: {}", input);
        when(io.prompt(anyString())).thenReturn(input);
        SceneID result = scene.play(player, io);
        assertEquals(SceneID.ACCEPT, result, "Input: " + input + "should return SceneID.ACCEPT");
    }

    /**
     * Testing REFUSE path works as intended.
     * @param input valid inputs that lead to {@link SceneID#REFUSE}, from {@code @ValueSource}
     */
    @ParameterizedTest(name="input ''{0}'' returns REFUSE")
    @ValueSource(strings = {"2", "refuse", "r", "Refuse"})
    void testPlayRefusePath(String input) {
        logger.debug("Testing REFUSE path with input: {}", input);
        when(io.prompt(anyString())).thenReturn(input);
        SceneID result = scene.play(player, io);
        assertEquals(SceneID.REFUSE, result, "input: " + input + "should return SceneID.REFUSE");
    }

    @Test
    @DisplayName("play() prints error and re-prompts on invalid input")
    void testPlayInvalidInputReprompt() {
        logger.debug("Testing play() shows error message on invalid input, then reprompts & accepts valid input");
        when(io.prompt(anyString())).thenReturn("invalid", "1");
        SceneID result = scene.play(player, io);
        verify(io).println(contains("invalid response"));
        assertEquals(SceneID.ACCEPT, result, "Should return SceneID.ACCEPT on '1' input after reprompt");
    }

    @Test
    @DisplayName("play() prints error and re-prompts on empty input")
    void testPlayEmptyInputReprompt() {
        logger.debug("Testing play() shows error message on empty input, then reprompts & accepts valid input");
        when(io.prompt(anyString())).thenReturn("", "2");
        SceneID result = scene.play(player, io);
        verify(io).println(contains("invalid response"));
        assertEquals(SceneID.REFUSE, result, "Should return SceneID.REFUSE on '2' input after reprompt");
    }

    @Test
    @DisplayName("play() prints player name in text")
    void testPlayPrintsPlayerName() {
        logger.debug("Testing play() prints player name in the narrative text");
        when(io.prompt(anyString())).thenReturn("1");
        scene.play(player, io);
        verify(player, atLeast(1)).getName();
    }
}
