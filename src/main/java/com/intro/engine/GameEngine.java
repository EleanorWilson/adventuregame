package com.intro.engine;

import com.intro.io.GameIO;
import com.intro.model.Player;
import com.intro.scene.*;

import java.util.Map;
import java.util.EnumMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This is the core game engine that drives the text adventure.
 */
@SuppressWarnings("LoggingSimilarMessage")
public class GameEngine {

    private static final Logger logger = LogManager.getLogger(GameEngine.class);

    /**
     * Abstraction for input/output.
     */
    private final GameIO io;

    /**
     * Object for storing map of {@link SceneID} to its {@link Scene}
     * implementation.
     */
    private final Map<SceneID, Scene> scenes;

    /**
     * Constructor for a new {@link GameEngine}.
     * @param io {@link GameIO} must not be {@code null}.
     */
    public GameEngine(GameIO io) {
        this.io = io;
        this.scenes = buildSceneMap();
        logger.debug("GameEngine initialised with {} registered scenes", this.scenes.size());
    }

    /**
     * Package-private constructor used by unit tests.
     * <p>
     *     This constructor allows tests to supply mock {@link Scene} instances, keeping each test
     *     fully isolated from real scene implementations.
     * </p>
     * @param io {@link GameIO} implementation (usually a mock)
     * @param scenes pre-built map of {@link SceneID} and {@link Scene}
     */
    GameEngine(GameIO io, Map<SceneID, Scene> scenes) {
        this.io = io;
        this.scenes = scenes;
        logger.debug("GameEngine initialised with {} registered scenes", scenes.size());
    }

    /**
     * Starts a new game and runs the game loop.
     *
     * <p>
     *     A new {@link Player} object is created and the loop begins
     *     at the {@link SceneID#PLAYER_SETUP} scene. On each iteration, the
     *     current scene is searched for, if it exists, it is executed with
     *     the {@link Scene#play(Player, GameIO)} method and the return
     *     value determines the next scene.
     * </p>
     * <p>
     *     The loop ends when either an unknown {@link SceneID} is
     *     encountered, at which point an error message displays, or if the
     *     {@link SceneID} returns {@code null}. Both lead to the game-over
     *     screen being displayed.
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
            this.io.println("An unexpected error occurred: " + e.getMessage());
        } finally {
            logger.info("Game over");
            this.printGameOver();
        }
    }

    /**
     * Displays the game over message.
     */
    public void printGameOver() {
        this.io.println();
        this.io.println("******************************************************");
        this.io.println("                      GAME OVER");
        this.io.println("******************************************************");

    }

    /**
     * Creates and returns a map of {@link SceneID} and {@link Scene}
     * pairs.
     * <p>
     *     Each {@link Scene} is instantiated here and mapped to its
     *     {@link Scene#getID()} key.
     * </p>
     * <p>
     *     Code maintenance: New scenes should have their own classes
     *     that implement the {@link Scene} class. They should be added
     *     to the {@link SceneID} constants class and then added to this
     *     method as {@code new NewSceneNameHere()} so they can be
     *     instantiated and added to the scene map builder.
     * </p>
     * @return map of {@link SceneID} and corresponding {@link Scene}
     */
    private static Map<SceneID, Scene> buildSceneMap() {
        Map<SceneID, Scene> map = new EnumMap<>(SceneID.class);

        // Adding Enums and Scenes
        map.put(SceneID.PLAYER_SETUP, new PlayerSetupScene());
        map.put(SceneID.FOREST, new ForestScene());
        map.put(SceneID.QUEST, new QuestScene());
        map.put(SceneID.CROWS, new CrowsScene());
        map.put(SceneID.ACCEPT, new AcceptQuestScene());
        map.put(SceneID.REFUSE, new RefuseQuestScene());
        map.put(SceneID.FIGHT, new FightScene());

        return map;
    }
}
