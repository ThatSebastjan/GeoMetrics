package com.app.geometrics

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.geometrics.app.ReportOverlay
import com.geometrics.app.components.MapBoxContainer
import org.jetbrains.compose.ui.tooling.preview.Preview
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await

@Composable
@Preview(showBackground = true)
fun MapScreen(modifier: Modifier = Modifier) {
    var showReportScreen by remember { mutableStateOf(false) }
    var currentCoordinates by remember { mutableStateOf(Pair(0.0, 0.0)) }
    val context = LocalContext.current
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val loc = try { fusedClient.lastLocation.await() } catch (_: Exception) { null }
            loc?.let { currentCoordinates = Pair(it.latitude, it.longitude) }
        }
    }
    Surface(
        modifier = modifier.fillMaxSize(),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MapBoxContainer(modifier = Modifier.fillMaxSize())


            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter),
                contentAlignment = Alignment.Center
            ) {
                Button(onClick = { showReportScreen = true }) {
                    Text("Report an Incident")
                }
            }

            // Overlay the ReportScreen when requested, passing current coordinates
            if (showReportScreen) {
                ReportOverlay(
                    modifier = Modifier.fillMaxSize(),
                    currentCoordinates = currentCoordinates,
                    onClose = { showReportScreen = false }
                )
            }
        }
    }
}
