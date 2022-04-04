package com.foxit.kotlin.dto

import java.time.Instant

data class Task(
    val columnId: Int,
    val name: String,
    val description: String? = null,
    override val id: Int = -1,
    val created: Instant = Instant.now(),
) : Dto
