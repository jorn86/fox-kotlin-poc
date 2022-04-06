package com.foxit.kotlin.dao

import com.foxit.kotlin.db.insert
import com.foxit.kotlin.db.query
import com.foxit.kotlin.db.querySingle
import com.foxit.kotlin.db.update
import com.foxit.kotlin.dto.Dto
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

internal abstract class Dao<T: Dto>(
    table: String,
    writeFields: List<String>,
    allFields: List<String> = listOf("*"),
    idField: String = "id",
) : IDao<T> {
    private val selectAllStatement = allFields.joinToString(", ", "SELECT "," FROM $table")
    private val selectSingleStatement = "$selectAllStatement WHERE $idField = ?"
    private val insertStatement = writeFields.joinToString(", ", "INSERT INTO $table (", ") ") +
        writeFields.joinToString(",", "VALUES (", ")") { "?" }
    private val updateStatement = writeFields.joinToString(", ", "UPDATE $table SET ", " WHERE $idField = ?") { "$it = ?"}

    protected abstract fun createFromResultSet(resultSet: ResultSet): T
    protected abstract fun toPreparedStatement(dto: T, statement: PreparedStatement)

    override fun selectSingle(connection: Connection, id: Int) = connection.querySingle(selectSingleStatement,
        { setInt(1, id) }) { createFromResultSet(this) }

    override fun selectAll(connection: Connection) = connection.query(selectAllStatement) { createFromResultSet(this) }

    override fun insert(connection: Connection, dto: T) = connection.insert(insertStatement,
        { toPreparedStatement(dto, this) }) { getInt(1) }
        .single()

    override fun update(connection: Connection, dto: T) = connection.update(updateStatement) {
        toPreparedStatement(dto, this)
        setInt(parameterMetaData.parameterCount, dto.id)
    }
}
