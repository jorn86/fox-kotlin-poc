package com.foxit.kotlin.dao.magic

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Write(val name: String = "")
