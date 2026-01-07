package com.geometrics.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.app.geometrics.AssessScreen
import com.app.geometrics.MapScreen
import com.app.geometrics.SettingsScreen
import org.jetbrains.compose.ui.tooling.preview.Preview

private sealed class Screen(val title: String, val icon: @Composable () -> Unit) {
    object Map : Screen("Map", { Icon(
        painter = painterResource(R.drawable.ic_map_24),
        contentDescription = "Assess"
    ) })
    object Assess : Screen("Assess", {  Icon(
        painter = painterResource(R.drawable.ic_assess_24),
        contentDescription = "Assess"
    ) })
    object Settings : Screen("Settings", {
        Icon(
        painter = painterResource(R.drawable.ic_settings_24),
            contentDescription = "Settings"
        )
    })
}

@Composable
@Preview(showBackground = true)
fun App() {
    var selectedScreen by remember { mutableStateOf<Screen>(Screen.Assess) }
    val screens = listOf(Screen.Map, Screen.Assess, Screen.Settings)

    MaterialTheme {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    modifier = Modifier,
//                        .padding(horizontal = 16.dp, vertical = 8.dp) // floating bar
//                        .clip(RoundedCornerShape(topEnd = 48.dp, topStart = 48.dp)),
                    containerColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    screens.forEach { screen ->
                        NavigationBarItem(
                            icon = screen.icon,
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
                is Screen.Settings -> SettingsScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(innerPadding)
                )
            }
        }
    }
}