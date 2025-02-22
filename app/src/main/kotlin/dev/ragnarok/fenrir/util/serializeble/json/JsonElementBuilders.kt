/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(ExperimentalContracts::class)

package dev.ragnarok.fenrir.util.serializeble.json

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Builds [JsonObject] with the given [builderAction] builder.
 * Example of usage:
 * ```
 * val json = buildJsonObject {
 *     put("booleanKey", true)
 *     putJsonArray("arrayKey") {
 *         for (i in 1..10) add(i)
 *     }
 *     putJsonObject("objectKey") {
 *         put("stringKey", "stringValue")
 *     }
 * }
 * ```
 */
inline fun buildJsonObject(builderAction: JsonObjectBuilder.() -> Unit): JsonObject {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    val builder = JsonObjectBuilder()
    builder.builderAction()
    return builder.build()
}


/**
 * Builds [JsonArray] with the given [builderAction] builder.
 * Example of usage:
 * ```
 * val json = buildJsonArray {
 *     add(true)
 *     addJsonArray {
 *         for (i in 1..10) add(i)
 *     }
 *     addJsonObject {
 *         put("stringKey", "stringValue")
 *     }
 * }
 * ```
 */
inline fun buildJsonArray(builderAction: JsonArrayBuilder.() -> Unit): JsonArray {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    val builder = JsonArrayBuilder()
    builder.builderAction()
    return builder.build()
}

/**
 * DSL builder for a [JsonObject]. To create an instance of builder, use [buildJsonObject] build function.
 */
@JsonDslMarker
class JsonObjectBuilder @PublishedApi internal constructor() {

    private val content: MutableMap<String, JsonElement> = linkedMapOf()

    /**
     * Add the given JSON [element] to a resulting JSON object using the given [key].
     *
     * Returns the previous value associated with [key], or `null` if the key was not present.
     */
    fun put(key: String, element: JsonElement): JsonElement? = content.put(key, element)

    @PublishedApi
    internal fun build(): JsonObject = JsonObject(content)
}

/**
 * Add the [JSON][JsonObject] produced by the [builderAction] function to a resulting json object using the given [key].
 *
 * Returns the previous value associated with [key], or `null` if the key was not present.
 */
fun JsonObjectBuilder.putJsonObject(
    key: String,
    builderAction: JsonObjectBuilder.() -> Unit
): JsonElement? =
    put(key, buildJsonObject(builderAction))

/**
 * Add the [JSON array][JsonArray] produced by the [builderAction] function to a resulting json object using the given [key].
 *
 * Returns the previous value associated with [key], or `null` if the key was not present.
 */
fun JsonObjectBuilder.putJsonArray(
    key: String,
    builderAction: JsonArrayBuilder.() -> Unit
): JsonElement? =
    put(key, buildJsonArray(builderAction))

/**
 * Add the given boolean [value] to a resulting JSON object using the given [key].
 *
 * Returns the previous value associated with [key], or `null` if the key was not present.
 */
fun JsonObjectBuilder.put(key: String, value: Boolean?): JsonElement? =
    put(key, JsonPrimitive(value))

/**
 * Add the given numeric [value] to a resulting JSON object using the given [key].
 *
 * Returns the previous value associated with [key], or `null` if the key was not present.
 */
fun JsonObjectBuilder.put(key: String, value: Number?): JsonElement? =
    put(key, JsonPrimitive(value))

/**
 * Add the given string [value] to a resulting JSON object using the given [key].
 *
 * Returns the previous value associated with [key], or `null` if the key was not present.
 */
fun JsonObjectBuilder.put(key: String, value: String?): JsonElement? =
    put(key, JsonPrimitive(value))

/**
 * DSL builder for a [JsonArray]. To create an instance of builder, use [buildJsonArray] build function.
 */
@JsonDslMarker
class JsonArrayBuilder @PublishedApi internal constructor() {

    private val content: MutableList<JsonElement> = mutableListOf()

    /**
     * Adds the given JSON [element] to a resulting array.
     *
     * Always returns `true` similarly to [ArrayList] specification.
     */
    fun add(element: JsonElement): Boolean {
        content += element
        return true
    }

    @PublishedApi
    internal fun build(): JsonArray = JsonArray(content)
}

/**
 * Adds the given boolean [value] to a resulting array.
 *
 * Always returns `true` similarly to [ArrayList] specification.
 */
fun JsonArrayBuilder.add(value: Boolean?): Boolean = add(JsonPrimitive(value))

/**
 * Adds the given numeric [value] to a resulting array.
 *
 * Always returns `true` similarly to [ArrayList] specification.
 */
fun JsonArrayBuilder.add(value: Number?): Boolean = add(JsonPrimitive(value))

/**
 * Adds the given string [value] to a resulting array.
 *
 * Always returns `true` similarly to [ArrayList] specification.
 */
fun JsonArrayBuilder.add(value: String?): Boolean = add(JsonPrimitive(value))

/**
 * Adds the [JSON][JsonObject] produced by the [builderAction] function to a resulting array.
 *
 * Always returns `true` similarly to [ArrayList] specification.
 */
fun JsonArrayBuilder.addJsonObject(builderAction: JsonObjectBuilder.() -> Unit): Boolean =
    add(buildJsonObject(builderAction))

/**
 * Adds the [JSON][JsonArray] produced by the [builderAction] function to a resulting array.
 *
 * Always returns `true` similarly to [ArrayList] specification.
 */
fun JsonArrayBuilder.addJsonArray(builderAction: JsonArrayBuilder.() -> Unit): Boolean =
    add(buildJsonArray(builderAction))

private const val infixToDeprecated =
    "Infix 'to' operator is deprecated for removal for the favour of 'add'"
private const val unaryPlusDeprecated =
    "Unary plus is deprecated for removal for the favour of 'add'"

@DslMarker
internal annotation class JsonDslMarker
