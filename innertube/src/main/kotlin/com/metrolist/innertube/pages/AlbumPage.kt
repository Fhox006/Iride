package com.metrolist.innertube.pages

import com.metrolist.innertube.models.Album
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.MusicResponsiveHeaderRenderer
import com.metrolist.innertube.models.MusicResponsiveListItemRenderer
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.getItems
import com.metrolist.innertube.models.oddElements
import com.metrolist.innertube.models.response.BrowseResponse
import com.metrolist.innertube.models.splitBySeparator
import com.metrolist.innertube.utils.parseTime
import java.util.Locale

data class AlbumPage(
    val album: AlbumItem,
    val songs: List<SongItem>,
    val otherVersions: List<AlbumItem>,
    val similarAlbums: List<AlbumItem> = emptyList(),
    val unavailableSongs: List<String> = emptyList(),
    val releaseDate: String? = null,
) {
    companion object {
        /** Shelf count and order vary: a lone carousel can contain alternate editions. */
        fun albumShelves(response: BrowseResponse, album: AlbumItem): Pair<List<AlbumItem>, List<AlbumItem>> {
            val primary = response.contents?.singleColumnBrowseResultsRenderer?.tabs
                ?: response.contents?.twoColumnBrowseResultsRenderer?.tabs
            val sections = primary?.flatMap { it?.tabRenderer?.content?.sectionListRenderer?.contents.orEmpty() }.orEmpty() +
                response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer?.contents.orEmpty()
            val versions = mutableListOf<AlbumItem>()
            val recommendations = mutableListOf<AlbumItem>()
            fun releaseName(value: String) = value.lowercase(Locale.ROOT).replace(
                Regex("""\s*(?:\([^)]*\)|\[[^\]]*\]|[-–:]\s*(?:deluxe|expanded|remaster|anniversary).*)"""), "",
            ).trim()
            for (shelf in sections.mapNotNull { it.musicCarouselShelfRenderer }) {
                val title = shelf.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs.orEmpty()
                    .joinToString("") { it.text }.lowercase(Locale.ROOT)
                val isVersionsShelf = listOf("other version", "altre version", "otras version", "autres version", "andere version", "outras vers").any { it in title }
                val albums = shelf.contents.mapNotNull { it.musicTwoRowItemRenderer }.mapNotNull { renderer ->
                    if (!renderer.isAlbum && renderer.navigationEndpoint.browseEndpoint?.browseId?.startsWith("MPRE") != true) return@mapNotNull null
                    val parsed = NewReleaseAlbumPage.fromMusicTwoRowItemRenderer(renderer)
                        ?: (ArtistPage.fromMusicTwoRowItemRenderer(renderer) as? AlbumItem)
                        ?: return@mapNotNull null
                    if (isVersionsShelf && parsed.artists.isNullOrEmpty()) parsed.copy(artists = album.artists) else parsed
                }
                for (item in albums) {
                    if (item.id == album.id) continue
                    val sameArtist = item.artists.orEmpty().any { candidate -> album.artists.orEmpty().any {
                        (candidate.id != null && candidate.id == it.id) || candidate.name.equals(it.name, true)
                    } }
                    if (isVersionsShelf || (sameArtist && releaseName(item.title) == releaseName(album.title))) versions += item
                    else recommendations += item
                }
            }
            val versionIds = versions.map { it.id }.toSet()
            return versions.distinctBy { it.id } to recommendations.distinctBy { it.id }.filterNot { it.id in versionIds }
        }

        fun getPlaylistId(response: BrowseResponse): String? {
            var playlistId = response.microformat?.microformatDataRenderer?.urlCanonical?.substringAfterLast('=')
            if (playlistId == null)
            {
                playlistId = response.header?.musicDetailHeaderRenderer?.menu?.menuRenderer?.topLevelButtons?.firstOrNull()
                    ?.buttonRenderer?.navigationEndpoint?.watchPlaylistEndpoint?.playlistId
            }
            return playlistId
        }

        fun getTitle(response: BrowseResponse): String? {
            val title = getHeader(response)?.title ?: response.header?.musicDetailHeaderRenderer?.title
            return title?.runs?.firstOrNull()?.text
        }

        fun getYear(response: BrowseResponse): Int? {
            val title = getHeader(response)?.subtitle ?: response.header?.musicDetailHeaderRenderer?.subtitle
            return title?.runs?.lastOrNull()?.text?.toIntOrNull()
        }

        fun getThumbnail(response: BrowseResponse): String? {
            return response.background?.musicThumbnailRenderer?.getThumbnailUrl() ?: response.header?.musicDetailHeaderRenderer?.thumbnail
                ?.croppedSquareThumbnailRenderer?.getThumbnailUrl()
        }

        fun getArtists(response: BrowseResponse): List<Artist> {
            val artists = getHeader(response)?.straplineTextOne?.runs?.oddElements()?.map {
                Artist(
                    name = it.text,
                    id = it.navigationEndpoint?.browseEndpoint?.browseId
                )
            } ?: response.header?.musicDetailHeaderRenderer?.subtitle?.runs?.splitBySeparator()?.getOrNull(1)?.oddElements()?.map {
                Artist(
                    name = it.text,
                    id = it.navigationEndpoint?.browseEndpoint?.browseId
                )
            } ?: emptyList()

            return artists
        }

        private fun getHeader(response: BrowseResponse): MusicResponsiveHeaderRenderer? {
            val tabs = response.contents?.singleColumnBrowseResultsRenderer?.tabs
                ?: response.contents?.twoColumnBrowseResultsRenderer?.tabs
            val section =
                tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
            val header = section?.musicResponsiveHeaderRenderer
            return header
        }

        fun getSongs(response: BrowseResponse, album: AlbumItem): List<SongItem> {
            val tabs = response.contents?.singleColumnBrowseResultsRenderer?.tabs ?: response.contents?.twoColumnBrowseResultsRenderer?.tabs
            val shelfRenderer = tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicShelfRenderer ?:
                response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer?.contents?.firstOrNull()?.musicShelfRenderer

            val songs = shelfRenderer?.contents?.getItems()?.mapNotNull {
                getSong(it, album)
            }
            return songs ?: emptyList()
        }

        /** Album track shelves can be returned in different columns depending on release type. */
        fun getPlaylistSongs(response: BrowseResponse, album: AlbumItem): List<SongItem> {
            val tabSections = (response.contents?.singleColumnBrowseResultsRenderer?.tabs
                ?: response.contents?.twoColumnBrowseResultsRenderer?.tabs)
                ?.flatMap { it?.tabRenderer?.content?.sectionListRenderer?.contents.orEmpty() }
                .orEmpty()
            val secondarySections = response.contents?.twoColumnBrowseResultsRenderer
                ?.secondaryContents?.sectionListRenderer?.contents.orEmpty()
            val sections = tabSections + secondarySections + response.contents?.sectionListRenderer?.contents.orEmpty()
            val songs = sections.flatMap { section ->
                section.musicPlaylistShelfRenderer?.contents?.getItems().orEmpty() +
                    section.musicShelfRenderer?.contents?.getItems().orEmpty()
            }.mapNotNull { renderer ->
                getSong(renderer, album)
            }
            return songs.distinctBy { it.id }
        }

        fun getSong(renderer: MusicResponsiveListItemRenderer, album: AlbumItem? = null): SongItem? {
            val libraryTokens = PageHelper.extractLibraryTokensFromMenuItems(renderer.menu?.menuRenderer?.items)

            return SongItem(
                id = renderer.playlistItemData?.videoId ?: return null,
                title = PageHelper.extractRuns(renderer.flexColumns, "MUSIC_VIDEO").firstOrNull()?.text ?: return null,
                artists = PageHelper.extractRuns(renderer.flexColumns, "MUSIC_PAGE_TYPE_ARTIST").map{
                    Artist(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId
                    )
                }.ifEmpty {
                    album?.artists ?: emptyList()
                },
                album = album?.let {
                    Album(it.title, it.browseId)
                } ?: renderer.flexColumns.getOrNull(2)?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()?.let {
                    Album(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId!!
                    )
                }!!,
                duration = renderer.fixedColumns?.firstOrNull()
                    ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()
                    ?.text?.parseTime() ?: return null,
                musicVideoType = renderer.musicVideoType,
                thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: album?.thumbnail!!,
                explicit = renderer.badges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null,
                libraryAddToken = libraryTokens.addToken,
                libraryRemoveToken = libraryTokens.removeToken
            )
        }
    }
}
