package com.intro.scene;

import com.intro.model.Player;
import com.intro.io.GameIO;

/**
 * Opening scene of the game, allows player to choose their character's name.
 * <p>
 *     Prompts player to choose a name, displays name to player, allows the
 *     player to change the name. If player satisfied with choice, transitions to
 *     {@link SceneID#FOREST}.
 * </p>
 * <p>
 *     A blank or whitespace-only name is rejected with a prompt to enter a valid name.
 * </p>
 */
public class PlayerSetupScene implements Scene {

    /**
     * {@inheritDoc}
     * @return {@link SceneID#PLAYER_SETUP}
     */
    @Override
    public SceneID getID() {
        return SceneID.PLAYER_SETUP;
    }

    /**
     * {@inheritDoc}
     * <p>
     *     Loops until the Player has decided on their character's name. Player prompted to enter
     *     a name, the name is stored on the {@code player} object and displayed for the player.
     *     Blank or whitespace-only names are rejected. Player then prompted to decide between
     *     two options:
     *     <ul>
     *         <li>{@code 1} or {@code yes} will let player change name</li>
     *         <li>{@code 2} or {@code no} will start the game, returning {@link SceneID#FOREST}</li>
     *     </ul>
     * </p>
     * @param player current player
     * @param io the {@link GameIO}
     * @return {@link SceneID#FOREST} once player has chosen their name.
     */
    @Override
    public SceneID play(Player player, GameIO io) {
        while (true) {
            String name = io.prompt("Enter your character name: ");
            if (name == null || name.isBlank()){
                io.println("Name cannot be blank. Please enter a valid name.");
                continue;
            }
            player.setName(name);
            io.println("Your name is: " + name);
            while (true) {
                String answer = io.prompt("Would you like to change your name?\n    1. Yes\n    2. No").toLowerCase();
                if (answer.isEmpty()) {
                    io.println("You have entered an invalid response, please enter 1 or 2.");
                } else if (answer.charAt(0) == 'y' || answer.charAt(0) == '1') {
                    break;
                } else if (answer.charAt(0) == 'n' || answer.charAt(0) == '2') {
                    io.println();
                    io.println("******************************************************");
                    io.println("                THE ENCHANTED FOREST");
                    io.println("******************************************************");
                    return SceneID.FOREST;
                } else {
                    io.println("You have entered an invalid response, please try again.");
                }
            }
        }
    }
}
