package com.metrolist.music.ui.component

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Outline
import android.net.Uri
import android.net.http.SslError
import android.os.Message
import android.util.Base64
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebMessage
import android.webkit.WebMessagePort
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

const val EXPORTIFY_URL = "https://exportify.app/"
private const val EXPORTIFY_ORIGIN = "https://exportify.app"
private const val SPOTIFY_LOGOUT_URL = "https://accounts.spotify.com/logout"
private const val MAX_EXPORT_BYTES = 128L * 1024 * 1024
private const val EXPORT_CHUNK_BYTES = 48 * 1024

private enum class ExportifyAccountChangeStage {
    IDLE,
    SPOTIFY_LOGOUT,
    EXPORTIFY_LOGOUT,
    RESTART,
}

sealed interface ExportifyPageError {
    data class Http(val statusCode: Int) : ExportifyPageError

    data object Connection : ExportifyPageError
}

/**
 * Hosts the official service; the caller supplies the size and surrounding app chrome. [onExport]
 * receives a cache file whose contents and empty parent directory the recipient owns.
 */
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun ExportifyWebView(
    modifier: Modifier = Modifier,
    handleBackNavigation: Boolean = true,
    onExport: (File) -> Unit,
    onError: (String) -> Unit,
    onLoading: (Boolean) -> Unit,
    onDownloadStarted: () -> Unit,
    reloadRequest: Int = 0,
    changeAccountRequest: Int = 0,
    onAccountChangeInProgress: (Boolean) -> Unit = {},
    onPageError: (ExportifyPageError) -> Unit = { onError("Exportify page could not be loaded") },
    onPageHostChanged: (String?) -> Unit = {},
) {
    val exportCallback by rememberUpdatedState(onExport)
    val errorCallback by rememberUpdatedState(onError)
    val loadingCallback by rememberUpdatedState(onLoading)
    val downloadCallback by rememberUpdatedState(onDownloadStarted)
    val pageErrorCallback by rememberUpdatedState(onPageError)
    val pageHostCallback by rememberUpdatedState(onPageHostChanged)
    val accountChangeCallback by rememberUpdatedState(onAccountChangeInProgress)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var browser by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var bridge by remember { mutableStateOf<ExportifyDownloadBridge?>(null) }
    val downloadScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    val popups = remember { mutableSetOf<WebView>() }
    var disposed by remember { mutableStateOf(false) }
    var accountChangeStage by remember { mutableStateOf(ExportifyAccountChangeStage.IDLE) }
    var accountChangeFailed by remember { mutableStateOf(false) }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    browser?.onPause()
                    CookieManager.getInstance().flush()
                }
                Lifecycle.Event.ON_RESUME -> browser?.onResume()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            disposed = true
            lifecycle.removeObserver(observer)
            bridge?.close()
            bridge = null
            downloadScope.cancel()
            popups.forEach { it.destroy() }
            popups.clear()
            browser?.apply {
                stopLoading()
                setDownloadListener(null)
                webChromeClient = null
                destroy()
            }
            browser = null
        }
    }

    BackHandler(
        enabled =
            handleBackNavigation &&
                canGoBack &&
                accountChangeStage == ExportifyAccountChangeStage.IDLE
    ) {
        browser?.goBack()
    }

    LaunchedEffect(reloadRequest) {
        if (reloadRequest > 0)
            browser?.let { view ->
                if (accountChangeStage != ExportifyAccountChangeStage.IDLE) {
                    accountChangeFailed = false
                    accountChangeStage = ExportifyAccountChangeStage.SPOTIFY_LOGOUT
                    accountChangeCallback(true)
                    loadingCallback(true)
                    view.loadUrl(SPOTIFY_LOGOUT_URL)
                } else view.reload()
            }
    }

    LaunchedEffect(changeAccountRequest) {
        if (changeAccountRequest > 0)
            browser?.let { view ->
                view.stopLoading()
                bridge?.close()
                bridge = null
                popups.forEach { it.destroy() }
                popups.clear()
                canGoBack = false
                accountChangeFailed = false
                accountChangeStage = ExportifyAccountChangeStage.SPOTIFY_LOGOUT
                accountChangeCallback(true)
                loadingCallback(true)
                // Spotify owns its login cookies. Its logout endpoint avoids clearing other Iride
                // logins.
                view.loadUrl(SPOTIFY_LOGOUT_URL)
            }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            IrideFolioWebView(context).apply {
                browser = this
                // WRAP_CONTENT makes Chromium resolve viewport-relative CSS heights to zero.
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                // Clip the Android view itself rather than rendering it through a Compose mask.
                outlineProvider =
                    object : ViewOutlineProvider() {
                        override fun getOutline(view: View, outline: Outline) {
                            outline.setRoundRect(
                                0,
                                0,
                                view.width,
                                view.height,
                                16 * resources.displayMetrics.density,
                            )
                        }
                    }
                clipToOutline = true
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    safeBrowsingEnabled = true
                    setSupportMultipleWindows(true)
                    javaScriptCanOpenWindowsAutomatically = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    builtInZoomControls = true
                    displayZoomControls = false
                }
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                setOnTouchListener { view, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN ->
                            view.parent?.requestDisallowInterceptTouchEvent(true)
                        MotionEvent.ACTION_UP,
                        MotionEvent.ACTION_CANCEL ->
                            view.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    false
                }
                webViewClient =
                    object : WebViewClient() {
                        private var navigationId = 0L
                        private var navigationUrl: String? = null

                        private fun revealFrame(view: WebView, url: String?) {
                            // Exportify removes the authorization query with history.replaceState.
                            if (url != navigationUrl && url != view.url) return
                            val requestId = navigationId
                            view.postVisualStateCallback(
                                requestId,
                                object : WebView.VisualStateCallback() {
                                    override fun onComplete(requestId: Long) {
                                        if (!disposed && requestId == navigationId) {
                                            view.requestLayout()
                                            view.invalidate()
                                            loadingCallback(false)
                                        }
                                    }
                                },
                            )
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean {
                            if (!request.isForMainFrame) return false
                            return request.url.scheme != "https"
                        }

                        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                            navigationId++
                            navigationUrl = url
                            bridge?.close()
                            bridge = null
                            loadingCallback(true)
                            pageHostCallback(url?.let { Uri.parse(it).host })
                        }

                        override fun onPageFinished(view: WebView, url: String?) {
                            if (accountChangeFailed) return
                            when (accountChangeStage) {
                                ExportifyAccountChangeStage.SPOTIFY_LOGOUT -> {
                                    val uri = url?.let(Uri::parse)
                                    if (
                                        uri?.scheme == "https" &&
                                            uri.host == "accounts.spotify.com" &&
                                            uri.path in listOf("/logout", "/status") &&
                                            view.url == url
                                    ) {
                                        CookieManager.getInstance().flush()
                                        accountChangeStage =
                                            ExportifyAccountChangeStage.EXPORTIFY_LOGOUT
                                        view.loadUrl(EXPORTIFY_URL)
                                    }
                                    return
                                }
                                ExportifyAccountChangeStage.EXPORTIFY_LOGOUT -> {
                                    if (isExportifyUrl(url) && isExportifyUrl(view.url)) {
                                        view.evaluateJavascript(
                                            "(() => { if (location.origin !== 'https://exportify.app') return false; " +
                                                "localStorage.removeItem('access_token'); localStorage.removeItem('code_verifier'); " +
                                                "return true; })()"
                                        ) { cleared ->
                                            if (
                                                !disposed &&
                                                    accountChangeStage ==
                                                        ExportifyAccountChangeStage
                                                            .EXPORTIFY_LOGOUT &&
                                                    isExportifyUrl(view.url)
                                            ) {
                                                if (cleared == "true") {
                                                    accountChangeStage =
                                                        ExportifyAccountChangeStage.RESTART
                                                    view.loadUrl(
                                                        "${EXPORTIFY_URL}?change_user=true"
                                                    )
                                                } else {
                                                    accountChangeStage =
                                                        ExportifyAccountChangeStage.IDLE
                                                    accountChangeCallback(false)
                                                    loadingCallback(false)
                                                    errorCallback(
                                                        "Exportify account could not be changed"
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    return
                                }
                                ExportifyAccountChangeStage.RESTART -> {
                                    if (!isExportifyUrl(url) || !isExportifyUrl(view.url)) return
                                    accountChangeStage = ExportifyAccountChangeStage.IDLE
                                    view.clearHistory()
                                    accountChangeCallback(false)
                                }
                                ExportifyAccountChangeStage.IDLE -> Unit
                            }
                            // Navigation completion does not guarantee that Chromium has drawn its
                            // frame.
                            revealFrame(view, url)
                            canGoBack = view.canGoBack()
                            CookieManager.getInstance().flush()
                            if (isExportifyUrl(url) && isExportifyUrl(view.url) && bridge == null) {
                                val newBridge =
                                    ExportifyDownloadBridge(
                                        view,
                                        onExport = { if (!disposed) exportCallback(it) },
                                        onError = { if (!disposed) errorCallback(it) },
                                        onStarted = { if (!disposed) downloadCallback() },
                                    )
                                bridge = newBridge
                                try {
                                    newBridge.connect()
                                } catch (_: Exception) {
                                    newBridge.close()
                                    bridge = null
                                    errorCallback("Exportify download capture is unavailable")
                                }
                            }
                        }

                        override fun onPageCommitVisible(view: WebView, url: String?) {
                            if (accountChangeStage == ExportifyAccountChangeStage.IDLE)
                                revealFrame(view, url)
                        }

                        override fun doUpdateVisitedHistory(
                            view: WebView,
                            url: String?,
                            isReload: Boolean,
                        ) {
                            canGoBack = view.canGoBack()
                        }

                        override fun onReceivedError(
                            view: WebView,
                            request: WebResourceRequest,
                            error: WebResourceError,
                        ) {
                            if (request.isForMainFrame) {
                                accountChangeFailed =
                                    accountChangeStage != ExportifyAccountChangeStage.IDLE
                                accountChangeCallback(false)
                                loadingCallback(false)
                                pageErrorCallback(ExportifyPageError.Connection)
                            }
                        }

                        override fun onReceivedHttpError(
                            view: WebView,
                            request: WebResourceRequest,
                            errorResponse: WebResourceResponse,
                        ) {
                            if (request.isForMainFrame) {
                                accountChangeFailed =
                                    accountChangeStage != ExportifyAccountChangeStage.IDLE
                                accountChangeCallback(false)
                                loadingCallback(false)
                                pageErrorCallback(ExportifyPageError.Http(errorResponse.statusCode))
                            }
                        }

                        override fun onReceivedSslError(
                            view: WebView,
                            handler: SslErrorHandler,
                            error: SslError,
                        ) {
                            handler.cancel()
                            // SSL callbacks can also concern images/scripts: only obscure a failed
                            // page.
                            if (error.url == view.url) {
                                accountChangeFailed =
                                    accountChangeStage != ExportifyAccountChangeStage.IDLE
                                accountChangeCallback(false)
                                loadingCallback(false)
                                pageErrorCallback(ExportifyPageError.Connection)
                            }
                        }
                    }
                webChromeClient =
                    object : WebChromeClient() {
                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            callback?.onCustomViewHidden()
                        }

                        override fun onCreateWindow(
                            view: WebView,
                            isDialog: Boolean,
                            isUserGesture: Boolean,
                            resultMsg: Message,
                        ): Boolean {
                            if (!isUserGesture || popups.size >= 2) return false
                            val popup = WebView(context)
                            popups.add(popup)
                            popup.webViewClient =
                                object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        popupView: WebView,
                                        request: WebResourceRequest,
                                    ): Boolean {
                                        if (request.url.scheme == "https")
                                            view.loadUrl(request.url.toString())
                                        popups.remove(popupView)
                                        popupView.post { popupView.destroy() }
                                        return true
                                    }
                                }
                            (resultMsg.obj as WebView.WebViewTransport).webView = popup
                            resultMsg.sendToTarget()
                            return true
                        }
                    }
                setDownloadListener { url, userAgent, disposition, mimeType, _ ->
                    if (!isExportifyUrl(this.url)) return@setDownloadListener
                    val name = URLUtil.guessFileName(url, disposition, mimeType)
                    if (url.startsWith("blob:") || url.startsWith("data:")) {
                        bridge?.capture(url, name)
                    } else if (isExportifyUrl(url)) {
                        val cookie = CookieManager.getInstance().getCookie(url)
                        downloadCallback()
                        downloadScope.launch {
                            var downloaded: File? = null
                            try {
                                val file =
                                    downloadExport(context.cacheDir, url, name, userAgent, cookie)
                                downloaded = file
                                withContext(Dispatchers.Main) {
                                    if (!disposed) {
                                        exportCallback(file)
                                        downloaded = null
                                    }
                                }
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (_: Exception) {
                                withContext(Dispatchers.Main) {
                                    if (!disposed)
                                        errorCallback("Exportify download could not be received")
                                }
                            } finally {
                                downloaded?.delete()
                                downloaded?.parentFile?.delete()
                            }
                        }
                    }
                }
                loadUrl(EXPORTIFY_URL)
            }
        },
    )
}

