package net.mbonnin.appengine

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import net.mbonnin.appengine.DataStore.KEY_APPLE
import net.mbonnin.appengine.DataStore.KEY_GOOGLE
import okio.buffer
import okio.source
import java.util.*
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


class MainServlet : HttpServlet() {
    private val moshi = Moshi.Builder().build()!!

    private val mapAdapter by lazy {
        val type =
            Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
        moshi.adapter<Map<String, String>>(type)
    }

    private fun resourceAsMap(path: String): Map<String, String>? {
        val inputStream = this::class.java.getResourceAsStream(path) ?: return null
        val source = inputStream.source().buffer()
        return source.use {
            mapAdapter.fromJson(it)!!
        }

    }

    private fun config(): Config? {
        val inputStream = this::class.java.getResourceAsStream("/config.json") ?: return null
        val source = inputStream.source().buffer()
        return source.use {
            moshi.adapter(Config::class.java).fromJson(it)!!
        }
    }


    override fun service(req: HttpServletRequest, resp: HttpServletResponse) {
        println("service=${req.servletPath} pathInfo=${req.pathInfo}")

        val config = config() ?: throw (Exception("Cannot get config"))

        sendGoogle(resp, config.packageName, config.incomingWebHook)
        sendApple(resp, config.itunesAppId, config.incomingWebHook)

        resp.status = 200
        resp.writer.write("App reviews updated")
    }

    companion object {

        // resp is optional so we can run this in unit tests
        internal fun sendApple(resp: HttpServletResponse?, itunesAppId: String, incomingWebHook: String) {
            val reviews = ItunesApi.getReviews(itunesAppId) ?: throw (Exception("Cannot get apple reviews"))

            val lastId = try {
                DataStore.readSeconds(KEY_APPLE) ?: 0L
            } catch (e: Exception) {
                e.printStackTrace()
                0L
            }
            var maxId = 0L
            println("lastId=$lastId")

            if (reviews.isEmpty()) {
                return
            }

            reviews.sortedBy { it.id } // start with the last comment first
                .forEach { review ->
                    if (review.id <= lastId) {
                        return@forEach
                    }

                    if (review.id > maxId) {
                        maxId = review.id
                    }
                    Slack.sendMessage(review.toSlackMessage(), incomingWebHook)
                }

            if (maxId > 0) {
                println("maxId=$maxId")
                try {
                    DataStore.writeSeconds(KEY_APPLE, maxId)
                } catch (e: Exception) {
                    //
                }
            }
        }
    }

    internal fun sendGoogle(
        resp: HttpServletResponse,
        packageName: String,
        incomingWebHook: String
    ) {
        val map = resourceAsMap("/secret.json")
        if (map == null) {
            resp.sendError(500)
            resp.writer.write("Cannot find secret, make sure you add a secret.json file in your resources")
            return
        }

        val reviews = GooglePlayApi.getReviews(packageName)
        if (reviews.isEmpty()) {
            return
        }

        val lastSeconds = DataStore.readSeconds(KEY_GOOGLE) ?: 0L
        var maxSeconds = 0L
        println("lastSeconds=$lastSeconds")

        reviews.reversed() // start with the last comment first
            .forEach { review ->
                val userComment = review.comments.firstOrNull()?.userComment ?: return@forEach

                if (userComment.lastModified.seconds <= lastSeconds) {
                    return@forEach
                }

                if (userComment.lastModified.seconds > maxSeconds) {
                    maxSeconds = userComment.lastModified.seconds
                }

                Slack.sendMessage(review.toSlackMessage()!!, incomingWebHook)
            }

        if (maxSeconds > 0) {
            println("maxSeconds=$maxSeconds")
            DataStore.writeSeconds(KEY_GOOGLE, maxSeconds)
        }
    }
}

fun com.google.api.services.androidpublisher.model.Review.toSlackMessage() : Map<String, Any>? {
    return comments.firstOrNull()?.let {
        SlackMessageBuilder(
            starRating = it.userComment.starRating,
            os = OS.ANDROID,
            language = it.userComment.reviewerLanguage,
            userName = authorName,
            appVersion = it.userComment.appVersionName,
            apiLevel = it.userComment.androidOsVersion,
            device = it.userComment.device,
            originalText = it.userComment.text,
            seconds = it.userComment.lastModified.seconds
        ).build()
    }
}

fun ItunesApi.Review.toSlackMessage(): Map<String, Any> {
    return SlackMessageBuilder(
        starRating = rating.toInt(),
        os = OS.IOS,
        userName = author,
        appVersion = version,
        originalText = "${title}\n${content}",
        seconds = Date().time / 1000,
        language = language,
        apiLevel = null,
        device = null
    ).build()
}





