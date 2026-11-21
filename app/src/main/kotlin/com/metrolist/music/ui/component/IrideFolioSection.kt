package com.metrolist.music.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.size.Precision
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.IrideTabEntrance
import com.metrolist.music.ui.utils.irideDepthEnter
import com.metrolist.music.ui.utils.rememberReducedMotion
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withTimeoutOrNull

/** Home reveals stable shelves immediately while warming artwork in the background. */
val LocalFolioSectionFastReveal = staticCompositionLocalOf { false }

/**
 * One entrance owns the heading, controls, artwork and footer. Images warm alongside composition.
 */
@Composable
fun IrideFolioSection(
    sectionKey: String,
    ready: Boolean = true,
    entranceReady: Boolean = true,
    presentedSections: MutableSet<String>? = null,
    artwork: List<String?> = emptyList(),
    modifier: Modifier = Modifier,
    placeholder: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val entranceOrder = LocalFolioSectionEntranceOrder.current
    val fastReveal = LocalFolioSectionFastReveal.current
    val reducedMotion = rememberReducedMotion()
    val context = LocalContext.current
    val lightweightEntrances by rememberLightweightFolioMode()
    val blurRadius =
        with(LocalDensity.current) { IrideFolioContextMotionSpec.PanelBlurRadius.toPx() }
    val blurEffects = remember(blurRadius) { ArtistPhotoBlurEffects(blurRadius, TileMode.Decal) }
    val defaultSeen = remember { IrideTabEntrance.sectionsFor("home_blocks") }
    val seen = presentedSections ?: defaultSeen
    val wasPresented = remember(sectionKey) { sectionKey in seen }
    val view = LocalView.current
    var inViewport by remember(sectionKey) { mutableStateOf(false) }
    var imagesReady by
        remember(sectionKey) {
            mutableStateOf(
                fastReveal || wasPresented || reducedMotion ||
                    (ready && artwork.all { it.isNullOrBlank() }),
            )
        }
    val progress =
        remember(sectionKey) { Animatable(if (wasPresented || reducedMotion) 1f else 0f) }
    DisposableEffect(entranceOrder, sectionKey) {
        onDispose { entranceOrder?.remove(sectionKey) }
    }
    SideEffect {
        if (wasPresented || reducedMotion) entranceOrder?.presented(sectionKey)
        entranceOrder?.readiness(sectionKey, ready && imagesReady && entranceReady)
    }
    LaunchedEffect(sectionKey, ready, fastReveal, artwork, lightweightEntrances) {
        if (!fastReveal || !ready) return@LaunchedEffect
        // Warm visible Home artwork before the user reaches each shelf; never gate its layout.
        val artworkLimit = if (sectionKey == "for_you_section") 8 else 4
        val artworkSize = if (sectionKey == "for_you_section") 300 else if (lightweightEntrances) 256 else 384
        artwork.filterNotNull().filter(String::isNotBlank).distinct().take(artworkLimit).map { url ->
            async {
                context.imageLoader.execute(
                    ImageRequest.Builder(context).data(url).size(artworkSize, artworkSize)
                        .precision(Precision.INEXACT).build()
                )
            }
        }.awaitAll()
    }
    LaunchedEffect(sectionKey, ready, inViewport, lightweightEntrances, fastReveal) {
        if (fastReveal) {
            imagesReady = true
            return@LaunchedEffect
        }
        if (!ready || wasPresented || !inViewport || imagesReady) return@LaunchedEffect
        // Bound first presentation, not the downloads. Slow artwork continues in the same slots.
        val artworkLimit = if (lightweightEntrances) 2 else 4
        val artworkSize = if (lightweightEntrances) 256 else 384
        withTimeoutOrNull(IrideMotion.SectionArtworkBudgetMillis) {
            artwork.filterNotNull().filter(String::isNotBlank).distinct().take(artworkLimit).map { url ->
                async {
                    context.imageLoader.execute(
                        ImageRequest.Builder(context).data(url).size(artworkSize, artworkSize)
                            .precision(Precision.INEXACT).build()
                    )
                }
            }.awaitAll()
        }
        imagesReady = true
    }
    LaunchedEffect(sectionKey, ready, imagesReady, reducedMotion, inViewport, entranceReady) {
        if (!ready || !imagesReady || !inViewport || !entranceReady) return@LaunchedEffect
        if (reducedMotion || wasPresented) progress.snapTo(1f)
        else {
            entranceOrder?.awaitTurn(sectionKey)
            progress.animateTo(
                1f,
                if (lightweightEntrances && !fastReveal) tween(160)
                else spring(dampingRatio = 1f, stiffness = IrideMotion.SectionEntranceStiffness),
            )
        }
        seen.add(sectionKey)
        entranceOrder?.presented(sectionKey)
    }
    val blockModifier =
        modifier.fillMaxWidth().onGloballyPositioned { coordinates ->
            val bounds = coordinates.boundsInWindow()
            inViewport = bounds.height > 0f && bounds.bottom > 0f && bounds.top < view.height
            entranceOrder?.position(sectionKey, bounds.top, inViewport, ready && imagesReady && entranceReady)
        }
    // After first presentation, refresh only the body; keep mood controls and headings usable.
    if (!ready && sectionKey !in seen) {
        Box(blockModifier) { placeholder() }
    } else {
        val contentVisible by remember(progress) {
            derivedStateOf { imagesReady && progress.value >= 0.95f }
        }
        val entranceModifier = if (lightweightEntrances && !fastReveal) {
            // Reduced mode uses a classic alpha dissolve without moving or filtering the section.
            Modifier.graphicsLayer {
                alpha = progress.value.coerceIn(0f, 1f)
            }
        } else {
            Modifier.irideDepthEnter(
                progress = { if (imagesReady) progress.value else 0f },
                effect = {
                    if (reducedMotion) null
                    else blurEffects.at(
                        IrideFolioContextMotionSpec.panelBlurProgress(progress.value)
                    )
                },
            )
        }
        Box(blockModifier) {
            Column(
                Modifier.fillMaxWidth()
                    .then(if (contentVisible) Modifier else Modifier.clearAndSetSemantics { })
                    .then(entranceModifier),
                content = content,
            )
            if (!imagesReady) {
                Box(Modifier.matchParentSize().clipToBounds()) { placeholder() }
            }
            if (!contentVisible) {
                Box(
                    Modifier.matchParentSize().pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) awaitPointerEvent()
                        }
                    }
                )
            }
        }
    }
}
