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
        assertEquals(Routes.FRIENDS_LIVE, screen("карта друзей"))
        assertEquals(Routes.FRIENDS_LIVE, screen("открой карту друзей"))
        assertEquals(Routes.FRIENDS_LIVE, screen("friends live"))
        assertEquals(Routes.MAP, screen("открыть карту"))
        assertEquals(Routes.MAP, screen("open map"))
        assertEquals(Routes.MAP, screen("карта"))
        assertEquals(Routes.HOME, screen("грузы"))
        assertEquals(Routes.HOME, screen("loads"))
        assertEquals(Routes.COMMUNITY, screen("сообщество"))
        assertEquals(Routes.COMMUNITY, screen("таблица лидеров"))
        assertEquals(Routes.COMMUNITY, screen("челленджи"))
        assertEquals(Routes.VOICE_ROOMS, screen("голосовые комнаты"))
        assertEquals(Routes.ADD_DIESEL, screen("добавить дизель"))
        assertEquals(Routes.MAINTENANCE, screen("обслуживание"))
        assertEquals(Routes.MAINTENANCE, screen("то"))
        assertEquals(Routes.SETTINGS, screen("настройки"))
        assertEquals(Routes.SCANNER, screen("сканер"))
        assertEquals(Routes.FINANCIAL_ADVISOR, screen("финансовый советник"))
        assertEquals(Routes.VOICE_ASSISTANT, screen("голосовой ассистент"))
        assertEquals(Routes.VOICE_ASSISTANT, screen("voice assistant"))
        assertEquals(Routes.PHOTO_GALLERY, screen("галерея фото"))
        assertEquals(Routes.STATUS, screen("статусы"))
        assertEquals(Routes.GROUPS, screen("группы"))
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
    fun addFriendOpensCommunityWhereTheFormLives() {
        assertEquals(Routes.COMMUNITY, screen("добавить друга"))
        assertEquals(Routes.COMMUNITY, screen("добавить друзей"))
        assertEquals(Routes.COMMUNITY, screen("add friends"))
    }

    @Test
    fun chatCallAndMessageUseFriendsNotAGenericSearch() {
        val chat = AppVoiceActions.matchSpoken("чат с Anna") as AppVoiceAction.ChatWithFriend
        assertEquals("anna", AppVoiceActions.normalize(chat.peerQuery))
        val call = AppVoiceActions.matchSpoken("позвони Ivan") as AppVoiceAction.CallFriend
        assertEquals("ivan", AppVoiceActions.normalize(call.peerQuery))
        val msg = AppVoiceActions.matchSpoken("напиши Nick on the yard") as AppVoiceAction.MessageFriend
        assertEquals("nick", AppVoiceActions.normalize(msg.peerQuery))
        assertEquals("on the yard", msg.text)
    }

    @Test
    fun deepLinkPathsAreAppRoutes() {
        val add = AppVoiceActions.parseUri("truckerload://app/add_load") as AppVoiceAction.OpenScreen
        assertEquals(Routes.ADD_LOAD, add.route)
        val chat = AppVoiceActions.parseUri("truckerload://assistant/chat?peer=Nick_1")
            as AppVoiceAction.ChatWithFriend
        assertEquals("Nick_1", chat.peerQuery)
        val feature = AppVoiceActions.parseUri("truckerload://assistant/open?featureName=цель")
            as AppVoiceAction.OpenScreen
        assertEquals(Routes.STATS, feature.route)
        val diesel = AppVoiceActions.parseUri("truckerload://assistant/add_diesel?amount=80")
        assertTrue(diesel is AppVoiceAction.AddDiesel)
    }

    @Test
    fun peerMatcherDisambiguatesDrivers() {
        val peers = listOf(
            VoicePeerRef("1", "@Anna"),
            VoicePeerRef("2", "Ivan Petrov"),
            VoicePeerRef("3", "Ivan S"),
        )
        val unique = AppVoiceActions.matchPeers("anna", peers) as VoicePeerMatch.Unique
        assertEquals("1", unique.peer.id)
        val ambiguous = AppVoiceActions.matchPeers("ivan", peers) as VoicePeerMatch.Ambiguous
        assertEquals(2, ambiguous.candidates.size)
        assertTrue(AppVoiceActions.matchPeers("missing", peers) is VoicePeerMatch.None)
    }

    private fun screen(spoken: String): String {
        val action = AppVoiceActions.matchSpoken(spoken)
        assertTrue("expected screen for '$spoken', got $action", action is AppVoiceAction.OpenScreen)
        return (action as AppVoiceAction.OpenScreen).route
    }
}
