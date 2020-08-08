package de.randombyte.fourdata.serialization

import kotlinx.serialization.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializer(forClass = LocalDateTime::class)
object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    val dateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd-HH-mm-ss")

    fun deserialize(string: String) = LocalDateTime.parse(string,
        dateTimeFormatter
    )
    fun serialize(localDateTime: LocalDateTime) = localDateTime.format(dateTimeFormatter)

    override val descriptor = PrimitiveDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder) =
        deserialize(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(
            serialize(
                value
            )
        )
    }
}