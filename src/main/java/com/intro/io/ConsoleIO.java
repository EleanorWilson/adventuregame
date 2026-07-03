package com.intro.io;

import java.io.PrintStream;
import java.util.Scanner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implementation of the abstract {@link GameIO} class.
 * ConsoleIO writes output to {@link System#out} and reads input from {@link System#in}.
 * All other classes interact with IO through the {@link GameIO} interface.
 * ConsoleIO can be mocked.
 */
@SuppressWarnings("java:S106")
public class ConsoleIO implements GameIO {

    private static final Logger logger = LogManager.getLogger(ConsoleIO.class);

    /**
     * Reads player input.
     */
    private final Scanner scanner = new Scanner(System.in);

    /**
     * {@inheritDoc}
     * @param text line of text to display
     */
    @Override
    public void println(String text) {
        logger.debug("println: '{}'", text);
        System.out.println(text);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void println() {
        System.out.println();
    }

    /**
     * {@inheritDoc}
     * @param message text to display before waiting for player input
     * @return reads next line from player input and trims whitespace.
     */
    @Override
    public String prompt(String message) {
        logger.debug("prompt: '{}'", message);
        System.out.print(message);
        String input = this.scanner.nextLine().trim();
        logger.debug("prompt reply: '{}'", input);
        return input;
    }

    /**
     * Method to clear the narrative text from the text area in the UI, using an ANSI escape sequence:
     * <code>\033[H\033[2J</code>
     * <h2>ANSI Sequence:</h2>
     * There are two commands in the ANSI sequence:
     * <ol>
     *     <li><code>\033[H</code></li>
     *     <li><code>\033[2J</code></li>
     * </ol>
     * Which are doing the following:
     * <ul>
     *     <li><code>\033</code> : Escape character</li>
     *     <li><code>[</code> : Control Sequence Introducer</li>
     *     <li><code>H</code> : Move Cursor to Home: (1,1)</li>
     *     <li><code>2J</code> : Erase Entire Screen</li>
     * </ul>
     *     <blockquote>
     *         <b>N.B.</b>
     *         <p>
     *             {@link PrintStream#flush() System.out.flush()} is required here in order to force
     *             the {@link PrintStream#print(String)  System.out.print()} to be sent immediately to the
     *             terminal, instead of remaining in the buffer (which would wait for a newline before printing).
     *         </p>
     *         <p>
     *             {@link PrintStream#print(String) print()} is used over {@link PrintStream#println(String) println()}
     *             to avoid a blank line being printed before the narrative text.
     *         </p>
     *     </blockquote>
     */
    @Override
    public void clearOutput() {
        logger.debug("clearOutput");
        System.out.print("\033[H\033[2J");
        /* forces immediate output to terminal, without waiting for buffer. */
        System.out.flush();
    }
}
