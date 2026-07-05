package com.truckerload.presentation.theme

import androidx.compose.ui.unit.dp

/** Shared UI dimensions — single source for icons, cards, and touch targets. */
object UiDimens {
    // Icons
    val IconNav = 24.dp
    val IconNavCompact = 20.dp
    val IconNavSelectedCompact = 20.dp
    val IconList = 20.dp
    val IconButton = 24.dp
    val IconFab = 28.dp
    val IconInline = 18.dp

    // Touch targets (Material minimum)
    val TouchTarget = 48.dp
    val TouchTargetCompact = 40.dp
    val FabSize = 56.dp

    // Cards (minimum heights for consistent list rhythm)
    val LoadCardMinHeight = 120.dp
    val StatCardMinHeight = 80.dp
    val GoalHeroMinHeight = 150.dp
    val ProfileHeaderMinHeight = 200.dp
    val ChatListItemMinHeight = 80.dp

    // Hero progress rings
    val HomeProgressRingSize = 180.dp
    val GoalProgressRingSize = 220.dp

    // Avatars & voice UI
    val AvatarProfile = 64.dp
    val AvatarVoiceGrid = 64.dp
    val AvatarVoiceSpeakingRing = 70.dp
    val AvatarCallLarge = 120.dp
    val AvatarCallActive = 100.dp
    val CallActionButton = 72.dp
    val CallActionIcon = 32.dp

    // Toolbar / top bar
    val ToolbarTouchTarget = 44.dp
    val ToolbarIconSize = 24.dp
}
