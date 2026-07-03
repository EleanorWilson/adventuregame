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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForestSceneTest {

    private static final Logger logger = LogManager.getLogger(ForestSceneTest.class);

    @Mock
    private GameIO io;

    @Mock
    private Player player;

    private ForestScene scene;

    @BeforeEach
    void setUp() {
        logger.debug("Setting up CrowsSceneTest");
        scene = new ForestScene();
    }

    @Test
    @DisplayName("getID() returns correct enum")
    void testGetIDReturnsEnum() {
        logger.debug("Testing getID() returns correct enum");
        assertEquals(SceneID.FOREST, scene.getID(), "getID() should return SceneID.FOREST");
    }

    /**
     * Testing FRIENDLY path works as intended. Also testing that case does not matter ("friendly" and "FRIENDLY", etc, are treated the same). Also testing that a single "f" input would be registered.
     * @param input valid inputs that lead to SceneID.QUEST, from {@code @ValueSource}
     */
    @ParameterizedTest(name="input ''{0}'' returns QUEST")
    @ValueSource(strings = {"1", "friendly", "f", "Friendly"})
    // @DisplayName("play() returns FIGHT with \"1\" or \"fight\" input")
    void testPlayFightPath(String input) {
        logger.debug("Testing friendly path with input: {}", input);
        when(io.prompt(anyString())).thenReturn(input);
        SceneID result = scene.play(player, io);
        assertEquals(SceneID.QUEST, result, "Input: " + input + "should return SceneID.QUEST");
    }

    /**
     * Testing THREATEN path works as intended. Also testing that case does not matter ("threaten" and "Threaten", etc, are treated the same). Also testing that a single "t" input would be registered.
     * @param input valid inputs that lead to SceneID.CROWS, from {@code @ValueSource}
     */
    @ParameterizedTest(name="input ''{0}'' returns CROWS")
    @ValueSource(strings = {"2", "threaten", "t", "Threaten"})
    void testPlayFlightPath(String input) {
        logger.debug("Testing flight path with input: {}", input);
        when(io.prompt(anyString())).thenReturn(input);
        SceneID result = scene.play(player, io);
        assertEquals(SceneID.CROWS, result, "input: " + input + "should return SceneID.CROWS");
    }

    @Test
    @DisplayName("play() prints error and re-prompts on invalid input")
    void testPlayInvalidInputReprompt() {
        logger.debug("Testing play() shows error message on invalid input, then reprompts & accepts valid input");
        when(io.prompt(anyString())).thenReturn("invalid", "1");
        SceneID result = scene.play(player, io);
        verify(io).println(contains("invalid response"));
        assertEquals(SceneID.QUEST, result, "Should return QUEST on '1' input after reprompt");
    }

    @Test
    @DisplayName("play() prints error and re-prompts on empty input")
    void testPlayEmptyInputReprompt() {
        logger.debug("Testing play() shows error message on empty input, then reprompts & accepts valid input");
        when(io.prompt(anyString())).thenReturn("", "2");
        SceneID result = scene.play(player, io);
        verify(io).println(contains("invalid response"));
        assertEquals(SceneID.CROWS, result, "Should return CROWS on '1' input after reprompt");
    }
}
