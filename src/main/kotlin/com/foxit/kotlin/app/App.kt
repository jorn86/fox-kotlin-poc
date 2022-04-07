package com.foxit.kotlin.app

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import com.foxit.kotlin.dao.ColumnDao
import com.foxit.kotlin.dao.TaskDao
import com.foxit.kotlin.db.DatabaseService
import com.foxit.kotlin.dto.Column
import com.foxit.kotlin.dto.Task
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.io.path.bufferedWriter

class App(private val db: DatabaseService) {
    private val log = LoggerFactory.getLogger(javaClass)

    private lateinit var columns: SnapshotStateList<Column>
    private lateinit var tasks: SnapshotStateList<Task>

    private lateinit var applicationScope: ApplicationScope
    private lateinit var windowScope: FrameWindowScope

    fun start() = application {
        applicationScope = this
        Window(
            title = "Fox Kanban board",
            onCloseRequest = ::exitApplication,
            state = rememberWindowState(width = 1600.dp, height = 900.dp),
        ) {
            windowScope = this
            App()
        }
    }

    @Composable
    @Preview
    private fun App() {
        columns = remember { mutableStateListOf() }
        tasks = remember { mutableStateListOf() }

        // Where to put this code so this check isn't needed? We want to init once, not every re-compose
        if (columns.isEmpty()) {
            db.connection {
                columns.addAll(ColumnDao.selectAll(this))
                tasks.addAll(TaskDao.selectAll(this))
            }
        }

        val popupVisible = remember { mutableStateOf(false) }
        Window(
            title = "Popup",
            visible = popupVisible.value,
            onCloseRequest = { popupVisible(false) },
            state = rememberWindowState(position = WindowPosition(Alignment.Center), width = 250.dp, height = 150.dp),
        ) {
            Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                Text("Hallo Joost", modifier = Modifier.align(Alignment.CenterHorizontally))
                Button(onClick = { popupVisible(false) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("OK")
                }
            }
        }

        // Doesn't show scrollbars or allow mousewheel/drag :( but at least it scrolls to the focused field
        Row(Modifier.padding(20.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            MenuBar(popupVisible)

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
    private fun MenuBar(popupVisible: MutableState<Boolean>) {
        var shutdownEnabled by remember { mutableStateOf(false) }
        Column {
            Button({
                val dialog = JFileChooser()
                dialog.fileSelectionMode = JFileChooser.FILES_ONLY
                dialog.addChoosableFileFilter(FileNameExtensionFilter("Plain text (*.txt)", "txt"))
                dialog.isAcceptAllFileFilterUsed = false
                dialog.currentDirectory = Paths.get(".").toFile()
                dialog.selectedFile = File(dialog.currentDirectory, "export.txt")
                val result = dialog.showSaveDialog(windowScope.window)
                if (result == JFileChooser.APPROVE_OPTION) {
                    exportTo(dialog.selectedFile.toPath())
                    log.info("Data exported to ${dialog.selectedFile.absolutePath}")
                }
            }) {
                Image(Icons.Default.ArrowForward, "Export", colorFilter = ColorFilter.tint(MaterialTheme.colors.onPrimary))
            }
            Button({
                popupVisible(true)
                shutdownEnabled = true
            }) {
                Image(Icons.Default.Warning, "Joost", colorFilter = ColorFilter.tint(MaterialTheme.colors.onPrimary))
            }

            Button({
                applicationScope.exitApplication()
            }) {
                Image(Icons.Default.Close, "Exit", colorFilter = ColorFilter.tint(MaterialTheme.colors.onPrimary))
            }
            Button({
                val os = System.getProperty("os.name")
                val command = when {
                    os.contains("Windows") -> "shutdown.exe -s -t 0"
                    os.contains("Linux") ||
                            os.contains("Mac") -> "shutdown -h now"
                    else -> throw IllegalArgumentException("Unsupported OS $os")
                }
                log.info("Shutting down: $command")
                Runtime.getRuntime().exec(command)
                applicationScope.exitApplication()
            }, enabled = shutdownEnabled) {
                Column {
                    Image(Icons.Default.ArrowDropDown, "Exit", colorFilter = ColorFilter.tint(MaterialTheme.colors.onPrimary))
                }
            }
        }
    }

    private fun exportTo(path: Path) {
        path.bufferedWriter().use { writer ->
            columns.forEach { column ->
                writer.append(column.name).append(":\n")
                tasks.filter { task -> task.columnId == column.id }.forEach {
                    writer.append("- ").append(it.name).append("\n")
                }
            }
        }
    }

    @Composable
    private fun TaskColumn(column: Column, tasks: Collection<Task>) {
        Column(modifier = Modifier.width(300.dp)
            .fillMaxHeight()
            .shadow(2.dp, shape = RoundedCornerShape(5.dp))
            .verticalScroll(rememberScrollState())
            .padding(10.dp)) {
            Text(column.name, fontSize = 24.sp)
            tasks.sortedBy { it.index }.forEach {
                Task(it)
            }

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

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun Task(task: Task) {
        val source = remember { MutableInteractionSource() }
        val hovered by source.collectIsHoveredAsState()
        val menuVisible = remember { mutableStateOf(false) }
        Column(modifier = Modifier
            .width(300.dp)
            .hoverable(source)
            .mouseClickable { if (buttons.isSecondaryPressed) menuVisible(true) }
            .padding(top = 5.dp)
            .shadow(3.dp, shape = RoundedCornerShape(if(hovered) 7.dp else 5.dp))
            .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            EditableTextField(task.name, fontSize = 20.sp){ value, _ ->
                updateTask(task) { task.update(name = value) }
                true
            }
            EditableTextField(task.description ?: "", fontStyle = FontStyle.Italic){ value, _ ->
                updateTask(task) { task.update(description = value) }
                true
            }
            DateField("Created: ", task.created)
            DateField("Last modified: ", task.modified)

            Row(modifier = Modifier.fillMaxWidth()) {
                TaskContextMenu(task, menuVisible)
            }
        }
    }

    @Composable
    private fun TaskContextMenu(task: Task, visible: MutableState<Boolean>) {
        DropdownMenu(visible.value, modifier = Modifier.width(250.dp), onDismissRequest = { visible(false) }) {
            columns.filter { it.id != task.columnId }.forEach { column ->
                DropdownMenuItem({
                    val newIndex = db.connection { TaskDao.getMaxIndex(this, column.id) } + 1
                    updateTask(task) { it.update(columnId = column.id, index = newIndex) }
                    tasks.filter { it.columnId == task.columnId && it.index > task.index }.forEach { otherTask ->
                        updateTask(otherTask) { it.update(index = it.index - 1) }
                    }
                    visible(false)
                }) {
                    Text("Move to ${column.name}")
                }
            }

            Divider()

            val isFirstInColumn = task.index == 1
            DropdownMenuItem({
                val otherTask = tasks.single { it.columnId == task.columnId && it.index == task.index - 1 }
                updateTask(task) { it.update(index = it.index - 1) }
                updateTask(otherTask) { it.update(index = it.index + 1) }
                visible(false)
            }, enabled = !isFirstInColumn) {
                Text("Rank higher")
            }

            val isLastInColumn = task.index == tasks.filter { it.columnId == task.columnId }.maxOfOrNull { it.index }
            DropdownMenuItem({
                val otherTask = tasks.single { it.columnId == task.columnId && it.index == task.index + 1 }
                updateTask(task) { it.update(index = it.index + 1) }
                updateTask(otherTask) { it.update(index = it.index - 1) }
                visible(false)
            }, enabled = !isLastInColumn) {
                Text("Rank lower")
            }
        }
    }

    @Composable
    private fun ColumnScope.DateField(label: String, instant: Instant, zone: ZoneId = ZoneId.of("Europe/Amsterdam")) {
        Text(label + instant.atZone(zone).format(DATETIME_FORMAT),
            fontSize = 10.sp,
            modifier = Modifier.align(Alignment.End))
    }

    private fun updateTask(task: Task, update: (Task) -> Task) {
        val updated = update(task)
        db.connection {
            TaskDao.update(this, updated)
        }
        tasks.replaceIf(updated) { it.id == task.id }
    }

    companion object {
        private val DATETIME_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy H:mm")
    }
}

fun <T> MutableList<T>.replaceIf(newElement: T, condition: (T) -> Boolean) = replaceAll { if (condition(it)) newElement else it }

operator fun <T> MutableState<T>.invoke(value: T) {
    this.value = value
}
