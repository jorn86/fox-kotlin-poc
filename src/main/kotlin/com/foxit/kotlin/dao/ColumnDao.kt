package com.foxit.kotlin.dao

import com.foxit.kotlin.dto.Column
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp

object ColumnDao : Dao<Column>("column", listOf("index", "name", "modified")) {
    override fun createFromResultSet(resultSet: ResultSet) = Column(
        resultSet.getInt("index"),
        resultSet.getString("name"),
        resultSet.getInt("id"),
        resultSet.getTimestamp("created").toInstant(),
        resultSet.getTimestamp("modified").toInstant(),
    )

    override fun toPreparedStatement(dto: Column, statement: PreparedStatement) {
        statement.setInt(1, dto.index)
        statement.setString(2, dto.name)
        statement.setTimestamp(3, Timestamp.from(dto.modified))
    }
}
