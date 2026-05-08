/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.player

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.preferences.core.edit
import androidx.media3.common.C
import androidx.media3.common.Player.STATE_ENDED
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.CropAlbumArtKey
import com.metrolist.music.constants.DarkModeKey
import com.metrolist.music.constants.HidePlayerThumbnailKey
import com.metrolist.music.constants.HideStatusBarOnFullscreenKey
import com.metrolist.music.constants.KeepScreenOn
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.PlayerBackgroundStyleKey
import com.metrolist.music.constants.PlayerButtonsStyle
import com.metrolist.music.constants.PlayerButtonsStyleKey
import com.metrolist.music.constants.PlayerHorizontalPadding
import com.metrolist.music.constants.QueuePeekHeight
import com.metrolist.music.constants.SleepTimerDefaultKey
import com.metrolist.music.constants.SleepTimerFadeOutKey
import com.metrolist.music.constants.SleepTimerStopAfterCurrentSongKey
import com.metrolist.music.constants.SliderStyle
import com.metrolist.music.constants.SliderStyleKey
import com.metrolist.music.constants.SquigglySliderKey
import com.metrolist.music.constants.ThumbnailCornerRadius
import com.metrolist.music.constants.UseNewPlayerDesignKey
import com.metrolist.music.db.entities.LyricsEntity
import com.metrolist.music.di.LyricsHelperEntryPoint
import com.metrolist.music.extensions.togglePlayPause
import com.metrolist.music.extensions.toggleRepeatMode
import com.metrolist.music.listentogether.RoomRole
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.CastConnectionHandler
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.playback.SongWithArtists
import com.metrolist.music.ui.component.AnimatedAlbumGradientBackground
import com.metrolist.music.ui.component.BottomSheet
import com.metrolist.music.ui.component.BottomSheetState
import com.metrolist.music.ui.component.LocalBottomSheetPageState
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.Lyrics
import com.metrolist.music.ui.component.LyricsPillController
import com.metrolist.music.ui.component.PlayerSliderTrack
import com.metrolist.music.ui.component.ResizableIconButton
import com.metrolist.music.ui.component.SquigglySlider
import com.metrolist.music.ui.component.WavySlider
import com.metrolist.music.ui.component.rememberBottomSheetState
import com.metrolist.music.ui.menu.PlayerMenu
import com.metrolist.music.ui.screens.settings.DarkMode
import com.metrolist.music.ui.theme.InterFontFamily
import com.metrolist.music.ui.theme.PlayerColorExtractor
import com.metrolist.music.ui.theme.PlayerSliderColors
import com.metrolist.music.ui.utils.ShowMediaInfo
import com.metrolist.music.ui.utils.ShowOffsetDialog
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt
import com.metrolist.music.ui.component.Icon as MIcon

