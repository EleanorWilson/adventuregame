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
 * Unit tests for {@link CrowsScene}.
 *
 * <h3>Parameterization strategy</h3>
 * <p>
 *     {@link CrowsScene} accepts multiple synonymous inputs for each of its two
 *     decision paths:
 * </p>
 * <ul>
 *     <li><strong>Fight path</strong> — recognised inputs: {@code "1"}, {@code "fight"}</li>
 *     <li><strong>Flight path</strong> — recognised inputs: {@code "2"}, {@code "flight"}, {@code "flee"}</li>
 * </ul>
 * <p>
 *     The original design had one {@code @Test} method per input (five methods total).
 *     Each was structurally identical — stub a single prompt return value, call
 *     {@code play()}, assert the expected {@link SceneID}.  This is a textbook case for
 *     {@link ParameterizedTest @ParameterizedTest} with
 *     {@link ValueSource @ValueSource(strings = {...})}: one method per decision path,
 *     one invocation per recognised input.  Removing the duplication means that adding
 *     a new synonym (e.g. {@code "attack"}) requires changing only the {@code @ValueSource}
 *     annotation, not writing a whole new method.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class CrowsSceneTest {

    private static final Logger logger = LogManager.getLogger(CrowsSceneTest.class);

    /** Mock {@link GameIO} injected by Mockito. */
    @Mock
    private GameIO mockIO;

    /** Mock {@link Player} injected by Mockito. */
    @Mock
    private Player mockPlayer;

    /** The scene under test, recreated before every test invocation. */
    private CrowsScene scene;

    /**
     * Creates a fresh {@link CrowsScene} before each test (including each
     * {@code @ParameterizedTest} invocation).
     */
    @BeforeEach
    void setUp() {
        logger.debug("Setting up CrowsSceneTest");
        scene = new CrowsScene();
    }

    // -------------------------------------------------------------------------
    // getID
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link CrowsScene#getID()} returns {@link SceneID#CROWS}.
     */
    @Test
    @DisplayName("getID() returns SceneID.CROWS")
    void testGetIDReturnsCrows() {
        logger.debug("Testing getID returns CROWS");
        assertEquals(SceneID.CROWS, scene.getID(),
                "getID() should return SceneID.CROWS");
    }

    // -------------------------------------------------------------------------
    // Valid input — fight path
    // -------------------------------------------------------------------------

    /**
     * Verifies that every recognised "fight" input returns {@link SceneID#FIGHT}.
     *
     * <p>
     *     Inputs under test: {@code "1"} (numeric choice) and {@code "fight"} (keyword).
     *     Each is supplied as a separate JUnit 5 test invocation via
     *     {@link ValueSource @ValueSource(strings = {...})}.
     * </p>
     *
     * @param input a recognised fight-path input string, injected by JUnit 5.
     */
    @ParameterizedTest(name = "input ''{0}'' routes to FIGHT")
    @ValueSource(strings = {"1", "fight"})
    @DisplayName("play() returns FIGHT for each recognised fight-path input")
    void testPlayFightPath(String input) {
        logger.debug("Testing fight path with input: '{}'", input);
        when(mockIO.prompt(anyString())).thenReturn(input);

        SceneID result = scene.play(mockPlayer, mockIO);

        assertEquals(SceneID.FIGHT, result,
                "Input '" + input + "' should lead to SceneID.FIGHT");
    }

    // -------------------------------------------------------------------------
    // Valid input — flight path
    // -------------------------------------------------------------------------

    /**
     * Verifies that every recognised "flight" input returns {@link SceneID#FOREST}.
     *
     * <p>
     *     Inputs under test: {@code "2"} (numeric choice), {@code "flight"} and
     *     {@code "flee"} (keyword synonyms).  Each is a separate invocation via
     *     {@link ValueSource @ValueSource(strings = {...})}.
     * </p>
     *
     * @param input a recognised flight-path input string, injected by JUnit 5.
     */
    @ParameterizedTest(name = "input ''{0}'' routes back to FOREST")
    @ValueSource(strings = {"2", "flight", "flee"})
    @DisplayName("play() returns FOREST for each recognised flight-path input")
    void testPlayFlightPath(String input) {
        logger.debug("Testing flight path with input: '{}'", input);
        when(mockIO.prompt(anyString())).thenReturn(input);

        SceneID result = scene.play(mockPlayer, mockIO);

        assertEquals(SceneID.FOREST, result,
                "Input '" + input + "' should lead back to SceneID.FOREST");
    }

    // -------------------------------------------------------------------------
    // Invalid / empty input validation
    // -------------------------------------------------------------------------

    /**
     * Verifies that a completely invalid input causes the scene to print an error
     * message and re-prompt, eventually resolving on the next valid entry.
     */
    @Test
    @DisplayName("play() prints error and re-prompts on unrecognised input")
    void testPlayInvalidThenValidInput() {
        logger.debug("Testing play loops on invalid input then resolves");
        when(mockIO.prompt(anyString())).thenReturn("invalid", "1");

        SceneID result = scene.play(mockPlayer, mockIO);

        assertEquals(SceneID.FIGHT, result,
                "Should return FIGHT after recovering from an invalid response");
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

        assertEquals(SceneID.FOREST, result,
                "Should return FOREST after recovering from an empty response");
        verify(mockIO).println("You have entered an invalid response, please enter 1 or 2.");
    }

    // -------------------------------------------------------------------------
    // Flight-path confirmation message
    // -------------------------------------------------------------------------

    /**
     * Verifies that choosing the flight path prints a confirmation message to the
     * player before returning to the forest.
     */
    @Test
    @DisplayName("play() prints a confirmation message when player flees")
    void testPlayFlightPrintsConfirmationMessage() {
        logger.debug("Testing play prints flight confirmation message");
        when(mockIO.prompt(anyString())).thenReturn("2");

        scene.play(mockPlayer, mockIO);

        verify(mockIO).println("You choose to flee and race back into the forest, wiser now than before.");
    }
}
