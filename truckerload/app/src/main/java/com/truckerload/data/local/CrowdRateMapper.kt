package com.truckerload.data.local

import com.truckerload.data.local.entities.CrowdRateEntity
import com.truckerload.domain.crowd.CrowdRateReport
import com.truckerload.domain.crowd.CrowdRateSource
import com.truckerload.domain.model.EquipmentType

fun CrowdRateEntity.toReport(): CrowdRateReport = CrowdRateReport(
    id = id,
    fromState = fromState,
    toState = toState,
    rpm = rpm,
    rate = rate,
    miles = miles,
    reportedAtMillis = reportedAtMillis,
    source = runCatching { CrowdRateSource.valueOf(source) }.getOrDefault(CrowdRateSource.NETWORK),
    peerLabel = peerLabel,
    equipmentType = EquipmentType.fromStorage(equipmentType),
)

fun CrowdRateReport.toEntity(syncedAtMillis: Long = System.currentTimeMillis()): CrowdRateEntity =
    CrowdRateEntity(
        id = id,
        fromState = fromState,
        toState = toState,
        rpm = rpm,
        rate = rate,
        miles = miles,
        reportedAtMillis = reportedAtMillis,
        source = source.name,
        peerLabel = peerLabel,
        syncedAtMillis = syncedAtMillis,
        equipmentType = equipmentType?.name,
    )
