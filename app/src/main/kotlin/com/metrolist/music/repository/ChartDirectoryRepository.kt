package com.metrolist.music.repository

import android.content.Context
import android.util.AtomicFile
import com.metrolist.music.data.remote.ChartMarket
import com.metrolist.music.data.remote.ChartMarkets
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import java.io.File
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

data class ChartDirectoryState(
    val markets: List<ChartMarket> = ChartMarket.home,
    val hasSourceCatalog: Boolean = false,
    val refreshing: Boolean = false,
    val refreshFailed: Boolean = false,
)

@Serializable
private data class CachedChartDirectory(val codes: List<String>, val savedAt: Long)

/** Discover real source markets. A listed market is checked separately before opening its chart. */
@Singleton
class ChartDirectoryRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val dailyCharts: DailyChartRepository,
) {
    private val cache = AtomicFile(File(context.filesDir, "daily_charts/directory.json"))
    private val json = Json { ignoreUnknownKeys = true }
    private val refreshLock = Mutex()
    private val chartRequests = Semaphore(2)
    private val mutableState = MutableStateFlow(ChartDirectoryState())
    val state: StateFlow<ChartDirectoryState> = mutableState.asStateFlow()
    private var loadedDisk = false
    private var checkedAt = 0L
    private var retryAt = 0L
    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 12_000
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 12_000
        }
    }

    suspend fun refresh(force: Boolean = false): Unit = withContext(Dispatchers.IO) {
        refreshLock.withLock {
            if (!loadedDisk) {
                loadedDisk = true
                runCatching {
                    cache.openRead().bufferedReader().use { json.decodeFromString<CachedChartDirectory>(it.readText()) }
                }.getOrNull()?.takeIf { validCodes(it.codes) && it.savedAt >= 0 }?.let {
                    checkedAt = it.savedAt
                    mutableState.value = ChartDirectoryState(it.codes.map(::ChartMarket), hasSourceCatalog = true)
                }
            }
            val now = System.currentTimeMillis()
            if (!force && (now in checkedAt until checkedAt + 24 * 60 * 60_000L || now < retryAt)) return@withLock
            mutableState.value = mutableState.value.copy(refreshing = true, refreshFailed = false)
            try {
                var codes: List<String>? = null
                var failure: Exception? = null
                repeat(2) { attempt ->
                    if (codes == null) {
                        try {
                            val response = client.get(SOURCE_URL)
                            check(response.status.value == 200) { "Chart directory HTTP ${response.status.value}" }
                            codes = parseSource(response.bodyAsText())
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            failure = error
                            if (attempt == 0) delay(500)
                        }
                    }
                }
                val fresh = checkNotNull(codes) { failure?.message ?: "Missing chart directory" }
                try {
                    cache.baseFile.parentFile?.mkdirs()
                    val stream = cache.startWrite()
                    try {
                        stream.write(json.encodeToString(CachedChartDirectory(fresh, now)).toByteArray(Charsets.UTF_8))
                        cache.finishWrite(stream)
                    } catch (error: Exception) {
                        cache.failWrite(stream)
                        throw error
                    }
                } catch (error: Exception) {
                    Timber.w(error, "Could not cache chart directory")
                }
                checkedAt = now
                retryAt = 0L
                mutableState.value = ChartDirectoryState(fresh.map(::ChartMarket), hasSourceCatalog = true)
            } catch (cancelled: CancellationException) {
                mutableState.value = mutableState.value.copy(refreshing = false)
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Chart directory refresh failed")
                retryAt = now + 5 * 60_000L
                mutableState.value = mutableState.value.copy(refreshing = false, refreshFailed = true)
            }
        }
    }

    /** Only cards entering the viewport request a chart; directory browsing never fetches every country. */
    suspend fun checkMarket(market: ChartMarket, force: Boolean = false) {
        chartRequests.withPermit { dailyCharts.refresh(market, force) }
    }

    companion object {
        const val SOURCE_URL = ChartMarkets.INDEX_URL
        private val isoCountries = Locale.getISOCountries().map { it.lowercase(Locale.ROOT) }.toSet()

        internal fun parseSource(html: String): List<String> {
            val codes = ChartMarkets.parse(html, 0L).codes
            require(validCodes(codes)) { "Incomplete chart directory" }
            return codes
        }

        private fun validCodes(codes: List<String>): Boolean =
            codes.size >= 30 && codes.distinct().size == codes.size &&
                codes.containsAll(listOf("global", "us", "it")) &&
                codes.all { it == "global" || it in isoCountries }
    }
}
