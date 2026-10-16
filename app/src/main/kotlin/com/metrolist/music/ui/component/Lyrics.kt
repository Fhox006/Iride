/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.metrolist.music.constants.ExperimentalLyricsKey
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.lyrics.LyricsTranslationHelper
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.LyricsViewModel

@Composable
fun Lyrics(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    showLyrics: Boolean,
    lyricsViewModel: LyricsViewModel = hiltViewModel(),
    onShowOptionsMenu: () -> Unit = {},
    isFullScreen: Boolean = false,
    onExitFullScreen: () -> Unit = {},
    showPills: Boolean = true,
    pillsController: LyricsPillController? = null,
    textScale: Float = 1f,
    singleLineOnly: Boolean = false,
    compactPresentation: Boolean = false,
    fullness: () -> Float = { if (isFullScreen) 1f else 0f },
    morphing: () -> Boolean = { false },
) {
    val (experimentalLyrics, _) = rememberPreference(key = ExperimentalLyricsKey, defaultValue = true)

    if (experimentalLyrics) {
        ExperimentalLyrics(
            sliderPositionProvider = sliderPositionProvider,
            modifier = modifier,
            showLyrics = showLyrics,
            lyricsViewModel = lyricsViewModel,
            onShowOptionsMenu = onShowOptionsMenu,
            isFullScreen = isFullScreen,
            onExitFullScreen = onExitFullScreen,
            showPills = showPills,
            pillsController = pillsController,
            textScale = textScale,
            singleLineOnly = singleLineOnly,
            compactPresentation = compactPresentation,
            fullness = fullness,
            morphing = morphing,
        )
    } else {
        if (pillsController != null) {
            val database = LocalDatabase.current
            val connection = LocalPlayerConnection.current
            val entity = connection?.currentLyrics?.collectAsState(initial = null)?.value
            SideEffect {
                pillsController.hasTranslations = !entity?.translatedLyrics.isNullOrBlank()
                pillsController.translateAction = {
                    if (entity != null && entity.translatedLyrics.isNotBlank()) {
                        database.query { upsert(LyricsTranslationHelper.clearTranslations(entity)) }
                        LyricsTranslationHelper.triggerClearTranslations()
                    } else {
                        LyricsTranslationHelper.triggerManualTranslation()
                    }
                }
            }
        }
        OriginalLyrics(
            sliderPositionProvider = sliderPositionProvider,
            modifier = modifier,
            showLyrics = showLyrics,
            pillsController = pillsController,
            textScale = textScale,
            singleLineOnly = singleLineOnly,
            compactPresentation = compactPresentation,
            fullness = fullness,
        )
    }
}
