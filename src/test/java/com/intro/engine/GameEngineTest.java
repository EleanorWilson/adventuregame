package com.intro.engine;

import com.intro.io.GameIO;
import com.intro.model.Player;
import com.intro.scene.Scene;
import com.intro.scene.SceneID;
import java.util.EnumMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GameEngine}.
 *
 * <p>
 *     The package-private {@code GameEngine(GameIO, Map)} constructor is used
 *     to inject mock {@link Scene} objects, keeping tests fully isolated from
 *     real scene implementations. Mockito verifies IO interactions and scene
 *     invocations.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class GameEngineTest {

    private static final Logger logger = LogManager.getLogger(GameEngineTest.class);

    /** Mock {@link GameIO} injected by Mockito. */
    @Mock
    private GameIO mockIO;

    /** Mock {@link Scene} injected by Mockito, used as the initial PLAYER_SETUP scene. */
    @Mock
    private Scene mockScene;

    /**
     * Resets mocks to a clean state before every test (handled by Mockito extension).
     */
    @BeforeEach
    void setUp() {
        logger.debug("Setting up GameEngineTest");
    }

    // -------------------------------------------------------------------------
    // printGameOver tests
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link GameEngine#printGameOver()} calls {@link GameIO#println()}
     * for the blank line and three calls to {@link GameIO#println(String)} for the
     * GAME OVER banner.
     */
    @Test
    @DisplayName("printGameOver() prints the game over banner to io")
    void testPrintGameOver() {
        logger.debug("Testing printGameOver prints GAME OVER banner");
        GameEngine engine = new GameEngine(mockIO);

        engine.printGameOver();

        verify(mockIO).println();
        verify(mockIO, times(2)).println("******************************************************");
        verify(mockIO).println("                      GAME OVER");
    }

    // -------------------------------------------------------------------------
    // run() — normal completion
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link GameEngine#run()} calls {@link Scene#play(Player, GameIO)}
     * exactly once when the initial scene returns {@code null} (game end).
     */
    @Test
    @DisplayName("run() calls play() on the first scene and ends when null is returned")
    void testRunCallsPlayOnFirstScene() {
        logger.debug("Testing run calls play on first scene");
        when(mockScene.play(any(Player.class), eq(mockIO))).thenReturn(null);

        Map<SceneID, Scene> scenes = new EnumMap<>(SceneID.class);
        scenes.put(SceneID.PLAYER_SETUP, mockScene);

        GameEngine engine = new GameEngine(mockIO, scenes);
        engine.run();

        verify(mockScene, times(1)).play(any(Player.class), eq(mockIO));
    }

    /**
     * Verifies that {@link GameEngine#run()} transitions through two scenes when
     * the first scene returns a valid {@link SceneID} and the second returns {@code null}.
     */
    @Test
    @DisplayName("run() transitions between scenes until null is returned")
    void testRunTransitionsBetweenScenes() {
        logger.debug("Testing run transitions between two scenes");
        Scene secondScene = mock(Scene.class);

        when(mockScene.play(any(Player.class), eq(mockIO))).thenReturn(SceneID.FOREST);
        when(secondScene.play(any(Player.class), eq(mockIO))).thenReturn(null);

        Map<SceneID, Scene> scenes = new EnumMap<>(SceneID.class);
        scenes.put(SceneID.PLAYER_SETUP, mockScene);
        scenes.put(SceneID.FOREST, secondScene);

        GameEngine engine = new GameEngine(mockIO, scenes);
        engine.run();

        verify(mockScene,   times(1)).play(any(Player.class), eq(mockIO));
        verify(secondScene, times(1)).play(any(Player.class), eq(mockIO));
    }

    /**
     * Verifies that {@link GameEngine#run()} always prints the game over banner
     * via the {@code finally} block, even on a normal (null) exit.
     */
    @Test
    @DisplayName("run() always prints game over on normal exit")
    void testRunAlwaysPrintsGameOverOnNormalExit() {
        logger.debug("Testing run always prints game over");
        when(mockScene.play(any(Player.class), eq(mockIO))).thenReturn(null);

        Map<SceneID, Scene> scenes = new EnumMap<>(SceneID.class);
        scenes.put(SceneID.PLAYER_SETUP, mockScene);

        GameEngine engine = new GameEngine(mockIO, scenes);
        engine.run();

        verify(mockIO).println("                      GAME OVER");
    }

    // -------------------------------------------------------------------------
    // run() — unknown scene
    // -------------------------------------------------------------------------

    /**
     * Verifies that when a scene returns a {@link SceneID} that has no registered
     * {@link Scene} entry, {@link GameEngine#run()} prints an error message and
     * still displays the game over banner.
     */
    @Test
    @DisplayName("run() prints an error and ends when a scene ID has no registered scene")
    void testRunPrintsErrorForUnknownScene() {
        logger.debug("Testing run handles missing scene registration");
        when(mockScene.play(any(Player.class), eq(mockIO))).thenReturn(SceneID.FIGHT);

        Map<SceneID, Scene> scenes = new EnumMap<>(SceneID.class);
        scenes.put(SceneID.PLAYER_SETUP, mockScene);

        GameEngine engine = new GameEngine(mockIO, scenes);
        engine.run();

        verify(mockIO).println("ERROR: Unknown Scene 'FIGHT'.");
        verify(mockIO).println("                      GAME OVER");
    }

    // -------------------------------------------------------------------------
    // run() — RuntimeException handling
    // -------------------------------------------------------------------------

    /**
     * Verifies that a {@link RuntimeException} thrown inside a scene is caught
     * by {@link GameEngine#run()}, an error message is printed, and the game over
     * banner is still shown.
     */
    @Test
    @DisplayName("run() catches RuntimeException, prints error message, and still shows game over")
    void testRunCatchesRuntimeException() {
        logger.debug("Testing run catches RuntimeException from scene");
        when(mockScene.play(any(Player.class), eq(mockIO)))
                .thenThrow(new RuntimeException("Test exception"));

        Map<SceneID, Scene> scenes = new EnumMap<>(SceneID.class);
        scenes.put(SceneID.PLAYER_SETUP, mockScene);

        GameEngine engine = new GameEngine(mockIO, scenes);
        engine.run();

        verify(mockIO).println(contains("Test exception"));
        verify(mockIO).println("                      GAME OVER");
    }

    // -------------------------------------------------------------------------
    // run() — empty scene map
    // -------------------------------------------------------------------------

    /**
     * Verifies that an empty scene map (no {@code PLAYER_SETUP} scene registered)
     * immediately triggers the unknown-scene error path and still shows game over.
     */
    @Test
    @DisplayName("run() handles an empty scene map gracefully")
    void testRunWithEmptySceneMapPrintsError() {
        logger.debug("Testing run with empty scene map");
        Map<SceneID, Scene> scenes = new EnumMap<>(SceneID.class);

        GameEngine engine = new GameEngine(mockIO, scenes);
        engine.run();

        verify(mockIO).println("ERROR: Unknown Scene 'PLAYER_SETUP'.");
        verify(mockIO).println("                      GAME OVER");
    }
}
