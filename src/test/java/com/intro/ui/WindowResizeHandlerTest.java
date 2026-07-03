package com.intro.ui;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link WindowResizeHandler}.
 *
 * <h2>What is tested here</h2>
 * <p>
 *     {@link WindowResizeHandler#resolveEdge(double, double, double, double, double)},
 *     {@link WindowResizeHandler#computeBounds(WindowResizeHandler.ResizeEdge, double, double, double, double, double, double, double, double)},
 *     and {@link WindowResizeHandler#cursorFor(WindowResizeHandler.ResizeEdge)} are pure
 *     functions with no field access and no JavaFX platform dependency.
 *     {@link Cursor} is a simple value holder that does not require
 *     {@link javafx.application.Platform} to be initialised. All three are
 *     fully covered here, including margin-boundary and minimum-size clamping
 *     edge cases.
 * </p>
 * <p>
 *     {@link WindowResizeHandler#attach(javafx.scene.Node, javafx.stage.Stage, double, double)}
 *     is tested in {@link AttachTests} using a headless JavaFX toolkit
 *     ({@link javafx.application.Platform#startup(Runnable)}) with real
 *     {@link javafx.scene.shape.Rectangle}, {@link Stage}, and {@link MouseEvent}
 *     instances fired via {@link javafx.event.Event#fireEvent(javafx.event.EventTarget, javafx.event.Event)}.
 *     {@link DragDeltaTests} uses Mockito with a plain {@link ScreenPoint} interface
 *     to verify the screen-coordinate delta contract that feeds
 *     {@link WindowResizeHandler#computeBounds} during a drag (JavaFX
 *     {@link MouseEvent} cannot be mocked on this JVM).
 * </p>
 */
class WindowResizeHandlerTest {

    private static final Logger logger = LogManager.getLogger(WindowResizeHandlerTest.class);

    private static final double RECT_WIDTH = 200;
    private static final double RECT_HEIGHT = 100;
    private static final double MARGIN = 6;

    private static final double START_X = 10;
    private static final double START_Y = 20;
    private static final double START_WIDTH = 300;
    private static final double START_HEIGHT = 200;
    private static final double MIN_WIDTH = 100;
    private static final double MIN_HEIGHT = 100;

    @Test
    @DisplayName("resolveEdge() returns NONE for a point away from every edge")
    void resolveEdge_pointAwayFromEdges_returnsNone() {
        logger.debug("Testing resolveEdge returns NONE for a central point");
        WindowResizeHandler.ResizeEdge edge =
                WindowResizeHandler.resolveEdge(100, 50, RECT_WIDTH, RECT_HEIGHT, MARGIN);
        assertEquals(WindowResizeHandler.ResizeEdge.NONE, edge);
    }

    @ParameterizedTest(name = "resolveEdge({0}, {1}) resolves to {2}")
    @CsvSource({
            "100, 0,   N",
            "100, 99,  S",
            "0,   50,  W",
            "199, 50,  E",
            "0,   0,   NW",
            "199, 0,   NE",
            "0,   99,  SW",
            "199, 99,  SE",
    })
    @DisplayName("resolveEdge() resolves single edges and corners correctly")
    void resolveEdge_resolvesEdgesAndCorners(double x, double y, String expected) {
        logger.debug("Testing resolveEdge({}, {}) -> {}", x, y, expected);
        WindowResizeHandler.ResizeEdge edge =
                WindowResizeHandler.resolveEdge(x, y, RECT_WIDTH, RECT_HEIGHT, MARGIN);
        assertEquals(WindowResizeHandler.ResizeEdge.valueOf(expected), edge,
                "resolveEdge(" + x + ", " + y + ") should resolve to " + expected);
    }

    @Test
    @DisplayName("resolveEdge() prioritizes corners over single edges")
    void resolveEdge_cornerTakesPriorityOverSingleEdge() {
        logger.debug("Testing resolveEdge corner priority");
        WindowResizeHandler.ResizeEdge edge =
                WindowResizeHandler.resolveEdge(2, 2, RECT_WIDTH, RECT_HEIGHT, MARGIN);
        assertEquals(WindowResizeHandler.ResizeEdge.NW, edge,
                "A point near both the top and left edges should resolve to NW, not N or W");
    }

    @ParameterizedTest(name = "resolveEdge at margin boundary ({0}, {1}) -> {2}")
    @CsvSource({
            "6,   50,  W",
            "194, 50,  E",
            "100, 6,   N",
            "100, 94,  S",
            "7,   50,  NONE",
            "193, 50,  NONE",
            "100, 7,   NONE",
            "100, 93,  NONE",
    })
    @DisplayName("resolveEdge() treats the margin boundary inclusively on edges, exclusively in the interior")
    void resolveEdge_marginBoundary_inclusiveOnEdgeExclusiveInInterior(double x, double y, String expected) {
        logger.debug("Testing resolveEdge margin boundary ({}, {}) -> {}", x, y, expected);
        WindowResizeHandler.ResizeEdge edge =
                WindowResizeHandler.resolveEdge(x, y, RECT_WIDTH, RECT_HEIGHT, MARGIN);
        assertEquals(WindowResizeHandler.ResizeEdge.valueOf(expected), edge);
    }

    @Test
    @DisplayName("resolveEdge() with zero margin resolves only points exactly on the perimeter")
    void resolveEdge_zeroMargin_onlyExactPerimeter() {
        logger.debug("Testing resolveEdge with zero margin");
        assertEquals(WindowResizeHandler.ResizeEdge.W,
                WindowResizeHandler.resolveEdge(0, 50, RECT_WIDTH, RECT_HEIGHT, 0));
        assertEquals(WindowResizeHandler.ResizeEdge.NONE,
                WindowResizeHandler.resolveEdge(1, 50, RECT_WIDTH, RECT_HEIGHT, 0));
        assertEquals(WindowResizeHandler.ResizeEdge.NW,
                WindowResizeHandler.resolveEdge(0, 0, RECT_WIDTH, RECT_HEIGHT, 0));
    }

    @Test
    @DisplayName("resolveEdge() with a large margin can classify the centre as a corner")
    void resolveEdge_largeMargin_centreResolvesToCorner() {
        logger.debug("Testing resolveEdge with large margin overlapping centre");
        WindowResizeHandler.ResizeEdge edge =
                WindowResizeHandler.resolveEdge(50, 50, RECT_WIDTH, RECT_HEIGHT, 60);
        assertEquals(WindowResizeHandler.ResizeEdge.NW, edge,
                "When margin bands overlap, corner priority should still apply");
    }
    @Test
    @DisplayName("computeBounds(NONE, ...) returns the starting bounds unchanged")
    void computeBounds_noneEdge_returnsStartingBoundsUnchanged() {
        logger.debug("Testing computeBounds(NONE) returns unchanged bounds");
        WindowResizeHandler.Bounds bounds = WindowResizeHandler.computeBounds(
                WindowResizeHandler.ResizeEdge.NONE,
                START_X, START_Y, START_WIDTH, START_HEIGHT,
                999, -999,
                MIN_WIDTH, MIN_HEIGHT);

        assertEquals(START_X, bounds.x());
        assertEquals(START_Y, bounds.y());
        assertEquals(START_WIDTH, bounds.width());
        assertEquals(START_HEIGHT, bounds.height());
    }

    @Test
    @DisplayName("computeBounds(E, ...) grows width only, leaving x/y/height unchanged")
    void computeBounds_eastEdge_growsWidthOnly() {
        logger.debug("Testing computeBounds(E) grows width only");
        WindowResizeHandler.Bounds bounds = WindowResizeHandler.computeBounds(
                WindowResizeHandler.ResizeEdge.E,
                START_X, START_Y, START_WIDTH, START_HEIGHT,
                50, 0,
                MIN_WIDTH, MIN_HEIGHT);

        assertEquals(START_X, bounds.x());
        assertEquals(START_Y, bounds.y());
        assertEquals(350, bounds.width());
        assertEquals(START_HEIGHT, bounds.height());
    }

    @Test
    @DisplayName("computeBounds(S, ...) grows height only, leaving x/y/width unchanged")
    void computeBounds_southEdge_growsHeightOnly() {
        logger.debug("Testing computeBounds(S) grows height only");
        WindowResizeHandler.Bounds bounds = WindowResizeHandler.computeBounds(
                WindowResizeHandler.ResizeEdge.S,
                START_X, START_Y, START_WIDTH, START_HEIGHT,
                0, 40,
                MIN_WIDTH, MIN_HEIGHT);

        assertEquals(START_X, bounds.x());
        assertEquals(START_Y, bounds.y());
        assertEquals(START_WIDTH, bounds.width());
        assertEquals(240, bounds.height());
    }

    @Test
    @DisplayName("computeBounds(W, ...) resizes width and repositions x together")
    void computeBounds_westEdge_resizesAndRepositions() {
        logger.debug("Testing computeBounds(W) resizes and repositions");
        WindowResizeHandler.Bounds bounds = WindowResizeHandler.computeBounds(
                WindowResizeHandler.ResizeEdge.W,
                START_X, START_Y, START_WIDTH, START_HEIGHT,
                30, 0,
                MIN_WIDTH, MIN_HEIGHT);

        assertEquals(40, bounds.x(), "x should shift right by the drag delta");
        assertEquals(START_Y, bounds.y());
        assertEquals(270, bounds.width(), "width should shrink by the drag delta");
        assertEquals(START_HEIGHT, bounds.height());
    }

    @Test
    @DisplayName("computeBounds(N, ...) resizes height and repositions y together")
    void computeBounds_northEdge_resizesAndRepositions() {
        logger.debug("Testing computeBounds(N) resizes and repositions");
        WindowResizeHandler.Bounds bounds = WindowResizeHandler.computeBounds(
                WindowResizeHandler.ResizeEdge.N,
                START_X, START_Y, START_WIDTH, START_HEIGHT,
                0, 25,
                MIN_WIDTH, MIN_HEIGHT);

        assertEquals(START_X, bounds.x());
        assertEquals(45, bounds.y(), "y should shift down by the drag delta");
        assertEquals(START_WIDTH, bounds.width());
        assertEquals(175, bounds.height(), "height should shrink by the drag delta");
    }

    @Test
    @DisplayName("computeBounds(E, ...) with zero delta leaves bounds unchanged")
    void computeBounds_eastEdge_zeroDelta_unchanged() {
        logger.debug("Testing computeBounds(E) with zero delta");
        WindowResizeHandler.Bounds bounds = WindowResizeHandler.computeBounds(
                WindowResizeHandler.ResizeEdge.E,
                START_X, START_Y, START_WIDTH, START_HEIGHT,
                0, 0,
                MIN_WIDTH, MIN_HEIGHT);

        assertEquals(START_X, bounds.x());
        assertEquals(START_Y, bounds.y());
        assertEquals(START_WIDTH, bounds.width());
        assertEquals(START_HEIGHT, bounds.height());
    }

    @Test
    @DisplayName("computeBounds(NW, ...) resizes and repositions both dimensions")
    void computeBounds_nwCorner_resizesAndRepositionsBothDimensions() {
        logger.debug("Testing computeBounds(NW) resizes both dimensions");
        WindowResizeHandler.Bounds bounds = WindowResizeHandler.computeBounds(
                WindowResizeHandler.ResizeEdge.NW,
                START_X, START_Y, START_WIDTH, START_HEIGHT,
                20, 10,
                MIN_WIDTH, MIN_HEIGHT);

        assertEquals(30, bounds.x());
        assertEquals(30, bounds.y());
        assertEquals(280, bounds.width());
        assertEquals(190, bounds.height());
    }

    @Test
    @DisplayName("computeBounds(SE, ...) grows width and height without moving origin")
    void computeBounds_seCorner_growsBothDimensions() {
        logger.debug("Testing computeBounds(SE) grows both dimensions");
        WindowResizeHandler.Bounds bounds = WindowResizeHandler.computeBounds(
                WindowResizeHandler.ResizeEdge.SE,
                START_X, START_Y, START_WIDTH, START_HEIGHT,
                50, 40,
                MIN_WIDTH, MIN_HEIGHT);

        assertEquals(START_X, bounds.x());
        assertEquals(START_Y, bounds.y());
        assertEquals(350, bounds.width());
        assertEquals(240, bounds.height());
    }

    @Test
    @DisplayName("computeBounds(E, ...) clamps width at the minimum")
    void computeBounds_eastEdge_clampsAtMinWidth() {
        logger.debug("Testing computeBounds(E) clamps at minimum width");
        WindowResizeHandler.Bounds bounds = WindowResizeHandler.computeBounds(
                WindowResizeHandler.ResizeEdge.E,
                START_X, START_Y, START_WIDTH, START_HEIGHT,
                -250, 0,
                MIN_WIDTH, MIN_HEIGHT);

        assertEquals(MIN_WIDTH, bounds.width(), "Width should be clamped at the minimum");
        assertEquals(START_X, bounds.x(), "x is unaffected by the E edge even when clamped");
    }

    @Test
    @DisplayName("computeBounds(W, ...) clamps width and x together at the minimum")
    void computeBounds_westEdge_clampsWidthAndX() {
        logger.debug("Testing computeBounds(W) clamps width and x together");
        WindowResizeHandler.Bounds bounds = WindowResizeHandler.computeBounds(
                WindowResizeHandler.ResizeEdge.W,
                START_X, START_Y, START_WIDTH, START_HEIGHT,
                250, 0,
                MIN_WIDTH, MIN_HEIGHT);

        assertEquals(MIN_WIDTH, bounds.width(), "Width should be clamped at the minimum");
        assertEquals(210, bounds.x(),
                "x should stop at startX + (startWidth - minWidth) = 10 + 200 = 210");
    }

    @Test
    @DisplayName("computeBounds(N, ...) clamps height and y together at the minimum")
    void computeBounds_northEdge_clampsHeightAndY() {
        logger.debug("Testing computeBounds(N) clamps height and y together");
        WindowResizeHandler.Bounds bounds = WindowResizeHandler.computeBounds(
                WindowResizeHandler.ResizeEdge.N,
                START_X, START_Y, START_WIDTH, START_HEIGHT,
                0, 150,
                MIN_WIDTH, MIN_HEIGHT);

        assertEquals(MIN_HEIGHT, bounds.height(), "Height should be clamped at the minimum");
        assertEquals(120, bounds.y(),
                "y should stop at startY + (startHeight - minHeight) = 20 + 100 = 120");
    }

    @Test
    @DisplayName("computeBounds(S, ...) clamps height at the minimum without moving y")
    void computeBounds_southEdge_clampsAtMinHeight() {
        logger.debug("Testing computeBounds(S) clamps at minimum height");
        WindowResizeHandler.Bounds bounds = WindowResizeHandler.computeBounds(
                WindowResizeHandler.ResizeEdge.S,
                START_X, START_Y, START_WIDTH, START_HEIGHT,
                0, -150,
                MIN_WIDTH, MIN_HEIGHT);

        assertEquals(MIN_HEIGHT, bounds.height());
        assertEquals(START_Y, bounds.y(), "y is unaffected by the S edge even when clamped");
    }

    @Test
    @DisplayName("computeBounds(NW, ...) clamps both dimensions and both positions at the minimum")
    void computeBounds_nwCorner_clampsBothDimensionsAndPositions() {
        logger.debug("Testing computeBounds(NW) clamps both dimensions");
        WindowResizeHandler.Bounds bounds = WindowResizeHandler.computeBounds(
                WindowResizeHandler.ResizeEdge.NW,
                START_X, START_Y, START_WIDTH, START_HEIGHT,
                250, 150,
                MIN_WIDTH, MIN_HEIGHT);

        assertEquals(MIN_WIDTH, bounds.width());
        assertEquals(MIN_HEIGHT, bounds.height());
        assertEquals(210, bounds.x());
        assertEquals(120, bounds.y());
    }

    @ParameterizedTest(name = "cursorFor({0}) returns a non-null cursor")
    @EnumSource(WindowResizeHandler.ResizeEdge.class)
    @DisplayName("cursorFor() returns a non-null cursor for every edge, DEFAULT for NONE")
    void cursorFor_returnsNonNullCursorForEveryEdge(WindowResizeHandler.ResizeEdge edge) {
        logger.debug("Testing cursorFor({}) returns a cursor", edge);
        Cursor cursor = WindowResizeHandler.cursorFor(edge);

        assertNotNull(cursor, "cursorFor(" + edge + ") must not return null");
        if (edge == WindowResizeHandler.ResizeEdge.NONE) {
            assertEquals(Cursor.DEFAULT, cursor, "NONE should map to the default cursor");
        }
    }

    @ParameterizedTest(name = "cursorFor({0}) returns {1}")
    @CsvSource({
            "N,  V_RESIZE",
            "S,  V_RESIZE",
            "E,  H_RESIZE",
            "W,  H_RESIZE",
            "NE, NE_RESIZE",
            "SW, NE_RESIZE",
            "NW, NW_RESIZE",
            "SE, NW_RESIZE",
    })
    @DisplayName("cursorFor() maps every active edge to the correct directional cursor")
    void cursorFor_mapsEdgesToDirectionalCursors(String edgeName, String cursorName) {
        logger.debug("Testing cursorFor({}) -> {}", edgeName, cursorName);
        WindowResizeHandler.ResizeEdge edge = WindowResizeHandler.ResizeEdge.valueOf(edgeName);
        Cursor expected = switch (cursorName) {
            case "V_RESIZE" -> Cursor.V_RESIZE;
            case "H_RESIZE" -> Cursor.H_RESIZE;
            case "NE_RESIZE" -> Cursor.NE_RESIZE;
            case "NW_RESIZE" -> Cursor.NW_RESIZE;
            default -> throw new IllegalArgumentException("Unknown cursor: " + cursorName);
        };

        assertEquals(expected, WindowResizeHandler.cursorFor(edge));
    }

    @Test
    @DisplayName("DEFAULT_MARGIN is loaded from config and is positive")
    void defaultMargin_isPositive() {
        logger.debug("Testing DEFAULT_MARGIN is positive (value={})", WindowResizeHandler.DEFAULT_MARGIN);
        assertTrue(WindowResizeHandler.DEFAULT_MARGIN > 0,
                "DEFAULT_MARGIN should be a positive pixel value");
    }

    /**
     * Verifies the screen-coordinate delta arithmetic performed during a resize
     * drag before {@link WindowResizeHandler#computeBounds} is called. Uses
     * Mockito mocks of a plain {@link ScreenPoint} interface rather than
     * {@link MouseEvent}, which cannot be mocked on this JVM.
     */
    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("drag delta contract (Mockito)")
    class DragDeltaTests {

        @Test
        @DisplayName("screen deltas from mocked screen points feed computeBounds correctly")
        void dragDelta_fromMockScreenPoints_producesExpectedBounds() {
            logger.debug("Testing drag delta contract with mocked screen points");
            ScreenPoint press = new ScreenPoint(500.0, 500.0);
            ScreenPoint drag = new ScreenPoint(500.0, 500.0);

            double deltaX = drag.screenX() - press.screenX();
            double deltaY = drag.screenY() - press.screenY();

            WindowResizeHandler.Bounds bounds = WindowResizeHandler.computeBounds(
                    WindowResizeHandler.ResizeEdge.E,
                    START_X, START_Y, START_WIDTH, START_HEIGHT,
                    deltaX, deltaY,
                    MIN_WIDTH, MIN_HEIGHT);

            assertEquals(350, bounds.width());
            assertEquals(START_X, bounds.x());
            assertEquals(START_HEIGHT, bounds.height());
        }
    }

    /**
     * For replacing the screen coordinates from a {@link MouseEvent} during drag handling.
     * @param screenX the {@code x} coordinate of the {@link MouseEvent}
     * @param screenY the {@code y} coordinate of the {@link MouseEvent}
     */
    record ScreenPoint(double screenX, double screenY) {}

    /**
     * Tests for {@link WindowResizeHandler#attach(Node, Stage, double, double)}.
     *
     * <p>
     *     Starts a headless JavaFX toolkit once per nested class, then drives the
     *     registered handlers by firing real {@link MouseEvent} instances on a
     *     {@link Rectangle} node. Mockito is not used here because JavaFX node
     *     types cannot be mocked or spied on this JVM.
     * </p>
     */
    @Nested
    @DisplayName("attach() event-handler integration")
    class AttachTests {

        private Rectangle target;
        private Stage stage;

        @BeforeAll
        static void initJavaFxToolkit() throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(1);
            try {
                Platform.startup(latch::countDown);
            } catch (IllegalStateException alreadyRunning) {
                latch.countDown();
            }
            assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX toolkit should start within 5 seconds");
        }

        @BeforeEach
        void setUpAttach() throws InterruptedException {
            this.runOnFxThread(() -> {
                this.target = new Rectangle(RECT_WIDTH, RECT_HEIGHT);
                this.stage = new Stage();
                this.stage.setX(START_X);
                this.stage.setY(START_Y);
                this.stage.setWidth(START_WIDTH);
                this.stage.setHeight(START_HEIGHT);
            });
        }

        @Test
        @DisplayName("attach() wires MOUSE_MOVED so hovering an edge updates the cursor")
        void attach_mouseMoved_setsCursorForEdge() throws InterruptedException {
            logger.debug("Testing MOUSE_MOVED sets cursor on edge hover");
            this.runOnFxThread(() -> {
                WindowResizeHandler.attach(this.target, this.stage, MIN_WIDTH, MIN_HEIGHT);
                this.fire(this.mouseEvent(MouseEvent.MOUSE_MOVED, 100, 0, 500, 500));
                assertEquals(Cursor.V_RESIZE, this.target.getCursor());
            });
        }

        @Test
        @DisplayName("attach() MOUSE_MOVED sets DEFAULT cursor when hovering the interior")
        void attach_mouseMoved_setsDefaultCursorInInterior() throws InterruptedException {
            logger.debug("Testing MOUSE_MOVED sets default cursor in interior");
            this.runOnFxThread(() -> {
                WindowResizeHandler.attach(this.target, this.stage, MIN_WIDTH, MIN_HEIGHT);
                this.fire(this.mouseEvent(MouseEvent.MOUSE_MOVED, 100, 50, 500, 500));
                assertEquals(Cursor.DEFAULT, this.target.getCursor());
            });
        }

        @Test
        @DisplayName("attach() MOUSE_DRAGGED without a prior edge press does not resize the stage")
        void attach_mouseDragged_withoutPress_doesNotResizeStage() throws InterruptedException {
            logger.debug("Testing MOUSE_DRAGGED without press does not resize");
            this.runOnFxThread(() -> {
                WindowResizeHandler.attach(this.target, this.stage, MIN_WIDTH, MIN_HEIGHT);
                this.fire(this.mouseEvent(MouseEvent.MOUSE_DRAGGED, 100, 50, 550, 550));
                assertEquals(START_WIDTH, this.stage.getWidth());
                assertEquals(START_HEIGHT, this.stage.getHeight());
            });
        }

        @Test
        @DisplayName("attach() MOUSE_DRAGGED after interior press does not resize the stage")
        void attach_mouseDragged_afterInteriorPress_doesNotResizeStage() throws InterruptedException {
            logger.debug("Testing MOUSE_DRAGGED after interior press does not resize");
            this.runOnFxThread(() -> {
                WindowResizeHandler.attach(this.target, this.stage, MIN_WIDTH, MIN_HEIGHT);
                this.fire(this.mouseEvent(MouseEvent.MOUSE_PRESSED, 100, 50, 500, 500));
                this.fire(this.mouseEvent(MouseEvent.MOUSE_DRAGGED, 100, 50, 550, 550));
                assertEquals(START_WIDTH, this.stage.getWidth());
                assertEquals(START_HEIGHT, this.stage.getHeight());
            });
        }

        @Test
        @DisplayName("attach() MOUSE_DRAGGED after edge press resizes the stage")
        void attach_mouseDragged_afterEdgePress_resizesStage() throws InterruptedException {
            logger.debug("Testing MOUSE_DRAGGED after E-edge press resizes stage");
            this.runOnFxThread(() -> {
                WindowResizeHandler.attach(this.target, this.stage, MIN_WIDTH, MIN_HEIGHT);
                this.fire(this.mouseEvent(MouseEvent.MOUSE_PRESSED, 199, 50, 500, 500));
                this.fire(this.mouseEvent(MouseEvent.MOUSE_DRAGGED, 199, 50, 550, 500));
                assertEquals(350, this.stage.getWidth());
                assertEquals(START_HEIGHT, this.stage.getHeight());
            });
        }

        @Test
        @DisplayName("attach() MOUSE_RELEASED clears drag state so a subsequent drag does not resize")
        void attach_mouseReleased_clearsDragState() throws InterruptedException {
            logger.debug("Testing MOUSE_RELEASED clears drag state");
            this.runOnFxThread(() -> {
                WindowResizeHandler.attach(this.target, this.stage, MIN_WIDTH, MIN_HEIGHT);
                this.fire(this.mouseEvent(MouseEvent.MOUSE_PRESSED, 199, 50, 500, 500));
                this.fire(this.mouseEvent(MouseEvent.MOUSE_RELEASED, 199, 50, 500, 500));
                this.fire(this.mouseEvent(MouseEvent.MOUSE_DRAGGED, 199, 50, 550, 500));
                assertEquals(START_WIDTH, this.stage.getWidth());
            });
        }

        @Test
        @DisplayName("attach() west-edge drag repositions x and shrinks width together")
        void attach_westEdgeDrag_repositionsAndShrinks() throws InterruptedException {
            logger.debug("Testing west-edge drag repositions and shrinks");
            this.runOnFxThread(() -> {
                WindowResizeHandler.attach(this.target, this.stage, MIN_WIDTH, MIN_HEIGHT);
                this.fire(this.mouseEvent(MouseEvent.MOUSE_PRESSED, 0, 50, 500, 500));
                this.fire(this.mouseEvent(MouseEvent.MOUSE_DRAGGED, 0, 50, 530, 500));
                assertEquals(40, this.stage.getX());
                assertEquals(270, this.stage.getWidth());
            });
        }

        private void runOnFxThread(FxRunnable action) throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Throwable> error = new AtomicReference<>();
            Platform.runLater(() -> {
                try {
                    action.run();
                } catch (Throwable t) {
                    error.set(t);
                } finally {
                    latch.countDown();
                }
            });
            assertTrue(latch.await(5, TimeUnit.SECONDS), "FX runnable should complete within 5 seconds");
            if (error.get() != null) {
                Throwable t = error.get();
                if (t instanceof RuntimeException runtime) {
                    throw runtime;
                }
                if (t instanceof Error err) {
                    throw err;
                }
                throw new AssertionError(t);
            }
        }

        @FunctionalInterface
        private interface FxRunnable {
            void run();
        }

        private void fire(MouseEvent event) {
            Event.fireEvent(this.target, event);
        }

        private MouseEvent mouseEvent(javafx.event.EventType<MouseEvent> type,
                                      double localX, double localY,
                                      double screenX, double screenY) {
            boolean pressed = type == MouseEvent.MOUSE_PRESSED || type == MouseEvent.MOUSE_DRAGGED;
            return new MouseEvent(type,
                    localX, localY, screenX, screenY,
                    MouseButton.PRIMARY, 1,
                    false, false, false, false,
                    pressed, false, false,
                    false, false,
                    false, null);
        }
    }
}
