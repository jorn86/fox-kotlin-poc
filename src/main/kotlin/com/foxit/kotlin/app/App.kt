package com.foxit.kotlin.app

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.foxit.kotlin.dao.ColumnDao
import com.foxit.kotlin.dao.TaskDao
import com.foxit.kotlin.db.DatabaseService
import com.foxit.kotlin.dto.Column
import com.foxit.kotlin.dto.Task
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class App(private val db: DatabaseService) {
    private val log = LoggerFactory.getLogger(javaClass)

    private lateinit var columns: SnapshotStateList<Column>
    private lateinit var tasks: SnapshotStateList<Task>

    fun start() = application {
        Window(
            title = "Fox Kanban board",
            onCloseRequest = ::exitApplication,
            state = rememberWindowState(width = 1600.dp, height = 900.dp),
        ) {
            App()
        }
    }

    @Composable
    @Preview
    fun App() {
        columns = remember { mutableStateListOf() }
        tasks = remember { mutableStateListOf() }

        // Where to put this code so this check isn't needed? We want to init once, not every re-compose
        if (columns.isEmpty()) {
            db.connection {
                columns.addAll(ColumnDao.selectAll(this))
                tasks.addAll(TaskDao.selectAll(this))
            }
        }

        var popupVisible by remember { mutableStateOf(false) }
        Window(
            title = "Popup",
            visible = popupVisible,
            onCloseRequest = { popupVisible = false },
            state = rememberWindowState(position = WindowPosition(Alignment.Center), width = 250.dp, height = 150.dp),
        ) {
            Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                Text("Hallo Joost", modifier = Modifier.align(Alignment.CenterHorizontally))
                Button(onClick = { popupVisible = false }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("OK")
                }
            }
        }

        // Doesn't show scrollbars or allow mousewheel/drag :( but at least it scrolls to the focused field
        Row(Modifier.padding(20.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Button({
                popupVisible = true
            }) {
                Image(Icons.Default.Warning, "")
            }

            columns.sortedBy { it.index }.forEach {
                TaskColumn(it, tasks.filter { task -> task.columnId == it.id })
            }

            SubmittableTextField(modifier = Modifier.width(300.dp),
                placeholder = { Text("Add column...", fontStyle = FontStyle.Italic) })
                { name, setter -> handleNewColumn(name, setter) }
        }
    }

    private fun handleNewColumn(name: String, setter: (String) -> Unit): Boolean {
        if (name.trim().isBlank()) return false
        db.connection {
            val nextIndex = (columns.maxOfOrNull { it.index } ?: 0) + 1
            val id = ColumnDao.insert(this, Column(nextIndex, name.trim()))
            columns.add(ColumnDao.selectSingle(this, id))
        }
        setter("")
        return true
    }

    @Composable
    private fun TaskColumn(column: Column, tasks: Collection<Task>) {
        Column(modifier = Modifier.width(300.dp)
            .fillMaxHeight()
            .shadow(2.dp, shape = RoundedCornerShape(5.dp))
            .verticalScroll(rememberScrollState())
            .padding(10.dp)) {
            Text(column.name, fontSize = 24.sp)
            tasks.sortedBy { it.index }.forEach { Task(it) }

            SubmittableTextField(modifier = Modifier.padding(top = 20.dp).fillMaxWidth(),
                placeholder = { Text("Add task...", fontStyle = FontStyle.Italic) })
                { name, setter -> handleNewTask(column.id, name, setter) }
        }
    }

    private fun handleNewTask(columnId: Int, name: String, nameSetter: (String) -> Unit): Boolean {
        if (name.trim().isBlank()) return false
        db.connection {
            val nextIndex = TaskDao.getMaxIndex(this, columnId) + 1
            val id = TaskDao.insert(this, Task(columnId, nextIndex, name))
            tasks.add(TaskDao.selectSingle(this, id))
        }
        nameSetter("")
        return true
    }

    @Composable
    private fun Task(task: Task) {
        Column(modifier = Modifier
            .width(300.dp)
            .padding(top = 5.dp)
            .shadow(3.dp, shape = RoundedCornerShape(10.dp))
            .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            var editDescription by remember { mutableStateOf(false) }
            Text(task.name, fontSize = 20.sp)
            if (editDescription) {
                SubmittableTextField(task.description ?: "", singleLine = false) { value, _ ->
                    val updated = task.update(description = value)
                    db.connection {
                        TaskDao.update(this, updated)
                    }
                    tasks.replaceAll { if (it.id != task.id) it else updated }
                    editDescription = false
                }
            } else {
                Text(task.description ?: "<blank>", fontStyle = FontStyle.Italic,
                    modifier = Modifier.clickable { editDescription = true })
            }
            DateField("Created: ", task.created)
            DateField("Last modified: ", task.modified)
        }
    }

    @Composable
    private fun ColumnScope.DateField(label: String, instant: Instant, zone: ZoneId = ZoneId.of("Europe/Amsterdam")) {
        Text(label + instant.atZone(zone).format(DATETIME_FORMAT),
            fontSize = 10.sp,
            modifier = Modifier.align(Alignment.End))
    }

    companion object {
        private val DATETIME_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy H:mm")
    }
}
