package com.intro.io;

import com.intro.ui.GamePanel;
import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * Implementation of {@link GameIO}.
 * <p>
 *     TODO finish Javadoc description of class
 * </p>
 */
public class GuiIO implements GameIO {

    private static final Logger logger = LogManager.getLogger(GuiIO.class);

    /**
     * The FXML controller that deploys the {@link javafx.scene.control.TextArea} where the
     * narrative text is displayed. As well as the player input controls. UI interactions
     * occur through the {@link GamePanel}.
     */
    private final GamePanel gamePanel;

    /**
     * Executes tasks on the UI thread.
     * <p>
     *     This is normally {@code Platform::runLater} which allows tasks to be scheduled/run
     *     asynchronously on the JavaFX Application Thread. In testing, this can be replaced with
     *     {@code Runnable::run} to execute tasks on the calling thread and prevent the need to
     *     run the JavaFX platform.
     * </p>
     * @see #GuiIO(GamePanel)
     * @see #GuiIO(GamePanel, Consumer)
     */
    private final Consumer<Runnable> uiScheduler;

    /**
     * Constructor for {@code GuiIO}.
     * <p>
     *     The UI scheduler is set to {@link Platform#runLater(Runnable)} so all UI updates
     *     run on the JavaFX Application Thread.
     * </p>
     * @param gamePanel the controller building the narrative area and input controls, must not be {@code null}
     * @throws IllegalArgumentException if {@code gamePanel} is {@code null}
     */
    public GuiIO(GamePanel gamePanel) {
        this(gamePanel, Platform::runLater);
    }

    /**
     * Package-private constructor used in unit tests.
     * <p>
     *     Accepts a custom {@code uiScheduler} in place of the {@link Platform#runLater(Runnable)}.
     *     This removes the need to start a JavaFX platform for testing and makes Mockito testing
     *     of {@link GamePanel} methods more straightforward.
     * </p>
     * <p>
     *     How to use:
     * </p>
     * <pre>{@code
     * // EXAMPLE 1
     * GamePanel mockPanel = mock(GamePanel.class);
     * GuiIO guiIO = new GuiIO(mockPanel, Runnable::run);
     *
     * // EXAMPLE 2
     * @ExtendWith(MockitoExtension.class)
     * class TestClass {
     * @Mock
     * GamePanel mockPanel;
     * GuiIO guiIO;
     * @BeforeEach
     * void setUp() {
     *     this.guiIO = new GuiIO(mockPanel, Runnable::run);
     * }
     * }
     * }</pre>
     * @param gamePanel the controller building the narrative area and input controls, must not be {@code null}
     * @param uiScheduler a {@link Consumer} that executes a {@link Runnable} on a desired thread, must not be
     *                    {@code null}
     * @throws IllegalArgumentException if {@code gamePanel} is {@code null}
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
     * Appends {@code text} and a newline to the narrative text area in the UI.
     * <p>
     *     The update is scheduled with the {@link #uiScheduler} (or rather
     *     {@link Platform#runLater(Runnable)}). The game engine thread returns immediately
     *     without waiting for the UI to refresh.
     * </p>
     * @param text line of text to display
     */
    @Override
    public void println(String text) {
        logger.debug("println: [{}]", text);
        this.uiScheduler.accept(() -> this.gamePanel.appendNarrativeText(text + "\n"));
    }

    /**
     * Appends a blank line to the narrative text area in the UI.
     * <p>
     *     The update is scheduled with the {@link #uiScheduler} (or rather
     *     {@link Platform#runLater(Runnable)}). The game engine thread returns immediately
     *     without waiting for the UI to refresh.
     * </p>
     */
    @Override
    public void println() {
        logger.debug("println: blank");
        this.uiScheduler.accept(() -> this.gamePanel.appendNarrativeText("\n"));
    }

    /**
     * Displays a prompt message in the text area, enables the input controls and input bar, then
     * blocks the calling thread until the player submits their response.
     * <h2>Sequence:</h2>
     * <p>
     *     <ol>
     *         <li>
     *             A task is scheduled with {@link #uiScheduler}. Once the task has run on the JavaFX
     *             Application Thread, the prompt text is appended and the
     *             {@link javafx.scene.control.TextField Input Field} and
     *             {@link javafx.scene.control.Button Submit Button} are enabled, allowing the player
     *             to enter their response.
     *         </li>
     *         <li>
     *             The game thread blocks on {@link GamePanel#awaitPlayerInput()}, which calls
     *             {@link LinkedBlockingQueue#take()}.
     *         </li>
     *         <li>
     *             When player clicks Submit or presses Enter, the JavaFX Application Thread event handler
     *             trims the input, disables input field/controls and places text into the queue.
     *         </li>
     *         <li>
     *             {@link LinkedBlockingQueue#take()} unblocks the thread and returns the player input to
     *      *         the game engine.
     *         </li>
     *     </ol>
     * </p>
     * @param message text to display before waiting for player input, must not be {@code null}
     * @return the trimmed input, never {@code null} but can be an empty string.
     */
    @Override
    public String prompt(String message) {
        logger.debug("prompt:[{}]", message);
        this.uiScheduler.accept(() -> {
            this.gamePanel.appendNarrativeText(message + "\n");
            this.gamePanel.setInputEnabled(true);
        });
        try {
            String input = this.gamePanel.awaitPlayerInput();
            logger.debug("prompt received: [{}]", input);
            return input;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Game thread interrupted whilst awaiting player input");
            return "";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearOutput() {
        logger.debug("Clearing output");
        this.uiScheduler.accept(this.gamePanel::clearNarrativeText);
    }
}

