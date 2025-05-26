import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import models.*
import db.*

fun main() = runBlocking {
    println("Starting database connection test...")

    try {
        // Test connection by getting the database
        val database = MongoClientProvider.client.getDatabase("GeoMetricsDB")
        println("✅ Successfully connected to MongoDB database: ${database.name}")

        // Test flood collection
        val floodCount = Database.floodCollection.countDocuments()
        println("✅ Flood collection access OK - Contains $floodCount documents")

        // Test landslide collection
        val landslideCount = Database.landSlideCollection.countDocuments()
        println("✅ Landslide collection access OK - Contains $landslideCount documents")

        // Try to fetch some documents (if any)
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

        println("\nDatabase connection test completed successfully!")
    } catch (e: Exception) {
        println("❌ Database connection test failed: ${e.message}")
        e.printStackTrace()
    } finally {
        // Close the client connection when done
        MongoClientProvider.client.close()
        println("Connection closed.")
    }
}