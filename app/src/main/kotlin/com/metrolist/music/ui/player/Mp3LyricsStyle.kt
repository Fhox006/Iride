package com.metrolist.music.ui.player

import androidx.datastore.preferences.core.stringPreferencesKey

internal enum class Mp3LyricsStyle { GRADIENT, TV }

internal val Mp3LyricsStyleKey = stringPreferencesKey("mp3LyricsStyle")
