/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.artist

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.metrolist.music.R
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.utils.ComposeToImage
import com.metrolist.music.viewmodels.ArtistGameArtistInfo
import com.metrolist.music.viewmodels.ArtistGameViewModel
import com.metrolist.music.viewmodels.GameUiState
import kotlinx.coroutines.launch

private fun formatTime(ms: Long): String = "%.1fs".format(ms / 1000f)

@Composable
fun ArtistGameScreen(
    navController: NavController,
    viewModel: ArtistGameViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState
    val artistInfo = viewModel.artistInfo
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun shareResult(timeMs: Long, isNewBest: Boolean, correctCount: Int, totalRounds: Int) {
        scope.launch {
            val bitmap = ComposeToImage.createArtistGameResultImage(
                context = context,
                artistThumbnailUrl = artistInfo?.thumbnailUrl,
                artistName = artistInfo?.name.orEmpty(),
                timeText = formatTime(timeMs),
                isNewBest = isNewBest,
                correctCount = correctCount,
                totalRounds = totalRounds,
            )
            val uri = ComposeToImage.saveBitmapAsFile(context, bitmap, "iride_game_${System.currentTimeMillis()}")
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "image/png"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(sendIntent, null))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        when (val state = uiState) {
            is GameUiState.Loading -> LoadingContent()
            is GameUiState.NotEnoughSongs -> NotEnoughSongsContent()
            is GameUiState.Ready -> ReadyContent(
                artistInfo = artistInfo,
                state = state,
                onPlayNow = viewModel::onPlayNowClicked,
                onShare = {
                    val timeMs = state.bestScoreMs
                    val correctCount = state.bestCorrectCount
                    val totalRounds = state.bestTotalRounds
                    if (timeMs != null && correctCount != null && totalRounds != null) {
                        shareResult(timeMs, isNewBest = false, correctCount = correctCount, totalRounds = totalRounds)
                    }
                },
            )
            is GameUiState.Countdown -> CountdownContent(state.value)
            is GameUiState.Playing -> PlayingContent(
                artistName = artistInfo?.name,
                state = state,
                elapsedMs = viewModel.elapsedMs,
                onSelect = viewModel::onOptionSelected,
            )
            is GameUiState.Finished -> FinishedContent(
                artistInfo = artistInfo,
                state = state,
                onShare = { shareResult(state.totalMs, state.isNewBest, state.correctCount, state.totalRounds) },
            )
        }

        IconButton(
            onClick = navController::navigateUp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.arrow_back),
                contentDescription = null,
                tint = Color.White,
            )
        }

        if (uiState is GameUiState.Playing) {
            Text(
                text = formatTime(viewModel.elapsedMs),
                style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp),
                color = Color.Black,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .background(Color.White, RoundedCornerShape(50))
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color.White)
    }
}

@Composable
private fun NotEnoughSongsContent() {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.guess_game_not_enough_songs),
            style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 16.sp),
            color = Color.White,
        )
    }
}

@Composable
private fun ArtistAvatar(thumbnailUrl: String?, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.1f))
            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
    ) {
        if (thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun ReadyContent(
    artistInfo: ArtistGameArtistInfo?,
    state: GameUiState.Ready,
    onPlayNow: () -> Unit,
    onShare: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        ArtistAvatar(artistInfo?.thumbnailUrl, 120.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = artistInfo?.name.orEmpty(),
            style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp),
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.guess_game_subtitle),
            style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 13.sp),
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        if (state.bestScoreMs != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = formatTime(state.bestScoreMs),
                    style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 40.sp),
                    color = Color.White,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.guess_game_best_label),
                    style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 12.sp, letterSpacing = 1.sp),
                    color = Color.White.copy(alpha = 0.5f),
                )
                if (state.bestCorrectCount != null && state.bestTotalRounds != null) {
                    Spacer(Modifier.height(14.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.guess_game_result_correct, state.bestCorrectCount, state.bestTotalRounds),
                            style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp),
                            color = Color.White,
                        )
                        Text(
                            text = stringResource(R.string.guess_game_result_correct_label),
                            style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 11.sp),
                            color = Color.White.copy(alpha = 0.5f),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                IconButton(onClick = onShare) {
                    Icon(painterResource(R.drawable.share), contentDescription = stringResource(R.string.share), tint = Color.White)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(20.dp),
            ) {
                Text(
                    text = stringResource(R.string.guess_game_no_score),
                    style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 15.sp),
                    color = Color.White,
                )
            }
        }
        if (state.preparing) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.guess_game_loading_hint),
                style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 12.sp),
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onPlayNow,
            enabled = !state.preparing,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
        ) {
            if (state.preparing) {
                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    text = stringResource(R.string.guess_game_play_now),
                    style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp),
                )
            }
        }
    }
}

