package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.ui.theme.FolioShape
import com.metrolist.music.ui.theme.fillSubtle
import com.metrolist.music.ui.theme.strokeCard
import com.metrolist.music.ui.theme.textPrimary
import androidx.compose.ui.Modifier
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

@Composable
fun EditableCollectionTitle(title: String, onSave: (String) -> Unit, modifier: Modifier = Modifier) {
    var editing by remember(title) { mutableStateOf(false) }
    var value by remember(title) { mutableStateOf(TextFieldValue(title)) }
    val focusManager = LocalFocusManager.current
    val shape = FolioShape(5.dp)
    val style = TextStyle(
        fontFamily = IBMPlexMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (IrideFolio.CollectionTitleFontSize.value * 0.78f).sp,
        letterSpacing = IrideFolio.CollectionTitleLetterSpacing,
    )
    fun finish() {
        val result = value.text.trim()
        editing = false
        focusManager.clearFocus()
        if (result.isNotEmpty() && result != title) onSave(result)
    }
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (editing) {
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                textStyle = style.copy(color = MaterialTheme.colorScheme.textPrimary),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.textPrimary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { finish() }),
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.fillSubtle)
                    .border(1.dp, MaterialTheme.colorScheme.strokeCard, shape)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        } else {
            Text(
                text = title,
                style = TextStyle(fontFamily = IBMPlexMonoFontFamily, fontWeight = FontWeight.Bold,
                    fontSize = IrideFolio.CollectionTitleFontSize,
                    letterSpacing = IrideFolio.CollectionTitleLetterSpacing),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = IrideFolio.CollectionTitleMaxLines,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable {
                    value = TextFieldValue(title, selection = androidx.compose.ui.text.TextRange(title.length))
                    editing = true
                },
            )
        }
    }
}
