/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.metrolist.music.constants.TopNavigationBarKey
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.utils.rememberPreference

@Composable
fun <T> EnumDialog(
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
    title: String,
    current: T,
    values: List<T>,
    valueText: @Composable (T) -> String,
    valueDescription: (@Composable (T) -> String)? = null,
) {
    val (topNavigationBarEnabled) = rememberPreference(TopNavigationBarKey, defaultValue = false)

    ListDialog(
        onDismiss = onDismiss,
    ) {
        items(values) { value ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSelect(value)
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                RadioButton(
                    selected = value == current,
                    onClick = null,
                    colors = if (topNavigationBarEnabled) {
                        RadioButtonDefaults.colors(
                            selectedColor = Color.White,
                            unselectedColor = Color.White.copy(alpha = 0.6f),
                        )
                    } else {
                        RadioButtonDefaults.colors()
                    },
                )

                Column(
                    modifier = Modifier.padding(start = 16.dp),
                ) {
                    Text(
                        text = valueText(value),
                        style = if (topNavigationBarEnabled) {
                            MaterialTheme.typography.bodyLarge.copy(fontFamily = SpaceMonoFontFamily)
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                        color = if (topNavigationBarEnabled) Color.White else MaterialTheme.colorScheme.onSurface,
                    )
                    if (valueDescription != null) {
                        Text(
                            text = valueDescription(value),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (topNavigationBarEnabled) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
