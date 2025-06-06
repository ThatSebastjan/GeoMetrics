package db

import DbCheckResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import models.*
import api.ApiClient

suspend fun checkDatabaseUpToDate(): DbCheckResult {
    return withContext(Dispatchers.IO) {
        try {
            var status = "Database is "
            var isUpToDate = true

            val missingFireStations = checkFireStationsApi()
            if (missingFireStations.isNotEmpty()) {
                status += "missing ${missingFireStations.size} fire stations. "
                isUpToDate = false
            }

            val missingEarthquakes = checkEarthquakesApi()
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

private suspend fun checkFireStationsApi(): List<FireStationProperties> {
    val scraper = Scraper()
    val scrapedStations = scraper.scrapeFireStations()
    val dbStations = ApiClient.get<FireStation>(0, ApiClient.count<FireStation>())

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

private suspend fun checkEarthquakesApi(): List<EarthquakeProperties> {
    val dbEarthquakes = ApiClient.get<Earthquake>(0, ApiClient.count<Earthquake>())
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
            val dbStations = ApiClient.get<FireStation>(0, ApiClient.count<FireStation>())

            var maxId = dbStations.maxOfOrNull { it.id ?: 0 } ?: 0

            for (scraped in scrapedStations) {
                val exists = dbStations.any {
                    it.properties.location == scraped.location &&
                            it.properties.address == scraped.address
                }

                if (!exists) {
                    maxId++
                    val fireStation = FireStation(
                        type = "Feature",
                        id = maxId,
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
                    if (ApiClient.insert(fireStation)) {
                        addedFireStations++
                    }
                }
            }

            val dbEarthquakes = ApiClient.get<Earthquake>(0, ApiClient.count<Earthquake>())
            val latestTimestamp = dbEarthquakes
                .maxByOrNull { it.properties.timestamp.toEpochMilliseconds() }
                ?.properties?.timestamp?.toEpochMilliseconds() ?: 0

            val scrapedEarthquakes = scraper.scrapeEarthQuakes(latestTimestamp)

            var maxEarthquakeId = dbEarthquakes.maxOfOrNull { it.id ?: 0 } ?: 0

            for (scraped in scrapedEarthquakes) {
                maxEarthquakeId++
                val earthquake = Earthquake(
                    type = "Feature",
                    id = maxEarthquakeId,
                    geometry = GeoJsonPoint(
                        coordinates = arrayListOf(scraped.longitude, scraped.latitude)
                    ),
                    properties = EarthquakeProperties(
                        timestamp = Instant.fromEpochMilliseconds(scraped.timestamp.toLong()),
                        magnitude = scraped.magnitude,
                        depth = scraped.depth.toDouble()
                    )
                )
                if (ApiClient.insert(earthquake)) {
                    addedEarthquakes++
                }
            }

            "Database updated: Added $addedFireStations fire stations and $addedEarthquakes earthquakes"
        } catch (e: Exception) {
            "Error updating database: ${e.message}"
        }
    }
}