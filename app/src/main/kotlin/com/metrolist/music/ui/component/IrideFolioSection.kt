package com.metrolist.music.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import coil3.imageLoader
import coil3.request.ImageRequest
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.IrideTabEntrance
import com.metrolist.music.ui.utils.irideDepthEnter
import com.metrolist.music.ui.utils.rememberReducedMotion
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withTimeoutOrNull

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
    val reducedMotion = rememberReducedMotion()
    val blurRadius =
        with(LocalDensity.current) { IrideFolioContextMotionSpec.PanelBlurRadius.toPx() }
    val blurEffects = remember(blurRadius) { ArtistPhotoBlurEffects(blurRadius, TileMode.Decal) }
    val defaultSeen = remember { IrideTabEntrance.sectionsFor("home_blocks") }
    val seen = presentedSections ?: defaultSeen
    val wasPresented = remember(sectionKey) { sectionKey in seen }
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var inViewport by remember(sectionKey) { mutableStateOf(false) }
    var imagesReady by
        remember(sectionKey) {
            mutableStateOf(wasPresented || reducedMotion || (ready && artwork.all { it.isNullOrBlank() }))
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
    LaunchedEffect(sectionKey, ready, inViewport) {
        if (!ready || wasPresented || !inViewport || imagesReady) return@LaunchedEffect
        // Bound first presentation, not the downloads. Slow artwork continues in the same slots.
        val requests =
            artwork.filterNotNull().filter(String::isNotBlank).distinct().take(4).map { url ->
                scope.async {
                    context.imageLoader.execute(ImageRequest.Builder(context).data(url).build())
                }
            }
        withTimeoutOrNull(IrideMotion.SectionArtworkBudgetMillis) { requests.awaitAll() }
        imagesReady = true
    }
    LaunchedEffect(sectionKey, ready, imagesReady, reducedMotion, inViewport, entranceReady) {
        if (!ready || !imagesReady || !inViewport || !entranceReady) return@LaunchedEffect
        if (reducedMotion || wasPresented) progress.snapTo(1f)
        else {
            entranceOrder?.awaitTurn(sectionKey)
            progress.animateTo(
                1f,
                spring(dampingRatio = 1f, stiffness = IrideMotion.SectionEntranceStiffness),
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
        Box(blockModifier) {
            Column(
                Modifier.fillMaxWidth()
                    .irideDepthEnter({ if (imagesReady) progress.value else 0f })
                    .graphicsLayer {
                        renderEffect =
                            if (reducedMotion) null
                            else
                                blurEffects.at(
                                    IrideFolioContextMotionSpec.panelBlurProgress(progress.value)
                                )
                        clip = false
                    },
                content = content,
            )
            if (!imagesReady) {
                Box(Modifier.matchParentSize().clipToBounds()) { placeholder() }
            }
        }
    }
}
