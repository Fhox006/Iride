package com.metrolist.music.discovery

import java.text.Collator
import java.util.Locale

/** A compact catalog of music scenes, displayed in the app's selected language. */
data class DiscoveryLanguage(val code: String, val country: String) {
    val lyricsCode: String get() = Locale.forLanguageTag(code).isO3Language
    fun countryName(displayLocale: Locale): String = Locale.Builder().setRegion(country).build().getDisplayCountry(displayLocale)
    fun languageName(displayLocale: Locale): String = Locale.forLanguageTag(code).getDisplayLanguage(displayLocale)
        .replaceFirstChar { it.titlecase(displayLocale) }

    companion object {
        val all = listOf(
            DiscoveryLanguage("ar", "SA"), DiscoveryLanguage("zh", "CN"),
            DiscoveryLanguage("cs", "CZ"), DiscoveryLanguage("da", "DK"),
            DiscoveryLanguage("nl", "NL"), DiscoveryLanguage("en", "US"),
            DiscoveryLanguage("fi", "FI"), DiscoveryLanguage("fr", "FR"),
            DiscoveryLanguage("de", "DE"), DiscoveryLanguage("el", "GR"),
            DiscoveryLanguage("hi", "IN"), DiscoveryLanguage("id", "ID"),
            DiscoveryLanguage("it", "IT"), DiscoveryLanguage("ja", "JP"),
            DiscoveryLanguage("ko", "KR"), DiscoveryLanguage("no", "NO"),
            DiscoveryLanguage("pl", "PL"), DiscoveryLanguage("pt", "BR"),
            DiscoveryLanguage("ro", "RO"), DiscoveryLanguage("ru", "RU"),
            DiscoveryLanguage("es", "ES"), DiscoveryLanguage("sv", "SE"),
            DiscoveryLanguage("tr", "TR"), DiscoveryLanguage("uk", "UA"),
        )

        fun defaultFor(locale: Locale): DiscoveryLanguage =
            all.firstOrNull { it.code == locale.language || (it.code == "no" && locale.language in setOf("nb", "nn")) }
                ?: all.first { it.code == "en" }

        fun sections(selected: DiscoveryLanguage, locale: Locale): List<List<DiscoveryLanguage>> {
            val popular = listOf("en", "es", "fr", "de", "it", "pt")
                .filterNot { it == selected.code }.take(5).map { code -> all.first { it.code == code } }
            val collator = Collator.getInstance(locale)
            val remaining = all.filterNot { it == selected || it in popular }
                .sortedWith { a, b -> collator.compare(a.languageName(locale), b.languageName(locale)) }
            return listOf(listOf(selected) + popular, remaining)
        }
    }
}
