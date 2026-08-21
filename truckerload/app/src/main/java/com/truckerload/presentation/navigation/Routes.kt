package com.truckerload.presentation.navigation

import android.net.Uri

/**
 * Central navigation contract for Compose destinations and deep-link route builders.
 * Constants define route patterns; helper functions URL-encode path segments before navigation.
 */
object Routes {
    const val HOME = "home"
    const val STATS = "stats"
    const val ANALYTICS = "analytics"
    const val COMMUNITY = "community"
    const val PROFILE = "profile"
    const val PROFILE_EDIT = "profile_edit"
    const val PROFILE_SETUP = "profile_setup"
    const val PROFILE_PEER = "profile_peer/{peerId}"
    const val SOCIAL_CHAT = "social_chat/{chatId}"
    const val ADVANCED_STATS = "advanced_stats"
    const val MAP = "map"
    const val LOAD_DETAIL = "load_detail/{loadId}"
    const val ADD_LOAD = "add_load"
    const val EDIT_LOAD = "edit_load/{loadId}?focusFinish={focusFinish}"
    const val ADD_PAYCHECK = "add_paycheck"
    const val ADD_DIESEL = "add_diesel"
    const val MAINTENANCE = "maintenance"
    const val FINANCIAL_ADVISOR = "financial_advisor"
    const val VOICE_ASSISTANT = "voice_assistant"
    const val SETTINGS = "settings"
    const val PRIVACY_SETTINGS = "privacy_settings"
    const val ABOUT = "about"
    const val FRIENDS_LIVE = "friends_live"
    const val CAMERA = "camera"
    const val CAMERA_FOR_LOAD = "camera_load/{loadId}/{tripId}/{loadDate}"
    const val SCANNER = "scanner"
    const val SCANNER_FOR_LOAD = "scanner_load/{loadId}/{tripId}/{loadDate}"
    /** Widget camera/scan: pick one of the last loads, then open attached capture. */
    const val ATTACH_PICK = "attach_pick/{mode}"
    const val SCAN_GALLERY = "scan_gallery"
    const val PHOTO_GALLERY = "photo_gallery"
    const val PHOTO_DETAIL = "photo_detail/{photoId}"

    fun loadDetail(loadId: String) = "load_detail/${encodePathSegment(loadId)}"
    fun editLoad(loadId: String, focusFinish: Boolean = false) =
        "edit_load/${encodePathSegment(loadId)}?focusFinish=$focusFinish"
    fun photoDetail(photoId: String) = "photo_detail/${encodePathSegment(photoId)}"
    fun socialChat(chatId: String) = "social_chat/${encodePathSegment(chatId)}"
    fun cameraForLoad(loadId: String, tripId: String, loadDate: String): String {
        return "camera_load/${encodePathSegment(loadId)}/${encodePathSegment(tripId)}/${encodePathSegment(loadDate)}"
    }

    fun scannerForLoad(loadId: String, tripId: String, loadDate: String): String {
        return "scanner_load/${encodePathSegment(loadId)}/${encodePathSegment(tripId)}/${encodePathSegment(loadDate)}"
    }

    fun attachPick(mode: String): String = "attach_pick/${encodePathSegment(mode)}"

    private fun encodePathSegment(value: String): String =
        Uri.encode(value.ifBlank { "_" }) ?: "_"

    const val VOICE_ROOMS = "voice_rooms"
    const val VOICE_ROOM = "voice_room/{roomId}"
    const val CALL = "call/{callId}"
    const val STATUS = "status"
    const val GROUPS = "groups"
    const val GROUP_DETAIL = "group_detail/{chatId}"

    fun groupDetail(chatId: String) = "group_detail/${encodePathSegment(chatId)}"
    fun peerProfile(peerId: String) = "profile_peer/${encodePathSegment(peerId)}"

    fun voiceRoom(roomId: String) = "voice_room/${encodePathSegment(roomId)}"
    fun call(callId: String) = "call/${encodePathSegment(callId)}"
}
