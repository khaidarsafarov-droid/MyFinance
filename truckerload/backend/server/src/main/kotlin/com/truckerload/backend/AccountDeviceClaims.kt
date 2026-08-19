package com.truckerload.backend

/**
 * One registered phone and one registered tablet per account.
 * Same [deviceId] may refresh its own slot; a different device of the same
 * form factor is rejected.
 */
internal object AccountDeviceClaims {
    fun decide(
        self: AccountDeviceRecord?,
        occupant: AccountDeviceRecord?,
        userId: java.util.UUID,
        deviceId: String,
        formFactor: String,
        now: Long,
    ): DeviceClaimResult {
        if (self != null && self.formFactor == formFactor) {
            return DeviceClaimResult.Claimed(self.copy(lastSeenAt = now))
        }
        if (occupant != null && occupant.deviceId != deviceId) {
            return DeviceClaimResult.SlotTaken(formFactor, occupant.deviceId)
        }
        val registeredAt = self?.registeredAt ?: now
        return DeviceClaimResult.Claimed(
            AccountDeviceRecord(
                userId = userId,
                deviceId = deviceId,
                formFactor = formFactor,
                registeredAt = registeredAt,
                lastSeenAt = now,
            ),
        )
    }
}
