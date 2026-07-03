package com.intro.scene;

import com.intro.model.Player;
import com.intro.io.GameIO;

/**
 * Accessed through the {@code refuse} option of the {@link SceneID#QUEST} scene.
 */
public class RefuseQuestScene implements Scene {

    /**
     * {@inheritDoc}
     * @return {@link SceneID#REFUSE}
     */
    @Override
    public SceneID getID() {
        return SceneID.REFUSE;
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
        io.println("You reject the quest and the wizard bows their head in sorrow, letting you pass.");
        io.println();
        io.println("You continue on your journey through the forest, unchanged.");

        /* Prompts the Game Over text. */
        return null;
    }
}