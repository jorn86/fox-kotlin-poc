package com.foxit.kotlin.db

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class DbSchemaTest {
    private lateinit var db: DatabaseService

    @BeforeEach
    fun init(info: TestInfo) {
        db = DatabaseService("jdbc:h2:mem:${info.testMethod};DB_CLOSE_DELAY=-1")
        db.connect()
    }

    @Test
    fun testSchemaCanBeCreated() {
        db.createSchema()
    }
}
