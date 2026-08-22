/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import com.metrolist.music.BuildConfig
import com.metrolist.music.R
import com.metrolist.music.constants.UpdateDownloadIdKey
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface UpdateDownloadState {
    data object Idle : UpdateDownloadState

    data class Downloading(val progressPercent: Int) : UpdateDownloadState

    data class Failed(val reason: String? = null) : UpdateDownloadState

    data object ReadyToInstall : UpdateDownloadState
}

object UpdateDownloader {
    private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    private const val NO_DOWNLOAD_ID = -1L

    fun getUpdateDirectory(context: Context): File =
        File(context.getExternalFilesDir(null), "update").apply { mkdirs() }

    fun canInstallPackages(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings(context: Context) {
        context.startActivity(
            Intent(
                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    suspend fun enqueueUpdate(
        context: Context,
        downloadUrl: String,
        versionName: String,
    ) = withContext(Dispatchers.IO) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        cancelPendingDownload(context, downloadManager)

        getUpdateDirectory(context).listFiles()?.forEach { it.delete() }
        val destination = File(getUpdateDirectory(context), "Iride-${versionName}.apk")

        val request =
            DownloadManager.Request(downloadUrl.toUri())
                .setTitle("Iride $versionName")
                .setDescription(context.getString(R.string.update_download_notification_description))
                .setMimeType(APK_MIME_TYPE)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.fromFile(destination))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

        val id = downloadManager.enqueue(request)
        context.dataStore.edit { it[UpdateDownloadIdKey] = id }
    }

    fun queryDownloadState(context: Context): UpdateDownloadState {
        val downloadId = context.dataStore.get(UpdateDownloadIdKey, NO_DOWNLOAD_ID)
        if (downloadId == NO_DOWNLOAD_ID) {
            return if (findNewestApk(context) != null) {
                UpdateDownloadState.ReadyToInstall
            } else {
                UpdateDownloadState.Idle
            }
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.query(DownloadManager.Query().setFilterById(downloadId)).use { cursor ->
            if (!cursor.moveToFirst()) return UpdateDownloadState.Idle

            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            return when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> UpdateDownloadState.ReadyToInstall
                DownloadManager.STATUS_FAILED -> {
                    val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                    UpdateDownloadState.Failed(mapFailureReason(context, reason))
                }
                else -> {
                    val bytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val percent = if (total > 0) ((bytes * 100) / total).toInt().coerceIn(0, 100) else 0
                    UpdateDownloadState.Downloading(percent)
                }
            }
        }
    }

    fun onDownloadCompleted(context: Context, receivedDownloadId: Long): Boolean {
        val expectedId = context.dataStore.get(UpdateDownloadIdKey, NO_DOWNLOAD_ID)
        if (expectedId == NO_DOWNLOAD_ID || receivedDownloadId != expectedId) return false

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.query(DownloadManager.Query().setFilterById(expectedId)).use { cursor ->
            if (!cursor.moveToFirst()) return false
            return cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) ==
                DownloadManager.STATUS_SUCCESSFUL
        }
    }

    fun promptInstall(context: Context) {
        val apk = findNewestApk(context) ?: return
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, APK_MIME_TYPE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )
    }

    fun cleanupStaleDownloads(context: Context) {
        getUpdateDirectory(context).listFiles()?.forEach { file ->
            val version = extractVersionFromFileName(file.name)
            if (version != null && Updater.compareVersions(version, BuildConfig.VERSION_NAME) < 0) {
                file.delete()
            }
        }
    }

    private fun cancelPendingDownload(
        context: Context,
        downloadManager: DownloadManager,
    ) {
        val previousId = context.dataStore.get(UpdateDownloadIdKey, NO_DOWNLOAD_ID)
        if (previousId != NO_DOWNLOAD_ID) {
            downloadManager.remove(previousId)
        }
    }

    private fun findNewestApk(context: Context): File? =
        getUpdateDirectory(context)
            .listFiles { file -> file.name.endsWith(".apk") && file.length() > 0 }
            ?.maxByOrNull { it.lastModified() }

    private fun extractVersionFromFileName(fileName: String): String? =
        Regex("^Iride-(.+?)\\.apk$").find(fileName)?.groupValues?.getOrNull(1)

    private fun mapFailureReason(context: Context, reason: Int): String? =
        when (reason) {
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> context.getString(R.string.update_download_error_no_space)
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE,
            DownloadManager.ERROR_HTTP_DATA_ERROR,
            -> context.getString(R.string.update_download_error_connection)
            else -> null
        }
}
