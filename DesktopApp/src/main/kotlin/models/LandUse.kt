package models

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

@Serializable
data class LandUseProperties(
    val OBJECTID: Int,
    val RABA_ID: Int
)

@Serializable
data class LandUse(
    @Serializable(with = ObjectIdSerializer::class)
    @BsonId val _id: ObjectId = ObjectId(),
    val type: String,
    val id: Int? = null,
    val geometry: GeoJsonPolygon,
    val properties: LandUseProperties,
    val __v: Int = 0
)