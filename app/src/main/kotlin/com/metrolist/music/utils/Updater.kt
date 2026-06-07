/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import com.metrolist.music.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val description: String,
    val releaseDate: String,
    val assets: List<ReleaseAsset>,
    val preRelease: Boolean = false,
)

data class ReleaseAsset(
    val name: String,
    val downloadUrl: String,
    val size: Long,
    val architecture: String,
    val variant: String // "foss" or "gms"
)

object Updater {
    private val client = HttpClient()
    var lastCheckTime = -1L
        private set
    
    private var cachedReleaseInfo: ReleaseInfo? = null
    private var cachedAllReleases: List<ReleaseInfo> = emptyList()
    
    private const val CHECK_INTERVAL_MILLIS = 2 * 60 * 60 * 1000L // 2 hours
    private const val GITHUB_API_BASE = "https://api.github.com/repos/Fhox006/Iride"

    /**
     * Compares two version strings.
     * Returns: 1 if v1 > v2, -1 if v1 < v2, 0 if equal
     */
    fun compareVersions(v1: String, v2: String): Int {
        val clean1 = v1.removePrefix("v")
        val clean2 = v2.removePrefix("v")

        val dash1 = clean1.indexOf('-')
        val dash2 = clean2.indexOf('-')
        val base1 = if (dash1 >= 0) clean1.substring(0, dash1) else clean1
        val base2 = if (dash2 >= 0) clean2.substring(0, dash2) else clean2
        val pre1 = if (dash1 >= 0) clean1.substring(dash1 + 1) else null
        val pre2 = if (dash2 >= 0) clean2.substring(dash2 + 1) else null

        val v1Parts = base1.split(".").map { it.toIntOrNull() ?: 0 }
        val v2Parts = base2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLength = maxOf(v1Parts.size, v2Parts.size)

        for (i in 0 until maxLength) {
            val part1 = v1Parts.getOrNull(i) ?: 0
            val part2 = v2Parts.getOrNull(i) ?: 0
            when {
                part1 > part2 -> return 1
                part1 < part2 -> return -1
            }
        }

        // base equal — stable > pre-release (semver); extract numeric suffix for correct ordering
        return when {
            pre1 == null && pre2 == null -> 0
            pre1 == null -> 1
            pre2 == null -> -1
            else -> {
                val numMatch1 = Regex("(\\D+)(\\d+)$").find(pre1)
                val numMatch2 = Regex("(\\D+)(\\d+)$").find(pre2)
                val prefix1 = numMatch1?.groupValues?.get(1) ?: pre1
                val prefix2 = numMatch2?.groupValues?.get(1) ?: pre2
                val num1 = numMatch1?.groupValues?.get(2)?.toIntOrNull()
                val num2 = numMatch2?.groupValues?.get(2)?.toIntOrNull()
                val prefixCmp = prefix1.compareTo(prefix2)
                when {
                    prefixCmp != 0 -> prefixCmp
                    num1 != null && num2 != null -> num1.compareTo(num2)
                    else -> pre1.compareTo(pre2)
                }
            }
        }
    }

    /**
     * Checks if the latest version is newer than the current version.
     * Returns true if an update is available (latestVersion > currentVersion)
     */
    fun isUpdateAvailable(currentVersion: String, latestVersion: String): Boolean {
        return compareVersions(latestVersion, currentVersion) > 0
    }

    /**
     * Get the current app's architecture and variant
     */
    private fun getCurrentAppVariant(): Pair<String, String> {
        val architecture = BuildConfig.ARCHITECTURE
        val variant = if (BuildConfig.CAST_AVAILABLE) "gms" else "foss"
        return architecture to variant
    }

    /**
     * Parse release assets from GitHub API response
     */
    private fun parseAssets(assetsArray: JSONArray): List<ReleaseAsset> {
        val assets = mutableListOf<ReleaseAsset>()
        
        for (i in 0 until assetsArray.length()) {
            val asset = assetsArray.getJSONObject(i)
            val name = asset.getString("name")
            
            // Skip non-APK files
            if (!name.endsWith(".apk")) continue
            
            val downloadUrl = asset.getString("browser_download_url")
            val size = asset.getLong("size")
            
            // Parse architecture and variant from filename
            val (arch, variant) = when {
                name == "Iride.apk" -> "universal" to "foss"
                name == "Iride-GMS.apk" -> "universal" to "gms"
                name.startsWith("app-foss") && name.endsWith("-release.apk") -> "universal" to "foss"
                name.startsWith("app-gms") && name.endsWith("-release.apk") -> "universal" to "gms"
                else -> null to null
            }
            
            if (arch != null && variant != null) {
                assets.add(ReleaseAsset(name, downloadUrl, size, arch, variant))
            }
        }
        
        return assets
    }

    /**
     * Fetch latest release from GitHub API
     */
    suspend fun getLatestRelease(forceRefresh: Boolean = false): Result<ReleaseInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Return cached if available and not forcing refresh
                if (cachedReleaseInfo != null && !forceRefresh) {
                    return@runCatching cachedReleaseInfo!!
                }
                
                val response = client.get("$GITHUB_API_BASE/releases/latest")
                    .bodyAsText()
                val json = JSONObject(response)
                