private fun isExportifyUrl(url: String?): Boolean {
    val uri = url?.let(Uri::parse) ?: return false
    return uri.scheme == "https" &&
        uri.host == "exportify.app" &&
        uri.port in listOf(-1, 443) &&
        uri.userInfo == null
}

private fun exportFile(cacheDir: File, name: String): File {
    val safeName =
        name
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .replace(Regex("[\\x00-\\x1f\\x7f]"), "_")
    require(safeName.endsWith(".csv", true) || safeName.endsWith(".zip", true))
    var stem = safeName.dropLast(4).ifBlank { "playlist" }
    // Android filenames are limited by UTF-8 bytes, so truncate without splitting Unicode
    // characters.
    while (stem.toByteArray(Charsets.UTF_8).size > 220) {
        stem = stem.substring(0, stem.offsetByCodePoints(stem.length, -1))
    }
    val directory = File(cacheDir, "exportify/${UUID.randomUUID()}")
    check(directory.mkdirs())
    return File(directory, stem + safeName.takeLast(4))
}

/** A port is delivered only to the trusted top origin, never injected into Spotify login frames. */
private class ExportifyDownloadBridge(
    private val view: WebView,
    private val onExport: (File) -> Unit,
    private val onError: (String) -> Unit,
    private val onStarted: () -> Unit,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val messages = Channel<String>(capacity = 4)
    private val connectionToken = UUID.randomUUID().toString()
    private var port: WebMessagePort? = null
    @Volatile private var closed = false

    fun connect() {
        val ports = view.createWebMessageChannel()
        port = ports[0]
        ports[0].setWebMessageCallback(
            object : WebMessagePort.WebMessageCallback() {
                override fun onMessage(port: WebMessagePort, message: WebMessage) {
                    val data = message.data ?: return
                    if (closed || !isExportifyUrl(view.url)) return
                    if (data == "{\"type\":\"started\"}") {
                        onStarted()
                        return
                    }
                    if (data.length > EXPORT_CHUNK_BYTES * 2 || !messages.trySend(data).isSuccess) {
                        onError("Exportify download could not be received")
                        close()
                    }
                }
            }
        )
        scope.launch {
            var file: File? = null
            var output: FileOutputStream? = null
            var expectedBytes = 0L
            var receivedBytes = 0L
            try {
                for (data in messages) {
                    try {
                        val message = JSONObject(data)
                        when (message.getString("type")) {
                            "started" -> {
                                withContext(Dispatchers.Main) { if (!closed) onStarted() }
                                continue
                            }
                            "begin" -> {
                                check(output == null)
                                expectedBytes = message.getLong("size")
                                require(expectedBytes in 1..MAX_EXPORT_BYTES)
                                file = exportFile(view.context.cacheDir, message.getString("name"))
                                output = FileOutputStream(file)
                                receivedBytes = 0
                                withContext(Dispatchers.Main) { if (!closed) onStarted() }
                            }
                            "chunk" -> {
                                val bytes = Base64.decode(message.getString("data"), Base64.NO_WRAP)
                                require(bytes.size <= EXPORT_CHUNK_BYTES)
                                receivedBytes += bytes.size
                                require(receivedBytes <= expectedBytes)
                                checkNotNull(output).write(bytes)
                            }
                            "end" -> {
                                check(receivedBytes == expectedBytes)
                                checkNotNull(output).close()
                                output = null
                                val complete = checkNotNull(file)
                                withContext(Dispatchers.Main) {
                                    if (!closed) {
                                        onExport(complete)
                                        file = null
                                    }
                                }
                            }
                            "failed" -> {
                                runCatching { output?.close() }
                                output = null
                                file?.delete()
                                file?.parentFile?.delete()
                                file = null
                                withContext(Dispatchers.Main) {
                                    if (!closed) onError("Exportify download could not be received")
                                }
                                continue
                            }
                            else -> error("Unknown export message")
                        }
                        reply(true)
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (_: Exception) {
                        runCatching { output?.close() }
                        output = null
                        file?.delete()
                        file?.parentFile?.delete()
                        file = null
                        reply(false)
                        withContext(Dispatchers.Main) {
                            if (!closed) onError("Exportify download could not be received")
                        }
                    }
                }
            } finally {
                runCatching { output?.close() }
                file?.delete()
                file?.parentFile?.delete()
            }
        }
        view.evaluateJavascript(
            EXPORT_CAPTURE_SCRIPT.replace("__IRIDE_PORT_TOKEN__", connectionToken)
        ) { result ->
            if (!closed && isExportifyUrl(view.url) && result == "true") {
                view.postWebMessage(
                    WebMessage(connectionToken, arrayOf(ports[1])),
                    Uri.parse(EXPORTIFY_ORIGIN),
                )
            } else if (!closed) {
                ports[1].close()
                onError("Exportify download capture is unavailable")
                close()
            }
        }
    }

    fun capture(url: String, name: String) {
        if (closed || !isExportifyUrl(view.url)) return
        if (url.length > EXPORT_CHUNK_BYTES * 2) {
            onError("Exportify download could not be received")
            return
        }
        view.evaluateJavascript(
            "window.__irideExportCapture && window.__irideExportCapture.capture(" +
                JSONObject.quote(url) +
                "," +
                JSONObject.quote(name) +
                ")",
            null,
        )
    }

    private suspend fun reply(ok: Boolean) =
        withContext(Dispatchers.Main) {
            if (!closed) port?.postMessage(WebMessage(if (ok) "ok" else "error"))
        }

    fun close() {
        closed = true
        port?.close()
        port = null
        messages.close()
        scope.cancel()
    }
}

private suspend fun downloadExport(
    cacheDir: File,
    url: String,
    name: String,
    userAgent: String,
    cookie: String?,
): File {
    val file = exportFile(cacheDir, name)
    try {
        var target = url
        repeat(4) { redirectCount ->
            require(isExportifyUrl(target))
            val connection = URL(target).openConnection() as HttpURLConnection
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.setRequestProperty("User-Agent", userAgent)
                cookie?.let { connection.setRequestProperty("Cookie", it) }
                if (connection.responseCode in 300..399) {
                    check(redirectCount < 3)
                    target =
                        URL(URL(target), checkNotNull(connection.getHeaderField("Location")))
                            .toString()
                } else {
                    check(connection.responseCode in 200..299)
                    require(connection.contentLengthLong <= MAX_EXPORT_BYTES)
                    connection.inputStream.use { input ->
                        file.outputStream().use { output ->
                            val buffer = ByteArray(EXPORT_CHUNK_BYTES)
                            var total = 0L
                            while (true) {
                                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                                val count = input.read(buffer)
                                if (count < 0) break
                                total += count
                                require(total <= MAX_EXPORT_BYTES)
                                output.write(buffer, 0, count)
                            }
                            require(total > 0)
                        }
                    }
                    return file
                }
            } finally {
                connection.disconnect()
            }
        }
        error("Too many redirects")
    } catch (exception: Exception) {
        file.delete()
        file.parentFile?.delete()
        throw exception
    }
}