@Composable
internal fun PlayerControlsPanel(
    mediaMetadata: MediaMetadata,
    playerConnection: PlayerConnection,
    navController: NavController,
    state: BottomSheetState,
    effectiveIsPlaying: Boolean,
    playbackState: Int,
    showInlineLyrics: Boolean,
    onShowInlineLyricsChange: (Boolean) -> Unit,
    showQueue: Boolean,
    onShowQueueChange: (Boolean) -> Unit,
    isFullScreen: Boolean,
    onIsFullScreenChange: (Boolean) -> Unit,
    sliderPosition: Long?,
    onSliderPositionChange: (Long?) -> Unit,
    effectivePosition: Long,
    duration: Long,
    onSeekFinished: (Long) -> Unit,
    TextBackgroundColor: Color,
    textButtonColor: Color,
    iconButtonColor: Color,
    sideButtonContainerColor: Color,
    sideButtonContentColor: Color,
    currentSong: SongWithArtists?,
    isCasting: Boolean,
    castHandler: CastConnectionHandler?,
    castIsPlaying: Boolean,
    isListenTogetherGuest: Boolean,
    isMuted: Boolean,
    canSkipNext: Boolean,
    canSkipPrevious: Boolean,
    repeatMode: Int,
    hidePlayerThumbnail: Boolean,
    cropAlbumArt: Boolean,
    useNewPlayerDesign: Boolean,
    pureBlack: Boolean,
    playerBackground: PlayerBackgroundStyle,
    useDarkTheme: Boolean,
    sliderStyle: SliderStyle,
    squigglySlider: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val copiedTitleStr = stringResource(R.string.copied_title)
    val copiedArtistStr = stringResource(R.string.copied_artist)

    val playPauseRoundness by animateDpAsState(
        targetValue = if (effectiveIsPlaying) 24.dp else 36.dp,
        animationSpec = tween(durationMillis = 90, easing = LinearEasing),
        label = "playPauseRoundness",
    )

    Column(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding),
        ) {
            AnimatedContent(targetState = showInlineLyrics || showQueue, label = "ThumbnailAnimation") { show ->
                if (show) {
                    Row {
                        if (hidePlayerThumbnail) {
                            Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(ThumbnailCornerRadius)).background(MaterialTheme.colorScheme.surfaceVariant).clickable { onShowInlineLyricsChange(false); onShowQueueChange(false) }, contentAlignment = Alignment.Center) {
                                Icon(painter = painterResource(R.drawable.small_icon), contentDescription = null, modifier = Modifier.size(32.dp), tint = textButtonColor.copy(alpha = 0.7f))
                            }
                        } else {
                            AsyncImage(model = mediaMetadata.thumbnailUrl, contentDescription = null, contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit, modifier = Modifier.size(56.dp).clip(RoundedCornerShape(ThumbnailCornerRadius)).clickable { onShowInlineLyricsChange(false); onShowQueueChange(false) })
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                } else {
                    Spacer(modifier = Modifier.width(0.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                AnimatedContent(targetState = mediaMetadata.title, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "") { title ->
                    Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = TextBackgroundColor, modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp).combinedClickable(enabled = true, indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = { val albumId = mediaMetadata.album?.id ?: currentSong?.album?.id ?: currentSong?.song?.albumId; if (albumId != null) { navController.navigate("album/$albumId"); state.collapseSoft() } }, onLongClick = { val clip = ClipData.newPlainText(copiedTitleStr, title); clipboardManager.setPrimaryClip(clip); Toast.makeText(context, copiedTitleStr, Toast.LENGTH_SHORT).show() }))
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    if (mediaMetadata.explicit) MIcon.Explicit()
                    if (mediaMetadata.artists.any { it.name.isNotBlank() }) {
                        val annotatedString = buildAnnotatedString { mediaMetadata.artists.forEachIndexed { index, artist -> val tag = "artist_${artist.id.orEmpty()}"; pushStringAnnotation(tag = tag, annotation = artist.id.orEmpty()); withStyle(SpanStyle(color = TextBackgroundColor, fontSize = 16.sp)) { append(artist.name) }; pop(); if (index != mediaMetadata.artists.lastIndex) append(", ") } }
                        Box(modifier = Modifier.fillMaxWidth().basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp).padding(end = 12.dp)) {
                            var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                            var clickOffset by remember { mutableStateOf<Offset?>(null) }
                            Text(text = annotatedString, style = MaterialTheme.typography.titleMedium.copy(color = TextBackgroundColor), maxLines = 1, overflow = TextOverflow.Ellipsis, onTextLayout = { layoutResult = it }, modifier = Modifier.pointerInput(Unit) { awaitPointerEventScope { while (true) { val event = awaitPointerEvent(); val tapPosition = event.changes.firstOrNull()?.position; if (tapPosition != null) { clickOffset = tapPosition } } } }.combinedClickable(enabled = true, indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = { val tapPosition = clickOffset; val layout = layoutResult; if (tapPosition != null && layout != null) { val offset = layout.getOffsetForPosition(tapPosition); annotatedString.getStringAnnotations(offset, offset).firstOrNull()?.let { ann -> val artistId = ann.item; if (artistId.isNotBlank()) { navController.navigate("artist/$artistId"); state.collapseSoft() } } } }, onLongClick = { val clip = ClipData.newPlainText(copiedArtistStr, annotatedString); clipboardManager.setPrimaryClip(clip); Toast.makeText(context, copiedArtistStr, Toast.LENGTH_SHORT).show() }))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            if (useNewPlayerDesign) {
                val shareShape = RoundedCornerShape(topStart = 50.dp, bottomStart = 50.dp, topEnd = 3.dp, bottomEnd = 3.dp)
                val favShape = RoundedCornerShape(topStart = 3.dp, bottomStart = 3.dp, topEnd = 50.dp, bottomEnd = 50.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    AnimatedContent(targetState = showInlineLyrics, label = "MoreButton") { showLyrics -> if (!showLyrics) { FilledIconButton(onClick = { menuState.show { PlayerMenu(mediaMetadata = mediaMetadata, navController = navController, playerBottomSheetState = state, onShowDetailsDialog = { mediaMetadata.id.let { bottomSheetPageState.show { ShowMediaInfo(it) } } }, onDismiss = menuState::dismiss) } }, shape = shareShape, colors = IconButtonDefaults.filledIconButtonColors(containerColor = textButtonColor, contentColor = iconButtonColor), modifier = Modifier.size(42.dp)) { Icon(painter = painterResource(R.drawable.more_horiz), contentDescription = null, modifier = Modifier.size(24.dp)) } } }
                    AnimatedContent(targetState = showInlineLyrics, label = "LikeButton") { showLyrics -> if (showLyrics) { Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(24.dp)).background(textButtonColor).clickable { onIsFullScreenChange(!isFullScreen) }, contentAlignment = Alignment.Center) { Icon(painter = painterResource(if (isFullScreen) R.drawable.expand_less else R.drawable.fullscreen), contentDescription = null, tint = iconButtonColor, modifier = Modifier.size(24.dp)) } } else { val isEpisode = currentSong?.song?.isEpisode == true; val isFavorite = if (isEpisode) currentSong?.song?.inLibrary != null else currentSong?.song?.liked == true; FilledIconButton(onClick = playerConnection::toggleLike, shape = favShape, colors = IconButtonDefaults.filledIconButtonColors(containerColor = textButtonColor, contentColor = iconButtonColor), modifier = Modifier.size(42.dp)) { Icon(painter = painterResource(if (isFavorite) R.drawable.favorite else R.drawable.favorite_border), contentDescription = null, modifier = Modifier.size(24.dp)) } } }
                }
            } else {
                AnimatedContent(targetState = showInlineLyrics, label = "MoreButton") { showLyrics -> if (!showLyrics) { Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(24.dp)).background(textButtonColor).clickable { menuState.show { PlayerMenu(mediaMetadata = mediaMetadata, navController = navController, playerBottomSheetState = state, onShowDetailsDialog = { mediaMetadata.id.let { bottomSheetPageState.show { ShowMediaInfo(it) } } }, onDismiss = menuState::dismiss) } }) { Icon(painter = painterResource(R.drawable.more_horiz), contentDescription = null, tint = iconButtonColor, modifier = Modifier.align(Alignment.Center).size(24.dp)) } } }
                AnimatedContent(targetState = showInlineLyrics, label = "LikeButton") { showLyrics -> if (showLyrics) { Spacer(modifier = Modifier.size(12.dp)); val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null); Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(24.dp)).background(textButtonColor).clickable { menuState.show { com.metrolist.music.ui.menu.LyricsMenu(lyricsProvider = { currentLyrics }, songProvider = { currentSong?.song }, mediaMetadataProvider = { mediaMetadata }, onDismiss = menuState::dismiss, onShowOffsetDialog = { bottomSheetPageState.show { ShowOffsetDialog(songProvider = { currentSong?.song }) } }) } }) { Icon(painter = painterResource(R.drawable.more_horiz), contentDescription = null, tint = iconButtonColor, modifier = Modifier.align(Alignment.Center).size(24.dp)) } } }
            }
        }
        Spacer(Modifier.height(24.dp))
        when (sliderStyle) {
            SliderStyle.DEFAULT -> { Slider(value = (sliderPosition ?: effectivePosition).toFloat(), valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()), onValueChange = { if (!isListenTogetherGuest) onSliderPositionChange(it.toLong()) }, onValueChangeFinished = { if (!isListenTogetherGuest) { sliderPosition?.let { onSeekFinished(it) }; onSliderPositionChange(null) } }, enabled = !isListenTogetherGuest, colors = PlayerSliderColors.getSliderColors(textButtonColor, playerBackground, useDarkTheme), modifier = Modifier.padding(horizontal = PlayerHorizontalPadding)) }
            SliderStyle.WAVY -> { if (squigglySlider) { SquigglySlider(value = (sliderPosition ?: effectivePosition).toFloat(), valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()), onValueChange = { onSliderPositionChange(it.toLong()) }, onValueChangeFinished = { sliderPosition?.let { onSeekFinished(it) }; onSliderPositionChange(null) }, modifier = Modifier.padding(horizontal = PlayerHorizontalPadding), colors = PlayerSliderColors.getSliderColors(textButtonColor, playerBackground, useDarkTheme), isPlaying = effectiveIsPlaying) } else { WavySlider(value = (sliderPosition ?: effectivePosition).toFloat(), valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()), onValueChange = { onSliderPositionChange(it.toLong()) }, onValueChangeFinished = { sliderPosition?.let { onSeekFinished(it) }; onSliderPositionChange(null) }, colors = PlayerSliderColors.getSliderColors(textButtonColor, playerBackground, useDarkTheme), modifier = Modifier.padding(horizontal = PlayerHorizontalPadding), isPlaying = effectiveIsPlaying) } }
            SliderStyle.SLIM -> { Slider(value = (sliderPosition ?: effectivePosition).toFloat(), valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()), onValueChange = { if (!isListenTogetherGuest) onSliderPositionChange(it.toLong()) }, onValueChangeFinished = { if (!isListenTogetherGuest) { sliderPosition?.let { onSeekFinished(it) }; onSliderPositionChange(null) } }, enabled = !isListenTogetherGuest, thumb = { Spacer(modifier = Modifier.size(0.dp)) }, track = { sliderState -> PlayerSliderTrack(sliderState = sliderState, colors = PlayerSliderColors.getSliderColors(textButtonColor, playerBackground, useDarkTheme)) }, modifier = Modifier.padding(horizontal = PlayerHorizontalPadding)) }
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding + 4.dp)) {
            Text(text = makeTimeString(sliderPosition ?: effectivePosition), style = MaterialTheme.typography.labelMedium, color = TextBackgroundColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "", style = MaterialTheme.typography.labelMedium, color = TextBackgroundColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(24.dp))
        AnimatedVisibility(visible = !isFullScreen, enter = slideInVertically(initialOffsetY = { it }) + fadeIn(), exit = shrinkVertically(shrinkTowards = Alignment.Top) + slideOutVertically(targetOffsetY = { it }) + fadeOut()) {
            Column {
                if (useNewPlayerDesign) {
                    Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding)) {
                        val bIS = remember { MutableInteractionSource() }; val nIS = remember { MutableInteractionSource() }; val pPIS = remember { MutableInteractionSource() }
                        val isPP by pPIS.collectIsPressedAsState(); val isB by bIS.collectIsPressedAsState(); val isN by nIS.collectIsPressedAsState()
                        val pPW by animateFloatAsState(targetValue = if (isPP) 1.9f else if (isB || isN) 1.1f else 1.3f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f), label = "pPW")
                        val bBW by animateFloatAsState(targetValue = if (isB) 0.65f else if (isPP) 0.35f else 0.45f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f), label = "bBW")
                        val nBW by animateFloatAsState(targetValue = if (isN) 0.65f else if (isPP) 0.35f else 0.45f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f), label = "nBW")
                        FilledIconButton(onClick = playerConnection::seekToPrevious, enabled = canSkipPrevious && !isListenTogetherGuest, shape = RoundedCornerShape(50), interactionSource = bIS, colors = IconButtonDefaults.filledIconButtonColors(containerColor = sideButtonContainerColor, contentColor = sideButtonContentColor), modifier = Modifier.height(68.dp).weight(bBW)) { Icon(painter = painterResource(R.drawable.skip_previous), contentDescription = null, modifier = Modifier.size(32.dp)) }
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledIconButton(onClick = { if (isListenTogetherGuest) { playerConnection.toggleMute(); return@FilledIconButton }; if (isCasting) { if (castIsPlaying) castHandler?.pause() else castHandler?.play() } else if (playbackState == STATE_ENDED) { playerConnection.player.seekTo(0, 0); playerConnection.player.playWhenReady = true } else playerConnection.togglePlayPause() }, shape = RoundedCornerShape(50), interactionSource = pPIS, colors = IconButtonDefaults.filledIconButtonColors(containerColor = textButtonColor, contentColor = iconButtonColor), modifier = Modifier.height(68.dp).weight(pPW)) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) { Icon(painter = painterResource(if (isListenTogetherGuest) (if (isMuted) R.drawable.volume_off else R.drawable.volume_up) else (if (effectiveIsPlaying) R.drawable.pause else R.drawable.play)), contentDescription = null, modifier = Modifier.size(32.dp)); Spacer(modifier = Modifier.width(8.dp)); Text(text = if (isListenTogetherGuest) (if (isMuted) stringResource(R.string.unmute) else stringResource(R.string.mute)) else (if (effectiveIsPlaying) stringResource(R.string.pause) else stringResource(R.string.play)), style = MaterialTheme.typography.titleMedium) } }
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledIconButton(onClick = playerConnection::seekToNext, enabled = canSkipNext && !isListenTogetherGuest, shape = RoundedCornerShape(50), interactionSource = nIS, colors = IconButtonDefaults.filledIconButtonColors(containerColor = sideButtonContainerColor, contentColor = sideButtonContentColor), modifier = Modifier.height(68.dp).weight(nBW)) { Icon(painter = painterResource(R.drawable.skip_next), contentDescription = null, modifier = Modifier.size(32.dp)) }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = PlayerHorizontalPadding)) {
                        Box(modifier = Modifier.weight(1f)) { ResizableIconButton(icon = when (repeatMode) { com.metrolist.music.playback.Player.REPEAT_MODE_OFF, com.metrolist.music.playback.Player.REPEAT_MODE_ALL -> R.drawable.repeat; com.metrolist.music.playback.Player.REPEAT_MODE_ONE -> R.drawable.repeat_one; else -> R.drawable.repeat }, color = TextBackgroundColor, modifier = Modifier.size(32.dp).padding(4.dp).align(Alignment.Center).alpha(if (isListenTogetherGuest) 0.5f else 1f), enabled = !isListenTogetherGuest, onClick = { playerConnection.player.toggleRepeatMode() }) }
                        Box(modifier = Modifier.weight(1f)) { ResizableIconButton(icon = R.drawable.skip_previous, enabled = canSkipPrevious && !isListenTogetherGuest, color = TextBackgroundColor, modifier = Modifier.size(32.dp).align(Alignment.Center).alpha(if (isListenTogetherGuest) 0.5f else 1f), onClick = playerConnection::seekToPrevious) }
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.size(72.dp).clip(RoundedCornerShape(playPauseRoundness)).background(textButtonColor).clickable { if (isListenTogetherGuest) { playerConnection.toggleMute(); return@clickable }; if (isCasting) { if (castIsPlaying) castHandler?.pause() else castHandler?.play() } else if (playbackState == STATE_ENDED) { playerConnection.player.seekTo(0, 0); playerConnection.player.playWhenReady = true } else playerConnection.player.togglePlayPause() }) { Image(painter = painterResource(if (isListenTogetherGuest) (if (isMuted) R.drawable.volume_off else R.drawable.volume_up) else if (playbackState == STATE_ENDED) R.drawable.replay else if (effectiveIsPlaying) R.drawable.pause else R.drawable.play), contentDescription = null, colorFilter = ColorFilter.tint(iconButtonColor), modifier = Modifier.align(Alignment.Center).size(36.dp)) }
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) { ResizableIconButton(icon = R.drawable.skip_next, enabled = canSkipNext && !isListenTogetherGuest, color = TextBackgroundColor, modifier = Modifier.size(32.dp).align(Alignment.Center).alpha(if (isListenTogetherGuest) 0.5f else 1f), onClick = playerConnection::seekToNext) }
                        Box(modifier = Modifier.weight(1f)) { val isE = currentSong?.song?.isEpisode == true; val isF = if (isE) currentSong?.song?.inLibrary != null else currentSong?.song?.liked == true; ResizableIconButton(icon = if (isF) R.drawable.favorite else R.drawable.favorite_border, color = if (isF) MaterialTheme.colorScheme.onSurface else TextBackgroundColor, modifier = Modifier.size(32.dp).padding(4.dp).align(Alignment.Center), onClick = playerConnection::toggleLike) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
    showPeekContent: Boolean = true,
) {
    val context = LocalContext.current; val menuState = LocalMenuState.current; val database = LocalDatabase.current; val bottomSheetPageState = LocalBottomSheetPageState.current; val playerConnection = LocalPlayerConnection.current ?: return
    val (useNewPlayerDesign, _) = rememberPreference(UseNewPlayerDesignKey, defaultValue = true)
    val (hidePlayerThumbnail, _) = rememberPreference(HidePlayerThumbnailKey, false); val (hideStatusBarOnFullscreen) = rememberPreference(HideStatusBarOnFullscreenKey, false); val cropAlbumArt by rememberPreference(CropAlbumArtKey, false)
    var showInlineLyrics by rememberSaveable { mutableStateOf(false) }; var showQueue by rememberSaveable { mutableStateOf(false) }; var isFullScreen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.isExpanded) { if (!state.isExpanded) { showQueue = false; showInlineLyrics = false; isFullScreen = false } }
    val playerBackground by rememberEnumPreference(key = PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.ANIMATED_GRADIENT); val playerButtonsStyle by rememberEnumPreference(key = PlayerButtonsStyleKey, defaultValue = PlayerButtonsStyle.DEFAULT)
    val isSystemInDarkTheme = isSystemInDarkTheme(); val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.ON); val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) { if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON }
    val isPlaying by playerConnection.isPlaying.collectAsState(); val isKeepScreenOn by rememberPreference(KeepScreenOn, false); val keepScreenOn = isPlaying && isKeepScreenOn
    DisposableEffect(playerBackground, state.isExpanded, useDarkTheme, keepScreenOn, isFullScreen, hideStatusBarOnFullscreen) {
        val window = (context as? Activity)?.window
        if (window != null && state.isExpanded) { val insetsController = WindowCompat.getInsetsController(window, window.decorView); when (playerBackground) { PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT, PlayerBackgroundStyle.ANIMATED_GRADIENT -> { insetsController.isAppearanceLightStatusBars = false } else -> { insetsController.isAppearanceLightStatusBars = !useDarkTheme } }; if (isFullScreen && hideStatusBarOnFullscreen) { insetsController.hide(WindowInsetsCompat.Type.statusBars()); insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE } else { insetsController.show(WindowInsetsCompat.Type.statusBars()) }; if (keepScreenOn && state.isExpanded) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        onDispose { if (window != null) { val insetsController = WindowCompat.getInsetsController(window, window.decorView); insetsController.isAppearanceLightStatusBars = !useDarkTheme; insetsController.show(WindowInsetsCompat.Type.statusBars()); window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) } }
    }
    BackHandler(enabled = state.isExpanded && showQueue) { showQueue = false }
    val onBackgroundColor = when (playerBackground) { PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.secondary; else -> MaterialTheme.colorScheme.onSurface }
    val useBlackBackground = remember(isSystemInDarkTheme, darkTheme, pureBlack) { val useDarkTheme = if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON; useDarkTheme && pureBlack }
    val playbackState by playerConnection.playbackState.collectAsState(); val mediaMetadata by playerConnection.mediaMetadata.collectAsState(); val currentSong by playerConnection.currentSong.collectAsState(initial = null); val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null); val automix by playerConnection.service.automixItems.collectAsState(); val repeatMode by playerConnection.repeatMode.collectAsState(); val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState(); val canSkipNext by playerConnection.canSkipNext.collectAsState(); val isMuted by playerConnection.isMuted.collectAsState()
    val sliderStyle by rememberEnumPreference(SliderStyleKey, SliderStyle.DEFAULT); val squigglySlider by rememberPreference(SquigglySliderKey, defaultValue = false)
    val listenTogetherManager = LocalListenTogetherManager.current; val listenTogetherRoleState = listenTogetherManager?.role?.collectAsState(initial = RoomRole.NONE); val isListenTogetherGuest = listenTogetherRoleState?.value == RoomRole.GUEST
    val castHandler = remember(playerConnection) { try { playerConnection.service.castConnectionHandler } catch (e: Exception) { null } }; val isCasting by castHandler?.isCasting?.collectAsState() ?: remember { mutableStateOf(false) }; val castPosition by castHandler?.castPosition?.collectAsState() ?: remember { mutableLongStateOf(0L) }; val castDuration by castHandler?.castDuration?.collectAsState() ?: remember { mutableLongStateOf(0L) }; val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying; val positionState = remember { mutableLongStateOf(0L) }; val durationState = remember { mutableLongStateOf(0L) }; var position by positionState; var duration by durationState; val effectivePosition by remember { derivedStateOf { if (isCasting) castPosition else position } }
    var sliderPosition by remember { mutableStateOf<Long?>(null) }; var lastManualSeekTime by remember { mutableLongStateOf(0L) }
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }; val gradientColorsCache = remember { mutableMapOf<String, List<Color>>() }
    if (!canSkipNext && automix.isNotEmpty()) playerConnection.service.addToQueueAutomix(automix[0], 0)
    val fallbackColor = MaterialTheme.colorScheme.surface.toArgb()
    LaunchedEffect(mediaMetadata?.id, playerBackground) { if (playerBackground == PlayerBackgroundStyle.GRADIENT) { val currentMetadata = mediaMetadata; if (currentMetadata != null && currentMetadata.thumbnailUrl != null) { val cachedColors = gradientColorsCache[currentMetadata.id]; if (cachedColors != null) { gradientColors = cachedColors; return@LaunchedEffect }; withContext(Dispatchers.IO) { val request = ImageRequest.Builder(context).data(currentMetadata.thumbnailUrl).size(100, 100).allowHardware(false).memoryCacheKey("gradient_${currentMetadata.id}").build(); val result = runCatching { context.imageLoader.execute(request) }.getOrNull(); if (result != null) { val bitmap = result.image?.toBitmap(); if (bitmap != null) { val palette = withContext(Dispatchers.Default) { Palette.from(bitmap).maximumColorCount(8).resizeBitmapArea(100 * 100).generate() }; val extractedColors = PlayerColorExtractor.extractGradientColors(palette = palette, fallbackColor = fallbackColor); gradientColorsCache[currentMetadata.id] = extractedColors; withContext(Dispatchers.Main) { gradientColors = extractedColors } } } } } } else gradientColors = emptyList() }
    LaunchedEffect(state.isExpanded, mediaMetadata?.id) { if (state.isExpanded && mediaMetadata != null) { val capturedMetadata = mediaMetadata ?: return@LaunchedEffect; withContext(Dispatchers.IO) { try { val currentLyricsValue = playerConnection.currentLyrics.first(); if (currentLyricsValue == null) { val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, com.metrolist.music.di.LyricsHelperEntryPoint::class.java); val lyricsHelper = entryPoint.lyricsHelper().getLyrics(capturedMetadata); database.query { upsert(LyricsEntity(capturedMetadata.id, fetchedLyricsWithProvider.lyrics, fetchedLyricsWithProvider.provider)) } } } catch (_: Exception) {} } } }
    LaunchedEffect(mediaMetadata?.id) { isFullScreen = false; if (showInlineLyrics && mediaMetadata != null) { delay(200); if (currentLyrics == null) showInlineLyrics = false } }
    val TextBackgroundColor by animateColorAsState(targetValue = when (playerBackground) { PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground; else -> Color.White }, label = "TextBackgroundColor")
    val (textButtonColor, iconButtonColor) = when { playerBackground == PlayerBackgroundStyle.BLUR || playerBackground == PlayerBackgroundStyle.GRADIENT || playerBackground == PlayerBackgroundStyle.ANIMATED_GRADIENT -> { when (playerButtonsStyle) { PlayerButtonsStyle.PRIMARY -> Pair(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary); PlayerButtonsStyle.TERTIARY -> Pair(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary); else -> Pair(Color.White, Color.Black) } } else -> { when (playerButtonsStyle) { PlayerButtonsStyle.PRIMARY -> Pair(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary); PlayerButtonsStyle.TERTIARY -> Pair(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.onTertiary); else -> if (useDarkTheme) Pair(Color.White, Color.Black) else Pair(Color.Black, Color.White) } } }
    val (sideButtonContainerColor, sideButtonContentColor) = when { playerBackground == PlayerBackgroundStyle.BLUR || playerBackground == PlayerBackgroundStyle.GRADIENT || playerBackground == PlayerBackgroundStyle.ANIMATED_GRADIENT -> { when (playerButtonsStyle) { PlayerButtonsStyle.PRIMARY -> Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer); PlayerButtonsStyle.TERTIARY -> Pair(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer); else -> Pair(Color.White.copy(alpha = 0.2f), Color.White) } } else -> { when (playerButtonsStyle) { PlayerButtonsStyle.PRIMARY -> Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer); PlayerButtonsStyle.TERTIARY -> Pair(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer); else -> if (useDarkTheme) Pair(Color.White.copy(alpha = 0.2f), Color.White) else Pair(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurface) } } }
    LaunchedEffect(effectiveIsPlaying, isCasting) { if (!isCasting && effectiveIsPlaying) { while (isActive) { delay(100); if (sliderPosition == null) { position = playerConnection.player.currentPosition; duration = playerConnection.player.duration } } } }
    LaunchedEffect(playbackState, mediaMetadata?.id) { if (!isCasting) { position = playerConnection.player.currentPosition; duration = playerConnection.player.duration } }
    LaunchedEffect(isCasting, castPosition, castDuration) { if (isCasting && sliderPosition == null) { if (System.currentTimeMillis() - lastManualSeekTime > 1500) { position = castPosition; if (castDuration > 0) duration = castDuration } } }
    val dismissedBound = QueuePeekHeight + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(); val queueSheetState = rememberBottomSheetState(dismissedBound = dismissedBound, expandedBound = state.expandedBound, collapsedBound = dismissedBound + 1.dp, initialAnchor = 1); val bottomSheetBackgroundColor = when (playerBackground) { PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT, PlayerBackgroundStyle.ANIMATED_GRADIENT -> MaterialTheme.colorScheme.surfaceContainer; else -> if (useBlackBackground) Color.Black else MaterialTheme.colorScheme.surfaceContainer }; val backgroundAlpha = state.progress.coerceIn(0f, 1f)
    BottomSheet(
        state = state,
        modifier = modifier,
        background = { Box(modifier = Modifier.fillMaxSize().background(bottomSheetBackgroundColor)) { when (playerBackground) { PlayerBackgroundStyle.BLUR -> { AnimatedContent(targetState = mediaMetadata?.thumbnailUrl, transitionSpec = { fadeIn(tween(800)).togetherWith(fadeOut(tween(800))) }, label = "blurBackground") { thumbnailUrl -> if (thumbnailUrl != null) { Box(modifier = Modifier.alpha(backgroundAlpha)) { AsyncImage(model = ImageRequest.Builder(context).data(thumbnailUrl).size(100, 100).allowHardware(false).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().blur(if (useDarkTheme) 150.dp else 100.dp)); Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f))) } } } } PlayerBackgroundStyle.GRADIENT -> { AnimatedContent(targetState = gradientColors, transitionSpec = { fadeIn(tween(800)).togetherWith(fadeOut(tween(800))) }, label = "gradientBackground") { colors -> if (colors.isNotEmpty()) { val stops = if (colors.size >= 3) arrayOf(0.0f to colors[0], 0.5f to colors[1], 1.0f to colors[2]) else arrayOf(0.0f to colors[0], 0.6f to colors[0].copy(alpha = 0.7f), 1.0f to Color.Black); Box(Modifier.fillMaxSize().alpha(backgroundAlpha).background(Brush.verticalGradient(colorStops = stops)).background(Color.Black.copy(alpha = 0.2f))) } } } PlayerBackgroundStyle.ANIMATED_GRADIENT -> { var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }; LaunchedEffect(mediaMetadata?.id, mediaMetadata?.thumbnailUrl) { if (mediaMetadata?.thumbnailUrl != null) { bitmap = context.imageLoader.execute(ImageRequest.Builder(context).data(mediaMetadata?.thumbnailUrl).size(100, 100).allowHardware(false).build()).image?.toBitmap() } else bitmap = null }; AnimatedAlbumGradientBackground(thumbnail = bitmap, modifier = Modifier.fillMaxSize().alpha(backgroundAlpha)) } else -> {} } } },
        onDismiss = if (!isListenTogetherGuest) { { playerConnection.service.clearAutomix(); playerConnection.player.stop(); playerConnection.player.clearMediaItems() } } else null,
        collapsedContent = { if (showPeekContent) MiniPlayer(positionState = positionState, durationState = durationState, playerBottomSheetState = state) },
    ) {
        when (LocalConfiguration.current.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                val density = LocalDensity.current; val vPadding = max(WindowInsets.systemBars.getTop(density), WindowInsets.systemBars.getBottom(density)); val vPaddingDp = with(density) { vPadding.toDp() }
                Row(modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).add(WindowInsets(top = vPaddingDp, bottom = vPaddingDp))).padding(bottom = 24.dp).fillMaxSize()) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).nestedScroll(state.preUpPostDownNestedScrollConnection)) { val sliderPos by rememberUpdatedState(sliderPosition); val isExpProvider = remember(state) { { state.isExpanded } }; AnimatedContent(targetState = when { showInlineLyrics -> "lyrics"; showQueue -> "queue"; else -> "thumbnail" }, label = "PlayerView", transitionSpec = { fadeIn() togetherWith fadeOut() }) { view -> when (view) { "lyrics" -> InlineLyricsView(mediaMetadata = mediaMetadata, showLyrics = true, positionProvider = { effectivePosition }, isFullScreen = isFullScreen, onExitFullScreen = { isFullScreen = false }, onShowOptionsMenu = { mediaMetadata?.let { mm -> menuState.show { com.metrolist.music.ui.menu.LyricsMenu(lyricsProvider = { currentLyrics }, songProvider = { currentSong?.song }, mediaMetadataProvider = { mm }, onDismiss = menuState::dismiss, onShowOffsetDialog = { bottomSheetPageState.show { ShowOffsetDialog(songProvider = { currentSong?.song }) } }) } } }, textButtonColor = textButtonColor, iconButtonColor = iconButtonColor); "queue" -> InlineQueuePanel(navController = navController, playerBottomSheetState = state, textButtonColor = textButtonColor, iconButtonColor = iconButtonColor, onClose = { showQueue = false }); else -> Thumbnail(sliderPositionProvider = { sliderPos }, modifier = Modifier.animateContentSize(), isPlayerExpanded = isExpProvider, isLandscape = true, isListenTogetherGuest = isListenTogetherGuest) } } }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(if (showInlineLyrics || showQueue) 0.65f else 1f, false).animateContentSize().windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))) { Spacer(Modifier.weight(1f)); mediaMetadata?.let { mm -> PlayerControlsPanel(mediaMetadata = mm, playerConnection = playerConnection, navController = navController, state = state, effectiveIsPlaying = effectiveIsPlaying, playbackState = playbackState, showInlineLyrics = showInlineLyrics, onShowInlineLyricsChange = { showInlineLyrics = it }, showQueue = showQueue, onShowQueueChange = { showQueue = it }, isFullScreen = isFullScreen, onIsFullScreenChange = { isFullScreen = it }, sliderPosition = sliderPosition, onSliderPositionChange = { sliderPosition = it }, effectivePosition = effectivePosition, duration = duration, onSeekFinished = { pos -> if (isCasting) { castHandler?.seekTo(pos); lastManualSeekTime = System.currentTimeMillis() } else playerConnection.player.seekTo(pos); position = pos }, TextBackgroundColor = TextBackgroundColor, textButtonColor = textButtonColor, iconButtonColor = iconButtonColor, sideButtonContainerColor = sideButtonContainerColor, sideButtonContentColor = sideButtonContentColor, currentSong = currentSong, isCasting = isCasting, castHandler = castHandler, castIsPlaying = castIsPlaying, isListenTogetherGuest = isListenTogetherGuest, isMuted = isMuted, canSkipNext = canSkipNext, canSkipPrevious = canSkipPrevious, repeatMode = repeatMode, hidePlayerThumbnail = hidePlayerThumbnail, cropAlbumArt = cropAlbumArt, useNewPlayerDesign = useNewPlayerDesign, pureBlack = pureBlack, playerBackground = playerBackground, useDarkTheme = useDarkTheme, sliderStyle = sliderStyle, squigglySlider = squigglySlider) }; Spacer(Modifier.weight(1f)) }
                }
            }
            else -> {
                val bPadding by animateDpAsState(targetValue = if (isFullScreen) 0.dp else queueSheetState.collapsedBound, label = "bottomPadding")
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)).padding(bottom = bPadding).animateContentSize()) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) { val sliderPos by rememberUpdatedState(sliderPosition); val isExpProvider = remember(state) { { state.isExpanded } }; AnimatedContent(targetState = when { showInlineLyrics -> "lyrics"; showQueue -> "queue"; else -> "thumbnail" }, label = "PlayerView", transitionSpec = { fadeIn() togetherWith fadeOut() }) { view -> when (view) { "lyrics" -> InlineLyricsView(mediaMetadata = mediaMetadata, showLyrics = true, positionProvider = { effectivePosition }, isFullScreen = isFullScreen, onExitFullScreen = { isFullScreen = false }, onShowOptionsMenu = { mediaMetadata?.let { mm -> menuState.show { com.metrolist.music.ui.menu.LyricsMenu(lyricsProvider = { currentLyrics }, songProvider = { currentSong?.song }, mediaMetadataProvider = { mm }, onDismiss = menuState::dismiss, onShowOffsetDialog = { bottomSheetPageState.show { ShowOffsetDialog(songProvider = { currentSong?.song }) } }) } } }, textButtonColor = textButtonColor, iconButtonColor = iconButtonColor); "queue" -> InlineQueuePanel(navController = navController, playerBottomSheetState = state, textButtonColor = textButtonColor, iconButtonColor = iconButtonColor, onClose = { showQueue = false }); else -> Thumbnail(sliderPositionProvider = { sliderPos }, modifier = Modifier.nestedScroll(state.preUpPostDownNestedScrollConnection), isPlayerExpanded = isExpProvider, isListenTogetherGuest = isListenTogetherGuest) } } }
                    Spacer(Modifier.height(30.dp))
                }
            }
        }
        if (isFullScreen && showInlineLyrics) Box(modifier = Modifier.fillMaxWidth().height(queueSheetState.collapsedBound + 60.dp).align(Alignment.BottomCenter).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { isFullScreen = false })
        AnimatedVisibility(visible = !isFullScreen, enter = slideInVertically(initialOffsetY = { it }) + fadeIn(), exit = shrinkVertically(shrinkTowards = Alignment.Top) + slideOutVertically(targetOffsetY = { it }) + fadeOut()) {
            Queue(state = queueSheetState, playerBottomSheetState = state, navController = navController, background = if (useBlackBackground) Color.Black else MaterialTheme.colorScheme.surfaceContainer, onBackgroundColor = onBackgroundColor, TextBackgroundColor = TextBackgroundColor, textButtonColor = textButtonColor, iconButtonColor = iconButtonColor, pureBlack = pureBlack, showInlineLyrics = showInlineLyrics, playerBackground = playerBackground, isLyricsLoading = mediaMetadata != null && currentLyrics == null, isQueueActive = showQueue, onToggleLyrics = { showInlineLyrics = !showInlineLyrics; if (showInlineLyrics) showQueue = false }, onToggleQueue = { showQueue = !showQueue; if (showQueue) showInlineLyrics = false })
        }
    }
}

