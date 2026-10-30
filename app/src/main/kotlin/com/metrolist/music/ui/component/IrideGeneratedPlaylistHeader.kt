package com.metrolist.music.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.util.lerp
import coil3.compose.AsyncImage
import com.metrolist.music.constants.IrideBaseBorderWidth
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily
import com.metrolist.music.ui.theme.strokeCard
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.irideArtworkOverlayBorder
import com.metrolist.music.ui.utils.rememberEnterProgress
import sv.lib.squircleshape.SquircleShape

/** Shared artwork, title and metadata layout for automatic playlists and daily charts. */
@Composable
fun IrideGeneratedPlaylistHeader(
    title: String,
    thumbnails: List<String>,
    metadataLine: String,
    @DrawableRes badgeIcon: Int? = null,
    modifier: Modifier = Modifier,
    artwork: (@Composable () -> Unit)? = null,
) {
    val coverShape = SquircleShape(radius = IrideFolio.GeneratedPlaylistArtworkRadius, cornerSmoothing = IrideFolio.GeneratedPlaylistArtworkSmoothing)
    val animatedCoverProgress = rememberEnterProgress(
        play = true,
        durationMillis = 420,
        easing = IrideMotion.EaseOutQuart,
    )
    val coverProgress = if (LocalCollectionExpansionProgress.current != null) 1f else animatedCoverProgress
    var coverLoaded by remember(thumbnails.firstOrNull()) { mutableStateOf(thumbnails.isEmpty()) }
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = IrideFolio.HorizontalPadding)
            .padding(top = IrideFolio.GeneratedPlaylistTopPadding, bottom = IrideFolio.GeneratedPlaylistBottomPadding),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().graphicsLayer {
                alpha = coverProgress
                val scale = lerp(0.94f, 1f, coverProgress)
                scaleX = scale
                scaleY = scale
            },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(IrideFolio.GeneratedPlaylistArtworkSize).collectionExpansionTarget(
                ready = artwork != null || badgeIcon != null || coverLoaded,
            )) {
                if (artwork != null) {
                    Surface(
                        modifier = Modifier.size(IrideFolio.GeneratedPlaylistArtworkSize)
                            .irideArtworkOverlayBorder(IrideBaseBorderWidth, MaterialTheme.colorScheme.strokeCard, coverShape)
                            .shadow(IrideFolio.GeneratedPlaylistArtworkShadow, coverShape),
                        shape = coverShape,
                        color = MaterialTheme.colorScheme.surface,
                    ) { artwork() }
                } else if (badgeIcon != null) {
                    GlassPlaylistCover(
                        thumbnails = thumbnails,
                        icon = badgeIcon,
                        size = IrideFolio.GeneratedPlaylistArtworkSize,
                        shape = coverShape,
                        iconSizeFraction = IrideFolio.GeneratedPlaylistBadgeSizeFraction,
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(IrideFolio.GeneratedPlaylistArtworkSize)
                            .irideArtworkOverlayBorder(IrideBaseBorderWidth, MaterialTheme.colorScheme.strokeCard, coverShape)
                            .shadow(IrideFolio.GeneratedPlaylistArtworkShadow, coverShape, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        shape = coverShape,
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        AsyncImage(
                            model = thumbnails.firstOrNull(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            onSuccess = { coverLoaded = true },
                            onError = { coverLoaded = true },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(IrideFolio.GeneratedPlaylistTitleGap))
        IrideCollectionTitle(title = title)
        Spacer(Modifier.height(IrideFolio.GeneratedPlaylistMetadataGap))
        Text(
            text = metadataLine,
            style = TextStyle(fontFamily = IBMPlexMonoFontFamily, fontSize = IrideFolio.GeneratedPlaylistMetadataFontSize),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
