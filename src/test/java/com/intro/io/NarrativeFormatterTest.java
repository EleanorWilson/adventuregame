package com.intro.io;

import javafx.scene.text.Font;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NarrativeFormatterTest {

    private static final Font MONOSPACE = Font.font("monospace", 14);

    @Test
    @DisplayName("separatorLine() repeats asterisks to the requested width")
    void separatorLine_repeatsAsterisks() {
        assertEquals("**********", NarrativeFormatter.separatorLine(10));
        assertEquals("*", NarrativeFormatter.separatorLine(0));
        assertEquals("*", NarrativeFormatter.separatorLine(-5));
    }

    @Test
    @DisplayName("centeredLine() pads text with leading spaces")
    void centeredLine_padsWithSpaces() {
        assertEquals("  GAME OVER", NarrativeFormatter.centeredLine("GAME OVER", 14));
        assertEquals("GAME OVER", NarrativeFormatter.centeredLine("GAME OVER", 9));
        assertEquals("GAME OVER", NarrativeFormatter.centeredLine("GAME OVER", 5));
    }

    @Test
    @DisplayName("centeredLine() handles odd remainder by flooring padding")
    void centeredLine_oddRemainder() {
        assertEquals(" AB", NarrativeFormatter.centeredLine("AB", 4));
    }

    @Test
    @DisplayName("charsPerLine() returns minimum when available pixels is non-positive")
    void charsPerLine_nonPositivePixels() {
        assertEquals(NarrativeFormatter.MIN_LINE_WIDTH, NarrativeFormatter.charsPerLine(0, MONOSPACE));
        assertEquals(NarrativeFormatter.MIN_LINE_WIDTH, NarrativeFormatter.charsPerLine(-10, MONOSPACE));
    }

    @Test
    @DisplayName("charsPerLine() scales with available pixel width")
    void charsPerLine_scalesWithWidth() {
        int narrow = NarrativeFormatter.charsPerLine(80, MONOSPACE);
        int wide = NarrativeFormatter.charsPerLine(400, MONOSPACE);
        assertTrue(wide > narrow);
        assertTrue(narrow >= NarrativeFormatter.MIN_LINE_WIDTH);
    }
}
