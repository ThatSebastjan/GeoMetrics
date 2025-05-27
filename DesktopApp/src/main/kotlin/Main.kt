import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.Alignment
import androidx.compose.foundation.border
import kotlin.random.Random
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toList
import db.Database
import models.FireStation
import models.FireStationProperties
import models.GeoJsonPoint
import models.Earthquake
import models.EarthquakeProperties
import java.time.Instant
import models.Scraper

// Function to check if DB is up to date compared to scraped data
suspend fun checkDatabaseUpToDate(): String {
    return withContext(Dispatchers.IO) {
        try {
            var status = "Database is "
            var isUpToDate = true

            // Check fire stations
            val fireStationsMissingCount = checkFireStations()
            if (fireStationsMissingCount > 0) {
                status += "missing $fireStationsMissingCount fire stations. "
                isUpToDate = false
            }

            // Check earthquakes
            val earthquakesMissingCount = checkEarthquakes()
            if (earthquakesMissingCount > 0) {
                status += "missing $earthquakesMissingCount earthquakes. "
                isUpToDate = false
            }

            if (isUpToDate) {
                status = "Database is up to date! All fire stations and earthquakes are present."
            } else {
                status += "Click 'Update DB' to add missing data."
            }

            status
        } catch (e: Exception) {
            "Error checking database: ${e.message}"
        }
    }
}

// Check fire stations in database vs scraped
private suspend fun checkFireStations(): Int {
    val scraper = Scraper()
    val scrapedStations = scraper.scrapeFireStations()
    val dbStations = Database.fireStationCollection.find().toList()

    var missingCount = 0

    for (scraped in scrapedStations) {
        val exists = dbStations.any {
            it.properties.location == scraped.location &&
                    it.properties.address == scraped.address
        }
        if (!exists) missingCount++
    }

    return missingCount
}

// Check earthquakes in database vs scraped
private suspend fun checkEarthquakes(): Int {
    val dbEarthquakes = Database.earthquakeCollection.find().toList()

    // Find the newest earthquake timestamp in the database
    val latestTimestamp = dbEarthquakes
        .maxByOrNull { it.properties.timestamp.toEpochMilli() }
        ?.properties?.timestamp?.toEpochMilli() ?: 0

    val scraper = Scraper()
    val scrapedEarthquakes = scraper.scrapeEarthQuakes(latestTimestamp)

    return scrapedEarthquakes.size
}

