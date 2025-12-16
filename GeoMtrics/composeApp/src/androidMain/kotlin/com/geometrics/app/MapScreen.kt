package com.app.geometrics

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.geometrics.app.components.MapBoxContainer
import com.mapbox.maps.MapboxMap


@Composable
@Preview(showBackground = true)
fun MapScreen(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text("Map", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))
        Text ("Placeholder view for the Map view")
        Spacer(modifier = Modifier.height(12.dp))
        MapBoxContainer(
            modifier = Modifier
                .fillMaxSize()
        )
    }
}

