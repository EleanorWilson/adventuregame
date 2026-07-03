package com.intro;

import com.intro.engine.GameEngine;
import com.intro.io.ConsoleIO;
import com.intro.ui.GameWindow;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Entry point for the game.
 * <p>Feeds a real terminal input/output ({@link ConsoleIO}) into
 * a new {@link GameEngine}, then starts the game loop.</p>
 */
public class App {

    private static final Logger logger = LogManager.getLogger(App.class);

    /**
     * Application entry point
     * @param args command line arguments (not used).
     */
    public static void main(String[] args) {
        logger.info("App starting");
        GameWindow.main(args);
    }

}
