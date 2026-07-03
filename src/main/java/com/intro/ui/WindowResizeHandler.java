package com.intro.ui;

import com.intro.config.GameConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

/**
 * Enables resizing the {@link Stage} by dragging the edges or corners, since
 * {@link javafx.stage.StageStyle#UNDECORATED} removes the native resize options along the title bar.
 * <h2>Need for this class:</h2>
 * <p>
 *     JavaFX does not provide a built-in edge-resize behaviour for {@code UNDECORATED} stages which are
 *     otherwise normally supplied by the OS window manager. This class re-implements the standard behaviour:
 *     i.e. hovering near an edge should show a resize cursor, dragging from that edge should resize
 *     the stage.
 * </p>
 * <h2>Separating Logic from JavaFX</h2>
 * <p>
 *     Logical methods are {@code static} for unit-testing purposes (they can be tested without running a
 *     JavaFX platform):
 * </p>
 * <ul>
 *     <li>
 *         {@link #resolveEdge(double, double, double, double, double)}: given a mouse position and the current
 *         size of the window, this method decides which edge or corner (if any) the cursor is close enough to
 *         for a resize gesture to begin.
 *     </li>
 *     <li>
 *         {@link #computeBounds(ResizeEdge, double, double, double, double, double, double, double, double)}:
 *         given the target edge, the bounds of the stage and how far the mouse has moved, this method calculates
 *         the new bounds (which are restricted by minimum height/width of the window).
 *     </li>
 *     <li>
 *         {@link #cursorFor(ResizeEdge)} - this method displays the correct cursor type for edge dragging.
 *     </li>
 * </ul>
 * <p>
 *     {@link Cursor} does not require {@link javafx.application.Platform} to be initialised so
 *     {@link #cursorFor(ResizeEdge)} can be unit tested directly.
 * </p>
 * <h2>Nested {@link  DragState} class</h2>
 * <p>
 *     Transient drag states (active edge, starting mouse position, starting stage bounds) live in a
 *     {@code private static} nested class. {@link DragState} is created once per
 *     {@link #attach(Node, Stage, double, double)} call and shared across the registered mouse handlers.
 * </p>
 * <p>
 *     {@link DragState} is intentionally nested, rather than separate because:
 * </p>
 * <ul>
 *     <li>It is only used by  {@link #attach(Node, Stage, double, double)} and no other class needs it</li>
 *     <li>Its complex logic and calculations are performed by unit-tested methods like
 *     {@link #computeBounds(ResizeEdge, double, double, double, double, double, double, double, double)}
 *     so its internal logic is already indirectly verified.</li>
 * </ul>
 *
 * @see DragState
 * @see #attach(Node, Stage, double, double)
 * @see GameWindow
 * @see GamePanel
 * @see GameConfig
 */
public final class WindowResizeHandler {

    private static final Logger logger = LogManager.getLogger(WindowResizeHandler.class);

    /**
     * Enums for edges and corners of the window, to identify which edges a resize gesture applies to.
     * {@code NONE} means a cursor is not close enough to any edge.
     * <p>
     *     Cardinal values: {@link #N}, {@link #S}, {@link #W}, {@link #E} affect a single edge.
     *     Diagonal values: {@link #NW}, {@link #NE}, {@link #SW}, {@link #SE} affect two edges
     *     simultaneously.
     * </p>
     * @see #resolveEdge(double, double, double, double, double)
     * @see #computeBounds(ResizeEdge, double, double, double, double, double, double, double, double)
     * @see #cursorFor(ResizeEdge)
     */
    public enum ResizeEdge {
         NONE, N, S, E, W, NE, NW, SE, SW;

        /**
         * Returns {@code true} if the edge includes the {@link #W} (left) side of the window.
         * @return {@code true} for {@link #W}, {@link #NW}, {@link #SW}
         */
        boolean affectsWest() {
            return switch (this) {
                case W, NW, SW -> true;
                default -> false;
            };
        }

