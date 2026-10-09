package com.metrolist.music.ui.player

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.*
import com.metrolist.music.db.entities.LYRICS_OFFSET_BIAS_MS
import com.metrolist.music.lyrics.LyricsTranslationHelper
import com.metrolist.music.ui.component.LyricsPillController
import com.metrolist.music.ui.screens.settings.defaultList
import com.metrolist.music.ui.utils.rememberReducedMotion
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.LyricsMenuViewModel
import com.metrolist.music.viewmodels.LyricsSearchStatus
import com.metrolist.music.viewmodels.LyricsViewModel
import kotlinx.coroutines.flow.combine
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream

@Composable
internal fun IrideTvLyrics(
    position: Long,
    controller: LyricsPillController,
    onOptions: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LyricsViewModel = hiltViewModel(),
) {
    val connection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val database = LocalDatabase.current
    val scope = rememberCoroutineScope()
    val metadata by connection.mediaMetadata.collectAsState()
    val playing by connection.isPlaying.collectAsState()
    val song by connection.currentSong.collectAsState(initial = null)
    val entity by connection.currentLyrics.collectAsState(initial = null)
    val lines by viewModel.lines.collectAsState()
    val displayedLyrics by viewModel.displayedLyrics.collectAsState()
    val status by viewModel.lyricsSearchStatus.collectAsState()
    val romanizeList by rememberPreference(LyricsRomanizeList, "")
    val romanizeByLine by rememberPreference(LyricsRomanizeCyrillicByLineKey, false)
    val romanizeAsMain by rememberPreference(LyricsRomanizeAsMainKey, false)
    val romanizeEnabled by rememberPreference(LyricsRomanizeToggleKey, false)
    val showIntervals by rememberPreference(ShowIntervalIndicatorKey, true)
    val language by rememberPreference(TranslateLanguageKey, "en")
    val translationMode by rememberPreference(TranslateModeKey, "Literal")
    val menuModel: LyricsMenuViewModel = hiltViewModel()
    val refetch by menuModel.refetchRequested.collectAsState()
    val reducedMotion = rememberReducedMotion()
    val languages = remember(romanizeList) {
        if (romanizeList.isBlank()) defaultList.filter { it.second }.map { it.first }
        else romanizeList.split(',').mapNotNull { item ->
            val parts = item.split(':')
            parts.firstOrNull()?.takeIf { parts.getOrNull(1).toBoolean() }
        }
    }
    LaunchedEffect(metadata?.id, languages, romanizeByLine, showIntervals) {
        metadata?.let { viewModel.loadProgressiveLyrics(it, languages, romanizeByLine, showIntervals) }
    }
    LaunchedEffect(refetch) {
        if (refetch) {
            menuModel.clearRefetchRequest()
            metadata?.let { viewModel.loadProgressiveLyrics(it, languages, romanizeByLine, showIntervals, force = true) }
        }
    }
    LaunchedEffect(lines, entity, language, translationMode) {
        LyricsTranslationHelper.loadTranslationsFromDatabase(lines, entity, language, translationMode)
    }
    // Observe streaming translations and asynchronously generated romanizations without polling.
    val texts by produceState<List<Pair<String?, String?>>>(emptyList(), lines) {
        value = lines.map { it.romanizedTextFlow.value to it.translatedTextFlow.value }
        if (lines.isNotEmpty()) {
            combine(lines.map { line ->
                combine(line.romanizedTextFlow, line.translatedTextFlow) { romanized, translated -> romanized to translated }
            }) { it.toList() }.collect { value = it }
        }
    }
    var webView by remember { mutableStateOf<TvLyricsWebView?>(null) }
    val latestOptions by rememberUpdatedState(onOptions)
    SideEffect {
        controller.hasTranslations = texts.any { !it.second.isNullOrBlank() }
        controller.isSelectionModeActive = false
        controller.isRomanizeActive = romanizeEnabled
        controller.scrollAction = { webView?.script("window.IrideTV?.scroll($it)") }
        controller.translateAction = {
            if (controller.hasTranslations) {
                entity?.let { current -> database.query { upsert(LyricsTranslationHelper.clearTranslations(current)) } }
                lines.forEach { it.translatedTextFlow.value = null }
                LyricsTranslationHelper.triggerClearTranslations()
            } else {
                val settings = context.dataStore
                val provider = settings[AiProviderKey] ?: "Gemini"
                val apiKey = settings[OpenRouterApiKey].orEmpty()
                val deeplKey = settings[DeeplApiKey].orEmpty()
                if ((if (provider == "DeepL") deeplKey else apiKey).isBlank()) latestOptions()
                else LyricsTranslationHelper.translateLyrics(
                    lyrics = lines, targetLanguage = language, apiKey = apiKey,
                    baseUrl = settings[OpenRouterBaseUrlKey] ?: OpenRouterDefaultBaseUrl,
                    model = settings[OpenRouterModelKey] ?: OpenRouterDefaultModel,
                    mode = translationMode, scope = scope, context = context, provider = provider,
                    deeplApiKey = deeplKey, deeplFormality = settings[DeeplFormalityKey] ?: "default",
                    songId = metadata?.id.orEmpty(), database = database,
                    systemPrompt = settings[AiSystemPromptKey].orEmpty(),
                )
            }
        }
    }
    DisposableEffect(Unit) {
        LyricsTranslationHelper.setCompositionActive(true)
        onDispose {
            LyricsTranslationHelper.setCompositionActive(false)
            LyricsTranslationHelper.cancelTranslation()
            controller.translateAction = {}
            controller.scrollAction = {}
        }
    }
    val payload = remember(lines, texts, romanizeAsMain, romanizeEnabled) {
        JSONArray().apply {
            lines.forEachIndexed { index, line ->
                put(JSONObject().apply {
                    put("time", line.time); put("text", line.text); put("background", line.isBackground)
                    put("translation", texts.getOrNull(index)?.second.orEmpty())
                    put("romanized", if (romanizeAsMain && romanizeEnabled) texts.getOrNull(index)?.first.orEmpty() else "")
                    put("words", JSONArray().apply {
                        line.words.orEmpty().forEach { word ->
                            put(JSONObject().apply {
                                put("text", word.text); put("start", word.startTime * 1000)
                                put("end", word.endTime * 1000); put("space", word.hasTrailingSpace)
                            })
                        }
                    })
                })
            }
        }.toString()
    }
    val synced = displayedLyrics?.trimStart()?.startsWith('[') == true
    val effectivePosition = position + (song?.song?.effectiveLyricsOffset ?: LYRICS_OFFSET_BIAS_MS)
    val emptyMessage = stringResource(
        if (status == LyricsSearchStatus.Loading || status == LyricsSearchStatus.Idle) R.string.mp3_lyrics_loading
        else R.string.lyrics_not_found,
    )
    LaunchedEffect(webView, payload, synced, metadata?.id) {
        webView?.updateLyrics(payload, synced)
    }
    LaunchedEffect(webView, effectivePosition, playing, reducedMotion, emptyMessage) {
        webView?.updatePlayback(effectivePosition, playing, reducedMotion, emptyMessage)
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(webView, lifecycle) {
        val currentView = webView
        val observer = LifecycleEventObserver { _, _ ->
            currentView?.setActive(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        }
        lifecycle.addObserver(observer)
        // A visible player/dialog can have a STARTED navigation owner while the activity is resumed.
        currentView?.setActive(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        onDispose { lifecycle.removeObserver(observer) }
    }
    val accessibleText = if (synced) lines.lastOrNull { !it.isBackground && it.time <= effectivePosition }?.text.orEmpty()
        else lines.firstOrNull()?.text.orEmpty()
    val previousLabel = stringResource(R.string.mp3_lyrics_previous_line)
    val nextLabel = stringResource(R.string.mp3_lyrics_next_line)
    Box(modifier, contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { TvLyricsWebView(it).also { view -> webView = view } },
            // A window change must retain the live page, including manual browsing and rain.
            onReset = {},
            onRelease = { it.release() },
            modifier = Modifier.fillMaxSize()
                .semantics {
                    contentDescription = accessibleText.ifBlank { emptyMessage }
                    customActions = listOf(
                        CustomAccessibilityAction(previousLabel) { controller.scrollAction(-1); true },
                        CustomAccessibilityAction(nextLabel) { controller.scrollAction(1); true },
                    )
                }
                .pointerInput(webView) {
                    var distance = 0f
                    detectVerticalDragGestures(
                        onDragStart = { distance = 0f },
                        onDragEnd = {
                            if (kotlin.math.abs(distance) > 24.dp.toPx()) {
                                controller.scrollAction(if (distance < 0) 1 else -1)
                            }
                        },
                        onVerticalDrag = { change, delta -> change.consume(); distance += delta },
                    )
                },
        )
    }
}

/** Serves only bundled assets. No network, JS bridge, file access, or navigation. */
@SuppressLint("SetJavaScriptEnabled")
private class TvLyricsWebView(context: Context) : WebView(context) {
    private var loaded = false
    private var lyrics = "{}"
    private var playback = "{}"
    private var active = true

    init {
        setBackgroundColor(android.graphics.Color.rgb(8, 9, 10))
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        settings.apply {
            javaScriptEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            blockNetworkLoads = true
            domStorageEnabled = false
        }
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) = true
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse {
                val uri = request?.url
                val name = uri?.path?.removePrefix("/tv_lyrics/")
                val allowed = setOf("index.html", "tv-lyrics.css", "tv-lyrics.js", "tv-lyrics-model.js", "raindrop-fx.js", "rain-static.webp", "anton.ttf")
                if (uri?.scheme != "https" || uri.host != "iride.invalid" || name !in allowed) {
                    return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(byteArrayOf()))
                }
                return try {
                    val stream = if (name == "anton.ttf") context.resources.openRawResource(R.font.anton)
                        else context.assets.open("tv_lyrics/$name")
                    val mime = when (name?.substringAfterLast('.')) {
                        "html" -> "text/html"
                        "css" -> "text/css"
                        "js" -> "application/javascript"
                        "ttf" -> "font/ttf"
                        else -> "image/webp"
                    }
                    WebResourceResponse(mime, "utf-8", stream)
                } catch (_: java.io.IOException) {
                    WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(byteArrayOf()))
                }
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                loaded = true
                script("window.IrideTV?.update($lyrics)")
                script("window.IrideTV?.update($playback)")
                setActive(active)
            }
        }
        loadUrl("https://iride.invalid/tv_lyrics/index.html")
    }

    override fun onTouchEvent(event: MotionEvent?) = false

    fun script(code: String) { if (loaded) evaluateJavascript(code, null) }

    fun updateLyrics(lines: String, synced: Boolean) {
        lyrics = "{\"lines\":$lines,\"synced\":$synced}"
        script("window.IrideTV?.update($lyrics)")
    }

    fun updatePlayback(position: Long, playing: Boolean, reducedMotion: Boolean, message: String) {
        playback = JSONObject().put("position", position).put("playing", playing)
            .put("reducedMotion", reducedMotion).put("message", message).toString()
        script("window.IrideTV?.update($playback)")
    }

    fun setActive(value: Boolean) {
        active = value
        if (value) onResume() else onPause()
        script("window.IrideTV?.update({visible:$value})")
    }

    fun release() {
        script("window.IrideTV?.dispose()")
        loaded = false
        stopLoading()
        destroy()
    }
}