                val releaseInfo = ReleaseInfo(
                    tagName = json.getString("tag_name"),
                    versionName = json.getString("name"),
                    description = json.getString("body"),
                    releaseDate = json.getString("published_at"),
                    assets = parseAssets(json.getJSONArray("assets")),
                    preRelease = json.optBoolean("prerelease", false),
                )
                
                cachedReleaseInfo = releaseInfo
                lastCheckTime = System.currentTimeMillis()
                releaseInfo
            }
        }

    /**
     * Fetch all releases from GitHub API (paginated)
     */
    suspend fun getAllReleases(forceRefresh: Boolean = false): Result<List<ReleaseInfo>> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (cachedAllReleases.isNotEmpty() && !forceRefresh) {
                    return@runCatching cachedAllReleases
                }
                
                val releases = mutableListOf<ReleaseInfo>()
                var page = 1
                var hasMore = true
                
                while (hasMore && page <= 10) { // Limit to 10 pages
                    val response = client.get("$GITHUB_API_BASE/releases?page=$page&per_page=30")
                        .bodyAsText()
                    val json = JSONArray(response)
                    
                    if (json.length() == 0) {
                        hasMore = false
                        break
                    }
                    
                    for (i in 0 until json.length()) {
                        val releaseObj = json.getJSONObject(i)
                        releases.add(ReleaseInfo(
                            tagName = releaseObj.getString("tag_name"),
                            versionName = releaseObj.getString("name"),
                            description = releaseObj.getString("body"),
                            releaseDate = releaseObj.getString("published_at"),
                            assets = parseAssets(releaseObj.getJSONArray("assets")),
                            preRelease = releaseObj.optBoolean("prerelease", false),
                        ))
                    }
                    
                    page++
                }
                
                cachedAllReleases = releases
                releases
            }
        }

    /**
     * Get the download URL for the correct app variant
     */
    fun getDownloadUrlForCurrentVariant(releaseInfo: ReleaseInfo): String? {
        val (currentArch, currentVariant) = getCurrentAppVariant()
        
        return releaseInfo.assets
            .find { it.architecture == currentArch && it.variant == currentVariant }
            ?.downloadUrl
    }

    /**
     * Get all available download URLs for a release
     */
    fun getAllDownloadUrls(releaseInfo: ReleaseInfo): Map<String, String> {
        return releaseInfo.assets.associate { "${it.architecture}-${it.variant}" to it.downloadUrl }
    }

    /**
     * Check if update is needed (respects 2-hour cache)
     */
    suspend fun checkForUpdate(forceRefresh: Boolean = false): Result<Pair<ReleaseInfo?, Boolean>> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Check if we should fetch (2 hour interval)
                val shouldFetch = forceRefresh || 
                    (System.currentTimeMillis() - lastCheckTime) > CHECK_INTERVAL_MILLIS
                
                if (!shouldFetch && cachedReleaseInfo != null) {
                    val hasUpdate = isUpdateAvailable(
                        BuildConfig.VERSION_NAME,
                        cachedReleaseInfo!!.tagName
                    )
                    return@runCatching cachedReleaseInfo!! to hasUpdate
                }
                
                val result = getLatestRelease(forceRefresh = true)
                if (result.isSuccess) {
                    val releaseInfo = result.getOrThrow()
                    val hasUpdate = isUpdateAvailable(
                        BuildConfig.VERSION_NAME,
                        releaseInfo.tagName
                    )
                    releaseInfo to hasUpdate
                } else {
                    throw result.exceptionOrNull() ?: Exception("Unknown error")
                }
            }
        }

    /**
     * Get the download URL for the correct app variant
     * Returns null if no matching asset is found
     */
    fun getLatestDownloadUrl(): String? {
        return cachedReleaseInfo?.let { getDownloadUrlForCurrentVariant(it) }
    }
    
    /**
     * Get the latest release info (cached)
     */
    fun getCachedLatestRelease(): ReleaseInfo? = cachedReleaseInfo

    suspend fun getLatestAvailableReleaseIncludingPrerelease(forceRefresh: Boolean = false): Result<ReleaseInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                val releases = getAllReleases(forceRefresh = forceRefresh).getOrThrow()
                releases
                    .filter { it.assets.isNotEmpty() }
                    .maxWithOrNull { a, b -> compareVersions(a.tagName, b.tagName) }
                    ?: throw IllegalStateException("No valid release with APK assets found")
            }
        }

    suspend fun checkForAnyUpdate(forceRefresh: Boolean = false): Result<Pair<ReleaseInfo?, Boolean>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val shouldFetch = forceRefresh ||
                    (System.currentTimeMillis() - lastCheckTime) > CHECK_INTERVAL_MILLIS
                val releaseInfo =
                    if (!shouldFetch && cachedAllReleases.isNotEmpty()) {
                        cachedAllReleases
                            .filter { it.assets.isNotEmpty() }
                            .maxWithOrNull { a, b -> compareVersions(a.tagName, b.tagName) }
                    } else {
                        getLatestAvailableReleaseIncludingPrerelease(forceRefresh = true).getOrThrow()
                    }

                if (releaseInfo == null) return@runCatching null to false

                cachedReleaseInfo = releaseInfo
                lastCheckTime = System.currentTimeMillis()

                val hasUpdate = isUpdateAvailable(BuildConfig.VERSION_NAME, releaseInfo.tagName)
                releaseInfo to hasUpdate
            }
        }
}