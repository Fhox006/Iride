/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.lyrics

object LyricsProviderRegistry {
    private val providerMap = mapOf(
        "LrcLib" to LrcLibLyricsProvider,
        "BetterLyricsUnison" to BetterLyricsUnisonProvider,
        "BetterLyricsSillaba" to BetterLyricsSillabaProvider,
        "BetterLyrics" to BetterLyricsProvider,
        "Paxsenix" to PaxsenixLyricsProvider,
        "KuGou" to KuGouLyricsProvider,
        "LyricsPlus" to LyricsPlusProvider,
        "YouTubeSubtitle" to YouTubeSubtitleLyricsProvider,
        "YouTube" to YouTubeLyricsProvider,
    )

    val providerNames = providerMap.keys.toList()

    fun getProviderByName(name: String): LyricsProvider? = providerMap[name]

    fun getProviderName(provider: LyricsProvider): String? =
        providerMap.entries.find { it.value == provider }?.key

    fun deserializeProviderOrder(orderString: String): List<String> {
        if (orderString.isBlank()) {
            return getDefaultProviderOrder()
        }
        return orderString.split(",").map { it.trim() }.filter { it in providerNames }
    }

    fun serializeProviderOrder(providers: List<String>): String {
        return providers.filter { it in providerNames }.joinToString(",")
    }

    fun getDefaultProviderOrder(): List<String> = listOf(
        "LrcLib",
        "BetterLyricsUnison",
        "BetterLyricsSillaba",
        "BetterLyrics",
        "Paxsenix",
        "KuGou",
        "LyricsPlus",
        "YouTubeSubtitle",
        "YouTube",
    )

    fun getOrderedProviders(orderString: String): List<LyricsProvider> {
        val order = deserializeProviderOrder(orderString)
        return order.mapNotNull { getProviderByName(it) }
    }

    /**
     * Tie-break rank when two providers return the same [LyricsTier]. Lower value wins.
     * LrcLib beats every word-by-word provider; among word providers, Unison beats Sillaba.
     */
    fun getTierTieBreakPriority(providerName: String): Int =
        getDefaultProviderOrder().indexOf(providerName).let { if (it == -1) Int.MAX_VALUE else it }
}
