package db

import kotlinx.coroutines.flow.toList
import models.GeoJsonPolygon
import models.LandSlide
import models.LandSlideProperties
import models.Flood
import models.FloodProperties

suspend fun getNextUniqueLandslideObjectId(): Int {
    val allLandslides = Database.landSlideCollection.find().toList()
    val maxObjectId = allLandslides.maxOfOrNull { it.properties.OBJECTID } ?: 0
    return maxObjectId + 1
}

suspend fun getNextUniqueFloodObjectId(): Int {
    val allFloods = Database.floodCollection.find().toList()
    val maxObjectId = allFloods.maxOfOrNull { it.properties.OBJECTID } ?: 0
    return maxObjectId + 1
}

suspend fun insertLandslideIfUnique(
    geometry: GeoJsonPolygon,
    landSlideType: Int
): Boolean {
    val objectId = getNextUniqueLandslideObjectId()
    val allLandslides = Database.landSlideCollection.find().toList()
    val exists = allLandslides.any { it.properties.OBJECTID == objectId }
    if (!exists) {
        val landslide = LandSlide(
            type = "Feature",
            id = objectId,
            geometry = geometry,
            properties = LandSlideProperties(
                OBJECTID = objectId,
                LandSlideType = landSlideType
            )
        )
        Database.landSlideCollection.insertOne(landslide)
        return true
    }
    return false
}

suspend fun insertFloodIfUnique(
    geometry: GeoJsonPolygon,
    floodType: Int
): Boolean {
    val objectId = getNextUniqueFloodObjectId()
    val allFloods = Database.floodCollection.find().toList()
    val exists = allFloods.any { it.properties.OBJECTID == objectId }
    if (!exists) {
        val flood = Flood(
            type = "Feature",
            id = objectId,
            geometry = geometry,
            properties = FloodProperties(
                OBJECTID = objectId,
                FloodType = floodType
            )
        )
        Database.floodCollection.insertOne(flood)
        return true
    }
    return false
}