package com.foxit.kotlin.app

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.*
import com.foxit.kotlin.dao.ColumnDao
import com.foxit.kotlin.dao.TaskDao
import com.foxit.kotlin.db.DatabaseService
import com.foxit.kotlin.dto.Column
import com.foxit.kotlin.dto.Task
import com.google.common.annotations.VisibleForTesting
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Connection
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.io.path.bufferedWriter

class App(private val db: DatabaseService) {
    private val log = LoggerFactory.getLogger(javaClass)

    // Saving state like this is probably bad? But how else to do it...
    private lateinit var columns: SnapshotStateList<Column>
    private lateinit var tasks: SnapshotStateList<Task>

    // Saving scopes may not be bad but I'm not sure
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
            getRandomQuote()
        }
    }

    @Composable
    @Preview
    @VisibleForTesting internal fun App() {
        columns = remember { mutableStateListOf() }
        tasks = remember { mutableStateListOf() }

        LaunchedEffect(Unit) {
            delay(2000)
            db.connection {
                columns.addAll(ColumnDao.selectAll(this))
                tasks.addAll(TaskDao.selectAll(this))
            }
        }

        val popupVisible = remember { mutableStateOf(false) }
        Window(
            title = "Popup",
            visible = popupVisible.value,
            onCloseRequest = {
                popupVisible.value = false
            },
            state = rememberWindowState(position = WindowPosition(Alignment.Center), width = 300.dp, height = 200.dp),
        ) {
            Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
                Text(getRandomQuote(), modifier = Modifier.align(Alignment.CenterHorizontally), textAlign = TextAlign.Center)
                Button(onClick = {
                    popupVisible.value = false
                }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("OK")
                }
            }
        }

        // Doesn't show scrollbars or allow mousewheel/drag :( but at least it scrolls to the focused field
        Row(Modifier.padding(20.dp).fillMaxSize().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            MenuBar(popupVisible)

            if (columns.isEmpty()) {
                Box(Modifier.fillMaxHeight().width(800.dp)) {
                    CircularProgressIndicator(Modifier.scale(4f).align(Alignment.Center))
                }
            } else {
                columns.sortedBy { it.index }.forEach {
                    TaskColumn(it, tasks.filter { task -> task.columnId == it.id })
                }

                SubmittableTextField(modifier = Modifier.width(300.dp).testTag("NewColumn"),
                    placeholder = { Text("Add column...", fontStyle = FontStyle.Italic) })
                { name -> handleNewColumn(name) }
            }
        }
    }

    private fun handleNewColumn(name: MutableState<String>): Boolean {
        val newName = name.value.trim()
        if (newName.isBlank()) return false
        db.connection {
            val nextIndex = (columns.maxOfOrNull { it.index } ?: 0) + 1
            columns.add(ColumnDao.insertAndRequery(this, Column(nextIndex, newName)))
        }
        name.value = ""
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
                Image(Icons.Outlined.ImportExport, "Export", colorFilter = ColorFilter.tint(MaterialTheme.colors.onPrimary))
            }
            Button({
                popupVisible.value = true
                shutdownEnabled = true
            }) {
                Image(Icons.Outlined.Warning, "Quote", colorFilter = ColorFilter.tint(MaterialTheme.colors.onPrimary))
            }

            Button({
                applicationScope.exitApplication()
            }) {
                Image(Icons.Outlined.Logout, "Exit", colorFilter = ColorFilter.tint(MaterialTheme.colors.onPrimary))
            }
            Button({
                val os = System.getProperty("os.name")
                val command = when {
                    os.contains("Windows") -> "shutdown.exe -s -t 0"
                    os.contains("Linux") || os.contains("Mac") -> "shutdown -h now"
                    else -> throw IllegalArgumentException("Unsupported OS $os")
                }
                log.info("Shutting down: $command")
                Runtime.getRuntime().exec(command)
                applicationScope.exitApplication()
            }, enabled = shutdownEnabled) {
                Column {
                    Image(Icons.Outlined.PowerSettingsNew, "Shutdown", colorFilter = ColorFilter.tint(MaterialTheme.colors.onPrimary))
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
        Column(modifier = Modifier.width(300.dp).fillMaxHeight()
            .shadow(2.dp, shape = RoundedCornerShape(5.dp))
            .verticalScroll(rememberScrollState())
            .padding(10.dp)) {
            Text(column.name, fontSize = 24.sp)
            tasks.sortedBy { it.index }.forEach {
                Task(it)
            }

            SubmittableTextField(modifier = Modifier.padding(top = 20.dp).fillMaxWidth(),
                placeholder = { Text("Add task...", fontStyle = FontStyle.Italic) })
                { name -> handleNewTask(column.id, name) }
        }
    }

    private fun handleNewTask(columnId: Int, name: MutableState<String>): Boolean {
        val newName = name.value.trim()
        if (newName.isBlank()) return false
        name.value = ""
        db.connection {
            val nextIndex = TaskDao.getMaxIndex(this, columnId) + 1
            tasks.add(TaskDao.insertAndRequery(this,
                Task(columnId, nextIndex, newName, getRandomQuote())))
        }
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
            .mouseClickable {
                if (buttons.isSecondaryPressed) {
                    menuVisible.value = true
                }
            }
            .padding(top = 5.dp)
            .shadow(3.dp, shape = RoundedCornerShape(if(hovered) 7.dp else 5.dp))
            .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            EditableTextField(task.name, fontSize = 20.sp) { name ->
                db.connection { saveTask(this, task.update(name = name.value)) }
                true
            }
            EditableTextField(task.description ?: "", fontStyle = FontStyle.Italic) { description ->
                db.connection { saveTask(this, task.update(description = description.value)) }
                true
            }
            DateField("Created: ", task.created)
            DateField("Last modified: ", task.modified)

            Box(modifier = Modifier.fillMaxWidth()) {
                TaskContextMenu(task, menuVisible)
            }
        }
    }

    @Composable
    private fun TaskContextMenu(task: Task, visible: MutableState<Boolean>) {
        DropdownMenu(visible.value, modifier = Modifier.width(250.dp), onDismissRequest = {
            visible.value = false
        }) {
            columns.filter { it.id != task.columnId }.forEach { column ->
                DropdownMenuItem({
                    db.transaction {
                        val newIndex = TaskDao.getMaxIndex(this, column.id) + 1
                        saveTask(this, task.update(columnId = column.id, index = newIndex))
                        tasks.filter { it.columnId == task.columnId && it.index > task.index }.forEach { otherTask ->
                            saveTask(this, otherTask.update(index = otherTask.index - 1))
                        }
                    }
                    visible.value = false
                }) {
                    Text("Move to ${column.name}")
                }
            }

            Divider()

            val isFirstInColumn = task.index == 1
            DropdownMenuItem({
                val otherTask = tasks.single { it.columnId == task.columnId && it.index == task.index - 1 }
                db.transaction {
                    saveTask(this, task.update(index = task.index - 1))
                    saveTask(this, otherTask.update(index = otherTask.index + 1))
                }
                visible.value = false
            }, enabled = !isFirstInColumn) {
                Image(Icons.Outlined.ArrowUpward, "RankUp")
                Text("Rank higher")
            }

            val isLastInColumn = task.index == tasks.filter { it.columnId == task.columnId }.maxOfOrNull { it.index }
            DropdownMenuItem({
                val otherTask = tasks.single { it.columnId == task.columnId && it.index == task.index + 1 }
                db.transaction {
                    saveTask(this, task.update(index = task.index + 1))
                    saveTask(this, otherTask.update(index = otherTask.index - 1))
                }
                visible.value = false
            }, enabled = !isLastInColumn) {
                Image(Icons.Outlined.ArrowDownward, "RankDown")
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

    private fun saveTask(connection: Connection, task: Task) {
        TaskDao.update(connection, task)
        tasks.replaceWithIf(task) { it.id == task.id }
    }

    // Sure, we could have used a proper HTTP request library and JSON parser. But why?
    private fun getRandomQuote() = URL("https://api.quotable.io/random").openStream().bufferedReader().use {
        it.readText().substringAfter("\"content\":\"", "Quote not found").substringBefore("\"")
    }

    companion object {
        private val DATETIME_FORMAT = DateTimeFormatter.ofPattern("d MMMM yyyy H:mm")
    }
}

fun <T> MutableList<T>.replaceWithIf(newElement: T, condition: (T) -> Boolean) =
    replaceAll { if (condition(it)) newElement else it }
