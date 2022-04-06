package com.foxit.kotlin.dao.magic

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Read(val name: String = "")
