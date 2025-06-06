package models

// Earthquake data model for MongoDB and serialization

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId


@Serializable
data class EarthquakeProperties(
    val timestamp: kotlinx.datetime.Instant,
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
