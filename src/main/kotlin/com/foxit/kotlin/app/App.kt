package com.foxit.kotlin.app

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.foxit.kotlin.db.DatabaseService
import com.foxit.kotlin.orm.TaskMapper

class App(private val db: DatabaseService) {
    @Composable
    @Preview
    fun app() {
        var text by remember { mutableStateOf("Hello, World!") }

        MaterialTheme {
            Column {
                Button(onClick = {
                    text = "Hello, Desktop!"
                }) {
                    Text(text)
                }

                val tasks = db.connection { TaskMapper.selectAll(this) }
                tasks.forEach { Text(it.name) }
            }
        }
    }

    fun start() = application {
        Window(onCloseRequest = ::exitApplication) {
            app()
        }
    }
}
