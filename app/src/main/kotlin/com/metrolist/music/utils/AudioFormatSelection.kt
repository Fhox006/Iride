/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */
/*
 * Adapted from ArchiveTune, GPL-3.0, by the Iride contributors (2026).
 * Source: https://github.com/rukamori/ArchiveTune/blob/cdf8d6c18ced70360668c88b45c303c550b11262/app/src/main/kotlin/moe/rukamori/archivetune/utils/YTPlayerUtils.kt
 * selectAudioFormatCandidates, extractCodec and codecRank; original contributors in upstream Git history.
 */
package com.metrolist.music.utils

import com.metrolist.innertube.models.response.PlayerResponse
import com.metrolist.music.constants.AudioQuality
import timber.log.Timber

internal object AudioFormatSelection {
    private const val logTag = "AudioFormatSelection"
    fun candidates(
        playerResponse: PlayerResponse,
        audioQuality: AudioQuality,
        networkMetered: Boolean,
    ): List<PlayerResponse.StreamingData.Format> {
        Timber.tag(logTag).i("Finding format with audioQuality: $audioQuality, network metered: $networkMetered")

        val audioFormats =
            playerResponse.streamingData
                ?.adaptiveFormats
                ?.asSequence()
                ?.filter { it.isAudio && it.isOriginal && it.bitrate > 0 }
                ?.filter { it.url != null || it.signatureCipher != null || it.cipher != null }
                ?.toList()
                .orEmpty()

        if (audioFormats.isEmpty()) return emptyList()

        val effectiveQuality =
            when (audioQuality) {
                AudioQuality.AUTO -> if (networkMetered) AudioQuality.HIGH else AudioQuality.HIGHEST
                else -> audioQuality
            }

        val targetBitrateBps =
            when (effectiveQuality) {
                AudioQuality.LOW -> 70_000
                AudioQuality.HIGH -> 160_000
                AudioQuality.HIGHEST -> 320_000
                AudioQuality.AUTO -> null
            }

        val preferHigher =
            compareByDescending<PlayerResponse.StreamingData.Format> { it.bitrate }
                .thenByDescending { codecRank(extractCodec(it.mimeType)) }
                .thenByDescending { it.url != null }
                .thenByDescending { it.audioSampleRate ?: 0 }

        val preferLowerAboveTarget =
            compareBy<PlayerResponse.StreamingData.Format> { it.bitrate }
                .thenByDescending { codecRank(extractCodec(it.mimeType)) }
                .thenByDescending { it.url != null }
                .thenByDescending { it.audioSampleRate ?: 0 }

        val candidates =
            when {
                targetBitrateBps == null || effectiveQuality == AudioQuality.HIGHEST -> {
                    audioFormats.sortedWith(preferHigher)
                }

                else -> {
                    val preferred =
                        audioFormats
                            .filter { it.bitrate <= targetBitrateBps }
                            .sortedWith(preferHigher)
                    val fallback =
                        audioFormats
                            .filter { it.bitrate > targetBitrateBps }
                            .sortedWith(preferLowerAboveTarget)

                    preferred + fallback
                }
            }

        Timber
            .tag(logTag)
            .v(
                "Available audio formats: ${
                    candidates.take(12).map {
                        val codec = extractCodec(it.mimeType)
                        val direct = if (it.url != null) "direct" else "cipher"
                        "${it.mimeType} ($direct, codec=${codec ?: "unknown"}) @ ${it.bitrate}bps"
                    }
                }",
            )

        return candidates
    }

    private fun extractCodec(mimeType: String): String? {
        val match = Regex("""codecs="([^"]+)"""").find(mimeType) ?: return null
        return match.groupValues
            .getOrNull(1)
            ?.split(",")
            ?.firstOrNull()
            ?.trim()
    }

    private fun codecRank(codec: String?): Int =
        when {
            codec.isNullOrBlank() -> 0
            codec.contains("opus", ignoreCase = true) -> 3
            codec.contains("mp4a", ignoreCase = true) -> 2
            else -> 1
        }

}
