package com.intro.scene;

import com.intro.io.GameIO;
import com.intro.model.Player;

public class AcceptQuestScene implements Scene {

    /**
     * {@inheritDoc}
     * @return {@link SceneID#ACCEPT}
     */
    @Override
    public SceneID getID() {
        return SceneID.ACCEPT;
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
        io.println("You accept the quest and search the forest for the lost amulet. After many hours you stumble across a glade in the woods. At its centre you find the amulet.");
        io.println();
        io.println("You bring it to the wizard, who tells all of your epic deed.");
        io.println();
        io.println("Bards forever more sing the praises of " + player.getName() + ", the hero of the realm!");

        /* Prompts the Game Over text. */
        return null;
    }
}
