package com.metrolist.music.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import com.metrolist.music.ui.component.IrideFolioPrimaryButton
import com.metrolist.music.ui.component.IridePageLayoutDefaults
import com.metrolist.music.ui.component.iridePageTitleTextStyle
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.theme.textSecondary

@Composable
internal fun OnboardingWelcomeLegacy(onNext: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val viewportHeight = maxHeight
        Box(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .heightIn(min = viewportHeight)
                    .padding(horizontal = IridePageLayoutDefaults.HorizontalPadding, vertical = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_logo),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.textPrimary,
                    modifier = Modifier.size(88.dp),
                )
                Spacer(Modifier.height(28.dp))
                Text(
                    text = "Iride",
                    style = iridePageTitleTextStyle(),
                    color = MaterialTheme.colorScheme.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.onboarding_welcome_tagline),
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
                    IrideFolioPrimaryButton(
                        onClick = onNext,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_get_started),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}