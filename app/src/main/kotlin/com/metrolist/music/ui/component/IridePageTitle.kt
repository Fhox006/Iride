package com.metrolist.music.ui.component

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.theme.textPrimary

@Composable
fun IridePageTitle(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
    monospace: Boolean = false,
) {
    Text(
        text = stringResource(titleRes).lowercase().replaceFirstChar { it.titlecase() },
        modifier = modifier,
        style = MaterialTheme.typography.headlineLarge.copy(
            fontSize = 34.sp,
            lineHeight = 41.sp,
            fontWeight = FontWeight.Bold,
        ).let { if (monospace) it.copy(fontFamily = SpaceMonoFontFamily) else it },
        color = MaterialTheme.colorScheme.textPrimary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
