package com.metrolist.music.ui.component

import com.metrolist.music.data.remote.ArtistArtworkSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistPortraitQualityTest {
    private data class Candidate(val source: ArtistArtworkSource, val quality: ArtistArtworkQuality)

    private fun choose(vararg candidates: Candidate) = preferredArtistArtwork(
        candidates.toList(), { it.source }, { it.quality.borders >= 0.25f }, { it.quality },
    )?.source

    @Test
    fun spotifyPortraitBeatsHighResolutionWhiteApplePoster() {
        assertEquals(ArtistArtworkSource.SPOTIFY, choose(
            Candidate(ArtistArtworkSource.APPLE_MUSIC, ArtistArtworkQuality(0f, 1f, 0.35f)),
            Candidate(ArtistArtworkSource.SPOTIFY, ArtistArtworkQuality(0.85f, 0.80f, 0.65f)),
            Candidate(ArtistArtworkSource.YOUTUBE_MUSIC, ArtistArtworkQuality(0.7f, 0.55f, 0.5f)),
        ))
    }

    @Test
    fun youtubeCanSupplyTheBestPortraitWithGoodBorders() {
        assertEquals(ArtistArtworkSource.YOUTUBE_MUSIC, choose(
            Candidate(ArtistArtworkSource.APPLE_MUSIC, ArtistArtworkQuality(0f, 1f, 0.35f)),
            Candidate(ArtistArtworkSource.SPOTIFY, ArtistArtworkQuality(0.4f, 0.5f, 0.6f)),
            Candidate(ArtistArtworkSource.YOUTUBE_MUSIC, ArtistArtworkQuality(0.9f, 0.9f, 0.8f)),
        ))
    }

    @Test
    fun rejectsChaoticBordersEvenWithLargeSharpFace() {
        assertEquals(ArtistArtworkSource.SPOTIFY, choose(
            Candidate(ArtistArtworkSource.APPLE_MUSIC, ArtistArtworkQuality(1f, 0.1f, 1f)),
            Candidate(ArtistArtworkSource.SPOTIFY, ArtistArtworkQuality(0.8f, 0.85f, 0.7f)),
        ))
    }

    @Test
    fun resolutionAndBordersStillMatterBetweenPortraits() {
        assertEquals(ArtistArtworkSource.SPOTIFY, choose(
            Candidate(ArtistArtworkSource.APPLE_MUSIC, ArtistArtworkQuality(0.8f, 0.4f, 0.35f)),
            Candidate(ArtistArtworkSource.SPOTIFY, ArtistArtworkQuality(0.8f, 0.9f, 0.9f)),
        ))
    }

    @Test
    fun preservesServiceOrderForEquivalentQualityAndSmallDifferences() {
        assertEquals(ArtistArtworkSource.APPLE_MUSIC, choose(
            Candidate(ArtistArtworkSource.SPOTIFY, ArtistArtworkQuality(0.82f, 0.9f, 0.9f)),
            Candidate(ArtistArtworkSource.APPLE_MUSIC, ArtistArtworkQuality(0.8f, 0.9f, 0.9f)),
        ))
        assertEquals(ArtistArtworkSource.SPOTIFY, choose(
            Candidate(ArtistArtworkSource.YOUTUBE_MUSIC, ArtistArtworkQuality(0.8f, 0.9f, 0.9f)),
            Candidate(ArtistArtworkSource.SPOTIFY, ArtistArtworkQuality(0.8f, 0.9f, 0.9f)),
        ))
    }

    @Test
    fun detectorFailureCannotReplaceDefinedApplePhotoForTidierBorders() {
        assertEquals(ArtistArtworkSource.APPLE_MUSIC, choose(
            Candidate(ArtistArtworkSource.APPLE_MUSIC, ArtistArtworkQuality(0f, 0.5f, 1f)),
            Candidate(ArtistArtworkSource.SPOTIFY, ArtistArtworkQuality(0f, 1f, 0.7f)),
        ))
    }

    @Test
    fun faceAloneCannotReplaceSharpApplePhoto() {
        assertEquals(ArtistArtworkSource.APPLE_MUSIC, choose(
            Candidate(ArtistArtworkSource.APPLE_MUSIC, ArtistArtworkQuality(0f, 0.9f, 0.9f)),
            Candidate(ArtistArtworkSource.SPOTIFY, ArtistArtworkQuality(1f, 0.9f, 0.9f)),
        ))
    }

    @Test
    fun missingAppleFaceDoesNotJustifyAnEnlargedSpotifyPortrait() {
        assertEquals(ArtistArtworkSource.APPLE_MUSIC, choose(
            Candidate(ArtistArtworkSource.APPLE_MUSIC, ArtistArtworkQuality(0f, 0.9f, 0.35f)),
            Candidate(ArtistArtworkSource.SPOTIFY, ArtistArtworkQuality(1f, 0.9f, 0.4f)),
        ))
    }

    @Test
    fun missingAppleFaceDoesNotJustifyBusySpotifyBackground() {
        assertEquals(ArtistArtworkSource.APPLE_MUSIC, choose(
            Candidate(ArtistArtworkSource.APPLE_MUSIC, ArtistArtworkQuality(0f, 0.9f, 0.35f)),
            Candidate(ArtistArtworkSource.SPOTIFY, ArtistArtworkQuality(1f, 0.5f, 1f)),
        ))
    }

    @Test
    fun modestImprovementCannotReplaceDecentApplePortrait() {
        assertEquals(ArtistArtworkSource.APPLE_MUSIC, choose(
            Candidate(ArtistArtworkSource.APPLE_MUSIC, ArtistArtworkQuality(0.8f, 0.55f, 0.4f)),
            Candidate(ArtistArtworkSource.SPOTIFY, ArtistArtworkQuality(0.8f, 0.9f, 0.9f)),
        ))
    }

    @Test
    fun meaningfulTechnicalUpgradeCanReplaceAppleWithoutFaceDetection() {
        assertEquals(ArtistArtworkSource.SPOTIFY, choose(
            Candidate(ArtistArtworkSource.APPLE_MUSIC, ArtistArtworkQuality(0f, 0.45f, 0.35f)),
            Candidate(ArtistArtworkSource.SPOTIFY, ArtistArtworkQuality(0f, 0.9f, 0.85f)),
        ))
    }

    @Test
    fun appleReplacementIsIndependentOfDownloadCompletionOrder() {
        val apple = Candidate(ArtistArtworkSource.APPLE_MUSIC, ArtistArtworkQuality(0f, 1f, 0.35f))
        val spotify = Candidate(ArtistArtworkSource.SPOTIFY, ArtistArtworkQuality(0.85f, 0.8f, 0.65f))
        val youtube = Candidate(ArtistArtworkSource.YOUTUBE_MUSIC, ArtistArtworkQuality(0.5f, 0.75f, 0.6f))
        assertEquals(choose(apple, spotify, youtube), choose(youtube, spotify, apple))
        assertEquals(ArtistArtworkSource.SPOTIFY, choose(youtube, apple, spotify))
    }

    @Test
    fun sharpIllustrationWithPerfectCornersCannotBeatGoodBlendingPortrait() {
        assertEquals(ArtistArtworkSource.SPOTIFY, choose(
            Candidate(ArtistArtworkSource.APPLE_MUSIC, ArtistArtworkQuality(0f, 1f, 1f, 1f, 0.15f)),
            Candidate(ArtistArtworkSource.SPOTIFY, ArtistArtworkQuality(0.85f, 0.85f, 0.7f, 0.85f, 0.95f)),
        ))
    }

    @Test
    fun accidentalFaceDetectionInGraphicDoesNotOutrankRealPortrait() {
        assertEquals(ArtistArtworkSource.SPOTIFY, choose(
            Candidate(ArtistArtworkSource.APPLE_MUSIC, ArtistArtworkQuality(0.95f, 1f, 1f, 1f, 0.15f)),
            Candidate(ArtistArtworkSource.SPOTIFY, ArtistArtworkQuality(0.85f, 0.85f, 0.7f, 0.85f, 0.95f)),
        ))
    }

    @Test
    fun photographicBlackAndWhiteApplePortraitRetainsServicePreference() {
        assertEquals(ArtistArtworkSource.APPLE_MUSIC, choose(
            Candidate(ArtistArtworkSource.APPLE_MUSIC, ArtistArtworkQuality(0.85f, 0.9f, 0.9f, 0.9f, 0.95f)),
            Candidate(ArtistArtworkSource.SPOTIFY, ArtistArtworkQuality(0.9f, 0.9f, 0.9f, 0.9f, 0.95f)),
        ))
    }

    @Test
    fun goodFaceAndCornersCannotHidePoorLowerBlend() {
        assertEquals(ArtistArtworkSource.APPLE_MUSIC, choose(
            Candidate(ArtistArtworkSource.APPLE_MUSIC, ArtistArtworkQuality(0.8f, 0.85f, 0.8f, 0.85f)),
            Candidate(ArtistArtworkSource.SPOTIFY, ArtistArtworkQuality(1f, 1f, 1f, 0.3f)),
        ))
    }

    @Test
    fun existingPortraitSurvivesTemporaryProviderFailure() {
        val previous = Candidate(ArtistArtworkSource.SPOTIFY, ArtistArtworkQuality(0.85f, 0.85f, 0.8f))
        val chosen = preferredArtistArtwork(
            listOf(Candidate(ArtistArtworkSource.YOUTUBE_MUSIC, ArtistArtworkQuality(0.2f, 0.7f, 0.6f))),
            { it.source }, { it.quality.borderScore >= 0.25f }, { it.quality }, previous,
        )
        assertEquals(previous, chosen)
    }

    @Test
    fun establishedAlternativeCannotBeReplacedAgainEvenByBetterApplePhoto() {
        val previous = Candidate(ArtistArtworkSource.SPOTIFY, ArtistArtworkQuality(0.6f, 0.7f, 0.6f))
        assertEquals(previous, preferredArtistArtwork(
            listOf(Candidate(ArtistArtworkSource.APPLE_MUSIC, ArtistArtworkQuality(1f, 1f, 1f))),
            { it.source }, { it.quality.borderScore >= 0.25f }, { it.quality }, previous,
        ))
    }

    @Test
    fun usableApplePhotoWinsAcrossBroadRangeOfAlternativeImprovements() {
        for (portrait in listOf(0f, 0.5f, 1f)) for (borders in listOf(0.4f, 0.7f, 1f)) {
            for (definition in listOf(0.45f, 0.7f, 1f)) {
                assertEquals(ArtistArtworkSource.APPLE_MUSIC, choose(
                    Candidate(ArtistArtworkSource.APPLE_MUSIC, ArtistArtworkQuality(portrait, borders, definition)),
                    Candidate(ArtistArtworkSource.SPOTIFY, ArtistArtworkQuality(1f, 1f, 1f)),
                ))
            }
        }
    }

    @Test
    fun smallScoreChangeDoesNotReplaceEstablishedPhoto() {
        val previous = Candidate(ArtistArtworkSource.SPOTIFY, ArtistArtworkQuality(0.8f, 0.85f, 0.8f))
        val chosen = preferredArtistArtwork(
            listOf(Candidate(ArtistArtworkSource.SPOTIFY, ArtistArtworkQuality(0.9f, 0.85f, 0.8f))),
            { it.source }, { it.quality.borderScore >= 0.25f }, { it.quality }, previous,
        )
        assertEquals(previous, chosen)
    }

    @Test
    fun cachedIllustrationCannotPreventReplacementByGoodPortrait() {
        val previous = Candidate(ArtistArtworkSource.APPLE_MUSIC, ArtistArtworkQuality(0f, 1f, 1f, 1f, 0.15f))
        val portrait = Candidate(ArtistArtworkSource.SPOTIFY, ArtistArtworkQuality(0.85f, 0.85f, 0.7f, 0.85f, 0.95f))
        assertEquals(portrait, preferredArtistArtwork(
            listOf(portrait), { it.source }, { it.quality.borderScore >= 0.25f }, { it.quality }, previous,
        ))
    }

    @Test
    fun scoresVisiblePortraitAboveTinyWeakOrClippedFace() {
        val portrait = artistPortraitQuality(0.85f, 0.12f, 0.5f, 0.4f)
        assertTrue(portrait > 0.8f)
        assertTrue(artistPortraitQuality(0.4f, 0.12f, 0.5f, 0.4f) > 0f)
        assertEquals(0f, artistPortraitQuality(0.9f, 0.02f, 0.5f, 0.4f), 0f)
        assertEquals(0f, artistPortraitQuality(0.3f, 0.12f, 0.5f, 0.4f), 0f)
        assertEquals(0f, artistPortraitQuality(0.9f, 0.25f, 0.02f, 0.1f), 0f)
        assertEquals(0f, artistPortraitQuality(Float.NaN, 0.12f, 0.5f, 0.4f), 0f)
    }

    @Test
    fun acceptsOffCenterPortraitWhenHeadIsStillVisible() {
        assertTrue(artistPortraitQuality(0.85f, 0.12f, 0.25f, 0.4f) > 0.8f)
    }

    @Test
    fun realResolutionAndDetailsBeatEnlargedOrBlankImage() {
        val pixels = IntArray(64 * 64) { index ->
            if ((index / 64 + index % 64) % 2 == 0) 0xFF222222.toInt() else 0xFFDDDDDD.toInt()
        }
        val native = artistDefinitionQuality(pixels, 64, 64, 1200)
        val enlarged = artistDefinitionQuality(pixels, 64, 64, 160)
        val blank = artistDefinitionQuality(IntArray(64 * 64) { -1 }, 64, 64, 1200)
        assertTrue(native > enlarged)
        assertTrue(native > blank)
    }
}
