/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Embedded

@Immutable
data class PlaylistCategoryWithCount(
    @Embedded
    val category: PlaylistCategoryEntity,
    val songCount: Int,
)