private val EXPORT_CAPTURE_SCRIPT =
    """
    (() => {
      if (location.origin !== '$EXPORTIFY_ORIGIN' || window !== window.top) return false;
      const connectionToken = '__IRIDE_PORT_TOKEN__';
      if (window.__irideExportCapture) {
        window.__irideExportCapture.prepareConnection(connectionToken);
        return true;
      }
      let expectedConnection = connectionToken;
      let port = null, waiting = null, queue = Promise.resolve(), queued = 0;
      const blobs = new Map(), seen = new Set();
      const maximum = $MAX_EXPORT_BYTES, chunkBytes = $EXPORT_CHUNK_BYTES;
      function send(message) {
        return new Promise((resolve, reject) => {
          if (!port) { reject(new Error('bridge unavailable')); return; }
          const timer = setTimeout(() => { waiting = null; reject(new Error('transfer timeout')); }, 35000);
          waiting = result => {
            clearTimeout(timer); waiting = null;
            result === 'ok' ? resolve() : reject(new Error('transfer rejected'));
          };
          port.postMessage(JSON.stringify(message));
        });
      }
      async function transfer(blob, name) {
        if (!blob.size || blob.size > maximum) throw new Error('invalid export size');
        await send({type: 'begin', name: name, size: blob.size});
        for (let offset = 0; offset < blob.size; offset += chunkBytes) {
          const bytes = new Uint8Array(await blob.slice(offset, offset + chunkBytes).arrayBuffer());
          let binary = '';
          for (let i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i]);
          await send({type: 'chunk', data: btoa(binary)});
        }
        await send({type: 'end'});
      }
      function capture(url, name) {
        if (!port || !/\.(csv|zip)$/i.test(name) || !/^(blob:|data:)/.test(url)) return false;
        if (seen.has(url)) return true;
        if (queued >= 8) return false;
        seen.add(url); queued++;
        const blob = blobs.get(url);
        queue = queue.then(async () => {
          const exported = blob || await (await fetch(url)).blob();
          await transfer(exported, name);
        }).catch(() => {
          if (port) port.postMessage(JSON.stringify({type: 'failed'}));
        }).finally(() => { queued--; });
        if (seen.size > 64) seen.delete(seen.values().next().value);
        return true;
      }
      const createUrl = URL.createObjectURL.bind(URL), revokeUrl = URL.revokeObjectURL.bind(URL);
      URL.createObjectURL = function(blob) {
        const url = createUrl(blob); blobs.set(url, blob); return url;
      };
      URL.revokeObjectURL = function(url) { blobs.delete(url); return revokeUrl(url); };
      const click = HTMLAnchorElement.prototype.click, dispatch = HTMLAnchorElement.prototype.dispatchEvent;
      HTMLAnchorElement.prototype.click = function() {
        if (capture(this.href, this.download)) return;
        return click.apply(this, arguments);
      };
      // FileSaver dispatches a synthetic click on a detached anchor; a document listener alone misses it.
      HTMLAnchorElement.prototype.dispatchEvent = function(event) {
        if (event.type === 'click' && capture(this.href, this.download)) return true;
        return dispatch.apply(this, arguments);
      };
      document.addEventListener('click', event => {
        const anchor = event.target.closest && event.target.closest('a[download]');
        if (anchor && capture(anchor.href, anchor.download)) {
          event.preventDefault(); event.stopImmediatePropagation(); return;
        }
        const button = event.target.closest && event.target.closest('button');
        if (port && button && !button.disabled && button.querySelector('[data-icon="download"], [data-icon="file-archive"], [data-icon="file-zipper"]')) {
          port.postMessage(JSON.stringify({type: 'started'}));
        }
      }, true);
      window.addEventListener('message', event => {
        // A cross-origin iframe must not be able to replace the native port with its own.
        if (!expectedConnection || event.data !== expectedConnection || !event.ports[0]) return;
        expectedConnection = null;
        if (port) port.close();
        port = event.ports[0];
        port.onmessage = event => { if (waiting) waiting(event.data); };
        port.start();
      });
      window.__irideExportCapture = {
        capture: capture,
        prepareConnection: token => { expectedConnection = token; }
      };
      return true;
    })();
"""
        .trimIndent()
