/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.ragnarok.fenrir.util.serializeble.json

import dev.ragnarok.fenrir.util.serializeble.json.internal.*
import dev.ragnarok.fenrir.util.serializeble.json.internal.lexer.StringJsonLexer
import kotlinx.serialization.*
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import java.io.InputStream

/**
 * The main entry point to work with JSON serialization.
 * It is typically used by constructing an application-specific instance, with configured JSON-specific behaviour
 * and, if necessary, registered in [SerializersModule] custom serializers.
 * `Json` instance can be configured in its `Json {}` factory function using [JsonBuilder].
 * For demonstration purposes or trivial usages, Json [companion][Json.Default] can be used instead.
 *
 * Then constructed instance can be used either as regular [SerialFormat] or [StringFormat]
 * or for converting objects to [JsonElement] back and forth.
 *
 * This is the only serial format which has the first-class [JsonElement] support.
 * Any serializable class can be serialized to or from [JsonElement] with [Json.decodeFromJsonElement] and [Json.encodeToJsonElement] respectively or
 * serialize properties of [JsonElement] type.
 *
 * Example of usage:
 * ```
 * @Serializable
 * class DataHolder(val id: Int, val data: String, val extensions: JsonElement)
 *
 * val json = Json
 * val instance = DataHolder(42, "some data", buildJsonObject { put("additional key", "value") }
 *
 * // Plain StringFormat usage
 * val stringOutput: String = json.encodeToString(instance)
 *
 * // JsonElement serialization specific for JSON only
 * val jsonTree: JsonElement = json.encodeToJsonElement(instance)
 *
 * // Deserialize from string
 * val deserialized: DataHolder = json.decodeFromString<DataHolder>(stringOutput)
 *
 * // Deserialize from json tree, JSON-specific
 * val deserializedFromTree: DataHolder = json.decodeFromJsonElement<DataHolder>(jsonTree)
 *
 *  // Deserialize from string to JSON tree, JSON-specific
 *  val deserializedToTree: JsonElement = json.parseToJsonElement(stringOutput)
 * ```
 *
 * Json instance also exposes its [configuration] that can be used in custom serializers
 * that rely on [JsonDecoder] and [JsonEncoder] for customizable behaviour.
 */
@OptIn(ExperimentalSerializationApi::class)
sealed class Json(
    val configuration: JsonConfiguration,
    override val serializersModule: SerializersModule
) : StringFormat {

    @Deprecated(
        "Should not be accessed directly, use Json.schemaCache accessor instead",
        ReplaceWith("schemaCache"),
        DeprecationLevel.ERROR
    )
    internal val _schemaCache: DescriptorSchemaCache = DescriptorSchemaCache()

    /**
     * The default instance of [Json] with default configuration.
     */
    companion object Default : Json(JsonConfiguration(), EmptySerializersModule)

    /**
     * Serializes the [value] into an equivalent JSON using the given [serializer].
     *
     * @throws [SerializationException] if the given value cannot be serialized to JSON.
     */
    final override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        val result = JsonStringBuilder()
        try {
            val encoder = StreamingJsonEncoder(
                result, this,
                WriteMode.OBJ,
                arrayOfNulls(WriteMode.values().size)
            )
            encoder.encodeSerializableValue(serializer, value)
            return result.toString()
        } finally {
            result.release()
        }
    }

    /**
     * Deserializes the given JSON [string] into a value of type [T] using the given [deserializer].
     *
     * @throws [SerializationException] if the given JSON string cannot be deserialized to the value of type [T].
     */
    final override fun <T> decodeFromString(
        deserializer: DeserializationStrategy<T>,
        string: String
    ): T {
        val lexer = StringJsonLexer(string)
        val input = StreamingJsonDecoder(this, WriteMode.OBJ, lexer, deserializer.descriptor)
        val result = input.decodeSerializableValue(deserializer)
        lexer.expectEof()
        return result
    }

    /**
     * Serializes the given [value] into an equivalent [JsonElement] using the given [serializer]
     *
     * @throws [SerializationException] if the given value cannot be serialized.
     */
    fun <T> encodeToJsonElement(serializer: SerializationStrategy<T>, value: T): JsonElement {
        return writeJson(value, serializer)
    }

    /**
     * Deserializes the given [element] into a value of type [T] using the given [deserializer].
     *
     * @throws [SerializationException] if the given JSON string cannot be deserialized to the value of type [T].
     */
    fun <T> decodeFromJsonElement(
        deserializer: DeserializationStrategy<T>,
        element: JsonElement
    ): T {
        return readJson(element, deserializer)
    }

    /**
     * Deserializes the given JSON [string] into a corresponding [JsonElement] representation.
     *
     * @throws [SerializationException] if the given JSON string is malformed and cannot be deserialized
     */
    fun parseToJsonElement(string: String): JsonElement {
        return decodeFromString(JsonElementSerializer, string)
    }

    fun parseToJsonElement(stream: InputStream): JsonElement {
        return decodeFromStream(JsonElementSerializer, stream)
    }

    fun printJsonElement(element: JsonElement): String {
        return encodeToString(JsonElementSerializer, element)
    }
}

