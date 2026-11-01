package com.metrolist.music.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MixPosterPolicyTest {
    @Test
    fun photoAttemptsTryEveryDistinctArtistBeforeRepeatingUsedArtists() {
        assertEquals(listOf("second", "third", "fourth", "first"), MixPosterPolicy.artistAttempts(
            listOf("first", "second", "second", "third", "fourth"), emptySet(), "first", true))
        assertEquals(listOf("second", "first"), MixPosterPolicy.artistAttempts(
            listOf("first", "second"), setOf("first"), null, false))
        assertEquals(listOf("first"), MixPosterPolicy.artistAttempts(listOf("first", "first"), emptySet(), null, false))
        assertTrue(MixPosterPolicy.artistAttempts(emptyList(), emptySet(), null, true).isEmpty())
    }

    @Test
    fun onlyArtistPhotosCanRemainCachedWithoutRetry() {
        assertFalse(MixPosterPolicy.needsPhotoRetry(MixPosterPhotoSource.ARTIST))
        assertTrue(MixPosterPolicy.needsPhotoRetry(MixPosterPhotoSource.TRACK))
        assertTrue(MixPosterPolicy.needsPhotoRetry(MixPosterPhotoSource.PLAYLIST))
        assertTrue(MixPosterPolicy.needsPhotoRetry(MixPosterPhotoSource.GRAPHIC))
    }

    @Test
    fun remainsStableAcrossArtistChangesUntilSevenDaysPass() {
        val generated = 1_000L
        assertFalse(MixPosterPolicy.needsRefresh(generated, generated + MixPosterPolicy.WEEK_MS - 1))
        assertFalse(MixPosterPolicy.needsRefresh(generated, generated + 1))
        assertTrue(MixPosterPolicy.needsRefresh(generated, generated + MixPosterPolicy.WEEK_MS))
    }

    @Test
    fun unavailableArtistMetadataDoesNotInvalidateFreshPhoto() {
        assertFalse(MixPosterPolicy.needsRefresh(1_000, 1_001))
    }

    @Test
    fun missingOrFallbackEntriesNeedARealPhoto() {
        assertTrue(MixPosterPolicy.needsRefresh(0, 1_001))
    }

    @Test
    fun prefersFirstArtistButAvoidsDuplicatesAcrossMixes() {
        assertEquals("first", MixPosterPolicy.chooseArtist(listOf("first", "second"), emptySet(), null, false))
        assertEquals("second", MixPosterPolicy.chooseArtist(listOf("first", "second"), setOf("first"), null, false))
    }

    @Test
    fun rotatesToAnotherArtistWhenPossible() {
        assertEquals("second", MixPosterPolicy.chooseArtist(listOf("first", "second"), emptySet(), "first", true))
        assertEquals("third", MixPosterPolicy.chooseArtist(listOf("first", "second", "third"), setOf("second"), "first", true))
    }

    @Test
    fun permitsRepetitionOnlyWhenNoAlternativeExists() {
        assertEquals("first", MixPosterPolicy.chooseArtist(listOf("first", "first"), setOf("first"), "first", true))
        assertNull(MixPosterPolicy.chooseArtist(emptyList(), emptySet(), null, true))
    }

    @Test
    fun rejectsPhotosBelowFortyPercentAverageLuminance() {
        fun gray(value: Int) = (0xFF shl 24) or (value shl 16) or (value shl 8) or value

        assertTrue(MixPosterPolicy.isPhotoBrightEnough(intArrayOf(gray(102), gray(102))))
        assertFalse(MixPosterPolicy.isPhotoBrightEnough(intArrayOf(gray(101), gray(101))))
        assertFalse(MixPosterPolicy.isPhotoBrightEnough(intArrayOf()))
        assertTrue(MixPosterPolicy.averageLuminance(intArrayOf(gray(120)))!! >
            MixPosterPolicy.averageLuminance(intArrayOf(gray(80)))!!)
    }

    @Test
    fun retainsBrighterDarkArtistAndRejectsPlaylistThumbnailAsArtistSource() {
        assertTrue(MixPosterPolicy.isBrighterThan(0.32f, 0.21f))
        assertFalse(MixPosterPolicy.isBrighterThan(0.21f, 0.32f))
        assertTrue(MixPosterPolicy.isArtistSource(MixPosterPhotoSource.ARTIST, "artist.jpg", "mix.jpg"))
        assertFalse(MixPosterPolicy.isArtistSource(MixPosterPhotoSource.ARTIST, "mix.jpg", "mix.jpg"))
        assertFalse(MixPosterPolicy.isArtistSource(MixPosterPhotoSource.PLAYLIST, "mix.jpg", "mix.jpg"))
    }
}
