package api

import com.mongodb.ConnectionString
import com.sun.jdi.InvalidTypeException
import io.github.cdimascio.dotenv.dotenv
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import models.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit


object ApiClient {
    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val dotenv = dotenv()
    val BASE_URL = dotenv["API_URL"] ?: "http://localhost:3001/manegement"

    val ENDPOINT_MAP = mapOf(
        Pair(Earthquake::class, "earthquakes"),
        Pair(FireStation::class, "fireStations"),
        Pair(Flood::class, "floods"),
        Pair(LandLot::class, "landLots"),
        Pair(LandSlide::class, "landSlides"),
        Pair(LandUse::class, "landUse"),
    )

    val json = Json { ignoreUnknownKeys = true }



    inline fun <reified T> count() : Int {
        val endpoint = ENDPOINT_MAP[T::class] ?: throw InvalidTypeException("Invalid type passed to ApiClient.count")

        val request = Request.Builder()
            .url("$BASE_URL/$endpoint/count")
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: "0"
            return body.toInt()
        }
    }



    inline fun <reified T> get(offset: Int, count: Int) : List<T> {
        val endpoint = ENDPOINT_MAP[T::class] ?: throw InvalidTypeException("Invalid type passed to ApiClient.get")

        val request = Request.Builder()
            .url("$BASE_URL/$endpoint/$offset/$count")
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: "[]"
            return json.decodeFromString(body)
        }
    }



    inline fun <reified T> insert(obj: T) : Boolean {
        val endpoint = ENDPOINT_MAP[obj!!::class] ?: throw InvalidTypeException("Invalid type passed to ApiClient.insert")

        val requestBody = json.encodeToString(obj)
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL/$endpoint/insert")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            return response.code == 200
        }
    }


}