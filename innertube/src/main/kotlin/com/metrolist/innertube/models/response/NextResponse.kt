package com.metrolist.innertube.models.response

import com.metrolist.innertube.models.FrameworkUpdates
import com.metrolist.innertube.models.NavigationEndpoint
import com.metrolist.innertube.models.OnResponseReceivedEndpoint
import com.metrolist.innertube.models.PlaylistPanelRenderer
import com.metrolist.innertube.models.Tabs
import com.metrolist.innertube.models.YouTubeDataPage
import kotlinx.serialization.Serializable

@Serializable
data class NextResponse(
    val contents: Contents? = null,
    val continuationContents: ContinuationContents? = null,
    val currentVideoEndpoint: NavigationEndpoint? = null,
    // Populated only when this response is a comments continuation (no `contents`/`continuationContents`).
    val onResponseReceivedEndpoints: List<OnResponseReceivedEndpoint>? = null,
    val frameworkUpdates: FrameworkUpdates? = null,
) {
    @Serializable
    data class Contents(
        val singleColumnMusicWatchNextResultsRenderer: SingleColumnMusicWatchNextResultsRenderer?,
        val twoColumnWatchNextResults: YouTubeDataPage.Contents.TwoColumnWatchNextResults?,
    ) {
        @Serializable
        data class SingleColumnMusicWatchNextResultsRenderer(
            val tabbedRenderer: TabbedRenderer?,
        ) {
            @Serializable
            data class TabbedRenderer(
                val watchNextTabbedResultsRenderer: WatchNextTabbedResultsRenderer?,
            ) {
                @Serializable
                data class WatchNextTabbedResultsRenderer(
                    val tabs: List<Tabs.Tab>,
                )
            }
        }
    }

    @Serializable
    data class ContinuationContents(
        val playlistPanelContinuation: PlaylistPanelRenderer,
    )
}
