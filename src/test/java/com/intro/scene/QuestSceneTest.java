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
 * Unit tests for {@link QuestScene}.
 *
 * <h3>Parameterization strategy</h3>
 * <p>
 *     {@link QuestScene} recognises two sets of synonymous inputs:
 * </p>
 * <ul>
 *     <li><strong>Accept path</strong> — recognised inputs: {@code "1"}, {@code "accept"}</li>
 *     <li><strong>Refuse path</strong> — recognised inputs: {@code "2"}, {@code "refuse"}</li>
 * </ul>
 * <p>
 *     The original four identical {@code @Test} methods are replaced by two
 *     {@link ParameterizedTest @ParameterizedTest} methods using
 *     {@link ValueSource @ValueSource(strings = {...})}.
 * </p>
 *
 * <h3>Stub management with {@code lenient()}</h3>
 * <p>
 *     {@link QuestScene#play(Player, GameIO)} always calls {@link Player#getName()} to
 *     embed the player's name in narrative text.  Placing the stub in
 *     {@link #setUp() @BeforeEach} with
 *     {@code lenient().when(mockPlayer.getName()).thenReturn(...)} suppresses
 *     Mockito's {@code UnnecessaryStubbingException} for the one test that does
 *     <em>not</em> call {@code play()} (i.e. {@link #testGetIDReturnsQuest()}), while
 *     keeping strict checking active for all other stubs.  This removes the need to
 *     repeat the same stub declaration inside every test method body.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class QuestSceneTest {

    private static final Logger logger = LogManager.getLogger(QuestSceneTest.class);

    /** Mock {@link GameIO} injected by Mockito. */
    @Mock
    private GameIO mockIO;

    /** Mock {@link Player} injected by Mockito. */
    @Mock
    private Player mockPlayer;

    /** The scene under test, recreated before every test invocation. */
    private QuestScene scene;

    /**
     * Creates a fresh {@link QuestScene} and registers a lenient player-name stub
     * before each test (including each {@code @ParameterizedTest} invocation).
     *
     * <p>
     *     The {@code lenient()} qualifier tells Mockito that it is acceptable for this
     *     particular stub to go unused in tests that do not call
     *     {@link QuestScene#play(Player, GameIO)}.  All other stubs in these tests remain
     *     under strict verification, preserving the safety net against accidental unused
     *     stubs elsewhere.
     * </p>
     */
    @BeforeEach
    void setUp() {
        logger.debug("Setting up QuestSceneTest");
        scene = new QuestScene();
        lenient().when(mockPlayer.getName()).thenReturn("Arthur");
    }

    // -------------------------------------------------------------------------
    // getID
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link QuestScene#getID()} returns {@link SceneID#QUEST}.
     */
    @Test
    @DisplayName("getID() returns SceneID.QUEST")
    void testGetIDReturnsQuest() {
        logger.debug("Testing getID returns QUEST");
        assertEquals(SceneID.QUEST, scene.getID(),
                "getID() should return SceneID.QUEST");
    }

    // -------------------------------------------------------------------------
    // Valid input — accept path
    // -------------------------------------------------------------------------

    /**
     * Verifies that every recognised "accept" input returns {@link SceneID#ACCEPT}.
     *
     * <p>
     *     Inputs under test: {@code "1"} (numeric choice) and {@code "accept"}
     *     (keyword, whose first character {@code 'a'} triggers the accept branch).
     *     Each is a separate JUnit 5 invocation via
     *     {@link ValueSource @ValueSource(strings = {...})}.
     * </p>
     *
     * @param input a recognised accept-path input string, injected by JUnit 5.
     */
    @ParameterizedTest(name = "input ''{0}'' routes to ACCEPT")
    @ValueSource(strings = {"1", "accept"})
    @DisplayName("play() returns ACCEPT for each recognised accept-path input")
    void testPlayAcceptPath(String input) {
        logger.debug("Testing accept path with input: '{}'", input);
        when(mockIO.prompt(anyString())).thenReturn(input);

        SceneID result = scene.play(mockPlayer, mockIO);

        assertEquals(SceneID.ACCEPT, result,
                "Input '" + input + "' should lead to SceneID.ACCEPT");
    }

    // -------------------------------------------------------------------------
    // Valid input — refuse path
    // -------------------------------------------------------------------------

    /**
     * Verifies that every recognised "refuse" input returns {@link SceneID#REFUSE}.
     *
     * <p>
     *     Inputs under test: {@code "2"} (numeric choice) and {@code "refuse"}
     *     (keyword, whose first character {@code 'r'} triggers the refuse branch).
     *     Each is a separate JUnit 5 invocation via
     *     {@link ValueSource @ValueSource(strings = {...})}.
     * </p>
     *
     * @param input a recognised refuse-path input string, injected by JUnit 5.
     */
    @ParameterizedTest(name = "input ''{0}'' routes to REFUSE")
    @ValueSource(strings = {"2", "refuse"})
    @DisplayName("play() returns REFUSE for each recognised refuse-path input")
    void testPlayRefusePath(String input) {
        logger.debug("Testing refuse path with input: '{}'", input);
        when(mockIO.prompt(anyString())).thenReturn(input);

        SceneID result = scene.play(mockPlayer, mockIO);

        assertEquals(SceneID.REFUSE, result,
                "Input '" + input + "' should lead to SceneID.REFUSE");
    }

    // -------------------------------------------------------------------------
    // Invalid / empty input validation
    // -------------------------------------------------------------------------

    /**
     * Verifies that an unrecognised input triggers an error message and re-prompt,
     * eventually resolving on the next valid entry.
     */
    @Test
    @DisplayName("play() prints error and re-prompts on unrecognised input")
    void testPlayInvalidThenValidInput() {
        logger.debug("Testing play loops on invalid input");
        when(mockIO.prompt(anyString())).thenReturn("xyz", "1");

        SceneID result = scene.play(mockPlayer, mockIO);

        assertEquals(SceneID.ACCEPT, result,
                "Should return ACCEPT after recovering from invalid input");
        verify(mockIO).println("You have entered an invalid response, please enter 1 or 2.");
    }

    /**
     * Verifies that an empty string input triggers an error message and re-prompt,
     * eventually resolving on the next valid entry.
     */
    @Test
    @DisplayName("play() prints error and re-prompts on empty input")
    void testPlayEmptyThenValidInput() {
        logger.debug("Testing play loops on empty input");
        when(mockIO.prompt(anyString())).thenReturn("", "2");

        SceneID result = scene.play(mockPlayer, mockIO);

        assertEquals(SceneID.REFUSE, result,
                "Should return REFUSE after recovering from empty input");
        verify(mockIO).println("You have entered an invalid response, please enter 1 or 2.");
    }

    // -------------------------------------------------------------------------
    // Player name usage
    // -------------------------------------------------------------------------

    /**
     * Verifies that the scene calls {@link Player#getName()} at least once during
     * {@link QuestScene#play(Player, GameIO)}, confirming the player's name is
     * embedded in the narrative text.
     */
    @Test
    @DisplayName("play() retrieves the player's name for personalised narrative")
    void testPlayUsesPlayerName() {
        logger.debug("Testing play uses player name in narrative");
        when(mockIO.prompt(anyString())).thenReturn("1");

        scene.play(mockPlayer, mockIO);

        verify(mockPlayer, atLeastOnce()).getName();
    }
}