@Composable
private fun CountdownContent(value: Int) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = value,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "countdown",
        ) { count ->
            Text(
                text = count.toString(),
                style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 140.sp),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun PlayingContent(
    artistName: String?,
    state: GameUiState.Playing,
    elapsedMs: Long,
    onSelect: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 80.dp),
    ) {
        Spacer(Modifier.weight(1f))
        if (!artistName.isNullOrBlank()) {
            Text(
                text = artistName,
                style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
        }
        val songsLeft = state.totalRounds - state.roundIndex
        Text(
            text = pluralStringResource(R.plurals.guess_game_songs_left, songsLeft, songsLeft),
            style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.5.sp),
            color = Color.White.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(20.dp))
        state.options.forEach { option ->
            val result = state.result
            val isSelected = result?.selectedId == option.id
            val isCorrectOption = option.id == state.correctId
            val borderColor = when {
                result == null -> Color.White.copy(alpha = 0.25f)
                isSelected && result.correct -> Color(0xFF4CAF50)
                isSelected && !result.correct -> Color(0xFFE53935)
                isCorrectOption -> Color(0xFF4CAF50)
                else -> Color.White.copy(alpha = 0.1f)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(2.dp, borderColor, RoundedCornerShape(14.dp))
                    .clickable(enabled = result == null) { onSelect(option.id) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = option.thumbnail,
                    contentDescription = null,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = option.title,
                    style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 14.sp),
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                )
                if (isSelected) {
                    Icon(
                        painter = painterResource(if (result!!.correct) R.drawable.check else R.drawable.close),
                        contentDescription = null,
                        tint = if (result.correct) Color(0xFF4CAF50) else Color(0xFFE53935),
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (state.result != null && !state.result.correct) {
                stringResource(R.string.guess_game_hint_wrong)
            } else {
                stringResource(R.string.guess_game_hint_default)
            },
            style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 13.sp),
            color = Color.White.copy(alpha = 0.5f),
        )
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun FinishedContent(
    artistInfo: ArtistGameArtistInfo?,
    state: GameUiState.Finished,
    onShare: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ArtistAvatar(artistInfo?.thumbnailUrl, 140.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = artistInfo?.name.orEmpty(),
                style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp),
                color = Color.White,
            )
            if (state.isNewBest) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.guess_game_new_best),
                    style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                    color = Color.Black,
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = formatTime(state.totalMs),
                style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 52.sp),
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.guess_game),
                style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 13.sp, letterSpacing = 1.sp),
                color = Color.White.copy(alpha = 0.5f),
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = stringResource(R.string.guess_game_result_correct, state.correctCount, state.totalRounds),
                        style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = Color.White,
                    )
                    Text(
                        text = stringResource(R.string.guess_game_result_correct_label),
                        style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 11.sp),
                        color = Color.White.copy(alpha = 0.5f),
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatTime(state.bestScoreMs),
                        style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = Color.White,
                    )
                    Text(
                        text = stringResource(R.string.guess_game_best_label),
                        style = TextStyle(fontFamily = SpaceMonoFontFamily, fontSize = 11.sp),
                        color = Color.White.copy(alpha = 0.5f),
                    )
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onShare,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
        ) {
            Icon(painterResource(R.drawable.share), contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.share),
                style = TextStyle(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp),
            )
        }
    }
}
