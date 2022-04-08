package com.foxit.kotlin.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

@Composable
fun EditableTextField(
    text: String = "",
    editable: Boolean = false,
    fontStyle: FontStyle? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    onSubmit: (MutableState<String>) -> Boolean,
) {
    var edit by remember { mutableStateOf(editable) }
    if (edit) {
        SubmittableTextField(text, singleLine = false, fontSize = fontSize, onSubmit = { value ->
            if (onSubmit(value)) edit = false
        })
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.clickable { edit = true }) {
            Icon(Icons.Default.Edit, "edit", Modifier.size(15.dp))
            Text(text, fontStyle = fontStyle, fontSize = fontSize, modifier = Modifier.fillMaxWidth())
        }
    }
}
