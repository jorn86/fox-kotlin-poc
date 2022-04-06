package com.foxit.kotlin.dao.magic

@Target(AnnotationTarget.CONSTRUCTOR, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Table(val name: String = "")
