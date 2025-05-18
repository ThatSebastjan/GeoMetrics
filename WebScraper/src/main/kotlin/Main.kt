import it.skrape.core.htmlDocument
import it.skrape.fetcher.*
import it.skrape.selects.html5.*


data class Mountain(val name: String, val height: Int, val mountainRange: String, val municipality: String)
data class Settlement(val name: String, val municipality: String, val postNumber: String, val postName: String)


fun scrapeMountains(): List<Mountain> {
    val list = mutableListOf<Mountain>()

    skrape(HttpFetcher) {
        request {
            url = "https://en.wikipedia.org/wiki/List_of_mountains_in_Slovenia"
        }

        response {
            htmlDocument {
                tbody {
                    findFirst {
                        findAll("tr").drop(1).map {
                            val name = it.findByIndex(0, "td").text
                            val height = it.findByIndex(1, "td").text.toIntOrNull()
                            val mountainRange = it.findByIndex(2, "td").text
                            val municipality = it.findByIndex(3, "td").text

                            if(height != null) {
                                list.add(Mountain(name, height, when(mountainRange){"" -> "N/A" else -> mountainRange}, when(municipality){"" -> "N/A" else -> municipality}))
                            }
                        }
                    }
                }
            }
        }
    }

    return list
}


fun scrapeSettlements(): List<Settlement> {
    val list = mutableListOf<Settlement>()
    val links = mutableListOf<String>()
    val baseUrl = "https://sl.wikipedia.org"

    skrape(HttpFetcher) {
        request {
            url = "$baseUrl/wiki/Seznam_naselij_v_Sloveniji"
        }

        response {
            htmlDocument {
                findFirst("a.mw-selflink"){
                    parent.children[1].children.forEach {
                        links.add(it.attribute("href"))
                    }
                }
            }
        }
    }


    for(link in links){
        val currentUrl = "$baseUrl$link"
        println("Grabbing: $currentUrl")

        skrape(HttpFetcher) {
            request {
                url = currentUrl
            }

            response {
                htmlDocument {
                    tbody {
                        findFirst {
                            findAll("tr").drop(1).map {

                                val name = it.findByIndex(1, "td").text
                                val municipality = it.findByIndex(2, "td").text
                                val postNumber = it.findByIndex(3, "td").text
                                val postName = when(it.children.size) {
                                    5 -> it.findByIndex(4, "td").text
                                    else -> "N/A"
                                }

                                list.add(Settlement(name, municipality, postNumber, postName))

                            }
                        }
                    }
                }
            }
        }
    }

    return list
}


fun main() {
    val mountains = scrapeMountains()
    for(m in mountains) println(m)

    val settlements = scrapeSettlements()
    for (s in settlements) println(s)
}