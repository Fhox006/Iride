package com.metrolist.music.data.remote

import kotlinx.serialization.Serializable
import org.jsoup.Jsoup
import java.util.Locale

@Serializable
data class ChartMarketCatalog(val codes: List<String>, val checkedAt: Long)

object ChartMarkets {
    const val INDEX_URL = "https://kworb.net/spotify/"
    private val isoCodes = Locale.getISOCountries().map { it.lowercase(Locale.ROOT) }.toSet()
    private val dailyPath = Regex("^/spotify/country/(global|[a-z]{2})_daily\\.html$")

    val countries: List<ChartMarket> = isoCodes.sorted().map(::ChartMarket)

    // Offline bootstrap; the public country index updates this list automatically.
    val bootstrap: List<ChartMarket> = ("global us gb ad ar au at by be bo br bg ca cl co cr cy cz dk do ec eg sv ee fi fr " +
        "de gr gt hn hk hu is in id ie il it jp kz lv lt lu my mt mx ma nl nz ni ng no pk pa py pe ph pl pt ro ru sa " +
        "sg sk za kr es se ch tw th tr ua ae uy ve vn").split(' ').map(::ChartMarket)

    fun valid(catalog: ChartMarketCatalog): Boolean = catalog.checkedAt >= 0 && catalog.codes.size >= 3 &&
        "global" in catalog.codes && catalog.codes.distinct().size == catalog.codes.size &&
        catalog.codes.all { it == "global" || it in isoCodes }

    fun parse(html: String, checkedAt: Long): ChartMarketCatalog {
        val codes = Jsoup.parse(html, INDEX_URL).select("a[href]").mapNotNull { link ->
            val url = runCatching { java.net.URI(link.absUrl("href")) }.getOrNull() ?: return@mapNotNull null
            if (url.scheme != "https" || (url.host != "kworb.net" && url.host != "www.kworb.net")) return@mapNotNull null
            dailyPath.matchEntire(url.path.orEmpty())?.groupValues?.get(1)
                ?.takeIf { it == "global" || it in isoCodes }
        }.distinct()
        return ChartMarketCatalog(codes, checkedAt).also { require(valid(it)) { "Invalid country chart index" } }
    }
}
