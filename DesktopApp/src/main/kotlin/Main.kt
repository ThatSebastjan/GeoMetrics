import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import api.ApiClient
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import models.*
import db.checkDatabaseUpToDate
import db.updateDatabase

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
    val buttonWidth = 220.dp
    val buttonHeight = 52.dp

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Data Management",
            style = MaterialTheme.typography.h6,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Box(
            Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = onCheckDb,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black, contentColor = Color.White),
                    modifier = Modifier
                        .width(buttonWidth)
                        .height(buttonHeight)
                ) {
                    Text("Check DB", fontSize =  16.sp)
                }
                Button(
                    onClick = onUpdateDb,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black, contentColor = Color.White),
                    modifier = Modifier
                        .width(buttonWidth)
                        .height(buttonHeight)
                ) {
                    Text("Update DB", fontSize =  16.sp)
                }
            }
        }

        if (isLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Working on it...")
            }
        } else {
            Text("DB Status: $dbStatus", modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        if (missingFireStations.isNotEmpty() || missingEarthquakes.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
                    .padding(12.dp)
            ) {
                val listState = rememberLazyListState()
                LazyColumn(state = listState) {
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
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun UniversalGeneratorTab() {
    val types = listOf("Earthquake", "Fire Station", "Landslide", "Flood")
    var selectedType by remember { mutableStateOf(types[0]) }
    var expanded by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    var longitude by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }

    var fsLocation by remember { mutableStateOf("") }
    var fsAddress by remember { mutableStateOf("") }
    var fsCity by remember { mutableStateOf("") }
    var fsDescription by remember { mutableStateOf("") }
    var fsTelephone by remember { mutableStateOf("") }

    var eqMagnitude by remember { mutableStateOf("") }
    var eqDepth by remember { mutableStateOf("") }

    var polygonText by remember { mutableStateOf("") }

    var typeInt by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Universal Generator", style = MaterialTheme.typography.h6)

        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text(selectedType)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                types.forEach { type ->
                    DropdownMenuItem(onClick = {
                        selectedType = type
                        expanded = false
                    }) { Text(type) }
                }
            }
        }

        when (selectedType) {
            "Earthquake" -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = eqMagnitude, onValueChange = { eqMagnitude = it }, label = { Text("Magnitude") })
                        OutlinedTextField(value = longitude, onValueChange = { longitude = it }, label = { Text("Longitude") })
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = eqDepth, onValueChange = { eqDepth = it }, label = { Text("Depth") })
                        OutlinedTextField(value = latitude, onValueChange = { latitude = it }, label = { Text("Latitude") })
                    }
                }
            }
            "Fire Station" -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = fsLocation, onValueChange = { fsLocation = it }, label = { Text("Location") })
                        OutlinedTextField(value = fsAddress, onValueChange = { fsAddress = it }, label = { Text("Address") })
                        OutlinedTextField(value = fsCity, onValueChange = { fsCity = it }, label = { Text("City") })
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = fsDescription, onValueChange = { fsDescription = it }, label = { Text("Description") })
                        OutlinedTextField(value = fsTelephone, onValueChange = { fsTelephone = it }, label = { Text("Telephone") })
                        OutlinedTextField(value = longitude, onValueChange = { longitude = it }, label = { Text("Longitude") })
                        OutlinedTextField(value = latitude, onValueChange = { latitude = it }, label = { Text("Latitude") })
                    }
                }
            }
            "Landslide", "Flood" -> {
                OutlinedTextField(
                    modifier = Modifier.width(500.dp),
                    value = polygonText,
                    onValueChange = { polygonText = it },
                    label = { Text("Polygon coordinates (lon,lat;lon,lat;...)") }
                )
                OutlinedTextField(
                    modifier = Modifier.width(500.dp),
                    value = typeInt,
                    onValueChange = { typeInt = it },
                    label = { Text(if (selectedType == "Landslide") "LandSlideType" else "FloodType") }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Button(
                onClick = {
                    scope.launch {
                        try {
                            when (selectedType) {
                                "Earthquake" -> {
                                    val maxId = ApiClient.get<Earthquake>(0, 1).maxOfOrNull { it.id ?: 0 } ?: 0
                                    val eq = Earthquake(
                                        type = "Feature",
                                        id = maxId + 1,
                                        geometry = GeoJsonPoint(coordinates = arrayListOf(longitude.toDouble(), latitude.toDouble())),
                                        properties = EarthquakeProperties(
                                            timestamp = Clock.System.now(),
                                            magnitude = eqMagnitude.toDouble(),
                                            depth = eqDepth.toDouble()
                                        )
                                    )
                                    val ok = ApiClient.insert(eq)
                                    status = if (ok) "✅ Earthquake inserted!" else "❌ Insert failed"
                                }
                                "Fire Station" -> {
                                    val maxId = ApiClient.get<FireStation>(0, 1).maxOfOrNull { it.id ?: 0 } ?: 0
                                    val fs = FireStation(
                                        type = "Feature",
                                        id = maxId + 1,
                                        geometry = GeoJsonPoint(coordinates = arrayListOf(longitude.toDouble(), latitude.toDouble())),
                                        properties = FireStationProperties(
                                            location = fsLocation,
                                            address = fsAddress,
                                            city = fsCity,
                                            description = fsDescription,
                                            telephoneNumber = fsTelephone
                                        )
                                    )
                                    val ok = ApiClient.insert(fs)
                                    status = if (ok) "✅ Fire Station inserted!" else "❌ Insert failed"
                                }
                                "Landslide" -> {
                                    val coords = polygonText.split(";").map {
                                        val (lon, lat) = it.split(",").map(String::toDouble)
                                        arrayListOf(lon, lat)
                                    }
                                    val polygon = arrayListOf(coords as ArrayList<ArrayList<Double>>)
                                    val landslides = ApiClient.get<LandSlide>(0, 1)
                                    val maxObjectId = landslides.maxOfOrNull { it.properties.OBJECTID } ?: 0
                                    val landslide = LandSlide(
                                        type = "Feature",
                                        id = maxObjectId + 1,
                                        geometry = GeoJsonPolygon(coordinates = polygon),
                                        properties = LandSlideProperties(
                                            OBJECTID = maxObjectId + 1,
                                            LandSlideType = typeInt.toInt()
                                        )
                                    )
                                    val ok = ApiClient.insert(landslide)
                                    status = if (ok) "✅ Landslide inserted!" else "❌ Insert failed"
                                }
                                "Flood" -> {
                                    val coords = polygonText.split(";").map {
                                        val (lon, lat) = it.split(",").map(String::toDouble)
                                        arrayListOf(lon, lat)
                                    }
                                    val polygon = arrayListOf(coords as ArrayList<ArrayList<Double>>)
                                    val floods = ApiClient.get<Flood>(0, 1)
                                    val maxObjectId = floods.maxOfOrNull { it.properties.OBJECTID } ?: 0
                                    val flood = Flood(
                                        type = "Feature",
                                        id = maxObjectId + 1,
                                        geometry = GeoJsonPolygon(coordinates = polygon),
                                        properties = FloodProperties(
                                            OBJECTID = maxObjectId + 1,
                                            FloodType = typeInt.toInt()
                                        )
                                    )
                                    val ok = ApiClient.insert(flood)
                                    status = if (ok) "✅ Flood inserted!" else "❌ Insert failed"
                                }
                            }
                        } catch (e: Exception) {
                            status = "❌ Error: ${e.message}"
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black, contentColor = Color.White)
            ) {
                Text("Generate")
            }
        }

        status?.let { Text(it) }
    }
}

@Composable
fun ScraperTab() {
    val scope = rememberCoroutineScope()
    var isRunning by remember { mutableStateOf(false) }
    var progressMessages by remember { mutableStateOf(listOf<String>()) }
    val listState = rememberLazyListState()

    LaunchedEffect(progressMessages.size) {
        if (progressMessages.isNotEmpty()) {
            listState.animateScrollToItem(progressMessages.lastIndex)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Scraper Progress", style = MaterialTheme.typography.h6, modifier = Modifier.align(Alignment.CenterHorizontally))

        Button(
            modifier =  Modifier.height(48.dp).width(250.dp).align(Alignment.CenterHorizontally),
            onClick = {
                isRunning = true
                progressMessages = listOf("Starting scraper...")
                scope.launch {
                    try {
                        val scraper = Scraper()
                        progressMessages = progressMessages + "Scraping fire stations..."
                        val fireStations = scraper.scrapeFireStations()
                        progressMessages = progressMessages + "Found ${fireStations.size} fire stations."

                        progressMessages = progressMessages + "Scraping earthquakes..."
                        val earthquakes = scraper.scrapeEarthQuakes(0) { batchIndex, totalBatches ->
                            progressMessages = progressMessages + "Processed batch $batchIndex of $totalBatches"
                        }
                        progressMessages = progressMessages + "Found ${earthquakes.size} earthquakes."
                        progressMessages = progressMessages + "Scraping complete!"
                    } catch (e: Exception) {
                        progressMessages = progressMessages + "Error: ${e.message}"
                    } finally {
                        isRunning = false
                    }
                }
            },
            enabled = !isRunning,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black, contentColor = Color.White)
        ) {
            Text("Run Scraper")
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
                .padding(12.dp)
        ) {
            LazyColumn(state = listState) {
                items(progressMessages) { msg ->
                    Text(msg)
                }
            }
        }
    }
}

@Composable
fun DatabaseViewerTab() {
    var subTabIndex by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf(0) }
    var totalCount by remember { mutableStateOf(0) }
    var earthquakes by remember { mutableStateOf<List<Earthquake>>(emptyList()) }
    var fireStations by remember { mutableStateOf<List<FireStation>>(emptyList()) }
    var floods by remember { mutableStateOf<List<Flood>>(emptyList()) }
    var landslides by remember { mutableStateOf<List<LandSlide>>(emptyList()) }
    var landLots by remember { mutableStateOf<List<LandLot>>(emptyList()) }
    var landUses by remember { mutableStateOf<List<LandUse>>(emptyList()) }
    val pageSize = 10
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        TabRow(
            selectedTabIndex = subTabIndex,
            backgroundColor = Color.Black,
            contentColor = Color.White
        ) {
            Tab(selected = subTabIndex == 0, onClick = { subTabIndex = 0; page = 0 }) { Text("Earthquakes") }
            Tab(selected = subTabIndex == 1, onClick = { subTabIndex = 1; page = 0 }) { Text("Fire Stations") }
            Tab(selected = subTabIndex == 2, onClick = { subTabIndex = 2; page = 0 }) { Text("Floods") }
            Tab(selected = subTabIndex == 3, onClick = { subTabIndex = 3; page = 0 }) { Text("Landslides") }
            Tab(selected = subTabIndex == 4, onClick = { subTabIndex = 4; page = 0 }) { Text("Land Lots") }
            Tab(selected = subTabIndex == 5, onClick = { subTabIndex = 5; page = 0 }) { Text("Land Uses") }
        }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.weight(1f)) {
            when (subTabIndex) {
                0 -> {
                    totalCount = ApiClient.count<Earthquake>()
                    earthquakes = ApiClient.get<Earthquake>(page * pageSize, pageSize)
                    var editingId by remember { mutableStateOf<Int?>(null) }
                    var editedMagnitude by remember { mutableStateOf("") }
                    var editedDepth by remember { mutableStateOf("") }
                    var editedTimestamp by remember { mutableStateOf("") }
                    var editedLongitude by remember { mutableStateOf("") }
                    var editedLatitude by remember { mutableStateOf("") }
                    LazyColumn {
                        items(earthquakes) { eq ->
                            if (editingId == eq.id) {
                                OutlinedTextField(
                                    value = editedMagnitude,
                                    onValueChange = { editedMagnitude = it },
                                    label = { Text("Magnitude") }
                                )
                                OutlinedTextField(
                                    value = editedDepth,
                                    onValueChange = { editedDepth = it },
                                    label = { Text("Depth") }
                                )
                                OutlinedTextField(
                                    value = editedTimestamp,
                                    onValueChange = { editedTimestamp = it },
                                    label = { Text("Timestamp (ISO 8601)") }
                                )
                                OutlinedTextField(
                                    value = editedLongitude,
                                    onValueChange = { editedLongitude = it },
                                    label = { Text("Longitude") }
                                )
                                OutlinedTextField(
                                    value = editedLatitude,
                                    onValueChange = { editedLatitude = it },
                                    label = { Text("Latitude") }
                                )
                                Row {
                                    Button(colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black, contentColor = Color.White), onClick = {
                                        scope.launch {
                                            val updated = eq.copy(
                                                properties = eq.properties.copy(
                                                    magnitude = editedMagnitude.toDoubleOrNull()
                                                        ?: eq.properties.magnitude,
                                                    depth = editedDepth.toDoubleOrNull() ?: eq.properties.depth,
                                                    timestamp = kotlinx.datetime.Instant.parse(editedTimestamp)
                                                ),
                                                geometry = eq.geometry.copy(
                                                    coordinates = arrayListOf(
                                                        editedLongitude.toDoubleOrNull()
                                                            ?: eq.geometry.coordinates.getOrNull(0) ?: 0.0,
                                                        editedLatitude.toDoubleOrNull()
                                                            ?: eq.geometry.coordinates.getOrNull(1) ?: 0.0
                                                    )
                                                )
                                            )
                                            if (ApiClient.update(updated)) {
                                                earthquakes = earthquakes.map { if (it.id == eq.id) updated else it }
                                            }
                                            editingId = null
                                        }
                                    }) { Text("Save") }
                                    Spacer(Modifier.width(8.dp))
                                    Button(colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red, contentColor = Color.White), onClick = { editingId = null }) { Text("Cancel") }
                                }
                            } else {
                                Text("ID: ${eq.id}, Mag: ${eq.properties.magnitude}, Depth: ${eq.properties.depth}, Time: ${eq.properties.timestamp}")
                                Button(colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black, contentColor = Color.White), onClick = {
                                    editingId = eq.id
                                    editedMagnitude = eq.properties.magnitude.toString()
                                    editedDepth = eq.properties.depth.toString()
                                    editedTimestamp = eq.properties.timestamp.toString()
                                    editedLongitude = eq.geometry.coordinates.getOrNull(0)?.toString() ?: ""
                                    editedLatitude = eq.geometry.coordinates.getOrNull(1)?.toString() ?: ""
                                }) { Text("Edit") }
                            }
                            Divider()
                        }
                    }
                }
                1 -> {
                    totalCount = ApiClient.count<FireStation>()
                    fireStations = ApiClient.get<FireStation>(page * pageSize, pageSize)
                    var editingId by remember { mutableStateOf<Int?>(null) }
                    var editedLocation by remember { mutableStateOf("") }
                    var editedAddress by remember { mutableStateOf("") }
                    var editedCity by remember { mutableStateOf("") }
                    var editedDescription by remember { mutableStateOf("") }
                    var editedTelephone by remember { mutableStateOf("") }
                    var editedLongitude by remember { mutableStateOf("") }
                    var editedLatitude by remember { mutableStateOf("") }

                    LazyColumn {
                        items(fireStations) { fs ->
                            if (editingId == fs.id) {
                                OutlinedTextField(
                                    value = editedLocation,
                                    onValueChange = { editedLocation = it },
                                    label = { Text("Location") }
                                )
                                OutlinedTextField(
                                    value = editedAddress,
                                    onValueChange = { editedAddress = it },
                                    label = { Text("Address") }
                                )
                                OutlinedTextField(
                                    value = editedCity,
                                    onValueChange = { editedCity = it },
                                    label = { Text("City") }
                                )
                                OutlinedTextField(
                                    value = editedDescription,
                                    onValueChange = { editedDescription = it },
                                    label = { Text("Description") }
                                )
                                OutlinedTextField(
                                    value = editedTelephone,
                                    onValueChange = { editedTelephone = it },
                                    label = { Text("Telephone") }
                                )
                                OutlinedTextField(
                                    value = editedLongitude,
                                    onValueChange = { editedLongitude = it },
                                    label = { Text("Longitude") }
                                )
                                OutlinedTextField(
                                    value = editedLatitude,
                                    onValueChange = { editedLatitude = it },
                                    label = { Text("Latitude") }
                                )
                                Row {
                                    Button(colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black, contentColor = Color.White), onClick = {
                                        scope.launch {
                                            val updated = fs.copy(
                                                properties = fs.properties.copy(
                                                    location = editedLocation,
                                                    address = editedAddress,
                                                    city = editedCity,
                                                    description = editedDescription,
                                                    telephoneNumber = editedTelephone
                                                ),
                                                geometry = fs.geometry.copy(
                                                    coordinates = arrayListOf(
                                                        editedLongitude.toDoubleOrNull()
                                                            ?: fs.geometry.coordinates.getOrNull(0) ?: 0.0,
                                                        editedLatitude.toDoubleOrNull()
                                                            ?: fs.geometry.coordinates.getOrNull(1) ?: 0.0
                                                    )
                                                )
                                            )
                                            if (ApiClient.update(updated)) {
                                                fireStations = fireStations.map { if (it.id == fs.id) updated else it }
                                            }
                                            editingId = null
                                        }
                                    }) { Text("Save") }
                                    Spacer(Modifier.width(8.dp))
                                    Button(colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red, contentColor = Color.White), onClick = { editingId = null }) { Text("Cancel") }
                                }
                            } else {
                                Text("ID: ${fs.id}, Location: ${fs.properties.location}, Address: ${fs.properties.address}, City: ${fs.properties.city}, Desc: ${fs.properties.description}, Tel: ${fs.properties.telephoneNumber}")
                                Button(colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black, contentColor = Color.White), onClick = {
                                    editingId = fs.id
                                    editedLocation = fs.properties.location
                                    editedAddress = fs.properties.address
                                    editedCity = fs.properties.city
                                    editedDescription = fs.properties.description
                                    editedTelephone = fs.properties.telephoneNumber
                                    editedLongitude = fs.geometry.coordinates.getOrNull(0)?.toString() ?: ""
                                    editedLatitude = fs.geometry.coordinates.getOrNull(1)?.toString() ?: ""
                                }) { Text("Edit") }
                            }
                            Divider()
                        }
                    }
                }

                2 -> {
                    totalCount = ApiClient.count<Flood>()
                    floods = ApiClient.get<Flood>(page * pageSize, pageSize)
                    var editingId by remember { mutableStateOf<Int?>(null) }
                    var editedFloodType by remember { mutableStateOf("") }
                    var editedObjectId by remember { mutableStateOf("") }
                    var editedPolygonText by remember { mutableStateOf("") }

                    LazyColumn {
                        items(floods) { flood ->
                            if (editingId == flood.id) {
                                OutlinedTextField(
                                    value = editedObjectId,
                                    onValueChange = { editedObjectId = it },
                                    label = { Text("OBJECTID") }
                                )
                                OutlinedTextField(
                                    value = editedFloodType,
                                    onValueChange = { editedFloodType = it },
                                    label = { Text("FloodType") }
                                )
                                OutlinedTextField(
                                    value = editedPolygonText,
                                    onValueChange = { editedPolygonText = it },
                                    label = { Text("Polygon coordinates (lon,lat;lon,lat;...)") }
                                )
                                Row {
                                    Button(colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black, contentColor = Color.White), onClick = {
                                        scope.launch {
                                            val coords = editedPolygonText.split(";").mapNotNull {
                                                val parts = it.split(",")
                                                if (parts.size == 2) {
                                                    val lon = parts[0].toDoubleOrNull()
                                                    val lat = parts[1].toDoubleOrNull()
                                                    if (lon != null && lat != null) arrayListOf(lon, lat) else null
                                                } else null
                                            }
                                            val polygon = arrayListOf(coords as ArrayList<ArrayList<Double>>)
                                            val updated = flood.copy(
                                                properties = flood.properties.copy(
                                                    OBJECTID = editedObjectId.toIntOrNull()
                                                        ?: flood.properties.OBJECTID,
                                                    FloodType = editedFloodType.toIntOrNull()
                                                        ?: flood.properties.FloodType
                                                ),
                                                geometry = flood.geometry.copy(
                                                    coordinates = polygon
                                                )
                                            )
                                            if (ApiClient.update(updated)) {
                                                floods = floods.map { if (it.id == flood.id) updated else it }
                                            }
                                            editingId = null
                                        }
                                    }) { Text("Save") }
                                    Spacer(Modifier.width(8.dp))
                                    Button(colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red, contentColor = Color.White), onClick = { editingId = null }) { Text("Cancel") }
                                }
                            } else {
                                Text("ID: ${flood.id}, OBJECTID: ${flood.properties.OBJECTID}, FloodType: ${flood.properties.FloodType}")
                                Button(colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black, contentColor = Color.White), onClick = {
                                    editingId = flood.id
                                    editedObjectId = flood.properties.OBJECTID.toString()
                                    editedFloodType = flood.properties.FloodType.toString()
                                    editedPolygonText = flood.geometry.coordinates
                                        .flatMap { it }
                                        .joinToString(";") { pair -> pair.joinToString(",") }
                                }) { Text("Edit") }
                            }
                            Divider()
                        }
                    }
                }

                3 -> {
                    totalCount = ApiClient.count<LandSlide>()
                    landslides = ApiClient.get<LandSlide>(page * pageSize, pageSize)
                    var editingId by remember { mutableStateOf<Int?>(null) }
                    var editedLandSlideType by remember { mutableStateOf("") }
                    var editedObjectId by remember { mutableStateOf("") }
                    var editedPolygonText by remember { mutableStateOf("") }

                    LazyColumn {
                        items(landslides) { landslide ->
                            if (editingId == landslide.id) {
                                OutlinedTextField(
                                    value = editedObjectId,
                                    onValueChange = { editedObjectId = it },
                                    label = { Text("OBJECTID") }
                                )
                                OutlinedTextField(
                                    value = editedLandSlideType,
                                    onValueChange = { editedLandSlideType = it },
                                    label = { Text("LandSlideType") }
                                )
                                OutlinedTextField(
                                    value = editedPolygonText,
                                    onValueChange = { editedPolygonText = it },
                                    label = { Text("Polygon coordinates (lon,lat;lon,lat;...)") }
                                )
                                Row {
                                    Button(colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black, contentColor = Color.White), onClick = {
                                        scope.launch {
                                            val coords = editedPolygonText.split(";").mapNotNull {
                                                val parts = it.split(",")
                                                if (parts.size == 2) {
                                                    val lon = parts[0].toDoubleOrNull()
                                                    val lat = parts[1].toDoubleOrNull()
                                                    if (lon != null && lat != null) arrayListOf(lon, lat) else null
                                                } else null
                                            }
                                            val polygon = arrayListOf(coords as ArrayList<ArrayList<Double>>)
                                            val updated = landslide.copy(
                                                properties = landslide.properties.copy(
                                                    OBJECTID = editedObjectId.toIntOrNull()
                                                        ?: landslide.properties.OBJECTID,
                                                    LandSlideType = editedLandSlideType.toIntOrNull()
                                                        ?: landslide.properties.LandSlideType
                                                ),
                                                geometry = landslide.geometry.copy(
                                                    coordinates = polygon
                                                )
                                            )
                                            if (ApiClient.update(updated)) {
                                                landslides =
                                                    landslides.map { if (it.id == landslide.id) updated else it }
                                            }
                                            editingId = null
                                        }
                                    }) { Text("Save") }
                                    Spacer(Modifier.width(8.dp))
                                    Button(colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red, contentColor = Color.White), onClick = { editingId = null }) { Text("Cancel") }
                                }
                            } else {
                                Text("ID: ${landslide.id}, OBJECTID: ${landslide.properties.OBJECTID}, LandSlideType: ${landslide.properties.LandSlideType}")
                                Button(colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black, contentColor = Color.White), onClick = {
                                    editingId = landslide.id
                                    editedObjectId = landslide.properties.OBJECTID.toString()
                                    editedLandSlideType = landslide.properties.LandSlideType.toString()
                                    editedPolygonText = landslide.geometry.coordinates
                                        .flatMap { it }
                                        .joinToString(";") { pair -> pair.joinToString(",") }
                                }) { Text("Edit") }
                            }
                            Divider()
                        }
                    }
                }

                4 -> {
                    totalCount = ApiClient.count<LandLot>()
                    landLots = ApiClient.get<LandLot>(page * pageSize, pageSize)
                    var editingId by remember { mutableStateOf<Int?>(null) }
                    var editedSTParcele by remember { mutableStateOf("") }
                    var editedEIDParcela by remember { mutableStateOf("") }
                    var editedObjectId by remember { mutableStateOf("") }
                    var editedKOId by remember { mutableStateOf("") }
                    var editedPovrsina by remember { mutableStateOf("") }
                    var editedPolygonText by remember { mutableStateOf("") }

                    LazyColumn {
                        items(landLots) { lot ->
                            if (editingId == lot.id) {
                                OutlinedTextField(
                                    value = editedSTParcele,
                                    onValueChange = { editedSTParcele = it },
                                    label = { Text("ST_PARCELE") }
                                )
                                OutlinedTextField(
                                    value = editedEIDParcela,
                                    onValueChange = { editedEIDParcela = it },
                                    label = { Text("EID_PARCELA") }
                                )
                                OutlinedTextField(
                                    value = editedObjectId,
                                    onValueChange = { editedObjectId = it },
                                    label = { Text("OBJECTID") }
                                )
                                OutlinedTextField(
                                    value = editedKOId,
                                    onValueChange = { editedKOId = it },
                                    label = { Text("KO_ID") }
                                )
                                OutlinedTextField(
                                    value = editedPovrsina,
                                    onValueChange = { editedPovrsina = it },
                                    label = { Text("POVRSINA") }
                                )
                                OutlinedTextField(
                                    value = editedPolygonText,
                                    onValueChange = { editedPolygonText = it },
                                    label = { Text("Polygon coordinates (lon,lat;lon,lat;...)") }
                                )
                                Row {
                                    Button(colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black, contentColor = Color.White), onClick = {
                                        scope.launch {
                                            val coords = editedPolygonText.split(";").mapNotNull {
                                                val parts = it.split(",")
                                                if (parts.size == 2) {
                                                    val lon = parts[0].toDoubleOrNull()
                                                    val lat = parts[1].toDoubleOrNull()
                                                    if (lon != null && lat != null) arrayListOf(lon, lat) else null
                                                } else null
                                            }
                                            val polygon = arrayListOf(coords as ArrayList<ArrayList<Double>>)
                                            val updated = lot.copy(
                                                properties = lot.properties.copy(
                                                    ST_PARCELE = editedSTParcele,
                                                    EID_PARCELA = editedEIDParcela,
                                                    OBJECTID = editedObjectId.toIntOrNull() ?: lot.properties.OBJECTID,
                                                    KO_ID = editedKOId.toIntOrNull() ?: lot.properties.KO_ID,
                                                    POVRSINA = editedPovrsina.toIntOrNull() ?: lot.properties.POVRSINA
                                                ),
                                                geometry = lot.geometry.copy(
                                                    coordinates = polygon
                                                )
                                            )
                                            if (ApiClient.update(updated)) {
                                                landLots = landLots.map { if (it.id == lot.id) updated else it }
                                            }
                                            editingId = null
                                        }
                                    }) { Text("Save") }
                                    Spacer(Modifier.width(8.dp))
                                    Button(colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red, contentColor = Color.White), onClick = { editingId = null }) { Text("Cancel") }
                                }
                            } else {
                                Text("ID: ${lot.id}, ST_PARCELE: ${lot.properties.ST_PARCELE}, EID_PARCELA: ${lot.properties.EID_PARCELA}, OBJECTID: ${lot.properties.OBJECTID}, KO_ID: ${lot.properties.KO_ID}, POVRSINA: ${lot.properties.POVRSINA}")
                                Button(colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black, contentColor = Color.White), onClick = {
                                    editingId = lot.id
                                    editedSTParcele = lot.properties.ST_PARCELE
                                    editedEIDParcela = lot.properties.EID_PARCELA
                                    editedObjectId = lot.properties.OBJECTID.toString()
                                    editedKOId = lot.properties.KO_ID.toString()
                                    editedPovrsina = lot.properties.POVRSINA.toString()
                                    editedPolygonText = lot.geometry.coordinates
                                        .flatMap { it }
                                        .joinToString(";") { pair -> pair.joinToString(",") }
                                }) { Text("Edit") }
                            }
                            Divider()
                        }
                    }
                }

                5 -> {
                    totalCount = ApiClient.count<LandUse>()
                    landUses = ApiClient.get<LandUse>(page * pageSize, pageSize)
                    var editingId by remember { mutableStateOf<Int?>(null) }
                    var editedObjectId by remember { mutableStateOf("") }
                    var editedRabaId by remember { mutableStateOf("") }
                    var editedPolygonText by remember { mutableStateOf("") }

                    LazyColumn {
                        items(landUses) { landUse ->
                            if (editingId == landUse.id) {
                                OutlinedTextField(
                                    value = editedObjectId,
                                    onValueChange = { editedObjectId = it },
                                    label = { Text("OBJECTID") }
                                )
                                OutlinedTextField(
                                    value = editedRabaId,
                                    onValueChange = { editedRabaId = it },
                                    label = { Text("RABA_ID") }
                                )
                                OutlinedTextField(
                                    value = editedPolygonText,
                                    onValueChange = { editedPolygonText = it },
                                    label = { Text("Polygon coordinates (lon,lat;lon,lat;...)") }
                                )
                                Row {
                                    Button(colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black, contentColor = Color.White), onClick = {
                                        scope.launch {
                                            val coords = editedPolygonText.split(";").mapNotNull {
                                                val parts = it.split(",")
                                                if (parts.size == 2) {
                                                    val lon = parts[0].toDoubleOrNull()
                                                    val lat = parts[1].toDoubleOrNull()
                                                    if (lon != null && lat != null) arrayListOf(lon, lat) else null
                                                } else null
                                            }
                                            val polygon = arrayListOf(coords as ArrayList<ArrayList<Double>>)
                                            val updated = landUse.copy(
                                                properties = landUse.properties.copy(
                                                    OBJECTID = editedObjectId.toIntOrNull()
                                                        ?: landUse.properties.OBJECTID,
                                                    RABA_ID = editedRabaId.toIntOrNull() ?: landUse.properties.RABA_ID
                                                ),
                                                geometry = landUse.geometry.copy(
                                                    coordinates = polygon
                                                )
                                            )
                                            if (ApiClient.update(updated)) {
                                                landUses = landUses.map { if (it.id == landUse.id) updated else it }
                                            }
                                            editingId = null
                                        }
                                    }) { Text("Save") }
                                    Spacer(Modifier.width(8.dp))
                                    Button(colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red, contentColor = Color.White), onClick = { editingId = null }) { Text("Cancel") }
                                }
                            } else {
                                Text("ID: ${landUse.id}, OBJECTID: ${landUse.properties.OBJECTID}, RABA_ID: ${landUse.properties.RABA_ID}")
                                Button(colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black, contentColor = Color.White), onClick = {
                                    editingId = landUse.id
                                    editedObjectId = landUse.properties.OBJECTID.toString()
                                    editedRabaId = landUse.properties.RABA_ID.toString()
                                    editedPolygonText = landUse.geometry.coordinates
                                        .flatMap { it }
                                        .joinToString(";") { pair -> pair.joinToString(",") }
                                }) { Text("Edit") }
                            }
                            Divider()
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { if (page > 0) page-- },
                enabled = page > 0,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black, contentColor = Color.White)
            ) { Text("Previous") }
            Text("Page ${page + 1} of ${((totalCount - 1) / pageSize) + 1}")
            Button(
                onClick = { if ((page + 1) * pageSize < totalCount) page++ },
                enabled = (page + 1) * pageSize < totalCount,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Black, contentColor = Color.White)
            ) { Text("Next") }
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
        Column(Modifier.fillMaxSize()) {
            Surface(color = Color.Black) {
                TabRow(
                    selectedTabIndex = tabIndex,
                    modifier = Modifier.height(64.dp),
                    backgroundColor = Color.Black,
                    contentColor = Color.White
                ) {
                    Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }) { Text("Management") }
                    Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }) { Text("Generator") }
                    Tab(selected = tabIndex == 2, onClick = { tabIndex = 2 }) { Text("Scraper") }
                    Tab(selected = tabIndex == 3, onClick = { tabIndex = 3 }) { Text("DB Viewer") }
                }
            }
            Box(Modifier.fillMaxSize()) {
                when (tabIndex) {
                    0 ->
                        DataManagerTab(
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
                                    //Re-check after update:
                                    val result = checkDatabaseUpToDate()
                                    missingFireStations = result.missingFireStations
                                    missingEarthquakes = result.missingEarthquakes
                                    isLoading = false
                                }
                            },
                            missingFireStations = missingFireStations,
                            missingEarthquakes = missingEarthquakes
                        )
                    1 -> UniversalGeneratorTab()
                    2 -> ScraperTab()
                    3 -> DatabaseViewerTab()
                }
            }
        }
    }
}

fun main() = application {

    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
