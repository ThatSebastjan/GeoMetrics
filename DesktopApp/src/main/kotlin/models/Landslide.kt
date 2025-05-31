package models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

@Serializable
data class LandSlideProperties(
    val OBJECTID: Int,
    val LandSlideType: Int
)

@Serializable
data class LandSlide(
    @Serializable(with = ObjectIdSerializer::class)
    @BsonId val _id: ObjectId = ObjectId(),
    val type: String,
    val id: Int? = null,
    val documentId: Int? = null,
    val geometry: GeoJsonPolygon,
    val properties: LandSlideProperties,
    val __v: Int = 0
)