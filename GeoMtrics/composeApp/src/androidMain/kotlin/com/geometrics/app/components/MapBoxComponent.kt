package com.geometrics.app.components

import android.Manifest
import android.content.Context
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.geometrics.app.Constants
import com.geometrics.app.models.Incident
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.QueryRenderedFeaturesCallback
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.ScreenBox
import com.mapbox.maps.ScreenCoordinate
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.image.image
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.fillLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.addGeoJSONSourceFeatures
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.updateGeoJSONSourceFeatures
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.addOnMoveListener
import com.mapbox.turf.TurfMeasurement
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException


@Composable
fun MapBoxContainer(
    modifier: Modifier = Modifier,
    heightDp: Int = 300,
    zoomLevel: Double = 14.0,
    addLandLotLayer: Boolean = false,
    showIncidents: Boolean = false,
    incidents: List<Incident> = emptyList(),
    refreshTrigger: Int = 0,
    onIncidentClick: ((Incident) -> Unit)? = null,
    onAssesmentResult: ((floodRisk: Double, landslideRisk: Double, earthquakeRisk: Double) -> Unit)? = null
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

    LaunchedEffect(mapView) {

        if(mapView == null) return@LaunchedEffect

        if(addLandLotLayer) {
            mapView?.mapboxMap?.addOnMoveListener(object : OnMoveListener {
                override fun onMove(detector: MoveGestureDetector): Boolean {
                    return false
                }

                override fun onMoveBegin(detector: MoveGestureDetector) {}

                override fun onMoveEnd(detector: MoveGestureDetector) {
                    handleOnMoveEnd(mapView);
                }
            })

            mapView.mapboxMap.addSource(
                geoJsonSource("land_lots")
            )


            mapView.mapboxMap.addLayer(lineLayer("land_data_outline", "land_lots") {
                lineColor("#005000")
                lineWidth(2.0)
                lineOpacity(0.3)
                minZoom(13.5)
            })

            mapView.mapboxMap.addLayer(fillLayer("land_data", "land_lots") {
                fillColor("#000000")
                fillOutlineColor("#000000")
                fillOpacity(Expression.switchCase {
                    eq {
                        get { literal("active") }
                        literal(true)
                    }
                    literal(0.1)
                    literal(0.0)
                })
                minZoom(13.5)
            })

            mapView.mapboxMap.addSource(
                geoJsonSource("label_source")
            )

            mapView.mapboxMap.addLayer(symbolLayer("land_labels", "label_source") {
                textColor("#220022")
                textHaloColor("#ffffff")
                textHaloWidth(2.0)
                minZoom(13.5)
                textField(Expression.get("label"))
                textSize(11.0)
                textLetterSpacing(0.05)
                textOffset(listOf(0.0, 0.0))
            })

            mapView.mapboxMap.addOnMapClickListener {
                handleMapClick(mapView.mapboxMap, it, onAssesmentResult)
                return@addOnMapClickListener false
            }

        }
    }

    // Load incident icons from drawable resources
    LaunchedEffect(mapView) {
        if (mapView == null) return@LaunchedEffect
        
        // Wait for style to be loaded
        mapView.mapboxMap.getStyle { style ->
            try {
                // Load PNG images from drawable
                val landslideResId = context.resources.getIdentifier("landslide", "drawable", context.packageName)
                val earthquakeResId = context.resources.getIdentifier("earthquake", "drawable", context.packageName)
                val floodResId = context.resources.getIdentifier("flood", "drawable", context.packageName)
                
                if (landslideResId != 0) {
                    val bitmap = BitmapFactory.decodeResource(context.resources, landslideResId)
                    bitmap?.let { 
                        style.addImage("icon_landslide", it)
                        println("MapBox: Loaded landslide icon")
                    }
                }
                if (earthquakeResId != 0) {
                    val bitmap = BitmapFactory.decodeResource(context.resources, earthquakeResId)
                    bitmap?.let { 
                        style.addImage("icon_earthquake", it)
                        println("MapBox: Loaded earthquake icon")
                    }
                }
                if (floodResId != 0) {
                    val bitmap = BitmapFactory.decodeResource(context.resources, floodResId)
                    bitmap?.let { 
                        style.addImage("icon_flood", it)
                        println("MapBox: Loaded flood icon")
                    }
                }
            } catch (e: Exception) {
                println("MapBox: Error loading icons: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    // Add incident markers when showIncidents is true
    LaunchedEffect(mapView, showIncidents, incidents, refreshTrigger) {
        if (mapView == null || !showIncidents) {
            println("MapBox: LaunchedEffect skipped - mapView=$mapView, showIncidents=$showIncidents")
            return@LaunchedEffect
        }

        println("=== MapBox: Adding incident markers ===")
        println("Incidents to display: ${incidents.size}")
        incidents.forEach { inc ->
            println("  - ${inc.type} at (${inc.latitude}, ${inc.longitude})")
        }

        try {
            val map = mapView.mapboxMap

            val incidentSource = map.getSourceAs<GeoJsonSource>("incidents")
            if (incidentSource == null) {
                println("MapBox: Creating new incidents source")
                map.addSource(geoJsonSource("incidents") {
                    featureCollection(com.mapbox.geojson.FeatureCollection.fromFeatures(emptyList()))
                })
            } else {
                println("MapBox: Incidents source already exists")
            }

            val features = incidents.map { incident ->
                Feature.fromGeometry(
                    Point.fromLngLat(incident.longitude, incident.latitude)
                ).apply {
                    addStringProperty("id", incident.id)
                    addStringProperty("type", incident.type)
                    addNumberProperty("severity", incident.severity)
                    // Use icon name instead of emoji
                    addStringProperty("icon", when(incident.type.lowercase()) {
                        "landslide" -> "icon_landslide"
                        "earthquake" -> "icon_earthquake"
                        "flood" -> "icon_flood"
                        else -> "icon_landslide" // Default fallback
                    })
                }
            }

            println("MapBox: Created ${features.size} features")

            val src = map.getSourceAs<GeoJsonSource>("incidents")
            src?.featureCollection(com.mapbox.geojson.FeatureCollection.fromFeatures(features))
            println("MapBox: Updated source with features")

            try {
                // Remove old layer if it exists
                try {
                    map.getStyle()?.removeStyleLayer("incident_layer")
                } catch (_: Exception) {}
                
                // Add new layer with icon images instead of text
                map.addLayer(symbolLayer("incident_layer", "incidents") {
                    iconImage(Expression.get("icon"))
                    iconSize(0.24)
                    iconAllowOverlap(true)
                    iconIgnorePlacement(true)
                    iconAnchor(IconAnchor.CENTER)
                })
                println("MapBox: Incident layer added successfully with PNG icons")

                map.addOnMapClickListener { point ->
                    val screenPoint = map.pixelForCoordinate(point)
                    val bounds = RenderedQueryGeometry(ScreenBox(
                        ScreenCoordinate(screenPoint.x - 20, screenPoint.y - 20),
                        ScreenCoordinate(screenPoint.x + 20, screenPoint.y + 20)
                    ))

                    map.queryRenderedFeatures(
                        bounds,
                        RenderedQueryOptions(mutableListOf("incident_layer"), null)
                    ) { result ->
                        result.value?.firstOrNull()?.let { queriedFeature ->
                            val incidentId = queriedFeature.queriedFeature.feature.getStringProperty("id")
                            val incident = incidents.find { it.id == incidentId }
                            incident?.let { onIncidentClick?.invoke(it) }
                        }
                    }
                    false
                }
            } catch (e: Exception) {
                println("MapBox: Layer probably already exists (this is OK): ${e.message}")
            }
        } catch (e: Exception) {
            println("MapBox ERROR: ${e.message}")
            e.printStackTrace()
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


val previousView: Array<Double> = arrayOf(0.0, 0.0, 0.0, 0.0)
var httpClient: OkHttpClient? = null

private fun handleOnMoveEnd(mapView: MapView){
    println("handleOnMoveEnd called!")

    if(httpClient == null){
        httpClient = OkHttpClient()
    }

    val map = mapView.mapboxMap

    val dataSrc = map.getSourceAs<GeoJsonSource>("land_lots")
    val labelSrc = map.getSourceAs<GeoJsonSource>("label_source")

    if(dataSrc == null || labelSrc == null){
        println("Error: no data or label source!")
        return
    }

    val topLeft = map.coordinateForPixel(ScreenCoordinate(0.0, 0.0))
    val bottomRight = map.coordinateForPixel(ScreenCoordinate(mapView.width.toDouble(),
        mapView.height.toDouble()
    ))

    val dst = TurfMeasurement.distance(topLeft, bottomRight)

    if(dst > 10){
        return //Max 10km diagonal
    }

    val minX = topLeft.longitude()
    val minY = bottomRight.latitude()
    val maxX = bottomRight.longitude()
    val maxY = topLeft.latitude()

    val reqUrl = "${Constants.BACKEND_URL}/map/query/${previousView[0]},${previousView[1]},${previousView[2]},${previousView[3]},$minX,$minY,$maxX,$maxY"
    val req = Request.Builder().url(reqUrl).build()

    val call = httpClient!!.newCall(req)
    call.enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            println("Error getting land lots: $e")
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                if(it.isSuccessful) {
                    val respBody = it.body.string()
                    handleLandLots(dataSrc, labelSrc, respBody)
                }
            }
        }
    })

    previousView[0] = minX
    previousView[1] = minY
    previousView[2] = maxX
    previousView[3] = maxY
}


val presentFeatureIds: HashSet<String> = HashSet()
fun handleLandLots(dataSource: GeoJsonSource, labelSource: GeoJsonSource, resp: String){
    val respJson = JSONObject(resp)
    val data = respJson.getJSONArray("data")

    val features: MutableList<Feature> = mutableListOf()
    val labels: MutableList<Feature> = mutableListOf()

    for(i in 0 until data.length()){
        val str = data.getJSONObject(i).toString()
        val f = Feature.fromJson(str)

        if(presentFeatureIds.contains(f.id())){
            continue
        }

        presentFeatureIds.add(f.id()!!)
        features.add(f)

        val lotLbl = f.getStringProperty("ST_PARCELE")

        if(lotLbl != null) {
            val center = TurfMeasurement.center(f)
            val lbl = Feature.fromGeometry(center.geometry(), null, f.id())
            lbl.id()
            lbl.addStringProperty("label", lotLbl)
            labels.add(lbl)
        }
    }

    println("Got ${features.size} new features!")

    dataSource.addGeoJSONSourceFeatures(features)
    labelSource.addGeoJSONSourceFeatures(labels)
}


fun handleMapClick(
    map: MapboxMap,
    p: Point,
    onAssesmentResult: ((floodRisk: Double, landslideRisk: Double, earthquakeRisk: Double) -> Unit)?
){
    val screenPoint = map.pixelForCoordinate(p)
    val bounds = RenderedQueryGeometry(ScreenBox(
        ScreenCoordinate(screenPoint.x - 10, screenPoint.y - 10),
        ScreenCoordinate(screenPoint.x + 10, screenPoint.y + 10)
    ))

    val featureList = map.queryRenderedFeatures(
        bounds,
        RenderedQueryOptions(mutableListOf("land_data"), null),
        QueryRenderedFeaturesCallback({
            val first = it.value?.firstOrNull()
            if(first != null) {
                handleSelectedFeature(map, first.queriedFeature.feature, onAssesmentResult)
            }
        })
    )
}


var previousActiveFeature: Feature? = null

fun handleSelectedFeature(
    map: MapboxMap,
    feature: Feature,
    onAssesmentResult: ((floodRisk: Double, landslideRisk: Double, earthquakeRisk: Double) -> Unit)?
){
    val dataSrc = map.getSourceAs<GeoJsonSource>("land_lots")

    if(dataSrc == null){
        println("Error: no data!")
        return
    }

    if(previousActiveFeature != null) {
        previousActiveFeature?.addBooleanProperty("active", false)
        dataSrc.updateGeoJSONSourceFeatures(listOf(previousActiveFeature!!))
    }

    feature.addBooleanProperty("active", true)
    dataSrc.updateGeoJSONSourceFeatures(listOf(feature))
    previousActiveFeature = feature

    //println("Clicked lot: ${feature.getStringProperty("ST_PARCELE")}")


    //Query assessment results

    //Can't access coordinates directly... so this is a workaround
    val coords = JSONObject(feature.geometry()!!.toJson()).getJSONArray("coordinates")
    val bodyObj = JSONObject()
    bodyObj.put("bounds", coords)

    postAssesment("${Constants.BACKEND_URL}/map/assess", bodyObj.toString(), { succecss, responseBody ->

        if(!succecss || responseBody == null){
            return@postAssesment
        }

        val respObj = JSONObject(responseBody)
        val results = respObj.getJSONObject("results")

        //Descriptive details are also returned in respObj "details" property...

        val floodRisk = results.getDouble("floodRisk")
        val landSlideRisk = results.getDouble("landSlideRisk")
        val earthQuakeRisk = results.getDouble("earthQuakeRisk")

        if(onAssesmentResult != null) {
            onAssesmentResult(floodRisk, landSlideRisk, earthQuakeRisk)
        }
    })
}


fun postAssesment(url: String, json: String, onResult: (success: Boolean, responseBody: String?) -> Unit) {
    val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
    val body = json.toRequestBody(mediaType)
    val request = Request.Builder()
        .url(url)
        .post(body)
        .build()

    if(httpClient == null){
        httpClient = OkHttpClient()
    }

    val call = httpClient!!.newCall(request)
    call.enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onResult(false, e.message)
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                val respBody = it.body?.string()
                onResult(it.isSuccessful, respBody)
            }
        }
    })
}
