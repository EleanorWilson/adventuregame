package com.intro.scene;

import com.intro.io.GameIO;
import com.intro.model.Player;

/**
 * Each implementation of this interface should {@code @Override} the
 * {@link Scene#play(Player, GameIO)} method. Implementations will handle
 * their own narrative text displays, prompts and inputs. They should
 * return the unique ID of the next scene.
 */
public interface Scene {

    /**
     * Returns the unique ID for each scene, which must match one of the
     * SceneID listed in {@link SceneID}.
     */
    SceneID getID();

    /**
     * Executes the scene, displaying any narrative text and processing any
     * decision prompts. Player inputs determine which scene will be played
     * next.
     * @param player current player
     * @param io the {@link GameIO}
     * @return the unique ID of the next scene.
     */
    SceneID play(Player player, GameIO io);

}
