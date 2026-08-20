package com.truckerload.presentation.screens.add

import com.truckerload.R
import com.truckerload.domain.model.EquipmentType

fun EquipmentType.labelRes(): Int = when (this) {
    EquipmentType.DRY_VAN -> R.string.equipment_dry_van
    EquipmentType.REEFER -> R.string.equipment_reefer
    EquipmentType.FLATBED -> R.string.equipment_flatbed
    EquipmentType.POWER_ONLY -> R.string.equipment_power_only
    EquipmentType.AMAZON_RELAY -> R.string.equipment_amazon_relay
    EquipmentType.BOX_TRUCK -> R.string.equipment_box_truck
    EquipmentType.CARGO_VAN -> R.string.equipment_cargo_van
    EquipmentType.CAR_HAULER -> R.string.equipment_car_hauler
    EquipmentType.OTHER -> R.string.equipment_other
}

fun EquipmentType.emoji(): String = when (this) {
    EquipmentType.DRY_VAN -> "📦"
    EquipmentType.REEFER -> "❄️"
    EquipmentType.FLATBED -> "🪵"
    EquipmentType.POWER_ONLY -> "🚛"
    EquipmentType.AMAZON_RELAY -> "🧡"
    EquipmentType.BOX_TRUCK -> "🚚"
    EquipmentType.CARGO_VAN -> "🚐"
    EquipmentType.CAR_HAULER -> "🚗"
    EquipmentType.OTHER -> "•"
}
