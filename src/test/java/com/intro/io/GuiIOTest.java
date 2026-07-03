package com.intro.io;

import com.intro.ui.GamePanel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GuiIO}.
 *
 * <h2>Tested behaviour</h2>
 * <ul>
 *     <li>Constructor validation — {@code null} panel is rejected.</li>
 *     <li>{@link GuiIO#println(String)} — schedules {@link GamePanel#appendNarrativeText(String)}
 *         with the correct text and a trailing newline.</li>
 *     <li>{@link GuiIO#println()} — schedules a bare {@code "\n"} append.</li>
 *     <li>{@link GuiIO#prompt(String)} — appends the prompt text, enables player
 *         input, blocks until {@link GamePanel#awaitPlayerInput()} returns, and
 *         passes the result back to the caller.</li>
 *     <li>Interrupt handling — when {@link GamePanel#awaitPlayerInput()} throws
 *         {@link InterruptedException}, {@link GuiIO#prompt(String)} returns
 *         {@code ""} and re-asserts the thread's interrupt flag.</li>
 * </ul>
 *
 * <h2>Approach: scheduler injection</h2>
 * <p>
 *     In production, {@link GuiIO} dispatches all UI updates via
 *     {@code Platform.runLater()}, which requires a running JavaFX toolkit.
 *     Running a full JavaFX toolkit in a headless test environment (such as a
 *     CI server or Replit) is unreliable and unnecessary for testing this class.
 * </p>
 * <p>
 *     The package-private constructor
 *     {@link GuiIO#GuiIO(GamePanel, java.util.function.Consumer)} accepts a
 *     custom {@code uiScheduler}. Tests inject {@code Runnable::run}, which
 *     executes scheduled tasks <em>synchronously on the test thread</em>.
 *     This means:
 * </p>
 * <ul>
 *     <li>No JavaFX platform is needed.</li>
 *     <li>All calls to {@link GamePanel} methods happen before the assertion
 *         line — no {@code CountDownLatch} or {@code Thread.sleep} required.</li>
 *     <li>Mockito {@link InOrder} verification reliably captures the exact
 *         call sequence within {@link GuiIO#prompt(String)}.</li>
 * </ul>
 * <p>
 *     {@link GamePanel} is a plain Java class (FXML controller, not a JavaFX
 *     node subtype), so Mockito can mock it without any additional configuration.
 * </p>
 *
 * <h2>Parameterization strategy</h2>
 * <p>
 *     The {@code println} tests use {@link ValueSource @ValueSource} to verify
 *     that the trailing-newline rule holds for multiple input strings — a normal
 *     sentence, an empty string, and a string that already ends with a newline —
 *     without duplicating test method bodies.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class GuiIOTest {

    private static final Logger logger = LogManager.getLogger(GuiIOTest.class);

    /**
     * Mockito-managed mock of the FXML controller.
     * Because {@link GamePanel} is a plain (non-final) Java class, Mockito can
     * proxy it without any special configuration.
     */
    @Mock
    private GamePanel mockPanel;

    /**
     * The system under test. Constructed in {@link #setUp()} with
     * {@code Runnable::run} as the scheduler so that all UI tasks run
     * synchronously on the test thread.
     */
    private GuiIO guiIO;

    /**
     * Creates a fresh {@link GuiIO} before each test, injecting
     * {@code Runnable::run} as the UI scheduler so that all
     * {@code uiScheduler.accept(...)} calls execute synchronously.
     */
    @BeforeEach
    void setUp() {
        logger.debug("Setting up GuiIOTest — injecting synchronous scheduler");
        guiIO = new GuiIO(mockPanel, Runnable::run);
    }

    /**
     * Clears any interrupt flag left on the test thread after interrupt-handling
     * tests.  {@link Thread#interrupted()} returns and clears the flag atomically;
     * it is safe to call even when the flag is not set.
     */
    @AfterEach
    void clearInterruptFlag() {
        Thread.interrupted();
    }

    // -------------------------------------------------------------------------
    // Constructor validation
    // -------------------------------------------------------------------------

    /**
     * Verifies that passing {@code null} as the {@link GamePanel} throws
     * {@link IllegalArgumentException} immediately, before any method is called.
     */
    @Test
    @DisplayName("constructor throws IllegalArgumentException for null GamePanel")
    void constructor_throwsForNullGamePanel() {
        logger.debug("Testing constructor null-panel guard");
        assertThrows(IllegalArgumentException.class,
                () -> new GuiIO(null),
                "A null GamePanel should be rejected at construction time");
    }

    /**
     * Verifies that a valid (non-null) {@link GamePanel} mock is accepted by the
     * constructor without throwing.
     */
    @Test
    @DisplayName("constructor accepts a valid GamePanel")
    void constructor_acceptsValidGamePanel() {
        logger.debug("Testing constructor accepts valid panel");
        assertDoesNotThrow(() -> new GuiIO(mockPanel),
                "A non-null GamePanel should be accepted");
    }

    // -------------------------------------------------------------------------
    // println(String) tests
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link GuiIO#println(String)} calls
     * {@link GamePanel#appendNarrativeText(String)} with the supplied text
     * followed by exactly one newline character.
     *
     * <h3>Why parameterized?</h3>
     * <p>
     *     The trailing-newline rule must hold for any text: a typical sentence,
     *     an empty string, and a string that already ends with {@code '\n'}.
     *     Using {@link ValueSource @ValueSource} covers all three cases from a
     *     single method body, so adding further edge cases only requires a new
     *     entry in the annotation.
     * </p>
     *
     * @param text the text passed to {@code println}, supplied by
     *             {@link ValueSource @ValueSource}.
     */
    @ParameterizedTest(name = "println(\"{0}\") appends text + newline")
    @ValueSource(strings = {"Hello, world!", "", "Already ends with newline\n"})
    @DisplayName("println(String) appends text + trailing newline to GamePanel")
    void println_appendsTextWithTrailingNewline(String text) throws Exception {
        logger.debug("Testing println appends '{}' + newline", text);

        guiIO.println(text);

        verify(mockPanel).appendNarrativeText(text + "\n");
    }

    // -------------------------------------------------------------------------
    // println() tests
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link GuiIO#println()} (no-arg overload) calls
     * {@link GamePanel#appendNarrativeText(String)} with exactly {@code "\n"},
     * inserting a blank line into the narrative.
     */
    @Test
    @DisplayName("println() appends a bare newline to GamePanel")
    void println_blankLine_appendsNewlineOnly() throws Exception {
        logger.debug("Testing println() appends bare newline");

        guiIO.println();

        verify(mockPanel).appendNarrativeText("\n");
    }

    // -------------------------------------------------------------------------
    // prompt(String) tests — call sequence
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link GuiIO#prompt(String)} calls
     * {@link GamePanel#appendNarrativeText(String)} with the prompt message
     * followed by a newline.
     */
    @Test
    @DisplayName("prompt(String) appends the prompt message with a trailing newline")
    void prompt_appendsPromptTextWithTrailingNewline() throws Exception {
        logger.debug("Testing prompt appends message text");
        when(mockPanel.awaitPlayerInput()).thenReturn("any");

        guiIO.prompt("What do you do?");

        verify(mockPanel).appendNarrativeText("What do you do?\n");
    }

    /**
     * Verifies that {@link GuiIO#prompt(String)} enables player input via
     * {@link GamePanel#setInputEnabled(boolean) setInputEnabled(true)} before
     * calling {@link GamePanel#awaitPlayerInput()}.
     *
     * <p>
     *     The ordering guarantee is important: input must be unlocked before the
     *     game thread blocks, otherwise the player would see locked controls and
     *     have no way to provide an answer.  {@link InOrder} verifies the exact
     *     sequence: append text → enable input → await input.
     * </p>
     */
    @Test
    @DisplayName("prompt(String) enables input before blocking for player response")
    void prompt_enablesInputBeforeAwaitingPlayerInput() throws Exception {
        logger.debug("Testing prompt enables input before blocking");
        when(mockPanel.awaitPlayerInput()).thenReturn("any");

        guiIO.prompt("Choose a path:");

        InOrder inOrder = inOrder(mockPanel);
        inOrder.verify(mockPanel).appendNarrativeText("Choose a path:\n");
        inOrder.verify(mockPanel).setInputEnabled(true);
        inOrder.verify(mockPanel).awaitPlayerInput();
    }

    // -------------------------------------------------------------------------
    // prompt(String) tests — return value
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link GuiIO#prompt(String)} returns exactly the string
     * produced by {@link GamePanel#awaitPlayerInput()}, passing the player's
     * answer back to the game engine unchanged.
     *
     * <h3>Why parameterized?</h3>
     * <p>
     *     The pass-through contract must hold for a typical answer, an empty
     *     string (player submits without typing), and a multi-word answer with
     *     spaces. {@link ValueSource @ValueSource} covers all three from a
     *     single method body.
     * </p>
     *
     * @param playerInput the value {@link GamePanel#awaitPlayerInput()} is
     *                    stubbed to return, supplied by
     *                    {@link ValueSource @ValueSource}.
     */
    @ParameterizedTest(name = "prompt returns \"{0}\" from GamePanel")
    @ValueSource(strings = {"go north", "", "pick up the sword"})
    @DisplayName("prompt(String) returns whatever awaitPlayerInput() returns")
    void prompt_returnsPlayerInputFromPanel(String playerInput) throws Exception {
        logger.debug("Testing prompt returns '{}'", playerInput);
        when(mockPanel.awaitPlayerInput()).thenReturn(playerInput);

        String result = guiIO.prompt("What next?");

        assertEquals(playerInput, result,
                "prompt() should return the exact value from awaitPlayerInput()");
    }

    // -------------------------------------------------------------------------
    // prompt(String) tests — interrupt handling
    // -------------------------------------------------------------------------

    /**
     * Verifies that when {@link GamePanel#awaitPlayerInput()} throws
     * {@link InterruptedException}, {@link GuiIO#prompt(String)} returns an
     * empty string rather than propagating the exception.
     *
     * <p>
     *     The game engine's {@code run()} method has a {@code finally} block
     *     that still prints the GAME OVER banner, so returning {@code ""} gives
     *     the engine a chance to shut down gracefully.
     * </p>
     */
    @Test
    @DisplayName("prompt() returns empty string when game thread is interrupted")
    void prompt_returnsEmptyStringOnInterrupt() throws Exception {
        logger.debug("Testing prompt returns empty string on interrupt");
        when(mockPanel.awaitPlayerInput()).thenThrow(new InterruptedException());

        String result = guiIO.prompt("A question:");

        assertEquals("", result,
                "prompt() should return an empty string when the thread is interrupted");
    }

    /**
     * Verifies that when {@link GamePanel#awaitPlayerInput()} throws
     * {@link InterruptedException}, {@link GuiIO#prompt(String)} re-asserts
     * the calling thread's interrupt flag before returning.
     *
     * <p>
     *     Swallowing an {@link InterruptedException} without re-asserting the
     *     flag is a well-known Java threading anti-pattern: it silently breaks
     *     cooperative cancellation for any caller that checks
     *     {@link Thread#isInterrupted()} after the call returns.
     * </p>
     *
     * <p>
     *     The {@link AfterEach} method {@link #clearInterruptFlag()} clears the
     *     flag after this test so it does not affect subsequent tests.
     * </p>
     */
    @Test
    @DisplayName("prompt() re-asserts the thread interrupt flag after InterruptedException")
    void prompt_reassertsInterruptFlagOnInterrupt() throws Exception {
        logger.debug("Testing prompt re-asserts interrupt flag");
        when(mockPanel.awaitPlayerInput()).thenThrow(new InterruptedException());

        guiIO.prompt("A question:");

        assertTrue(Thread.currentThread().isInterrupted(),
                "prompt() must re-assert the interrupt flag via Thread.currentThread().interrupt()");
    }

    // -------------------------------------------------------------------------
    // clearOutput() tests
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link GuiIO#clearOutput()} delegates to
     * {@link GamePanel#clearNarrativeText()} via the injected UI scheduler.
     *
     * <p>
     *     Because {@code Runnable::run} is injected as the scheduler in
     *     {@link #setUp()}, the scheduled task runs synchronously, so the
     *     delegation can be verified immediately after the call returns.
     * </p>
     */
    @Test
    @DisplayName("clearOutput() delegates to GamePanel.clearNarrativeText()")
    void clearOutput_delegatesToGamePanelClearNarrativeText() {
        logger.debug("Testing clearOutput delegates to GamePanel.clearNarrativeText()");

        guiIO.clearOutput();

        verify(mockPanel).clearNarrativeText();
    }

    // -------------------------------------------------------------------------
    // printBanner() tests
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link GuiIO#printBanner(String)} delegates to
     * {@link GamePanel#appendBanner(String)} via the injected UI scheduler.
     */
    @Test
    @DisplayName("printBanner() delegates to GamePanel.appendBanner()")
    void printBanner_delegatesToGamePanelAppendBanner() {
        logger.debug("Testing printBanner delegates to GamePanel.appendBanner()");

        guiIO.printBanner("THE ENCHANTED FOREST");

        verify(mockPanel).appendBanner("THE ENCHANTED FOREST");
    }
}
