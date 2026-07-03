package com.intro.engine;

import com.intro.io.GameIO;
import com.intro.model.Player;
import com.intro.scene.*;
import java.util.EnumMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



/**
 * Core game engine that drives the text adventure.
 *
 * <p>
 *     Owns the scene registry and the main game loop. Scenes are registered
 *     once at construction via {@link #buildSceneMap()} and looked up by
 *     {@link SceneID} on each loop iteration. All player-facing output is
 *     routed through the injected {@link GameIO} so the engine is decoupled
 *     from whether it is running in GUI or console mode.
 * </p>
 */
public class GameEngine {

    private static final Logger logger = LogManager.getLogger(GameEngine.class);

    /** Abstraction for input/output. */
    private final GameIO io;

    /** Maps each {@link SceneID} to its {@link Scene} implementation. */
    private final Map<SceneID, Scene> scenes;

    /**
     * Production constructor. Wires {@code io} and builds the full scene
     * registry via {@link #buildSceneMap()}.
     *
     * @param io {@link GameIO} implementation; must not be {@code null}.
     */
    public GameEngine(GameIO io) {
        this.io = io;
        this.scenes = buildSceneMap();
        logger.debug("GameEngine initialised with {} registered scenes", this.scenes.size());
    }

    /**
     * Package-private constructor used by unit tests.
     *
     * <p>
     *     Bypasses {@link #buildSceneMap()} so tests can supply mock
     *     {@link Scene} instances, keeping each test fully isolated from real
     *     scene implementations without needing to drive IO through them.
     * </p>
     *
     * @param io     {@link GameIO} implementation, typically a mock in tests.
     * @param scenes pre-built map of {@link SceneID} to {@link Scene}; may be empty.
     */
    GameEngine(GameIO io, Map<SceneID, Scene> scenes) {
        this.io = io;
        this.scenes = scenes;
        logger.debug("GameEngine initialised via test constructor with {} registered scenes",
                scenes.size());
    }

    /**
     * Starts a new game and runs the game loop.
     *
     * <p>
     *     A fresh {@link Player} is created and the loop begins at
     *     {@link SceneID#PLAYER_SETUP}. On each iteration the current scene is
     *     looked up, played via {@link Scene#play(Player, GameIO)}, and its
     *     return value determines the next scene.
     * </p>
     * <p>
     *     The loop ends when a scene returns {@code null} (clean game-over) or
     *     when an unregistered {@link SceneID} is encountered (error path).
     *     Either way, the game-over banner is printed via the {@code finally}
     *     block. Any {@link RuntimeException} escaping a scene is caught,
     *     reported to the player, and also ends the loop.
     * </p>
     */
    public void run() {
        logger.info("Game started");
        Player player = new Player();
        SceneID currentSceneID = SceneID.PLAYER_SETUP;

        try {
            while (currentSceneID != null) {
                logger.debug("Entering scene: {}", currentSceneID);
                Scene scene = this.scenes.get(currentSceneID);
                if (scene == null) {
                    logger.error("No scene registered for ID: {}", currentSceneID);
                    this.io.println("ERROR: Unknown Scene '" + currentSceneID + "'.");
                    break;
                }
                currentSceneID = scene.play(player, this.io);
            }
        } catch (RuntimeException e) {
            logger.error("Unexpected runtime error during game execution", e);
            this.io.println("An unexpected error occured: " + e.getMessage());
        } finally {
            logger.info("Game over");
            this.printGameOver();
        }
    }

    /**
     * Prints the game-over banner to {@link #io}.
     */
    public void printGameOver() {
        this.io.println();
        this.io.printBanner("GAME OVER");
    }

    /**
     * Creates and returns the scene registry for a new game.
     *
     * <p>
     *     This method is {@code static} because it reads no instance state — it
     *     is a pure factory. Keeping it {@code static} prevents the
     *     "overridable method called from constructor" family of issues and
     *     makes the intent explicit: the result depends only on the scene
     *     classes themselves, not on the object being constructed.
     * </p>
     *
     * <p>
     *     To register a new scene: implement {@link Scene}, add its constant to
     *     {@link SceneID}, then add a {@code map.put(...)} line here.
     * </p>
     *
     * @return a fully populated {@link EnumMap} of {@link SceneID} to {@link Scene}.
     */
    private static Map<SceneID, Scene> buildSceneMap() {
        Map<SceneID, Scene> map = new EnumMap<>(SceneID.class);

        map.put(SceneID.PLAYER_SETUP, new PlayerSetupScene());
        map.put(SceneID.FOREST,       new ForestScene());
        map.put(SceneID.QUEST,        new QuestScene());
        map.put(SceneID.CROWS,        new CrowsScene());
        map.put(SceneID.ACCEPT,       new AcceptQuestScene());
        map.put(SceneID.REFUSE,       new RefuseQuestScene());
        map.put(SceneID.FIGHT,        new FightScene());

        return map;
    }
}
