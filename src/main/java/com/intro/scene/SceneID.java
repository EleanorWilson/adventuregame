package com.intro.scene;

/**
 * Enumeration of every scene identifier in the game.
 */
public enum SceneID {
    /**
     * SceneID for player setup scene.
     */
    PLAYER_SETUP,

    /**
     * SceneID for forest description scene. First main scene of the game, also
     * accessed through the {@code flight} response of the {@link SceneID#CROWS}
     * scene.
     */
    FOREST,

    /**
     * SceneID for scene accessed through {@code friendly} approach of the
     * {@link SceneID#FOREST} scene.
     */
    QUEST,

    /**
     * SceneID for scene accessed through {@code threaten} approach of the
     * {@link SceneID#FOREST} scene.
     */
    CROWS,

    /**
     * SceneID for accept quest scene. Accessed through {@code accept} approach
     * of the {@link SceneID#QUEST} scene.
     */
    ACCEPT,

    /**
     * SceneID for refuse quest scene. Accessed through {@code refuse} approach
     * of the {@link SceneID#QUEST} scene.
     */
    REFUSE,

    /**
     * SceneID for fight scene. Accessed through {@code fight} approach of the
     * {@link SceneID#CROWS} scene.
     */
    FIGHT;
}
