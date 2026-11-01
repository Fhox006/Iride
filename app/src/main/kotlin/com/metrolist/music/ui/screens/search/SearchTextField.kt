package com.metrolist.music.ui.screens.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import com.metrolist.music.ui.utils.rememberReducedMotion
import com.metrolist.music.ui.component.IrideTypewriterCursorBlinkMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Native editing and scrolling, with the same half-line-height block cursor as artist titles. */
@Composable
internal fun SearchTextField(
    state: TextFieldState,
    editing: Boolean,
    confirmationCursorVisible: Boolean,
    textStyle: TextStyle,
    placeholder: String,
    placeholderStyle: TextStyle,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    val reducedMotion = rememberReducedMotion()
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var cursorVisible by remember { mutableStateOf(false) }
    LaunchedEffect(editing, state.text.toString(), state.selection, reducedMotion) {
        cursorVisible = editing
        if (reducedMotion) return@LaunchedEffect
        if (editing) {
            while (isActive) {
                delay(IrideTypewriterCursorBlinkMillis)
                cursorVisible = !cursorVisible
            }
        }
    }
    BasicTextField(
        state = state,
        readOnly = !editing,
        textStyle = textStyle,
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        onKeyboardAction = { onSubmit(state.text.toString()) },
        scrollState = scroll,
        cursorBrush = SolidColor(Color.Transparent),
        onTextLayout = { getResult -> layout = getResult() },
        modifier = modifier.drawWithContent {
            drawContent()
            val result = layout
            if ((if (editing) cursorVisible else confirmationCursorVisible) && result != null) {
                val index = (if (editing) state.selection.end else state.text.length)
                    .coerceIn(0, result.layoutInput.text.length)
                val caret = result.getCursorRect(index)
                val width = caret.height * 0.5f
                // The block may extend into the trailing gutter; shifting it left covers the last glyph.
                val left = searchCursorLeft(caret.left, scroll.value, 1.dp.toPx())
                drawRect(textStyle.color, Offset(left, caret.top), Size(width, caret.height))
            }
        },
        decorator = { innerField ->
            Box {
                if (state.text.isEmpty()) Text(placeholder, style = placeholderStyle, maxLines = 1)
                innerField()
            }
        },
    )
}
