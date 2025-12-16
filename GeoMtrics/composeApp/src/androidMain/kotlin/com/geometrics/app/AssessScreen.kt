package com.app.geometrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.geometrics.app.components.GaugeComponent
import com.geometrics.app.components.MapBoxContainer
import com.mapbox.maps.extension.style.expressions.dsl.generated.color

@Composable
@Preview(showBackground = true)
fun AssessScreen(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(top = 24.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text("Assess", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth(.95f)
                    .padding(bottom = 16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.background,
                        shape = RoundedCornerShape(48.dp)
                    )
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Column(
                    modifier = Modifier
                        .padding(8.dp),
                ) {
                    GaugeComponent(
                        100f, size = 100.dp, strokeWidth = 12.dp, startColor = Color(
                            0xFF00FFF5
                        ), endColor = Color(0xFF002AFF)
                    )
                    Text("Flood", modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                Column(
                    modifier = Modifier
                        .padding(8.dp),
                ) {
                    GaugeComponent(
                        100f, size = 100.dp, strokeWidth = 12.dp, startColor = Color(
                            0xFFFFE500
                        ), endColor = Color(0xFFFF5722)
                    )
                    Text("Landslide", modifier = Modifier.align(Alignment.CenterHorizontally))

                }
                Column(
                    modifier = Modifier
                        .padding(8.dp),
                ) {
                    GaugeComponent(
                        100f, size = 100.dp, strokeWidth = 12.dp, startColor = Color(
                            0xFFFF857D
                        ), endColor = Color(0xFFF70303)
                    )
                    Text("Earthquake", modifier = Modifier.align(Alignment.CenterHorizontally))

                }
            }


        Surface(
            modifier = Modifier
                .fillMaxHeight(.95f),
            shape = RoundedCornerShape(48.dp),
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
        ) {
            MapBoxContainer(
                zoomLevel = 18.0,
                modifier = Modifier
                    .fillMaxWidth(.95f)
                    .height(300.dp)
            )
        }
    }
}

