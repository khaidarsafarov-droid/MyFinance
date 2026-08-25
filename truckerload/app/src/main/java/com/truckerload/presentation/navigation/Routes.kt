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
    const val PROFILE = "profile"
    const val PROFILE_SETUP = "profile_setup"
    const val MAP = "map"
    const val LOAD_DETAIL = "load_detail/{loadId}"
    const val ADD_LOAD = "add_load"
    const val EDIT_LOAD = "edit_load/{loadId}?focusFinish={focusFinish}"
    const val ADD_PAYCHECK = "add_paycheck"
    const val ADD_DIESEL = "add_diesel"
    const val DIESEL = "diesel"
    const val MAINTENANCE = "maintenance"
    const val TAX_TRACKER = "tax_tracker"
    const val VOICE_ASSISTANT = "voice_assistant"
    const val SETTINGS = "settings"
    const val PRIVACY_SETTINGS = "privacy_settings"
    const val ABOUT = "about"
    const val IMPROVE = "improve"
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
    fun cameraForLoad(loadId: String, tripId: String, loadDate: String): String {
        return "camera_load/${encodePathSegment(loadId)}/${encodePathSegment(tripId)}/${encodePathSegment(loadDate)}"
    }

    fun scannerForLoad(loadId: String, tripId: String, loadDate: String): String {
        return "scanner_load/${encodePathSegment(loadId)}/${encodePathSegment(tripId)}/${encodePathSegment(loadDate)}"
    }

    fun attachPick(mode: String): String = "attach_pick/${encodePathSegment(mode)}"

    private fun encodePathSegment(value: String): String =
        Uri.encode(value.ifBlank { "_" }) ?: "_"
}
