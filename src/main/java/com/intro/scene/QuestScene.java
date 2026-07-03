package com.intro.scene;

import com.intro.model.Player;
import com.intro.io.GameIO;

/**
 * The scene is accessed through the {@code friendly} option of the {@link SceneID#FOREST} scene.
 * <p>
 *     The mysterious figure from {@link SceneID#FOREST} scene is revealed as a wizard searching
 *     for an amulet to save the realm. The player is given a choice:
 *     <ul>
 *         <li>{@code 1} or {@code accept} which returns {@link SceneID#ACCEPT}</li>
 *         <li>{@code 2} or {@code refuse} which returns {@link SceneID#REFUSE}</li>
 *     </ul>
 *     Either choice leads to one of the endings on the {@code friendly} branch.
 * </p>
 */
public class QuestScene implements Scene {

    /**
     * {@inheritDoc}
     * @return {@link SceneID#QUEST}
     */
    @Override
    public SceneID getID() {
        return SceneID.QUEST;
    }

    /**
     * {@inheritDoc}
     * <p>
     *     Prints the quest offer scene. Decision prompts then loops until the player chooses a valid option:
     *     <ul>
     *         <li>{@code 1} or {@code accept} returns {@link SceneID#ACCEPT}</li>
     *         <li>{@code 2} or {@code refuse} returns {@link SceneID#REFUSE}</li>
     *     </ul>
     * </p>
     * @param player current player
     * @param io the {@link GameIO}
     * @return {@link SceneID#ACCEPT} or {@link SceneID#REFUSE}
     */
    @Override
    public SceneID play(Player player, GameIO io) {
        io.println();
        io.println("You greet the mysterious figure and introduce yourself, 'My name is " + player.getName() + ", how can I help you?'");
        io.println();
        io.println("The mysterious figure lowers their hood and reveals white curly hair and kind eyes.");
        io.println();
        io.println("'Pleased to meet you, "+ player.getName() + ", I am Cadellin, a wizard searching high and low for a relic to save our realm. I have a quest for you. Find the lost Amulet of Omniscience. It is hidden deep within these very woods.'");
        io.println();
        io.println("'Will you accept my quest?'");
        io.println("    1. Accept");
        io.println("    2. Refuse");

        while(true) {
            String choice = io.prompt("Your choice: ").toLowerCase();
            if (choice.isEmpty()) {
                io.println("You have entered an invalid response, please enter 1 or 2.");
            } else if (choice.charAt(0) == '1' || choice.charAt(0) == 'a') {
                return SceneID.ACCEPT;
            } else if (choice.charAt(0) == '2' || choice.charAt(0) == 'r') {
                return SceneID.REFUSE;
            } else {
                io.println("You have entered an invalid response, please enter 1 or 2.");
            }
        }
    }
}
