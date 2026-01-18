package com.geometrics.app

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
fun DetectOverlay(
    modifier: Modifier = Modifier,
    currentCoordinates: Pair<Double, Double>,
    onClose: () -> Unit,
    backendUrl: String = "http://localhost:3001/detect"
) {
    var loading by remember { mutableStateOf(true) }
    var distanceMeters by remember { mutableStateOf<Double?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val client = OkHttpClient()
    val handler = Handler(Looper.getMainLooper())

    fun sendDetectionJson(url: String, json: String, onResult: (success: Boolean, responseBody: String?) -> Unit) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
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

    // Trigger sending once when composed
    if (loading && distanceMeters == null && errorMsg == null) {
        val (lat, lon) = currentCoordinates
        val json = JSONObject().apply {
            put("longitude", lon)   // order long, lat per request
            put("latitude", lat)
            put("timestamp", System.currentTimeMillis())
        }.toString()

        loading = true
        sendDetectionJson(backendUrl, json) { success, responseBody ->
            handler.post {
                loading = false
                if (!success) {
                    errorMsg = responseBody ?: "Unknown error"
                    return@post
                }

                if (responseBody.isNullOrBlank()) {
                    errorMsg = "Empty response"
                    return@post
                }

                // Try parse numeric distance from JSON { "distance": 123 } or plain number
                try {
                    val obj = JSONObject(responseBody)
                    if (obj.has("distance")) {
                        distanceMeters = obj.getDouble("distance")
                    } else {
                        // fallback: try parse top-level numeric value
                        distanceMeters = responseBody.toDoubleOrNull()
                        if (distanceMeters == null) errorMsg = responseBody
                    }
                } catch (e: Exception) {
                    // Not JSON -> try plain numeric
                    distanceMeters = responseBody.toDoubleOrNull()
                    if (distanceMeters == null) errorMsg = responseBody
                }
            }
        }
    }

    // UI overlay
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.2f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Detect nearest water", style = MaterialTheme.typography.titleMedium)

                if (loading) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        Text("Detecting...", style = MaterialTheme.typography.bodyMedium)
                    }
                } else if (distanceMeters != null) {
                    val meters = String.format("%.0f", distanceMeters)
                    Text("Nearest body of water is $meters meters away", style = MaterialTheme.typography.bodyLarge)
                } else if (errorMsg != null) {
                    Text("Error: $errorMsg", style = MaterialTheme.typography.bodyMedium, color = Color.Red)
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onClose) {
                        Text("Close")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        // reset and retry
                        loading = true
                        distanceMeters = null
                        errorMsg = null
                    }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}
