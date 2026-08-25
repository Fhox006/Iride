/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.metrolist.music.BuildConfig
import com.metrolist.music.ui.screens.CrashActivity
import timber.log.Timber
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

class CrashHandler private constructor(
    private val applicationContext: Context
) : Thread.UncaughtExceptionHandler {

    private val defaultHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val crashLog = buildCrashLog(throwable)
            Timber.e(throwable, "App crashed")
            saveCrashReportToFile(crashLog)
            val intent = Intent(applicationContext, CrashActivity::class.java).apply {
                putExtra(EXTRA_CRASH_LOG, crashLog)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            applicationContext.startActivity(intent)
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(1)
        } catch (e: Exception) {
            Timber.e(e, "Error handling crash")
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun saveCrashReportToFile(report: String) {
        try {
            val fileName = "iride-crash-${System.currentTimeMillis()}.txt"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                }
                val uri =
                    applicationContext.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        ?: return
                applicationContext.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(report.toByteArray())
                }
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                File(dir, fileName).writeText(report)
            }
        } catch (_: Exception) {
        }
    }

    private fun buildCrashLog(throwable: Throwable): String {
        val stackTrace = StringWriter().apply {
            throwable.printStackTrace(PrintWriter(this))
        }.toString()

        return buildString {
            appendLine("Metrolist Crash Report")
            appendLine("=".repeat(50))
            appendLine()
            appendLine("Manufacturer: ${Build.MANUFACTURER}")
            appendLine("Device: ${Build.MODEL}")
            appendLine("Android version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine()
            appendLine("=".repeat(50))
            appendLine("Events before crash:")
            appendLine("=".repeat(50))
            appendLine()
            appendLine(DiagnosticLog.snapshot())
            appendLine()
            appendLine("=".repeat(50))
            appendLine("Stacktrace:")
            appendLine("=".repeat(50))
            appendLine()
            append(stackTrace)
        }
    }

    companion object {
        const val EXTRA_CRASH_LOG = "crash_log"

        fun install(context: Context) {
            val handler = CrashHandler(context.applicationContext)
            Thread.setDefaultUncaughtExceptionHandler(handler)
            Timber.d("CrashHandler installed")
        }
    }
}