@Composable
fun InlineLyricsView(mediaMetadata: MediaMetadata?, showLyrics: Boolean, positionProvider: () -> Long, onShowOptionsMenu: () -> Unit = {}, isFullScreen: Boolean = false, onExitFullScreen: () -> Unit = {}, textButtonColor: Color, iconButtonColor: Color) {
    val playerConnection = LocalPlayerConnection.current ?: return; val currentLyrics by playerConnection.currentLyrics.collectAsState(initial = null); val context = LocalContext.current; val database = LocalDatabase.current; val coroutineScope = rememberCoroutineScope(); val pillsController = remember { LyricsPillController() }
    LaunchedEffect(mediaMetadata?.id, currentLyrics) { if (mediaMetadata != null && currentLyrics == null) { delay(500); coroutineScope.launch(Dispatchers.IO) { try { val entryPoint = EntryPointAccessors.fromApplication(context.applicationContext, com.metrolist.music.di.LyricsHelperEntryPoint::class.java); val lyricsHelper = entryPoint.lyricsHelper(); val fetchedLyricsWithProvider = lyricsHelper.getLyrics(mediaMetadata); database.query { upsert(LyricsEntity(mediaMetadata.id, fetchedLyricsWithProvider.lyrics, fetchedLyricsWithProvider.provider)) } } catch (e: Exception) {} } } }
    InlinePlayerPageFrame(
        pills = { PlayerPill(icon = R.drawable.more_vert, isActive = false, textButtonColor = textButtonColor, iconButtonColor = iconButtonColor, modifier = Modifier.weight(1f), onClick = onShowOptionsMenu); PlayerPill(icon = R.drawable.translate, isActive = pillsController.hasTranslations, textButtonColor = textButtonColor, iconButtonColor = iconButtonColor, modifier = Modifier.weight(1f), onClick = { pillsController.translateAction() }); PlayerPill(icon = R.drawable.lyrics, isActive = pillsController.isSelectionModeActive, textButtonColor = textButtonColor, iconButtonColor = iconButtonColor, modifier = Modifier.weight(1f), onClick = { pillsController.selectionAction() }) },
        content = { ProvideTextStyle(value = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, textAlign = TextAlign.Center, fontFamily = InterFontFamily)) { Lyrics(sliderPositionProvider = positionProvider, showLyrics = showLyrics, onShowOptionsMenu = onShowOptionsMenu, isFullScreen = isFullScreen, onExitFullScreen = onExitFullScreen, showPills = false, pillsController = pillsController) } },
    )
}

