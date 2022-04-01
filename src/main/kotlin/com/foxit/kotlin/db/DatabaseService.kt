package com.foxit.kotlin.db

import org.h2.jdbcx.JdbcDataSource
import org.slf4j.LoggerFactory
import java.sql.Connection
import javax.sql.DataSource

class DatabaseService(
    private val url: String = "jdbc:h2:./kotlin-poc",
    private val user: String = "sa",
    private val password: String = "sa",
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private lateinit var dataSource: DataSource

    fun connect() {
        val dataSource = JdbcDataSource().also {
            it.setURL(url)
            it.user = user
            it.password = password
        }
        dataSource.connection.use {
            it.createStatement().use { st ->
                st.executeQuery("SELECT 1")
            }
        }
        log.info("Database connection successful")
        this.dataSource = dataSource
    }

    fun <T> connection(action: (Connection) -> T) = dataSource.connection.use {
        action(it)
    }
}
