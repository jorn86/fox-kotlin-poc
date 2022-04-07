package com.foxit.kotlin

import com.foxit.kotlin.app.App
import com.foxit.kotlin.dao.ColumnDao
import com.foxit.kotlin.dao.TaskDao
import com.foxit.kotlin.db.DatabaseService
import com.foxit.kotlin.db.createSchema
import com.foxit.kotlin.dto.Column
import com.foxit.kotlin.dto.Task
import java.time.Instant
import java.time.temporal.ChronoUnit

fun main() {
//    val db = DatabaseService.file("./kotlin-poc") // persistent, file
    val db = DatabaseService.inMemory("kotlin-poc") // in memory only, not persisted
    db.connect()
    initDb(db) // first run only!

    App(db).start()
}

private fun initDb(db: DatabaseService) {
    db.createSchema()

    db.transaction {
        val todo = ColumnDao.insert(this, Column(1,"TODO"))
        val progress = ColumnDao.insert(this, Column(2, "In progress"))
        val review = ColumnDao.insert(this, Column(2, "In review"))
        TaskDao.insert(this, Task(todo, 2, "Second task", "With description",
            modified = Instant.now().minus(400, ChronoUnit.MINUTES)))
        TaskDao.insert(this, Task(todo, 1, "First task",
            modified = Instant.now().minus(500, ChronoUnit.MINUTES)))
        TaskDao.insert(this, Task(progress, 2, "Another task",
            "With a much longer description that should span multiple lines",
            modified = Instant.now().minus(234, ChronoUnit.MINUTES)))
        TaskDao.insert(this, Task(progress, 1, "Progress task", "With description",
            modified = Instant.now().minus(42, ChronoUnit.MINUTES)))
    }
}
