package db

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import models.Flood
import models.LandSlide

object Database {
    private val database: MongoDatabase = MongoClientProvider.client.getDatabase("GeoMetricsDB")

    val floodCollection = database.getCollection<Flood>("floods")
    val landSlideCollection = database.getCollection<LandSlide>("land_slides")
}
