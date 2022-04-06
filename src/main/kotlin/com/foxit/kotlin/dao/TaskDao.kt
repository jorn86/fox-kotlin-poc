package com.foxit.kotlin.dao

import com.foxit.kotlin.dao.magic.MagicDao
import com.foxit.kotlin.db.querySingle
import com.foxit.kotlin.dto.Task
import java.sql.Connection

object TaskDao : IDao<Task> by MagicDao.create(Task::class) {
    fun getMaxIndex(connection: Connection, columnId: Int) = connection.querySingle(
        "SELECT MAX(index) FROM task WHERE column_id = ?",
        { setInt(1, columnId)})
        { getInt(1) }
}
