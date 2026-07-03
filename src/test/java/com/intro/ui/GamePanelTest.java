package com.intro.ui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javafx.geometry.Insets;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link GamePanel}.
 *
 * <h2>What is tested here</h2>
 * <p>
 *     {@link GamePanel} is a JavaFX FXML controller. Its {@code @FXML}-annotated
 *     fields ({@link javafx.scene.control.TextArea}, {@link javafx.scene.control.TextField},
 *     {@link javafx.scene.control.Button}) are only injected when the class is
 *     instantiated via {@link javafx.fxml.FXMLLoader}. Methods that touch those
 *     fields ({@code appendNarrativeText}, {@code setInputEnabled}) therefore
 *     require a running JavaFX platform and are exercised through integration
 *     testing with the full application.
 * </p>
 * <p>
 *     Two areas of {@link GamePanel} are pure Java and fully testable without
 *     any JavaFX runtime:
 * </p>
 * <ol>
 *     <li>
 *         <strong>Input queue concurrency</strong> — the
 *         {@link LinkedBlockingQueue} contract of {@link GamePanel#awaitPlayerInput()}:
 *         blocking, unblocking, capacity, and interrupt handling.
 *     </li>
 *     <li>
 *         <strong>Window control delegation</strong> — the three handler methods
 *         ({@link GamePanel#handleMinimise()}, {@link GamePanel#handleMaximise()},
 *         {@link GamePanel#handleClose()}) must delegate to
 *         {@link WindowControls} and nothing else. Testing them with a Mockito
 *         mock of {@link WindowControls} (a plain Java interface) avoids any
 *         need to mock the JavaFX {@link javafx.stage.Stage} class, whose static
 *         initialisers may attempt to load native graphics libraries.
 *     </li>
 * </ol>
 *
 * <h2>Why reflection is used for the input queue</h2>
 * <p>
 *     The {@code inputQueue} field is {@code private final}, which is the
 *     correct access level for a field that should never be touched by external
 *     code in production. In tests, however, we need to place values into the
 *     queue to simulate what {@code handleSubmit()} would do on the JavaFX
 *     Application Thread. Using {@link Field#setAccessible(boolean)} is
 *     acceptable here because the alternative — making {@code inputQueue}
 *     package-private or exposing it through a getter — would weaken the
 *     production API purely to satisfy a test.
 * </p>
 *
 * <h2>Why the window-control handlers are package-private</h2>
 * <p>
 *     {@link GamePanel#handleMinimise()}, {@link GamePanel#handleMaximise()},
 *     and {@link GamePanel#handleClose()} are annotated with {@code @FXML} but
 *     have package-private (default) access. {@code @FXML} works at any
 *     visibility level; the {@link javafx.fxml.FXMLLoader} uses reflection with
 *     {@code setAccessible(true)} to invoke them regardless. Making them
 *     package-private rather than private allows tests in this same package to
 *     call them directly — simulating a button click without needing the JavaFX
 *     event system.
 * </p>
 *
 * <h2>Constructing GamePanel without FXMLLoader</h2>
 * <p>
 *     {@code new GamePanel()} (using the implicit no-arg constructor) works
 *     because {@code inputQueue} is initialised in its field declaration:
 * </p>
 * <pre>{@code
 *     private final LinkedBlockingQueue<String> inputQueue = new LinkedBlockingQueue<>(1);
 * }</pre>
 * <p>
 *     The {@code @FXML} fields will be {@code null}, but none of the methods
 *     tested here touch them. Any test that calls {@code appendNarrativeText} or
 *     {@code setInputEnabled} would throw {@link NullPointerException} and should
 *     not be added here — those methods belong in integration tests.
 * </p>
 *
 * <h2>Parameterization strategy</h2>
 * <p>
 *     The return-value test uses {@link ValueSource @ValueSource} to confirm that
 *     {@code awaitPlayerInput()} faithfully delivers any string from the queue —
 *     a normal word, an empty string (player submits without typing), and a
 *     sentence with spaces — without duplicating the test method body.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class GamePanelTest {

    private static final Logger logger = LogManager.getLogger(GamePanelTest.class);

    /**
     * The {@link GamePanel} instance under test.
     * Constructed directly (not via {@link javafx.fxml.FXMLLoader}); the
     * {@code @FXML} fields will be {@code null}.
     */
    private GamePanel panel;

    /**
     * The private {@code inputQueue} field extracted via reflection so tests
     * can simulate the Submit event without needing the JavaFX Application Thread.
     */
    private LinkedBlockingQueue<String> inputQueue;

    /**
     * Mockito-managed mock of {@link WindowControls}.
     *
     * <p>
     *     {@link WindowControls} is a plain Java interface, so Mockito can mock
     *     it trivially without touching any JavaFX classes. This mock is injected
     *     into the panel via {@link GamePanel#setWindowControls(WindowControls)}
     *     in each window-control test, replacing the production implementation
     *     (which calls real {@link javafx.stage.Stage} methods).
     * </p>
     */
    @Mock
    private WindowControls mockWindowControls;

    /**
     * Creates a fresh {@link GamePanel} and extracts its {@code inputQueue}
     * field via reflection before each test.
     *
     * @throws Exception if the {@code inputQueue} field cannot be found or
     *                   made accessible (indicates a change in {@link GamePanel}'s
     *                   field name or type).
     */
    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        logger.debug("Setting up GamePanelTest — constructing panel and reflecting inputQueue");
        panel = new GamePanel();

        Field queueField = GamePanel.class.getDeclaredField("inputQueue");
        queueField.setAccessible(true);
        inputQueue = (LinkedBlockingQueue<String>) queueField.get(panel);
    }

    // -------------------------------------------------------------------------
    // setStage() — null guard
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link GamePanel#setStage(javafx.stage.Stage)} rejects a
     * {@code null} argument immediately with {@link IllegalArgumentException}.
     *
     * <p>
     *     A {@code null} stage would cause a {@link NullPointerException} deep
     *     inside the drag handlers at the point the player first tries to move
     *     the window — a confusing, hard-to-diagnose failure. Failing fast at
     *     the setter boundary produces a clear error at the correct call site.
     * </p>
     */
    @Test
    @DisplayName("setStage(null) throws IllegalArgumentException")
    void setStage_throwsForNullStage() {
        logger.debug("Testing setStage(null) throws");
        assertThrows(IllegalArgumentException.class,
                () -> panel.setStage(null),
                "setStage(null) must throw IllegalArgumentException");
    }

    // -------------------------------------------------------------------------
    // setWindowControls() — null guard
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link GamePanel#setWindowControls(WindowControls)} rejects
     * a {@code null} argument immediately with {@link IllegalArgumentException}.
     *
     * <p>
     *     A {@code null} {@link WindowControls} would cause a
     *     {@link NullPointerException} the first time a window-control button is
     *     clicked. Failing fast here produces a clear error during application
     *     startup rather than a runtime crash when the user interacts with the UI.
     * </p>
     */
    @Test
    @DisplayName("setWindowControls(null) throws IllegalArgumentException")
    void setWindowControls_throwsForNullControls() {
        logger.debug("Testing setWindowControls(null) throws");
        assertThrows(IllegalArgumentException.class,
                () -> panel.setWindowControls(null),
                "setWindowControls(null) must throw IllegalArgumentException");
    }

    // -------------------------------------------------------------------------
    // handleMinimise() — delegation
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link GamePanel#handleMinimise()} delegates to
     * {@link WindowControls#minimise()} and nothing else.
     *
     * <p>
     *     The mock's {@code minimise()} call is verified with
     *     {@link org.mockito.Mockito#verify(Object)}. Mockito's strict stubbing
     *     mode (active via {@link MockitoExtension}) would fail the test if any
     *     other {@link WindowControls} method was called unexpectedly.
     * </p>
     */
    @Test
    @DisplayName("handleMinimise() calls WindowControls.minimise()")
    void handleMinimise_callsWindowControlsMinimise() {
        logger.debug("Testing handleMinimise delegates to WindowControls.minimise()");
        panel.setWindowControls(mockWindowControls);

        panel.handleMinimise();

        verify(mockWindowControls).minimise();
    }

    // -------------------------------------------------------------------------
    // handleMaximise() — delegation
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link GamePanel#handleMaximise()} delegates to
     * {@link WindowControls#toggleMaximise()} and nothing else.
     *
     * <p>
     *     The decision of whether to maximise or restore is encapsulated inside
     *     the {@link WindowControls} implementation in {@link GameWindow}; this
     *     handler's only job is to call {@code toggleMaximise()}. The test
     *     confirms exactly that contract.
     * </p>
     */
    @Test
    @DisplayName("handleMaximise() calls WindowControls.toggleMaximise()")
    void handleMaximise_callsWindowControlsToggleMaximise() {
        logger.debug("Testing handleMaximise delegates to WindowControls.toggleMaximise()");
        panel.setWindowControls(mockWindowControls);

        panel.handleMaximise();

        verify(mockWindowControls).toggleMaximise();
    }

    // -------------------------------------------------------------------------
    // handleClose() — delegation
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link GamePanel#handleClose()} delegates to
     * {@link WindowControls#close()} and nothing else.
     *
     * <p>
     *     In production, {@link WindowControls#close()} calls
     *     {@link Platform#exit()} and {@link System#exit(int)}, which would
     *     terminate the test JVM. The mock intercepts the call safely, allowing
     *     the delegation contract to be verified without side effects.
     * </p>
     */
    @Test
    @DisplayName("handleClose() calls WindowControls.close()")
    void handleClose_callsWindowControlsClose() {
        logger.debug("Testing handleClose delegates to WindowControls.close()");
        panel.setWindowControls(mockWindowControls);

        panel.handleClose();

        verify(mockWindowControls).close();
    }

    // -------------------------------------------------------------------------
    // awaitPlayerInput() — blocking behaviour
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link GamePanel#awaitPlayerInput()} blocks the calling
     * thread until an item is placed into the queue.
     *
     * <p>
     *     The call is made from a {@link CompletableFuture} running on a
     *     separate thread. The test asserts the future is not yet done (i.e. the
     *     thread is blocked), then offers a value to the queue and asserts the
     *     future completes within a generous timeout.
     * </p>
     */
    @Test
    @DisplayName("awaitPlayerInput() blocks until an item is placed in the queue")
    void awaitPlayerInput_blocksUntilInputIsOffered() throws Exception {
        logger.debug("Testing awaitPlayerInput blocks before input arrives");

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                return panel.awaitPlayerInput();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        });

        assertFalse(future.isDone(),
                "awaitPlayerInput() should block — future must not complete before input is offered");

        inputQueue.put("trigger");

        assertEquals("trigger", future.get(2, TimeUnit.SECONDS),
                "awaitPlayerInput() should unblock and return the offered value");
    }

    // -------------------------------------------------------------------------
    // awaitPlayerInput() — return value
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link GamePanel#awaitPlayerInput()} returns exactly the
     * string that was placed into the queue, with no modification.
     *
     * <h3>Why parameterized?</h3>
     * <p>
     *     The pass-through contract must hold for a normal answer, an empty
     *     string (player submits without typing), and an answer with spaces.
     *     {@link ValueSource @ValueSource} covers all three from a single method
     *     body; adding further cases requires only a new string in the annotation.
     * </p>
     *
     * @param input the value placed into the queue, supplied by
     *              {@link ValueSource @ValueSource}.
     * @throws Exception if the queue interaction or assertion fails unexpectedly.
     */
    @ParameterizedTest(name = "awaitPlayerInput() returns \"{0}\"")
    @ValueSource(strings = {"go north", "", "pick up the sword"})
    @DisplayName("awaitPlayerInput() returns the exact string from the queue")
    void awaitPlayerInput_returnsExactStringFromQueue(String input) throws Exception {
        logger.debug("Testing awaitPlayerInput returns '{}'", input);

        inputQueue.put(input);
        String result = panel.awaitPlayerInput();

        assertEquals(input, result,
                "awaitPlayerInput() must return the value exactly as placed in the queue");
    }

    // -------------------------------------------------------------------------
    // awaitPlayerInput() — interrupt handling
    // -------------------------------------------------------------------------

    /**
     * Verifies that when the blocking thread is interrupted,
     * {@link GamePanel#awaitPlayerInput()} throws {@link InterruptedException}
     * rather than swallowing it silently.
     *
     * <p>
     *     The method is called on a background thread. After confirming the
     *     thread is blocked, it is interrupted. An {@link AtomicBoolean} flag
     *     is used to communicate the outcome back to the test thread, since
     *     assertions thrown in background threads do not automatically fail the
     *     JUnit test.
     * </p>
     */
    @Test
    @DisplayName("awaitPlayerInput() throws InterruptedException when the thread is interrupted")
    void awaitPlayerInput_throwsInterruptedExceptionOnInterrupt() throws Exception {
        logger.debug("Testing awaitPlayerInput throws on interrupt");

        AtomicBoolean exceptionWasThrown = new AtomicBoolean(false);

        Thread blockingThread = new Thread(() -> {
            try {
                panel.awaitPlayerInput();
            } catch (InterruptedException e) {
                exceptionWasThrown.set(true);
            }
        });
        blockingThread.start();

        Thread.sleep(50);
        blockingThread.interrupt();
        blockingThread.join(2_000);

        assertFalse(blockingThread.isAlive(),
                "The blocked thread should have terminated after being interrupted");
        assertTrue(exceptionWasThrown.get(),
                "awaitPlayerInput() should propagate InterruptedException when the thread is interrupted");
    }

    // -------------------------------------------------------------------------
    // inputQueue — capacity constraint
    // -------------------------------------------------------------------------

    /**
     * Verifies that the {@code inputQueue} has a capacity of exactly 1, so that
     * a second submission attempt while the game thread has not yet consumed the
     * first answer is silently rejected by {@link LinkedBlockingQueue#offer(Object)}.
     *
     * <p>
     *     In production, the input controls are disabled immediately after the
     *     first submission, making a genuine second offer impossible. The capacity
     *     limit is a belt-and-suspenders guard that prevents a race condition from
     *     corrupting the game state if the controls are somehow activated twice.
     * </p>
     */
    @Test
    @DisplayName("inputQueue capacity is 1 — second offer is rejected when queue is full")
    void inputQueue_hasCapacityOfOne() {
        logger.debug("Testing inputQueue capacity is exactly 1");

        assertTrue(inputQueue.offer("first"),
                "The first offer should succeed on an empty queue (capacity 1)");
        assertFalse(inputQueue.offer("second"),
                "The second offer should be rejected — queue is already at capacity");
    }

    // -------------------------------------------------------------------------
    // availableNarrativePixels — pure function
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link GamePanel#availableNarrativePixels(double, Insets, double, double)}
     * subtracts outer padding, inner content padding, and scrollbar width from the
     * total narrative area width.
     */
    @Test
    @DisplayName("availableNarrativePixels() subtracts padding and scrollbar from area width")
    void availableNarrativePixels_subtractsInsetsAndScrollbar() {
        logger.debug("Testing availableNarrativePixels subtracts insets and scrollbar");

        double result = GamePanel.availableNarrativePixels(
                400, new Insets(12, 12, 12, 12), 120, 14);

        assertEquals(242, result, 0.001,
                "Available width should be area minus outer padding, content padding, and scrollbar");
    }

    // -------------------------------------------------------------------------
    // themeIconContent(boolean) — pure function
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link GamePanel#themeIconContent(boolean)} returns the
     * moon glyph when the light theme is not active (dark theme showing) and
     * the sun glyph when it is active (light theme showing).
     *
     * <p>
     *     This is a pure static function with no field access and no side
     *     effects, so — unlike {@link GamePanel#handleToggleTheme()}, which
     *     touches the {@code @FXML} fields {@code rootPane} and
     *     {@code themeToggleIcon} — it can be tested directly without any
     *     JavaFX platform or FXML injection.
     * </p>
     *
     * @param lightThemeActive the input theme state.
     * @param expectedIcon     the icon constant expected for that state.
     */
    @ParameterizedTest(name = "themeIconContent({0}) returns the expected glyph")
    @org.junit.jupiter.params.provider.CsvSource({
            "false, MOON",
            "true, SUN"
    })
    @DisplayName("themeIconContent(boolean) returns the moon glyph for dark, sun glyph for light")
    void themeIconContent_returnsExpectedGlyph(boolean lightThemeActive, String expectedIcon) {
        logger.debug("Testing themeIconContent({}) returns {}", lightThemeActive, expectedIcon);

        String result = GamePanel.themeIconContent(lightThemeActive);

        String expected = "MOON".equals(expectedIcon) ? GamePanel.THEME_ICON_MOON : GamePanel.THEME_ICON_SUN;
        assertEquals(expected, result,
                "themeIconContent(" + lightThemeActive + ") should return the " + expectedIcon + " glyph");
    }

    /**
     * Verifies that {@link GamePanel#themeIconContent(boolean)} never returns
     * the same glyph for both theme states, guarding against a copy-paste
     * mistake where both branches accidentally return the same constant.
     */
    @Test
    @DisplayName("themeIconContent(boolean) returns different glyphs for the two theme states")
    void themeIconContent_glyphsDifferBetweenStates() {
        logger.debug("Testing themeIconContent glyphs differ between states");

        assertNotEquals(GamePanel.themeIconContent(false), GamePanel.themeIconContent(true),
                "The dark-theme and light-theme icon glyphs must differ");
    }
}
