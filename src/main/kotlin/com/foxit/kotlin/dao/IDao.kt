package com.foxit.kotlin.dao

import com.foxit.kotlin.dto.Dto
import java.sql.Connection

interface IDao<T : Dto> {
    fun selectSingle(connection: Connection, id: Int): T
    fun selectAll(connection: Connection): List<T>
    fun insert(connection: Connection, dto: T): Int
    fun update(connection: Connection, dto: T): Int

    fun insertAndRequery(connection: Connection, dto: T) = selectSingle(connection, insert(connection, dto))
}
