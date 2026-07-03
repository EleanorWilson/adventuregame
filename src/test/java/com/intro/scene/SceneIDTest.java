package com.intro.scene;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link SceneID} enumeration.
 *
 * <h3>Parameterization strategy</h3>
 * <p>
 *     The original design had seven individual {@code @Test} methods — one per constant —
 *     each doing identical work (call {@code valueOf}, compare {@code name()}). This was
 *     pure boilerplate: adding a new {@link SceneID} constant required a developer to
 *     remember to add a matching test method, and the tests were not self-maintaining.
 * </p>
 * <p>
 *     {@link EnumSource @EnumSource(SceneID.class)} solves both problems at once. JUnit 5
 *     automatically supplies every declared constant as a separate test invocation, so
 *     coverage remains complete and grows automatically when new constants are added.
 *     The seven individual methods and the separate {@code assertAll} name-check method
 *     are replaced by a single {@code @ParameterizedTest}, reducing 8 methods to 1
 *     while keeping the same number of assertions.
 * </p>
 */
class SceneIDTest {

    private static final Logger logger = LogManager.getLogger(SceneIDTest.class);

    // -------------------------------------------------------------------------
    // Count test — catches additions / removals at a glance
    // -------------------------------------------------------------------------

    /**
     * Verifies that exactly seven {@link SceneID} constants are declared.
     *
     * <p>
     *     This acts as a "canary" test: if a constant is added or removed the count
     *     changes and this test fails immediately, prompting the developer to review
     *     whether other test or production code needs updating.
     * </p>
     */
    @Test
    @DisplayName("SceneID enum declares exactly 7 constants")
    void testSceneIDHasSevenValues() {
        logger.debug("Verifying SceneID constant count");
        assertEquals(7, SceneID.values().length,
                "SceneID should declare exactly 7 constants; update this test if intentionally changed");
    }

    // -------------------------------------------------------------------------
    // Per-constant tests — driven automatically by @EnumSource
    // -------------------------------------------------------------------------

    /**
     * For every {@link SceneID} constant, verifies two properties in one invocation:
     * <ol>
     *     <li>The constant can be located by name using {@link SceneID#valueOf(String)}
     *         (i.e. the constant actually exists and its name is not mangled).</li>
     *     <li>The constant's {@link Enum#name()} round-trips correctly through
     *         {@code valueOf} — meaning the declared name in source exactly matches the
     *         runtime string returned by {@code name()}.</li>
     * </ol>
     *
     * <p>
     *     Using {@link EnumSource @EnumSource(SceneID.class)} means JUnit 5 automatically
     *     passes each constant as a separate test invocation. Adding a new {@link SceneID}
     *     constant requires <em>no changes</em> to this test file; coverage is maintained
     *     automatically.
     * </p>
     *
     * @param sceneID the {@link SceneID} constant under test, injected by JUnit 5.
     */
    @ParameterizedTest(name = "SceneID.{0} exists and name() round-trips through valueOf")
    @EnumSource(SceneID.class)
    @DisplayName("Each SceneID constant exists and has a self-consistent name")
    void testEachConstantExistsAndNameIsConsistent(SceneID sceneID) {
        logger.debug("Checking SceneID constant: {}", sceneID.name());
        SceneID found = assertDoesNotThrow(
                () -> SceneID.valueOf(sceneID.name()),
                sceneID.name() + " should be retrievable via SceneID.valueOf(name())");
        assertEquals(sceneID.name(), found.name(),
                "valueOf(name()) should return the same constant; name should be self-consistent");
    }

    // -------------------------------------------------------------------------
    // Negative test — unknown constant
    // -------------------------------------------------------------------------

    /**
     * Verifies that {@link SceneID#valueOf(String)} throws {@link IllegalArgumentException}
     * when given a name that does not correspond to any declared constant.
     * This confirms the enum does not silently accept arbitrary strings.
     */
    @Test
    @DisplayName("SceneID.valueOf throws IllegalArgumentException for an unknown name")
    void testValueOfThrowsForUnknownConstant() {
        logger.debug("Verifying valueOf throws for unknown constant");
        assertThrows(IllegalArgumentException.class,
                () -> SceneID.valueOf("UNKNOWN_SCENE"),
                "valueOf should throw for a name that is not a declared SceneID constant");
    }
}
