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

    // Common fields
    var longitude by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }

    // Fire Station fields
    var fsLocation by remember { mutableStateOf("") }
    var fsAddress by remember { mutableStateOf("") }
    var fsCity by remember { mutableStateOf("") }
    var fsDescription by remember { mutableStateOf("") }
    var fsTelephone by remember { mutableStateOf("") }

    // Earthquake fields
    var eqMagnitude by remember { mutableStateOf("") }
    var eqDepth by remember { mutableStateOf("") }

    // Landslide/Flood fields
    var polygonText by remember { mutableStateOf("") }

    // Landslide/Flood properties
    var typeInt by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Universal Generator", style = MaterialTheme.typography.h6)

        // Type selector
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
                                    val landslides = ApiClient.get<LandSlide>(0, 10000)
                                    val maxObjectId = landslides.maxOfOrNull { it.properties.OBJECTID } ?: 0
                                    val exists = landslides.any { it.properties.OBJECTID == maxObjectId + 1 }
                                    if (!exists) {
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
                                    } else {
                                        status = "❌ Landslide insert failed (duplicate OBJECTID)"
                                    }
                                }
                                "Flood" -> {
                                    val coords = polygonText.split(";").map {
                                        val (lon, lat) = it.split(",").map(String::toDouble)
                                        arrayListOf(lon, lat)
                                    }
                                    val polygon = arrayListOf(coords as ArrayList<ArrayList<Double>>)
                                    val floods = ApiClient.get<Flood>(0, 10000)
                                    val maxObjectId = floods.maxOfOrNull { it.properties.OBJECTID } ?: 0
                                    val exists = floods.any { it.properties.OBJECTID == maxObjectId + 1 }
                                    if (!exists) {
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
                                    } else {
                                        status = "❌ Flood insert failed (duplicate OBJECTID)"
                                    }
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
                        val scraper = models.Scraper()
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
    val scope = rememberCoroutineScope()
    val pageSize = 10

    LaunchedEffect(subTabIndex, page) {
        when (subTabIndex) {
            0 -> scope.launch {
                totalCount = ApiClient.count<Earthquake>()
                earthquakes = ApiClient.get<Earthquake>(page * pageSize, pageSize)
            }
            1 -> scope.launch {
                totalCount = ApiClient.count<FireStation>()
                fireStations = ApiClient.get<FireStation>(page * pageSize, pageSize)
            }
            2 -> scope.launch {
                totalCount = ApiClient.count<Flood>()
                floods = ApiClient.get<Flood>(page * pageSize, pageSize)
            }
            3 -> scope.launch {
                totalCount = ApiClient.count<LandSlide>()
                landslides = ApiClient.get<LandSlide>(page * pageSize, pageSize)
            }
            4 -> scope.launch {
                totalCount = ApiClient.count<LandLot>()
                landLots = ApiClient.get<LandLot>(page * pageSize, pageSize)
            }
            5 -> scope.launch {
                totalCount = ApiClient.count<LandUse>()
                landUses = ApiClient.get<LandUse>(page * pageSize, pageSize)
            }
        }
    }

    fun resetPage() {
        page = 0
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        TabRow(
            selectedTabIndex = subTabIndex,
            backgroundColor = Color.Black,
            contentColor = Color.White
        ) {
            Tab(selected = subTabIndex == 0, onClick = { subTabIndex = 0; resetPage() }) { Text("Earthquakes") }
            Tab(selected = subTabIndex == 1, onClick = { subTabIndex = 1; resetPage() }) { Text("Fire Stations") }
            Tab(selected = subTabIndex == 2, onClick = { subTabIndex = 2; resetPage() }) { Text("Floods") }
            Tab(selected = subTabIndex == 3, onClick = { subTabIndex = 3; resetPage() }) { Text("Landslides") }
            Tab(selected = subTabIndex == 4, onClick = { subTabIndex = 4; resetPage() }) { Text("Land Lots") }
            Tab(selected = subTabIndex == 5, onClick = { subTabIndex = 5; resetPage() }) { Text("Land Uses") }
        }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.weight(1f)) {
            when (subTabIndex) {
                0 -> LazyColumn { items(earthquakes) { eq -> Text("ID: ${eq.id}, Mag: ${eq.properties.magnitude}, Depth: ${eq.properties.depth}, Time: ${eq.properties.timestamp}"); Divider() } }
                1 -> LazyColumn { items(fireStations) { fs -> Text("ID: ${fs.id}, Location: ${fs.properties.location}, Address: ${fs.properties.address}, City: ${fs.properties.city}"); Divider() } }
                2 -> LazyColumn { items(floods) { f -> Text("ID: ${f.id}, Type: ${f.properties.FloodType}, ObjectID: ${f.properties.OBJECTID}"); Divider() } }
                3 -> LazyColumn { items(landslides) { l -> Text("ID: ${l.id}, Type: ${l.properties.LandSlideType}, ObjectID: ${l.properties.OBJECTID}"); Divider() } }
                4 -> LazyColumn { items(landLots) { lot -> Text("ID: ${lot.id}, ST_PARCELE: ${lot.properties.ST_PARCELE}, EID_PARCELA: ${lot.properties.EID_PARCELA}, KO_ID: ${lot.properties.KO_ID}, POVRSINA: ${lot.properties.POVRSINA}"); Divider() } }
                5 -> LazyColumn { items(landUses) { use -> Text("ID: ${use.id}, OBJECTID: ${use.properties.OBJECTID}, RABA_ID: ${use.properties.RABA_ID}"); Divider() } }
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
                    0 -> // In your App() composable, inside the DataManagerTab call:
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
                                    // Optionally, re-check after update:
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


    //Get example
    /*
    val eqs = ApiClient.get<Earthquake>(0, 10);

    for(e in eqs){
        println(e);
    }
    */


    //Count examples
    /*
    println("Earthquakes: ${ApiClient.count<Earthquake>()}")
    println("FireStations: ${ApiClient.count<FireStation>()}")
    println("Floods: ${ApiClient.count<Flood>()}")
    println("LandLots: ${ApiClient.count<LandLot>()}")
    println("LandSlides: ${ApiClient.count<LandSlide>()}")
    println("LandUse: ${ApiClient.count<LandUse>()}")
    */


    //Insert example
    /*
    val eq = models.Earthquake(
        type = "Feature",
        id = 999,
        geometry = models.GeoJsonPoint(coordinates = arrayListOf(12.34, 23.45)),
        properties = models.EarthquakeProperties(
            timestamp = Clock.System.now(),
            magnitude = 9.21,
            depth = 12.34
        )
    )

    if(ApiClient.insert<Earthquake>(eq)){
        println("OK")
    }
    else {
        println("Error")
    }
    */
}
