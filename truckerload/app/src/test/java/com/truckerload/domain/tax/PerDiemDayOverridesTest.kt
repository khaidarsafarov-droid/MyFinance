package com.truckerload.domain.tax

import org.junit.Assert.assertEquals
import org.junit.Test

class PerDiemDayOverridesTest {

    @Test
    fun apply_addsIncluded_andRemovesExcluded() {
        val auto = setOf("2026-08-01", "2026-08-02")
        val snapshot = PerDiemOverrideSnapshot(
            included = setOf("2026-08-10"),
            excluded = setOf("2026-08-01"),
        )
        assertEquals(
            setOf("2026-08-02", "2026-08-10"),
            PerDiemDayOverrides.apply(auto, snapshot),
        )
    }

    @Test
    fun toggle_autoOn_excludes() {
        assertEquals(
            PerDiemOverrideMutation.UPSERT_EXCLUDED,
            PerDiemDayOverrides.mutationForToggle(
                isoDate = "2026-08-01",
                autoDates = setOf("2026-08-01"),
                effectiveDates = setOf("2026-08-01"),
            ),
        )
    }

    @Test
    fun toggle_excludedAuto_deletesOverride() {
        assertEquals(
            PerDiemOverrideMutation.DELETE,
            PerDiemDayOverrides.mutationForToggle(
                isoDate = "2026-08-01",
                autoDates = setOf("2026-08-01"),
                effectiveDates = emptySet(),
            ),
        )
    }

    @Test
    fun toggle_manualAdd_deletesOverride() {
        assertEquals(
            PerDiemOverrideMutation.DELETE,
            PerDiemDayOverrides.mutationForToggle(
                isoDate = "2026-08-10",
                autoDates = emptySet(),
                effectiveDates = setOf("2026-08-10"),
            ),
        )
    }

    @Test
    fun toggle_emptyDay_includes() {
        assertEquals(
            PerDiemOverrideMutation.UPSERT_INCLUDED,
            PerDiemDayOverrides.mutationForToggle(
                isoDate = "2026-08-24",
                autoDates = emptySet(),
                effectiveDates = emptySet(),
            ),
        )
    }
}
