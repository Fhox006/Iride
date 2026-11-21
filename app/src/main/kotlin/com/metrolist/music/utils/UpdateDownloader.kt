/**
 * Iride Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.app.DownloadManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import com.metrolist.music.BuildConfig
import com.metrolist.music.R
import com.metrolist.music.constants.UpdateDownloadAllowsRoamingKey
import com.metrolist.music.constants.UpdateDownloadIdKey
import com.metrolist.music.constants.UpdateDownloadTagKey
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface UpdateDownloadState {
    data object Idle : UpdateDownloadState

    data class Downloading(val progressPercent: Int) : UpdateDownloadState

    data class Failed(val reason: String? = null) : UpdateDownloadState

    data object ReadyToInstall : UpdateDownloadState
}

object UpdateDownloader {
    private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    private val downloadMutex = Mutex()

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
        releaseTag: String,
        allowOverRoaming: Boolean = false,
    ) = withContext(Dispatchers.IO) {
        downloadMutex.withLock {
            val prefs = context.dataStore.data.first()
            val previousId = prefs[UpdateDownloadIdKey]
            val downloadManager = context.getSystemService(DownloadManager::class.java)
            if (prefs[UpdateDownloadTagKey] == releaseTag && previousId != null) {
                val state = queryDownloadState(context, downloadManager, previousId)
                val needsRoaming = allowOverRoaming && prefs[UpdateDownloadAllowsRoamingKey] != true
                if (state == UpdateDownloadState.ReadyToInstall ||
                    (state is UpdateDownloadState.Downloading && !needsRoaming)
                ) return@withLock
            }

            val destination = File(getUpdateDirectory(context), "Iride-${UUID.randomUUID()}.apk")
            val request =
                DownloadManager.Request(downloadUrl.toUri())
                    .setTitle(context.getString(R.string.update_download_ready_title, versionName))
                    .setDescription(context.getString(R.string.update_download_ready_description))
                    .setMimeType(APK_MIME_TYPE)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                    .setDestinationUri(Uri.fromFile(destination))
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(allowOverRoaming)

            withContext(NonCancellable) {
                previousId?.let { downloadManager.remove(it) }
                val id = downloadManager.enqueue(request)
                context.dataStore.edit {
                    it[UpdateDownloadIdKey] = id
                    it[UpdateDownloadTagKey] = releaseTag
                    it[UpdateDownloadAllowsRoamingKey] = allowOverRoaming
                }
            }
        }
        Unit
    }

    fun queryDownloadState(context: Context, releaseTag: String? = null): UpdateDownloadState {
        val downloadId = trackedDownloadId(context, releaseTag) ?: return UpdateDownloadState.Idle
        return queryDownloadState(context, context.getSystemService(DownloadManager::class.java), downloadId)
    }

    private fun queryDownloadState(
        context: Context,
        downloadManager: DownloadManager,
        downloadId: Long,
    ): UpdateDownloadState {
        downloadManager.query(DownloadManager.Query().setFilterById(downloadId)).use { cursor ->
            if (!cursor.moveToFirst()) return UpdateDownloadState.Idle

            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            return when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    val localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                    if (downloadedApk(localUri) != null) UpdateDownloadState.ReadyToInstall
                    else UpdateDownloadState.Idle
                }
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

    fun onDownloadCompleted(context: Context, receivedDownloadId: Long, releaseTag: String): Boolean =
        receivedDownloadId == trackedDownloadId(context, releaseTag) &&
            queryDownloadState(context, releaseTag) == UpdateDownloadState.ReadyToInstall

    fun promptInstall(context: Context, releaseTag: String? = null) {
        val apk = findDownloadedApk(context, releaseTag) ?: return
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, APK_MIME_TYPE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )
    }

    suspend fun installUpdate(context: Context, releaseTag: String) = withContext(Dispatchers.IO) {
        val apk = findDownloadedApk(context, releaseTag) ?: return@withContext
        if (!canInstallPackages(context)) {
            withContext(Dispatchers.Main) { openInstallPermissionSettings(context) }
            return@withContext
        }

        var session: PackageInstaller.Session? = null
        try {
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            params.setAppPackageName(context.packageName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
            }

            val installer = context.packageManager.packageInstaller
            session = installer.openSession(installer.createSession(params))
            session.openWrite(apk.name, 0, apk.length()).use { output ->
                apk.inputStream().use { input -> input.copyTo(output) }
                session.fsync(output)
            }

            val callback =
                Intent(context, UpdateInstallStatusReceiver::class.java)
                    .setAction("${context.packageName}.UPDATE_INSTALL_STATUS")
            val flags =
                PendingIntent.FLAG_UPDATE_CURRENT or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val statusIntent = PendingIntent.getBroadcast(context, 17041, callback, flags)
            session.commit(statusIntent.intentSender)
        } catch (_: Exception) {
            runCatching { session?.abandon() }
            withContext(Dispatchers.Main) { promptInstall(context, releaseTag) }
        }
    }

    suspend fun discardOtherDownload(context: Context, releaseTag: String?) = withContext(Dispatchers.IO) {
        downloadMutex.withLock {
            val prefs = context.dataStore.data.first()
            if (releaseTag != null && prefs[UpdateDownloadTagKey] == releaseTag) return@withLock
            prefs[UpdateDownloadIdKey]?.let { context.getSystemService(DownloadManager::class.java).remove(it) }
            context.dataStore.edit {
                it.remove(UpdateDownloadIdKey)
                it.remove(UpdateDownloadTagKey)
                it.remove(UpdateDownloadAllowsRoamingKey)
            }
            getUpdateDirectory(context).listFiles()?.filter { it.extension == "apk" }?.forEach { it.delete() }
        }
    }

    private fun trackedDownloadId(context: Context, releaseTag: String?): Long? {
        val prefs = context.dataStore.snapshotOrDefault() ?: return null
        val tag = prefs[UpdateDownloadTagKey] ?: return null
        if (releaseTag != null && tag != releaseTag) return null
        if (!Updater.isUpdateAvailable(BuildConfig.VERSION_NAME, tag) &&
            !Updater.FORCE_UPDATE_ANNOUNCEMENT_FOR_TESTING
        ) return null
        return prefs[UpdateDownloadIdKey]
    }

    private fun findDownloadedApk(context: Context, releaseTag: String?): File? {
        val id = trackedDownloadId(context, releaseTag) ?: return null
        val manager = context.getSystemService(DownloadManager::class.java)
        manager.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
            if (!cursor.moveToFirst()) return null
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            if (status != DownloadManager.STATUS_SUCCESSFUL) return null
            return downloadedApk(cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)))
        }
    }

    private fun downloadedApk(localUri: String?): File? =
        localUri?.toUri()?.takeIf { it.scheme == "file" }?.path?.let(::File)
            ?.takeIf { it.isFile && it.length() > 0 }

    private fun mapFailureReason(context: Context, reason: Int): String? =
        when (reason) {
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> context.getString(R.string.update_download_error_no_space)
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE,
            DownloadManager.ERROR_HTTP_DATA_ERROR,
            -> context.getString(R.string.update_download_error_connection)
            else -> null
        }
}
