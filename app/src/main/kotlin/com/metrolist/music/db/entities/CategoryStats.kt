/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.db.entities

import androidx.compose.runtime.Immutable

@Immutable
data class CategoryStats(
    val name: String,
    val colorHex: String?,
    val songCountListened: Int,
    val timeListened: Long?,
)
