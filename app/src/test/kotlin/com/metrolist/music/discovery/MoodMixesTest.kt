package com.metrolist.music.discovery

import com.metrolist.innertube.models.BrowseEndpoint
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.pages.HomePage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodMixesTest {
    private fun playlist(id: String, title: String, podcast: Boolean = false) = PlaylistItem(
        id = id, title = title, author = null, songCountText = null, thumbnail = null,
        playEndpoint = null, shuffleEndpoint = null, radioEndpoint = null, isPodcast = podcast,
    )

    @Test
    fun keepsMixesWithoutRequiringTheCategoryNameInTheirTitle() {
        val valid = playlist("RDchill", "Chill Mix 01")
        val items = listOf(valid, valid, playlist("RDartist", "Artist Radio"),
            playlist("PLordinary", "Relaxing songs"), playlist("RDsad", "Sad Mix 02"),
            playlist("RDpodcast", "Chill Mix 03", podcast = true),
            SongItem("song", "Chill Mix", emptyList(), thumbnail = ""))
        assertEquals(listOf(valid, items[4]), MoodMixes.select(items))
    }

    @Test
    fun handlesCaseAliasesAndWordBoundaries() {
        assertEquals(MixMood.SAD, MixMood.fromMixTitle("Your SAD MIX 01"))
        assertEquals(MixMood.FEEL_GOOD, MixMood.fromMixTitle("Feel-Good Mix 2"))
        assertEquals(MixMood.ENERGY, MixMood.fromMixTitle("Energize Mix 1"))
        assertNull(MixMood.fromMixTitle("Chilled Mix"))
        assertNull(MixMood.fromMixTitle("Chill Remixes"))
        assertNull(MixMood.fromMixTitle("Chill Radio"))
        assertNull(MixMood.fromMixTitle("Travel Mix"))
        assertEquals(MixMood.CHILL, MixMood.fromMixTitle("Mix relax 1"))
        assertEquals(MixMood.SAD, MixMood.fromMixTitle("Mix malinconico 2"))
        assertEquals(MixMood.SLEEP, MixMood.fromMixTitle("Sonno Mix1"))
    }

    @Test
    fun removesTravelAndWellnessButPreservesFocusAndFeelGood() {
        fun chip(title: String, params: String? = title) = HomePage.Chip(
            title, BrowseEndpoint(browseId = "FEmusic_home", params = params), null,
        )
        val result = MoodMixes.chips(listOf(chip("Travel"), chip("Commute"), chip("Wellness"),
            chip("Focus"), chip("Feel Good"), chip("Sad"), chip("Melancholic"),
            chip("Relax"), chip("Sleep"), chip("Party", null), chip("Podcasts")))
        assertEquals(listOf("Focus", "Feel Good", "Sad", "Sad", "Chill", "Sleep"), result.map { it.title })
        assertEquals("Relax", result.first { it.title == "Chill" }.endpoint?.params)
        assertEquals("Focus", MixMood.fromChip("Focus")?.label)
        assertNull(MixMood.fromChip("Wellness"))
        assertNull(MixMood.fromChip("Dopaminergic"))
    }

    @Test
    fun deduplicatesBeforeLimitingAndDropsPreviousItemsOnEmptyResponses() {
        val mixes = (0..12).map { playlist("RD$it", "Workout Mix $it") }
        assertEquals(10, MoodMixes.select(listOf(mixes.first()) + mixes).size)
        assertTrue(MoodMixes.select(listOf(playlist("RDother", "Artist Radio"))).isEmpty())
    }

    @Test
    fun recognizesLocalizedGenericAndPersonalizedMixesButExcludesOtherStations() {
        val mixes = listOf(playlist("RDEMrelax", "Mix relax 1"),
            playlist("RDEMgeneric", "Mix 01"), playlist("RDsuper", "Il tuo supermix"),
            playlist("RDTMAK5uy_personalized", "Recommandations personnalis?es"))
        val others = listOf(playlist("RDCLAK5uy_curated", "Incense & Tea"),
            playlist("RDartist", "Artist Radio"), playlist("PLremixes", "Chill Remixes"),
            playlist("RDTMAK5uy_podcast", "Mix podcast", podcast = true))
        assertEquals(mixes, MoodMixes.select(mixes + others))
    }

    @Test
    fun keepsUnknownAndPreviouslyRenamedCategoriesAndDistinctEndpoints() {
        val names = listOf("Melancholic", "Dopaminergic", "Love", "Rest", "Wellness",
            "Dopaminergic", "Travel", "Rest", "Feel good", "Festive", "Jazz")
        val chips = names.mapIndexed { index, title -> HomePage.Chip(
            title, BrowseEndpoint(browseId = "FEmusic_home", params = "params-$index"), null,
        ) }
        val result = MoodMixes.chips(chips + chips.first())
        assertEquals(9, result.size)
        assertEquals(2, result.count { it.title == "Rest" })
        assertEquals(2, result.count { it.title == "Dopaminergic" })
        assertTrue(result.any { it.title == "Jazz" })
        assertEquals("Sad", result.first().title)
    }

    @Test
    fun recognizesMixesForCategoriesWithNoPosterMood() {
        val mix = playlist("RDTMAK5uy_jazz", "Jazz Mix 1")
        assertNull(MixMood.fromChip("Jazz"))
        assertTrue(MoodMixes.isAllowedCategory("Jazz"))
        assertEquals(listOf(mix), MoodMixes.select(listOf(mix)))
        assertTrue(MoodMixes.select(listOf(playlist("RDstation", "Jazz station"))).isEmpty())
    }


    @Test
    fun sortsNumberedMixesBeforeSupermixWithoutDependingOnTheLanguage() {
        val titles = listOf("Supermix", "Mix 04", "Mix 2", "Mix 01", "Mix 3")
        val mixes = titles.mapIndexed { index, title -> playlist("RDTMA-$index", title) }
        assertEquals(listOf("Mix 01", "Mix 2", "Mix 3", "Mix 04", "Supermix"),
            MoodMixes.select(mixes).map { it.title })
    }

    @Test
    fun acceptsLocalizedAutomaticMixesAndSortsUnicodeDigits() {
        val titles = listOf("あなたのおすすめ", "مزيج ٣", "ミックス ４", "Микс २", "混合 １")
        val mixes = titles.mapIndexed { index, title -> playlist("RDTMA-$index", title) }
        assertEquals(listOf(titles[4], titles[3], titles[1], titles[2], titles[0]),
            MoodMixes.select(mixes).map { it.title })
        val chips = listOf("حزين", "集中", "Любовь").map { title -> HomePage.Chip(
            title, BrowseEndpoint(browseId = "FEmusic_home", params = title), null,
        ) }
        assertEquals(chips, MoodMixes.chips(chips))
    }

}
