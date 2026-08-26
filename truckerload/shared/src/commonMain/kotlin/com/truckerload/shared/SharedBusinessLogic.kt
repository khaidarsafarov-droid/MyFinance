package com.truckerload.shared

import com.truckerload.contract.PushPlatforms
import com.truckerload.domain.auth.AuthProvider
import com.truckerload.domain.goal.GoalMoneyMath
import com.truckerload.domain.platform.PlatformTime

/**
 * Stable Kotlin entry point the iOS client (`ios/`) imports as `TruckerLoadShared`.
 *
 * Keep this façade free of `android.*` / UI. Android continues to call
 * [GoalMoneyMath] directly; Swift should go through this object so the
 * framework surface stays small.
 */
object SharedBusinessLogic {
    fun iosPushPlatform(): String = PushPlatforms.IOS

    fun roundMoney(value: Double): Double = GoalMoneyMath.roundMoney(value)

    fun dailyTarget(goal: Double, totalGross: Double, daysRemaining: Int): Double =
        GoalMoneyMath.dailyTarget(goal, totalGross, daysRemaining)

    fun expectedGrossByNow(goal: Double, daysActive: Int): Double =
        GoalMoneyMath.expectedGrossByNow(goal, daysActive)

    fun epochMillis(): Long = PlatformTime.epochMillis()

    fun reservedAuthProviders(): List<String> = AuthProvider.entries.map { it.name }
}
