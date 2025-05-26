package models

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

@Serializable
data class FireStationProperties(
    val location: String,
    val address: String,
    val city: String,
    val description: String,
    val telephoneNumber: String
)

@Serializable
data class FireStation(
    @Serializable(with = ObjectIdSerializer::class)
    @BsonId val id: ObjectId = ObjectId(),
    val type: String,
    val documentId: Int? = null,
    val geometry: GeoJsonPoint,
    val properties: FireStationProperties,
    val __v: Int = 0
)