/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.util.Log
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

object DiagnosticLog {
    private const val MAX_ENTRIES = 600

    private val entries = ArrayDeque<String>(MAX_ENTRIES)
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    val tree: Timber.Tree = object : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (priority < Log.INFO) return
            val label = when (priority) {
                Log.INFO -> "I"
                Log.WARN -> "W"
                Log.ERROR -> "E"
                Log.ASSERT -> "A"
                else -> "?"
            }
            val cause = t?.let { " | ${it.javaClass.simpleName}: ${it.message}" } ?: ""
            record("${timeFormat.format(Date())} $label/${tag ?: "App"}: $message$cause")
        }
    }

    @Synchronized
    fun record(line: String) {
        if (entries.size >= MAX_ENTRIES) entries.removeFirst()
        entries.addLast(line)
    }

    @Synchronized
    fun snapshot(): String = entries.joinToString(separator = "\n")

    @Synchronized
    fun clear() = entries.clear()
}
