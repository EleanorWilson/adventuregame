package com.intro.ui;

/**
 * Abstracts the three OS-window control operations — minimise, maximise/restore,
 * and close — behind a plain Java interface.
 *
 * <h2>Why this interface exists</h2>
 * <p>
 *     The custom title bar in {@code GamePanel.fxml} contains three buttons that
 *     must interact with the JavaFX {@link javafx.stage.Stage} (to iconify,
 *     maximise, or close the OS window). If {@link GamePanel} held a direct
 *     {@code Stage} reference for this purpose, unit-testing those handlers would
 *     require either a running JavaFX platform or Mockito-mocking a JavaFX class
 *     whose static initialisers may try to load native graphics libraries.
 * </p>
 * <p>
 *     By introducing this interface, {@link GamePanel} depends only on a plain
 *     Java abstraction. Tests inject a Mockito mock of {@code WindowControls};
 *     production code (in {@link GameWindow}) supplies an anonymous implementation
 *     backed by the real {@link javafx.stage.Stage}. Neither side needs to know
 *     about the other's implementation.
 * </p>
 *
 * <h2>Usage</h2>
 * <p>
 *     {@link GameWindow#start(javafx.stage.Stage)} creates the implementation
 *     and passes it to {@link GamePanel#setWindowControls(WindowControls)}.
 *     The three {@code @FXML} handler methods in {@link GamePanel} delegate to
 *     this interface:
 * </p>
 * <ul>
 *     <li>{@code handleMinimise()} → {@link #minimise()}</li>
 *     <li>{@code handleMaximise()} → {@link #toggleMaximise()}</li>
 *     <li>{@code handleClose()}    → {@link #close()}</li>
 * </ul>
 *
 * <h2>Note on drag-to-move</h2>
 * <p>
 *     Window <em>dragging</em> (moving the window by clicking and dragging the
 *     title bar) is <em>not</em> part of this interface because it requires
 *     reading the stage's current X/Y position on every mouse-move event. That
 *     tight, per-pixel coupling to the stage made it impractical to abstract.
 *     Drag handling is implemented separately in {@link GamePanel} using a direct
 *     {@link javafx.stage.Stage} reference set via
 *     {@link GamePanel#setStage(javafx.stage.Stage)}.
 * </p>
 */
interface WindowControls {

    /**
     * Minimises (iconifies) the application window to the taskbar.
     *
     * <p>
     *     In production this calls
     *     {@link javafx.stage.Stage#setIconified(boolean) Stage.setIconified(true)}.
     * </p>
     */
    void minimise();

    /**
     * Toggles the window between maximised and restored (normal) states.
     *
     * <p>
     *     In production this reads
     *     {@link javafx.stage.Stage#isMaximized() Stage.isMaximized()} and
     *     calls {@link javafx.stage.Stage#setMaximized(boolean) Stage.setMaximized(!current)}.
     * </p>
     */
    void toggleMaximise();

    /**
     * Closes the application window and terminates the JVM.
     *
     * <p>
     *     In production this calls {@link javafx.application.Platform#exit()} to
     *     shut down the JavaFX runtime, then
     *     {@link System#exit(int) System.exit(0)} to terminate the JVM process
     *     with exit code {@code 0} (normal/error-free termination). The
     *     {@link System#exit(int)} call is necessary because the game-engine
     *     thread may be blocked in
     *     {@link GamePanel#awaitPlayerInput()} and would otherwise keep the JVM
     *     alive after the window has closed.
     * </p>
     */
    void close();
}
