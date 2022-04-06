package com.foxit.kotlin.dao

import com.foxit.kotlin.db.*
import com.foxit.kotlin.dto.Dto
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

abstract class Dao<T: Dto>(
    table: String,
    writeFields: List<String>,
    allFields: List<String> = listOf("*"),
    idField: String = "id",
) {
    private val selectAllStatement = allFields.joinToString(", ", "SELECT "," FROM $table")
    private val selectSingleStatement = "$selectAllStatement WHERE $idField = ?"
    private val insertStatement = writeFields.joinToString(", ", "INSERT INTO $table (", ") ") +
        writeFields.joinToString(",", "VALUES (", ")") { "?" }
    private val updateStatement = writeFields.joinToString(", ", "UPDATE $table SET ", " WHERE $idField = ?") { "$it = ?"}

    abstract fun createFromResultSet(resultSet: ResultSet): T
    abstract fun toPreparedStatement(dto: T, statement: PreparedStatement)

    fun selectSingle(connection: Connection, id: Int) = connection.querySingle(selectSingleStatement,
        { setInt(1, id) }) { createFromResultSet(this) }

    fun selectAll(connection: Connection) = connection.query(selectAllStatement) { createFromResultSet(this) }

    fun insert(connection: Connection, dto: T) = connection.insert(insertStatement,
        { toPreparedStatement(dto, this) }) { getInt(1) }
        .single()

    fun update(connection: Connection, dto: T) = connection.update(updateStatement) {
        toPreparedStatement(dto, this)
        setInt(parameterMetaData.parameterCount, dto.id)
    }
}