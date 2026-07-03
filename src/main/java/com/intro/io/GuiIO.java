package com.intro.io;

import com.intro.ui.GamePanel;
import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;

/**
 * JavaFX-backed implementation of {@link GameIO}.
 *
 * <h2>Role in the architecture</h2>
 * <p>
 *     {@code GuiIO} is the bridge between the game engine's blocking,
 *     sequential logic and the JavaFX UI thread model. The game engine calls
 *     {@link #println(String)}, {@link #println()}, and {@link #prompt(String)}
 *     exactly as it does with {@link ConsoleIO} — it knows nothing about
 *     JavaFX threads. {@code GuiIO} handles all thread-safety concerns so that
 *     the engine and scene classes remain unchanged.
 * </p>
 *
 * <h2>Threading model</h2>
 * <p>
 *     JavaFX requires that all UI updates happen on the
 *     <em>JavaFX Application Thread</em> (JAT). The game engine runs on a
 *     separate background thread so it can block without freezing the UI.
 *     {@code GuiIO} bridges this gap using two mechanisms:
 * </p>
 * <ul>
 *     <li>
 *         <strong>Output</strong> — {@link #println(String)} and
 *         {@link #println()} schedule text appends on the JAT via the
 *         {@link #uiScheduler}. The game thread continues immediately; it does
 *         not wait for the UI to refresh.
 *     </li>
 *     <li>
 *         <strong>Input</strong> — {@link #prompt(String)} schedules the
 *         prompt text and input-unlock on the JAT, then <em>blocks</em> the
 *         game thread on {@link GamePanel#awaitPlayerInput()}. The game thread
 *         stays suspended until the player submits (the JAT places the text
 *         into a {@link java.util.concurrent.LinkedBlockingQueue} inside
 *         {@link GamePanel}), at which point {@code prompt()} returns it.
 *     </li>
 * </ul>
 *
 * <h2>Scheduler injection for testability</h2>
 * <p>
 *     Rather than calling {@link Platform#runLater(Runnable)} directly, all
 *     UI-thread dispatch goes through the {@link #uiScheduler} field (a
 *     {@link Consumer}{@code <Runnable>}). The public constructor
 *     {@link #GuiIO(GamePanel)} sets this to {@code Platform::runLater} for
 *     production use. The package-private constructor
 *     {@link #GuiIO(GamePanel, Consumer)} lets tests inject
 *     {@code Runnable::run} instead, which executes tasks synchronously on
 *     the calling thread — no JavaFX platform required. This is the standard
 *     approach for testing JavaFX code in headless environments.
 * </p>
 *
 * <h2>Interrupted thread handling</h2>
 * <p>
 *     If the game thread is interrupted while blocked in {@link #prompt(String)}
 *     (for example because the window was closed), the
 *     {@link InterruptedException} is caught, the interrupt flag is re-asserted
 *     with {@link Thread#interrupt()}, and an empty string is returned.
 *     Swallowing the interrupt without re-asserting the flag is a well-known
 *     Java threading anti-pattern; it would silently break cooperative
 *     cancellation logic higher up the call stack.
 * </p>
 *
 * <h2>FXML architecture note</h2>
 * <p>
 *     {@link GamePanel} is the FXML controller for {@code GamePanel.fxml}.
 *     It is a plain Java class — it does not extend any JavaFX node.
 *     {@code GuiIO} holds a reference to it and calls only its public API:
 *     {@link GamePanel#appendNarrativeText(String)},
 *     {@link GamePanel#awaitPlayerInput()}, and
 *     {@link GamePanel#setInputEnabled(boolean)}.
 * </p>
 */
public class GuiIO implements GameIO {

    private static final Logger logger = LogManager.getLogger(GuiIO.class);

    /**
     * The FXML controller that owns the narrative {@link javafx.scene.control.TextArea}
     * and player input controls. All UI interactions are delegated here.
     */
    private final GamePanel gamePanel;

    /**
     * Executes tasks on the UI thread.
     *
     * <p>
     *     In production this is {@code Platform::runLater}, which schedules
     *     tasks on the JavaFX Application Thread asynchronously. In tests this
     *     is replaced with {@code Runnable::run}, which executes tasks
     *     synchronously on the calling thread, eliminating the need for a
     *     running JavaFX platform and making assertions straightforward.
     * </p>
     *
     * @see #GuiIO(GamePanel)
     * @see #GuiIO(GamePanel, Consumer)
     */
    private final Consumer<Runnable> uiScheduler;

    /**
     * Constructs a production {@code GuiIO} wired to the given
     * {@link GamePanel} controller.
     *
     * <p>
     *     The UI scheduler is set to {@link Platform#runLater(Runnable)},
     *     which dispatches all UI updates asynchronously to the JavaFX
     *     Application Thread.
     * </p>
     *
     * @param gamePanel the controller providing the narrative area and input
     *                  controls; must not be {@code null}.
     * @throws IllegalArgumentException if {@code gamePanel} is {@code null}.
     */
    public GuiIO(GamePanel gamePanel) {
        this(gamePanel, Platform::runLater);
    }

