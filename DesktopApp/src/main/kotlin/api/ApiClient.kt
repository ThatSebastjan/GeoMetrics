package api

import com.mongodb.ConnectionString
import io.github.cdimascio.dotenv.dotenv
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import models.Earthquake
import models.FireStation
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody



object ApiClient {
    private val client = OkHttpClient()

    private val dotenv = dotenv()
    private val BASE_URL = ConnectionString(
        dotenv["API_URL"] ?: "http://localhost:3000/"
    )

    private val json = Json { ignoreUnknownKeys = true }

    fun getEarthquakes(): List<Earthquake> {
        val request = Request.Builder()
            .url("$BASE_URL/earthquakes")
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: "[]"
            return json.decodeFromString(body)
        }
    }

    fun getFireStations(): List<FireStation> {
        val request = Request.Builder()
            .url("$BASE_URL/firestations")
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: "[]"
            return json.decodeFromString(body)
        }
    }

    fun addEarthquake(eq: Earthquake): Earthquake {
        val requestBody = json.encodeToString(eq)
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$BASE_URL/earthquakes")
            .post(requestBody)
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: "{}"
            return json.decodeFromString(body)
        }
    }

    fun addFireStation(fs: FireStation): FireStation {
        val requestBody = json.encodeToString(fs)
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$BASE_URL/firestations")
            .post(requestBody)
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: "{}"
            return json.decodeFromString(body)
        }
    }
}