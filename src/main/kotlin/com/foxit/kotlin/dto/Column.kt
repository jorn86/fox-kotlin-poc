package com.foxit.kotlin.dto

import com.foxit.kotlin.dao.magic.Id
import com.foxit.kotlin.dao.magic.Write
import java.time.Instant

data class Column(
    @Write val index: Int,
    @Write val name: String,
    @Id override val id: Int = -1,
    val created: Instant = Instant.now(),
    @Write val modified: Instant = Instant.now(),
): Dto {
    fun update(
        index: Int = this.index,
        name: String = this.name,
    ) = copy(
        index = index,
        name = name.trim(),
        modified = Instant.now())
}
