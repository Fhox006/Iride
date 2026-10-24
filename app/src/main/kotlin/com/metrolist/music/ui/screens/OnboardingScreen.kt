/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import com.metrolist.music.ui.component.IrideFolioPrimaryButton
import com.metrolist.music.ui.component.IrideFolioSecondaryButton

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import com.metrolist.music.ui.theme.FolioShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import com.metrolist.music.R
import com.metrolist.music.constants.AppLanguageKey
import com.metrolist.music.constants.ContentLanguageKey
import com.metrolist.music.constants.OnboardingCompletedKey
import com.metrolist.music.ui.component.IrideIconTile
import com.metrolist.music.ui.component.IridePageLayoutDefaults
import com.metrolist.music.ui.component.iridePageTitleTextStyle
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily
import com.metrolist.music.ui.theme.fillSubtle
import com.metrolist.music.ui.theme.strokeHairline
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.theme.textSecondary
import com.metrolist.music.utils.dataStore
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState { 2 }

    fun completeOnboarding() {
        coroutineScope.launch {
            context.dataStore.edit {
                it[OnboardingCompletedKey] = true
                it[AppLanguageKey] = "en"
                it[ContentLanguageKey] = "en"
            }
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
            when (page) {
                0 -> WelcomePage(
                    onNext = {
                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                    },
                )
                1 -> LoginPage(
                    onLogin = {
                        completeOnboarding()
                        navController.navigate("login")
                    },
                    onSkip = {
                        completeOnboarding()
                        navController.navigate(Screens.Home.route) {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    },
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 12.dp, bottom = 20.dp)
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
    OnboardingPage(
        icon = R.drawable.ic_logo,
        title = "Iride",
        description = stringResource(R.string.onboarding_welcome_tagline),
        primaryAction = stringResource(R.string.onboarding_get_started),
        onPrimaryAction = onNext,
    )
}

@Composable
private fun LoginPage(onLogin: () -> Unit, onSkip: () -> Unit) {
    OnboardingPage(
        icon = R.drawable.login,
        title = stringResource(R.string.onboarding_login_title),
        description = stringResource(R.string.onboarding_login_desc),
        primaryAction = stringResource(R.string.onboarding_login_cta),
        onPrimaryAction = onLogin,
        secondaryAction = stringResource(R.string.onboarding_login_skip),
        onSecondaryAction = onSkip,
    )
}

@Composable
private fun OnboardingPage(
    @DrawableRes icon: Int,
    title: String,
    description: String,
    primaryAction: String,
    onPrimaryAction: () -> Unit,
    secondaryAction: String? = null,
    onSecondaryAction: () -> Unit = {},
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val viewportHeight = maxHeight
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            // Keep the actions with the message; scroll the complete group when text exceeds the viewport.
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .heightIn(min = viewportHeight)
                    .padding(horizontal = IridePageLayoutDefaults.HorizontalPadding, vertical = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (icon == R.drawable.ic_logo) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.textPrimary,
                        modifier = Modifier.size(88.dp),
                    )
                } else {
                    IrideIconTile(
                        size = 80.dp,
                        color = MaterialTheme.colorScheme.fillSubtle,
                    ) {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.textPrimary,
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
                Spacer(Modifier.height(28.dp))
                Text(
                    text = title,
                    style = iridePageTitleTextStyle(),
                    color = MaterialTheme.colorScheme.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = IBMPlexMonoFontFamily,
                        lineHeight = 22.sp,
                        letterSpacing = 0.sp,
                    ),
                    color = MaterialTheme.colorScheme.textSecondary,
                    textAlign = TextAlign.Center,
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                ) {
                    OnboardingActionButton(text = primaryAction, onClick = onPrimaryAction)
                    if (secondaryAction != null) {
                        OnboardingActionButton(
                            text = secondaryAction,
                            onClick = onSecondaryAction,
                            secondary = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingActionButton(
    text: String,
    onClick: () -> Unit,
    secondary: Boolean = false,
) {
    val modifier = Modifier.fillMaxWidth()
    val label: @Composable () -> Unit = {
        Text(
            text = text,
            textAlign = TextAlign.Center,
        )
    }
    if (secondary) {
        IrideFolioSecondaryButton(
            onClick = onClick,
            modifier = modifier,
        ) {
            label()
        }
    } else {
        IrideFolioPrimaryButton(
            onClick = onClick,
            modifier = modifier,
        ) {
            label()
        }
    }
}
