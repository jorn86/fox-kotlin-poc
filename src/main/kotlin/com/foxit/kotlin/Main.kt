package com.foxit.kotlin

import com.foxit.kotlin.app.App
import com.foxit.kotlin.db.DatabaseService
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("com.foxit.kotlin.Main")

fun main() {
    val db = DatabaseService(
        "jdbc:h2:mem:kotlin-poc" // Enable if you want a non-persistent, in memory db (instead of a file)
    ).also { it.connect() }

    val two = db.connection {
        it.createStatement().use { st ->
            st.executeQuery("SELECT 2")
            require(st.resultSet.next())
            st.resultSet.getInt(1)
        }
    }
    log.info("Two is $two")

    App.start()
}