@Composable
fun MoreActionsButton(mediaMetadata: MediaMetadata, navController: NavController, state: BottomSheetState, textButtonColor: Color, iconButtonColor: Color) {
    val menuState = LocalMenuState.current
    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(24.dp)).background(textButtonColor).clickable { menuState.show { PlayerMenu(mediaMetadata = mediaMetadata, navController = navController, playerBottomSheetState = state, onShowDetailsDialog = { mediaMetadata.id.let { LocalBottomSheetPageState.current.show { ShowMediaInfo(it) } } }, onDismiss = menuState::dismiss) } }) { Image(painter = painterResource(R.drawable.more_horiz), contentDescription = null, colorFilter = ColorFilter.tint(iconButtonColor)) }
}

@Composable
internal fun PlayerPill(icon: Int, isActive: Boolean, enabled: Boolean = true, textButtonColor: Color, iconButtonColor: Color, onClick: () -> Unit, modifier: Modifier = Modifier, text: String? = null) {
    val bgColor = if (isActive) textButtonColor else Color.Transparent; val iconTint = if (isActive) iconButtonColor else textButtonColor.copy(alpha = if (enabled) 0.8f else 0.4f)
    Box(modifier = modifier.height(42.dp).clip(RoundedCornerShape(50)).background(bgColor).border(1.dp, textButtonColor.copy(alpha = if (enabled) 0.35f else 0.2f), RoundedCornerShape(50)).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) { if (text != null) { Text(text = text, color = iconTint, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().basicMarquee()) } else { Icon(painter = painterResource(icon), contentDescription = null, tint = iconTint, modifier = Modifier.size(17.dp)) } }
}

@Composable
internal fun InlinePlayerPageFrame(modifier: Modifier = Modifier, pills: @Composable RowScope.() -> Unit, content: @Composable BoxScope.() -> Unit) {
    Column(modifier = modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top)).padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 16.dp)) { Spacer(Modifier.fillMaxHeight(0.10f)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 12.dp), content = pills); Box(modifier = Modifier.weight(1f), content = content) }
}
