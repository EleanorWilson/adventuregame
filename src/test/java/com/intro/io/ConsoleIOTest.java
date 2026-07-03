package com.intro.io;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ConsoleIO}.
 *
 * <p>
 *     The full public contract of each method is verified:
 * </p>
 * <ul>
 *     <li>
 *         {@link ConsoleIO#println(String)} — the supplied text is written to
 *         {@link System#out}.
 *     </li>
 *     <li>
 *         {@link ConsoleIO#println()} — exactly one line separator is written
 *         to {@link System#out}.
 *     </li>
 *     <li>
 *         {@link ConsoleIO#prompt(String)} — the message is printed to
 *         {@link System#out}, and the next line from {@link System#in} is read
 *         and returned with surrounding whitespace trimmed.
 *     </li>
 * </ul>
 *
 * <p>
 *     Each test redirects {@link System#out} to a {@link ByteArrayOutputStream}
 *     in {@link #setUp()} and restores both {@link System#out} and
 *     {@link System#in} in {@link #tearDown()}.
 * </p>
 */
class ConsoleIOTest {

    private static final Logger logger = LogManager.getLogger(ConsoleIOTest.class);

    /** Captures output written to {@link System#out} during each test. */
    private ByteArrayOutputStream capturedOut;

    /** Retains the original {@link System#out} for restoration after each test. */
    private PrintStream originalOut;

    /** Retains the original {@link System#in} for restoration after each test. */
    private InputStream originalIn;

    /** Shared {@link ConsoleIO} instance; tests that need custom input create their own. */
    private ConsoleIO consoleIO;

    @BeforeEach
    void setUp() {
        logger.debug("ConsoleIOTest setUp — redirecting System.out");
        originalOut = System.out;
        originalIn  = System.in;
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut));
        consoleIO = new ConsoleIO();
    }

    @AfterEach
    void tearDown() {
        logger.debug("ConsoleIOTest tearDown — restoring System.out / System.in");
        System.setOut(originalOut);
        System.setIn(originalIn);
    }

    // -------------------------------------------------------------------------
    // println(String)
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link ConsoleIO#println(String)} writes the supplied text
     * to {@link System#out}.
     */
    @Test
    @DisplayName("println(String) writes text to System.out")
    void testPrintlnWithTextWritesToSystemOut() {
        logger.debug("Testing println(String) writes to System.out");
        consoleIO.println("Hello");
        assertTrue(capturedOut.toString().contains("Hello"),
                "Output should contain the printed text");
    }

    // -------------------------------------------------------------------------
    // println()
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link ConsoleIO#println()} writes exactly one line
     * separator to {@link System#out}.
     */
    @Test
    @DisplayName("println() writes a blank line to System.out")
    void testPrintlnNoArgWritesNewline() {
        logger.debug("Testing println() writes blank line");
        consoleIO.println();
        assertEquals(System.lineSeparator(), capturedOut.toString(),
                "println() should write only a newline");
    }

    // -------------------------------------------------------------------------
    // prompt(String) — output side
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link ConsoleIO#prompt(String)} prints the message to
     * {@link System#out} before reading input.
     */
    @Test
    @DisplayName("prompt(String) prints the message to System.out")
    void testPromptPrintsMessage() {
        logger.debug("Testing prompt prints message to System.out");
        System.setIn(new ByteArrayInputStream("input\n".getBytes()));
        ConsoleIO ioWithInput = new ConsoleIO();

        ioWithInput.prompt("Enter something: ");

        assertTrue(capturedOut.toString().contains("Enter something: "),
                "prompt() should print the message to System.out");
    }

    // -------------------------------------------------------------------------
    // prompt(String) — input side
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link ConsoleIO#prompt(String)} reads the next line from
     * {@link System#in} and returns it with surrounding whitespace trimmed.
     */
    @Test
    @DisplayName("prompt() reads from System.in and returns trimmed input")
    void testPromptReadsAndTrimsInput() {
        logger.debug("Testing prompt reads trimmed input from System.in");
        System.setIn(new ByteArrayInputStream("  player input  \n".getBytes()));
        ConsoleIO ioWithInput = new ConsoleIO();

        String result = ioWithInput.prompt("Enter something: ");

        assertEquals("player input", result,
                "prompt() should return the trimmed input from System.in");
    }

    // -------------------------------------------------------------------------
    // clearOutput()
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link ConsoleIO#clearOutput()} writes the ANSI
     * "move cursor home + erase screen" escape sequence ({@code "\033[H\033[2J"})
     * to {@link System#out}.
     */
    @Test
    @DisplayName("clearOutput() writes the ANSI clear-screen escape sequence to System.out")
    void testClearOutputWritesAnsiEscapeSequence() {
        logger.debug("Testing clearOutput writes ANSI escape sequence");
        consoleIO.clearOutput();
        assertEquals("\033[H\033[2J", capturedOut.toString(),
                "clearOutput() should write exactly the ANSI home+erase escape sequence");
    }
}
