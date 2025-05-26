package models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.time.Instant

// Date serializer for timestamps
object DateSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.parse(decoder.decodeString())
    }
}

@Serializable
data class GeoJsonPoint(
    val type: String = "Point",
    val coordinates: ArrayList<Double> = ArrayList(),
    @Serializable(with = ObjectIdSerializer::class)
    val _id: ObjectId? = null
)

@Serializable
data class EarthquakeProperties(
    @Serializable(with = DateSerializer::class)
    val timestamp: Instant,
    val magnitude: Double,
    val depth: Int
)

@Serializable
data class Earthquake(
    @Serializable(with = ObjectIdSerializer::class)
    @BsonId val id: ObjectId = ObjectId(),
    val type: String,
    val documentId: Int? = null,
    val geometry: GeoJsonPoint,
    val properties: EarthquakeProperties,
    val __v: Int = 0
)