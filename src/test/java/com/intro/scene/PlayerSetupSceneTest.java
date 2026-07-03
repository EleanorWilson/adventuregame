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
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerSetupSceneTest {

    private static final Logger logger = LogManager.getLogger(PlayerSetupSceneTest.class);

    @Mock
    private GameIO io;

    @Mock
    private Player player;

    @Spy
    private Player playerSpy;

    private PlayerSetupScene scene;

    @BeforeEach
    void setUp() {
        logger.debug("Setting up PlayerSetupSceneTest");
        scene = new PlayerSetupScene();
    }

    @Test
    @DisplayName("getID() returns correct enum")
    void testGetIDReturnsEnum() {
        logger.debug("Testing getID() returns correct enum");
        assertEquals(SceneID.PLAYER_SETUP, scene.getID(), "getID() should return SceneID.PLAYER_SETUP");
    }

    @Test
    @DisplayName("play() returns FOREST when player confirms name")
    void testPlayReturnsForestAfterNameConfirm() {
        logger.debug("Testing play() returns SceneID.FOREST once player has confirmed a valid name");
        when(io.prompt(anyString())).thenReturn("Player", "2");
        SceneID result = scene.play(player, io);
        assertEquals(SceneID.FOREST, result, "Should return FOREST on '2' input after player confirms name");
    }

    @Test
    @DisplayName("play() stores confirmed name on Player object")
    void testPlaySetsConfirmedName() {
        logger.debug("Testing play() sets player name once confirmed");
        playerSpy = new Player();
        when(io.prompt(anyString())).thenReturn("Player", "2");
        scene.play(playerSpy, io);
        assertEquals("Player", playerSpy.getName(), "The player name should be set to the user's inputted String");
    }

    @Test
    @DisplayName("play() echoes character name input back to player")
    void testPlayEchoesNameInput() {
        logger.debug("Testing play() sets player name once confirmed");
        String testName = "Player";
        when(io.prompt(anyString())).thenReturn(testName, "2");
        scene.play(player, io);
        verify(io).println(contains(testName));
    }

    @Test
    @DisplayName("play() returns FOREST when player confirms name")
    void testPlayAllowsNameChange() {
        logger.debug("Testing play() allows name change and sets player name");
        playerSpy = new Player();
        when(io.prompt(anyString())).thenReturn("OldName", "1", "NewName", "2");
        SceneID result = scene.play(playerSpy, io);
        assertEquals("NewName", playerSpy.getName(), "Player name should update to newest name");
        assertEquals(SceneID.FOREST, result, "play() should return SceneID.FOREST once name confirmed");
    }

    @ParameterizedTest(name = "name: ''{0}'' is blank or empty, player re-prompted to enter valid name")
    @ValueSource(strings = {"", " ", "    "})
    void testPlayHandlesBlankEmptyNames(String input) {
        logger.debug("Testing play recognises name: '{}' as invalid, re-prompts to enter valid name", input);
        playerSpy = new Player();
        when(io.prompt(anyString())).thenReturn(input, "validName", "2");
        SceneID result = scene.play(playerSpy, io);
        verify(io).println(contains("cannot be blank"));
        assertEquals(SceneID.FOREST, result, "Once valid name chosen, play() should return SceneID.FOREST");
        assertEquals("validName", playerSpy.getName(), "Player name should be set to the last valid input");
    }

    @Test
    @DisplayName("play() when name is null, re-prompts player to input new name")
    void testPlayRepromptsOnNullName() {
        logger.debug("Testing play() re-prompts player to enter new name, when first name was null");
        when(io.prompt(anyString())).thenReturn(null, "Name", "2");
        SceneID result = scene.play(player, io);
        verify(io).println(contains("cannot be blank"));
        assertEquals(SceneID.FOREST, result, "Once valid name chosen, play(() should return SceneID.FOREST");
    }

    @Test
    @DisplayName("play() show error on invalid confirm name response")
    void testPlayInvalidConfirmNameResponse() {
        logger.debug("Testing play re-prompts on invalid confirm name response");
        when(io.prompt(anyString())).thenReturn("Name", "invalid", "2");
        SceneID result = scene.play(player, io);
        assertEquals(SceneID.FOREST, result, "play() should return SceneID.FOREST after valid confirm name response");
        verify(io).println(contains("invalid response"));
    }

    @Test
    @DisplayName("play() re-prompts confirm name input if confirm name empty")
    void testPlayRepromptsConfirmNameIfEmpty() {
        logger.debug("Testing play() re-prompts player to confirm name choice if the confirm name input is empty");
        when(io.prompt(anyString())).thenReturn("Player", "", "2");
        SceneID result = scene.play(player, io);
        verify(io).println(contains("invalid response"));
        assertEquals(SceneID.FOREST, result, "Once valid name confirmation choice chosen, play() should return SceneID.FOREST");
    }

    @ParameterizedTest(name = "Choice: ''{0}'' accepted as valid, returns FOREST after name change")
    @ValueSource(strings = {"2", "n", "no", "No"})
    void testPlayReturnsForestAfterValidNameConfirm(String input) {
        logger.debug("Testing play() recognises: {} as valid confirm name choice", input);
        when(io.prompt(anyString())).thenReturn("Player", input);
        SceneID result = scene.play(player, io);
        assertEquals(SceneID.FOREST, result, "Should return FOREST after player confirms name");
    }

    @ParameterizedTest(name = "Choice: ''{0}'' accepted as valid, returns FOREST after name change")
    @ValueSource(strings = {"1", "y", "yes", "Yes"})
    void testPlayReturnsForestAfterValidNameChange(String input) {
        logger.debug("Testing play() recognises: {} as valid confirm name choice", input);
        when(io.prompt(anyString())).thenReturn("OldName", input, "NewName", "2");
        SceneID result = scene.play(player, io);
        assertEquals(SceneID.FOREST, result, "Should return FOREST after player confirms name");
    }
}
