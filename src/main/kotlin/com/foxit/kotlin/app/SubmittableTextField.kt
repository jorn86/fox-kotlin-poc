package com.foxit.kotlin.app

import androidx.compose.material.LocalTextStyle
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.TextUnit

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SubmittableTextField(
    initialValue: String = "",
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    fontSize: TextUnit? = null,
    placeholder: @Composable () -> Unit = {},
    onSubmit: (MutableState<String>) -> Unit,
) {
    val value = remember { mutableStateOf(initialValue) }
    val style = if (fontSize != null) LocalTextStyle.current.copy(fontSize = fontSize) else LocalTextStyle.current
    TextField(value.value, value.component2(), modifier.onKeyEvent {
        if (it.key in listOf(Key.Enter, Key.NumPadEnter) && it.type == KeyEventType.KeyDown) {
            onSubmit(value)
            return@onKeyEvent true
        }
        false
    }, textStyle = style, singleLine = singleLine, placeholder = placeholder)
}
