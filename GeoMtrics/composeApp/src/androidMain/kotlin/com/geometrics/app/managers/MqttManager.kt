package com.geometrics.app.managers

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.geometrics.app.Constants
import com.geometrics.app.models.Incident
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import kotlin.math.*

object MqttManager {
    private var mqttClient: Mqtt3AsyncClient? = null
    private var isConnected = false
    private var onIncidentReceived: ((Incident) -> Unit)? = null
    private var context: Context? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun initialize(context: Context) {
        if (mqttClient != null) {
            return
        }

        this.context = context.applicationContext

        val serverHost = Constants.MQTT_BROKER_HOST
        val serverPort = Constants.MQTT_BROKER_PORT
        val clientId = "geometrics_android_${System.currentTimeMillis()}"
        
        mqttClient = MqttClient.builder()
            .useMqttVersion3()
            .identifier(clientId)
            .serverHost(serverHost)
            .serverPort(serverPort)
            .buildAsync()
    }

    fun connect(onConnected: (() -> Unit)? = null, onError: ((String) -> Unit)? = null) {
        if (mqttClient == null) {
            onError?.invoke("MQTT client not initialized")
            return
        }
        
        if (isConnected) {
            onConnected?.invoke()
            return
        }

        try {
            mqttClient?.connect()?.whenComplete { connAck: Mqtt3ConnAck?, throwable: Throwable? ->
                if (throwable != null) {
                    isConnected = false
                    mainHandler.post {
                        onError?.invoke(throwable.message ?: "Connection failed")
                    }
                } else {
                    isConnected = true
                    subscribe()
                    mainHandler.post {
                        onConnected?.invoke()
                    }
                }
            }
        } catch (e: Exception) {
            mainHandler.post {
                onError?.invoke(e.message ?: "Connection error")
            }
        }
    }

    private fun subscribe() {
        try {
            mqttClient?.subscribeWith()
                ?.topicFilter(Constants.MQTT_TOPIC)
                ?.qos(MqttQos.AT_LEAST_ONCE)
                ?.callback { publish: Mqtt3Publish ->
                    try {
                        val jsonString = String(publish.payloadAsBytes ?: ByteArray(0))
                        
                        val jsonObject = org.json.JSONObject(jsonString)
                        
                        val incident = Incident(
                            id = jsonObject.getString("id"),
                            type = jsonObject.getString("type"),
                            severity = jsonObject.getInt("severity"),
                            latitude = jsonObject.getDouble("latitude"),
                            longitude = jsonObject.getDouble("longitude"),
                            timestamp = jsonObject.getLong("timestamp")
                        )
                        
                        handleIncidentReceived(incident)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                ?.send()
                ?.whenComplete { _, throwable: Throwable? ->
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setOnIncidentReceived(callback: (Incident) -> Unit) {
        onIncidentReceived = callback
    }

    fun disconnect() {
        try {
            if (mqttClient != null && isConnected) {
                mqttClient?.disconnect()?.whenComplete { _, throwable: Throwable? ->
                    isConnected = false
                }
            } else {
                isConnected = false
            }
        } catch (e: Exception) {
            isConnected = false
        }
    }

    fun isNearby(userLat: Double, userLon: Double, incidentLat: Double, incidentLon: Double): Boolean {
        val distance = calculateDistance(userLat, userLon, incidentLat, incidentLon)
        return distance <= Constants.NEARBY_RADIUS_KM
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadiusKm * c
    }

    private fun handleIncidentReceived(incident: Incident) {
        val ctx = context ?: return
        
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(ctx)
            val locationTask = fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            
            locationTask.addOnSuccessListener { location ->
                if (location != null) {
                    val userLat = location.latitude
                    val userLon = location.longitude
                    
                    if (isNearby(userLat, userLon, incident.latitude, incident.longitude)) {
                        val distance = calculateDistance(userLat, userLon, incident.latitude, incident.longitude)
                        
                        IncidentManager.addIncident(incident)
                        
                        mainHandler.post {
                            onIncidentReceived?.invoke(incident)
                        }
                        
                        IncidentNotificationManager.showIncidentNotification(ctx, incident, distance)
                    }
                } else {
                    IncidentManager.addIncident(incident)
                    mainHandler.post {
                        onIncidentReceived?.invoke(incident)
                    }
                }
            }.addOnFailureListener {
                IncidentManager.addIncident(incident)
                mainHandler.post {
                    onIncidentReceived?.invoke(incident)
                }
            }
        } catch (e: Exception) {
            IncidentManager.addIncident(incident)
            mainHandler.post {
                onIncidentReceived?.invoke(incident)
            }
        }
    }
}
