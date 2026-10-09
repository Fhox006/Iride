/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import com.metrolist.music.ui.theme.SpaceMonoFontFamily
import com.metrolist.music.ui.theme.fillSubtle
import com.metrolist.music.ui.theme.strokeCard
import com.metrolist.music.ui.theme.textPrimary
import com.metrolist.music.ui.theme.textSecondary
import com.metrolist.music.ui.utils.IrideMotion
import com.metrolist.music.ui.utils.rememberReducedMotion

val IrideSearchBoxHeight = 52.dp

/** Flat search field shared by search, library, and playlist screens. */
@Composable
fun IrideSearchBox(
    query: TextFieldValue,
    onQueryChange: (TextFieldValue) -> Unit,
    placeholderText: String,
    focusRequester: FocusRequester,
    onFocusChanged: (FocusState) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    showBorder: Boolean = true,
    animateQueryCorners: Boolean = false,
    cornerRadius: Dp = 18.dp,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val reducedMotion = rememberReducedMotion()
    val animatedCornerRadius by animateDpAsState(
        targetValue = if (animateQueryCorners && query.text.isNotBlank()) 5.dp else cornerRadius,
        animationSpec = tween(
            durationMillis = if (reducedMotion) 0 else IrideMotion.Medium,
            easing = IrideMotion.EaseOutQuart,
        ),
        label = "searchBoxCornerRadius",
    )
    val shape = RoundedCornerShape(animatedCornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IrideSearchBoxHeight)
            .clip(shape)
            .background(MaterialTheme.colorScheme.fillSubtle)
            .then(
                if (showBorder) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.strokeCard, shape)
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                focusRequester.requestFocus()
                keyboardController?.show()
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged(onFocusChanged),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.textPrimary.copy(alpha = 0.95f),
                    fontFamily = SpaceMonoFontFamily,
                    fontSize = 15.sp,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.textPrimary),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (query.text.isEmpty()) {
                        Text(
                            text = placeholderText,
                            style = TextStyle(
                                color = MaterialTheme.colorScheme.textSecondary,
                                fontFamily = SpaceMonoFontFamily,
                                fontSize = 15.sp,
                            ),
                        )
                    }
                    innerTextField()
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            )

            if (query.text.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.textSecondary,
                    )
                }
            }
        }
    }
}
