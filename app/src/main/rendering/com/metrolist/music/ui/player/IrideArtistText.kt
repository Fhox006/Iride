package com.metrolist.music.ui.player

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.ui.component.irideFolioCombinedClickable
import com.metrolist.music.ui.theme.InterFontFamily

@Composable
internal fun IrideArtistText(
    mediaMetadata: MediaMetadata,
    color: Color,
    fontSize: TextUnit,
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily = InterFontFamily,
    lineHeight: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight = FontWeight.Normal,
) {
    val annotated =
        remember(mediaMetadata.artists) {
            buildAnnotatedString {
                mediaMetadata.artists.forEachIndexed { index, artist ->
                    pushStringAnnotation(tag = "artist", annotation = artist.id.orEmpty())
                    append(artist.name)
                    pop()
                    if (index != mediaMetadata.artists.lastIndex) append(", ")
                }
            }
        }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var tapPosition by remember { mutableStateOf<Offset?>(null) }
    val interactionSource = remember { MutableInteractionSource() }
    // Underline while held so the press reads as "release to open artist screen".
    val isPressed by interactionSource.collectIsPressedAsState()

    Text(
        text = annotated,
        color = color,
        fontFamily = fontFamily,
        fontSize = fontSize,
        lineHeight = lineHeight,
        fontWeight = fontWeight,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textDecoration = if (isPressed) TextDecoration.Underline else null,
        onTextLayout = { layoutResult = it },
        modifier =
            modifier
                .pointerInput(annotated) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            event.changes.firstOrNull()?.position?.let { tapPosition = it }
                        }
                    }
                }
                .irideFolioCombinedClickable(
                    indication = null,
                    interactionSource = interactionSource,
                    onClick = {
                        val layout = layoutResult ?: return@irideFolioCombinedClickable
                        val pos = tapPosition ?: return@irideFolioCombinedClickable
                        val charOffset = layout.getOffsetForPosition(pos)
                        annotated
                            .getStringAnnotations("artist", charOffset, charOffset)
                            .firstOrNull()
                            ?.let { ann -> if (ann.item.isNotBlank()) onArtistClick(ann.item) }
                    },
                ),
    )
}
