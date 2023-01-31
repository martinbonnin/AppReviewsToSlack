package net.mbonnin.appengine

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.net.HttpURLConnection
import java.net.URL

object ItunesApi {
    data class Review(val author: String,
                      val version: String,
                      val rating: String,
                      val title: String,
                      val content: String,
                      val id: Long,
                      val language: String)

    val countries = arrayOf(
            "DZ",
            "AO",
            //"AI",
            //"AG",
            "AR",
            //"AM",
            "AU",
            "AT",
            "AZ",
            "BH",
            //"BD",
            //"BB",
            //"BY",
            "BE",
            //"BZ",
            //"BM",
            //"BO",
            //"BW",
            "BR",
            //"VG",
            //"BN",
            "BG",
            "CA",
            //"KY",
            "CL",
            "CN",
            "CO",
            "CR",
            "CI",
            "HR",
            //"CY",
            "CZ",
            "DK",
            //"DM",
            "DO",
            "EC",
            "EG",
            //"SV",
            //"EE",
            "FI",
            "FR",
            "DE",
            //"GH",
            "GR",
            //"GD",
            "GT",
            //"GY",
            "HN",
            "HK",
            "HU",
            //"IS",
            "IN",
            "ID",
            "IE",
            "IL",
            "IT",
            //"JM",
            "JP",
            "JO",
            //"KZ",
            //"KE",
            "KR",
            "KW",
            //"LV",
            "LB",
            //"LI",
            //"LT",
            "LU",
            "MO",
            //"MK",
            //"MG",
            "MY",
            "MV",
            //"ML",
            "MT",
            //"MU",
            "MX",
            //"MD",
            //"MS",
            //"NP",
            "NL",
            "NZ",
            //"NI",
            //"NE",
            "NG",
            "NO",
            "OM",
            "PK",
            "PA",
            //"PY",
            "PE",
            "PH",
            "PL",
            "PT",
            "QA",
            "RO",
            "RU",
            "SA",
            //"SN",
            "RS",
            "SG",
            //"SK",
            "SI",
            "ZA",
            "ES",
            "LK",
            //"KN",
            //"LC",
            //"VC",
            //"SR",
            "SE",
            "CH",
            "TW",
            //"TZ",
            "TH",
            //"BS",
            //"TT",
            //"TN",
            "TR",
            //"TC",
            //"UG",
            "GB",
            "UA",
            "AE",
            //"UY",
            "US",
            //"UZ",
            "VE",
            "VN",
            "YE"
    )

    fun getReviews(appId: String): List<Review>? {
        val list = mutableListOf<Review>()

        for (country in countries) {
            ItunesApi.getReviews(appId, country)?.let { list.addAll(it) }
        }
        return if (list.size > 50) {
            list.sortedBy { it.id }.takeLast(50)
        } else {
            list
        }
    }

    fun getReviews(appId: String, country: String): List<Review>? {
        val itunesUrl = "https://itunes.apple.com/${country.lowercase()}/rss/customerreviews/id=${appId}/sortBy=mostRecent/json"
        val url = URL(itunesUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.doInput = true

        connection.connect()

        val status = connection.responseCode
        if (status != HttpURLConnection.HTTP_OK) {
            val error = connection.errorStream?.bufferedReader()?.readText()
            System.out.println("$url: cannot get reviews: $error")
            return null
        }

        val moshi = Moshi.Builder().build()
        val adapter = moshi.adapter<Map<String, Any>>(Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java))

        val contents = connection.inputStream.bufferedReader().readText()
        val response = adapter.fromJson(contents)

        try {
            val feed = response!!.getObject("feed")
            val entry = feed.getListOrNull("entry")

            if (entry == null) {
                println("no entry for $country")
                return null
            }
            return entry.map {
                Review(
                        author = it.getObject("author").getObject("name").getString("label"),
                        version = it.getObject("im:version").getString("label"),
                        rating = it.getObject("im:rating").getString("label"),
                        title = it.getObject("title").getString("label"),
                        content = it.getObject("content").getString("label"),
                        id = it.getObject("id").getString("label").toLong(),
                        language = country.lowercase()
                )
            }
        } catch (e: Exception) {
            System.err.println("Error processing $itunesUrl")
            e.printStackTrace()
            return null
        }

    }
}

fun <K, V> Map<K, V>.getObject(key: K): Map<String, Any> {
    return get(key) as Map<String, Any>
}

fun <K, V> Map<K, V>.getList(key: K): List<Map<String, Any>> {
    return get(key) as List<Map<String, Any>>
}

fun <K, V> Map<K, V>.getListOrNull(key: K): List<Map<String, Any>>? {
    return get(key) as List<Map<String, Any>>?
}

fun <K, V> Map<K, V>.getString(key: K): String {
    return get(key) as String
}