package com.intro.ui;

import javafx.stage.StageStyle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.application.Platform;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import com.intro.config.GameConfig;
import com.intro.engine.GameEngine;
import com.intro.io.GuiIO;

import java.util.ResourceBundle;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * JavaFX {@link Application} point for the game.
 * <p>
 *     Loads the {@code GamePanel.fxml} with UI properties resolved from the
 *     {@code messages.properties} file using {@link ResourceBundle}. The {@link GamePanel}
 *     controller is linked with the {@link Stage} and {@link WindowControls}. The window
 *     dimensions are configured with {@link GameConfig} and the game engine is started on a
 *     separate {@code daemon} thread.
 * </p>
 * <p>
 *     The {@link Stage} uses {@link StageStyle#UNDECORATED} to replace the default title bar
 *     with a custom bar, the properties of which are defined in {@code GamePanel.fxml}, which
 *     matches the style of the main window, defined in {@code game.css}. The window controls
 *     ({@code toggle maximise}, {@code minimise} and {@code close}) are abstracted with the
 *     {@link WindowControls} interface.
 * </p>
 * <p>
 *     This class also loads all fonts found within the resources folder ({@code com/intro/ui/fonts/*})
 *     via {@link #loadFonts()} before {@link #start(Stage)}, {@code GamePanel.fxml} and the
 *     {@code game.css} stylesheet are loaded/applied.
 * </p>
 */
@SuppressWarnings("java:S1075")
public class GameWindow extends Application {

    private static final Logger logger = LogManager.getLogger(GameWindow.class);

    /**
     * Path of bundled font files. Fonts must be added to {@code src/main/resources} so that
     * Maven packages them into the JAR.
     */
    private static final String FONTS_PATH = "/com/intro/ui/fonts";

    /**
     * Called by the JavaFX Application Thread after {@link #main(String[])} invokes
     * {@link javafx.application.Application#launch(String...)}. Loads fonts first, then
     * FXML with {@code messages.properties} strings resolved, builds app and starts the game
     * engine thread.
     * <p>
     *     Font loading must come before {@link #applyStylesheet(Scene)} so that JavaFX already
     *     has access to fonts when CSS is loaded.
     * </p>
     * @param stage the primary stage
     * @throws IOException if {@code GamePanel.fxml} cannot be loaded from the path.
     */
    @Override
    public void start(Stage stage) throws IOException {
        logger.info("GameWindow.start() loading resources and building UI");
        loadFonts();
        ResourceBundle messages = ResourceBundle.getBundle("com.intro.ui.messages");
        logger.debug("Loaded messages resource bundle ({} keys(s))", messages.keySet().size());
        FXMLLoader loader = new FXMLLoader(this.getClass().getResource("/com/intro/ui/GamePanel.fxml"), messages);
        Parent root = loader.load();
        GamePanel controller = loader.getController();

        controller.setStage(stage);
        controller.setWindowControls(this.buildWindowControls(stage));

        GuiIO guiIO = new GuiIO(controller);

        double width = GameConfig.getDouble("ui.window.default.width", 900);
        double height = GameConfig.getDouble("ui.window.default.height", 620);
        Scene scene = new Scene(root, width, height);
        this.applyStylesheet(scene);

        double minWidth = GameConfig.getDouble("ui.window.min.width", 600);
        double minHeight = GameConfig.getDouble("ui.window.min.height", 400);
        WindowResizeHandler.attach(root, stage, minWidth, minHeight);

        this.configureStage(stage, scene, messages);
        stage.show();
        logger.info("GameWindow visible ({}x{})", (int) width, (int) height);

        this.startGameThread(guiIO);
    }

    /**
     * JavaFX entry point.
     * @param args not used by this application.
     */
    public static void main(String[] args) {
        logger.info("GameWindow.main() launching application");
        launch(args);
    }

    /**
     * Loads all font resources with JavaFX font system. Must be called before the CSS is
     * applied otherwise default fonts are used.
     * <p>
     *     To add a new font: place the {@code *.ttf} file inside the
     *     {@code src/main/resources/com/intro/ui/fonts} folder and add a {@link #loadFont(String, String)}
     *     call here in this method.
     *     <ul><li>N.B. The second argument, the font family name, must match the internal font family
     *     name found in the metadata of the {@code *.ttf} file.</li></ul>
     * </p>
     */
    private static void loadFonts() {
        loadFont(FONTS_PATH + "Metamorphous-Regular.ttf", "Metamorphous");
        loadFont(FONTS_PATH + "GoogleSansCode-Regular.ttf", "Google Sans Code Regular");
    }

    /**
     * Loads a single {@code *.ttf} (true type) font from the path and registers it with the JavaFX
     * font system.
     * <p>
     *     The {@code familyName} parameter is only used for logging, it must match the font's internal
     *     family name (as embedded in the TTF metadata) for the CSS {@code -fx-font-family} to resolve
     *     correctly.
     *     <blockquote>
     *         <b>N.B.</b> If the font is not loading correctly, inspect the font's internal
     *         name and update in the stylesheet.
     *     </blockquote>
     * </p>
     * @param resourcePath the classpath to the {@code *.ttf} file.
     * @param familyName the font's internal family name, used for log messages.
     */
    private static void loadFont(String resourcePath, String familyName) {
        try (InputStream stream = GameWindow.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                logger.warn("Font '{}' not found on the classpath at '{}' - CSS will use fallback", familyName, resourcePath);
                return;
            }
            Font font = Font.loadFont(stream, 14);
            if (font == null) {
                logger.warn("Font.loadFont() return null for '{}', font may be corrupt", familyName);
            } else {
                logger.warn("Registered font: {} from '{}'", familyName, resourcePath);
            }
        } catch (Exception e) {
            logger.warn("Could not close font stream for '{}':", familyName, e);
        }
    }

    /**
     * Returns a {@link WindowControls} implementation with a real {@link Stage} to keep stage separate
     * from the {@link GamePanel} for testing purposes.
     * @param stage the primary stage, must not be {@code null}.
     * @return {@link WindowControls}
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
                logger.debug("WindowControls.toggleMaximise() -> {}", next);
                stage.setMaximized(next);
            }

            @Override
            public void close() {
                logger.info("WindowControls.close() -> Platform.exit() and System.exit(0)");
                Platform.exit();
                System.exit(0);
            }
        };
    }

    /**
     * Applies the {@code game.css} stylesheet to the {@code scene}. Logs a warning if file not
     * found on path before defaulting to JavaFX default styles.
     * @param scene the scene to style
     */
    private void applyStylesheet(Scene scene) {
        URL cssURL = this.getClass().getResource("/com/intro/ui/game.css");
        if (cssURL != null) {
            scene.getStylesheets().add(cssURL.toExternalForm());
            logger.debug("Stylesheet applied: {}", cssURL);
        } else {
            logger.warn("game.css not found on path, using default JavaFX styles.");
        }
    }

    /**
     * Applies {@link StageStyle#UNDECORATED}, sets the taskbar title from {@code messages} and
     * applies configurations from {@link GameConfig}.
     * @param stage the stage to configure
     * @param scene the scene to attach
     * @param messages the bundle supplying the {@code ui.window.title} string
     */
    private void configureStage(Stage stage, Scene scene, ResourceBundle messages) {
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle(messages.getString("ui.window.title"));
        stage.setScene(scene);
        stage.setMinWidth(GameConfig.getDouble("ui.window.min.width", 600));
        stage.setMinHeight(GameConfig.getDouble("ui.window.min.height", 400));
        stage.setOnCloseRequest(event -> {
            logger.info("OS close request received, shutting down with exit code: 0");
            Platform.exit();
            System.exit(0);
        });
    }

    /**
     * Starts the game engine on a background daemon thread so it can block waiting for player
     * input without freezing the JavaFX Application Thread, thus preventing UI freezes.
     * @param guiIO the IO linked to the {@link GamePanel} controller
     */
    private void startGameThread(GuiIO guiIO) {
        Thread gameThread = new Thread(() -> {
            logger.info("Game engine thread started");
            try {
                 new GameEngine(guiIO).run();
                 logger.info("Game engine finished normally");
            } catch (Exception e) {
                logger.fatal("Unexpected fatal error in the game engine thread", e);
            }
        });
        gameThread.setName("game-engine-thread");
        gameThread.setDaemon(true);
        gameThread.start();
        logger.debug("Game engine daemon thread launched.");
    }
}