        /**
         * Returns {@code true} if the edge includes the {@link #E} (right) side of the window.
         * @return {@code true} for {@link #E}, {@link #NE}, {@link #SE}
         */
        boolean affectsEast() {
            return switch (this) {
                case E, NE, SE -> true;
                default -> false;
            };
        }

        /**
         * Returns {@code true} if the edge includes the {@link #N} (top) side of the window.
         * @return {@code true} for {@link #N}, {@link #NW}, {@link #NE}
         */
        boolean affectsNorth() {
            return switch (this) {
                case N, NW, NE -> true;
                default -> false;
            };
        }

        /**
         * Returns {@code true} if the edge includes the {@link #S} (left) side of the window.
         * @return {@code true} for {@link #S}, {@link #SW}, {@link #SE}
         */
        boolean affectsSouth() {
            return switch (this) {
                case S, SW, SE -> true;
                default -> false;
            };
        }

        /**
         * Returns {@code true} if at least one edge is included, else returns {@code false} if
         * {@link #NONE}.
         * @return {@code false} only for {@link #NONE}
         */
        boolean isActive() {
            return this != NONE;
        }
    }

    /**
     * Stage position and size after a resize gesture, result of
     * {@link #computeBounds(ResizeEdge, double, double, double, double, double, double, double, double)}.
     * @param x new stage X (screen position of left edge)
     * @param y new stage Y (screen position of top edge)
     * @param width new stage width; always {@code >=} the minimum window width
     * @param height new stage height; always {@code >=} the minimum window height
     * @see #computeBounds(ResizeEdge, double, double, double, double, double, double, double, double)
     * @see #applyBounds(Stage, Bounds)
     */
    public record Bounds(double x, double y, double width, double height) {}

    /**
     * How close (in pixels) the cursor must be to an edge for {@link #resolveEdge(double, double, double, double, double)}
     *  to report that edge, and the default margin used by {@link #attach(Node, Stage, double, double)}.
     *  <p>
     *      Loaded from {@code config.properties} via {@link GameConfig#getDouble(String, double)}.
     *  </p>
     *  Default value: {@code 6} px
     */
    static final double DEFAULT_MARGIN = GameConfig.getDouble("ui.window.resize.margin", 6);

    /**
     * Private constructor. WindowsResizeHandler is a static utility class, no instances of this class
     * will ever be required.
     */
    private WindowResizeHandler() {}

    /**
     * Determines which edge or corner (if any) a point at {@code (x, y)} is close enough to, given the
     * bounds of a {@code width} x {@code height} rectangle (the window). N.B. where the top left corner of
     * the window is the origin.
     * <p>
     *     Corners take priority over single edge cases, for example: a point near both the top and left
     *     within {@code margin} resolves to {@link ResizeEdge#NW}, rather than {@link ResizeEdge#N} or
     *     {@link ResizeEdge#W}. A point that is not within {@code margin} of any edge resolves to
     *     {@link ResizeEdge#NONE}.
     * </p>
     * @param x horizontal position relative to the window's left edge
     * @param y vertical position relative to the window's top edge
     * @param width the window's width; must be {@code > 0}
     * @param height the window's height; must be {@code > 0}
     * @param margin how close a point must be to an edge to count as "on" that edge; must be {@code >= 0}
     * @return the resolves {@link ResizeEdge}, or {@link ResizeEdge#NONE} if the point is not near any edge
     * @see #edgeAt(Node, MouseEvent)
     * @see #attach(Node, Stage, double, double)
     */
    static ResizeEdge resolveEdge(double x, double y, double width, double height, double margin) {
        boolean nearWest = x <= margin;
        boolean nearEast = x >= width - margin;
        boolean nearNorth = y <= margin;
        boolean nearSouth = y >= height - margin;

        if (nearNorth && nearWest) return ResizeEdge.NW;
        if (nearNorth && nearEast) return ResizeEdge.NE;
        if (nearSouth && nearWest) return ResizeEdge.SW;
        if (nearSouth && nearEast) return ResizeEdge.SE;
        if (nearNorth) return ResizeEdge.N;
        if (nearSouth) return ResizeEdge.S;
        if (nearWest) return ResizeEdge.W;
        if (nearEast) return ResizeEdge.E;
        return ResizeEdge.NONE;
    }

