package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.metrolist.music.ui.theme.IBMPlexMonoFontFamily

@Composable
fun IrideCollectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = TextStyle(
            fontFamily = IBMPlexMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = IrideFolio.CollectionTitleFontSize,
            letterSpacing = IrideFolio.CollectionTitleLetterSpacing,
        ),
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = IrideFolio.CollectionTitleMaxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}
