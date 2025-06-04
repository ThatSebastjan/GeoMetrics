package db

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import models.Flood
import models.LandSlide
import models.Earthquake
import models.FireStation
import models.LandLot
import models.LandUse

object Database {
    private val database: MongoDatabase = MongoClientProvider.client.getDatabase("GeoMetricsDB")

    val floodCollection = database.getCollection<Flood>("floods")
    val landSlideCollection = database.getCollection<LandSlide>("land_slides")
    val earthquakeCollection = database.getCollection<Earthquake>("earthquakes")
    val fireStationCollection = database.getCollection<FireStation>("firestations")
    val landLotCollection = database.getCollection<LandLot>("land_lots")
    val landUseCollection = database.getCollection<LandUse>("land_uses")
}
