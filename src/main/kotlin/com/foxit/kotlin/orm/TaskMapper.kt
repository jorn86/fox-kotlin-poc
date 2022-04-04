package com.foxit.kotlin.orm

import com.foxit.kotlin.dto.Task
import java.sql.PreparedStatement
import java.sql.ResultSet

object TaskMapper : DtoMapper<Task>("task", listOf("column_id", "name", "description")) {
    override fun createFromResultSet(resultSet: ResultSet) = Task(
        resultSet.getInt("column_id"),
        resultSet.getString("name"),
        resultSet.getString("description"),
        resultSet.getInt("id"),
        resultSet.getTimestamp("created").toInstant(),
    )

    override fun toPreparedStatement(dto: Task, statement: PreparedStatement) {
        statement.setInt(1, dto.columnId)
        statement.setString(2, dto.name)
        statement.setString(3, dto.description)
    }
}
