package com.metrolist.music.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class ChartMarketCatalogTest {
    private val index = """
        <a href="country/global_daily.html">Daily</a>
        <a href="country/it_daily.html">Daily</a>
        <a href="https://www.kworb.net/spotify/country/us_daily.html">Daily</a>
        <a href="country/it_daily.html">Duplicate</a>
        <a href="country/fr_weekly.html">Weekly</a>
        <a href="country/fr_daily_totals.html">Totals</a>
        <a href="country/zz_daily.html">Invalid country</a>
        <a href="https://example.com/spotify/country/br_daily.html">Untrusted host</a>
    """

    @Test fun discoversDailyCountriesFromOneIndexAndIgnoresUnrelatedLinks() {
        val catalog = ChartMarkets.parse(index, 123L)
        assertEquals(listOf("global", "it", "us"), catalog.codes)
        assertEquals(123L, catalog.checkedAt)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsChangedIndexInsteadOfReplacingTheOfflineCatalog() {
        ChartMarkets.parse("<h1>Service unavailable</h1>", 123L)
    }

    @Test fun cachedCatalogRequiresSafeUniqueIsoCodesAndGlobalChart() {
        val catalog = ChartMarkets.parse(index, 123L)
        assertTrue(ChartMarkets.valid(catalog))
        assertFalse(ChartMarkets.valid(catalog.copy(codes = catalog.codes + "../../x")))
        assertFalse(ChartMarkets.valid(catalog.copy(codes = catalog.codes + "it")))
        assertFalse(ChartMarkets.valid(catalog.copy(codes = listOf("it", "us", "fr"))))
    }

    @Test fun countriesUseLocalizedCountryNamesInsteadOfIsoLabels() {
        assertEquals("Italia", ChartMarket.Italy.countryName(Locale.ITALIAN))
        assertEquals("Brasile", ChartMarket("br").countryName(Locale.ITALIAN))
        assertTrue(ChartMarkets.countries.containsAll(listOf(ChartMarket("jp"), ChartMarket("ng"), ChartMarket("af"))))
        assertTrue(ChartMarkets.valid(ChartMarketCatalog(ChartMarkets.bootstrap.map { it.code }, 123L)))
    }
}
