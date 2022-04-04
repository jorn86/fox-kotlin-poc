package com.foxit.kotlin.dto

data class Column(
    val name: String,
    override val id: Int = -1,
): Dto
