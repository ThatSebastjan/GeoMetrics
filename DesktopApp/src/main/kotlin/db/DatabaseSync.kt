package db

import DbCheckResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import models.Earthquake
import models.EarthquakeProperties
import models.FireStation
import models.FireStationProperties
import models.GeoJsonPoint
import models.Scraper
import org.litote.kmongo.descending

suspend fun checkDatabaseUpToDate(): DbCheckResult {
    return withContext(Dispatchers.IO) {
        try {
            var status = "Database is "
            var isUpToDate = true

            val missingFireStations = checkFireStations()
            if (missingFireStations.isNotEmpty()) {
                status += "missing ${missingFireStations.size} fire stations. "
                isUpToDate = false
            }

            val missingEarthquakes = checkEarthquakes()
            if (missingEarthquakes.isNotEmpty()) {
                status += "missing ${missingEarthquakes.size} earthquakes. "
                isUpToDate = false
            }

            if (isUpToDate) {
                status = "Database is up to date! All fire stations and earthquakes are present."
            } else {
                status += "Click 'Update DB' to add missing data."
            }

            DbCheckResult(status, missingFireStations, missingEarthquakes)
        } catch (e: Exception) {
            DbCheckResult("Error checking database: ${e.message}", emptyList(), emptyList())
        }
    }
}

private suspend fun checkFireStations(): List<FireStationProperties> {
    val scraper = Scraper()
    val scrapedStations = scraper.scrapeFireStations()
    val dbStations = Database.fireStationCollection.find().toList()

    return scrapedStations.filter { scraped ->
        dbStations.none {
            it.properties.location == scraped.location &&
                    it.properties.address == scraped.address
        }
    }.map {
        FireStationProperties(
            location = it.location,
            address = it.address,
            city = it.city,
            description = it.description,
            telephoneNumber = it.telephoneNum
        )
    }
}

private suspend fun checkEarthquakes(): List<EarthquakeProperties> {
    val dbEarthquakes = Database.earthquakeCollection.find().toList()
    val latestTimestamp = dbEarthquakes
        .maxByOrNull { it.properties.timestamp.toEpochMilliseconds() }
        ?.properties?.timestamp?.toEpochMilliseconds() ?: 0

    val scraper = Scraper()
    val scrapedEarthquakes = scraper.scrapeEarthQuakes(latestTimestamp)

    return scrapedEarthquakes.map {
        EarthquakeProperties(
            timestamp = Instant.fromEpochMilliseconds(it.timestamp.toLong()),
            magnitude = it.magnitude,
            depth = it.depth.toDouble()
        )
    }
}

suspend fun updateDatabase(): String {
    return withContext(Dispatchers.IO) {
        try {
            var addedFireStations = 0
            var addedEarthquakes = 0
            val scraper = Scraper()

            val scrapedStations = scraper.scrapeFireStations()
            val dbStations = Database.fireStationCollection.find().toList()

            for (scraped in scrapedStations) {
                val exists = dbStations.any {
                    it.properties.location == scraped.location &&
                            it.properties.address == scraped.address
                }

                if (!exists) {
                    val maxId = Database.fireStationCollection.find()
                        .sort(descending(FireStation::id))
                        .limit(1)
                        .toList()
                        .firstOrNull()?.id ?: 0

                    val fireStation = FireStation(
                        type = "Feature",
                        id = maxId + 1,
                        geometry = GeoJsonPoint(
                            coordinates = arrayListOf(scraped.longitude, scraped.latitude)
                        ),
                        properties = FireStationProperties(
                            location = scraped.location,
                            address = scraped.address,
                            city = scraped.city,
                            description = scraped.description,
                            telephoneNumber = scraped.telephoneNum
                        )
                    )
                    Database.fireStationCollection.insertOne(fireStation)
                    addedFireStations++
                }
            }

            val dbEarthquakes = Database.earthquakeCollection.find().toList()
            val latestTimestamp = dbEarthquakes
                .maxByOrNull { it.properties.timestamp.toEpochMilliseconds() }
                ?.properties?.timestamp?.toEpochMilliseconds() ?: 0

            val scrapedEarthquakes = scraper.scrapeEarthQuakes(latestTimestamp)

            val maxEarthquakeId = Database.earthquakeCollection.find()
                .sort(descending(Earthquake::id))
                .limit(1)
                .toList()
                .firstOrNull()?.id ?: 0

            var nextEarthquakeId = maxEarthquakeId + 1

            for (scraped in scrapedEarthquakes) {
                val earthquake = Earthquake(
                    type = "Feature",
                    id = nextEarthquakeId++,
                    geometry = GeoJsonPoint(
                        coordinates = arrayListOf(scraped.longitude, scraped.latitude)
                    ),
                    properties = EarthquakeProperties(
                        timestamp = Instant.fromEpochMilliseconds(scraped.timestamp.toLong()),
                        magnitude = scraped.magnitude,
                        depth = scraped.depth.toDouble()
                    )
                )
                Database.earthquakeCollection.insertOne(earthquake)
                addedEarthquakes++
            }

            "Database updated: Added $addedFireStations fire stations and $addedEarthquakes earthquakes"
        } catch (e: Exception) {
            "Error updating database: ${e.message}"
        }
    }
}