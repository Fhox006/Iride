/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.models

import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.LocalItem

data class ForYouShelfItem(
    val artist: Artist,
    // Exactly 3 albums by `artist`, most-listened first, no duplicates.
    val tiles: List<LocalItem>,
)
