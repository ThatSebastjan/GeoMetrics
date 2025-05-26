package models

import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class GeoJsonPolygon(
    val type: String = "Polygon",
    val coordinates: ArrayList<ArrayList<ArrayList<Double>>> = ArrayList(),
    @Serializable(with = ObjectIdSerializer::class)
    val _id: ObjectId? = null
)