    /**
     * Package-private constructor used in unit tests.
     *
     * <p>
     *     Accepts a custom {@code uiScheduler} in place of
     *     {@link Platform#runLater(Runnable)}. Passing {@code Runnable::run}
     *     causes all scheduled UI tasks to execute synchronously on the test
     *     thread, removing the requirement for a running JavaFX platform and
     *     allowing straightforward Mockito verification of;p[[=]]
     *     {@link GamePanel} method calls.
     * </p>
     *
     * <p>
     *     Example usage in tests:
     * </p>
     * <pre>{@code
     * GamePanel mockPanel = mock(GamePanel.class);
     * GuiIO guiIO = new GuiIO(mockPanel, Runnable::run);
     * }</pre>
     *
     * @param gamePanel   the controller providing narrative area and input
     *                    controls; must not be {@code null}.
     * @param uiScheduler a {@link Consumer} that executes {@link Runnable}s on
     *                    the desired thread; must not be {@code null}.
     * @throws IllegalArgumentException if {@code gamePanel} is {@code null}.
     */
    GuiIO(GamePanel gamePanel, Consumer<Runnable> uiScheduler) {
        if (gamePanel == null) {
            throw new IllegalArgumentException("gamePanel must not be null");
        }
        this.gamePanel = gamePanel;
        this.uiScheduler = uiScheduler;
        logger.debug("GuiIO initialised (scheduler={})", uiScheduler.getClass().getSimpleName());
    }

    /**
     * Appends {@code text} followed by a newline to the narrative area.
     *
     * <p>
     *     The update is scheduled via {@link #uiScheduler} (in production,
     *     {@link Platform#runLater(Runnable)}). The calling game-engine thread
     *     returns immediately without waiting for the UI to refresh.
     * </p>
     *
     * @param text the narrative or descriptive text to display;
     *             {@code null} is rendered as the string {@code "null"}.
     */
    @Override
    public void println(String text) {
        logger.debug("println: [{}]", text);
        uiScheduler.accept(() -> gamePanel.appendNarrativeText(text + "\n"));
    }

    /**
     * Appends a blank line to the narrative area.
     *
     * <p>
     *     Equivalent to calling {@link #println(String)} with an empty string.
     *     Scheduled via {@link #uiScheduler}.
     * </p>
     */
    @Override
    public void println() {
        logger.debug("println (blank line)");
        uiScheduler.accept(() -> gamePanel.appendNarrativeText("\n"));
    }

    /**
     * Displays a prompt message in the narrative area, unlocks the input
     * controls, and then <strong>blocks the calling thread</strong> until the
     * player submits their response.
     *
     * <h3>Sequence of events</h3>
     * <ol>
     *     <li>
     *         A task is posted via {@link #uiScheduler}. When it runs on the
     *         JAT, the prompt text is appended and the input
     *         {@link javafx.scene.control.TextField} and Submit
     *         {@link javafx.scene.control.Button} are enabled, giving the player
     *         a visual cue that a response is expected.
     *     </li>
     *     <li>
     *         The game thread blocks on {@link GamePanel#awaitPlayerInput()},
     *         which internally calls
     *         {@link java.util.concurrent.LinkedBlockingQueue#take()}.
     *     </li>
     *     <li>
     *         When the player clicks Submit or presses Enter, the JAT event
     *         handler trims the input, disables the controls, and places the
     *         text into the queue.
     *     </li>
     *     <li>
     *         {@link java.util.concurrent.LinkedBlockingQueue#take()} unblocks
     *         and returns the player's input to the game engine.
     *     </li>
     * </ol>
     *
     * @param message the prompt text to display; must not be {@code null}.
     * @return the trimmed text the player entered; never {@code null}, but may
     *         be an empty string if the player submitted without typing, or if
     *         the game thread was interrupted.
     */
    @Override
    public String prompt(String message) {
        logger.debug("prompt: [{}]", message);
        uiScheduler.accept(() -> {
            gamePanel.appendNarrativeText(message + "\n");
            gamePanel.setInputEnabled(true);
        });
        try {
            String input = gamePanel.awaitPlayerInput();
            logger.debug("prompt received: [{}]", input);
            return input;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Game thread interrupted while awaiting player input");
            return "";
        }
    }

    /**
     * Clears all text from the narrative area.
     *
     * <p>
     *     The update is scheduled via {@link #uiScheduler} (in production,
     *     {@link Platform#runLater(Runnable)}), exactly like
     *     {@link #println(String)} and {@link #println()}, since
     *     {@link GamePanel#clearNarrativeText()} touches a JavaFX node and
     *     must run on the JavaFX Application Thread.
     * </p>
     */
    @Override
    public void clearOutput() {
        logger.debug("clearOutput");
        uiScheduler.accept(gamePanel::clearNarrativeText);
    }
}
