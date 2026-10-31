/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import com.metrolist.music.R
import com.metrolist.music.constants.AppLanguageKey
import com.metrolist.music.constants.ContentLanguageKey
import com.metrolist.music.constants.OnboardingCompletedKey
import com.metrolist.music.ui.theme.FolioShape
import com.metrolist.music.ui.theme.strokeHairline
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.utils.SystemNavigationMode
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.needsNavigationSetup
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val navigationMode = LocalSystemNavigationMode.current
    var setupWasRequired by rememberSaveable { mutableStateOf(needsNavigationSetup(navigationMode)) }
    val includeSetup = setupWasRequired || needsNavigationSetup(navigationMode)
    val tutorialPage = if (includeSetup) 3 else 2
    val pagerState = rememberPagerState { tutorialPage + 1 }

    // Keep the completed setup step after enabling gestures, but omit it for existing gesture users.
    LaunchedEffect(navigationMode) {
        if (needsNavigationSetup(navigationMode)) setupWasRequired = true
    }
    val gesturePage = if (navigationMode == SystemNavigationMode.Gestures) tutorialPage else 2

    // Returning from Settings advances to the fourth page only once gestures are active.
    LaunchedEffect(navigationMode, pagerState.settledPage) {
        if (pagerState.settledPage >= 2 && pagerState.settledPage != gesturePage) {
            pagerState.animateScrollToPage(gesturePage)
        }
    }
    var loginRequested by rememberSaveable { mutableStateOf(false) }

    suspend fun completeOnboarding() {
        context.dataStore.edit {
            it[OnboardingCompletedKey] = true
            it[AppLanguageKey] = "en"
            it[ContentLanguageKey] = "en"
        }
        navController.navigate(if (loginRequested) "login" else Screens.Home.route) {
            popUpTo("onboarding") { inclusive = true }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) { page ->
            CompositionLocalProvider(LocalExperienceVisible provides (pagerState.currentPage == page)) {
                when (page) {
                    0 -> WelcomePage(
                        onNext = {
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        },
                    )
                    1 -> LoginPage(
                        onLogin = {
                            loginRequested = true
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(2)
                            }
                        },
                        onSkip = {
                            loginRequested = false
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(2)
                            }
                        },
                    )
                    else -> if (page == tutorialPage) {
                        GestureNavigationFlow(
                            mode = navigationMode,
                            applyInsets = false,
                            onComplete = { completeOnboarding() },
                        )
                    } else {
                        GestureSetupPage(applyInsets = false)
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 4.dp, bottom = 16.dp)
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = pagerState.currentPage.toFloat(),
                        range = 0f..(pagerState.pageCount - 1).toFloat(),
                        steps = pagerState.pageCount - 2,
                    )
                },
        ) {
            repeat(pagerState.pageCount) { index ->
                Box(
                    modifier = Modifier
                        .size(width = 24.dp, height = 3.dp)
                        .clip(FolioShape(1.dp))
                        .background(
                            if (pagerState.currentPage == index) MaterialTheme.colorScheme.textPrimary
                            else MaterialTheme.colorScheme.strokeHairline,
                        ),
                )
            }
        }
    }
}

@Composable
private fun WelcomePage(onNext: () -> Unit) {
    ExperiencePage(
        title = stringResource(R.string.experience_intro_title),
        description = stringResource(R.string.experience_intro_body),
        illustration = { NavigationMotionIllustration(NavigationDemo.Welcome) },
        illustrationMaxHeight = 128.dp,
        applyInsets = false,
        actions = { ExperienceButton(stringResource(R.string.onboarding_get_started), onNext) },
    )
}

@Composable
private fun LoginPage(onLogin: () -> Unit, onSkip: () -> Unit) {
    ExperiencePage(
        title = stringResource(R.string.experience_account_title),
        description = stringResource(R.string.experience_account_body),
        illustration = { NavigationMotionIllustration(NavigationDemo.Account) },
        applyInsets = false,
        actions = {
            ExperienceButton(stringResource(R.string.onboarding_login_cta), onLogin)
            ExperienceButton(stringResource(R.string.onboarding_login_skip), onSkip, secondary = true)
        },
    )
}
