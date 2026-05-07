package com.metrolist.innertube.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ThumbnailRenderer(
    @JsonNames("croppedSquareThumbnailRenderer")
    val musicThumbnailRenderer: MusicThumbnailRenderer?,
    val musicAnimatedThumbnailRenderer: MusicAnimatedThumbnailRenderer?,
    val croppedSquareThumbnailRenderer: MusicThumbnailRenderer?,
) {
    @Serializable
    data class MusicThumbnailRenderer(
        val thumbnail: Thumbnails,
        val thumbnailCrop: String?,
        val thumbnailScale: String?,
    ) {
        fun getThumbnailUrl() = getBestThumbnailUrl(thumbnail.thumbnails.lastOrNull()?.url, 512)

        fun getThumbnailUrl(minSize: Int) = getBestThumbnailUrl(thumbnail.thumbnails.lastOrNull()?.url, minSize)

        fun getArtistThumbnailUrl() = getBestThumbnailUrl(thumbnail.thumbnails.lastOrNull()?.url, 512, isArtist = true)

        companion object {
            fun getBestThumbnailUrl(url: String?, minSize: Int = 512, isArtist: Boolean = false): String? {
                url ?: return null
                return if (isArtist) {
                    url.replace(Regex("=s\\d+.*$"), "=s512-c")
                        .replace(Regex("=w\\d+-h\\d+.*$"), "=s512-c")
                        .let { modified ->
                            if (modified == url && !url.contains("=")) "$url=s512-c" else modified
                        }
                } else {
                    url.replace(Regex("=w\\d+-h\\d+.*$"), "=w${minSize}-h${minSize}-l90-rj")
                        .replace(Regex("=s\\d+.*$"), "=w${minSize}-h${minSize}-l90-rj")
                        .let { modified ->
                            if (modified == url && !url.contains("=")) "$url=w${minSize}-h${minSize}-l90-rj" else modified
                        }
                }
            }
        }
    }

    @Serializable
    data class MusicAnimatedThumbnailRenderer(
        val animatedThumbnail: Thumbnails,
        val backupRenderer: MusicThumbnailRenderer,
    )
}
