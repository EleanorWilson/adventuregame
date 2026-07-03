package com.intro.scene;

import com.intro.model.Player;
import com.intro.io.GameIO;

/**
 * Main story scene that describes the setting.
 * <p>
 *     This scene prompts the first choice of the game for the player.
 *     Whether to be {@code friendly} or to {@code threaten}.
 *     This choice determines the path for the rest of the story.
 * </p>
 * <p>
 *     This scene is also the target of the loop if the {@code flight} option is selected in the {@link SceneID#CROWS} scene.
 * </p>
 */
public class ForestScene implements Scene {

    /**
     * {@inheritDoc}
     * @return {@link SceneID#FOREST}
     */
    @Override
    public SceneID getID() {
        return SceneID.FOREST;
    }

    /**
     * {@inheritDoc}
     * <p>
     *     Prints the description of the forest and the cloaked figure.
     *     Decision prompt then loops until the player enters a valid option:
     *     <ul>
     *         <li>{@code 1} or {@code friendly} returns {@link SceneID#QUEST}</li>
     *         <li>{@code 2} or {@code threaten} returns {@link SceneID#CROWS}</li>
     *     </ul>
     * </p>
     * @param player current player
     * @param io the {@link GameIO}
     * @return {@link SceneID#QUEST} or {@link SceneID#CROWS}
     */
    @Override
    public SceneID play(Player player, GameIO io) {
        io.println();
        io.println("You find yourself walking through an enchanted forest. Ancient oaks covered in moss loom overhead. A thick fog clings to the undergrowth and an eerie hum of magic moves in the air.");
        io.println();
        io.println("Ahead a mysterious figure in a dark cloak glides towards you through the mist, blocking your path.");
        io.println();
        io.println("How do you approach the figure?");
        io.println("    1. Friendly");
        io.println("    2. Threaten");

        while(true) {
            String choice = io.prompt("Your choice: ").toLowerCase();
            if (choice.isEmpty()) {
                io.println("You have entered an invalid response, please enter 1 or 2.");
            } else if (choice.charAt(0) == '1' || choice.charAt(0) == 'f') {
                return SceneID.QUEST;
            } else if (choice.charAt(0) == '2' || choice.charAt(0) == 't') {
                return SceneID.CROWS;
            } else {
                io.println("You have entered an invalid response, please enter 1 or 2.");
            }
        }
    }
}