    /**
     * Computes the new stage bounds for a resize drag gesture.
     * <p>
     *     {code deltaX} and {@code deltaY} are the total mouse movement in pixels since the drag gesture began.
     * </p>
     * <p>
     *     For edges that include {@link ResizeEdge#N} or {@link ResizeEdge#W} moving the mouse also shifts the
     *     stage's origin so the opposite edge stays fixed. Edges that include {@link ResizeEdge#S} or {@link ResizeEdge#E}
     *     only change size, since the top-left corner does not move.
     * </p>
     * <p>
     *     Width and height are bounded by {@code minWidth} and {@code minHeight}. When a dimension reaches these
     *     bounds, all sides are restricted from shrinking further. For example: if a dimension reaches the lower
     *     bound of {@link ResizeEdge#N} and {@link ResizeEdge#W}, both the {@link ResizeEdge#S} and {@link ResizeEdge#E}
     *     edges are also restricted from shrinking.
     * </p>
     * @param edge the user selected edge/corner being dragged
     *             {@link ResizeEdge#NONE} returns the starting bounds, unchanged.
     * @param startX the stage's X position when the drag began.
     * @param startY the stage's Y position when the drag began.
     * @param startWidth the stage's width when the drag began, must be {@code > 0}
     * @param startHeight the stage's height when the drag began, must be {@code > 0}
     * @param deltaX total horizontal (x-axis) mouse movement since drag began.
     * @param deltaY total vertical (y-axis) mouse movement since drag began.
     * @param minWidth the minimum allowed width of window; must be {@code > 0}
     * @param minHeight the minimum allowed height of window; must be {@code > 0}
     * @return the new {@link Bounds} for the stage.
     * @see Bounds
     * @see DragState#boundsFor(MouseEvent, double, double)
     */
    //@SuppressWarnings(java:S107)
    static Bounds computeBounds(ResizeEdge edge, double startX, double startY,
                                double startWidth, double startHeight,
                                double deltaX, double deltaY,
                                double minWidth, double minHeight) {
        if (!edge.isActive()) {
            return new Bounds(startX, startY, startWidth, startHeight);
        }

        double x = startX;
        double y = startY;
        double width = startWidth;
        double height = startHeight;

        if (edge.affectsEast()) {
            width = startWidth + deltaX;
        }
        if (edge.affectsSouth()) {
            height = startHeight + deltaY;
        }
        if (edge.affectsWest()) {
            width = startWidth - deltaX;
            x = startX + deltaX;
        }
        if (edge.affectsNorth()) {
            height = startHeight - deltaY;
            y = startY + deltaY;
        }
        if (width < minWidth) {
            if (edge.affectsWest()) {
                x = startX + (startWidth - minWidth);
            }
            width = minWidth;
        }
        if (height < minHeight) {
            if (edge.affectsNorth()) {
                y = startY + (startHeight - minHeight);
            }
            height = minHeight;
        }
        return new Bounds(x, y, width, height);
    }

    /**
     * Maps a {@link ResizeEdge} to the {@link Cursor} that should be shown whilst hovering over it.
     * @param edge the resolved edge; {@link ResizeEdge#NONE} maps to {@link Cursor#DEFAULT}.
     * @return the cursor to display; never {@code null}.
     * @see #attach(Node, Stage, double, double)
     */
    static Cursor cursorFor(ResizeEdge edge) {
        return switch (edge) {
            case N, S -> Cursor.V_RESIZE;
            case E, W -> Cursor.H_RESIZE;
            case NE, SW -> Cursor.NE_RESIZE;
            case NW, SE -> Cursor.NW_RESIZE;
            case NONE -> Cursor.DEFAULT;
        };
    }

