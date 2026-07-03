package com.intro.scene;

import com.intro.io.GameIO;
import com.intro.model.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PlayerSetupScene}.
 *
 * <h3>Parameterization strategy</h3>
 * <p>
 *     {@link PlayerSetupScene} rejects any name that is empty or consists entirely of
 *     whitespace (via {@link String#isBlank()}).  The original design had two separate
 *     {@code @Test} methods — one for {@code ""} and one for {@code "   "} — that were
 *     structurally identical (same stub sequence, same assertions).  They are consolidated
 *     into a single {@link ParameterizedTest @ParameterizedTest} with
 *     {@link ValueSource @ValueSource(strings = {...})}.  Adding further blank-string
 *     variants (e.g. {@code "\t"}) requires only a new entry in the annotation.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class PlayerSetupSceneTest {

    private static final Logger logger = LogManager.getLogger(PlayerSetupSceneTest.class);

    /** Mock {@link GameIO} injected by Mockito. */
    @Mock
    private GameIO mockIO;

    /** The scene under test, recreated before every test invocation. */
    private PlayerSetupScene scene;

    /**
     * Real {@link Player} instance used to verify name assignment side-effects.
     * A real object is preferred here so that {@link Player#getName()} returns
     * what was actually set, without requiring extra stubbing.
     */
    private Player player;

    /**
     * Creates a fresh {@link PlayerSetupScene} and a new {@link Player} before each
     * test (including each {@code @ParameterizedTest} invocation).
     */
    @BeforeEach
    void setUp() {
        logger.debug("Setting up PlayerSetupSceneTest");
        scene = new PlayerSetupScene();
        player = new Player();
    }

    // -------------------------------------------------------------------------
    // getID
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link PlayerSetupScene#getID()} returns {@link SceneID#PLAYER_SETUP}.
     */
    @Test
    @DisplayName("getID() returns SceneID.PLAYER_SETUP")
    void testGetIDReturnsPlayerSetup() {
        logger.debug("Testing getID returns PLAYER_SETUP");
        assertEquals(SceneID.PLAYER_SETUP, scene.getID(),
                "getID() should return SceneID.PLAYER_SETUP");
    }

    // -------------------------------------------------------------------------
    // Normal flow — confirm name
    // -------------------------------------------------------------------------

    /**
     * Verifies that when the player enters a name and immediately confirms it with
     * choice {@code "2"} (No — do not change), the scene returns {@link SceneID#FOREST}.
     */
    @Test
    @DisplayName("play() returns FOREST when the player confirms their name")
    void testPlayConfirmsNameReturnsForest() {
        logger.debug("Testing play returns FOREST on name confirmation");
        when(mockIO.prompt(anyString())).thenReturn("Hero", "2");

        SceneID result = scene.play(player, mockIO);

        assertEquals(SceneID.FOREST, result,
                "Confirming the name should transition to SceneID.FOREST");
    }

    /**
     * Verifies that the name entered by the player is stored on the {@link Player}
     * object after they confirm it.
     */
    @Test
    @DisplayName("play() stores the confirmed name on the Player object")
    void testPlaySetsPlayerName() {
        logger.debug("Testing play sets player name");
        when(mockIO.prompt(anyString())).thenReturn("Merlin", "2");

        scene.play(player, mockIO);

        assertEquals("Merlin", player.getName(),
                "The player's name should be set to the entered value");
    }

    /**
     * Verifies that the name entered by the player is echoed back to them before
     * the confirmation prompt is shown.
     */
    @Test
    @DisplayName("play() echoes the entered name back to the player")
    void testPlayEchosNameToPlayer() {
        logger.debug("Testing play echoes name to player");
        when(mockIO.prompt(anyString())).thenReturn("Percival", "2");

        scene.play(player, mockIO);

        verify(mockIO).println("Your name is: Percival");
    }

    // -------------------------------------------------------------------------
    // Name change loop
    // -------------------------------------------------------------------------

    /**
     * Verifies that when the player chooses to change their name (choice {@code "1"} /
     * "Yes") and then enters a new name and confirms it, the {@link Player} object
     * ends up holding the new name.
     */
    @Test
    @DisplayName("play() allows the player to change their name before confirming")
    void testPlayChangeNameUpdatesPlayerName() {
        logger.debug("Testing play with name change");
        when(mockIO.prompt(anyString())).thenReturn("OldName", "1", "NewName", "2");

        scene.play(player, mockIO);

        assertEquals("NewName", player.getName(),
                "After a name change the player object should hold the updated name");
    }

    // -------------------------------------------------------------------------
    // Blank / empty name rejection (parameterized)
    // -------------------------------------------------------------------------

    /**
     * Verifies that blank and empty names are rejected before being stored, the
     * appropriate error message is displayed, and the scene continues to prompt until
     * a valid (non-blank) name is entered.
     *
     * <h3>Why parameterized?</h3>
     * <p>
     *     Both {@code ""} (empty string) and {@code "   "} (whitespace-only string) must
     *     be rejected by the same {@link String#isBlank()} guard. The two original
     *     {@code @Test} methods were structurally identical: stub a bad name first, then
     *     a valid name, then the confirmation.  Merging them into one
     *     {@link ParameterizedTest @ParameterizedTest} with
     *     {@link ValueSource @ValueSource(strings = {...})} removes the duplication while
     *     keeping both input variants as separate test invocations.
     * </p>
     *
     * @param blankName an empty or whitespace-only name that should be rejected,
     *                  injected by JUnit 5.
     */
    @ParameterizedTest(name = "name=''{0}'' is rejected as blank and player is re-prompted")
    @ValueSource(strings = {"", "   "})
    @DisplayName("play() rejects blank or empty names and re-prompts for a valid name")
    void testPlayRejectsBlankOrEmptyName(String blankName) {
        logger.debug("Testing play rejects blank name: '{}'", blankName);
        when(mockIO.prompt(anyString())).thenReturn(blankName, "ValidName", "2");

        SceneID result = scene.play(player, mockIO);

        assertEquals(SceneID.FOREST, result,
                "Should transition to FOREST after a valid name follows a blank rejection");
        verify(mockIO).println("Name cannot be blank. Please enter a valid name.");
        assertEquals("ValidName", player.getName(),
                "Player name should be the first non-blank name entered");
    }

    // -------------------------------------------------------------------------
    // Invalid confirmation response
    // -------------------------------------------------------------------------

    /**
     * Verifies that an unrecognised response to the "would you like to change your name?"
     * prompt causes an error message and re-prompts, eventually proceeding when a valid
     * option is supplied.
     */
    @Test
    @DisplayName("play() re-prompts with error on an invalid confirmation response")
    void testPlayInvalidConfirmationResponse() {
        logger.debug("Testing play re-prompts on invalid confirmation");
        when(mockIO.prompt(anyString())).thenReturn("Arthur", "maybe", "2");

        SceneID result = scene.play(player, mockIO);

        assertEquals(SceneID.FOREST, result,
                "Should proceed to FOREST after invalid then valid confirmation");
        verify(mockIO).println("You have entered an invalid response, please try again.");
    }

    // -------------------------------------------------------------------------
    // clearOutput() before the FOREST banner
    // -------------------------------------------------------------------------

    /**
     * Verifies that once the player confirms their name, {@link GameIO#clearOutput()}
     * is called before the "THE ENCHANTED FOREST" banner is printed, so the
     * name-entry dialogue is wiped from view immediately before the game's own
     * narrative begins.
     *
     * <p>
     *     {@link InOrder} verifies the exact sequence: the call is not simply
     *     made at some point during {@code play()} — it must precede the first
     *     banner line.
     * </p>
     */
    @Test
    @DisplayName("play() clears output before printing the FOREST banner")
    void testPlayClearsOutputBeforeForestBanner() {
        logger.debug("Testing play calls clearOutput before FOREST banner");
        when(mockIO.prompt(anyString())).thenReturn("Gandalf", "2");

        scene.play(player, mockIO);

        InOrder inOrder = inOrder(mockIO);
        inOrder.verify(mockIO).clearOutput();
        inOrder.verify(mockIO).println("                THE ENCHANTED FOREST");
    }
}
