package com.intro.engine;

import com.intro.io.GameIO;
import com.intro.model.Player;
import com.intro.scene.SceneID;
import com.intro.scene.Scene;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.EnumMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class GameEngineTest {

    private static final Logger logger = LogManager.getLogger(GameEngineTest.class);

    @Mock
    private GameIO mockIO;

    @Mock
    private Scene mockScene;

    @BeforeEach
    void setUp() {
        logger.debug("Setting up GameEngineTest");
        // TODO check mocks instantiated before each automatically with @ExtendWith tag
    }

    /**
     * Method for creating the scene map with {@link SceneID#PLAYER_SETUP} key and {@link GameEngineTest#mockScene}.
     * @return Map<SceneID, Scene> {@code scenes} map
     */
    private Map<SceneID, Scene> mapSetUp() {
        Map<SceneID, Scene> scenes = new EnumMap<>(SceneID.class);
        scenes.put(SceneID.PLAYER_SETUP, this.mockScene);
        return scenes;
    }

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

    @Test
    @DisplayName("run() calls play() on first scene, ends when null returned")
    void testRunCallsPlayOnFirstScene() {
        logger.debug("Testing run calls play on first scene");
        when(mockScene.play(any(Player.class), eq(mockIO))).thenReturn(null);
        Map<SceneID, Scene> scenes = mapSetUp();
        GameEngine engine = new GameEngine(mockIO, scenes);
        engine.run();
        verify(mockScene, times(1)).play(any(Player.class), eq(mockIO));
    }

    @Test
    @DisplayName("run() moves between scenes until null returned")
    void testRunMovesBetweenScenes() {
        logger.debug("Testing run transitions between two scenes.");
        Scene secondScene = mock(Scene.class);
        when(mockScene.play(any(Player.class), eq(mockIO))).thenReturn(SceneID.FOREST);
        when(secondScene.play(any(Player.class), eq(mockIO))).thenReturn(null);

        Map<SceneID, Scene> scenes = mapSetUp();
        scenes.put(SceneID.FOREST, secondScene);

        GameEngine engine = new GameEngine(mockIO, scenes);
        engine.run();

        verify(mockScene, times(1)).play(any(Player.class), eq(mockIO));
        verify(secondScene, times(1)).play(any(Player.class), eq(mockIO));
    }

    @Test
    @DisplayName("run() always prints game over on normal exit")
    void testRunAlwaysPrintsGameOverOnNormalExit() {
        logger.debug("Testing run always prints game over");
        // when mock scene return null
        // verify times(2) println "******************************************************"
        // verify times(1) println "                      GAME OVER"
        when(mockScene.play(any(Player.class), eq(mockIO))).thenReturn(null);
        Map<SceneID, Scene> scenes = mapSetUp();
        GameEngine engine = new GameEngine(mockIO, scenes);
        engine.run();
        verify(mockIO).println();
        verify(mockIO, times(2)).println("******************************************************");
        verify(mockIO).println("                      GAME OVER");
    }

    @Test
    @DisplayName("run() prints error and ends when SceneID has no registered scene")
    void testRunPrintsErrorForUnknownScene() {
        logger.debug("Testing run handles missing scene");
        when(mockScene.play(any(Player.class), eq(mockIO))).thenReturn(SceneID.FIGHT);
        // PLAYER_SETUP scene added, no FIGHT scene added
        Map<SceneID, Scene> scenes = mapSetUp();
        GameEngine engine = new GameEngine(mockIO, scenes);
        engine.run();
        verify(mockIO).println("ERROR: Unknown Scene 'FIGHT'.");
        verify(mockIO).println();
        verify(mockIO, times(2)).println("******************************************************");
        verify(mockIO).println("                      GAME OVER");
    }

    /**
     * Run() should catch RuntimeException, display an error message and show the game over scene
     */
    @Test
    @DisplayName("run() catches RuntimeException")
    void testRunCatchesRuntimeException() {
        logger.debug("Testing run catches RuntimeException from scene");
        when(mockScene.play(any(Player.class), eq(mockIO))).thenThrow(new RuntimeException("Test Exception"));
        Map<SceneID, Scene> scenes = mapSetUp();
        GameEngine engine = new GameEngine(mockIO, scenes);
        engine.run();
        verify(mockIO).println(contains("Test Exception"));
        verify(mockIO).println();
        verify(mockIO, times(2)).println("******************************************************");
        verify(mockIO).println("                      GAME OVER");
    }

    @Test
    @DisplayName("run() handles empty scene map")
    void testRunWithEmptySceneMapPrintsError() {
        logger.debug("Testing run with empty scene map");
        Map<SceneID, Scene> scenes = new EnumMap<>(SceneID.class);
        GameEngine engine = new GameEngine(mockIO, scenes);
        engine.run();
        verify(mockIO).println(contains("ERROR: Unknown Scene"));
        verify(mockIO).println(contains("GAME OVER"));
    }
}
