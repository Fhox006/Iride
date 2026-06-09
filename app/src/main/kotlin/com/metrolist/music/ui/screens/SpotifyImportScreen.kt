/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.utils.backToMain

enum class ImportTutorialState {
    CHOICE, TUTORIAL_SHORT, TUTORIAL_LONG
}

fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyImportScreen(navController: NavController) {
    var tutorialState by remember { mutableStateOf(ImportTutorialState.CHOICE) }

    when (tutorialState) {
        ImportTutorialState.CHOICE -> ImportChoiceScreen(
            navController = navController,
            onSelectShort = { tutorialState = ImportTutorialState.TUTORIAL_SHORT },
            onSelectLong = { tutorialState = ImportTutorialState.TUTORIAL_LONG }
        )
        ImportTutorialState.TUTORIAL_SHORT -> ImportTutorialScreen(
            isLongExport = false,
            onBack = { tutorialState = ImportTutorialState.CHOICE }
        )
        ImportTutorialState.TUTORIAL_LONG -> ImportTutorialScreen(
            isLongExport = true,
            onBack = { tutorialState = ImportTutorialState.CHOICE }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportChoiceScreen(
    navController: NavController,
    onSelectShort: () -> Unit,
    onSelectLong: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal)
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Spacer(Modifier.height(64.dp))

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.library_music),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Import Your Spotify Library",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Choose how much listening history you want to import. You can always import your full lifetime data later.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OptionCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.sync,
                    title = "Quick Export",
                    subtitle = "Up to 5 days",
                    description = "Your recent listening activity. Fast to receive.",
                    badgeText = "⚡ Ready in ~2 days",
                    buttonText = "Choose Quick Export",
                    onClick = onSelectShort,
                    isPrimary = true
                )

                OptionCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.history,
                    title = "Full History",
                    subtitle = "Up to 30 days",
                    description = "Your complete lifetime listening data.",
                    badgeText = "📦 Ready in ~5 days",
                    buttonText = "Choose Full History",
                    onClick = onSelectLong,
                    isPrimary = false,
                    isRecommended = true
                )
            }

            Spacer(Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.info),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "💡 Don't worry — you can also import your full lifetime data later at any time from this screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            
            Spacer(Modifier.height(32.dp))
        }

        TopAppBar(
            title = { Text("Import Music Library") },
            navigationIcon = {
                IconButton(
                    onClick = navController::navigateUp,
                    onLongClick = navController::backToMain,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                    )
                }
            },
        )
    }
}

@Composable
fun OptionCard(
    modifier: Modifier = Modifier,
    iconRes: Int,
    title: String,
    subtitle: String,
    description: String,
    badgeText: String,
    buttonText: String,
    onClick: () -> Unit,
    isPrimary: Boolean,
    isRecommended: Boolean = false
) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isRecommended) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                        .align(Alignment.End)
                ) {
                    Text(
                        text = "REC",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Spacer(Modifier.height(18.dp))
            }

            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.height(48.dp)
            )

            Spacer(Modifier.height(12.dp))

            AssistChip(
                onClick = {},
                label = { Text(badgeText, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.height(24.dp)
            )

            Spacer(Modifier.height(12.dp))

            if (isPrimary) {
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(buttonText, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                }
            } else {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(buttonText, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportTutorialScreen(
    isLongExport: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal)
                ),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 88.dp, bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.timer),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "~5 minutes to complete",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                TutorialStep(
                    stepNumber = 1,
                    title = "Open Spotify in your browser",
                    description = {
                        Text("First, make sure you're logged into your Spotify account. Open a browser on your phone or computer — both work fine!")
                    },
                    actionLabel = "Open Spotify Login",
                    actionUrl = "https://accounts.spotify.com/login"
                )
            }

            item {
                TutorialStep(
                    stepNumber = 2,
                    title = "Head to your Privacy Settings",
                    description = {
                        Text("Once logged in, go to your Spotify Account Privacy page. Scroll all the way to the bottom — you'll find a section called **\"Download your data\"** or **\"Request your data\"**.")
                    },
                    actionLabel = "Open Spotify Privacy Page",
                    actionUrl = "https://www.spotify.com/us/account/privacy/",
                    highlight = true
                )
            }

            if (isLongExport) {
                item {
                    TutorialStep(
                        stepNumber = 3,
                        title = "⚙️ Enable the 30-day / Extended History option",
                        description = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("On the Privacy page, before tapping the download button, look for a checkbox or option that says something like:")
                                ElevatedCard(
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "\"Include your extended streaming history (up to 1 year)\"",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                                Text("✅ Make sure to check that box before confirming your request. This tells Spotify to include your full listening history — not just the last few days.")
                                Text(
                                    text = "⚠️ If you don't see this option, don't worry — Spotify may not show it in all regions. Just proceed normally.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        actionLabel = "Open Spotify Privacy Page",
                        actionUrl = "https://www.spotify.com/us/account/privacy/",
                        highlight = true
                    )
                }
            } else {
                item {
                    TutorialStep(
                        stepNumber = 3,
                        title = "Tap \"Request your data\"",
                        description = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("At the bottom of the Privacy page, you'll see a button. Tap it! Spotify will ask you to confirm. Just confirm — you're only requesting a copy of YOUR data, nothing changes on your account.")
                                Text(
                                    text = "ℹ️ No need to change any options here — just confirm the request as-is.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }

            item {
                TutorialStep(
                    stepNumber = 4,
                    title = "Spotify will email you 📬",
                    description = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("After confirming, Spotify sends you a confirmation email. Then, within **1–5 business days**, they'll send you another email with a download link. Check your inbox (and spam folder, just in case!).")
                            Text(
                                text = "The email comes from privacy@spotify.com",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }

            item {
                TutorialStep(
                    stepNumber = 5,
                    title = "Download the ZIP file",
                    description = {
                        Text("When the email arrives, click the download link. You'll get a ZIP file. Save it somewhere easy to find on your phone — like your Downloads folder.")
                    }
                )
            }

            item {
                TutorialStep(
                    stepNumber = 6,
                    title = "Import into Iride 🎉",
                    description = {
                        Text("Come back to this screen, tap the **\"Import ZIP File\"** button below, and select the file you just downloaded. Iride will do the rest!")
                    },
                    highlight = true
                )
            }

            item {
                Button(
                    onClick = { /* TODO: Launch file picker */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Icon(painter = painterResource(R.drawable.download), contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Import ZIP File")
                }
            }
        }

        TopAppBar(
            title = { Text("How to Export from Spotify") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = null,
                    )
                }
            },
        )
    }
}

@Composable
fun TutorialStep(
    stepNumber: Int,
    title: String,
    description: @Composable () -> Unit,
    actionUrl: String? = null,
    actionLabel: String? = null,
    highlight: Boolean = false
) {
    val context = LocalContext.current

    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (highlight) 4.dp else 2.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stepNumber.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                description()

                if (actionUrl != null && actionLabel != null) {
                    Spacer(Modifier.height(12.dp))
                    FilledTonalButton(
                        onClick = { openUrl(context, actionUrl) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(actionLabel)
                    }
                }
            }
        }
    }
}
