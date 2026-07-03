package com.intro.ui;

import com.intro.io.GuiIO;
import javafx.fxml.FXMLLoader;
import javafx.scene.shape.FillRule;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.SVGPath;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * FXML Controller for {@code GamePanel.fxml}.
 * <p>
 *     The {@code GamePanel.fxml} defines the layout and {@code GamePanel} defines the behaviour.
 *     {@link javafx.fxml.FXMLLoader} instantiates this class and injects the {@code FXML}
 *     annotated fields before calling {@link #initialise()}.
 * </p>
 */
@SuppressWarnings({"LogStatementNotGuardedByLogCondition", "LoggingSimilarMessage"})
public class GamePanel {

    private static final Logger logger = LogManager.getLogger(GamePanel.class);

    /**
     * The root layout node of {@code GamePanel.fxml}. used by {@link #handleToggleTheme()} to add/remove
     * the {@code "light-theme"} CSS class. Also usable by {@link WindowResizeHandler} as the node that
     * detects edge-drag gestures for window resizing.
     *
     */
    @FXML
    private BorderPane rootPane;

    /**
     * The {@link SVGPath} graphic inside {@link #themeToggleButton}.
     * <p>
     *     The {@code content} (path data) is toggled at runtime by {@link #handleToggleTheme()} between
     *     a moon and sun icon, depending on the active theme.
     * </p>
     * @see #themeIconContent(boolean)
     */
    @FXML
    private SVGPath themeToggleIcon;

    /**
     * The theme toggle button in the title bar. Retained currently for symmetry with other title-bar
     * controls and potential future use (e.g. updating tooltip text on toggle).
     */
    @FXML
    private Button themeToggleButton;

    /**
     * The {@link SVGPath} graphic inside minimise button. Content is set once, in {@link #initialise()}
     * and does not change at runtime.
     */
    @FXML
    private SVGPath minimiseIcon;

    /**
     * The {@link SVGPath} graphic inside maximise/restore button. Content is set once, in {@link #initialise()}
     * and does not change at runtime.
     */
    @FXML
    private SVGPath maximiseIcon;

    /**
     * The {@link SVGPath} graphic inside close button. Content is set once, in {@link #initialise()}
     * and does not change at runtime.
     */
    @FXML
    private SVGPath closeIcon;

    /**
     * Displays all narrative game text, prompts and output.
     * <p>Defined in the {@code GamePanel.fxml} file and loaded by {@link javafx.fxml.FXMLLoader}.</p>
     * <p>Uses word-wrapping and is read-only.</p>
     */
    @FXML
    private TextArea narrativeArea;

    /**
     * The input field for player responses. This is disabled until the game engine calls
     * {@link GuiIO#prompt(String)}.
     * <p>Defined in the {@code GamePanel.fxml} file and loaded by {@link javafx.fxml.FXMLLoader}.</p>
     */
    @FXML
    private TextField inputField;

    /**
     * Submits the player inputs to the game engine. This is disabled until the game engine calls
     * {@link GuiIO#prompt(String)}.
     * <p>Defined in the {@code GamePanel.fxml} file and loaded by {@link javafx.fxml.FXMLLoader}.</p>
     */
    @FXML
    private Button submitButton;

    /**
     * The window for the application. Only used by the drag-to-move handlers for moving the window.:
     * <ul>
     *     <li>{@link #onTitleBarPressed(MouseEvent)}</li>
     *     <li>{@link #onTitleBarDragged(MouseEvent)}</li>
     * </ul>
     * <p>
     *     N.B. The {@code minimise}, {@code maximise} and {@code close} buttons are handled by
     *     {@link #windowControls}.
     * </p>
     */
    private Stage stage;

    /**
     * Abstracts the minimise, maximise and close functions for the window.
     * <p>
     *     Set by the {@link GameWindow} with {@link #setWindowControls(WindowControls)}. In
     *     testing, a Mockito mock can be injected here.
     * </p>
     */
    private WindowControls windowControls;

    //--------------------------------------
    // Icons
    //--------------------------------------

    /**
     * Defaults to {@code false} (dark mode) but can be toggled by player to {@link true} (light mode).
     */
    private boolean lightThemeActive = false;

    static final String LIGHT_THEME_STYLE_CLASS = "light-theme";
    static final String THEME_ICON_KEY_MOON = "ui.icon.theme.moon";
    static final String THEME_ICON_KEY_SUN = "ui.icon.theme.sun";

    static final String THEME_ICON_MOON = IconRegistry.getPath(THEME_ICON_KEY_MOON,
            "M12.226 2.003a9.97 9.97 0 0 0-7.297 2.926c-3.905 3.905-3.905 10.237 0 14.142s10.237 3.905 14.142 0a9.97 9.97 0 0 0 2.926-7.297a10 10 0 0 0-.337-2.368a15 15 0 0 1-1.744 1.436c-1.351.949-2.733 1.563-3.986 1.842c-1.906.423-3.214.032-3.93-.684s-1.107-2.024-.684-3.93c.279-1.253.893-2.635 1.842-3.986c.414-.592.893-1.177 1.436-1.744a10 10 0 0 0-2.368-.337m5.43 15.654a7.96 7.96 0 0 0 2.251-4.438c-3.546 2.045-7.269 2.247-9.321.195s-1.85-5.775.195-9.321a8 8 0 1 0 6.876 13.564");

    static final String THEME_ICON_SUN = IconRegistry.getPath(THEME_ICON_KEY_SUN,
            "M12 16a4 4 0 1 0 0-8a4 4 0 0 0 0 8m0 2a6 6 0 1 0 0-12a6 6 0 0 0 0 12M11 0h2v4.062a8 8 0 0 0-2 0zM7.094 5.68L4.222 2.808L2.808 4.222L5.68 7.094A8 8 0 0 1 7.094 5.68M4.062 11H0v2h4.062a8 8 0 0 1 0-2m1.618 5.906l-2.872 2.872l1.414 1.414l2.872-2.872a8 8 0 0 1-1.414-1.414M11 19.938V24h2v-4.062a8 8 0 0 1-2 0m5.906-1.618l2.872 2.872l1.414-1.414l-2.872-2.872a8 8 0 0 1-1.414 1.414M19.938 13H24v-2h-4.062a8 8 0 0 1 0 2M18.32 7.094l2.872-2.872l-1.414-1.414l-2.872 2.872c.528.41 1.003.886 1.414 1.414");

    static final String WINDOW_ICON_KEY_MINIMISE = "ui.icon.window.minimise";
    static final String WINDOW_ICON_KEY_MAXIMISE = "ui.icon.window.maximise";
    static final String WINDOW_ICON_KEY_CLOSE = "ui.icon.window.close";

    static final String WINDOW_ICON_MINIMISE = "M2.5 12A1.5 1.5 0 0 1 4 10.5h16a1.5 1.5 0 0 1 0 3H4A1.5 1.5 0 0 1 2.5 12";
    static final String WINDOW_ICON_MAXIMISE = "M21 2H3a1 1 0 0 0-1 1v18a1 1 0 0 0 1 1h18a1 1 0 0 0 1-1V3a1 1 0 0 0-1-1m-1 18H4V4h16Z";
    static final String WINDOW_ICON_CLOSE = "M6.225 4.811a1 1 0 0 0-1.414 1.414L10.586 12L4.81 17.775a1 1 0 1 0 1.414 1.414L12 13.414l5.775 5.775a1 1 0 0 0 1.414-1.414L13.414 12l5.775-5.775a1 1 0 0 0-1.414-1.414L12 10.586z";

    //--------------------------------------
    // Move window
    //--------------------------------------

    /**
     * Horizontal offset in pixels between the cursor and the window's edge when the title bar
     * is first pressed. Used in drag-to-move feature to reposition the window.
     * @see #onTitleBarDragged(MouseEvent)
     */
    private double dragOffsetX;

    /**
     * Vertical offset in pixels between the cursor and the window's edge when the title bar
     * is first pressed. Used in drag-to-move feature to reposition the window.
     * @see #onTitleBarDragged(MouseEvent)
     */
    private double dragOffsetY;

    //--------------------------------------
    // Queue
    //--------------------------------------

    /**
     * Thread-safe queue with a capacity of {@code 1} for passing the player inputs to the
     * game engine thread.
     */
    private final LinkedBlockingQueue<String> inputQueue = new LinkedBlockingQueue<>(1);


    //--------------------------------------
    // FXML initialise
    //--------------------------------------

    /**
     * This is called automatically by the {@link javafx.fxml.FXMLLoader} once the {@code GamePanel.fxml}
     * has been parsed.
     * <p>
     *     N.B. Not currently being used for any post-injection setup, but could be added here in future.
     * </p>
     */
    @FXML
    private void initialise() {
        logger.debug("GamePanel controller initialised, FXML injection completed.");
        this.themeToggleIcon.setFillRule(FillRule.EVEN_ODD);
        this.themeToggleIcon.setContent(themeIconContent(this.lightThemeActive));
        this.minimiseIcon.setContent(WINDOW_ICON_KEY_MINIMISE);
        this.maximiseIcon.setContent(WINDOW_ICON_KEY_MAXIMISE);
        this.closeIcon.setContent(WINDOW_ICON_CLOSE);
    }

    //--------------------------------------
    // Setters
    //--------------------------------------

    /**
     * Provides the {@link Stage} needed for the drag-to-move support.
     * <p>
     *     Must be called after {@link FXMLLoader#load()} completes and before any stage is shown. The
     *     {@link Stage} is only used to read and update the window's screen position in
     *     {@link #onTitleBarPressed(MouseEvent)} and {@link #onTitleBarDragged(MouseEvent)}.
     * </p>
     * @param stage the primary stage, must not be {@code null}.
     * @throws IllegalArgumentException if {@code Stage} is {@code null}
     */
    public void setStage(Stage stage) {
        if (stage == null) {
            throw new IllegalArgumentException("Stage should not be null.");
        }
        this.stage = stage;
        logger.debug("Stage set in GamePanel.");
    }

    /**
     * Provides the {@link WindowControls} implementation used by the three window-control button handlers.
     * <p>
     *     Must be called after {@link FXMLLoader#load()} completes and before any stage is shown. In testing
     *     a Mockito mock is injected instead, removing the need for a real stage.
     * </p>
     * @param windowControls the {@link WindowControls} implementation, must not be {@code null}.
     * @throws IllegalArgumentException if {@code windowControls} is {@code null}
     */
    public void setWindowControls(WindowControls windowControls) {
        if (windowControls == null) {
            throw new IllegalArgumentException("windowControls must not be null.");
        }
        this.windowControls = windowControls;
        logger.debug("WindowControls set in GamePanel.");
    }

    // ------------------------------------------
    // FXML event handlers
    // ------------------------------------------

    // ------------------------------------------
    // Window controls
    // ------------------------------------------

    /**
     * Minimises the application window to the taskbar. Triggered by:
     * <ul><li>{@code onAction='#handleMinimise'}</li></ul>
     * In {@code GamePanel.fxml}. Runs on the JavaFX Application Thread and utilises
     * {@link WindowControls#minimise()}.
     */
    @FXML
    void handleMinimise() {
        if (this.windowControls == null) {
            logger.warn("handleMinimise invoked but WindowControls has not been set");
            return;
        }
        logger.debug("Minimise button clicked");
        this.windowControls.minimise();
    }

    /**
     * Toggles the application window between maximised and restored sizes. Triggered by:
     * <ul><li>{@code onAction='#handleMaximise'}</li></ul>
     * In {@code GamePanel.fxml}. Runs on the JavaFX Application Thread and utilises
     * {@link WindowControls#toggleMaximise()}.
     */
    @FXML
    void handleMaximise() {
        if (this.windowControls == null) {
            logger.warn("handleMaximise invoked but WindowControls has not been set");
            return;
        }
        logger.debug("Toggle Maximise button clicked");
        this.windowControls.toggleMaximise();
    }

    /**
     * Closes the application window and terminates the JVM. Triggered by:
     * <ul><li>{@code onAction='#handleClose'}</li></ul>
     * In {@code GamePanel.fxml}. Runs on the JavaFX Application Thread and utilises
     * {@link WindowControls#close()}.
     */
    @FXML
    void handleClose() {
        if (this.windowControls == null) {
            logger.warn("handleClose invoked but WindowControls has not been set");
            return;
        }
        logger.debug("Close button clicked");
        this.windowControls.close();
    }

    // ------------------------------------------
    // Theme Toggle
    // ------------------------------------------

    /**
     * Toggles between light/dark mode.
     * <p>
     *     Triggered by {@code onAction="#handleToggleTheme"} on the theme toggle button in
     *     {@code GamePanel.fxml}. Runs on the JavaFX Application Thread.
     * </p>
     * <p>Toggles {@link #lightThemeActive} {@code true} / {@code false}, and then:</p>
     * <ul>
     *     <li>Adds or removes the {@link #LIGHT_THEME_STYLE_CLASS} on {@link #rootPane}, which
     *     activates or deactivates every {@code .root.light-theme} rule in {@code game.css}.</li>
     *     <li>Updates {@link #themeToggleIcon}'s path data via {@link #themeIconContent(boolean)} so
     *     the icon always reflects the theme that is now active.</li>
     * </ul>
     * <p>
     *     Package-private so that unit tests in the same package can invoke it directly. Only touches
     *     {@link #rootPane} and {@link #themeToggleIcon} so it is safe to call from a test once the
     *     fields have been populated, with no {@link Stage} or {@link WindowControls} required.
     * </p>
     */
    @FXML
    void handleToggleTheme() {
        this.lightThemeActive = !this.lightThemeActive;
        logger.debug("Theme toggled, light theme ON now: {}", this.lightThemeActive);
        if (this.lightThemeActive) {
            this.rootPane.getStyleClass().add(LIGHT_THEME_STYLE_CLASS);
        } else {
            this.rootPane.getStyleClass().remove(LIGHT_THEME_STYLE_CLASS);
        }
        this.themeToggleIcon.setContent(themeIconContent(this.lightThemeActive));
    }

    /**
     * Returns the SVGPath 'd' content that should be displayed on the theme toggle button for the
     * given theme state.
     * <p>
     *     Can be unit-tested directly without any JavaFX platform or FXML injection.
     * </p>
     * @param lightThemeActive {@code true} if light theme has just been toggled ON, {@code false} if
     *        light theme has just been toggled OFF.
     * @return {@link #THEME_ICON_SUN} when {@link #lightThemeActive} is {@code true}.
     * {@link #THEME_ICON_MOON} when {@link #lightThemeActive} is {@code false}.
     */
    static String themeIconContent(boolean lightThemeActive) {
        return lightThemeActive ? THEME_ICON_SUN : THEME_ICON_MOON;
    }

    // ------------------------------------------
    // Title bar drag/double-click
    // ------------------------------------------

    /**
     * Records the cursor-to-window-corner offset when the player clicks the title bar.
     * <p>
     *     Stores the horizontal and vertical distances in pixels between the positions of the
     *     cursor and the window's corner (top-left):
     *     <ul>
     *         <li>Horizontal: {@link #dragOffsetX}</li>
     *         <li>Vertical: {@link #dragOffsetY}</li>
     *     </ul>
     * </p
     *<p>
     *     Does nothing if the {@link Stage} is {@code null}.
     *</p>
     * @param event the mouse-pressed event
     */
    @FXML
    private void onTitleBarPressed(MouseEvent event) {
        if (this.stage == null) {
            logger.debug("Stage is null, onTitleBarPressed returning with no changes.");
            return;
        }
        this.dragOffsetX = this.stage.getX() - event.getScreenX();
        this.dragOffsetY = this.stage.getY() - event.getScreenY();
    }

    /**
     * Moves the window to follow the cursor when the title bar is dragged.
     * <p>
     *     Adds the offsets record in {@link #onTitleBarPressed(MouseEvent)}
     *     to the current cursor screen position to determine the new window
     *     position.
     * </p>
     *<p>
     *     Does nothing if the {@link Stage} is {@code null}.
     *</p>
     * @param event the mouse-dragged event
     */
    @FXML
    private void onTitleBarDragged(MouseEvent event) {
        if (this.stage == null) {
            logger.debug("Stage is null, onTitleBarPressed returning with no changes.");
            return;
        }
        this.stage.setX(event.getScreenX() + this.dragOffsetX);
        this.stage.setY(event.getScreenY() + this.dragOffsetY);
    }

    /**
     * Toggles the maximise/restore sizes when the player double-clicks on the title bar.
     * <p>
     *     Checks {@link MouseEvent#getClickCount()} and acts when this count is exactly 2.
     *     Single clicks are ignored by this method. Uses {@link #handleMaximise()} with
     *     identical behaviour to clicking maximise button.
     * </p>
     */
    @FXML
    private void onTitleBarDoubleClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
            logger.debug("Title bar double-clicked, toggling maximise/restore.");
            this.handleMaximise();
        }
    }

    // ------------------------------------------
    // Player input
    // ------------------------------------------

    /**
     * Handles the submit action, triggered either by clicking the Submit button or by pressing
     * Enter inside the input field.
     * <p>
     *     Both controls use {@code onAction='#handleSubmit'} in {@code GamePanel.fxml}.
     * </p>
     */
    @FXML
    void handleSubmit() {
        String text = this.inputField.getText().trim();
        logger.debug("Submit triggered, input: [{}]", text);
        this.inputField.clear();
        this.setInputEnabled(false);
        if (!this.inputQueue.offer(text)) {
            logger.warn("Input queue full, discarded player input: [{}]", text);
        }
    }

    // ------------------------------------------
    // Tasks called by GuiIO
    // ------------------------------------------

    /**
     * Appends the {@code text} to the narrative area and scrolls to the bottom so the player
     * always sees the latest output.
     * <p>
     *     Must be called on the JavaFX Application Thread: always wrapped in a scheduler by
     *     {@link GuiIO}.
     * </p>
     * @param text the text to append, must not be {@code null}.
     */
    public void appendNarrativeText(String text) {
        this.narrativeArea.appendText(text);
        this.narrativeArea.positionCaret(this.narrativeArea.getText().length());
    }

    /**
     * Removes all text currently displayed in the narrative area.
     */
    public void clearNarrativeText() {
        this.narrativeArea.clear();
    }

    /**
     * Blocks the calling thread until the player submits an answer, then returns the trimmed
     * input String.
     * <p>
     *     Must be called from the game-engine background thread and <em>never</em> from the
     *     JavaFX Application Thread (blocking the JavaFX Application Thread would freeze the UI).
     * </p>
     * <p>
     *     {@code inputQueue.take()} suspends the game thread until {@link #handleSubmit()} is called.
     * </p>
     * @return the player's trimmed input, never {@code null} but may be {@code empty}.
     * @throws InterruptedException if the game thread is interrupted while waiting, such as
     * JVM shutting down after the application window is closed.
     */
    public String awaitPlayerInput() throws InterruptedException {
        logger.debug("Awaiting player input on the game thread");
        String input = this.inputQueue.take();
        logger.debug("Player input received: [{}]", input);
        return input;
    }

    /**
     * Toggles the player input controls enabled/disabled.
     * <p>
     *     {@code true} just before the game thread blocks to give the player a visual cue that
     *     a response is expected ({@link GuiIO#prompt(String)}). {@code false} after the player
     *     submits to prevent a double submit ({@link #handleSubmit()}). Must be called on the
     *     JavaFX Application Thread.
     * </p>
     * @param enabled {@code true} to allow typing/submitting; {@code false} to lock the controls.
     */
    public void setInputEnabled(boolean enabled) {
        logger.debug("setInputEnabled({})", enabled);
        this.inputField.setDisable(!enabled);
        this.submitButton.setDisable(!enabled);
        if (enabled) {
            this.inputField.requestFocus();
        }
    }
}

