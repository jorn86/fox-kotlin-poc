package com.foxit.kotlin.db

import org.h2.engine.IsolationLevel
import org.h2.jdbcx.JdbcDataSource
import org.slf4j.LoggerFactory
import java.sql.*
import javax.sql.DataSource

typealias ParameterSetter = PreparedStatement.() -> Unit
typealias ResultSetToDto<T> = ResultSet.() -> T

class DatabaseService private constructor(
    private val url: String,
    private val user: String = "sa",
    private val password: String = "sa",
    private val debug: Boolean = false,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private lateinit var dataSource: DataSource

    fun connect() {
        // This magic setting makes H2 log to slf4j
        val url = if (debug) "$url;TRACE_LEVEL_FILE=4" else url
        val dataSource = JdbcDataSource().also {
            it.setURL(url)
            it.user = user
            it.password = password
        }
        dataSource.connection.use {
            it.statement {
                executeQuery("SELECT 1")
                require(resultSet.next())
                require(!resultSet.next())
            }
        }
        log.info("Database connection successful")
        this.dataSource = dataSource
    }

    /**
     * Executes statements in auto-commit mode
     */
    fun <T> connection(action: Connection.() -> T) = dataSource.connection.use {
        action(it)
    }

    /**
     * Executes statements in a transaction, and commits when all of them succeed without throwing an exception
     */
    fun <T> transaction(level: IsolationLevel = IsolationLevel.SERIALIZABLE, action: Connection.() -> T) = connection {
        autoCommit = false
        transactionIsolation = level.jdbc
        try {
            action(this).also { commit() }
        } catch (e: SQLException) {
            rollback()
            throw e
        }
    }

    companion object {
        fun file(path: String, debug: Boolean = false) =
            DatabaseService("jdbc:h2:$path", debug = debug)

        fun inMemory(name: String, debug: Boolean = false) =
            DatabaseService("jdbc:h2:mem:$name;DB_CLOSE_DELAY=-1", debug = debug)
    }
}

fun <T> Connection.statement(action: Statement.() -> T) = createStatement().use(action)

/**
 * Executes an INSERT statement
 * @return the generated ids, as produced by the #handleResults mapper
 */
fun <T> Connection.insert(
    sql: String,
    setParameters: ParameterSetter = {},
    handleResults: ResultSetToDto<T>,
): List<T> {
    val statement = prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
    val updated = statement.apply(setParameters).executeUpdate()
    val generatedIds = statement.generatedKeys.map(handleResults)
    require(updated == generatedIds.size)
    return generatedIds
}

/**
 * Executes an INSERT or UPDATE statement
 * @return the number of updated rows
 */
fun Connection.update(sql: String, setParameters: ParameterSetter = {}) =
    prepareStatement(sql).apply(setParameters).executeUpdate()

/**
 * Queries a single record
 * @throws IllegalArgumentException if the query results in 0 or more than 1 result
 */
fun <T> Connection.querySingle(
    sql: String,
    setParameters: ParameterSetter = {},
    map: ResultSetToDto<T>,
): T {
    val results = query(sql, setParameters, map)
    return results.singleOrNull() ?: throw IllegalArgumentException("Expected 1 result, but got ${results.size}")
}

fun <T> Connection.query(sql: String, setParameters: ParameterSetter = {}, map: ResultSetToDto<T>) =
    prepareStatement(sql)
        .apply(setParameters)
        .executeQuery()
        .map(map)

fun <T> ResultSet.map(map: ResultSetToDto<T>): List<T> {
    val results = mutableListOf<T>()
    while (next()) {
        results.add(map())
    }
    return results
}
