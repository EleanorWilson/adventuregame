package com.intro.io;

/**
 * Abstract class to manage terminal input/output used by the game.
 * This abstract class will be used to prevent the game engine and
 * scene classes from using either {@link System#out} or
 * {@link java.util.Scanner} directly.
 */
public interface GameIO {

    /**
     * Prints {@code text} followed by the new line.
     * @param text line of text to display
     */
    void println(String text);

    /**
     * Prints blank line.
     */
    void println();

    /**
     * Displays {@code message} then reads and returns player's input
     * with whitespace removed.
     * @param message text to display before waiting for player's input
     * @return text entered by the player.
     */
    String prompt(String message);

    /**
     * Clears all narrative text previously displayed to the player.
     *
     * <p>
     *     Intended for use at scene transitions where prior dialogue should
     *     not remain visible once the player moves on — for example,
     *     {@link com.intro.scene.PlayerSetupScene} calls this immediately
     *     before printing the opening banner for {@link com.intro.scene.SceneID#FOREST},
     *     so only the game's own narrative remains on screen once the
     *     character name has been confirmed.
     * </p>
     */
    void clearOutput();

    /**
     * Prints a three-line banner: separator, centered title, separator.
     *
     * <p>
     *     Line width is determined by the IO implementation — the GUI uses the
     *     current narrative area width; console mode uses a fixed config width.
     * </p>
     *
     * @param title the banner title to center; must not be {@code null}
     */
    void printBanner(String title);

}
