package com.truckerload.domain.model

import com.truckerload.domain.money.Money

/**
 * Broker/shipper cancelled a booked load (TONU: truck ordered, not used).
 *
 * The load stays in the journal. Its rate becomes the TONU pay the driver typed
 * (0 when the broker pays nothing), loaded miles become 0, and deadhead stays as is.
 * The original rate and miles are kept so [undo] restores the load exactly.
 */
object LoadCancellation {

    /** Cancellations this close to pickup (or later) usually pay TONU. */
    const val TONU_WINDOW_MILLIS: Long = 7_200_000L

    /** Amazon Relay's usual TONU. */
    private const val AMAZON_TONU_CENTS = 17_500L

    /** A common broker TONU. */
    private const val BROKER_TONU_CENTS = 22_500L

    /** $10,000: guards against a mistyped extra digit. */
    private const val MAX_TONU_CENTS = 1_000_000L

    /** Common TONU amounts offered as quick picks. The driver can type any amount. */
    val SUGGESTED_TONU: List<Money> = listOf(Money.fromCents(AMAZON_TONU_CENTS), Money.fromCents(BROKER_TONU_CENTS))

    /** Highest TONU accepted from the form. */
    val MAX_TONU: Money = Money.fromCents(MAX_TONU_CENTS)

    /**
     * Marks [load] cancelled with [tonu] pay. Calling it again on a cancelled load only
     * changes the TONU amount; the stored original rate and miles are kept.
     */
    fun cancel(load: Load, tonu: Money, nowMillis: Long): Load {
        val pay = tonu.coerceAtLeast(Money.ZERO)
        if (load.isCancelled) {
            return load.copy(totalRate = pay, totalMiles = 0.0, updatedAt = nowMillis)
        }
        return load.copy(
            isCancelled = true,
            cancelledAt = nowMillis,
            originalRate = load.totalRate,
            originalMiles = load.totalMiles,
            totalRate = pay,
            totalMiles = 0.0,
            updatedAt = nowMillis,
        )
    }

    /** Restores the rate and loaded miles from before [cancel]. Deadhead is untouched. */
    fun undo(load: Load, nowMillis: Long): Load {
        if (!load.isCancelled) return load
        return load.copy(
            isCancelled = false,
            cancelledAt = null,
            totalRate = load.originalRate ?: load.totalRate,
            totalMiles = load.originalMiles ?: load.totalMiles,
            originalRate = null,
            originalMiles = null,
            updatedAt = nowMillis,
        )
    }

    /**
     * True when the cancellation happens within [TONU_WINDOW_MILLIS] of pickup or after it.
     * [pickupMillis] is null when the pickup time is unknown; the driver decides then.
     */
    fun isWithinTonuWindow(pickupMillis: Long?, nowMillis: Long): Boolean =
        pickupMillis != null && nowMillis >= pickupMillis - TONU_WINDOW_MILLIS

    /**
     * Amount the dialog starts with: the current TONU for an already cancelled load,
     * the first suggestion inside the TONU window, otherwise nothing (driver picks).
     */
    fun suggestedTonu(load: Load, pickupMillis: Long?, nowMillis: Long): Money? = when {
        load.isCancelled -> load.totalRate
        isWithinTonuWindow(pickupMillis, nowMillis) -> SUGGESTED_TONU.first()
        else -> null
    }

    /** True when [amount] is an acceptable TONU (0 allowed for "no pay"). */
    fun isValidTonu(amount: Money?): Boolean = amount != null && amount.cents >= 0L && amount <= MAX_TONU

    /**
     * A re-sent rate confirmation for a cancelled load updates what undo restores,
     * but the load stays cancelled with its TONU pay and 0 loaded miles.
     */
    fun carryOver(existing: Load, incoming: Load): Load {
        if (!existing.isCancelled) return incoming
        return incoming.copy(
            isCancelled = true,
            cancelledAt = existing.cancelledAt,
            originalRate = incoming.totalRate,
            originalMiles = incoming.totalMiles,
            totalRate = existing.totalRate,
            totalMiles = 0.0,
        )
    }
}
