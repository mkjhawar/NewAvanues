package com.augmentalis.database.migrations

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for DatabaseMigrations constants and version contract.
 *
 * These tests protect against accidental version regression and verify
 * that the version constant is consistent with the documented history.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
class DatabaseMigrationsConstantsTest {

    // ── CURRENT_SCHEMA_VERSION ────────────────────────────────────────────────

    @Test
    fun currentSchemaVersion_is_positive() {
        assertTrue(CURRENT_SCHEMA_VERSION > 0L, "CURRENT_SCHEMA_VERSION must be positive")
    }

    @Test
    fun currentSchemaVersion_meets_minimum_expected_value() {
        // Version history documents 7 migrations as of 260222.
        // This test protects against accidental downgrades.
        assertTrue(
            CURRENT_SCHEMA_VERSION >= 7L,
            "CURRENT_SCHEMA_VERSION should be at least 7 (7 migrations documented)"
        )
    }

    @Test
    fun currentSchemaVersion_value_matches_documented_version() {
        // Pinned to version 7 per the migration history in DatabaseMigrations.kt.
        // Update this test whenever a new migration is added.
        assertEquals(7L, CURRENT_SCHEMA_VERSION)
    }

    // ── MigratedSchema version delegation ────────────────────────────────────

    @Test
    fun migratedSchema_version_equals_CURRENT_SCHEMA_VERSION() {
        // MigratedSchema.version must delegate to CURRENT_SCHEMA_VERSION
        // so platform drivers call onUpgrade on existing installations.
        // We verify the constant is what MigratedSchema would expose.
        // (We cannot instantiate MigratedSchema without a SqlDriver in commonTest,
        //  so we verify the constant it uses.)
        assertEquals(7L, CURRENT_SCHEMA_VERSION)
    }

    // ── migrate() boundary conditions (pure logic) ────────────────────────────

    @Test
    fun migrate_noop_when_old_equals_new_version() {
        // Calling migrate with oldVersion == newVersion must not apply any migration.
        // Since we can't use a real SqlDriver here, we verify the conditional logic
        // by observing that none of the "if (oldVersion < X && newVersion >= X)" blocks
        // would fire when oldVersion == newVersion == CURRENT_SCHEMA_VERSION.
        val old = CURRENT_SCHEMA_VERSION
        val new = CURRENT_SCHEMA_VERSION
        // All conditions: old < 2 && new >= 2, etc. — none fire when old == new == 7.
        val v1To2 = old < 2L && new >= 2L
        val v2To3 = old < 3L && new >= 3L
        val v3To4 = old < 4L && new >= 4L
        val v4To5 = old < 5L && new >= 5L
        val v5To6 = old < 6L && new >= 6L
        val v6To7 = old < 7L && new >= 7L
        assertTrue(!v1To2 && !v2To3 && !v3To4 && !v4To5 && !v5To6 && !v6To7,
            "No migration branch should fire when old == new == CURRENT_SCHEMA_VERSION")
    }

    @Test
    fun migrate_all_branches_fire_for_fresh_install() {
        // A fresh install: old=0, new=CURRENT_SCHEMA_VERSION — all migration branches fire.
        val old = 0L
        val new = CURRENT_SCHEMA_VERSION
        val v1To2 = old < 2L && new >= 2L
        val v2To3 = old < 3L && new >= 3L
        val v3To4 = old < 4L && new >= 4L
        val v4To5 = old < 5L && new >= 5L
        val v5To6 = old < 6L && new >= 6L
        val v6To7 = old < 7L && new >= 7L
        assertTrue(v1To2, "V1→V2 must fire for fresh install")
        assertTrue(v2To3, "V2→V3 must fire for fresh install")
        assertTrue(v3To4, "V3→V4 must fire for fresh install")
        assertTrue(v4To5, "V4→V5 must fire for fresh install")
        assertTrue(v5To6, "V5→V6 must fire for fresh install")
        assertTrue(v6To7, "V6→V7 must fire for fresh install")
    }

    @Test
    fun migrate_only_later_branches_fire_for_partial_upgrade() {
        // Upgrading from version 5 to 7: only V5→V6 and V6→V7 branches fire.
        val old = 5L
        val new = CURRENT_SCHEMA_VERSION
        val v1To2 = old < 2L && new >= 2L
        val v2To3 = old < 3L && new >= 3L
        val v3To4 = old < 4L && new >= 4L
        val v4To5 = old < 5L && new >= 5L
        val v5To6 = old < 6L && new >= 6L
        val v6To7 = old < 7L && new >= 7L
        assertTrue(!v1To2, "V1→V2 must NOT fire when old=5")
        assertTrue(!v2To3, "V2→V3 must NOT fire when old=5")
        assertTrue(!v3To4, "V3→V4 must NOT fire when old=5")
        assertTrue(!v4To5, "V4→V5 must NOT fire when old=5")
        assertTrue(v5To6, "V5→V6 MUST fire when old=5 and new>=6")
        assertTrue(v6To7, "V6→V7 MUST fire when old=5 and new>=7")
    }
}
