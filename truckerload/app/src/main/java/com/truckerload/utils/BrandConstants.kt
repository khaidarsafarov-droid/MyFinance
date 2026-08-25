package com.truckerload.utils

/** User-facing brand and stable filesystem identifiers (no spaces in folder names). */
object BrandConstants {
    const val DISPLAY_NAME = "TruckoRig"
    const val DOWNLOADS_FOLDER = "TruckoRig"
    const val FILE_PREFIX = "TruckoRig"
    /** Previous on-disk folder names; restore still looks here. */
    const val LEGACY_DOWNLOADS_FOLDER = "TruckLog"
    const val LEGACY_FILE_PREFIX = "TruckLog"
    const val LEGACY_DOWNLOADS_FOLDER_V1 = "TruckerLoad"
    const val LEGACY_FILE_PREFIX_V1 = "TruckerLoad"
}
