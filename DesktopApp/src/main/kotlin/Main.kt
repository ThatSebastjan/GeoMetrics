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
import kotlinx.coroutines.*
import db.*
import kotlinx.coroutines.flow.toList
import models.Earthquake
import models.FireStationProperties
import models.EarthquakeProperties
import org.litote.kmongo.descending

data class DbCheckResult(
    val status: String,
    val missingFireStations: List<FireStationProperties>,
    val missingEarthquakes: List<EarthquakeProperties>
)

@Composable
fun DataManagerTab(
    dbStatus: String,
    isLoading: Boolean,
    onCheckDb: () -> Unit,
    onUpdateDb: () -> Unit,
    missingFireStations: List<FireStationProperties>,
    missingEarthquakes: List<EarthquakeProperties>
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
        if (missingFireStations.isNotEmpty() || missingEarthquakes.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp, max = 200.dp)
                    .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
                    .padding(8.dp)
            ) {
                LazyColumn {
                    if (missingFireStations.isNotEmpty()) {
                        item { Text("Missing Fire Stations:", style = MaterialTheme.typography.subtitle1) }
                        items(missingFireStations) { fs ->
                            Text("- ${fs.location}, ${fs.address}, ${fs.city}")
                        }
                    }
                    if (missingEarthquakes.isNotEmpty()) {
                        item { Text("Missing Earthquakes:", style = MaterialTheme.typography.subtitle1) }
                        items(missingEarthquakes) { eq ->
                            Text("- ${eq.timestamp} | M=${eq.magnitude} | D=${eq.depth}")
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun EarthquakeGeneratorTab() {
    var magnitude by remember { mutableStateOf("") }
    var depth by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.padding(16.dp)) {
        Text("Earthquake Generator", style = MaterialTheme.typography.h6)
        OutlinedTextField(
            value = magnitude,
            onValueChange = { magnitude = it },
            label = { Text("Magnitude (1.0 - 6.0)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = depth,
            onValueChange = { depth = it },
            label = { Text("Depth (1.0 - 20.0)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = longitude,
            onValueChange = { longitude = it },
            label = { Text("Longitude (13.35 - 16.60)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = latitude,
            onValueChange = { latitude = it },
            label = { Text("Latitude (45.42 - 46.88)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            scope.launch {
                try {
                    val maxEarthquakeId: Int = Database.earthquakeCollection.find()
                        .sort(descending(Earthquake::id))
                        .limit(1)
                        .toList()
                        .firstOrNull()?.id ?: 0

                    val eq = models.Earthquake(
                        type = "Feature",
                        id = maxEarthquakeId + 1,
                        geometry = models.GeoJsonPoint(
                            coordinates = arrayListOf(longitude.toDouble(), latitude.toDouble())
                        ),
                        properties = models.EarthquakeProperties(
                            timestamp = java.time.Instant.now(),
                            magnitude = magnitude.toDouble(),
                            depth = depth.toDouble()
                        )
                    )
                    db.Database.earthquakeCollection.insertOne(eq)
                    status = "✅ Earthquake inserted!"
                } catch (e: Exception) {
                    status = "❌ Error: ${e.message}"
                }
            }
        }) {
            Text("Generate Earthquake")
        }
        status?.let {
            Spacer(Modifier.height(8.dp))
            Text(it)
        }
    }
}

@Composable
@Preview
fun App() {
    var dbStatus by remember { mutableStateOf("Unknown") }
    var isLoading by remember { mutableStateOf(false) }
    var missingFireStations by remember { mutableStateOf(listOf<FireStationProperties>()) }
    var missingEarthquakes by remember { mutableStateOf(listOf<EarthquakeProperties>()) }
    var tabIndex by remember { mutableStateOf(0) }
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
                    dbStatus = dbStatus,
                    isLoading = isLoading,
                    onCheckDb = {
                        isLoading = true
                        scope.launch {
                            val result = checkDatabaseUpToDate()
                            dbStatus = result.status
                            missingFireStations = result.missingFireStations
                            missingEarthquakes = result.missingEarthquakes
                            isLoading = false
                        }
                    },
                    onUpdateDb = {
                        isLoading = true
                        scope.launch {
                            dbStatus = updateDatabase()
                            isLoading = false
                        }
                    },
                    missingFireStations = missingFireStations,
                    missingEarthquakes = missingEarthquakes
                )
                1 -> EarthquakeGeneratorTab()
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}