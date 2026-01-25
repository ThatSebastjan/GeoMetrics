package com.geometrics.app.managers

import android.content.Context
import android.content.SharedPreferences
import com.geometrics.app.models.Incident
import com.geometrics.app.Constants
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import android.os.Handler
import android.os.Looper

object IncidentManager {
    private const val PREFS_NAME = "incidents_prefs"
    private const val KEY_INCIDENTS = "incidents"

    private var sharedPrefs: SharedPreferences? = null
    private val incidents = mutableListOf<Incident>()
    private val client = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())

    fun initialize(context: Context) {
        if (sharedPrefs == null) {
            sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadIncidents()
            println("IncidentManager initialized. Loaded ${incidents.size} incidents from local storage")
        }
    }

    fun addIncident(incident: Incident) {
        incidents.add(incident)
        saveIncidents()
        println("IncidentManager: Added incident ${incident.id} (${incident.type}). Total: ${incidents.size}")
    }

    fun getAllIncidents(): List<Incident> {
        println("IncidentManager: getAllIncidents() called. Returning ${incidents.size} incidents")
        return incidents.toList()
    }

    fun getIncidentById(id: String): Incident? = incidents.find { it.id == id }

    fun clearAll() {
        incidents.clear()
        saveIncidents()
    }

    fun syncFromBackend(onComplete: ((success: Boolean, count: Int) -> Unit)? = null) {
        syncFromBlockchain(onComplete)
    }

    private fun syncFromBlockchain(onComplete: ((success: Boolean, count: Int) -> Unit)? = null) {
        val blockchainRequest = Request.Builder()
            .url("${Constants.BACKEND_URL}/report/blockchain")
            .get()
            .build()

        client.newCall(blockchainRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Failed to fetch from blockchain: ${e.message}")
                handler.post {
                    onComplete?.invoke(false, 0)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        val responseBody = it.body?.string()
                        println("Fetched from blockchain: ${responseBody?.length ?: 0} bytes")
                        
                        try {
                            val jsonArray = JSONArray(responseBody)
                            val blockchainIncidents = mutableListOf<Incident>()
                            
                            for (i in 0 until jsonArray.length()) {
                                val obj = jsonArray.getJSONObject(i)
                                val incident = Incident(
                                    id = obj.getString("id"),
                                    type = obj.getString("type"),
                                    severity = obj.getInt("severity"),
                                    latitude = obj.getDouble("latitude"),
                                    longitude = obj.getDouble("longitude"),
                                    timestamp = obj.getLong("timestamp")
                                )
                                blockchainIncidents.add(incident)
                            }
                            
                            println("Parsed ${blockchainIncidents.size} incidents from blockchain")
                            
                            incidents.clear()
                            incidents.addAll(blockchainIncidents)
                            
                            println("Loaded ${incidents.size} user-reported incidents from blockchain")
                            saveIncidents()
                            
                            handler.post {
                                onComplete?.invoke(true, incidents.size)
                            }
                        } catch (e: Exception) {
                            println("Error parsing blockchain response: ${e.message}")
                            e.printStackTrace()
                            handler.post {
                                onComplete?.invoke(false, 0)
                            }
                        }
                    } else {
                        println("Blockchain returned status ${it.code}")
                        handler.post {
                            onComplete?.invoke(false, 0)
                        }
                    }
                }
            }
        })
    }

    private fun saveIncidents() {
        val jsonArray = JSONArray()
        incidents.forEach { incident ->
            val obj = JSONObject().apply {
                put("id", incident.id)
                put("type", incident.type)
                put("severity", incident.severity)
                put("latitude", incident.latitude)
                put("longitude", incident.longitude)
                put("timestamp", incident.timestamp)
            }
            jsonArray.put(obj)
        }
        sharedPrefs?.edit()?.putString(KEY_INCIDENTS, jsonArray.toString())?.apply()
    }

    private fun loadIncidents() {
        incidents.clear()
        val jsonString = sharedPrefs?.getString(KEY_INCIDENTS, null) ?: return

        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val incident = Incident(
                    id = obj.getString("id"),
                    type = obj.getString("type"),
                    severity = obj.getInt("severity"),
                    latitude = obj.getDouble("latitude"),
                    longitude = obj.getDouble("longitude"),
                    timestamp = obj.getLong("timestamp")
                )
                incidents.add(incident)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
