package com.foxit.kotlin.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.foxit.kotlin.dao.ColumnDao
import com.foxit.kotlin.db.DatabaseService
import com.foxit.kotlin.db.createSchema
import com.foxit.kotlin.db.statement
import com.foxit.kotlin.dto.Column
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class UiTest {
    private lateinit var db: DatabaseService

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        db = DatabaseService.inMemory("UiTest")
        db.connect()
        db.createSchema()
        db.connection {
            ColumnDao.insert(this, Column(1, "First"))
        }

        composeTestRule.setContent {
            App(db).App()
        }
    }

    @After
    fun shutdownDb() {
        db.connection { statement { execute("SHUTDOWN") } }
    }

    @Test
    fun debugTree() {
        composeTestRule.onRoot().printToLog("UiTest")
    }

    @Test
    fun newColumn() {
        composeTestRule.onNodeWithTag("NewColumn").performTextInput("Test new column\n")
        composeTestRule.onNodeWithText("Test new column").assertIsDisplayed()
    }

    @Test
    fun exit() {
        composeTestRule.onNodeWithContentDescription("Exit").performClick()
    }
}
