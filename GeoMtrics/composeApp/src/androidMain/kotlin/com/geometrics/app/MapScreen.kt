package com.app.geometrics

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.geometrics.app.ReportOverlay
import com.geometrics.app.DetectOverlay
import com.geometrics.app.components.IncidentDetailDialog
import com.geometrics.app.components.IncidentNotification
import com.geometrics.app.components.MapBoxContainer
import com.geometrics.app.managers.IncidentManager
import com.geometrics.app.managers.MqttManager
import com.geometrics.app.models.Incident
import android.Manifest
import android.Manifest.permission
import android.content.pm.PackageManager.*
import androidx.core.content.ContextCompat.checkSelfPermission
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest.create
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority.*
import kotlinx.coroutines.tasks.await
import kotlin.math.*

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    var showReportScreen by remember { mutableStateOf(false) }
    var showDetectScreen by remember { mutableStateOf(false) }
    var currentCoordinates by remember { mutableStateOf(Pair(0.0, 0.0)) }
    var selectedIncident by remember { mutableStateOf<Incident?>(null) }
    var notificationIncident by remember { mutableStateOf<Pair<Incident, Double>?>(null) }
    var refreshIncidents by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val appContext = remember { context.applicationContext }
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    LaunchedEffect(Unit) {
        android.util.Log.d("MapScreen", "=== MapScreen LaunchedEffect started ===")
        android.util.Log.d("MapScreen", "Initializing IncidentManager...")
        IncidentManager.initialize(context)
        
        android.util.Log.d("MapScreen", "Initializing MqttManager...")
        MqttManager.initialize(appContext)
        android.util.Log.d("MapScreen", "MqttManager initialized")
        
        android.util.Log.d("MapScreen", "Setting incident received callback...")
        MqttManager.setOnIncidentReceived { incident ->
            android.util.Log.d("MapScreen", "Received incident via MQTT: ${incident.id}, type: ${incident.type}")
            val (userLat, userLon) = currentCoordinates
            android.util.Log.d("MapScreen", "User location: $userLat, $userLon")
            android.util.Log.d("MapScreen", "Incident location: ${incident.latitude}, ${incident.longitude}")
            
            if (userLat != 0.0 && userLon != 0.0) {
                val isNear = MqttManager.isNearby(userLat, userLon, incident.latitude, incident.longitude)
                android.util.Log.d("MapScreen", "Is nearby: $isNear")
                
                if (isNear) {
                    val distance = calculateDistance(userLat, userLon, incident.latitude, incident.longitude)
                    android.util.Log.d("MapScreen", "Distance: $distance km")
                    notificationIncident = Pair(incident, distance)
                    
                    IncidentManager.addIncident(incident)
                    refreshIncidents++
                } else {
                    android.util.Log.d("MapScreen", "Incident is not nearby (distance > 5km)")
                }
            } else {
                android.util.Log.w("MapScreen", "User location not available yet, storing incident anyway")
                IncidentManager.addIncident(incident)
                refreshIncidents++
            }
        }
        
        android.util.Log.d("MapScreen", "Attempting to connect to MQTT...")
        MqttManager.connect(
            onConnected = {
                android.util.Log.d("MapScreen", "=== MQTT connected successfully ===")
            },
            onError = { error ->
                android.util.Log.e("MapScreen", "=== MQTT connection ERROR: $error ===")
            }
        )
        
        IncidentManager.syncFromBackend { success, count ->
            if (success) {
                refreshIncidents++
            }
        }
    }

    LaunchedEffect(context, fusedClient) {
        if (checkSelfPermission(context, permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) return@LaunchedEffect

        val request = create().apply {
            interval = 5000
            fastestInterval = 2000
            priority = PRIORITY_HIGH_ACCURACY
        }

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { currentCoordinates = Pair(it.latitude, it.longitude) }
            }
        }

        try {
            fusedClient.requestLocationUpdates(request, callback, android.os.Looper.getMainLooper())
            kotlinx.coroutines.awaitCancellation()
        } finally {
            fusedClient.removeLocationUpdates(callback)
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            android.util.Log.d("MapScreen", "MapScreen disposing - disconnecting MQTT")
            MqttManager.disconnect()
        }
    }
    
    Surface(
        modifier = modifier.fillMaxSize(),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val currentIncidents = remember(refreshIncidents) { 
                IncidentManager.getAllIncidents() 
            }
            
            MapBoxContainer(
                modifier = Modifier.fillMaxSize(),
                showIncidents = true,
                incidents = currentIncidents,
                refreshTrigger = refreshIncidents,
                onIncidentClick = { incident ->
                    selectedIncident = incident
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    notificationIncident?.let { (incident, distance) ->
                        IncidentNotification(
                            incident = incident,
                            distanceKm = distance,
                            onDismiss = { notificationIncident = null },
                            onClick = {
                                selectedIncident = incident
                                notificationIncident = null
                            }
                        )
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = { showReportScreen = true }) {
                            Text("Report an Incident")
                        }
                        Button(onClick = { showDetectScreen = true }) {
                            Text("Nearest body of water")
                        }
                    }
                }
            }

            if (showReportScreen) {
                ReportOverlay(
                    modifier = Modifier.fillMaxSize(),
                    currentCoordinates = currentCoordinates,
                    onClose = {
                        showReportScreen = false
                        refreshIncidents++
                    }
                )
            }

            if (showDetectScreen) {
                DetectOverlay(
                    modifier = Modifier.fillMaxSize(),
                    currentCoordinates = currentCoordinates,
                    onClose = { showDetectScreen = false }
                )
            }

            selectedIncident?.let { incident ->
                IncidentDetailDialog(
                    incident = incident,
                    onDismiss = { selectedIncident = null }
                )
            }
        }
    }
}

private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadiusKm = 6371.0
    
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    
    return earthRadiusKm * c
}
