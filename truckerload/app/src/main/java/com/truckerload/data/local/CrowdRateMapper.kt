package com.truckerload.data.local

import com.truckerload.data.local.entities.CrowdRateEntity
import com.truckerload.domain.crowd.CrowdRateReport
import com.truckerload.domain.crowd.CrowdRateSource
import com.truckerload.domain.model.EquipmentType
import com.truckerload.domain.money.Money

fun CrowdRateEntity.toReport(): CrowdRateReport = CrowdRateReport(
    id = id,
    fromState = fromState,
    toState = toState,
    rpm = Money.fromCents(rpm),
    rate = Money.fromCents(rate),
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
        rpm = rpm.cents,
        rate = rate.cents,
        miles = miles,
        reportedAtMillis = reportedAtMillis,
        source = source.name,
        peerLabel = peerLabel,
        syncedAtMillis = syncedAtMillis,
        equipmentType = equipmentType?.name,
    )
