/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.recognition

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.music.LocalDatabase
import com.metrolist.music.R
import com.metrolist.music.db.entities.RecognitionHistory
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.SettingsBackTopBar
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberPreference
import com.metrolist.shazamkit.models.RecognitionResult
import com.metrolist.shazamkit.models.RecognitionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecognitionScreen(
    navController: NavController,
    autoStart: Boolean = false,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (com.metrolist.music.recognition.MusicRecognitionService.recognitionStatus.value
                is RecognitionStatus.Ready
        ) {
            com.metrolist.music.recognition.MusicRecognitionService
                .reset()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            com.metrolist.music.recognition.MusicRecognitionService
                .reset()
        }
    }

    val recognitionStatus by com.metrolist.music.recognition.MusicRecognitionService.recognitionStatus
        .collectAsState()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            hasPermission = isGranted
            if (isGranted) {
                coroutineScope.launch {
                    com.metrolist.music.recognition.MusicRecognitionService
                        .recognize(context)
                }
            }
        }

    fun startRecognition() {
        if (hasPermission) {
            coroutineScope.launch {
                com.metrolist.music.recognition.MusicRecognitionService
                    .recognize(context)
            }
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(Unit) {
        if (autoStart &&
            com.metrolist.music.recognition.MusicRecognitionService.recognitionStatus.value
                is RecognitionStatus.Ready
        ) {
            startRecognition()
        }
    }

    fun resetToReady() {
        com.metrolist.music.recognition.MusicRecognitionService
            .reset()
    }

    fun saveToHistory(result: RecognitionResult) {
        if (com.metrolist.music.recognition.MusicRecognitionService.resultSavedExternally) return
        coroutineScope.launch(Dispatchers.IO) {
            database.query {
                insert(
                    RecognitionHistory(
                        trackId = result.trackId,
                        title = result.title,
                        artist = result.artist,
                        album = result.album,
                        coverArtUrl = result.coverArtUrl,
                        coverArtHqUrl = result.coverArtHqUrl,
                        genre = result.genre,
                        releaseDate = result.releaseDate,
                        label = result.label,
                        shazamUrl = result.shazamUrl,
                        appleMusicUrl = result.appleMusicUrl,
                        spotifyUrl = result.spotifyUrl,
                        isrc = result.isrc,
                        youtubeVideoId = result.youtubeVideoId,
                        recognizedAt = LocalDateTime.now(),
                    ),
                )
            }
        }
    }

    Scaffold(
        topBar = {
            SettingsBackTopBar(
                title = stringResource(R.string.recognize_music),
                navController = navController,
                actions = {
                    IconButton(onClick = { navController.navigate("recognition_history") }) {
                        Icon(
                            painter = painterResource(R.drawable.history),
                            contentDescription = stringResource(R.string.recognition_history),
                            tint = Color.White.copy(alpha = 0.85f),
                        )
                    }
                },
            )
        },
        containerColor = Color.Black,
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AnimatedContent(
                targetState = recognitionStatus,
                transitionSpec = {
                    (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
                },
                label = "recognition_content",
            ) { status ->
                when (status) {
                    is RecognitionStatus.Ready -> {
                        ReadyState(
                            onStartRecognition = ::startRecognition,
                            useIrideStyle = true,
                        )
                    }

                    is RecognitionStatus.Listening -> {
                        ListeningState(
                            onCancel = {
                                com.metrolist.music.recognition.MusicRecognitionService
                                    .reset()
                            },
                            useIrideStyle = true,
                        )
                    }

                    is RecognitionStatus.Processing -> {
                        ProcessingState(useIrideStyle = true)
                    }

                    is RecognitionStatus.Success -> {
                        SuccessState(
                            result = status.result,
                            onPlayOnApp = { result ->
                                val searchQuery = "${result.title} ${result.artist}"
                                navController.navigate("search/${java.net.URLEncoder.encode(searchQuery, "UTF-8")}")
                            },
                            onTryAgain = {
                                startRecognition()
                            },
                            onClose = ::resetToReady,
                            onSaveToHistory = ::saveToHistory,
                            useIrideStyle = true,
                        )
                    }

                    is RecognitionStatus.NoMatch -> {
                        NoMatchState(
                            message = status.message,
                            onTryAgain = {
                                startRecognition()
                            },
                            useIrideStyle = true,
                        )
                    }

                    is RecognitionStatus.Error -> {
                        ErrorState(
                            message = status.message,
                            onTryAgain = {
                                startRecognition()
                            },
                            useIrideStyle = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadyState(onStartRecognition: () -> Unit, useIrideStyle: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .then(
                        if (useIrideStyle) {
                            Modifier
                                .background(Color.Transparent)
                                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        } else {
                            Modifier.background(
                                Brush.radialGradient(
                                    colors =
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            Color.Transparent,
                                        ),
                                ),
                            )
                        },
                    ).clickable { onStartRecognition() },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(
                            if (useIrideStyle) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.primary,
                        )
                        .then(
                            if (useIrideStyle) {
                                Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            } else {
                                Modifier
                            },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.mic),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = if (useIrideStyle) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onPrimary,
                )
            }
        }

        Text(
            text = stringResource(R.string.tap_to_recognize),
            style = if (useIrideStyle) {
                MaterialTheme.typography.titleMedium.copy(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.titleMedium
            },
            color = if (useIrideStyle) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ListeningState(onCancel: () -> Unit, useIrideStyle: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "scale",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Box(
            modifier = Modifier.size(260.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(200.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(
                            if (useIrideStyle) Color.White.copy(alpha = 0.10f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        ),
            )

            Box(
                modifier =
                    Modifier
                        .size(180.dp)
                        .scale(scale * 0.9f)
                        .clip(CircleShape)
                        .background(
                            if (useIrideStyle) Color.White.copy(alpha = 0.14f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        ),
            )

            Box(
                modifier =
                    Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(if (useIrideStyle) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary)
                        .then(
                            if (useIrideStyle) {
                                Modifier.border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            } else {
                                Modifier
                            },
                        )
                        .clickable { onCancel() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.mic),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = if (useIrideStyle) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onPrimary,
                )
            }
        }

        Text(
            text = stringResource(R.string.listening),
            style = if (useIrideStyle) {
                MaterialTheme.typography.titleMedium.copy(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.titleMedium
            },
            color = if (useIrideStyle) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary,
        )

        if (useIrideStyle) {
            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(5.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.9f)),
            ) {
                Text(stringResource(R.string.cancel), fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold)
            }
        } else {
            OutlinedButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

@Composable
private fun ProcessingState(useIrideStyle: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "rotate")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                ),
            label = "rotation",
        )

        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .border(
                            width = 4.dp,
                            brush =
                                if (useIrideStyle) {
                                    Brush.sweepGradient(
                                        colors =
                                            listOf(
                                                Color.White.copy(alpha = 0.9f),
                                                Color.White.copy(alpha = 0.4f),
                                                Color.Transparent,
                                                Color.White.copy(alpha = 0.4f),
                                                Color.White.copy(alpha = 0.9f),
                                            ),
                                    )
                                } else {
                                    Brush.sweepGradient(
                                        colors =
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                Color.Transparent,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                MaterialTheme.colorScheme.primary,
                                            ),
                                    )
                                },
                            shape = CircleShape,
                        ),
            )

            Icon(
                painter = painterResource(R.drawable.music_note),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (useIrideStyle) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary,
            )
        }

        Text(
            text = stringResource(R.string.processing),
            style = if (useIrideStyle) {
                MaterialTheme.typography.titleMedium.copy(fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.titleMedium
            },
            color = if (useIrideStyle) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SuccessState(
    result: RecognitionResult,
    onPlayOnApp: (RecognitionResult) -> Unit,
    onTryAgain: () -> Unit,
    onClose: () -> Unit,
    onSaveToHistory: (RecognitionResult) -> Unit,
    useIrideStyle: Boolean = false,
) {
    LaunchedEffect(result) {
        onSaveToHistory(result)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        Card(
            modifier =
                Modifier
                    .size(180.dp)
                    .aspectRatio(1f),
            shape = RoundedCornerShape(if (useIrideStyle) 5.dp else com.metrolist.music.constants.ThumbnailCornerRadius),
            elevation = if (useIrideStyle) {
                CardDefaults.cardElevation(defaultElevation = 0.dp)
            } else {
                CardDefaults.cardElevation(defaultElevation = 8.dp)
            },
        ) {
            AsyncImage(
                model = result.coverArtHqUrl ?: result.coverArtUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = result.title,
            style = if (useIrideStyle) {
                MaterialTheme.typography.headlineSmall.copy(fontFamily = SpaceMonoFontFamily)
            } else {
                MaterialTheme.typography.headlineSmall
            },
            fontWeight = FontWeight.Bold,
            color = if (useIrideStyle) Color.White.copy(alpha = 0.95f) else Color.Unspecified,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = result.artist,
            style = if (useIrideStyle) {
                MaterialTheme.typography.titleMedium.copy(fontFamily = SpaceMonoFontFamily)
            } else {
                MaterialTheme.typography.titleMedium
            },
            color = if (useIrideStyle) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        result.album?.let { album ->
            Text(
                text = album,
                style = MaterialTheme.typography.bodyMedium,
                color = if (useIrideStyle) Color.White.copy(alpha = 0.45f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (useIrideStyle) {
                Button(
                    onClick = { onPlayOnApp(result) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(5.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.play_on_app), fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onTryAgain,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(5.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.9f)),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.mic),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.re_listen), fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(5.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.6f)),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.close), fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { onPlayOnApp(result) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.play_on_app))
                }

                FilledTonalButton(
                    onClick = onTryAgain,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.mic),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.re_listen))
                }

                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}

@Composable
private fun NoMatchState(
    message: String,
    onTryAgain: () -> Unit,
    useIrideStyle: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(if (useIrideStyle) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.errorContainer)
                    .then(
                        if (useIrideStyle) {
                            Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        } else {
                            Modifier
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.close),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (useIrideStyle) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onErrorContainer,
            )
        }

        Text(
            text = stringResource(R.string.no_match_found),
            style = if (useIrideStyle) {
                MaterialTheme.typography.titleLarge.copy(fontFamily = SpaceMonoFontFamily)
            } else {
                MaterialTheme.typography.titleLarge
            },
            fontWeight = FontWeight.Bold,
            color = if (useIrideStyle) Color.White.copy(alpha = 0.9f) else Color.Unspecified,
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (useIrideStyle) Color.White.copy(alpha = 0.55f) else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )

        if (useIrideStyle) {
            OutlinedButton(
                onClick = onTryAgain,
                shape = RoundedCornerShape(5.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.9f)),
            ) {
                Icon(
                    painter = painterResource(R.drawable.refresh),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.try_again), fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(onClick = onTryAgain) {
                Icon(
                    painter = painterResource(R.drawable.refresh),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.try_again))
            }
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onTryAgain: () -> Unit,
    useIrideStyle: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(if (useIrideStyle) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.errorContainer)
                    .then(
                        if (useIrideStyle) {
                            Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        } else {
                            Modifier
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.error),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (useIrideStyle) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onErrorContainer,
            )
        }

        Text(
            text = stringResource(R.string.recognition_error),
            style = if (useIrideStyle) {
                MaterialTheme.typography.titleLarge.copy(fontFamily = SpaceMonoFontFamily)
            } else {
                MaterialTheme.typography.titleLarge
            },
            fontWeight = FontWeight.Bold,
            color = if (useIrideStyle) Color.White.copy(alpha = 0.9f) else Color.Unspecified,
        )

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (useIrideStyle) Color.White.copy(alpha = 0.55f) else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )

        if (useIrideStyle) {
            OutlinedButton(
                onClick = onTryAgain,
                shape = RoundedCornerShape(5.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.9f)),
            ) {
                Icon(
                    painter = painterResource(R.drawable.refresh),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.try_again), fontFamily = SpaceMonoFontFamily, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(onClick = onTryAgain) {
                Icon(
                    painter = painterResource(R.drawable.refresh),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.try_again))
            }
        }
    }
}
