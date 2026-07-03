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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleIOTest {

    private static final Logger logger = LogManager.getLogger(ConsoleIOTest.class);

    /**
     * Captures output written to {@link System#out}
     */
    private ByteArrayOutputStream capturedOut;

    /**
     * Original class's {@link System#out} for restoring after each test.
     */
    private PrintStream classOut;

    /**
     * Original class's {@link System#in} for restoring after each test.
     */
    private InputStream classIn;

    private ConsoleIO io;

    @BeforeEach
    void setUp() {
        logger.debug("Setting up ConsoleIOTest - redirecting System.out");
        classOut = System.out;
        classIn = System.in;
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut));
        io = new ConsoleIO();
    }

    @AfterEach
    void cleanUp() {
        logger.debug("Cleaning up ConsoleIOTest and resetting Sys in/out");
        System.setOut(classOut);
        System.setIn(classIn);
    }


    @Test
    @DisplayName("Println(String) writes to System.out")
    void testPrintlnWritesToSystemOut() {
        logger.debug("Testing println(String) writes to System.out");
        io.println("test");
        assertTrue(capturedOut.toString().contains("test"), "Output should contain the printed text.");
    }

    @Test
    @DisplayName("Println() prints a blank line")
    void testPrintlnPrintsBlankLine() {
        logger.debug("Testing println() prints blank line");
        io.println();
        assertEquals(System.lineSeparator(), capturedOut.toString(), "println() should write only a newline");
    }

    // console io reads line from system in & trims whitespace
    @Test
    @DisplayName("prompt() reads System.in and returns with whitespace trimmed")
    void testPromptReadsInputAndTrims() {
        logger.debug("Testing prompt() reads System.in input and trims leading/trailing whitespace");
        String whitespaceInput = "  whitespace test  ";
        System.setIn(new ByteArrayInputStream(whitespaceInput.getBytes()));
        ConsoleIO promptTestIO = new ConsoleIO();
        String result = promptTestIO.prompt("Test prompt");
        assertEquals("whitespace test", result, "prompt() should return trimmed System.in input");
    }

}
