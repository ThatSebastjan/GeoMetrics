package com.geometrics.app.models

data class Incident(
    val id: String,
    val type: String,
    val severity: Int,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
) {
    fun getSeverityText(): String = when (severity) {
        1 -> "Low"
        2 -> "Medium"
        3 -> "High"
        else -> "Unknown"
    }

    fun getFormattedDate(): String {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}

