/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Text
import com.metrolist.music.ui.utils.rememberReducedMotion
import kotlinx.coroutines.delay

private const val MS_PER_CHAR = 26L
private const val MAX_TOTAL_MS = 700L
private const val CURSOR_BLINK_MS = 280L
private const val CURSOR_BLINKS_AT_END = 2

/**
 * Types [text] out one character at a time, with a block cursor riding the last character.
 *
 * The hero moment of a screen, so at most one per screen — everything else in the Iride motion
 * vocabulary reveals with [com.metrolist.music.ui.utils.revealMask] instead. Space Mono makes this
 * work: a monospace face means no character is wider than another, so nothing shuffles as the line
 * grows.
 *
 * **No reflow, ever.** The full string is always laid out; the untyped tail is simply drawn
 * transparent. So the block occupies its final size from the first frame — critical here, because
 * the artist header is bottom-anchored and a line-count change mid-animation would shove the whole
 * page. It also means the cursor sits at exactly the right glyph boundary.
 *
 * The cursor blinks [CURSOR_BLINKS_AT_END] times once typing finishes and then disappears for good:
 * a cursor left blinking is a permanent loop on a loaded screen.
 *
 * @param resetKey retypes when it changes. Defaults to [text]; pass a stable id (an artist id) to
 *   keep the animation from replaying when the same screen recomposes with equal text.
 * @param animate false draws the finished line immediately. Deliberately **not** a key of the
 *   typing effect: a caller flipping it to false the moment the line is done (because the screen has
 *   already landed and must not retype when this composable is disposed and brought back) must not
 *   cut the animation short while it is still running.
 */
@Composable
fun TypewriterText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    resetKey: Any? = text,
    animate: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    textAlign: TextAlign = TextAlign.Start,
) {
    val reducedMotion = rememberReducedMotion()
    var typed by remember { mutableIntStateOf(if (animate) 0 else text.length) }
    var cursorVisible by remember { mutableStateOf(false) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }

    LaunchedEffect(resetKey, reducedMotion, text) {
        if (reducedMotion || !animate || text.isEmpty()) {
            typed = text.length
            cursorVisible = false
            return@LaunchedEffect
        }
        typed = 0
        cursorVisible = true
        val step = minOf(MS_PER_CHAR, MAX_TOTAL_MS / text.length)
        repeat(text.length) {
            delay(step)
            typed++
        }
        repeat(CURSOR_BLINKS_AT_END) {
            delay(CURSOR_BLINK_MS)
            cursorVisible = false
            delay(CURSOR_BLINK_MS)
            cursorVisible = true
        }
        delay(CURSOR_BLINK_MS)
        cursorVisible = false
    }

    val annotated = remember(text, typed, color) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = color)) { append(text.take(typed)) }
            withStyle(SpanStyle(color = Color.Transparent)) { append(text.drop(typed)) }
        }
    }

    Box(modifier = modifier) {
        Text(
            text = annotated,
            style = style,
            maxLines = maxLines,
            overflow = overflow,
            textAlign = textAlign,
            onTextLayout = { layout = it },
            modifier = Modifier.fillMaxWidth().drawWithContent {
                drawContent()
                val result = layout
                if (!cursorVisible || result == null) return@drawWithContent
                val caret = runCatching { result.getCursorRect(typed) }.getOrNull()
                    ?: return@drawWithContent
                drawRect(
                    color = color,
                    topLeft = caret.topLeft,
                    size = Size(caret.height * 0.5f, caret.height),
                )
            },
        )
    }
}
