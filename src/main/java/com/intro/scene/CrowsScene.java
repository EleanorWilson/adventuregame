package com.intro.scene;

import com.intro.io.GameIO;
import com.intro.model.Player;

/**
 * This scene is accessed through the {@code threaten} option of the
 * {@link SceneID#FOREST} scene.
 * <p>
 *     The mysterious figure summons a murder of crows and readies to attack the
 *     player. The player is given two choices: {@code Fight} or {@code Flight},
 *     leading to one of two endings on the {@code threaten} branch of the story.
 * </p>
 */
public class CrowsScene implements Scene {

    /**
     * {@inheritDoc}
     * @return {@link SceneID#CROWS}
     */
    @Override
    public SceneID getID() {
        return SceneID.CROWS;
    }

    /**
     * {@inheritDoc}
     * <p>
     *     Prints the crows scene. Decision prompt then loops until the player
     *     chooses a valid option:
     *     <ul>
     *         <li>{@code 1} or {@code fight} returns {@link SceneID#FIGHT}</li>
     *         <li>{@code 2} or {@code flight} returns {@link SceneID#FOREST}
     *         <ul><li>Loops back to an earlier scene.</li></ul>
     *         </li>
     *     </ul>
     * </p>
     *
     * @param player current player
     * @param io the {@link GameIO}
     * @return {@link SceneID#FIGHT} or {@link SceneID#FOREST}
     */
    @Override
    public SceneID play(Player player, GameIO io) {
        io.println();
        io.println("You step forward aggressively and take a fighting stance, holding your sword out before you.");
        io.println();
        io.println("The mysterious figure takes a panicked step back before summoning a murder of crows from their dark robes who swirl above the figure, ready to attack.");
        io.println();
        io.println("'Run or die', the figure warns.");
        io.println();
        io.println("How do you respond?");
        io.println("    1. Fight");
        io.println("    2. Flight");

        while (true) {
            String choice = io.prompt("Your choice: ").toLowerCase();
            if (choice.isEmpty()) {
                io.println("You have entered an invalid response, please enter 1 or 2.");
            } else if (choice.charAt(0) == '1' || choice.equals("fight")) {
                return SceneID.FIGHT;
            } else if (choice.charAt(0) == '2' || choice.equals("flight") || choice.equals("flee")) {
                io.println();
                io.println("You choose to flee and race back into the forest, wiser now than before.");
                return SceneID.FOREST;
            } else {
                io.println("You have entered an invalid response, please enter 1 or 2.");
            }
        }
    }
}
