package com.metrolist.music.ui.component

import com.metrolist.music.R

/** One ordering and grouping policy for collection, selection and player action menus. */
internal fun folioActionOrder(id: Int?): Pair<Int, Int> =
    when (id) {
        R.string.play_next -> 0 to 0
        R.string.add_to_queue -> 0 to 1
        R.string.like_all,
        R.string.dislike_all -> 1 to 0
        R.string.add_to_playlist -> 1 to 1
        R.string.add_to_library,
        R.string.remove_from_library,
        R.string.subscribe,
        R.string.subscribed -> 1 to 2
        R.string.action_download,
        R.string.remove_download,
        R.string.downloading -> 1 to 3
        R.string.go_to_source -> 2 to -1
        R.string.view_artist,
        R.string.song_menu_view_artists,
        R.string.song_menu_view_collaborators -> 2 to 0
        R.string.view_album -> 2 to 1
        R.string.other_versions -> 2 to 2
        R.string.share -> 3 to 0
        R.string.export_playlist -> 3 to 1
        R.string.edit -> 4 to 0
        R.string.reorder -> 4 to 1
        R.string.action_sync -> 4 to 2
        R.string.select -> 4 to 3
        R.string.sleep_timer -> 4 to 4
        R.string.repeat_one -> 4 to 5
        R.string.folio_dissolve -> 4 to 5
        R.string.listen_together -> 4 to 6
        R.string.details -> 4 to 7
        R.string.advanced -> 4 to 8
        R.string.lyrics_offset -> 0 to 0
        R.string.search -> 0 to 1
        R.string.refetch -> 4 to 9
        R.string.romanize_current_track -> 4 to 10
        R.string.delete,
        R.string.delete_uploaded_songs -> 5 to 0
        else -> 4 to 10
    }

private val collectionPlaybackActions =
    setOf(R.string.play, R.string.shuffle, R.string.radio, R.string.start_radio)

internal fun <T> folioContextItems(
    items: List<T>,
    actionId: (T) -> Int?,
    section: (T) -> Int? = { null },
): List<T> =
    items
        .filter { actionId(it) !in collectionPlaybackActions }
        .distinctBy { actionId(it) ?: it }
        .sortedWith(
            compareBy(
                { section(it) ?: folioActionOrder(actionId(it)).first },
                { folioActionOrder(actionId(it)).second },
            )
        )
