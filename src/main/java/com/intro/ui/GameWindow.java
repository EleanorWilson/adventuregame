package com.intro.ui;

import com.intro.config.GameConfig;
import com.intro.engine.GameEngine;
import com.intro.io.GuiIO;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Top-level JavaFX {@link Application} entry point for the game.
 *
 * <p>
 *     Loads {@code GamePanel.fxml} with UI strings resolved from
 *     {@code messages.properties} via a {@link ResourceBundle}, wires the
 *     {@link GamePanel} controller with its {@link Stage} and
 *     {@link WindowControls} dependencies, configures the OS window using
 *     dimensions from {@link GameConfig}, and starts the game engine on a
 *     background daemon thread.
 * </p>
 *
 * <p>
 *     The {@link Stage} uses {@link StageStyle#UNDECORATED} to remove the
 *     native OS title bar, which is replaced by the custom title bar defined in
 *     {@code GamePanel.fxml}. Window-control operations (minimise, maximise,
 *     close) are exposed through the {@link WindowControls} interface so that
 *     {@link GamePanel}'s handlers can be unit-tested without a running JavaFX
 *     platform.
 * </p>
 *
 * <p>
 *     <strong>Font loading:</strong> bundled TrueType fonts are registered
 *     with the JavaFX font system via {@link #loadFonts()} at the very start
 *     of {@link #start(Stage)}, before the FXML is loaded and before the
 *     stylesheet is applied. This guarantees that any {@code -fx-font-family}
 *     name in {@code game.css} resolves to the bundled font rather than a
 *     system fallback.
 * </p>
 *
 * <p>
 *     <strong>Edge-drag resizing:</strong> because {@code StageStyle.UNDECORATED}
 *     removes the OS-native resize grips along with the title bar,
 *     {@link WindowResizeHandler#attach(javafx.scene.Node, Stage, double, double)}
 *     is called on the loaded root node to re-implement edge/corner drag
 *     resizing, clamped to the same minimum dimensions applied to the stage
 *     in {@link #configureStage(Stage, Scene, ResourceBundle)}.
 * </p>
 */
public class GameWindow extends Application {

    private static final Logger logger = LogManager.getLogger(GameWindow.class);

    /**
     * Classpath root for all bundled font files.
     * Fonts must live at this path inside {@code src/main/resources} so that
     * Maven packages them into the JAR.
     */
    private static final String FONTS_PATH = "/com/intro/ui/fonts/";

    /**
     * Called by the JavaFX runtime on the JavaFX Application Thread after
     * {@link #main(String[])} invokes {@link Application#launch}. Loads
     * bundled fonts first, then loads the FXML with {@code messages.properties}
     * strings resolved, wires all components, builds the scene from
     * config-driven dimensions, and starts the game engine thread.
     *
     * <p>
     *     Font loading ({@link #loadFonts()}) must happen before
     *     {@link #applyStylesheet(Scene)} so that the JavaFX font registry
     *     already contains the bundled families when the CSS engine first
     *     evaluates the {@code -fx-font-family} rules.
     * </p>
     *
     * @param stage the primary stage provided by the JavaFX runtime.
     * @throws IOException if {@code GamePanel.fxml} cannot be loaded from the
     *                     classpath.
     */
    @Override
    public void start(Stage stage) throws IOException {
        logger.info("GameWindow.start() — loading resources and building UI");

        /*
         * Fonts must be registered before the stylesheet is applied.
         * If a font fails to load, applyStylesheet() will still run and
         * the CSS fallback chain (e.g. serif / monospace) takes over.
         */
        loadFonts();

        ResourceBundle messages = ResourceBundle.getBundle("com.intro.ui.messages");
        logger.debug("Loaded messages resource bundle ({} key(s))", messages.keySet().size());

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/intro/ui/GamePanel.fxml"),
                messages);
        Parent root = loader.load();
        GamePanel controller = loader.getController();

        controller.setStage(stage);
        controller.setWindowControls(buildWindowControls(stage));

        GuiIO guiIO = new GuiIO(controller);

        double width  = GameConfig.getDouble("ui.window.default.width",  900);
        double height = GameConfig.getDouble("ui.window.default.height", 620);
        Scene scene = new Scene(root, width, height);
        applyStylesheet(scene);

        double minWidth  = GameConfig.getDouble("ui.window.min.width",   600);
        double minHeight = GameConfig.getDouble("ui.window.min.height",  400);
        WindowResizeHandler.attach(root, stage, minWidth, minHeight);

        configureStage(stage, scene, messages);
        stage.show();
        logger.info("GameWindow visible ({}x{})", (int) width, (int) height);

        startGameThread(guiIO);
    }

    /**
     * Standard JavaFX entry point; delegates to {@link Application#launch}.
     *
     * @param args forwarded to the JavaFX launcher; not used by this application.
     */
    public static void main(String[] args) {
        logger.info("GameWindow.main() — launching JavaFX application");
        launch(args);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Registers all bundled TrueType fonts with the JavaFX font system.
     *
     * <p>
     *     This method must be called before the scene's stylesheet is applied
     *     (i.e., before {@link #applyStylesheet(Scene)}). JavaFX resolves
     *     {@code -fx-font-family} names at the moment the CSS is first
     *     evaluated; if the font has not yet been registered, the rule silently
     *     falls back to the next family in the {@code -fx-font-family} chain.
     * </p>
     *
     * <p>
     *     To add a new font: place the {@code .ttf} file in
     *     {@code src/main/resources/com/intro/ui/fonts/} and add a
     *     {@link #loadFont(String, String)} call here. The second argument must
     *     be the font's internal family name (visible in the TTF metadata), not
     *     the filename.
     * </p>
     *
     * <p>
     *     Not unit-tested: this method calls {@link Font#loadFont(InputStream, double)},
     *     a JavaFX API that requires an initialised platform. It is exercised by
     *     the application's manual startup path.
     * </p>
     */
    private static void loadFonts() {
        /*
         * Metamorphous — decorative serif font used for the window title label.
         * Internal family name: "Metamorphous"
         */
        loadFont(FONTS_PATH + "Metamorphous-Regular.ttf", "Metamorphous");

        /*
         * Google Sans Code — clean monospaced font used for narrative text,
         * the input field, and the submit button.
         * Internal family name: "Google Sans Code"
         */
        loadFont(FONTS_PATH + "GoogleSansCode-Regular.ttf", "Google Sans Code");
    }

    /**
     * Loads a single TrueType font from the classpath and registers it with
     * the JavaFX font system.
     *
     * <p>
     *     The {@code familyName} parameter is used only for logging — it must
     *     match the font's internal family name (as embedded in the TTF
     *     metadata) for the CSS {@code -fx-font-family} rule to resolve
     *     correctly, but this method does not validate that match. Use a tool
     *     such as <a href="https://fontdrop.info">fontdrop.info</a> to inspect
     *     a font's internal name if styles are not applying at runtime.
     * </p>
     *
     * <p>
     *     If the resource is not found or the stream cannot be read, a warning
     *     is logged and the method returns without throwing, allowing the rest
     *     of startup to continue with CSS fallback fonts.
     * </p>
     *
     * @param resourcePath classpath path to the {@code .ttf} file, e.g.
     *                     {@code "/com/intro/ui/fonts/Metamorphous-Regular.ttf"}.
     * @param familyName   the font's internal family name, used for log messages.
     */
    private static void loadFont(String resourcePath, String familyName) {
        try (InputStream stream = GameWindow.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                logger.warn("Font '{}' not found on classpath at '{}' — CSS will use fallback",
                        familyName, resourcePath);
                return;
            }
            Font font = Font.loadFont(stream, 14);
            if (font == null) {
                logger.warn("Font.loadFont() returned null for '{}' — file may be corrupt",
                        familyName);
            } else {
                logger.debug("Registered font '{}' from '{}'", familyName, resourcePath);
            }
        } catch (IOException e) {
            logger.warn("Could not close font stream for '{}': {}", familyName, e.getMessage());
        }
    }

    /**
     * Returns a {@link WindowControls} implementation backed by the real
     * {@link Stage}. Keeps stage interaction out of {@link GamePanel} so that
     * the window-control handlers can be tested with a mock.
     *
     * @param stage the primary stage; must not be {@code null}.
     * @return a {@link WindowControls} delegating to {@code stage}.
     */
    private WindowControls buildWindowControls(Stage stage) {
        return new WindowControls() {
            @Override
            public void minimise() {
                logger.debug("WindowControls.minimise()");
                stage.setIconified(true);
            }

            @Override
            public void toggleMaximise() {
                boolean next = !stage.isMaximized();
                logger.debug("WindowControls.toggleMaximise() → {}", next);
                stage.setMaximized(next);
            }

            @Override
            public void close() {
                logger.info("WindowControls.close() — Platform.exit() + System.exit(0)");
                Platform.exit();
                System.exit(0);
            }
        };
    }

    /**
     * Applies {@code game.css} to {@code scene}. Logs a warning and continues
     * with default JavaFX styles if the file is not found on the classpath.
     *
     * @param scene the scene to style.
     */
    private void applyStylesheet(Scene scene) {
        URL cssUrl = getClass().getResource("/com/intro/ui/game.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
            logger.debug("Stylesheet applied: {}", cssUrl);
        } else {
            logger.warn("game.css not found on classpath — using default JavaFX styles");
        }
    }

    /**
     * Applies {@link StageStyle#UNDECORATED}, sets the taskbar title from the
     * {@code messages} bundle, attaches the scene, enforces minimum dimensions
     * from {@link GameConfig}, and registers a fallback close handler for OS
     * shortcuts (e.g. Alt+F4).
     *
     * @param stage    the stage to configure.
     * @param scene    the scene to attach.
     * @param messages the bundle supplying the {@code ui.window.title} string.
     */
    private void configureStage(Stage stage, Scene scene, ResourceBundle messages) {
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle(messages.getString("ui.window.title"));
        stage.setScene(scene);
        stage.setMinWidth(GameConfig.getDouble("ui.window.min.width",   600));
        stage.setMinHeight(GameConfig.getDouble("ui.window.min.height", 400));
        stage.setOnCloseRequest(event -> {
            logger.info("OS close request received — shutting down (exit code 0)");
            Platform.exit();
            System.exit(0);
        });
    }

    /**
     * Starts the game engine on a background daemon thread so it can block
     * waiting for player input without freezing the JavaFX Application Thread.
     *
     * @param guiIO the IO bridge wired to the {@link GamePanel} controller.
     */
    private void startGameThread(GuiIO guiIO) {
        Thread gameThread = new Thread(() -> {
            logger.info("Game engine thread started");
            try {
                new GameEngine(guiIO).run();
                logger.info("Game engine finished normally");
            } catch (Exception e) {
                logger.fatal("Unexpected fatal error in game engine thread", e);
            }
        });
        gameThread.setName("game-engine-thread");
        gameThread.setDaemon(true);
        gameThread.start();
        logger.debug("Game engine daemon thread launched");
    }
}
