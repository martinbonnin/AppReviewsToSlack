package net.mbonnin.appengine

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.net.HttpURLConnection
import java.net.URL

object Itunes {
    data class Review(val author: String,
                      val version: String,
                      val rating: String,
                      val title: String,
                      val content: String,
                      val id: Long)

    fun getReviews(appId: String): List<Review>? {
        val url = URL("https://itunes.apple.com/us/rss/customerreviews/id=${appId}/sortBy=mostRecent/json")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.doInput = true

        connection.connect()

        val status = connection.responseCode
        if (status != HttpURLConnection.HTTP_OK) {
            val error = connection.inputStream.bufferedReader().readText()
            System.out.println("cannot get reviews: $error")
            return null
        }

        val moshi = Moshi.Builder().build()
        val adapter = moshi.adapter<Map<String, Any>>(Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java))

        val contents = connection.inputStream.bufferedReader().readText()
        val response = adapter.fromJson(contents)

        val feed = response!!.getObject("feed")
        val entry = feed.getList("entry")

        return entry.map {
            Review(
                    author = it.getObject("author").getObject("name").getString("label"),
                    version = it.getObject("im:version").getString("label"),
                    rating = it.getObject("im:rating").getString("label"),
                    title = it.getObject("title").getString("label"),
                    content = it.getObject("content").getString("label"),
                    id = it.getObject("id").getString("label").toLong()
            )
        }
    }
}

fun <K, V> Map<K, V>.getObject(key: K): Map<String, Any> {
    return get(key) as Map<String, Any>
}

fun <K, V> Map<K, V>.getList(key: K): List<Map<String, Any>> {
    return get(key) as List<Map<String, Any>>
}

fun <K, V> Map<K, V>.getString(key: K): String {
    return get(key) as String
}