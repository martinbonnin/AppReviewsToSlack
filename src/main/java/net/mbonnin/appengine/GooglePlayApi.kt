package net.mbonnin.appengine

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okio.Okio
import java.net.HttpURLConnection
import java.net.URL

@JsonClass(generateAdapter = true)
data class Timestamp(val seconds: Long)

@JsonClass(generateAdapter = true)
data class UserComment(
        val text: String,
        val lastModified: Timestamp,
        val starRating: Int,
        val reviewerLanguage: String?,
        val device: String?,
        val appVersionName: String?,
        val androidOsVersion: Int?
)

@JsonClass(generateAdapter = true)
data class Comment(
        val userComment: UserComment?
)

@JsonClass(generateAdapter = true)
data class Review(
        val reviewId: String,
        val authorName: String,
        val comments: List<Comment>
)

@JsonClass(generateAdapter = true)
data class Response(val reviews: List<Review>)

object GooglePlayApi {
    fun getReviews(token: String, packageName: String): List<Review>? {
        val url = URL("https://www.googleapis.com/androidpublisher/v3/applications/$packageName/reviews")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.doInput = true

        connection.addRequestProperty("Authorization", "Bearer $token")

        connection.connect()

        val status = connection.responseCode
        if (status != HttpURLConnection.HTTP_OK) {
            val error = connection.inputStream.bufferedReader().readText()
            System.out.println("cannot get reviews: $error")
            return null
        }

        val moshi = Moshi.Builder().build()
        val adapter = moshi.adapter(Response::class.java)

        val contents = connection.inputStream.bufferedReader().readText()
        val response = adapter.fromJson(contents)

        return response?.reviews
    }
}