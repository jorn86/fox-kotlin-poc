package com.foxit.kotlin.dao

import com.foxit.kotlin.db.querySingle
import com.foxit.kotlin.dto.Task
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp

object TaskDao : Dao<Task>("task", listOf("column_id", "index", "name", "description", "modified")) {
    override fun createFromResultSet(resultSet: ResultSet) = Task(
        resultSet.getInt("column_id"),
        resultSet.getInt("index"),
        resultSet.getString("name"),
        resultSet.getString("description"),
        resultSet.getInt("id"),
        resultSet.getTimestamp("created").toInstant(),
        resultSet.getTimestamp("modified").toInstant(),
    )

    override fun toPreparedStatement(dto: Task, statement: PreparedStatement) {
        statement.setInt(1, dto.columnId)
        statement.setInt(2, dto.index)
        statement.setString(3, dto.name)
        statement.setString(4, dto.description)
        statement.setTimestamp(5, Timestamp.from(dto.modified))
    }

    fun getMaxIndex(connection: Connection, columnId: Int) = connection.querySingle(
        "SELECT MAX(index) FROM task WHERE column_id = ?",
        { setInt(1, columnId)})
        { getInt(1) }
}
