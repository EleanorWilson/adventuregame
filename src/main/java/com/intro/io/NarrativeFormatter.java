package com.intro.io;

import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * Formats banner lines (separator and centered title) for a given character width.
 *
 * <p>
 *     Used by {@link GuiIO} and {@link ConsoleIO} when printing scene banners.
 *     Character width is derived from a JavaFX {@link Font} so GUI output matches
 *     the monospace narrative {@link javafx.scene.control.TextArea}.
 * </p>
 */
public final class NarrativeFormatter {

    /** Minimum characters per banner line when computed width is very small. */
    static final int MIN_LINE_WIDTH = 10;

    private NarrativeFormatter() {
    }

    /**
     * Computes how many monospace characters fit on one line at {@code availablePixels}.
     *
     * @param availablePixels horizontal space available for text; values {@code <= 0}
     *                        yield {@link #MIN_LINE_WIDTH}
     * @param font            font used by the narrative area; must not be {@code null}
     * @return character count, at least {@link #MIN_LINE_WIDTH}
     */
    public static int charsPerLine(double availablePixels, Font font) {
        if (availablePixels <= 0) {
            return MIN_LINE_WIDTH;
        }
        Text probe = new Text("*");
        probe.setFont(font);
        double charWidth = probe.getLayoutBounds().getWidth();
        if (charWidth <= 0) {
            return MIN_LINE_WIDTH;
        }
        return Math.max(MIN_LINE_WIDTH, (int) Math.floor(availablePixels / charWidth));
    }

    /**
     * Returns a line of asterisks exactly {@code width} characters long.
     *
     * @param width desired length; values below 1 are treated as 1
     * @return a string of {@code *} characters
     */
    public static String separatorLine(int width) {
        return "*".repeat(Math.max(1, width));
    }

    /**
     * Returns {@code text} padded with leading spaces so it is centered within
     * {@code width} characters.
     *
     * @param text  the title to center; must not be {@code null}
     * @param width total line width in characters
     * @return the padded line
     */
    public static String centeredLine(String text, int width) {
        int pad = Math.max(0, (width - text.length()) / 2);
        return " ".repeat(pad) + text;
    }
}
