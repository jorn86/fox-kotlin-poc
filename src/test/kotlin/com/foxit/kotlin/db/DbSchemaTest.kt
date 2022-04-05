package com.foxit.kotlin.db

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo

class DbSchemaTest {
    private lateinit var db: DatabaseService

    @BeforeEach
    fun init(info: TestInfo) {
        db = DatabaseService.inMemory(info.testMethod.get().name)
        db.connect()
    }

    @Test
    fun testSchemaCanBeCreated() {
        db.createSchema()
    }
}
