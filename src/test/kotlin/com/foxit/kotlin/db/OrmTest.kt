package com.foxit.kotlin.db

import com.foxit.kotlin.dto.Column
import com.foxit.kotlin.dto.Task
import com.foxit.kotlin.orm.ColumnMapper
import com.foxit.kotlin.orm.TaskMapper
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrmTest {
    private lateinit var db: DatabaseService

    @BeforeAll
    fun setup() {
        db = DatabaseService.inMemory("TaskQueryTest")
        db.connect()
        db.createSchema()
    }

    @AfterAll
    fun teardown() {
        db.connection { statement { execute("SHUTDOWN") } }
    }

    @Test
    fun insert() {
        db.connection {
            val id = ColumnMapper.insert(this, Column("Test"))
            TaskMapper.insert(this, Task(id, "Task"))
            val generatedTaskId = TaskMapper.insert(this, Task(id, "Task", "Desc", 42, Instant.EPOCH))
            assertNotEquals(42, generatedTaskId, "Should not have used the user-specified id on new record")
        }
    }

    @Test
    fun select() {
        db.connection {
            val tasks = TaskMapper.selectAll(this)
            assertEquals(2, tasks.size)
        }
    }

    @Test
    fun update() {
        val id = db.connection {
            val id = ColumnMapper.insert(this, Column("Test"))
            val task = Task(id, "Task")
            val taskId = TaskMapper.insert(this, task)
            TaskMapper.update(this, task.copy(id = taskId, description = "Updated"))
            taskId
        }
        val task = db.connection {
            TaskMapper.selectSingle(this, id)
        }
        assertEquals(id, task.id)
        assertNotNull(task.columnId)
        assertEquals("Task", task.name)
        assertEquals("Updated", task.description)
        assertNotNull(task.created)
    }
}