    /**
     * Attaches edge-drag behaviour to the {@code target}, resizing the {@code stage} when the player drags
     * from within {@link #DEFAULT_MARGIN} pixels of an edge.
     * <p>
     *     Registers four mouse event handlers on {@code target}:
     * </p>
     * <ul>
     *     <li>{@link MouseEvent#MOUSE_MOVED} updates the cursor using {@link #cursorFor(ResizeEdge)} based
     *     on {@link #edgeAt(Node, MouseEvent)}, to display the appropriate cursor.</li>
     *     <li> {@link MouseEvent#MOUSE_PRESSED} records the edge under the cursor and the stage's starting
     *     bounds and mouse position in a {@link DragState}.</li>
     *     <li>{@link MouseEvent#MOUSE_DRAGGED} calculates and applies the new bounds using
     *     {@link DragState#boundsFor(MouseEvent, double, double) boundsFor} and {@link #applyBounds(Stage, Bounds)}</li>
     *     <li>{@link MouseEvent#MOUSE_RELEASED} clears the active drag using {@link DragState#clear()}</li>
     * </ul>
     * <p>
     *     N.B. This is not unit-tested because it requires a live JavaFX {@link javafx.application.Platform Platform},
     *     a {@link Node} and a real {@link Stage} - none of which can be tested without an initialised platform.
     *     The arithmetic logic this method uses comes from {@link #resolveEdge}, {@link #computeBounds} &
     *     {@link #cursorFor} which covered by WindowResizeHandlerTest.
     * </p>
     * @param target the node whose edges trigger the resize gesture ({@link GameWindow}'s FXML root)
     * @param stage the undecorated stage that needs to be resized/repositioned
     * @param minWidth the minimum allowed stage width, usually loaded from {@code ui.window.min.width} by
     *                 {@link GameConfig}
     * @param minHeight the minimum allowed stage height, usually loaded from {@code ui.window.min.height} by
     *                 {@link GameConfig}
     * @see GameWindow#start(Stage) 
     * @see GameConfig
     */
    public static void attach(Node target, Stage stage, double minWidth, double minHeight) {
        logger.debug("Attaching JavaFX window resize handlers [minWidth={}, minHeight={}]", minWidth, minHeight);

        DragState dragState = new DragState();

        /* Sets the correct cursor type */
        target.addEventHandler(MouseEvent.MOUSE_MOVED, event ->
                target.setCursor(cursorFor(edgeAt(target, event))));

        /* Determines the active edge and initialises the resize drag gesture */
        target.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            ResizeEdge edge = edgeAt(target, event);
            dragState.begin(edge, event, stage);
        });

        /* Calculates and sets the new window bounds */
        target.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            Bounds bounds = dragState.boundsFor(event, minWidth, minHeight);
            if (bounds != null) {
                applyBounds(stage, bounds);
            }
        });

        target.addEventHandler(MouseEvent.MOUSE_RELEASED, event ->
                dragState.clear());
    }

    /**
     * Resolves which {@link ResizeEdge edge} the mouse in {@code event} is occurring on, relative to
     * {@code target}'s local bounds and {@link #DEFAULT_MARGIN}.
     * @param target the JavaFX node acting as the interactive surface for window resizing (i.e. window's root)
     * @param event the mouse event supplying the local {@code (x, y)} coordinates of the mouse ({@link MouseEvent#getX()},
     *              {@link MouseEvent#getY()})
     * @return the resolved edge, or {@link ResizeEdge#NONE} if the cursor is not near any edge.
     * @see #resolveEdge(double, double, double, double, double) 
     */
    private static ResizeEdge edgeAt(Node target, MouseEvent event) {
        javafx.geometry.Bounds bounds = target.getBoundsInLocal();
        return resolveEdge(event.getX(), event.getY(), bounds.getWidth(), bounds.getHeight(), DEFAULT_MARGIN);
    }

    /**
     * Applies a calculated {@link Bounds} record to a {@link Stage}, updating the position and size of the
     * window.
     * @param stage the stage to resize/reposition
     * @param bounds the new position and dimensions to apply.
     * @see Bounds
     * @see Stage#setX(double) 
     * @see Stage#setY(double) 
     * @see Stage#setWidth(double) 
     * @see Stage#setHeight(double)
     */
    private static void applyBounds(Stage stage, Bounds bounds) {
        stage.setX(bounds.x());
        stage.setY(bounds.y());
        stage.setWidth(bounds.width());
        stage.setHeight(bounds.height());
    }

    /**
     * This nested class tracks the state and lifecycle of a single window resizing gesture.
     * <p>
     *     Nested inside {@link WindowResizeHandler} rather than declared as a top-level class, because it exists
     *     to support {@link #attach(Node, Stage, double, double)} and is not used elsewhere. For every attach
     *     call, an instance of {@code DragState} is created.
     * </p>
     * <h2>Lifecycle:</h2>
     * <ol>
     *     <li>{@link #begin(ResizeEdge, MouseEvent, Stage)}: records the initial window geometry and the target edge
     *     of the current resizing gesture</li>
     *     <li>{@link #boundsFor(MouseEvent, double, double)}: calculates the new {@link Bounds} for each drag event</li>
     *     <li>{@link #clear()}: resets the edge when the mouse is {@link MouseEvent#MOUSE_RELEASED released}</li>
     * </ol>
     * @see #attach
     * @see #computeBounds
     * @see Bounds
     */
    private static final class DragState {
        private ResizeEdge activeEdge = ResizeEdge.NONE;
        private double startScreenX;
        private double startScreenY;
        private double startStageX;
        private double startStageY;
        private double startWidth;
        private double startHeight;

        /**
         * Records the edge under the cursor for the current resize gesture. If it is active, it records the current
         * stage bounds and mouse position (at the start of the drag)
         * @param edge the edge resolved by {@link #edgeAt(Node, MouseEvent)}
         * @param event the triggering mouse press event which provides the screen coordinates
         * @param stage the stage whose bounds are recorded
         * @see ResizeEdge#isActive()
         */
        void begin(ResizeEdge edge, MouseEvent event, Stage stage) {
            this.activeEdge = edge;
            if (!edge.isActive()) {
                return;
            }
            this.startScreenX = event.getScreenX();
            this.startScreenY = event.getScreenY();
            this.startStageX = stage.getX();
            this.startStageY = stage.getY();
            this.startWidth = stage.getWidth();
            this.startHeight = stage.getHeight();
            logger.debug("Resize drag started on edge: {}", edge);
        }

        /**
         * Calculates the new stage {@link Bounds} for the current drag position, or returns {@code null} if no
         * active edge was recorded at the time of the {@link MouseEvent#MOUSE_PRESSED} event.
         * @param event the drag event supplying the current screen position
         * @param minWidth minimum allowed stage width passed through to {@link #computeBounds}
         * @param minHeight minimum allowed stage height passed through to {@link #computeBounds}
         * @return the new bounds, or {@code null} when {@link #activeEdge} is {@link ResizeEdge#NONE NONE}
         */
        Bounds boundsFor(MouseEvent event, double minWidth, double minHeight) {
            if (!this.activeEdge.isActive()) {
                return null;
            }
            double deltaX = event.getScreenX() - this.startScreenX;
            double deltaY = event.getScreenY() - this.startScreenY;
            return computeBounds(this.activeEdge, this.startStageX, this.startStageY, this.startWidth, this.startHeight, deltaX, deltaY, minWidth, minHeight);
        }

        /**
         * Ends the current drag session by resetting {@link #activeEdge} to {@link ResizeEdge#NONE NONE}
         */
        void clear() {
            this.activeEdge = ResizeEdge.NONE;
        }
    }

}
