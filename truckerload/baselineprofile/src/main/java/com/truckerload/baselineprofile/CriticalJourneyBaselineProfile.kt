package com.truckerload.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Critical journeys for the shipped baseline profile.
 *
 * A fresh install is signed out until the public first-run Skip (and, if it
 * appears, Telegram "Skip for now") runs. Those are the same controls a user
 * sees. If a later gate has no public skip, the remaining taps are left out
 * and the startup trace is still kept. No debug login or hidden intent.
 */
@RunWith(AndroidJUnit4::class)
class CriticalJourneyBaselineProfile {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun criticalJourneys() {
        rule.collect(
            packageName = TARGET_PACKAGE,
            includeInStartupProfile = true,
            maxIterations = 5,
        ) {
            pressHome()
            startActivityAndWait()
            if (device.reachHome()) {
                device.scrollJournal()
                device.openLoadDetailAndBack()
                device.switchTabs()
                device.openAddLoad()
            }
        }
    }

    private fun UiDevice.reachHome(): Boolean {
        val deadline = System.currentTimeMillis() + GATE_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            dismissBlockers()
            if (seen(ADD_LOAD)) return true
            if (seen(FIRST_RUN)) {
                tap(FIRST_RUN_SKIP)
                continue
            }
            if (seen(TELEGRAM_SKIP)) {
                tap(TELEGRAM_SKIP)
                continue
            }
            if (seen(PROFILE_SETUP)) return false
            Thread.sleep(400)
        }
        return seen(ADD_LOAD)
    }

    private fun UiDevice.dismissBlockers() {
        tap(PREMIUM_DISMISS)
        tap(NOTIFICATION_DISMISS)
    }

    private fun UiDevice.scrollJournal() {
        val list = findObject(By.scrollable(true))
        if (list != null) {
            list.scroll(Direction.DOWN, 0.8f)
            waitForIdle()
            list.scroll(Direction.UP, 0.6f)
            waitForIdle()
            return
        }
        swipe(displayWidth / 2, (displayHeight * 0.75f).toInt(), displayWidth / 2, (displayHeight * 0.3f).toInt(), 30)
        waitForIdle()
    }

    private fun UiDevice.openLoadDetailAndBack() {
        val card = findObject(By.textContains(" mi"))
            ?: findObject(By.textContains("миль"))
            ?: return
        card.click()
        waitForIdle()
        pressBack()
        waitForIdle()
    }

    private fun UiDevice.switchTabs() {
        tap(HOME_TAB)
        tap(GOAL_TAB)
        tap(PROFILE_TAB)
        if (!tap(SETTINGS)) {
            tap(MENU)
            tap(SETTINGS)
        }
        tap(HOME_TAB)
    }

    private fun UiDevice.openAddLoad() {
        if (!tap(ADD_LOAD)) return
        waitForIdle()
        pressBack()
        waitForIdle()
    }

    private fun UiDevice.seen(labels: Array<String>): Boolean =
        labels.any { label ->
            findObject(By.text(label)) != null || findObject(By.desc(label)) != null
        }

    private fun UiDevice.tap(labels: Array<String>): Boolean {
        val target = labels.firstNotNullOfOrNull { label ->
            findObject(By.text(label)) ?: findObject(By.desc(label))
        } ?: return false
        target.click()
        waitForIdle()
        return true
    }

    private companion object {
        const val TARGET_PACKAGE = "com.truckorig"
        const val GATE_TIMEOUT_MS = 25_000L

        val ADD_LOAD = arrayOf("Add load", "Добавить груз", "Agregar carga")
        val FIRST_RUN = arrayOf("What’s your name?", "Как вас зовут?", "¿Cómo te llamas?")
        val FIRST_RUN_SKIP = arrayOf("Skip", "Пропустить", "Omitir")
        val TELEGRAM_SKIP = arrayOf("Skip for now", "Пропустить сейчас", "Omitir por ahora")
        val PROFILE_SETUP = arrayOf("Profile setup", "Настройка профиля", "Configuración del perfil")
        val PREMIUM_DISMISS = arrayOf(
            "Continue with the free version",
            "Остаться на бесплатной версии",
            "Seguir con la versión gratis",
        )
        val NOTIFICATION_DISMISS = arrayOf("Don't allow", "Don’t allow", "Не разрешать", "No permitir")
        val HOME_TAB = arrayOf("Home", "Главная", "Inicio")
        val GOAL_TAB = arrayOf("Goal", "Цель", "Meta", "Stats", "Статистика", "Estadísticas")
        val PROFILE_TAB = arrayOf("Profile", "Профиль", "Perfil")
        val SETTINGS = arrayOf("Settings", "Настройки", "Ajustes")
        val MENU = arrayOf("Menu", "Меню", "Menú")
    }
}
