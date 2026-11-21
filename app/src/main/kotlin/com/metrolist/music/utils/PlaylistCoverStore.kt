package com.metrolist.music.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.music.db.entities.PlaylistEntity
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun playlistCoverId(id: String): String = when (id) {
    "liked" -> PlaylistEntity.LIKED_PLAYLIST_ID
    "starred" -> PlaylistEntity.STARRED_PLAYLIST_ID
    "downloaded" -> PlaylistEntity.DOWNLOADED_PLAYLIST_ID
    "uploaded" -> "LP_UPLOADED"
    else -> if (id.all { it.isDigit() } && id.isNotEmpty()) "top_playlist/$id" else id.removePrefix("VL")
}

internal fun playlistCoverKey(id: String) = stringPreferencesKey("playlist_cover_${playlistCoverId(id)}")
/** Copy while the picker grant is alive; never persist a temporary gallery or cache URI. */
internal suspend fun importPlaylistCover(context: Context, id: String, uri: Uri) = withContext(Dispatchers.IO) {
    val image = context.imageLoader.execute(
        ImageRequest.Builder(context).data(uri).size(1600, 1600).allowHardware(false).build(),
    ).image?.toBitmap() ?: error("Unable to decode selected image")
    val side = minOf(image.width, image.height)
    val square = Bitmap.createBitmap(image, (image.width - side) / 2, (image.height - side) / 2, side, side)
    val directory = File(context.filesDir, "playlist_covers").apply { check(exists() || mkdirs()) }
    val file = File(directory, "${UUID.randomUUID()}.jpg")
    try {
        file.outputStream().use { check(square.compress(Bitmap.CompressFormat.JPEG, 92, it)) }
        context.dataStore.edit {
            it[playlistCoverKey(id)] = Uri.fromFile(file).toString()
        }
    } catch (error: Exception) {
        file.delete()
        throw error
    }
}
