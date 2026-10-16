package com.metrolist.music.models

import java.time.YearMonth

data class ReverieEdition(val primary: Int, val figureVariant: Int, val typographyVariant: Int)

/** Calendar-based art direction stays identical when a month is reopened. */
fun reverieEdition(month: YearMonth): ReverieEdition {
    val pigments = intArrayOf(
        0xFF6A8BA9.toInt(), 0xFFB77587.toInt(), 0xFF819D79.toInt(), 0xFF9DA56F.toInt(),
        0xFF6B9C8A.toInt(), 0xFFCC9276.toInt(), 0xFFC79D55.toInt(), 0xFFC7816E.toInt(),
        0xFF92965B.toInt(), 0xFFB47A4F.toInt(), 0xFF886C9D.toInt(), 0xFF628F89.toInt(),
    )
    val serial = month.year.toLong() * 12 + month.monthValue - 1
    return ReverieEdition(pigments[month.monthValue - 1], Math.floorMod(serial, 9), Math.floorMod(serial, 3))
}

fun reverieChapterOrder(month: YearMonth, available: List<ReverieChapter>): List<ReverieChapter> {
    val serial = month.year.toLong() * 12 + month.monthValue - 1
    val data = listOf(ReverieChapter.TIME, ReverieChapter.RHYTHM, ReverieChapter.GENRES, ReverieChapter.COLOURS)
    val offset = Math.floorMod(serial, data.size)
    val ordered = data.drop(offset) + data.take(offset)
    val highlights = if (serial % 2L == 0L) listOf(ReverieChapter.ALBUM, ReverieChapter.ARTIST)
        else listOf(ReverieChapter.ARTIST, ReverieChapter.ALBUM)
    return (listOf(ReverieChapter.OVERTURE) + ordered + highlights + ReverieChapter.SONG + ReverieChapter.POSTER)
        .filter { it in available }
}
