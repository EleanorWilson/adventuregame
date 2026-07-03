package com.intro.io;

/**
 * Abstract class to manage terminal input/output used by the game.
 * This abstract class will be used to prevent the game engine and
 * scene classes from using either {@link System#out} or
 * {@link java.util.Scanner} directly.
 */
public interface GameIO {

    /**
     * Prints {@code text} followed by new line.
     * @param text line of text to display
     */
    void println(String text);

    /**
     * Prints blank line.
     */
    void println();

    /**
     * Displays {@code message} then reads and returns player's input with whitespace removed.
     * @param message text to display before waiting for player input
     * @return text entered by the player
     */
    String prompt(String message);

    /**
     * Clears all narrative text previously displayed to the player.
     * <p>
     *     Intended for use at any scene transitions where prior dialogue should not remain visible
     *     once the player moves on, e.g. {@link com.intro.scene.PlayerSetupScene} once the name
     *     has been confirmed.
     * </p>
     */
    void clearOutput();

}
