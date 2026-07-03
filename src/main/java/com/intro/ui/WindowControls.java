package com.intro.ui;

/**
 * Abstract interface to provide abstraction for the operating system's window control features -
 * close, minimise, maximise/restore.
 * <p>
 *     The custom title bar in {@code GamePanel.fxml} contains three buttons that interact with
 *     {@link javafx.stage.Stage Stage}. This interface improves the testability of {@link GamePanel}.
 * </p>
 * <p>
 *     {@link GameWindow#start} creates an implementation and passes it to
 *     {@link GamePanel#setWindowControls(WindowControls) setWindowControls}. Three {@code FXML} handle methods use this
 *     interface:
 *     <ul>
 *         <li>{@link GamePanel#handleMinimise() handleMinimise()} -> {@link #minimise()}</li>
 *         <li>{@link GamePanel#handleMaximise() handleMaximise()} -> {@link #toggleMaximise()}</li>
 *         <li>{@link GamePanel#handleClose() handleClose()} -> {@link #close()}</li>
 *     </ul>
 * </p>
 */
public interface WindowControls {

    /**
     * Minimises the application window to the taskbar.
     * @see javafx.stage.Stage#setIconified(boolean)
     */
    void minimise();

    /**
     * Toggles the window's maximised and restored sizes.
     * @see javafx.stage.Stage#setMaximized(boolean)
     */
    void toggleMaximise();

    /**
     * Closes the application window and terminates the JVM, with exit code {@code 0}.
     * @see javafx.application.Platform#exit()
     * @see System#exit(int)
     */
    void close();

}
