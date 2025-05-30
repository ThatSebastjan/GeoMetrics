package models

// Earthquake data model for MongoDB and serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.time.Instant


@Serializable
data class EarthquakeProperties(
    @Contextual
    val timestamp: Instant,
    val magnitude: Double,
    val depth: Double
)

@Serializable
data class Earthquake(
    @Serializable(with = ObjectIdSerializer::class)
    @BsonId val _id: ObjectId = ObjectId(),
    val type: String,
    val id: Int? = null,
    val documentId: Int? = null,
    val geometry: GeoJsonPoint,
    val properties: EarthquakeProperties,
    val __v: Int = 0
)