/**
 * Creates an instance of [Json] configured from the optionally given [Json instance][from] and adjusted with [builderAction].
 */
fun Json(from: Json = Json.Default, builderAction: JsonBuilder.() -> Unit): Json {
    val builder = JsonBuilder(from)
    builder.builderAction()
    val conf = builder.build()
    return JsonImpl(conf, builder.serializersModule)
}

/**
 * Serializes the given [value] into an equivalent [JsonElement] using a serializer retrieved
 * from reified type parameter.
 *
 * @throws [SerializationException] if the given value cannot be serialized to JSON.
 */
inline fun <reified T> Json.encodeToJsonElement(value: T): JsonElement {
    return encodeToJsonElement(serializersModule.serializer(), value)
}

/**
 * Deserializes the given [json] element into a value of type [T] using a deserializer retrieved
 * from reified type parameter.
 *
 * @throws [SerializationException] if the given JSON string is malformed or cannot be deserialized to the value of type [T].
 */
inline fun <reified T> Json.decodeFromJsonElement(json: JsonElement): T =
    decodeFromJsonElement(serializersModule.serializer(), json)

inline fun <reified T> Json.decodeFromJsonElementOrNull(json: JsonElement?): T? =
    json?.let { decodeFromJsonElement(serializersModule.serializer(), it) }

inline fun <reified T> Json.decodeFromJsonElementOrNull(
    serializer: KSerializer<T>,
    json: JsonElement?
): T? =
    json?.let { decodeFromJsonElement(serializer, it) }

/**
 * Builder of the [Json] instance provided by `Json { ... }` factory function.
 */
@Suppress("unused", "DeprecatedCallableAddReplaceWith")
@OptIn(ExperimentalSerializationApi::class)
class JsonBuilder internal constructor(json: Json) {
    /**
     * Specifies whether default values of Kotlin properties should be encoded.
     * `false` by default.
     */
    var encodeDefaults: Boolean = json.configuration.encodeDefaults

    /**
     * Specifies whether `null` values should be encoded for nullable properties and must be present in JSON object
     * during decoding.
     *
     * When this flag is disabled properties with `null` values without default are not encoded;
     * during decoding, the absence of a field value is treated as `null` for nullable properties without a default value.
     *
     * `true` by default.
     */
    @ExperimentalSerializationApi
    var explicitNulls: Boolean = json.configuration.explicitNulls

    /**
     * Specifies whether encounters of unknown properties in the input JSON
     * should be ignored instead of throwing [SerializationException].
     * `false` by default.
     */
    var ignoreUnknownKeys: Boolean = json.configuration.ignoreUnknownKeys

    /**
     * Removes JSON specification restriction (RFC-4627) and makes parser
     * more liberal to the malformed input. In lenient mode quoted boolean literals,
     * and unquoted string literals are allowed.
     *
     * Its relaxations can be expanded in the future, so that lenient parser becomes even more
     * permissive to invalid value in the input, replacing them with defaults.
     *
     * `false` by default.
     */
    var isLenient: Boolean = json.configuration.isLenient

