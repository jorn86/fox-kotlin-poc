package com.foxit.kotlin.orm

import com.foxit.kotlin.dto.Column
import java.sql.PreparedStatement
import java.sql.ResultSet

object ColumnMapper : DtoMapper<Column>("column", listOf("name")) {
    override fun createFromResultSet(resultSet: ResultSet) = Column(
        resultSet.getString("name"),
        resultSet.getInt("id"),
    )

    override fun toPreparedStatement(dto: Column, statement: PreparedStatement) {
        statement.setString(1, dto.name)
    }
}
