package com.intro.scene;

import com.intro.io.GameIO;
import com.intro.model.Player;

/**
 * Accessed through the {@code Fight} option of the {@link SceneID#CROWS} scene.
 */
public class FightScene implements Scene {

    /**
     * {@inheritDoc}
     * @return {@link SceneID#FIGHT}
     */
    @Override
    public SceneID getID() {
        return SceneID.FIGHT;
    }

    /**
     * Prints the narrative text and returns {@code null} to prompt the game over text.
     * @param player current player
     * @param io the {@link GameIO}
     * @return {@code null}
     */
    @Override
    public SceneID play(Player player, GameIO io) {
        io.println();
        io.println("You choose to fight and begin to rush at the figure. Before you are able to swing your sword, the crows dive towards you and begin their attack.");
        io.println();
        io.println("You are killed...");
        io.println();

        /* Prompts the Game Over text. */
        return null;
    }
}
