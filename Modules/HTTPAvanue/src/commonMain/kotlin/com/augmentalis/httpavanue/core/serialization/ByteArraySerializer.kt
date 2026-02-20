package com.augmentalis.httpavanue.core.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * ByteArray serializer for kotlinx.serialization
 * Serializes ByteArray as Base64 string for JSON/XML compatibility
 */
@OptIn(ExperimentalEncodingApi::class)
object ByteArraySerializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ByteArray", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ByteArray) {
        encoder.encodeString(Base64.encode(value))
    }

    override fun deserialize(decoder: Decoder): ByteArray {
        return Base64.decode(decoder.decodeString())
    }
}

@OptIn(ExperimentalEncodingApi::class)
fun ByteArray.encodeBase64(): String = Base64.encode(this)

@OptIn(ExperimentalEncodingApi::class)
fun String.decodeBase64(): ByteArray = Base64.decode(this)