// Update database with missing data
suspend fun updateDatabase(): String {
    return withContext(Dispatchers.IO) {
        try {
            var addedFireStations = 0
            var addedEarthquakes = 0
            val scraper = Scraper()

            // Update fire stations
            val scrapedStations = scraper.scrapeFireStations()
            val dbStations = Database.fireStationCollection.find().toList()

            for (scraped in scrapedStations) {
                val exists = dbStations.any {
                    it.properties.location == scraped.location &&
                            it.properties.address == scraped.address
                }

                if (!exists) {
                    // Convert scraped FireStation to DB FireStation model
                    val fireStation = FireStation(
                        type = "Feature",
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

            // Update earthquakes - find latest timestamp from database
            val dbEarthquakes = Database.earthquakeCollection.find().toList()
            val latestTimestamp = dbEarthquakes
                .maxByOrNull { it.properties.timestamp.toEpochMilli() }
                ?.properties?.timestamp?.toEpochMilli() ?: 0

            val scrapedEarthquakes = scraper.scrapeEarthQuakes(latestTimestamp)

            for (scraped in scrapedEarthquakes) {
                val earthquake = Earthquake(
                    type = "Feature",
                    geometry = GeoJsonPoint(
                        coordinates = arrayListOf(scraped.longitude, scraped.latitude)
                    ),
                    properties = EarthquakeProperties(
                        timestamp = Instant.ofEpochMilli(scraped.timestamp.toLong()),
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
// Model for location with risk levels
data class LocationRisk(
    val id: Int,
    val latitude: Double,
    val longitude: Double,
    var landslideRisk: Int,
    var floodRisk: Int,
    var earthquakeRisk: Int
)

@Composable
fun DataManagerTab(
    locations: List<LocationRisk>,
    dbStatus: String,
    isLoading: Boolean,
    onCheckDb: () -> Unit,
    onUpdateDb: () -> Unit
) {
    Column(Modifier.padding(16.dp)) {
        Text("Data Management", style = MaterialTheme.typography.h6)
        Row(Modifier.padding(vertical = 8.dp)) {
            Button(onClick = onCheckDb, enabled = !isLoading) {
                Text("Check if DB is up to date")
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = onUpdateDb, enabled = !isLoading) {
                Text("Update DB")
            }
        }
        Spacer(Modifier.height(8.dp))
        if (isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Working on it...")
            }
        } else {
            Text("DB Status: $dbStatus")
        }
        Spacer(Modifier.height(8.dp))
        Text("Current locations in DB:")
        LazyColumn {
            items(locations) { loc ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Lat: %.4f, Lon: %.4f | Landslide: %d | Flood: %d | Earthquake: %d".format(
                        loc.latitude, loc.longitude, loc.landslideRisk, loc.floodRisk, loc.earthquakeRisk
                    ))
                }
            }
        }
    }
}

@Composable
fun EditableLocationRiskRow(
    location: LocationRisk,
    onUpdate: (LocationRisk) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var landslideRisk by remember { mutableStateOf(location.landslideRisk.toString()) }
    var floodRisk by remember { mutableStateOf(location.floodRisk.toString()) }
    var earthquakeRisk by remember { mutableStateOf(location.earthquakeRisk.toString()) }

    fun validRisk(value: String) = value.toIntOrNull()?.let { it in 1..100 } ?: false

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Lat: %.4f, Lon: %.4f |".format(location.latitude, location.longitude))
        TextField(
            value = landslideRisk,
            onValueChange = {
                if (it.length <= 3 && (it.isEmpty() || validRisk(it))) {
                    landslideRisk = it
                    if (validRisk(it) && validRisk(floodRisk) && validRisk(earthquakeRisk)) {
                        onUpdate(location.copy(
                            landslideRisk = it.toInt(),
                            floodRisk = floodRisk.toInt(),
                            earthquakeRisk = earthquakeRisk.toInt()
                        ))
                    }
                }
            },
            label = { Text("Landslide") },
            modifier = Modifier.width(100.dp),
            isError = landslideRisk.isNotEmpty() && !validRisk(landslideRisk)
        )
        TextField(
            value = floodRisk,
            onValueChange = {
                if (it.length <= 3 && (it.isEmpty() || validRisk(it))) {
                    floodRisk = it
                    if (validRisk(landslideRisk) && validRisk(it) && validRisk(earthquakeRisk)) {
                        onUpdate(location.copy(
                            landslideRisk = landslideRisk.toInt(),
                            floodRisk = it.toInt(),
                            earthquakeRisk = earthquakeRisk.toInt()
                        ))
                    }
                }
            },
            label = { Text("Flood") },
            modifier = Modifier.width(100.dp),
            isError = floodRisk.isNotEmpty() && !validRisk(floodRisk)
        )
        TextField(
            value = earthquakeRisk,
            onValueChange = {
                if (it.length <= 3 && (it.isEmpty() || validRisk(it))) {
                    earthquakeRisk = it
                    if (validRisk(landslideRisk) && validRisk(floodRisk) && validRisk(it)) {
                        onUpdate(location.copy(
                            landslideRisk = landslideRisk.toInt(),
                            floodRisk = floodRisk.toInt(),
                            earthquakeRisk = it.toInt()
                        ))
                    }
                }
            },
            label = { Text("Earthquake") },
            modifier = Modifier.width(100.dp),
            isError = earthquakeRisk.isNotEmpty() && !validRisk(earthquakeRisk)
        )
        if (onDelete != null) {
            Button(onClick = onDelete, modifier = Modifier.padding(start = 8.dp)) { Text("Delete") }
        }
    }
}

@Composable
fun GeneratorTab(
    locations: List<LocationRisk>,
    onAdd: (LocationRisk) -> Unit,
    onDelete: (LocationRisk) -> Unit,
    generatedData: List<LocationRisk>,
    onGenerate: (List<LocationRisk>) -> Unit,
    onUpdate: (LocationRisk) -> Unit,
    onDeleteGenerated: (LocationRisk) -> Unit,
    onSend: (List<LocationRisk>) -> Unit
) {
    var landslideRisk by remember { mutableStateOf("") }
    var floodRisk by remember { mutableStateOf("") }
    var earthquakeRisk by remember { mutableStateOf("") }
    var count by remember { mutableStateOf("5") }
    var error by remember { mutableStateOf(false) }

    fun validRisk(value: String) = value.toIntOrNull()?.let { it in 1..100 } ?: false

    Column(Modifier.padding(16.dp)) {
        Text("Data Generator", style = MaterialTheme.typography.h6)
        Row(Modifier.padding(vertical = 8.dp)) {
            TextField(
                value = landslideRisk,
                onValueChange = { if (it.length <= 3 && (it.isEmpty() || validRisk(it))) landslideRisk = it },
                label = { Text("Landslide Risk (1-100)") },
                isError = landslideRisk.isNotEmpty() && !validRisk(landslideRisk)
            )
            Spacer(Modifier.width(8.dp))
            TextField(
                value = floodRisk,
                onValueChange = { if (it.length <= 3 && (it.isEmpty() || validRisk(it))) floodRisk = it },
                label = { Text("Flood Risk (1-100)") },
                isError = floodRisk.isNotEmpty() && !validRisk(floodRisk)
            )
            Spacer(Modifier.width(8.dp))
            TextField(
                value = earthquakeRisk,
                onValueChange = { if (it.length <= 3 && (it.isEmpty() || validRisk(it))) earthquakeRisk = it },
                label = { Text("Earthquake Risk (1-100)") },
                isError = earthquakeRisk.isNotEmpty() && !validRisk(earthquakeRisk)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (validRisk(landslideRisk) && validRisk(floodRisk) && validRisk(earthquakeRisk)) {
                    val lat = Random.nextDouble(45.42, 46.88)
                    val lon = Random.nextDouble(13.38, 16.61)
                    onAdd(
                        LocationRisk(
                            id = Random.nextInt(),
                            latitude = lat,
                            longitude = lon,
                            landslideRisk = landslideRisk.toInt(),
                            floodRisk = floodRisk.toInt(),
                            earthquakeRisk = earthquakeRisk.toInt()
                        )
                    )
                    landslideRisk = ""
                    floodRisk = ""
                    earthquakeRisk = ""
                }
            }, enabled = validRisk(landslideRisk) && validRisk(floodRisk) && validRisk(earthquakeRisk)) { Text("Add") }
        }
        Text("Manually added locations:")
        LazyColumn {
            items(locations) { loc ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Lat: %.4f, Lon: %.4f | Landslide: %d | Flood: %d | Earthquake: %d".format(
                        loc.latitude, loc.longitude, loc.landslideRisk, loc.floodRisk, loc.earthquakeRisk
                    ))
                    Button(onClick = { onDelete(loc) }) { Text("Delete") }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Generate random data:")
        Row {
            TextField(
                value = count,
                onValueChange = {
                    count = it
                    error = it.toIntOrNull() == null || it.toInt() <= 0
                },
                label = { Text("Count") },
                isError = error
            )
        }
        if (error) {
            Text("Please enter a valid positive integer.", color = MaterialTheme.colors.error)
        }
        Button(
            onClick = {
                val c = count.toIntOrNull() ?: 0
                if (c > 0) {
                    val generated = List(c) {
                        LocationRisk(
                            id = Random.nextInt(),
                            latitude = Random.nextDouble(45.42, 46.88),
                            longitude = Random.nextDouble(13.38, 16.61),
                            landslideRisk = Random.nextInt(1, 101),
                            floodRisk = Random.nextInt(1, 101),
                            earthquakeRisk = Random.nextInt(1, 101)
                        )
                    }
                    onGenerate(generated)
                }
            },
            Modifier.padding(top = 8.dp),
            enabled = !error && (count.toIntOrNull() ?: 0) > 0
        ) {
            Text("Generate")
        }
        Spacer(Modifier.height(8.dp))
        if (generatedData.isNotEmpty()) {
            Text("Edit generated data before sending:")
            Box(
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
                    .padding(8.dp)
            ) {
                LazyColumn {
                    items(generatedData) { loc ->
                        EditableLocationRiskRow(
                            location = loc,
                            onUpdate = onUpdate,
                            onDelete = { onDeleteGenerated(loc) }
                        )
                    }
                }
            }
            Button(
                onClick = { onSend(generatedData) },
                Modifier.padding(top = 8.dp)
            ) {
                Text("Send to DB (simulation)")
            }
        }
    }
}

@Composable
@Preview
fun App() {
    var locations by remember { mutableStateOf(listOf<LocationRisk>()) }
    var generatedData by remember { mutableStateOf(listOf<LocationRisk>()) }
    var tabIndex by remember { mutableStateOf(0) }
    var dbStatus by remember { mutableStateOf("Unknown") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    MaterialTheme {
        Column {
            TabRow(
                selectedTabIndex = tabIndex,
                modifier = Modifier.height(64.dp)
            ) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }) { Text("Management") }
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }) { Text("Generator") }
            }
            when (tabIndex) {
                0 -> DataManagerTab(
                    locations = locations,
                    dbStatus = dbStatus,
                    isLoading = isLoading,
                    onCheckDb = {
                        isLoading = true
                        scope.launch {
                            dbStatus = checkDatabaseUpToDate()
                            isLoading = false
                        }
                    },
                    onUpdateDb = {
                        isLoading = true
                        scope.launch {
                            dbStatus = updateDatabase()
                            isLoading = false
                        }
                    }
                )
                1 -> GeneratorTab(
                    locations = locations,
                    onAdd = { locations = locations + it },
                    onDelete = { locations = locations - it },
                    generatedData = generatedData,
                    onGenerate = { generatedData = it },
                    onUpdate = { updated ->
                        generatedData = generatedData.map { if (it.id == updated.id) updated else it }
                    },
                    onDeleteGenerated = { toDelete ->
                        generatedData = generatedData.filter { it.id != toDelete.id }
                    },
                    onSend = { toAdd -> locations = locations + toAdd }
                )
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}