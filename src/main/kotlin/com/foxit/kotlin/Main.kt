package com.foxit.kotlin

import com.foxit.kotlin.app.App
import com.foxit.kotlin.orm.ColumnMapper
import com.foxit.kotlin.db.DatabaseService
import com.foxit.kotlin.orm.TaskMapper
import com.foxit.kotlin.db.createSchema
import com.foxit.kotlin.dto.Column
import com.foxit.kotlin.dto.Task

fun main() {
//    val db = DatabaseService() // persistent, file
    val db = DatabaseService.inMemory("kotlin-poc") // in memory only, not persisted
    db.connect()
    db.createSchema() // first run only!

    db.transaction {
        val todo = ColumnMapper.insert(this, Column("TODO"))
        val progress = ColumnMapper.insert(this, Column("In progress"))
        TaskMapper.insert(this, Task(todo, "First task"))
        TaskMapper.insert(this, Task(todo, "Second task", "With description"))
        TaskMapper.insert(this, Task(progress, "Progress task", "With description"))
    }

    App(db).start()
}
