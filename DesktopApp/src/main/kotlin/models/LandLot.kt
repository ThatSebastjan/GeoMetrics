package models

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

@Serializable
data class LandLotProperties(
    val ST_PARCELE: String,
    val EID_PARCELA: String,
    val OBJECTID: Int,
    val KO_ID: Int,
    val POVRSINA: Int
)

@Serializable
data class LandLot(
    @Serializable(with = ObjectIdSerializer::class)
    @BsonId val _id: ObjectId = ObjectId(),
    val type: String,
    val id: Int? = null,
    val geometry: GeoJsonPolygon,
    val properties: LandLotProperties,
    val __v: Int = 0
)