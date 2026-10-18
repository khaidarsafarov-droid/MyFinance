package com.truckerload.presentation.screens.social

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.presentation.di.LocalRpmThresholdsStore
import com.truckerload.presentation.premium.PremiumPaywallDialog
import com.truckerload.presentation.premium.rememberPremiumAccess
import com.truckerload.presentation.theme.LocalBrandPalette
import com.truckerload.presentation.theme.LocalOledDarkEnabled
import com.truckerload.presentation.theme.LocalReduceMotion
import com.truckerload.presentation.theme.SoftUiColors
import com.truckerload.presentation.theme.screenEnterAnimation
import com.truckerload.presentation.utils.useNavigationRail
import com.truckerload.utils.findActivity

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onAddLoad: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    showBack: Boolean = true,
    motionSettled: Boolean = true,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = uiState.profile
    val tablet = useNavigationRail()
    val reduceMotion = LocalReduceMotion.current
    val thresholds by LocalRpmThresholdsStore.current.thresholds.collectAsStateWithLifecycle()
    val page = if (LocalOledDarkEnabled.current) SoftUiColors.BackgroundOled else LocalBrandPalette.current.page
    val listState = rememberLazyListState()
    var nameOpen by remember { mutableStateOf(false) }
    var nameScroll by remember { mutableIntStateOf(0) }
    var paywallOpen by remember { mutableStateOf(false) }
    var cardsShown by remember { mutableStateOf(motionSettled) }
    val context = LocalContext.current
    val access = rememberPremiumAccess()
    LaunchedEffect(motionSettled) {
        if (motionSettled) cardsShown = true
    }
    LaunchedEffect(nameOpen, nameScroll, cardsShown) {
        if (!nameOpen || !cardsShown) return@LaunchedEffect
        if (reduceMotion) listState.scrollToItem(2) else listState.animateScrollToItem(2)
    }

    Scaffold(
        modifier = modifier,
        containerColor = page,
        contentWindowInsets = if (tablet) WindowInsets.safeDrawing else WindowInsets(0),
    ) { padding ->
        if (profile == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(48.dp))
                CircularProgressIndicator(color = LocalBrandPalette.current.primary)
            }
            return@Scaffold
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            item(key = "hero") {
                ProfileHero(
                    givenName = uiState.givenName,
                    familyName = uiState.familyName,
                    avatarUrl = profile.avatarUrl,
                    subscribed = uiState.subscribed,
                    totalLoads = profile.totalLoads,
                    totalRevenue = profile.totalRevenue,
                    averageRpm = profile.averageRpm,
                    redThreshold = thresholds.minProfit,
                    greenThreshold = thresholds.targetProfit,
                    showMenu = !showBack && !tablet,
                    showBack = showBack && !tablet,
                    onBack = onBack,
                    onEditName = {
                        nameOpen = true
                        nameScroll += 1
                    },
                    onAddLoad = onAddLoad,
                )
            }
            item(key = "body") {
                AnimatedVisibility(
                    visible = cardsShown,
                    enter = screenEnterAnimation(reduceMotion),
                ) {
                    Column {
                        ProfileTotals(loads = profile.totalLoads, miles = profile.totalMiles)
                        ProfileSectionHeader(stringResource(com.truckerload.R.string.profile_account_title))
                        ProfileAccountCard(
                            givenName = uiState.givenName,
                            familyName = uiState.familyName,
                            subscribed = uiState.subscribed,
                            premiumPhase = uiState.premiumPhase,
                            premiumPrice = uiState.premiumPrice,
                            onEditName = {
                                nameOpen = true
                                nameScroll += 1
                            },
                            onOpenSettings = onOpenSettings,
                            onPremium = {
                                if (uiState.subscribed) {
                                    val activity = context.findActivity() ?: return@ProfileAccountCard
                                    access.openSubscriptionManagement(activity)
                                } else {
                                    paywallOpen = true
                                }
                            },
                        )
                    }
                }
            }
            if (nameOpen) {
                item(key = "name") {
                    ProfileSectionHeader(stringResource(R.string.profile_editing))
                    ProfileCard {
                        ProfileNameSection(
                            givenName = uiState.givenName,
                            familyName = uiState.familyName,
                            message = uiState.nameMessage,
                            messageIsError = uiState.nameMessageIsError,
                            onSave = viewModel::saveName,
                        )
                    }
                }
            }
            item(key = "details") {
                ProfileFilledDetails(profile)
                Spacer(Modifier.height(if (tablet) 24.dp else 16.dp))
            }
        }
    }
    if (paywallOpen) {
        PremiumPaywallDialog(onDismiss = { paywallOpen = false })
    }
}
