package com.foxit.kotlin.dto

import java.time.Instant

data class Task(
    val columnId: Int,
    val index: Int,
    val name: String,
    val description: String? = null,
    override val id: Int = -1,
    val created: Instant = Instant.now(),
    val modified: Instant = Instant.now(),
) : Dto {
    fun update(
        columnId: Int = this.columnId,
        index: Int = this.index,
        name: String = this.name,
        description: String? = this.description
    ) = copy(
        columnId = columnId,
        index = index,
        name = name.trim(),
        description = description?.trim()?.takeIf { it.isNotBlank() },
        modified = Instant.now())
}
