package com.foxit.kotlin.db

import org.junit.Before
import org.junit.Test

class DbSchemaTest {
    private lateinit var db: DatabaseService

    @Before
    fun init() {
        db = DatabaseService.inMemory("schemaTest")
        db.connect()
    }

    @Test
    fun testSchemaCanBeCreated() {
        db.createSchema()
    }
}
