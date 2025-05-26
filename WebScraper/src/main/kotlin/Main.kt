import it.skrape.core.document
import it.skrape.core.htmlDocument
import it.skrape.fetcher.*
import it.skrape.selects.html5.*
import kotlinx.coroutines.*
import kotlin.math.ceil


data class FireStation(
    val longitude: Double,
    val latitude: Double,
    val location: String,
    val address: String,
    val city: String,
    val description: String,
    val telephoneNum: String
)

data class EarthQuake(
    val timestamp: Number,
    val magnitude: Double,
    val depth: Double,
    val longitude: Double,
    val latitude: Double
)



fun scrapeFireStations(): List<FireStation> {
    val list = mutableListOf<FireStation>()

    skrape(HttpFetcher) {
        request {
            url = "https://gasilec.net/wp-content/plugins/superstorefinder-wp/ssf-wp-xml.php?wpml_lang=&t=1748199266390"
        }

        response {
            document.findFirst("locator").findFirst("store").children.forEach {
                val loc = it.findFirst("location").text
                val address = it.findFirst("address").text.trim()
                val city = it.findFirst("city").text.trim()
                val latitude = it.findFirst("latitude").text.toDouble()
                val longitude = it.findFirst("longitude").text.toDouble()
                val description = it.findFirst("description").text
                val telephoneNum = it.findFirst("telephone").text

                list.add(FireStation(longitude, latitude, loc, address, city, description, telephoneNum))
            }
        }
    }

    return list
}



fun scrapeEarthQuakes(): List<EarthQuake> {
    val list = mutableListOf<EarthQuake>()

    suspend fun scrapePos(pageUrl: String): Pair<Double, Double> {
        var result = Pair(0.0, 0.0)

        skrape(HttpFetcher) {
            request {
                url = pageUrl
            }

            response {
                htmlDocument {
                    val tbody = findFirst("tbody")
                    val lon = tbody.children[3].children[1].text.toDouble()
                    val lat = tbody.children[4].children[1].text.toDouble()

                    result = Pair(lon, lat)
                }
            }
        }

        return result
    }

    runBlocking {
        skrape(HttpFetcher) {
            request {
                url = "https://www.potresi.com/country/slovenia"
            }

            response {
                htmlDocument {

                    val results = mutableListOf<Deferred<EarthQuake>>()
                    val earthquakes = findFirst("tbody").children

                    val batchSize = 20

                    // Process earthquakes in batches
                    earthquakes.chunked(batchSize).forEachIndexed { batchIndex, batch ->

                        println("Grabbing earthquake batch $batchIndex / ${ceil((earthquakes.size / batchSize).toDouble())}")

                        // Wait for the current batch to complete before starting the next
                        val batchResults = batch.map { earthquake ->
                            async(Dispatchers.IO) {

                                val timestamp = earthquake.children[0].attribute("data-order").toLong() * 1000
                                val magnitude = earthquake.children[1].findFirst("span").text.toDouble()
                                val detailsUrl = earthquake.children[2].findFirst("a").attribute("href")
                                val depth = earthquake.children[3].text.split(" ")[0].toDouble()
                                val coords = scrapePos(detailsUrl)

                                EarthQuake(timestamp, magnitude, depth, coords.first, coords.second)
                            }
                        }
                        results.addAll(batchResults)
                        runBlocking {
                            batchResults.awaitAll() // Wait for current batch to complete before starting next batch
                        }
                    }
                    // Collect all results
                    runBlocking {
                        list.addAll(results.awaitAll())
                    }
                }
            }
        }
    }

    return list
}



fun main() {
    val fireStations = scrapeFireStations()
    for (f in fireStations) println(f)

    val earthQuakes = scrapeEarthQuakes()
    for (e in earthQuakes) println(e)
}