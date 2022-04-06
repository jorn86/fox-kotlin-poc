package com.foxit.kotlin.dao.magic

import com.foxit.kotlin.dao.Dao
import com.foxit.kotlin.dao.IDao
import com.foxit.kotlin.dto.Dto
import com.google.common.base.CaseFormat
import com.google.common.collect.Ordering
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaField

object MagicDao {
    inline fun <reified T: Dto> create() = create(T::class)

    @PublishedApi
    internal fun <T: Dto> create(dto: KClass<T>): IDao<T> {
        require(dto.isData) { "DTO type must be a data class" }

        val table = dto.valueOf(Table::name) ?: CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, dto.simpleName!!)

        val constructor = dto.constructors.singleOrNull { it.hasAnnotation<Table>() }
            ?: dto.primaryConstructor
            ?: throw IllegalArgumentException("DTO must have a single constructor annotated with @Table, or a primary constructor")

        val order = Ordering.explicit(constructor.parameters.map { it.name }).onResultOf(KProperty<*>::name)
        val props = dto.declaredMemberProperties.sortedWith(order)
        val id = props.singleOrNull { it.hasAnnotation<Id>() }
            ?: throw IllegalArgumentException("DTO class must have a single property annotated with Id")
        require(id.returnType == Int::class.createType()) {
            "Id property must be of type ${Int::class}, but was ${id.returnType}"
        }
        require(!id.hasAnnotation<Write>()) { "Id property cannot be writeable" }

        val idField = id.name(Id::name)
        val allFields = props.map { it.fieldName() }
        val writeProps = props.filter { it.hasAnnotation<Write>() }
        val writeFields = writeProps.map { it.name(Write::name) }

        return object : Dao<T>(table, writeFields, allFields, idField) {
            override fun createFromResultSet(resultSet: ResultSet): T {
                val args = props.map { get(resultSet, it) }
                return constructor.call(*args.toTypedArray())
            }

            override fun toPreparedStatement(dto: T, statement: PreparedStatement) {
                writeProps.forEachIndexed { i, prop ->
                    val value = prop.get(dto)
                    statement.setObject(i + 1, if (value is Instant) Timestamp.from(value) else value)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> get(resultSet: ResultSet, prop: KProperty<T>): T? {
        val name = prop.fieldName()
        val value = when (prop.javaField!!.type) {
            String::class.java -> resultSet.getString(name)
            Instant::class.java -> resultSet.getTimestamp(name)?.toInstant()
            Int::class.java -> resultSet.getInt(name)
            else -> throw IllegalArgumentException("Property type ${prop.returnType} not implemented (yet)")
        } as T
        return value.takeIf { !resultSet.wasNull() }
    }

    private fun KProperty<*>.fieldName(): String = valueOf(Read::name) ?: valueOf(Write::name) ?: name(Id::name)

    private inline fun <reified T: Annotation> KProperty<*>.name(getter: (T) -> String) =
        valueOf(getter) ?: CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name)

    private inline fun <reified T: Annotation> KAnnotatedElement.valueOf(getter: (T) -> String): String? =
        findAnnotation<T>()?.let(getter)?.takeIf { it.isNotEmpty() }
}
