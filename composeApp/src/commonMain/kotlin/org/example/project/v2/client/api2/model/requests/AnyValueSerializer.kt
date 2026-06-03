package org.example.project.v2.client.api2.model.requests

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object AnyValueSerializer : KSerializer<Any> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("AnyValue", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Any) {
        when (value) {
            is String -> encoder.encodeString(value)
            is Int -> encoder.encodeInt(value)
            is Long -> encoder.encodeLong(value)
            is Double -> encoder.encodeDouble(value)
            is Float -> encoder.encodeFloat(value)
            is Boolean -> encoder.encodeBoolean(value)
            else -> encoder.encodeString(value.toString())
        }
    }

    override fun deserialize(decoder: Decoder): Any {
        return decoder.decodeString()
    }
}
