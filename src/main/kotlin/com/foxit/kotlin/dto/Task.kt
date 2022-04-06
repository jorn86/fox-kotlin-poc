package com.foxit.kotlin.dto

import com.foxit.kotlin.dao.magic.Id
import com.foxit.kotlin.dao.magic.Write
import java.time.Instant

data class Task(
    @Write val columnId: Int,
    @Write val index: Int,
    @Write val name: String,
    @Write val description: String? = null,
    @Id override val id: Int = -1,
    val created: Instant = Instant.now(),
    @Write val modified: Instant = Instant.now(),
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
