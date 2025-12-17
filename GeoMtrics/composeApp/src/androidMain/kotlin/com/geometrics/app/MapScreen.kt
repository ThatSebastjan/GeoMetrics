package com.app.geometrics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
    Surface(
        modifier = Modifier
            .fillMaxSize(),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
    ) {


        MapBoxContainer(
            modifier = Modifier
                .fillMaxSize()
        )
    }
}

