package com.geometrics.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.app.geometrics.AssessScreen
import com.app.geometrics.MapScreen
import com.app.geometrics.ProfileScreen
import org.jetbrains.compose.ui.tooling.preview.Preview

private sealed class Screen(val title: String, val icon: String) {
    object Map : Screen("Map", "🗺️")
    object Assess : Screen("Assess", "📝")
    object Profile : Screen("Profile", "👤")
}

@Composable
@Preview(showBackground = true)
fun App() {
    var selectedScreen by remember { mutableStateOf<Screen>(Screen.Assess) }
    val screens = listOf(Screen.Map, Screen.Assess, Screen.Profile)

    MaterialTheme {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(48.dp)),
                    containerColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    screens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Text(screen.icon) },
                            label = { Text(screen.title) },
                            selected = selectedScreen == screen,
                            onClick = { selectedScreen = screen }
                        )
                    }
                }
            }
        ) { innerPadding ->
            when (selectedScreen) {
                is Screen.Map -> MapScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(innerPadding)
                )
                is Screen.Assess -> AssessScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(innerPadding)
                )
                is Screen.Profile -> ProfileScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(innerPadding)
                )
            }
        }
    }
}