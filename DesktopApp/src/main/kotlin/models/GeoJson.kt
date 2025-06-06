package models

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class GeoJsonPolygon(
    @EncodeDefault
    val type: String = "Polygon",
    val coordinates: ArrayList<ArrayList<ArrayList<Double>>> = ArrayList(),
    @Serializable(with = ObjectIdSerializer::class)
    val _id: ObjectId? = null
)

@Serializable
@OptIn(ExperimentalSerializationApi::class)
data class GeoJsonPoint(
    @EncodeDefault
    val type: String = "Point",
    val coordinates: ArrayList<Double> = ArrayList(),
    @Serializable(with = ObjectIdSerializer::class)
    val _id: ObjectId? = null
)