package models

import it.skrape.core.document
import it.skrape.core.htmlDocument
import it.skrape.fetcher.*
import it.skrape.selects.*
import kotlinx.coroutines.*
import kotlinx.coroutines.coroutineScope


data class ScrapedFireStation(
    val longitude: Double,
    val latitude: Double,
    val location: String,
    val address: String,
    val city: String,
    val description: String,
    val telephoneNum: String
)

data class ScrapedEarthQuake(
    val timestamp: Number,
    val magnitude: Double,
    val depth: Double,
    val longitude: Double,
    val latitude: Double
)

class Scraper {
    fun scrapeFireStations(): List<ScrapedFireStation> {
        val list = mutableListOf<ScrapedFireStation>()

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

                    list.add(ScrapedFireStation(longitude, latitude, loc, address, city, description, telephoneNum))
                }
            }
        }

        return list
    }

    suspend fun scrapeEarthQuakes(
        latestTimestampInDb: Long = 0,
        onBatchProcessed: ((batchIndex: Int, totalBatches: Int) -> Unit)? = null
    ): List<ScrapedEarthQuake> {
        val earthquakes = mutableListOf<DocElement>()

        skrape(HttpFetcher) {
            request { url = "https://www.potresi.com/country/slovenia" }
            response {
                htmlDocument {
                    val rows = findFirst("tbody").children
                    for (earthquake in rows) {
                        val timestamp = earthquake.children[0].attribute("data-order").toLong() * 1000
                        if (timestamp <= latestTimestampInDb) break
                        earthquakes.add(earthquake)
                    }
                }
            }
        }

        suspend fun scrapePos(pageUrl: String): Pair<Double, Double> {
            var result = Pair(0.0, 0.0)
            skrape(HttpFetcher) {
                request { url = pageUrl }
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

        val batchSize = 20
        val result = mutableListOf<ScrapedEarthQuake>()
        val batches = earthquakes.chunked(batchSize)
        for ((batchIndex, batch) in batches.withIndex()) {
            coroutineScope {
                val batchResults = batch.map { earthquake ->
                    async(Dispatchers.IO) {
                        val timestamp = earthquake.children[0].attribute("data-order").toLong() * 1000
                        val magnitude = earthquake.children[1].findFirst("span").text.toDouble()
                        val detailsUrl = earthquake.children[2].findFirst("a").attribute("href")
                        val depth = earthquake.children[3].text.split(" ")[0].toDouble()
                        val coords = scrapePos(detailsUrl)
                        ScrapedEarthQuake(timestamp, magnitude, depth, coords.first, coords.second)
                    }
                }
                result.addAll(batchResults.awaitAll())
            }
            onBatchProcessed?.invoke(batchIndex + 1, batches.size)
        }
        return result
    }
}