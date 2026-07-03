<style>
body {
font-family: "Inter", "Inter Regular", "Segoe UI", sans-serif !important;
color: #E6DCD1;
background-color: #191A1C;
line-height: 1.6;
padding: 30px;
max-width: 800px;
}

h1 { color: #648fff }
h2 { color: #fe6100 } 
h3 { color: #dc267f }
h4 { color: #ffb000 }
h5 { color: #785ef0 }

code, pre, pre code {
font-family: "Google Sans Code", "Consolas", monospace !important;
color: #009E30;
font-weight: 700 !important;
background-color: #191A1C;
}

pre {
padding: 12px;
overflow-x: auto;
display: block;
}

</style>

# Threading issues

The application thread and javafx thread MUST be separate. The application thread needs to "pause" after a prompt whilst it waits for player input. Whereas the javafx thread must continue to run.

`Platform.runLater(Runnable);` is a static method that posts a `Runnable` to the Application thread's event queue, ensuring the code inside the `Runnable` executes on the GUI thread.

 ## Synchronisation

If the background task uses Platform.runLater to show the captcha dialogue but does not wait for the user’s input, it may proceed before the dialogue closes.

### Queues

When to use each type:

- `LinkedBlockingQueue` -> One blocking, one non-blocking, thread-safe, need a safety buffer
- `ConcurrentLinkedQueue` -> Need to poll _without_ blocking
- `SynchronousQueue`  -> no elements to store, producer/consumer threads hand-off directly
- `ArrayBlockingQueue` -> storing elements, bounded buffer between producer/consumer thread, limits memory usage, speed differences between threads

#### LinkedBlockingQueue

How to use:

```java
import java.util.concurrent.LinkedBlockingQueue;
// Method should have a throws InterruptedException line

// Create a LinkedBlockingQueue  with a capacity of 3
LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>(3);
queue.put("first thing");
queue.put("second thing");
queue.put("third thing");
```

# Fonts in FXML

To add a custom font, you should create a font directory under resources:

```java
src
├─ main/
│  ├─ java/
│  │  └─ app/
│  │      ├─ App.java
│  │      └─ Controller.java
│  └─ resources/
│      ├─ app/
│      │   ├─ view.fxml
│      │   └─ styles.css
│      └─ fonts/
│          └─ MyFont.ttf
```

# Box drawing alt codes

- ┌ (Box Drawings Light Down and Right) - Alt + 218
- ┐ (Box Drawings Light Down and Left) - Alt + 191
- └ (Box Drawings Light Up and Right) - Alt + 192
- ┘ (Box Drawings Light Up and Left) - Alt + 217
- ─ (Box Drawings Light Horizontal) - Alt + 196
- │ (Box Drawings Light Vertical) - Alt + 179
- ├ (Box Drawings Light Vertical and Right) - Alt + 195
- ┤ (Box Drawings Light Vertical and Left) - Alt + 180
- ┬ (Box Drawings Light Down and Horizontal) - Alt + 194
- ┴ (Box Drawings Light Up and Horizontal) - Alt + 193
- ┼ (Box Drawings Light Vertical and Horizontal) - Alt + 197


# Config and Properties loading

Aim is to separate config.properties into two files:

- config.properties should handle: settings, numeric limits, feature flags, etc
- messages.properties should handle strings that player sees on screen: window title, button labels, prompt text, etc and should be loaded by resourceBundle


# Reusable Code

## Wrapping from config file

```java
    /**
     * Soft-wraps a given string every 'n' characters at a clean breakpoint (e.g. newline
     * character or a space character), ensuring no individual line exceeds a specified
     * maximum length.
     * <p>
     *     The algorithm scans Strings in lengths of {@code n} number of characters. It
     *     first looks for newline '{@code n}' characters, then if none are found, breaks
     *     the line at the last available space character. If no space character is found
     *     the word is printed over multiple lines with a hyphen character before each line
     *     break.
     * </p>
     * <h3>Edge Case Handling:</h3>
     * <ul>
     *     <li>If the input is {@code null} or empty, it is returned without changes.</li>
     *     <li>If the input is shorter than {@code n}, it is returned without changes.</li>
     *     <li>If a single word is longer than {@code n} and contains no spaces or newlines
     *     to break on, the word is printed over multiple lines with a hyphen '{@code  -}'
     *     character.</li>
     *     <li>If {@code n} is {@code 1}, word is printed character by character.</li>
     * </ul>
     * @param input String to be processed (can be {@code null} or empty).
     * @param n the maximum allowed character width for any single line, must be greater than
     *          {@code 0}.
     * @return the formatted string containing wrapped and hyphenated lines
     * @throws IllegalArgumentException if {@code n} is less than or equal to {@code 0}.
     */
    // TODO remove unnecessary method when implementing GUI
    public String wrapStringByCharacterLimit(String input, int n) {
        // Check conditions
        if (n <= 0) {
            throw new IllegalArgumentException("Wrap width (n) must be greater than 0.");
        }
        logger.debug("Attempting to wrap string. Input length: {}, wrap width (n): {}",
                input != null ? input.length() : 0, n);
        if (input == null || input.isEmpty()) {
            return input;
        }
        StringBuilder sb = new StringBuilder(input);
        int index = 0;

        while (sb.length() > index + n) {
            // Check for existing newline characters
            int lastLineReturn = sb.lastIndexOf("\n", index + n);
            if (lastLineReturn > index) {
                index = lastLineReturn;
                continue;
            }
            // Check for spaces to break the line cleanly
            int lastSpace = sb.lastIndexOf(" ", index + n);
            if (lastSpace > index) {
                sb.replace(lastSpace, lastSpace + 1, "\n");
                index = lastSpace + 1;
                continue;
            }
            // Handle oversized words (if no space or newline found)
            logger.debug("Oversized word detected at index {}. Adding hyphenation.", index);
            if (n > 1) {
                // Insert hyphen at (n-1) position, so total line length = n
                int hyphenIndex = index + (n-1);
                sb.insert(hyphenIndex, "-\n");
                // Advance index past the hyphen and newline (\n is 1 char, - is 1 char = 2 char total)
                index = hyphenIndex + 2;
            } else {
                // Edge case where n=1, print char by char
                sb.insert(index + n, "\n");
                index = index + n + 1;
            }
        }
        return sb.toString();
    }
```

### TESTING 

Example tests for this method:

```java

    @Test
    @DisplayName("wrapStringByCharacterLimit returns null input unchanged")
    void testWrapReturnsNullInputUnchanged() {
        logger.debug("Testing wrap return null input unchanged");
        assertNull(io.wrapStringByCharacterLimit(null, 10), "Null input should be returned unchanged");
    }

    @Test
    @DisplayName("wrapStringByCharacterLimit returns empty strings unchanged")
    void testWrapReturnsEmptyStringsUnchanged() {
        logger.debug("Testing wrap returns empty strings unchanged");
        assertEquals("", io.wrapStringByCharacterLimit("", 10), "Empty string should be returned unchanged");
    }

    @Test
    @DisplayName("wrapStringByCharacterLimit returns Strings whose length is equal to or smaller than limit without changes")
    void testWrapReturnsUnchangedStrings() {
        logger.debug("Testing wrap returns unchanged String when length is below or equal to limit");
        int limit = 10;
        String testString1 = "ABCDEFGHIJ";
        String testString2 = "123456789";
        assertEquals(testString1, io.wrapStringByCharacterLimit(testString1, limit), "String of length " + testString1.length() + " and wrap limit " + limit + " should be returned unchanged");
        assertEquals(testString2, io.wrapStringByCharacterLimit(testString2, limit), "String of length " + testString2.length() + " and wrap limit " + limit + " should be returned unchanged");
    }

    @Test
    @DisplayName("wrapStringByCharacterLimit splits lines at last ' ' space character")
    void testWrapSplitsStringAtSpaces() {
        logger.debug("Testing wrap breaks at space characters");
        String testString = "Hello World Test";
        int limit = 10;
        String result = io.wrapStringByCharacterLimit(testString, limit);
        for (String line : result.split("\n")) {
            assertTrue(line.length() <= limit, "Line exceeds limit (" + limit + "): " + line + "(length: " + line.length() + ")");
        }
    }

    @Test
    @DisplayName("wrapStringByCharacterLimit splits lines at '\\n' newline characters")
    void testWrapSplitsStringAtLineBreakChars() {
        logger.debug("Testing wrap splits string at newline characters");
        String testString = "Testing\nString";
        int limit = 10;
        assertEquals("Testing\nString", io.wrapStringByCharacterLimit(testString, limit), "Wrap should split string at newline character");
    }

    @Test
    @DisplayName("wrapStringByCharacterLimit hyphenates words longer than limit")
    void testWrapHyphenatesLongWords() {
        logger.debug("Testing wrap hyphenates words longer than limit, when limit greater than 1");
        int limit = 5;
        String testString = "Incomprehensibilities";
        String result = io.wrapStringByCharacterLimit(testString, limit);
        for (String line : result.split("\n")) {
            assertTrue(line.length() <= limit, "Line exceeds limit (" + limit + "): " + line + "(length: " + line.length() + ")");
        }
        assertTrue(result.contains("-"), "Words longer than limit should be hyphenated, when limit greater than 1");
    }


    @Test
    @DisplayName("wrapStringByCharacterLimit prints char by char when limit is 1")
    void testWrapPrintsCharByChar() {
        logger.debug("Testing wrap prints char by char when limit is 1, with no hyphenation");
        String testString = "ABCDEFG";
        String result = io.wrapStringByCharacterLimit(testString, 1);
        for (String line : result.split("\n")) {
            assertTrue(line.length() <= 1, "Expected length 1 but found: " + line + "(length: " + line.length() + ")");
        }
        assertFalse(result.contains("-"), "limit = 1 should produce no hyphens");
    }

    @Test
    @DisplayName("wrapStringByCharacterLimit can handle paragraphs")
    void testWrapHandlesParagraphs() {
        logger.debug("Testing wrapStringByCharacterLimit handles paragraphs as expected, no lines exceed limit");
        int limit = 25;
        String testParagraph = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";
        String result = io.wrapStringByCharacterLimit(testParagraph, limit);
        for (String line : result.split("\n")) {
            assertTrue(line.length() <= limit, "Line exceeds limit (" + limit + "): " + line + "(length: " + line.length() + ")");
        }
    }


    @Test
    @DisplayName("wrapStringByCharacterLimit preserves non-whitespace characters")
    void testWrapPreservesWhitespace() {
        logger.debug("Testing wrapStringByCharacterLimit preserves non-whitespace characters");
        String input = "No non-whitespace characters are lost when wrap method is called";
        String result = io.wrapStringByCharacterLimit(input, 15);
        assertEquals(
                input.replaceAll("\\s+", ""),
                result.replaceAll("\\s+", ""),
                "All non-whitespace characters are preserved after wrapStringByCharacterLimit");
    }

    @ParameterizedTest(name = "Invalid limit throws IllegalArgumentException")
    @ValueSource(ints = {0, -1, -10})
    @DisplayName("wrapStringByCharacterLimit throws IllegalArgumentException with an invalid limit")
    void testWrapInvalidLimitThrowsException(int invalidLimit) {
        logger.debug("Testing wrap throws exception for invalid limit: {}", invalidLimit);
        assertThrows(IllegalArgumentException.class,
                () -> io.wrapStringByCharacterLimit("String", invalidLimit), "Should throw IllegalArgumentException when n = " + invalidLimit);
    }

```