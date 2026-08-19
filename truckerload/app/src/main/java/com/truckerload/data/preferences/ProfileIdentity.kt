package com.truckerload.data.preferences

/**
 * Login (Google / email) is only a seed for the in-app profile.
 * After the driver sets a name or photo, those values stay until they change them.
 */
object ProfileIdentity {

    val PLACEHOLDER_NAMES = setOf("", "Водитель", "Driver", "User")

    fun isPlaceholderName(name: String?): Boolean =
        name.isNullOrBlank() || name.trim() in PLACEHOLDER_NAMES

    fun isLocalAvatar(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return !url.startsWith("http://") && !url.startsWith("https://")
    }

    fun isProviderAvatar(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return url.startsWith("http://") || url.startsWith("https://")
    }

    /**
     * Merge a fresh login payload onto the stored profile.
     * Cloud nickname still wins when present; custom name/photo never revert to Google.
     */
    fun mergeLoginProfile(incoming: UserProfile, existing: UserProfile?): UserProfile {
        if (existing == null) return incoming
        val nickname = when {
            !incoming.nickname.isNullOrBlank() -> incoming.nickname
            else -> existing.nickname
        }
        val keepName = existing.customDisplayName &&
            (!existing.givenName.isBlank() || !existing.familyName.isBlank())
        val keepPhoto = existing.customPhoto || isLocalAvatar(existing.photoUrl)
        return incoming.copy(
            nickname = nickname,
            givenName = if (keepName) existing.givenName else incoming.givenName,
            familyName = if (keepName) existing.familyName else incoming.familyName,
            customDisplayName = keepName,
            photoUrl = if (keepPhoto) existing.photoUrl else incoming.photoUrl,
            customPhoto = keepPhoto,
            phoneNumber = existing.phoneNumber?.takeIf { it.isNotBlank() } ?: incoming.phoneNumber,
        )
    }

    fun mergeRoomDisplayName(existingName: String, loginName: String): String {
        if (!isPlaceholderName(existingName)) return existingName
        return loginName.takeIf { !isPlaceholderName(it) }.orEmpty().ifBlank { existingName }
    }

    /**
     * Room avatar is the driver's photo. Provider (Google) URL is used only when they
     * have never chosen or cleared a photo themselves.
     */
    fun mergeRoomAvatar(
        existingAvatar: String?,
        providerPhotoUrl: String?,
        customPhoto: Boolean,
    ): String? {
        if (isLocalAvatar(existingAvatar)) return existingAvatar
        if (customPhoto) return existingAvatar?.takeIf { it.isNotBlank() }
        if (!existingAvatar.isNullOrBlank()) return existingAvatar
        return providerPhotoUrl?.takeIf { it.isNotBlank() }
    }

    fun displayPhotoUrl(
        roomAvatar: String?,
        providerPhotoUrl: String?,
        customPhoto: Boolean,
    ): String? {
        if (!roomAvatar.isNullOrBlank()) return roomAvatar
        if (customPhoto) return null
        return providerPhotoUrl?.takeIf { it.isNotBlank() }
    }
}
