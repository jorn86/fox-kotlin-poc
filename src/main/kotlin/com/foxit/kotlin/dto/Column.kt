package com.foxit.kotlin.dto

import java.time.Instant

data class Column(
    val index: Int,
    val name: String,
    override val id: Int = -1,
    val created: Instant = Instant.now(),
    val modified: Instant = Instant.now(),
): Dto {
    fun update(
        index: Int = this.index,
        name: String = this.name,
    ) = copy(
        index = index,
        name = name.trim(),
        modified = Instant.now())
}
