/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.wrapped

import android.content.res.Resources
import androidx.annotation.StringRes
import com.metrolist.music.R

data class MessagePair(val range: LongRange, val tease: String, val reveal: String)

object WrappedRepository {
    private data class ListeningMessage(
        val range: LongRange,
        @StringRes val tease: Int,
        @StringRes val reveal: Int,
    )

    private val messages = listOf(
        ListeningMessage(0L..999L, R.string.wrapped_listening_01_tease, R.string.wrapped_listening_01_reveal),
        ListeningMessage(0L..999L, R.string.wrapped_listening_02_tease, R.string.wrapped_listening_02_reveal),
        ListeningMessage(0L..999L, R.string.wrapped_listening_03_tease, R.string.wrapped_listening_03_reveal),
        ListeningMessage(0L..999L, R.string.wrapped_listening_04_tease, R.string.wrapped_listening_04_reveal),

        ListeningMessage(1000L..4999L, R.string.wrapped_listening_05_tease, R.string.wrapped_listening_05_reveal),
        ListeningMessage(1000L..4999L, R.string.wrapped_listening_06_tease, R.string.wrapped_listening_06_reveal),
        ListeningMessage(1000L..4999L, R.string.wrapped_listening_07_tease, R.string.wrapped_listening_07_reveal),
        ListeningMessage(1000L..4999L, R.string.wrapped_listening_08_tease, R.string.wrapped_listening_08_reveal),

        ListeningMessage(5000L..14999L, R.string.wrapped_listening_09_tease, R.string.wrapped_listening_09_reveal),
        ListeningMessage(5000L..14999L, R.string.wrapped_listening_10_tease, R.string.wrapped_listening_10_reveal),
        ListeningMessage(5000L..14999L, R.string.wrapped_listening_11_tease, R.string.wrapped_listening_11_reveal),
        ListeningMessage(5000L..14999L, R.string.wrapped_listening_12_tease, R.string.wrapped_listening_12_reveal),

        ListeningMessage(15000L..39999L, R.string.wrapped_listening_13_tease, R.string.wrapped_listening_13_reveal),
        ListeningMessage(15000L..39999L, R.string.wrapped_listening_14_tease, R.string.wrapped_listening_14_reveal),
        ListeningMessage(15000L..39999L, R.string.wrapped_listening_15_tease, R.string.wrapped_listening_15_reveal),
        ListeningMessage(15000L..39999L, R.string.wrapped_listening_16_tease, R.string.wrapped_listening_16_reveal),

        ListeningMessage(40000L..Long.MAX_VALUE, R.string.wrapped_listening_17_tease, R.string.wrapped_listening_17_reveal),
        ListeningMessage(40000L..Long.MAX_VALUE, R.string.wrapped_listening_18_tease, R.string.wrapped_listening_18_reveal),
        ListeningMessage(40000L..Long.MAX_VALUE, R.string.wrapped_listening_19_tease, R.string.wrapped_listening_19_reveal),
        ListeningMessage(40000L..Long.MAX_VALUE, R.string.wrapped_listening_20_tease, R.string.wrapped_listening_20_reveal)
    )

    fun getMessage(minutes: Long, resources: Resources): MessagePair {
        val possibleMessages = messages.filter { minutes in it.range }
        val chosenMessage = if (possibleMessages.isNotEmpty()) {
            possibleMessages.random()
        } else {
            ListeningMessage(0L..Long.MAX_VALUE, R.string.wrapped_listening_21_tease, R.string.wrapped_listening_21_reveal)
        }
        return MessagePair(
            range = chosenMessage.range,
            tease = resources.getString(chosenMessage.tease),
            reveal = resources.getString(chosenMessage.reveal, minutes),
        )
    }
}
