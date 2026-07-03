package com.intro.scene;

/**
 * Class to store unique identifiers for each scene. Scene IDs should only need to be changed here.
 */
public enum SceneID {
    /**
     * Scene where Player chooses character name.
     */
    PLAYER_SETUP,

    /**
     * Scene that describes forest and mysterious figure.
     * Gives decision tree: friendly or threatening approach.
     */
    FOREST,

    /**
     * Scene accessed through the friendly approach.
     * Mysterious figure gives quest for amulet.
     * Gives decision tree: accept or refuse.
     */
    QUEST,

    /**
     * Scene accessed through threatening approach.
     * Mysterious figure summons a murder of crows.
     * Gives decision tree: fight or flight.
     */
    CROWS,

    /**
     * Ending scene accessed by accepting quest for amulet.
     */
    ACCEPT,

    /**
     * Ending scene accessed by refusing quest for amulet.
     */
    REFUSE,

    /**
     * Ending scene accessed by choosing to fight.
     */
    FIGHT
}
