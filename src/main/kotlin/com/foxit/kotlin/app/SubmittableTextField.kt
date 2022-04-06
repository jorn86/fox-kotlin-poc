package com.foxit.kotlin.app

import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SubmittableTextField(
    initialValue: String = "",
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    placeholder: @Composable () -> Unit = {},
    onSubmit: (String, (String) -> Unit) -> Unit,
) {
    val (value, setter) = remember { mutableStateOf(initialValue) }
    TextField(value, setter, modifier.onKeyEvent {
        if (it.key in listOf(Key.Enter, Key.NumPadEnter)) {
            onSubmit(value, setter)
        }
        false
    }, singleLine = singleLine, placeholder = placeholder)
}
