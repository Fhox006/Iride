package com.metrolist.music.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import com.metrolist.music.R
import com.metrolist.music.constants.CrossfadeEnabledKey
import com.metrolist.music.constants.CrossfadeGaplessKey
import com.metrolist.music.constants.SkipFadeKey
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.launch

@Composable
internal fun folioDissolveAction(): Material3MenuItemData {
    val context = LocalContext.current
    val scope = rememberMenuActionScope()
    val crossfade by rememberPreference(CrossfadeEnabledKey, false)
    val skipFade by rememberPreference(SkipFadeKey, false)
    val enabled = crossfade || skipFade
    return folioMenuAction(
        R.string.folio_dissolve,
        R.drawable.linear_scale,
        {
            scope.launch {
                context.dataStore.edit { preferences ->
                    preferences[CrossfadeEnabledKey] = !enabled
                    preferences[SkipFadeKey] = !enabled
                    if (!enabled) preferences[CrossfadeGaplessKey] = false
                }
            }
        },
    ).copy(selected = enabled)
}
