package com.geometrics.app.components

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView

@Composable
fun MapBoxContainer(
    modifier: Modifier = Modifier,
    heightDp: Int = 300,
    zoomLevel: Double = 14.0
) {
    if (LocalInspectionMode.current) return

    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val mapView = rememberMapView(context)

    // Runtime permission launcher
    var permissionGranted by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
    }

    // check and request permission on first composition
    LaunchedEffect(Unit) {
        val has = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (has) permissionGranted = true else launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // once we have permission and mapView is available, get last known location and center camera
    LaunchedEffect(permissionGranted, mapView) {
        if (!permissionGranted || mapView == null) return@LaunchedEffect
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return@LaunchedEffect
            val last = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            last?.let { location ->
                mapView.mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(Point.fromLngLat(location.longitude, location.latitude))
                    .zoom(zoomLevel)
                    .build()
                )
            }
        } catch (_: Exception) {  }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (mapView == null) return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            try { mapView?.onDestroy() } catch (_: Exception) {}
        }
    }

    AndroidView(
        factory = { mapView ?: android.view.View(context) },
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
    )
}

@Composable
private fun rememberMapView(context: Context): MapView? = remember {
    runCatching { MapView(context) }.getOrNull()
}
