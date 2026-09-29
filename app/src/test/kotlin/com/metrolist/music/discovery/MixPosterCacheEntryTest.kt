package com.metrolist.music.discovery

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MixPosterCacheEntryTest {
    @Test
    fun missingRendererVersionIdentifiesOldImagesAndKeepsPhotoReferences() {
        val entry = Json.decodeFromString<MixPosterCacheEntry>(
            """{"mixId":"mix","title":"Mix 1","mood":"CHILL","photoUrl":"https://example.com/photo.jpg","seed":42,"imageName":"old.png","size":448}""",
        )
        assertEquals(0, entry.rendererVersion)
        assertEquals("https://example.com/photo.jpg", entry.photoUrl)
        assertEquals(MixPosterPhotoSource.ARTIST, entry.photoSource)
    }

    @Test
    fun explicitRendererVersionIsSavedWithDefaultJsonSettings() {
        val entry = MixPosterCacheEntry(
            mixId = "mix", title = "Mix 1", mood = "CHILL", seed = 42,
            imageName = "poster.png", size = 448, rendererVersion = MixPosterRenderer.VERSION,
        )
        val encoded = Json.encodeToString(entry)
        assertTrue(encoded.contains("\"rendererVersion\":${MixPosterRenderer.VERSION}"))
        assertEquals(entry, Json.decodeFromString<MixPosterCacheEntry>(encoded))
    }

    @Test
    fun fallbackSourceSurvivesCacheRoundTrip() {
        val entry = MixPosterCacheEntry(
            mixId = "mix", title = "Mix 1", mood = "CHILL", seed = 42,
            imageName = "poster.png", size = 448, photoSource = MixPosterPhotoSource.TRACK,
        )
        val restored = Json.decodeFromString<MixPosterCacheEntry>(Json.encodeToString(entry))
        assertEquals(MixPosterPhotoSource.TRACK, restored.photoSource)
        assertTrue(MixPosterPolicy.needsPhotoRetry(restored.photoSource))
    }
    @Test
    fun graphicRerenderPreservesPhotoAgeAndOriginalReference() {
        val entry = MixPosterCacheEntry(
            mixId = "mix", title = "Mix 1", mood = "CHILL", seed = 42,
            imageName = "new-poster.png", size = 448, rendererVersion = MixPosterRenderer.VERSION,
            generatedAt = 1_000 + MixPosterPolicy.WEEK_MS,
            firstArtistId = "artist", sourceImageName = "original.source.png", photoFetchedAt = 1_000,
        )
        val restored = Json.decodeFromString<MixPosterCacheEntry>(Json.encodeToString(entry))
        assertEquals("original.source.png", restored.sourceImageName)
        assertEquals(1_000L, restored.photoFetchedAt)
        assertTrue(MixPosterPolicy.needsRefresh(restored.photoFetchedAt, entry.generatedAt))
    }

    @Test
    fun newFirstTrackWithSameSingerDoesNotRefreshPhoto() {
        val original = MixPosterCacheEntry(
            mixId = "mix", title = "Mix 1", mood = "CHILL", seed = 42,
            imageName = "poster.png", size = 448, firstTrackId = "old-track",
            firstArtistId = "artist", photoFetchedAt = 1_000,
        )
        val updated = original.copy(firstTrackId = "new-track")
        assertFalse(MixPosterPolicy.needsRefresh(updated.photoFetchedAt, 2_000))
    }

}
