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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ForestScene}.
 *
 * <h3>Parameterization strategy</h3>
 * <p>
 *     {@link ForestScene} accepts two sets of synonymous inputs:
 * </p>
 * <ul>
 *     <li><strong>Friendly / quest path</strong> — recognised inputs: {@code "1"}, {@code "f..."} (e.g. {@code "friendly"})</li>
 *     <li><strong>Threaten / crows path</strong> — recognised inputs: {@code "2"}, {@code "t..."} (e.g. {@code "threaten"})</li>
 * </ul>
 * <p>
 *     The original four individual {@code @Test} methods were structurally identical
 *     (stub prompt → call {@code play()} → assert {@link SceneID}).
 *     {@link ParameterizedTest @ParameterizedTest} with
 *     {@link ValueSource @ValueSource(strings = {...})} collapses each pair into one
 *     method, halving the method count while keeping the same test coverage.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class ForestSceneTest {

    private static final Logger logger = LogManager.getLogger(ForestSceneTest.class);

    /** Mock {@link GameIO} injected by Mockito. */
    @Mock
    private GameIO mockIO;

    /** Mock {@link Player} injected by Mockito. */
    @Mock
    private Player mockPlayer;

    /** The scene under test, recreated before every test invocation. */
    private ForestScene scene;

    /**
     * Creates a fresh {@link ForestScene} before each test (including each
     * {@code @ParameterizedTest} invocation).
     */
    @BeforeEach
    void setUp() {
        logger.debug("Setting up ForestSceneTest");
        scene = new ForestScene();
    }

    // -------------------------------------------------------------------------
    // getID
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link ForestScene#getID()} returns {@link SceneID#FOREST}.
     */
    @Test
    @DisplayName("getID() returns SceneID.FOREST")
    void testGetIDReturnsForest() {
        logger.debug("Testing getID returns FOREST");
        assertEquals(SceneID.FOREST, scene.getID(),
                "getID() should return SceneID.FOREST");
    }

    // -------------------------------------------------------------------------
    // Valid input — quest / friendly path
    // -------------------------------------------------------------------------

    /**
     * Verifies that every recognised "friendly" input returns {@link SceneID#QUEST}.
     *
     * <p>
     *     Inputs under test: {@code "1"} (numeric choice) and {@code "friendly"}
     *     (keyword, whose first character {@code 'f'} triggers the friendly branch).
     *     Each is a separate JUnit 5 invocation via
     *     {@link ValueSource @ValueSource(strings = {...})}.
     * </p>
     *
     * @param input a recognised quest-path input string, injected by JUnit 5.
     */
    @ParameterizedTest(name = "input ''{0}'' routes to QUEST")
    @ValueSource(strings = {"1", "friendly"})
    @DisplayName("play() returns QUEST for each recognised friendly-path input")
    void testPlayQuestPath(String input) {
        logger.debug("Testing quest path with input: '{}'", input);
        when(mockIO.prompt(anyString())).thenReturn(input);

        SceneID result = scene.play(mockPlayer, mockIO);

        assertEquals(SceneID.QUEST, result,
                "Input '" + input + "' should lead to SceneID.QUEST");
    }

    // -------------------------------------------------------------------------
    // Valid input — crows / threaten path
    // -------------------------------------------------------------------------

    /**
     * Verifies that every recognised "threaten" input returns {@link SceneID#CROWS}.
     *
     * <p>
     *     Inputs under test: {@code "2"} (numeric choice) and {@code "threaten"}
     *     (keyword, whose first character {@code 't'} triggers the threaten branch).
     *     Each is a separate JUnit 5 invocation via
     *     {@link ValueSource @ValueSource(strings = {...})}.
     * </p>
     *
     * @param input a recognised crows-path input string, injected by JUnit 5.
     */
    @ParameterizedTest(name = "input ''{0}'' routes to CROWS")
    @ValueSource(strings = {"2", "threaten"})
    @DisplayName("play() returns CROWS for each recognised threaten-path input")
    void testPlayCrowsPath(String input) {
        logger.debug("Testing crows path with input: '{}'", input);
        when(mockIO.prompt(anyString())).thenReturn(input);

        SceneID result = scene.play(mockPlayer, mockIO);

        assertEquals(SceneID.CROWS, result,
                "Input '" + input + "' should lead to SceneID.CROWS");
    }

    // -------------------------------------------------------------------------
    // Invalid / empty input validation
    // -------------------------------------------------------------------------

    /**
     * Verifies that an unrecognised input causes the scene to print an error message
     * and re-prompt, eventually resolving on the next valid entry.
     */
    @Test
    @DisplayName("play() prints error and re-prompts on unrecognised input")
    void testPlayInvalidThenValidInput() {
        logger.debug("Testing play loops on invalid input then resolves");
        when(mockIO.prompt(anyString())).thenReturn("invalid", "1");

        SceneID result = scene.play(mockPlayer, mockIO);

        assertEquals(SceneID.QUEST, result,
                "Should eventually return QUEST after recovering from invalid input");
        verify(mockIO).println("You have entered an invalid response, please enter 1 or 2.");
    }

    /**
     * Verifies that an empty string input causes the scene to print an error and
     * re-prompt, eventually resolving on the next valid entry.
     */
    @Test
    @DisplayName("play() prints error and re-prompts on empty input")
    void testPlayEmptyThenValidInput() {
        logger.debug("Testing play loops on empty input");
        when(mockIO.prompt(anyString())).thenReturn("", "2");

        SceneID result = scene.play(mockPlayer, mockIO);

        assertEquals(SceneID.CROWS, result,
                "Should return CROWS after recovering from empty input");
        verify(mockIO).println("You have entered an invalid response, please enter 1 or 2.");
    }

    // -------------------------------------------------------------------------
    // Narrative output
    // -------------------------------------------------------------------------

    /**
     * Verifies that scene description text is printed at least once when
     * {@link ForestScene#play(Player, GameIO)} is called.
     */
    @Test
    @DisplayName("play() prints the forest scene description")
    void testPlayPrintsSceneDescription() {
        logger.debug("Testing play prints scene description");
        when(mockIO.prompt(anyString())).thenReturn("1");

        scene.play(mockPlayer, mockIO);

        verify(mockIO, atLeastOnce()).println(anyString());
    }
}
