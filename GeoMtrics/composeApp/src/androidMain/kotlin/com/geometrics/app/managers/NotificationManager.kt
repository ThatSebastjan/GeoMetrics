package com.geometrics.app.managers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.geometrics.app.MainActivity
import com.geometrics.app.models.Incident

object IncidentNotificationManager {
    private const val CHANNEL_ID = "incidents_channel"
    private const val CHANNEL_NAME = "Natural Disaster Incidents"
    private const val NOTIFICATION_ID_BASE = 1000

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = AndroidNotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Notifications for nearby natural disaster incidents"
                enableVibration(true)
                enableLights(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showIncidentNotification(context: Context, incident: Incident, distanceKm: Double) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("incident_id", incident.id)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val severityText = when (incident.severity) {
            1 -> "Low"
            2 -> "Medium"
            3 -> "High"
            else -> "Unknown"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Nearby ${incident.type}")
            .setContentText("Severity: $severityText • ${String.format("%.1f km away", distanceKm)}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("A ${incident.type} incident has been reported nearby.\n\nSeverity: $severityText\nDistance: ${String.format("%.1f km", distanceKm)}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .apply {
                when (incident.severity) {
                    3 -> setDefaults(NotificationCompat.DEFAULT_ALL)
                    2 -> setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
                    else -> setDefaults(NotificationCompat.DEFAULT_SOUND)
                }
            }
            .build()

        val notificationId = NOTIFICATION_ID_BASE + incident.id.hashCode()
        notificationManager.notify(notificationId, notification)
    }
}
