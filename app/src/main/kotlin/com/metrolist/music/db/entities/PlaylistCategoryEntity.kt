/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.apache.commons.lang3.RandomStringUtils
import java.time.LocalDateTime

@Entity(tableName = "playlist_category")
data class PlaylistCategoryEntity(
    @PrimaryKey val id: String = generateCategoryId(),
    @ColumnInfo(index = true) val playlistId: String,
    val name: String,
    val colorHex: String? = null,
    val position: Int = 0,
    val isAuto: Boolean = false,
    val createdAt: LocalDateTime? = LocalDateTime.now(),
) {
    companion object {
        fun generateCategoryId() = "PC" + RandomStringUtils.insecure().next(8, true, false)
    }
}
