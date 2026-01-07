package com.geometrics.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.json.JSONObject

@Composable
fun ReportOverlay(
    modifier: Modifier = Modifier,
    currentCoordinates: Pair<Double, Double>,
    onClose: () -> Unit
) {
    var selectedDisaster by remember { mutableStateOf("Landslide") }
    var selectedSeverity by remember { mutableStateOf(1) }

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
                    println(json.toString()) // output to console for now
                    onClose()
                }) {
                    Text("Submit")
                }
            }
        }
    }
}
