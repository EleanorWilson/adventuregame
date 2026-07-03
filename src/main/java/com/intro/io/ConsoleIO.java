package com.intro.io;

import com.intro.config.GameConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Scanner;

/**
 * Console-based implementation of {@link GameIO}.
 *
 * <p>
 *     Writes output to {@link System#out} and reads player input from
 *     {@link System#in}. All game-engine and scene classes interact with IO
 *     exclusively through the {@link GameIO} interface; they have no
 *     knowledge of whether output is going to a terminal or to the JavaFX
 *     GUI via {@link GuiIO}.
 * </p>
 *
 * <p>
 *     Text is written to the terminal as-is. Line wrapping, if needed, is
 *     the responsibility of the terminal emulator. The JavaFX
 *     {@link com.intro.ui.GamePanel} TextArea handles wrapping automatically,
 *     so no manual character-limit wrapping is performed here either.
 * </p>
 *
 * <p>
 *     <strong>Why {@link System#out} is used directly in this class:</strong>
 *     {@code ConsoleIO} is the game's console-mode <em>display layer</em>, not
 *     application logging code. {@link System#out} is the correct mechanism
 *     for writing player-facing narrative to a terminal. Routing game text
 *     through a logger would prepend timestamps and level labels, route output
 *     through log appenders instead of stdout, and break the player experience.
 *     The "replace System.out with logger" inspection is suppressed here
 *     because it is a known false positive for intentional terminal output.
 * </p>
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class ConsoleIO implements GameIO {

    private static final Logger logger = LogManager.getLogger(ConsoleIO.class);

    /** Reads lines typed by the player on {@link System#in}. */
    private final Scanner scanner = new Scanner(System.in);

    /**
     * Prints {@code text} followed by a line separator to {@link System#out}.
     *
     * @param text the narrative or descriptive text to display.
     */
    @Override
    public void println(String text) {
        logger.debug("println: [{}]", text);
        System.out.println(text);
    }

    /**
     * Prints a blank line to {@link System#out}.
     */
    @Override
    public void println() {
        System.out.println();
    }

    /**
     * Displays {@code message} on {@link System#out}, reads the next line
     * from {@link System#in}, trims surrounding whitespace, and returns the
     * result.
     *
     * @param message the prompt text to display before waiting for input.
     * @return the player's trimmed input; never {@code null}, but may be
     *         empty if the player pressed Enter without typing.
     */
    @Override
    public String prompt(String message) {
        logger.debug("prompt: [{}]", message);
        System.out.println(message);
        String input = this.scanner.nextLine().trim();
        logger.debug("prompt received: [{}]", input);
        return input;
    }

    /**
     * Clears the terminal screen using the ANSI escape sequence
     * {@code "\033[H\033[2J"} (move cursor to home position, then erase the
     * entire screen), followed by a flush of {@link System#out}.
     *
     * <p>
     *     This is a best-effort operation: most modern terminal emulators
     *     (including the one used by the Replit workspace) honour these
     *     standard ANSI codes, but a terminal that does not support ANSI
     *     escape sequences will simply display the raw escape characters or
     *     ignore them — there is no reliable cross-platform way to clear an
     *     arbitrary terminal without depending on a native process (e.g.
     *     invoking {@code cls} on Windows), which this class deliberately
     *     avoids to keep {@link ConsoleIO} dependency-free and portable.
     * </p>
     */
    @Override
    public void clearOutput() {
        logger.debug("clearOutput: writing ANSI clear-screen sequence");
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /**
     * Prints a three-line banner using a fixed line width from
     * {@code io.console.line.width} in {@code config.properties}.
     *
     * @param title the banner title to center; must not be {@code null}
     */
    @Override
    public void printBanner(String title) {
        logger.debug("printBanner: [{}]", title);
        int width = GameConfig.getInt("io.console.line.width", 80);
        println(NarrativeFormatter.separatorLine(width));
        println(NarrativeFormatter.centeredLine(title, width));
        println(NarrativeFormatter.separatorLine(width));
    }
}
