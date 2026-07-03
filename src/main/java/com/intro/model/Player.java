package com.intro.model;

/**
 * Class to represent the player and hold character data.
 */
public class Player {

    /**
     * Player character name, initialised as an empty string until set by player.
     */
    private String name;

    /**
     * Creates a new {@code Player} object and sets {@code name} to an empty string.
     */
    public Player() {
        this.name = "";
    }

    /**
     * Returns the player character's name. This should never be {@code null}.
     * @return Player's current name.
     *
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the player character's name, or updates it.
     * @param name the new character name
     */
    public void setName(String name) {
        this.name = name;
    }

}
