package com.intro.ui;

import com.intro.config.GameConfig;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Enables resizing the undecorated {@link Stage} by dragging its edges or
 * corners, since {@link javafx.stage.StageStyle#UNDECORATED} (used by {@link GameWindow})
 * removes the OS-native resize grips along with the title bar.
 *
 * <h2>Why this class exists</h2>
 * <p>
 *     JavaFX provides no built-in edge-resize behaviour for undecorated stages —
 *     that is normally supplied by the operating system's window manager, which
 *     {@code StageStyle.UNDECORATED} disables. This class re-implements the
 *     standard behaviour: hovering near an edge shows a directional resize
 *     cursor, and dragging from that edge resizes (and, for the top/left edges,
 *     repositions) the stage.
 * </p>
 *
 * <h2>Split between pure logic and JavaFX glue</h2>
 * <p>
 *     Two pieces of pure logic are exposed as {@code static} methods so they
 *     can be unit-tested without a running JavaFX platform:
 * </p>
 * <ul>
 *     <li>
 *         {@link #resolveEdge(double, double, double, double, double)} — given
 *         a mouse position and the target's current size, decides which edge
 *         or corner (if any) the cursor is close enough to for a resize
 *         gesture to begin.
 *     </li>
 *     <li>
 *         {@link #computeBounds(ResizeEdge, double, double, double, double, double, double, double, double)} —
 *         given the edge being dragged, the stage's bounds at the start of the
 *         drag, and how far the mouse has moved, computes the new bounds,
 *         clamped to a minimum width/height.
 *     </li>
 *     <li>
 *         {@link #cursorFor(ResizeEdge)} — maps a resolved edge to the
 *         appropriate {@link Cursor} for hover feedback.
 *     </li>
 * </ul>
 * <p>
 *     {@link Cursor} does not require {@link javafx.application.Platform} to be
 *     initialised (it is a simple enum-like value holder), so
 *     {@link #cursorFor(ResizeEdge)} is also directly unit-testable.
 * </p>
 * <p>
 *     Everything else — attaching mouse listeners to a {@link Node} and
 *     mutating a real {@link Stage} — is thin instance-level glue in
 *     {@link #attach(Node, Stage, double, double)}. This mirrors the existing
 *     convention in {@link GamePanel} for drag-to-move: the pixel-level event
 *     wiring is not unit-tested (it requires a running JavaFX platform and a
 *     real {@link Stage}), while the underlying arithmetic is fully tested in
 *     {@code WindowResizeHandlerTest}.
 * </p>
 * <h2>Nested {@link DragState}</h2>
 * <p>
 *     Transient drag-session state (active edge, starting mouse position, starting
 *     stage bounds) lives in a {@code private static} nested class rather than a
 *     separate top-level type. {@link DragState} is created once per
 *     {@link #attach(Node, Stage, double, double)} call and shared across the
 *     registered mouse handlers via closure capture.
 * </p>
 * <p>
 *     It is intentionally <em>not</em> extracted to its own file because:
 * </p>
 * <ul>
 *     <li>it is an implementation detail of {@link #attach(Node, Stage, double, double)}
 *         — no other class needs it;</li>
 *     <li>keeping it {@code private} prevents other types from depending on
 *         mutable session state that is not part of this class's public API;</li>
 *     <li>its behaviour delegates to the unit-tested pure methods
 *         ({@link #computeBounds(ResizeEdge, double, double, double, double, double, double, double, double)}),
 *         so a separate type would not improve test coverage;</li>
 *     <li>this matches the convention in {@link GamePanel}, where title-bar
 *         drag offsets are private fields on the panel rather than a standalone
 *         helper class.</li>
 * </ul>
 * <p>
 *     Extract {@link DragState} to a package-private top-level class only if it
 *     becomes reusable across multiple handlers or grows substantial independent
 *     behaviour (e.g. snap-to-grid, multi-monitor clamping).
 * </p>
 *
 * @see DragState
 * @see #attach(Node, Stage, double, double)
 * @see GameWindow
 * @see GamePanel
 * @see GameConfig
 */
@SuppressWarnings("java:S107")
public final class WindowResizeHandler {

    private static final Logger logger = LogManager.getLogger(WindowResizeHandler.class);

    /**
     * Identifies which edge(s) of the resize target a drag gesture applies to.
     *
     * <p>
     *     Cardinal values ({@link #N}, {@link #S}, {@link #E}, {@link #W})
     *     affect a single edge. Diagonal values ({@link #NE}, {@link #NW},
     *     {@link #SE}, {@link #SW}) affect two edges simultaneously.
     *     {@link #NONE} means the cursor is not close enough to any edge to
     *     start a resize gesture.
     * </p>
     *
     * @see #resolveEdge(double, double, double, double, double)
     * @see #computeBounds(ResizeEdge, double, double, double, double, double, double, double, double)
     * @see #cursorFor(ResizeEdge)
     */
    public enum ResizeEdge {
        /** No edge is within the resize margin; no resize gesture applies. */
        NONE,
        /** Top edge only. */
        N,
        /** Bottom edge only. */
        S,
        /** Right edge only. */
        E,
        /** Left edge only. */
        W,
        /** Top-right corner (top and right edges). */
        NE,
        /** Top-left corner (top and left edges). */
        NW,
        /** Bottom-left corner (bottom and left edges). */
        SW,
        /** Bottom-right corner (bottom and right edges). */
        SE;

        /**
         * Returns {@code true} if this edge includes the west (left) side of
         * the target, meaning a drag should adjust the stage's X position and
         * width together.
         *
         * @return {@code true} for {@link #W}, {@link #NW}, and {@link #SW}.
         */
        boolean affectsWest() {
            return switch (this) {
                case W, NW, SW -> true;
                default -> false;
            };
        }

        /**
         * Returns {@code true} if this edge includes the east (right) side of
         * the target, meaning a drag should adjust the stage width only.
         *
         * @return {@code true} for {@link #E}, {@link #NE}, and {@link #SE}.
         */
        boolean affectsEast() {
            return switch (this) {
                case E, NE, SE -> true;
                default -> false;
            };
        }

        /**
         * Returns {@code true} if this edge includes the north (top) side of
         * the target, meaning a drag should adjust the stage's Y position and
         * height together.
         *
         * @return {@code true} for {@link #N}, {@link #NW}, and {@link #NE}.
         */
        boolean affectsNorth() {
            return switch (this) {
                case N, NW, NE -> true;
                default -> false;
            };
        }

        /**
         * Returns {@code true} if this edge includes the south (bottom) side of
         * the target, meaning a drag should adjust the stage height only.
         *
         * @return {@code true} for {@link #S}, {@link #SW}, and {@link #SE}.
         */
        boolean affectsSouth() {
            return switch (this) {
                case S, SW, SE -> true;
                default -> false;
            };
        }

        /**
         * Returns {@code true} if this value represents an edge or corner that
         * can participate in a resize drag (i.e. anything other than
         * {@link #NONE}).
         *
         * @return {@code false} only for {@link #NONE}.
         */
        boolean isActive() {
            return this != NONE;
        }
    }

    /**
     * Immutable result of
     * {@link #computeBounds(ResizeEdge, double, double, double, double, double, double, double, double)}:
     * the new top-left position and size a {@link Stage} should adopt after a
     * resize drag.
     *
     * @param x      new stage X (screen position of the left edge).
     * @param y      new stage Y (screen position of the top edge).
     * @param width  new stage width; always {@code >=} the supplied minimum.
     * @param height new stage height; always {@code >=} the supplied minimum.
     * @see #computeBounds(ResizeEdge, double, double, double, double, double, double, double, double)
     * @see #applyBounds(Stage, Bounds)
     */
    public record Bounds(double x, double y, double width, double height) {
    }

    /**
     * How close (in pixels) the cursor must be to an edge for
     * {@link #resolveEdge(double, double, double, double, double)} to report
     * that edge, and the default margin used by
     * {@link #attach(Node, Stage, double, double)}.
     *
     * <p>
     *     Loaded from {@code config.properties} via
     *     {@link GameConfig#getDouble(String, double)} — this is a tunable UI
     *     parameter, the same category of value as {@code ui.window.min.width}
     *     and {@code ui.window.min.height} in that file, so it lives alongside
     *     them rather than as a Java literal. The {@code 6} argument below is
     *     only a defensive fallback used if the {@code ui.window.resize.margin}
     *     key is ever missing from the properties file.
     * </p>
     *
     * @see GameConfig
     */
    static final double DEFAULT_MARGIN = GameConfig.getDouble("ui.window.resize.margin", 6);

    private WindowResizeHandler() {
        // static utility class; instances are unnecessary.
    }

    // -------------------------------------------------------------------------
    // Pure logic — fully unit-testable, no JavaFX platform required
    // -------------------------------------------------------------------------

    /**
     * Determines which edge or corner (if any) a point at {@code (x, y)} is
     * close enough to, given the bounds of a {@code width} x {@code height}
     * rectangle whose top-left corner is the origin.
     *
     * <p>
     *     Corners take priority over single edges: a point near both the top
     *     and the left within {@code margin} resolves to {@link ResizeEdge#NW},
     *     not {@link ResizeEdge#N} or {@link ResizeEdge#W}. A point that is
     *     not within {@code margin} of any edge resolves to
     *     {@link ResizeEdge#NONE}.
     * </p>
     *
     * @param x      horizontal position relative to the rectangle's left edge.
     * @param y      vertical position relative to the rectangle's top edge.
     * @param width  the rectangle's width; must be {@code > 0}.
     * @param height the rectangle's height; must be {@code > 0}.
     * @param margin how close (in the same units as {@code x}/{@code y}) the
     *               point must be to an edge to count as "on" that edge; must
     *               be {@code >= 0}.
     * @return the resolved {@link ResizeEdge}, or {@link ResizeEdge#NONE} if
     *         the point is not near any edge.
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
     * Computes the new stage bounds for a resize-drag gesture.
     *
     * <p>
     *     {@code deltaX}/{@code deltaY} are the total mouse movement (in screen
     *     pixels) since the drag began, i.e. {@code currentScreenX -
     *     startScreenX} and {@code currentScreenY - startScreenY}.
     * </p>
     * <p>
     *     For edges that include {@link ResizeEdge#N} or {@link ResizeEdge#W},
     *     moving the mouse also shifts the stage's origin so the opposite edge
     *     stays fixed — dragging the top edge down should shrink the window
     *     from the top, not grow it from the bottom. Edges that include
     *     {@link ResizeEdge#S} or {@link ResizeEdge#E} only change size, since
     *     the top-left corner does not move.
     * </p>
     * <p>
     *     Width and height are clamped to {@code minWidth}/{@code minHeight}.
     *     When a dimension is clamped on the {@link ResizeEdge#N}/
     *     {@link ResizeEdge#W} side, the corresponding position is also
     *     clamped so the opposite edge does not appear to "slide" past the
     *     minimum size — i.e. the window stops growing/shrinking exactly at
     *     the minimum, rather than the origin continuing to follow the cursor
     *     while the size flatlines.
     * </p>
     *
     * @param edge        which edge/corner is being dragged;
     *                    {@link ResizeEdge#NONE} returns the starting bounds
     *                    unchanged.
     * @param startX      the stage's X position when the drag began.
     * @param startY      the stage's Y position when the drag began.
     * @param startWidth  the stage's width when the drag began; must be
     *                    {@code > 0}.
     * @param startHeight the stage's height when the drag began; must be
     *                    {@code > 0}.
     * @param deltaX      total horizontal mouse movement since the drag began.
     * @param deltaY      total vertical mouse movement since the drag began.
     * @param minWidth    the minimum allowed width; must be {@code > 0}.
     * @param minHeight   the minimum allowed height; must be {@code > 0}.
     * @return the new {@link Bounds} for the stage.
     * @see Bounds
     * @see DragState#boundsFor(MouseEvent, double, double)
     */
    static Bounds computeBounds(ResizeEdge edge,
                                double startX, double startY,
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
     * Maps a {@link ResizeEdge} to the {@link Cursor} that should be shown
     * while hovering over it, following the standard OS convention (vertical
     * double-arrow for N/S, horizontal for E/W, diagonal for corners).
     *
     * @param edge the resolved edge; {@link ResizeEdge#NONE} maps to
     *             {@link Cursor#DEFAULT}.
     * @return the cursor to display; never {@code null}.
     * @see #resolveEdge(double, double, double, double, double)
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

    // -------------------------------------------------------------------------
    // JavaFX glue — not unit-tested; requires a running platform and a real
    // Stage/Node, exactly like the drag-to-move handlers in GamePanel.
    // -------------------------------------------------------------------------

    /**
     * Attaches edge-drag resize behaviour to {@code target}, resizing
     * {@code stage} when the player drags from within {@link #DEFAULT_MARGIN}
     * pixels of its edge.
     *
     * <p>
     *     Registers four mouse event handlers on {@code target}:
     * </p>
     * <ul>
     *     <li>{@link MouseEvent#MOUSE_MOVED} — updates the cursor via
     *         {@link #cursorFor(ResizeEdge)} based on {@link #edgeAt(Node, MouseEvent)}.</li>
     *     <li>{@link MouseEvent#MOUSE_PRESSED} — records the edge under the
     *         cursor and the stage's starting bounds and mouse position in a
     *         {@link DragState}.</li>
     *     <li>{@link MouseEvent#MOUSE_DRAGGED} — computes and applies the new
     *         bounds via {@link DragState#boundsFor(MouseEvent, double, double)}
     *         and {@link #applyBounds(Stage, Bounds)}.</li>
     *     <li>{@link MouseEvent#MOUSE_RELEASED} — clears the active drag via
     *         {@link DragState#clear()}.</li>
     * </ul>
     * <p>
     *     Not unit-tested: exercising this method requires a live JavaFX
     *     platform, a rendered {@link Node}, and a real {@link Stage}, none of
     *     which can be faked without an initialised platform. The arithmetic
     *     it delegates to ({@link #resolveEdge}, {@link #computeBounds},
     *     {@link #cursorFor}) is fully covered by
     *     {@code WindowResizeHandlerTest}.
     * </p>
     *
     * @param target    the node whose edges trigger the resize gesture; in
     *                  production this is {@link GameWindow}'s loaded FXML root.
     * @param stage     the undecorated stage to resize and reposition.
     * @param minWidth  the minimum allowed stage width, typically loaded from
     *                  {@code ui.window.min.width} in {@code config.properties}.
     * @param minHeight the minimum allowed stage height, typically loaded from
     *                  {@code ui.window.min.height} in {@code config.properties}.
     * @see GameWindow#start(Stage)
     * @see GameConfig
     */
    public static void attach(Node target, Stage stage, double minWidth, double minHeight) {
        logger.debug("Attaching edge-drag resize handling (minWidth={}, minHeight={})", minWidth, minHeight);

        var dragState = new DragState();

        target.addEventHandler(MouseEvent.MOUSE_MOVED, event ->
                target.setCursor(cursorFor(edgeAt(target, event))));

        target.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            ResizeEdge edge = edgeAt(target, event);
            dragState.begin(edge, event, stage);
        });

        target.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            Bounds bounds = dragState.boundsFor(event, minWidth, minHeight);
            if (bounds != null) {
                applyBounds(stage, bounds);
            }
        });

        target.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> dragState.clear());
    }

    /**
     * Resolves which {@link ResizeEdge} the mouse in {@code event} is over,
     * relative to {@code target}'s local bounds and {@link #DEFAULT_MARGIN}.
     *
     * @param target the node whose local coordinate space defines the edge
     *               detection rectangle.
     * @param event  the mouse event supplying {@link MouseEvent#getX()} and
     *               {@link MouseEvent#getY()} in local coordinates.
     * @return the resolved edge, or {@link ResizeEdge#NONE} if the cursor is
     *         not near any edge.
     * @see #resolveEdge(double, double, double, double, double)
     */
    private static ResizeEdge edgeAt(Node target, MouseEvent event) {
        var bounds = target.getBoundsInLocal();
        return resolveEdge(event.getX(), event.getY(),
                bounds.getWidth(), bounds.getHeight(), DEFAULT_MARGIN);
    }

    /**
     * Applies a computed {@link Bounds} record to a {@link Stage}, updating its
     * position and size in a single step.
     *
     * @param stage  the stage to mutate.
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
     * Mutable drag session between {@link MouseEvent#MOUSE_PRESSED} on an
     * active {@link ResizeEdge} and the corresponding
     * {@link MouseEvent#MOUSE_RELEASED}.
     *
     * <p>
     *     Nested inside {@link WindowResizeHandler} (rather than declared as a
     *     separate top-level class) because it exists solely to support
     *     {@link #attach(Node, Stage, double, double)}: one instance is created
     *     per attach call and is not part of the public API. Nesting keeps the
     *     session state encapsulated and co-located with the event handlers that
     *     consume it.
     * </p>
     *
     * <p>
     *     Lifecycle:
     * </p>
     * <ol>
     *     <li>{@link #begin(ResizeEdge, MouseEvent, Stage)} — snapshot edge and
     *         starting bounds on press;</li>
     *     <li>{@link #boundsFor(MouseEvent, double, double)} — compute new
     *         {@link Bounds} on each drag event;</li>
     *     <li>{@link #clear()} — reset on release.</li>
     * </ol>
     *
     * @see #attach(Node, Stage, double, double)
     * @see #computeBounds(ResizeEdge, double, double, double, double, double, double, double, double)
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
         * Records the edge under the cursor and, if it is active, snapshots
         * the stage bounds and mouse screen position at the start of the drag.
         *
         * @param edge  the edge resolved by {@link #edgeAt(Node, MouseEvent)}.
         * @param event the press event supplying screen coordinates.
         * @param stage the stage whose bounds are snapshotted.
         * @see ResizeEdge#isActive()
         */
        void begin(ResizeEdge edge, MouseEvent event, Stage stage) {
            activeEdge = edge;
            if (!edge.isActive()) {
                return;
            }
            startScreenX = event.getScreenX();
            startScreenY = event.getScreenY();
            startStageX = stage.getX();
            startStageY = stage.getY();
            startWidth = stage.getWidth();
            startHeight = stage.getHeight();
            logger.debug("Resize drag started on edge {}", edge);
        }

        /**
         * Computes the new stage {@link Bounds} for the current drag position,
         * or returns {@code null} if no active edge was recorded at press time.
         *
         * @param event     the drag event supplying the current screen position.
         * @param minWidth  minimum allowed stage width passed through to
         *                  {@link #computeBounds(ResizeEdge, double, double, double, double, double, double, double, double)}.
         * @param minHeight minimum allowed stage height passed through to
         *                  {@link #computeBounds(ResizeEdge, double, double, double, double, double, double, double, double)}.
         * @return the new bounds, or {@code null} when {@link #activeEdge} is
         *         {@link ResizeEdge#NONE}.
         * @see #computeBounds(ResizeEdge, double, double, double, double, double, double, double, double)
         */
        Bounds boundsFor(MouseEvent event, double minWidth, double minHeight) {
            if (!activeEdge.isActive()) {
                return null;
            }
            double deltaX = event.getScreenX() - startScreenX;
            double deltaY = event.getScreenY() - startScreenY;
            return computeBounds(activeEdge,
                    startStageX, startStageY, startWidth, startHeight,
                    deltaX, deltaY, minWidth, minHeight);
        }

        /**
         * Ends the current drag session by resetting {@link #activeEdge} to
         * {@link ResizeEdge#NONE}.
         */
        void clear() {
            activeEdge = ResizeEdge.NONE;
        }
    }
}
