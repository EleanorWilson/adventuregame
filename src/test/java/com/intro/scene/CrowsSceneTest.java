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
class CrowsSceneTest {

    private static final Logger logger = LogManager.getLogger(CrowsSceneTest.class);

    @Mock
    private GameIO io;

    @Mock
    private Player player;

    private CrowsScene scene;

    @BeforeEach
    void setUp() {
        logger.debug("Setting up CrowsSceneTest");
        scene = new CrowsScene();
    }

    @Test
    @DisplayName("getID() returns correct enum")
    void testGetIDReturnsEnum() {
        logger.debug("Testing getID() returns correct enum");
        assertEquals(SceneID.CROWS, scene.getID(), "getID() should return SceneID.CROWS");
    }

    /**
     * Testing FIGHT path works as intended.
     * @param input valid inputs that lead to SceneID.FIGHT, from {@code @ValueSource}
     */
    @ParameterizedTest(name="input ''{0}'' returns FIGHT")
    @ValueSource(strings = {"1", "fight"})
    // @DisplayName("play() returns FIGHT with \"1\" or \"fight\" input")
    void testPlayFightPath(String input) {
        logger.debug("Testing fight path with input: {}", input);
        when(io.prompt(anyString())).thenReturn(input);
        SceneID result = scene.play(player, io);
        assertEquals(SceneID.FIGHT, result, "Input: " + input + "should return SceneID.FIGHT");
    }

    /**
     * Testing FOREST (flight) path works as intended.
     * @param input valid inputs that lead to SceneID.FOREST, from {@code @ValueSource}
     */
    @ParameterizedTest(name="input ''{0}'' returns FOREST")
    @ValueSource(strings = {"2", "flight", "flee"})
    void testPlayFlightPath(String input) {
        logger.debug("Testing flight path with input: {}", input);
        when(io.prompt(anyString())).thenReturn(input);
        SceneID result = scene.play(player, io);
        assertEquals(SceneID.FOREST, result, "input: " + input + "should return SceneID.FOREST");
    }

    @Test
    @DisplayName("play() prints error and re-prompts on invalid input")
    void testPlayInvalidInputReprompt() {
        logger.debug("Testing play() shows error message on invalid input, then reprompts & accepts valid input");
        when(io.prompt(anyString())).thenReturn("invalid", "1");
        SceneID result = scene.play(player, io);
        verify(io).println(contains("invalid response"));
        assertEquals(SceneID.FIGHT, result, "Should return fight on '1' input after reprompt");
    }

    @Test
    @DisplayName("play() prints error and re-prompts on empty input")
    void testPlayEmptyInputReprompt() {
        logger.debug("Testing play() shows error message on empty input, then reprompts & accepts valid input");
        when(io.prompt(anyString())).thenReturn("", "1");
        SceneID result = scene.play(player, io);
        verify(io).println(contains("invalid response"));
        assertEquals(SceneID.FIGHT, result, "Should return fight on '1' input after reprompt");
    }

    @Test
    @DisplayName("play() prints flee message before returning FOREST")
    void testPlayPrintsFleeMessage() {
        logger.debug("Testing play() shows flee message before returning SceneID.FOREST when flight path chosen");
        when(io.prompt(anyString())).thenReturn("2");
        SceneID result = scene.play(player, io);
        verify(io).println(contains("You choose to flee"));
        assertEquals(SceneID.FOREST, result, "Should return forest on '2' input");
    }
}
