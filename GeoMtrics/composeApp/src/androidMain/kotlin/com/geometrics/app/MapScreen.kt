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
import com.geometrics.app.components.MapBoxContainer
import com.geometrics.app.managers.IncidentManager
import com.geometrics.app.models.Incident
import android.Manifest
import android.Manifest.permission
import android.content.pm.PackageManager
import android.content.pm.PackageManager.*
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest.create
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority.*
import kotlinx.coroutines.tasks.await

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    var showReportScreen by remember { mutableStateOf(false) }
    var showDetectScreen by remember { mutableStateOf(false) }
    var currentCoordinates by remember { mutableStateOf(Pair(0.0, 0.0)) }
    var selectedIncident by remember { mutableStateOf<Incident?>(null) }
    var refreshIncidents by remember { mutableStateOf(0) }
    val context = LocalContext.current
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    LaunchedEffect(Unit) {
        IncidentManager.initialize(context)
        IncidentManager.syncFromBackend { success, count ->
            if (success) {
                println("Synced $count incidents from backend/blockchain")
                refreshIncidents++
            } else {
                println("Sync failed, using local incidents only")
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
