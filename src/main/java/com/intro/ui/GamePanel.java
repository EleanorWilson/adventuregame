package com.intro.ui;

import com.intro.config.GameConfig;
import com.intro.io.NarrativeFormatter;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * FXML controller for {@code GamePanel.fxml} — the main game view.
 *
 * <h2>Role in the architecture</h2>
 * <p>
 *     {@code GamePanel} is the <em>controller</em> half of the FXML MVC pair.
 *     {@code GamePanel.fxml} defines the layout (what the UI looks like);
 *     this class defines the behaviour (what the UI does). The two are linked
 *     by the {@code fx:controller="com.intro.ui.GamePanel"} attribute in the
 *     FXML file. JavaFX's {@link javafx.fxml.FXMLLoader} instantiates this
 *     class and injects the annotated fields before calling
 *     {@link #initialize()}.
 * </p>
 *
 * <h2>Layout summary</h2>
 * <p>
 *     The FXML root is a {@code BorderPane} with three regions:
 * </p>
 * <ul>
 *     <li>
 *         <strong>TOP</strong> — a custom title bar {@code HBox} containing a
 *         window-title {@code Label} and four window-control {@code Button}s
 *         (theme toggle, minimise, maximise, close). Replaces the native OS
 *         title bar, which is removed by
 *         {@code StageStyle.UNDECORATED} in {@link GameWindow}.
 *         See the {@link WindowControls} section below.
 *     </li>
 *     <li>
 *         <strong>CENTER</strong> — {@link #narrativeArea}: a read-only,
 *         word-wrapping {@link TextArea} that displays all game output.
 *         Auto-scrolls to the bottom after every append.
 *     </li>
 *     <li>
 *         <strong>BOTTOM</strong> — {@link #inputField} and
 *         {@link #submitButton}: the player's input controls. Both start
 *         disabled and are only enabled when the game engine is actively
 *         waiting for a response via
 *         {@link com.intro.io.GuiIO#prompt(String)}.
 *     </li>
 * </ul>
 *
 * <h2>Custom title bar and WindowControls</h2>
 * <p>
 *     Because the Stage uses {@code StageStyle.UNDECORATED}, there is no native
 *     title bar. The custom title bar in {@code GamePanel.fxml} provides:
 * </p>
 * <ul>
 *     <li>
 *         <strong>Drag-to-move</strong> — {@link #onTitleBarPressed(MouseEvent)}
 *         records the cursor offset and {@link #onTitleBarDragged(MouseEvent)}
 *         repositions the window. Requires the {@link Stage} reference, set by
 *         {@link GameWindow} via {@link #setStage(Stage)}.
 *     </li>
 *     <li>
 *         <strong>Double-click to maximise/restore</strong> —
 *         {@link #onTitleBarDoubleClicked(MouseEvent)} calls
 *         {@link #handleMaximise()} when {@code clickCount == 2}.
 *     </li>
 *     <li>
 *         <strong>Window control buttons</strong> — the three {@code @FXML}
 *         handler methods ({@link #handleMinimise()}, {@link #handleMaximise()},
 *         {@link #handleClose()}) delegate to a {@link WindowControls}
 *         implementation set by {@link GameWindow} via
 *         {@link #setWindowControls(WindowControls)}.
 *     </li>
 * </ul>
 * <p>
 *     The {@link WindowControls} interface exists specifically to keep
 *     {@code GamePanel}'s window-control logic decoupled from the JavaFX
 *     {@link Stage} class. Tests inject a Mockito mock of {@link WindowControls}
 *     and verify that each handler delegates correctly, without needing the
 *     JavaFX platform to be running.
 * </p>
 *
 * <h2>Thread-safe input handoff</h2>
 * <p>
 *     The game engine runs on a background thread and blocks in
 *     {@link #awaitPlayerInput()} until the player submits. The Submit
 *     event handler runs on the JavaFX Application Thread (JAT). A
 *     {@link LinkedBlockingQueue} with capacity 1 acts as the rendezvous
 *     between the two threads:
 * </p>
 * <ol>
 *     <li>The game thread calls {@link #awaitPlayerInput()}, which calls
 *         {@link LinkedBlockingQueue#take()} and blocks.</li>
 *     <li>The player clicks Submit or presses Enter (JAT).
 *         {@link #handleSubmit()} trims the input, clears the field,
 *         disables controls, and calls {@link LinkedBlockingQueue#offer(Object)},
 *         which unblocks the game thread.</li>
 * </ol>
 * <p>
 *     A {@link LinkedBlockingQueue} is chosen over alternatives because:
 * </p>
 * <ul>
 *     <li>
 *         {@link java.util.concurrent.ConcurrentLinkedQueue} has no blocking
 *         {@code take()} — it would require a busy-wait spin loop.
 *     </li>
 *     <li>
 *         {@link java.util.concurrent.SynchronousQueue} requires the producer
 *         to block until the consumer is ready. Blocking the JAT inside
 *         {@code put()} would freeze the UI if the game thread has not yet
 *         entered {@code take()}.
 *     </li>
 *     <li>
 *         {@link LinkedBlockingQueue} with capacity 1 gives the JAT's
 *         {@code offer()} a one-slot buffer so it never blocks, while the
 *         game thread's {@code take()} still suspends until input arrives.
 *     </li>
 * </ul>
 *
 * <h2>CSS style classes</h2>
 * <p>The stylesheet {@code game.css} targets these classes applied in {@code GamePanel.fxml}:</p>
 * <ul>
 *     <li>{@code .title-bar}     — the custom title bar {@code HBox}.</li>
 *     <li>{@code .title-label}   — the window-title {@code Label}.</li>
 *     <li>{@code .title-button}  — base style for all four window-control {@code Button}s.</li>
 *     <li>{@code .title-icon}    — the {@link SVGPath} icon inside each window-control {@code Button}.</li>
 *     <li>{@code .theme-toggle-button} — additional style for the theme toggle button.</li>
 *     <li>{@code .close-button}  — additional style for the close button (red hover).</li>
 *     <li>{@code .narrative-area} — the story {@link TextArea}.</li>
 *     <li>{@code .input-field}   — the player's {@link TextField}.</li>
 *     <li>{@code .submit-button} — the Submit {@link Button}.</li>
 *     <li>{@code .input-bar}     — the bottom {@code HBox} container.</li>
 * </ul>
 *
 * <h2>Light / dark theme toggle</h2>
 * <p>
 *     {@link #handleToggleTheme()} switches the whole window between the dark
 *     theme (default) and a light theme by adding or removing the CSS class
 *     {@code "light-theme"} on {@link #rootPane}. {@code game.css} defines the
 *     light palette entirely under {@code .root.light-theme} selectors, so no
 *     second stylesheet needs to be loaded or swapped at runtime. The toggle
 *     button's icon (a {@link SVGPath} bound to {@link #themeToggleIcon}) is
 *     swapped between a moon glyph (shown while dark mode is active) and a sun
 *     glyph (shown while light mode is active) via {@link #themeIconContent(boolean)}.
 * </p>
 *
 * <h2>Extending this view</h2>
 * <p>
 *     To add a new view (inventory panel, stats panel, etc.), create a new
 *     {@code .fxml} file and a corresponding controller class following this
 *     same pattern. {@link GameWindow} can swap the active view by replacing
 *     the scene's root node. Do not add unrelated controls to this class or
 *     to {@code GamePanel.fxml}; keep one view per controller.
 * </p>
 */
public class GamePanel {

    private static final Logger logger = LogManager.getLogger(GamePanel.class);

    // -------------------------------------------------------------------------
    // FXML-injected fields — names must exactly match fx:id values in
    // GamePanel.fxml. FXMLLoader injects these before calling initialize().
    // -------------------------------------------------------------------------

    /**
     * The root layout node of {@code GamePanel.fxml}.
     *
     * <p>
     *     Used by {@link #handleToggleTheme()} to add/remove the
     *     {@code "light-theme"} CSS class, which is how the whole window
     *     switches between the dark (default) and light palettes defined in
     *     {@code game.css}. Also usable by {@link WindowResizeHandler} as the
     *     node that detects edge-drag gestures for window resizing.
     * </p>
     */
    @FXML
    private BorderPane rootPane;

    /**
     * The {@link SVGPath} graphic inside {@link #themeToggleButton}.
     *
     * <p>
     *     Its {@code content} (path data) is swapped at runtime by
     *     {@link #handleToggleTheme()} between a moon glyph and a sun glyph
     *     depending on which theme is currently active. See
     *     {@link #themeIconContent(boolean)}.
     * </p>
     */
    @FXML
    private SVGPath themeToggleIcon;

    /**
     * The theme toggle button in the title bar.
     * Retained mainly for symmetry with the other title-bar controls and
     * potential future use (e.g. updating its tooltip text on toggle).
     */
    @FXML
    private Button themeToggleButton;

    /**
     * The {@link SVGPath} graphic inside the minimise button.
     * Content is set once, in {@link #initialize()}, from
     * {@link #WINDOW_ICON_MINIMISE} — it never changes at runtime, unlike
     * {@link #themeToggleIcon}.
     */
    @FXML
    private SVGPath minimiseIcon;

    /**
     * The {@link SVGPath} graphic inside the maximise/restore button.
     * Content is set once, in {@link #initialize()}, from
     * {@link #WINDOW_ICON_MAXIMISE}.
     */
    @FXML
    private SVGPath maximiseIcon;

    /**
     * The {@link SVGPath} graphic inside the close button.
     * Content is set once, in {@link #initialize()}, from
     * {@link #WINDOW_ICON_CLOSE}.
     */
    @FXML
    private SVGPath closeIcon;

    /**
     * Displays all narrative text, prompts, and game output.
     * Defined as read-only and word-wrapping in {@code GamePanel.fxml}.
     * Injected by {@link javafx.fxml.FXMLLoader} after the FXML is parsed.
     */
    @FXML
    private TextArea narrativeArea;

    /**
     * The player's text entry field. Disabled until the game engine calls
     * {@link com.intro.io.GuiIO#prompt(String)}.
     * Injected by {@link javafx.fxml.FXMLLoader} after the FXML is parsed.
     */
    @FXML
    private TextField inputField;

    /**
     * Submits the player's input to the game engine. Disabled until the game
     * engine calls {@link com.intro.io.GuiIO#prompt(String)}.
     * Injected by {@link javafx.fxml.FXMLLoader} after the FXML is parsed.
     */
    @FXML
    private Button submitButton;

    // -------------------------------------------------------------------------
    // Window control dependencies — set by GameWindow after FXML load
    // -------------------------------------------------------------------------

    /**
     * The OS window for this application.
     *
     * <p>
     *     Used exclusively by the drag-to-move handlers
     *     ({@link #onTitleBarPressed(MouseEvent)},
     *     {@link #onTitleBarDragged(MouseEvent)}) to read and update the
     *     window's screen position. Not used for minimise/maximise/close — those
     *     are delegated to {@link #windowControls} so that the handlers can be
     *     tested without a running JavaFX platform.
     * </p>
     *
     * <p>
     *     Set by {@link GameWindow} via {@link #setStage(Stage)} after
     *     {@link javafx.fxml.FXMLLoader#load()} returns. {@code null} until
     *     that call is made; the drag handlers guard against {@code null} and
     *     return immediately, so the application will not crash if dragging is
     *     attempted before the stage is wired in.
     * </p>
     */
    private Stage stage;

    /**
     * Abstracts the minimise, maximise/restore, and close operations for the
     * OS window.
     *
     * <p>
     *     Set by {@link GameWindow} via {@link #setWindowControls(WindowControls)}
     *     after loading the FXML. In production the implementation delegates to
     *     the real {@link Stage}. In tests a Mockito mock is injected, removing
     *     the need for a running JavaFX platform.
     * </p>
     *
     * <p>
     *     {@code null} until {@link #setWindowControls(WindowControls)} is called.
     *     The three handler methods ({@link #handleMinimise()},
     *     {@link #handleMaximise()}, {@link #handleClose()}) log a warning and
     *     return early if this field is {@code null}, so the application will not
     *     crash if a button is somehow clicked before wiring is complete.
     * </p>
     */
    private WindowControls windowControls;

    // -------------------------------------------------------------------------
    // Theme state
    // -------------------------------------------------------------------------

    /**
     * CSS class added to {@link #rootPane} while the light theme is active.
     * Must match the {@code .root.light-theme} selectors in {@code game.css}.
     */
    static final String LIGHT_THEME_STYLE_CLASS = "light-theme";

    /**
     * Property key for the moon glyph in {@code icons.properties}, used as
     * the fallback constant name if the key is ever renamed there.
     */
    static final String THEME_ICON_KEY_MOON = "ui.icon.theme.moon";

    /**
     * Property key for the sun glyph in {@code icons.properties}.
     */
    static final String THEME_ICON_KEY_SUN = "ui.icon.theme.sun";

    /**
     * SVGPath "d" content for the moon glyph, shown on the theme toggle
     * button while the dark theme is active (indicating "currently dark").
     *
     * <p>
     *     Loaded from {@code icons.properties} via {@link IconRegistry}
     *     rather than hard-coded here, so the icon's vector data lives in a
     *     resource file — not Java source — and can be edited without a
     *     recompile. Sourced from the reference {@code moon.svg} kept at
     *     {@code src/main/resources/com/intro/ui/icons/} for provenance.
     *     That source SVG uses {@code fill-rule="evenodd"} (an inner circle
     *     is subtracted from an outer circle to form the crescent), so
     *     {@link #initialize()} sets {@link SVGPath#setFillRule} to
     *     {@link javafx.scene.shape.FillRule#EVEN_ODD} on
     *     {@link #themeToggleIcon} — without it, JavaFX's default non-zero
     *     winding rule would render a solid disc instead of a crescent. The
     *     literal below is only a defensive fallback used if the
     *     {@code ui.icon.theme.moon} key is ever missing from the properties
     *     file.
     * </p>
     */
    static final String THEME_ICON_MOON = IconRegistry.getPath(THEME_ICON_KEY_MOON,
            "M12.226 2.003a9.97 9.97 0 0 0-7.297 2.926c-3.905 3.905-3.905 10.237 0 14.142s10.237 3.905 14.142 0a9.97"
                    + " 9.97 0 0 0 2.926-7.297a10 10 0 0 0-.337-2.368a15 15 0 0 1-1.744 1.436c-1.351.949-2.733 1.563-3.986 1"
                    + ".842c-1.906.423-3.214.032-3.93-.684s-1.107-2.024-.684-3.93c.279-1.253.893-2.635 1.842-3.986c.414-.592"
                    + ".893-1.177 1.436-1.744a10 10 0 0 0-2.368-.337m5.43 15.654a7.96 7.96 0 0 0 2.251-4.438c-3.546 2.045-7."
                    + "269 2.247-9.321.195s-1.85-5.775.195-9.321a8 8 0 1 0 6.876 13.564");

    /**
     * SVGPath "d" content for the sun glyph, shown on the theme toggle button
     * while the light theme is active (indicating "currently light").
     *
     * <p>
     *     Loaded from {@code icons.properties} via {@link IconRegistry}; see
     *     {@link #THEME_ICON_MOON} for why, including the
     *     {@code fill-rule="evenodd"} / {@link javafx.scene.shape.FillRule#EVEN_ODD}
     *     requirement (the sun's ring is a circle with a smaller circle
     *     subtracted to leave a hollow centre). Sourced from the reference
     *     {@code sun.svg} kept at {@code src/main/resources/com/intro/ui/icons/}.
     *     The literal below is only a defensive fallback used if the
     *     {@code ui.icon.theme.sun} key is ever missing from the properties
     *     file.
     * </p>
     */
    static final String THEME_ICON_SUN = IconRegistry.getPath(THEME_ICON_KEY_SUN,
            "M12 16a4 4 0 1 0 0-8a4 4 0 0 0 0 8m0 2a6 6 0 1 0 0-12a6 6 0 0 0 0 12M11 0h2v4.062a8 8 0 0 0-2 0zM7.094 "
                    + "5.68L4.222 2.808L2.808 4.222L5.68 7.094A8 8 0 0 1 7.094 5.68M4.062 11H0v2h4.062a8 8 0 0 1 0-2m1.618 "
                    + "5.906l-2.872 2.872l1.414 1.414l2.872-2.872a8 8 0 0 1-1.414-1.414M11 19.938V24h2v-4.062a8 8 0 0 1-2 0"
                    + "m5.906-1.618l2.872 2.872l1.414-1.414l-2.872-2.872a8 8 0 0 1-1.414 1.414M19.938 13H24v-2h-4.062a8 8 0"
                    + " 0 1 0 2M18.32 7.094l2.872-2.872l-1.414-1.414l-2.872 2.872c.528.41 1.003.886 1.414 1.414");

    /**
     * Whether the light theme is currently active. Defaults to {@code false}
     * (dark theme, matching {@code game.css}'s un-scoped default rules).
     */
    private boolean lightThemeActive = false;

    // -------------------------------------------------------------------------
    // Window control icons — static, never change at runtime
    // -------------------------------------------------------------------------

    /**
     * Property key for the minimise icon in {@code icons.properties}.
     */
    static final String WINDOW_ICON_KEY_MINIMISE = "ui.icon.window.minimise";

    /**
     * Property key for the maximise/restore icon in {@code icons.properties}.
     */
    static final String WINDOW_ICON_KEY_MAXIMISE = "ui.icon.window.maximise";

    /**
     * Property key for the close icon in {@code icons.properties}.
     */
    static final String WINDOW_ICON_KEY_CLOSE = "ui.icon.window.close";

    /**
     * SVGPath "d" content for the minimise button — a single horizontal bar.
     *
     * <p>
     *     Loaded from {@code icons.properties} via {@link IconRegistry}.
     *     Unlike {@link #THEME_ICON_MOON} / {@link #THEME_ICON_SUN}, this
     *     icon never changes at runtime; it is set once on {@link #minimiseIcon}
     *     in {@link #initialize()}. The literal below is only a defensive
     *     fallback used if the {@code ui.icon.window.minimise} key is ever
     *     missing from the properties file.
     * </p>
     */
    static final String WINDOW_ICON_MINIMISE = IconRegistry.getPath(WINDOW_ICON_KEY_MINIMISE,
            "M2.5 12A1.5 1.5 0 0 1 4 10.5h16a1.5 1.5 0 0 1 0 3H4A1.5 1.5 0 0 1 2.5 12");

    /**
     * SVGPath "d" content for the maximise/restore button — a hollow square.
     *
     * <p>
     *     Loaded from {@code icons.properties} via {@link IconRegistry}; see
     *     {@link #WINDOW_ICON_MINIMISE} for why. Set once on
     *     {@link #maximiseIcon} in {@link #initialize()}. The literal below
     *     is only a defensive fallback used if the
     *     {@code ui.icon.window.maximise} key is ever missing from the
     *     properties file.
     * </p>
     */
    static final String WINDOW_ICON_MAXIMISE = IconRegistry.getPath(WINDOW_ICON_KEY_MAXIMISE,
            "M21 2H3a1 1 0 0 0-1 1v18a1 1 0 0 0 1 1h18a1 1 0 0 0 1-1V3a1 1 0 0 0-1-1m-1 18H4V4h16Z");

    /**
     * SVGPath "d" content for the close button — an X.
     *
     * <p>
     *     Loaded from {@code icons.properties} via {@link IconRegistry}; see
     *     {@link #WINDOW_ICON_MINIMISE} for why. Set once on
     *     {@link #closeIcon} in {@link #initialize()}. The literal below is
     *     only a defensive fallback used if the {@code ui.icon.window.close}
     *     key is ever missing from the properties file.
     * </p>
     */
    static final String WINDOW_ICON_CLOSE = IconRegistry.getPath(WINDOW_ICON_KEY_CLOSE,
            "M6.225 4.811a1 1 0 0 0-1.414 1.414L10.586 12L4.81 17.775a1 1 0 1 0 1.414 1.414L12 13.414l5.775 5.775a1 1"
                    + " 0 0 0 1.414-1.414L13.414 12l5.775-5.775a1 1 0 0 0-1.414-1.414L12 10.586z");

    // -------------------------------------------------------------------------
    // Drag-to-move state
    // -------------------------------------------------------------------------

    /**
     * Horizontal offset (in screen pixels) between the cursor and the window's
     * left edge at the moment the title bar was pressed.
     * Used by {@link #onTitleBarDragged(MouseEvent)} to reposition the window.
     */
    private double dragOffsetX;

    /**
     * Vertical offset (in screen pixels) between the cursor and the window's
     * top edge at the moment the title bar was pressed.
     * Used by {@link #onTitleBarDragged(MouseEvent)} to reposition the window.
     */
    private double dragOffsetY;

    // -------------------------------------------------------------------------
    // Concurrency
    // -------------------------------------------------------------------------

    /**
     * Thread-safe queue (capacity 1) used to pass player input from the
     * JavaFX Application Thread to the blocked game-engine thread.
     *
     * <p>
     *     Capacity is 1 so that {@link #handleSubmit()} cannot accidentally
     *     enqueue a second answer before the game thread has consumed the first
     *     — though in practice this is already prevented by disabling the
     *     controls on submit.
     * </p>
     */
    private final LinkedBlockingQueue<String> inputQueue = new LinkedBlockingQueue<>(1);

    // -------------------------------------------------------------------------
    // FXML lifecycle
    // -------------------------------------------------------------------------

    /**
     * Called automatically by {@link javafx.fxml.FXMLLoader} after all
     * {@link FXML}-annotated fields have been injected.
     *
     * <p>
     *     This is the FXML equivalent of a constructor body: it is safe to
     *     reference {@link #narrativeArea}, {@link #inputField}, and
     *     {@link #submitButton} here, whereas they would be {@code null} if
     *     accessed in an actual constructor (injection has not happened yet).
     * </p>
     *
     * <p>
     *     Currently used only for logging. Add any post-injection setup here
     *     in future (for example, binding properties, attaching change
     *     listeners, or setting up key bindings).
     * </p>
     */
    @FXML
    private void initialize() {
        logger.debug("GamePanel controller initialised — FXML injection complete");
        themeToggleIcon.setFillRule(FillRule.EVEN_ODD);
        themeToggleIcon.setContent(themeIconContent(lightThemeActive));
        minimiseIcon.setContent(WINDOW_ICON_MINIMISE);
        maximiseIcon.setContent(WINDOW_ICON_MAXIMISE);
        closeIcon.setContent(WINDOW_ICON_CLOSE);
    }

    // -------------------------------------------------------------------------
    // Wiring setters — called by GameWindow after FXMLLoader.load()
    // -------------------------------------------------------------------------

    /**
     * Provides the {@link Stage} reference needed for drag-to-move support.
     *
     * <p>
     *     Must be called by {@link GameWindow} after
     *     {@link javafx.fxml.FXMLLoader#load()} returns and before the stage
     *     is shown, so that dragging works from the first frame. The
     *     {@link Stage} is only used to read and update the window's screen
     *     position in {@link #onTitleBarPressed(MouseEvent)} and
     *     {@link #onTitleBarDragged(MouseEvent)}.
     * </p>
     *
     * @param stage the primary stage; must not be {@code null}.
     * @throws IllegalArgumentException if {@code stage} is {@code null}.
     */
    public void setStage(Stage stage) {
        if (stage == null) {
            throw new IllegalArgumentException("stage must not be null");
        }
        this.stage = stage;
        logger.debug("Stage reference wired into GamePanel (for drag-to-move)");
    }

    /**
     * Provides the {@link WindowControls} implementation used by the three
     * window-control button handlers.
     *
     * <p>
     *     Must be called by {@link GameWindow} after
     *     {@link javafx.fxml.FXMLLoader#load()} returns and before the stage
     *     is shown, so that clicking a button never encounters a {@code null}
     *     reference in production. In tests a Mockito mock is injected instead,
     *     removing the need for a real {@link Stage}.
     * </p>
     *
     * @param windowControls the window control implementation; must not be
     *                       {@code null}.
     * @throws IllegalArgumentException if {@code windowControls} is {@code null}.
     */
    public void setWindowControls(WindowControls windowControls) {
        if (windowControls == null) {
            throw new IllegalArgumentException("windowControls must not be null");
        }
        this.windowControls = windowControls;
        logger.debug("WindowControls wired into GamePanel");
    }

    // -------------------------------------------------------------------------
    // FXML event handlers — window controls
    // Package-private so that unit tests in com.intro.ui can call them directly.
    // @FXML is compatible with package-private access; FXMLLoader uses
    // reflection with setAccessible(true) regardless of visibility.
    // -------------------------------------------------------------------------

    /**
     * Minimises (iconifies) the application window to the taskbar.
     *
     * <p>
     *     Triggered by {@code onAction="#handleMinimise"} on the minimise button
     *     in {@code GamePanel.fxml}. Runs on the JavaFX Application Thread.
     *     Delegates to {@link WindowControls#minimise()}.
     * </p>
     *
     * <p>
     *     Package-private so that unit tests in the same package can invoke it
     *     directly after injecting a {@link WindowControls} mock, without needing
     *     the JavaFX Application Thread to fire the event.
     * </p>
     */
    @FXML
    void handleMinimise() {
        if (windowControls == null) {
            logger.warn("handleMinimise invoked but WindowControls has not been set");
            return;
        }
        logger.debug("Minimise button clicked");
        windowControls.minimise();
    }

    /**
     * Toggles the application window between maximised and restored states.
     *
     * <p>
     *     Triggered by {@code onAction="#handleMaximise"} on the maximise button
     *     in {@code GamePanel.fxml}, and also by
     *     {@link #onTitleBarDoubleClicked(MouseEvent)} when the click count is 2.
     *     Runs on the JavaFX Application Thread. Delegates to
     *     {@link WindowControls#toggleMaximise()}.
     * </p>
     *
     * <p>
     *     Package-private so that unit tests in the same package can invoke it
     *     directly after injecting a {@link WindowControls} mock.
     * </p>
     */
    @FXML
    void handleMaximise() {
        if (windowControls == null) {
            logger.warn("handleMaximise invoked but WindowControls has not been set");
            return;
        }
        logger.debug("Maximise button clicked");
        windowControls.toggleMaximise();
    }

    /**
     * Closes the application window and terminates the JVM.
     *
     * <p>
     *     Triggered by {@code onAction="#handleClose"} on the close button in
     *     {@code GamePanel.fxml}. Runs on the JavaFX Application Thread.
     *     Delegates to {@link WindowControls#close()}.
     * </p>
     *
     * <p>
     *     Package-private so that unit tests in the same package can invoke it
     *     directly after injecting a {@link WindowControls} mock, without the
     *     handler's production implementation calling
     *     {@link System#exit(int)} and terminating the test JVM.
     * </p>
     */
    @FXML
    void handleClose() {
        if (windowControls == null) {
            logger.warn("handleClose invoked but WindowControls has not been set");
            return;
        }
        logger.debug("Close button clicked");
        windowControls.close();
    }

    // -------------------------------------------------------------------------
    // FXML event handler — theme toggle
    // -------------------------------------------------------------------------

    /**
     * Switches the window between the dark (default) and light themes.
     *
     * <p>
     *     Triggered by {@code onAction="#handleToggleTheme"} on the theme
     *     toggle button in {@code GamePanel.fxml}. Runs on the JavaFX
     *     Application Thread.
     * </p>
     *
     * <p>Flips {@link #lightThemeActive} and then:</p>
     * <ul>
     *     <li>Adds or removes {@link #LIGHT_THEME_STYLE_CLASS} on
     *         {@link #rootPane}, which activates or deactivates every
     *         {@code .root.light-theme} rule in {@code game.css}.</li>
     *     <li>Updates {@link #themeToggleIcon}'s path data via
     *         {@link #themeIconContent(boolean)} so the icon always reflects
     *         the theme that is now active.</li>
     * </ul>
     *
     * <p>
     *     Package-private so that unit tests in the same package can invoke it
     *     directly. Only touches {@link #rootPane} and {@link #themeToggleIcon},
     *     both plain-old JavaFX node references, so it is safe to call from a
     *     test as long as those fields have been populated (e.g. via
     *     reflection) — no live {@link Stage} or {@link WindowControls} is
     *     required.
     * </p>
     */
    @FXML
    void handleToggleTheme() {
        lightThemeActive = !lightThemeActive;
        logger.debug("Theme toggled — light theme now {}", lightThemeActive);
        if (lightThemeActive) {
            rootPane.getStyleClass().add(LIGHT_THEME_STYLE_CLASS);
        } else {
            rootPane.getStyleClass().remove(LIGHT_THEME_STYLE_CLASS);
        }
        themeToggleIcon.setContent(themeIconContent(lightThemeActive));
    }

    /**
     * Returns the SVGPath "d" content that should be displayed on the theme
     * toggle button for the given theme state.
     *
     * <p>
     *     Pure function — no field access, no side effects — so it can be
     *     unit-tested directly without any JavaFX platform or FXML injection.
     * </p>
     *
     * @param lightThemeActive {@code true} if the light theme is (about to be)
     *                         active, {@code false} for the dark theme.
     * @return {@link #THEME_ICON_SUN} when {@code lightThemeActive} is
     *         {@code true} (the icon shows the currently-active theme, sun for
     *         light); {@link #THEME_ICON_MOON} otherwise.
     */
    static String themeIconContent(boolean lightThemeActive) {
        return lightThemeActive ? THEME_ICON_SUN : THEME_ICON_MOON;
    }

    // -------------------------------------------------------------------------
    // FXML event handlers — title bar drag and double-click
    // Private: these are implementation details of the drag mechanic and do not
    // need to be called from tests (they are tightly coupled to MouseEvent
    // coordinates and Stage position, which are impractical to fake).
    // -------------------------------------------------------------------------

    /**
     * Records the cursor-to-window-corner offset when the player presses the
     * mouse button on the title bar.
     *
     * <p>
     *     Stores the horizontal and vertical distances between the cursor's
     *     screen position and the window's top-left corner into
     *     {@link #dragOffsetX} and {@link #dragOffsetY}. These are consumed
     *     by {@link #onTitleBarDragged(MouseEvent)} on each subsequent drag event
     *     to keep the window aligned with the cursor.
     * </p>
     *
     * <p>
     *     Does nothing if {@link #stage} is {@code null} (i.e. before
     *     {@link #setStage(Stage)} has been called).
     * </p>
     *
     * @param event the mouse-pressed event supplied by the JavaFX runtime.
     */
    @FXML
    private void onTitleBarPressed(MouseEvent event) {
        if (stage == null) return;
        dragOffsetX = stage.getX() - event.getScreenX();
        dragOffsetY = stage.getY() - event.getScreenY();
    }

    /**
     * Moves the window to follow the cursor during a title bar drag.
     *
     * <p>
     *     Adds the offsets recorded in {@link #onTitleBarPressed(MouseEvent)}
     *     to the current cursor screen position to compute the new window
     *     origin, then updates {@link Stage#setX(double)} and
     *     {@link Stage#setY(double)} accordingly.
     * </p>
     *
     * <p>
     *     Does nothing if {@link #stage} is {@code null}.
     * </p>
     *
     * @param event the mouse-dragged event supplied by the JavaFX runtime.
     */
    @FXML
    private void onTitleBarDragged(MouseEvent event) {
        if (stage == null) return;
        stage.setX(event.getScreenX() + dragOffsetX);
        stage.setY(event.getScreenY() + dragOffsetY);
    }

    /**
     * Toggles maximise/restore when the player double-clicks the title bar.
     *
     * <p>
     *     Checks {@link MouseEvent#getClickCount()}; only acts when the count is
     *     exactly 2. Single clicks are ignored here (they are handled separately
     *     as the start of a potential drag in
     *     {@link #onTitleBarPressed(MouseEvent)}). Delegates to
     *     {@link #handleMaximise()} so the behaviour is identical to clicking
     *     the maximise button.
     * </p>
     *
     * @param event the mouse-clicked event supplied by the JavaFX runtime.
     */
    @FXML
    private void onTitleBarDoubleClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
            logger.debug("Title bar double-clicked — toggling maximise");
            handleMaximise();
        }
    }

    // -------------------------------------------------------------------------
    // FXML event handler — player input
    // -------------------------------------------------------------------------

    /**
     * Handles the Submit action, triggered either by clicking the Submit
     * button or by pressing Enter inside the input field.
     *
     * <p>
     *     Both controls wire to this method via {@code onAction="#handleSubmit"}
     *     in {@code GamePanel.fxml}. Runs on the JavaFX Application Thread.
     * </p>
     *
     * <p>Sequence of events:</p>
     * <ol>
     *     <li>Reads and trims the current text from {@link #inputField}.</li>
     *     <li>Clears the field immediately so the UI feels responsive.</li>
     *     <li>Disables both controls to prevent double-submission.</li>
     *     <li>Offers the trimmed text to {@link #inputQueue}.
     *         In normal flow the queue is always empty at this point because
     *         the controls are disabled until the game thread consumes the
     *         previous value. If {@link java.util.concurrent.LinkedBlockingQueue#offer}
     *         returns {@code false} (queue unexpectedly full), the input is
     *         logged as a warning and discarded rather than silently lost.
     *     </li>
     * </ol>
     */
    @FXML
    private void handleSubmit() {
        String text = this.inputField.getText().trim();
        logger.debug("Submit fired — input: [{}]", text);
        this.inputField.clear();
        setInputEnabled(false);
        if (!this.inputQueue.offer(text)) {
            logger.warn("Input queue full — player input discarded: [{}]", text);
        }
    }

    // -------------------------------------------------------------------------
    // Public API — called by GuiIO from the game-engine background thread
    // -------------------------------------------------------------------------

    /**
     * Appends {@code text} to the narrative area and scrolls to the bottom
     * so the player always sees the latest output.
     *
     * <p>
     *     <strong>Must be called on the JavaFX Application Thread.</strong>
     *     {@link com.intro.io.GuiIO} ensures this by wrapping every call in
     *     the UI scheduler (in production, {@link javafx.application.Platform#runLater(Runnable)}).
     * </p>
     *
     * <p>
     *     JavaFX's {@link TextArea#appendText(String)} does not auto-scroll.
     *     Calling {@link TextArea#positionCaret(int)} at the end of the text
     *     after each append moves the caret to the bottom, which causes the
     *     scroll pane inside the {@code TextArea} to follow.
     * </p>
     *
     * @param text the text to append; must not be {@code null}.
     */
    public void appendNarrativeText(String text) {
        narrativeArea.appendText(text);
        narrativeArea.positionCaret(narrativeArea.getText().length());
    }

    /**
     * Removes all text currently displayed in the narrative area.
     *
     * <p>
     *     <strong>Must be called on the JavaFX Application Thread.</strong>
     *     {@link com.intro.io.GameIO#clearOutput()} ensures this in the GUI
     *     implementation ({@link com.intro.io.GuiIO}) by wrapping the call in
     *     the UI scheduler, exactly as {@link #appendNarrativeText(String)} is.
     * </p>
     *
     * <p>
     *     Used by {@link com.intro.scene.PlayerSetupScene} to wipe the
     *     name-entry dialogue from view immediately before printing the
     *     "ENCHANTED FOREST" banner, so that once the player has confirmed
     *     their character name, only the game's own narrative remains visible
     *     in the text area from that point on.
     * </p>
     */
    public void clearNarrativeText() {
        narrativeArea.clear();
    }

    /**
     * Returns how many monospace characters fit on one line in the narrative area.
     *
     * <p>
     *     <strong>Must be called on the JavaFX Application Thread.</strong>
     *     Accounts for {@code TextArea} padding, the inner content margins defined
     *     in {@code game.css}, and a visible vertical scrollbar when present.
     * </p>
     *
     * @return character count for one banner line
     */
    public int getNarrativeLineWidthChars() {
        double areaWidth = narrativeArea.getWidth();
        if (areaWidth <= 0) {
            return GameConfig.getInt("io.narrative.fallback.width", 40);
        }

        double available = availableNarrativePixels(
                areaWidth,
                narrativeArea.getPadding(),
                getNarrativeContentHorizontalPadding(),
                getVisibleVerticalScrollbarWidth());

        return NarrativeFormatter.charsPerLine(available, narrativeArea.getFont());
    }

    /**
     * Computes horizontal pixel space available for narrative text after padding
     * and an optional vertical scrollbar.
     *
     * <p>
     *     Package-private static helper so the arithmetic can be unit-tested
     *     without a running JavaFX platform.
     * </p>
     *
     * @param areaWidth         total width of the narrative {@link TextArea}
     * @param outerPadding      padding applied directly on the {@code TextArea}
     * @param contentPaddingH   combined left + right padding on the inner
     *                          {@code .content} region
     * @param scrollbarWidth    width reserved by a visible vertical scrollbar
     * @return remaining horizontal pixels for text
     */
    static double availableNarrativePixels(
            double areaWidth, Insets outerPadding, double contentPaddingH, double scrollbarWidth) {
        return areaWidth - outerPadding.getLeft() - outerPadding.getRight()
                - contentPaddingH - scrollbarWidth;
    }

    /**
     * Returns combined left + right padding on the inner {@code .content} region
     * of the narrative {@link TextArea}, as applied by {@code game.css}.
     *
     * <p>
     *     <strong>Must be called on the JavaFX Application Thread.</strong>
     *     Falls back to {@code io.narrative.content.padding.fallback} when the
     *     content node is not yet in the scene graph.
     * </p>
     *
     * @return horizontal content padding in pixels
     */
    private double getNarrativeContentHorizontalPadding() {
        var content = narrativeArea.lookup(".content");
        if (content instanceof Region region) {
            var insets = region.getPadding();
            return insets.getLeft() + insets.getRight();
        }
        return GameConfig.getDouble("io.narrative.content.padding.fallback", 120);
    }

    /**
     * Returns the width reserved by a visible vertical scrollbar, if any.
     *
     * <p>
     *     <strong>Must be called on the JavaFX Application Thread.</strong>
     *     Uses {@link ScrollBar#prefWidth(double)} when layout width is not yet
     *     available.
     * </p>
     *
     * @return scrollbar width in pixels, or {@code 0} when no scrollbar is shown
     */
    private double getVisibleVerticalScrollbarWidth() {
        var scrollbarNode = narrativeArea.lookup(".scroll-bar:vertical");
        if (scrollbarNode instanceof ScrollBar vertical && vertical.isVisible()) {
            double width = vertical.getWidth();
            return width > 0 ? width : Math.max(0, vertical.prefWidth(-1));
        }
        return 0;
    }

    /**
     * Formats and appends a three-line banner (separator, centered title, separator).
     *
     * <p>
     *     <strong>Must be called on the JavaFX Application Thread.</strong>
     *     {@link com.intro.io.GuiIO#printBanner(String)} schedules this via the
     *     UI thread dispatcher.
     * </p>
     *
     * @param title the banner title to center; must not be {@code null}
     */
    public void appendBanner(String title) {
        int width = getNarrativeLineWidthChars();
        appendNarrativeText(NarrativeFormatter.separatorLine(width) + "\n");
        appendNarrativeText(NarrativeFormatter.centeredLine(title, width) + "\n");
        appendNarrativeText(NarrativeFormatter.separatorLine(width) + "\n");
    }

    /**
     * Blocks the calling thread until the player submits an answer, then
     * returns the trimmed input string.
     *
     * <p>
     *     <strong>Must be called from the game-engine background thread,
     *     never from the JavaFX Application Thread</strong> — blocking the JAT
     *     would freeze the entire UI.
     * </p>
     *
     * <p>
     *     Internally calls {@link LinkedBlockingQueue#take()}, which suspends
     *     the game thread until {@link #handleSubmit()} (running on the JAT)
     *     places the player's input into the queue.
     * </p>
     *
     * @return the player's trimmed input; never {@code null}, but may be an
     *         empty string if the player clicked Submit without typing.
     * @throws InterruptedException if the game thread is interrupted while
     *         waiting, for example because the JVM is shutting down after the
     *         window is closed.
     */
    public String awaitPlayerInput() throws InterruptedException {
        logger.debug("Awaiting player input on game thread");
        String input = inputQueue.take();
        logger.debug("Player input received: [{}]", input);
        return input;
    }

    /**
     * Enables or disables the player input controls.
     *
     * <p>
     *     Called with {@code true} by {@link com.intro.io.GuiIO#prompt(String)}
     *     just before the game thread blocks, giving the player a visual cue
     *     that a response is expected. Called with {@code false} inside
     *     {@link #handleSubmit()} after the player submits, preventing
     *     double-entry.
     * </p>
     *
     * <p>
     *     <strong>Must be called on the JavaFX Application Thread.</strong>
     * </p>
     *
     * @param enabled {@code true} to allow typing and submitting;
     *                {@code false} to lock the controls.
     */
    public void setInputEnabled(boolean enabled) {
        logger.debug("setInputEnabled({})", enabled);
        inputField.setDisable(!enabled);
        submitButton.setDisable(!enabled);
        if (enabled) {
            inputField.requestFocus();
        }
    }
}