    /**
     * Enables structured objects to be serialized as map keys by
     * changing serialized form of the map from JSON object (key-value pairs) to flat array like `[k1, v1, k2, v2]`.
     * `false` by default.
     */
    var allowStructuredMapKeys: Boolean = json.configuration.allowStructuredMapKeys

    /**
     * Specifies whether resulting JSON should be pretty-printed.
     *  `false` by default.
     */
    var prettyPrint: Boolean = json.configuration.prettyPrint

    /**
     * Specifies indent string to use with [prettyPrint] mode
     * 4 spaces by default.
     * Experimentality note: this API is experimental because
     * it is not clear whether this option has compelling use-cases.
     */
    @ExperimentalSerializationApi
    var prettyPrintIndent: String = json.configuration.prettyPrintIndent

    /**
     * Enables coercing incorrect JSON values to the default property value in the following cases:
     *   1. JSON value is `null` but property type is non-nullable.
     *   2. Property type is an enum type, but JSON value contains unknown enum member.
     *
     * `false` by default.
     */
    var coerceInputValues: Boolean = json.configuration.coerceInputValues

    /**
     * Switches polymorphic serialization to the default array format.
     * This is an option for legacy JSON format and should not be generally used.
     * `false` by default.
     */
    var useArrayPolymorphism: Boolean = json.configuration.useArrayPolymorphism

    /**
     * Name of the class descriptor property for polymorphic serialization.
     * "type" by default.
     */
    var classDiscriminator: String = json.configuration.classDiscriminator

    /**
     * Removes JSON specification restriction on
     * special floating-point values such as `NaN` and `Infinity` and enables their serialization and deserialization.
     * When enabling it, please ensure that the receiving party will be able to encode and decode these special values.
     * `false` by default.
     */
    var allowSpecialFloatingPointValues: Boolean =
        json.configuration.allowSpecialFloatingPointValues

    /**
     * Specifies whether Json instance makes use of [JsonNames] annotation.
     *
     * Disabling this flag when one does not use [JsonNames] at all may sometimes result in better performance,
     * particularly when a large count of fields is skipped with [ignoreUnknownKeys].
     * `true` by default.
     */
    var useAlternativeNames: Boolean = json.configuration.useAlternativeNames

    /**
     * Module with contextual and polymorphic serializers to be used in the resulting [Json] instance.
     */
    var serializersModule: SerializersModule = json.serializersModule

    @OptIn(ExperimentalSerializationApi::class)
    internal fun build(): JsonConfiguration {
        if (useArrayPolymorphism) require(classDiscriminator == defaultDiscriminator) {
            "Class discriminator should not be specified when array polymorphism is specified"
        }

        if (!prettyPrint) {
            require(prettyPrintIndent == defaultIndent) {
                "Indent should not be specified when default printing mode is used"
            }
        } else if (prettyPrintIndent != defaultIndent) {
            // Values allowed by JSON specification as whitespaces
            val allWhitespaces =
                prettyPrintIndent.all { it == ' ' || it == '\t' || it == '\r' || it == '\n' }
            require(allWhitespaces) {
                "Only whitespace, tab, newline and carriage return are allowed as pretty print symbols. Had $prettyPrintIndent"
            }
        }

        return JsonConfiguration(
            encodeDefaults, ignoreUnknownKeys, isLenient,
            allowStructuredMapKeys, prettyPrint, explicitNulls, prettyPrintIndent,
            coerceInputValues, useArrayPolymorphism,
            classDiscriminator, allowSpecialFloatingPointValues, useAlternativeNames
        )
    }
}

@OptIn(ExperimentalSerializationApi::class)
private class JsonImpl(configuration: JsonConfiguration, module: SerializersModule) :
    Json(configuration, module) {

    init {
        validateConfiguration()
    }

    private fun validateConfiguration() {
        if (serializersModule == EmptySerializersModule) return // Fast-path for in-place JSON allocations
        val collector = PolymorphismValidator(
            configuration.useArrayPolymorphism,
            configuration.classDiscriminator
        )
        serializersModule.dumpTo(collector)
    }
}

private const val defaultIndent = "    "
private const val defaultDiscriminator = "type"
