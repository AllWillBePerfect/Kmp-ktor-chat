package org.example.project.v2.client.parser

import kotlin.reflect.KClass

interface ChatParser {
    fun toJson(any: Any): String
    fun <T : Any> fromJson(raw: String, clazz: KClass<T>): T

    @Suppress("TooGenericExceptionCaught")
    fun <T : Any> fromJsonOrNull(raw: String, clazz: KClass<T>): T? {
        return try {
            fromJson(raw, clazz)
        } catch (_: Throwable) {
            null
        }
    }
}
