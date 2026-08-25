package com.truckerload.domain.tax

/**
 * Manual per-diem calendar edits on top of load-derived on-duty days.
 *
 * [included] days are forced on; [excluded] days are forced off.
 * New loads still add their days unless the driver already turned that date off.
 */
data class PerDiemOverrideSnapshot(
    val included: Set<String> = emptySet(),
    val excluded: Set<String> = emptySet(),
)

enum class PerDiemOverrideMutation {
    UPSERT_INCLUDED,
    UPSERT_EXCLUDED,
    DELETE,
}

object PerDiemDayOverrides {

    fun apply(autoDates: Set<String>, snapshot: PerDiemOverrideSnapshot): Set<String> =
        (autoDates - snapshot.excluded) + snapshot.included

    fun mutationForToggle(
        isoDate: String,
        autoDates: Set<String>,
        effectiveDates: Set<String>,
    ): PerDiemOverrideMutation {
        val on = isoDate in effectiveDates
        val auto = isoDate in autoDates
        return when {
            on && auto -> PerDiemOverrideMutation.UPSERT_EXCLUDED
            on && !auto -> PerDiemOverrideMutation.DELETE
            !on && auto -> PerDiemOverrideMutation.DELETE
            else -> PerDiemOverrideMutation.UPSERT_INCLUDED
        }
    }
}
