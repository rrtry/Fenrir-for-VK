package dev.ragnarok.fenrir.util.serializeble.retrofit.kotlinx.serialization

import kotlinx.serialization.SerializationStrategy
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Converter

internal class SerializationStrategyConverter<T>(
    private val contentType: MediaType,
    private val saver: SerializationStrategy<T>,
    private val serializer: Serializer
) : Converter<T, RequestBody> {
    override fun convert(value: T) = serializer.toRequestBody(contentType, saver, value)
}
