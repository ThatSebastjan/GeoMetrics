import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import models.*
import db.*

fun main() = runBlocking {
    println("Starting database connection test...")

    try {
        val database = MongoClientProvider.client.getDatabase("GeoMetricsDB")
        println("✅ Successfully connected to MongoDB database: ${database.name}")

        val floodCount = Database.floodCollection.countDocuments()
        println("✅ Flood collection access OK - Contains $floodCount documents")

        val landslideCount = Database.landSlideCollection.countDocuments()
        println("✅ Landslide collection access OK - Contains $landslideCount documents")

        val earthquakeCount = Database.earthquakeCollection.countDocuments()
        println("✅ Earthquake collection access OK - Contains $earthquakeCount documents")

        val fireStationCount = Database.fireStationCollection.countDocuments()
        println("✅ Fire station collection access OK - Contains $fireStationCount documents")

        if (floodCount > 0) {
            println("\nSample flood document:")
            val flood = Database.floodCollection.find().limit(1).toList().firstOrNull()
            flood?.let {
                println("ID: ${it.id}")
                println("Type: ${it.type}")
                println("Properties: ${it.properties}")
                println("Geometry coordinates count: ${it.geometry.coordinates.getOrNull(0)?.size ?: 0}")
            }
        }

        if (landslideCount > 0) {
            println("\nSample landslide document:")
            val landslide = Database.landSlideCollection.find().limit(1).toList().firstOrNull()
            landslide?.let {
                println("ID: ${it.id}")
                println("Type: ${it.type}")
                println("Properties: ${it.properties}")
                println("Geometry coordinates count: ${it.geometry.coordinates.getOrNull(0)?.size ?: 0}")
            }
        }

        if (earthquakeCount > 0) {
            println("\nSample earthquake document:")
            val earthquake = Database.earthquakeCollection.find().limit(1).toList().firstOrNull()
            earthquake?.let {
                println("ID: ${it._id}")
                println("Type: ${it.type}")
                println("Timestamp: ${it.properties.timestamp}")
                println("Magnitude: ${it.properties.magnitude}")
                println("Depth: ${it.properties.depth}")
                println("Coordinates: ${it.geometry.coordinates}")
            }
        }

        if (fireStationCount > 0) {
            println("\nSample fire station document:")
            val fireStation = Database.fireStationCollection.find().limit(1).toList().firstOrNull()
            fireStation?.let {
                println("ID: ${it.id}")
                println("Type: ${it.type}")
                println("Location: ${it.properties.location}")
                println("Address: ${it.properties.address}")
                println("City: ${it.properties.city}")
                println("Description: ${it.properties.description}")
                println("Telephone: ${it.properties.telephoneNumber}")
                println("Coordinates: ${it.geometry.coordinates}")
            }
        }

        println("\nDatabase connection test completed successfully!")
    } catch (e: Exception) {
        println("❌ Database connection test failed: ${e.message}")
        e.printStackTrace()
    } finally {

        MongoClientProvider.client.close()
        println("Connection closed.")
    }
}