package com.geometrics.app

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
fun ReportOverlay(
    modifier: Modifier = Modifier,
    currentCoordinates: Pair<Double, Double>,
    onClose: () -> Unit
) {
    var selectedDisaster by remember { mutableStateOf("Landslide") }
    var selectedSeverity by remember { mutableStateOf(1) }
    val client = OkHttpClient()


    fun sendReportJson(url: String, json: String, onResult: (success: Boolean, responseBody: String?) -> Unit) {
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        val call = client.newCall(request)
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

    // Fullscreen dimmed background
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.2f))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        // Card with the reporting options shown immediately
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Report an Incident", style = MaterialTheme.typography.titleMedium)

                // Disaster selection
                Text("Type", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val options = listOf("Landslide", "Earthquake", "Flood")
                    options.forEach { opt ->
                        val selected = opt == selectedDisaster
                        Button(
                            onClick = { selectedDisaster = opt },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primary else Color(0xffc8b3ff)
                            )
                        ) {
                            Text(opt)
                        }
                    }
                }

                // Severity selection (1..3)
                Text("Severity", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val severityOptions = listOf(1 to "Low", 2 to "Medium", 3 to "High")
                    severityOptions.forEach { (value, label) ->
                        val selected = selectedSeverity == value
                        Button(
                            onClick = { selectedSeverity = value },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primary else Color(0xffc8b3ff)
                            )
                        ) {
                            Text(label)
                        }
                    }
                }
            }

            // Submit / Cancel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onClose,
                ) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    // Build JSON including coordinates and print to console
                    val (lat, lon) = currentCoordinates
                    val json = JSONObject().apply {
                        put("type", selectedDisaster)
                        put("severity", selectedSeverity)
                        put("latitude", lat)
                        put("longitude", lon)
                        put("timestamp", System.currentTimeMillis())
                    }
                    sendReportJson(
                        "${Constants.BACKEND_URL}/report", json.toString(),
                        onResult = { success, responseBody ->
                            if (success) {
                                onClose()
                            }
                            else {
                                println("Error: $responseBody")
                                onClose()
                            }
                        }
                    )
                }) {
                    Text("Submit")
                }
            }
        }
    }
}
