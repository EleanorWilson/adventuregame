package com.intro;

import com.intro.ui.GameWindow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Application entry point.
 *
 * <p>
 *     Delegates immediately to {@link GameWindow#main(String[])}, which
 *     initialises the JavaFX toolkit and opens the game window.
 * </p>
 *
 * <p>
 *     To run the game in headless console mode instead (for example, during
 *     automated testing or on a machine without a display), replace the body
 *     of {@link #main(String[])} with:
 * </p>
 * <pre>{@code
 *     new GameEngine(new ConsoleIO()).run();
 * }</pre>
 */
public class App {

    private static final Logger logger = LogManager.getLogger(App.class);

    /**
     * JVM entry point.  Forwards all arguments to the JavaFX launcher.
     *
     * @param args command-line arguments; forwarded to
     *             {@link GameWindow#main(String[])} and then to the JavaFX
     *             launcher (not used by this application).
     */
    public static void main(String[] args) {
        logger.info("App starting — launching GUI");
        GameWindow.main(args);
    }
}
