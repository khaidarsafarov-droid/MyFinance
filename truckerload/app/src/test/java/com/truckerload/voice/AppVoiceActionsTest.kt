package com.truckerload.voice

import com.truckerload.presentation.navigation.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVoiceActionsTest {

    @Test
    fun spokenLabelsOpenRealScreens() {
        assertEquals(Routes.ADD_LOAD, screen("добавить груз"))
        assertEquals(Routes.ADD_LOAD, screen("add load"))
        assertEquals(Routes.STATS, screen("цель недели"))
        assertEquals(Routes.STATS, screen("goal"))
        assertEquals(Routes.ANALYTICS, screen("отчёты"))
        assertEquals(Routes.ANALYTICS, screen("reports"))
        assertEquals(Routes.MAP, screen("открыть карту"))
        assertEquals(Routes.MAP, screen("open map"))
        assertEquals(Routes.MAP, screen("карта"))
        assertEquals(Routes.HOME, screen("грузы"))
        assertEquals(Routes.HOME, screen("loads"))
        assertEquals(Routes.ADD_DIESEL, screen("добавить дизель"))
        assertEquals(Routes.MAINTENANCE, screen("обслуживание"))
        assertEquals(Routes.TAX_TRACKER, screen("налоги"))
        assertEquals(Routes.TAX_TRACKER, screen("tax"))
        assertEquals(Routes.MAINTENANCE, screen("то"))
        assertEquals(Routes.SETTINGS, screen("настройки"))
        assertEquals(Routes.SCANNER, screen("сканер"))
        assertEquals(Routes.FINANCIAL_ADVISOR, screen("финансовый советник"))
        assertEquals(Routes.VOICE_ASSISTANT, screen("голосовой ассистент"))
        assertEquals(Routes.VOICE_ASSISTANT, screen("voice assistant"))
        assertEquals(Routes.PHOTO_GALLERY, screen("галерея фото"))
        assertEquals(Routes.PROFILE, screen("профиль"))
        assertEquals(Routes.IMPROVE, screen("что улучшить"))
        assertEquals(Routes.IMPROVE, screen("suggest improvement"))
    }

    @Test
    fun everyInAppLabelMapsToItsScreen() {
        AppVoiceActions.screens.forEach { (route, phrases) ->
            phrases.forEach { phrase ->
                assertEquals("phrase=$phrase", route, screen(phrase))
            }
        }
    }

    @Test
    fun deepLinkPathsAreAppRoutes() {
        val add = AppVoiceActions.parseUri("truckerload://app/add_load") as AppVoiceAction.OpenScreen
        assertEquals(Routes.ADD_LOAD, add.route)
        val feature = AppVoiceActions.parseUri("truckerload://assistant/open?featureName=цель")
            as AppVoiceAction.OpenScreen
        assertEquals(Routes.STATS, feature.route)
        val diesel = AppVoiceActions.parseUri("truckerload://assistant/add_diesel?amount=80")
        assertTrue(diesel is AppVoiceAction.AddDiesel)
    }

    private fun screen(spoken: String): String {
        val action = AppVoiceActions.matchSpoken(spoken)
        assertTrue("expected screen for '$spoken', got $action", action is AppVoiceAction.OpenScreen)
        return (action as AppVoiceAction.OpenScreen).route
    }
}
