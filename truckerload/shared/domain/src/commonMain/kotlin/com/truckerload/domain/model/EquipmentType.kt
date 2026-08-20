package com.truckerload.domain.model

/**
 * Trailer / equipment used on a load. Stored as the enum name in Room.
 * Nullable on [Load] so existing rows and skipped form fields stay valid.
 */
enum class EquipmentType {
    DRY_VAN,
    REEFER,
    FLATBED,
    POWER_ONLY,
    AMAZON_RELAY,
    BOX_TRUCK,
    CARGO_VAN,
    CAR_HAULER,
    OTHER,
    ;

    companion object {
        fun fromStorage(raw: String?): EquipmentType? {
            if (raw.isNullOrBlank()) return null
            return entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
        }
    }
}
