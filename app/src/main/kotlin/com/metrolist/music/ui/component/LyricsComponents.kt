/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.text.Layout
import android.widget.Toast
import timber.log.Timber
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.music.R
import com.metrolist.music.lyrics.LyricsTranslationHelper
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.screens.settings.LyricsPosition
import com.metrolist.music.utils.ComposeToImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun LyricsTranslationHeader(
    status: LyricsTranslationHelper.TranslationStatus,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = status !is LyricsTranslationHelper.TranslationStatus.Idle,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        when (status) {
            is LyricsTranslationHelper.TranslationStatus.Translating -> {
                TranslationCard(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = stringResource(R.string.ai_translating_lyrics),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            is LyricsTranslationHelper.TranslationStatus.Error -> {
                LaunchedEffect(status) {
                    delay(3000L)
                    LyricsTranslationHelper.clearErrorStatus()
                }
                TranslationCard(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Icon(
                        painter = painterResource(R.drawable.error),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = status.message,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            is LyricsTranslationHelper.TranslationStatus.Success -> {
                TranslationCard(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ) {
                    Icon(
                        painter = painterResource(R.drawable.check),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.ai_lyrics_translated),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun TranslationCard(
    containerColor: Color,
    contentColor: Color,
    content: @Composable RowScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
internal fun LyricsActionOverlay(
    isAutoScrollEnabled: Boolean,
    isSynced: Boolean,
    isSelectionModeActive: Boolean,
    anySelected: Boolean,
    onSyncClick: () -> Unit,
    onCancelSelection: () -> Unit,
    onShareSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(bottom = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = !isAutoScrollEnabled && isSynced && !isSelectionModeActive,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            FilledTonalButton(onClick = onSyncClick) {
                Icon(painterResource(R.drawable.sync), stringResource(R.string.auto_scroll), Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.auto_scroll))
            }
        }

        AnimatedVisibility(
            visible = isSelectionModeActive,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onShareSelection,
                    enabled = anySelected
                ) {
                    Icon(painterResource(R.drawable.upload), stringResource(R.string.share_selected), Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
internal fun LyricsShareDialog(
    txt: String,
    title: String,
    arts: String,
    songId: String,
    onDismiss: () -> Unit,
    onShareAsImage: () -> Unit
) {
    LaunchedEffect(Unit) {
        onShareAsImage()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LyricsColorPickerDialog(
    txt: String,
    title: String,
    arts: String,
    thumbnailUrl: String?,
    lyricsTextPosition: LyricsPosition,
    onDismiss: () -> Unit,
    onShare: (backgroundColor: Color, textColor: Color, secondaryTextColor: Color, style: LyricsBackgroundStyle) -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // Freeze all parameters at dialog-open time so a song change doesn't update the UI
    val frozenTxt = remember { txt }
    val frozenTitle = remember { title }
    val frozenArts = remember { arts }
    val frozenThumbnailUrl = remember { thumbnailUrl }

    val pal = remember { mutableStateListOf<Color>() }
    var bgStyle by remember { mutableStateOf(LyricsBackgroundStyle.SOLID) }
    var previewBackgroundColor by remember { mutableStateOf(Color(0xFF242424)) }
    var previewTextColor by remember { mutableStateOf(Color.White) }
    var previewSecondaryTextColor by remember { mutableStateOf(Color.White.copy(alpha = 0.7f)) }

    val paletteColors = (pal.take(3) + listOf(Color(0xFF242424), Color(0xFF121212))).distinct().take(5)

    val align = when (lyricsTextPosition) {
        LyricsPosition.LEFT -> TextAlign.Left
        LyricsPosition.CENTER -> TextAlign.Center
        else -> TextAlign.Right
    }

    LaunchedEffect(frozenThumbnailUrl) {
        if (frozenThumbnailUrl != null) {
            withContext(Dispatchers.IO) {
                try {
                    val res = ImageLoader(context).execute(ImageRequest.Builder(context).data(frozenThumbnailUrl).allowHardware(false).build())
                    val bmp = res.image?.toBitmap()
                    if (bmp != null) {
                        val swatches = Palette.from(bmp).generate().swatches.sortedByDescending { it.population }
                        pal.clear()
                        pal.addAll(swatches.map { Color(it.rgb) }.filter {
                            val hsv = FloatArray(3)
                            android.graphics.Color.colorToHSV(it.toArgb(), hsv)
                            hsv[1] > 0.2f
                        }.take(5))
                        previewBackgroundColor = (pal.take(3) + listOf(Color(0xFF242424), Color(0xFF121212))).distinct().take(5).firstOrNull() ?: Color(0xFF242424)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to extract palette colors")
                }
            }
        }
    }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp)
            ) {
                Text("Share lyrics", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    LyricsImageCard(
                        lyricText = frozenTxt,
                        mediaMetadata = MediaMetadata(
                            id = "",
                            title = frozenTitle,
                            artists = listOf(MediaMetadata.Artist(name = frozenArts, id = null)),
                            thumbnailUrl = frozenThumbnailUrl,
                            duration = 0
                        ),
                        darkBackground = true,
                        backgroundColor = previewBackgroundColor,
                        backgroundStyle = bgStyle,
                        textColor = previewTextColor,
                        secondaryTextColor = previewSecondaryTextColor,
                        textAlign = align
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { bgStyle = LyricsBackgroundStyle.SOLID },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50),
                        colors = if (bgStyle == LyricsBackgroundStyle.SOLID) androidx.compose.material3.ButtonDefaults.buttonColors() else androidx.compose.material3.ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Text("Color")
                    }
                    Button(
                        onClick = { bgStyle = LyricsBackgroundStyle.BLUR },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50),
                        colors = if (bgStyle == LyricsBackgroundStyle.BLUR) androidx.compose.material3.ButtonDefaults.buttonColors() else androidx.compose.material3.ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Text("Blur")
                    }
                }

                AnimatedVisibility(visible = bgStyle == LyricsBackgroundStyle.SOLID) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(18.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            paletteColors.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .background(color, RoundedCornerShape(8.dp))
                                        .clickable { previewBackgroundColor = color }
                                        .border(
                                            2.dp,
                                            if (previewBackgroundColor == color) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                try {
                                    val configuration = context.resources.configuration
                                    val density = context.resources.displayMetrics.density
                                    val image = ComposeToImage.createLyricsImage(
                                        context = context,
                                        coverArtUrl = frozenThumbnailUrl,
                                        songTitle = frozenTitle,
                                        artistName = frozenArts,
                                        lyrics = frozenTxt,
                                        width = (configuration.screenWidthDp * density).toInt(),
                                        height = (configuration.screenHeightDp * density).toInt(),
                                        backgroundColor = previewBackgroundColor.toArgb(),
                                        backgroundStyle = bgStyle,
                                        textColor = previewTextColor.toArgb(),
                                        secondaryTextColor = previewSecondaryTextColor.toArgb(),
                                        lyricsAlignment = when (lyricsTextPosition) {
                                            LyricsPosition.LEFT -> Layout.Alignment.ALIGN_NORMAL
                                            LyricsPosition.CENTER -> Layout.Alignment.ALIGN_CENTER
                                            else -> Layout.Alignment.ALIGN_OPPOSITE
                                        }
                                    )
                                    val uri = ComposeToImage.saveBitmapAsFile(context, image, "lyrics_share_${System.currentTimeMillis()}")
                                    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clipData = android.content.ClipData.newUri(context.contentResolver, "Lyrics Image", uri)
                                    clipboardManager.setPrimaryClip(clipData)
                                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
                                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Timber.e(e, "Failed to copy image to clipboard")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(painterResource(R.drawable.content_copy), null, Modifier.size(20.dp))
                    }

                    Button(
                        onClick = { onShare(previewBackgroundColor, previewTextColor, previewSecondaryTextColor, bgStyle) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(painterResource(R.drawable.upload), null, Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LyricsApiSetupDialog(
    provider: String,
    currentApiKey: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val providerUrls = mapOf(
        "OpenRouter" to "https://openrouter.ai",
        "OpenAI" to "https://platform.openai.com/api-keys",
        "Gemini" to "https://aistudio.google.com/apikey",
        "Claude" to "https://console.anthropic.com/settings/keys",
        "XAi" to "https://console.x.ai",
        "Mistral" to "https://console.mistral.ai/api-keys",
        "Perplexity" to "https://perplexity.ai/settings/api",
        "DeepL" to "https://deepl.com/pro-api",
    )
    var keyInput by remember { mutableStateOf(currentApiKey) }
    val url = providerUrls[provider] ?: ""
    val primaryColor = MaterialTheme.colorScheme.primary

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        painterResource(R.drawable.key),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text("Translation setup", style = MaterialTheme.typography.headlineSmall)
                }

                Text(
                    "Provider: $provider",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    "To use AI lyrics translation, an API key for the selected provider is required.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                if (url.isNotEmpty()) {
                    val annotatedString = buildAnnotatedString {
                        append("Get your API key at ")
                        val start = length
                        append(url)
                        addLink(
                            LinkAnnotation.Url(
                                url = url,
                                styles = TextLinkStyles(
                                    style = SpanStyle(
                                        color = primaryColor,
                                        textDecoration = TextDecoration.Underline,
                                    ),
                                ),
                            ),
                            start = start,
                            end = length,
                        )
                    }
                    Text(annotatedString, style = MaterialTheme.typography.bodyMedium)
                }

                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(painterResource(R.drawable.key), contentDescription = null) },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(keyInput.trim()) },
                        enabled = keyInput.trim().isNotBlank(),
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            }
        }
    }
}

// Helper for coroutine scope
typealias CoroutineScope = kotlinx.coroutines.CoroutineScope
