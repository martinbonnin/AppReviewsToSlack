package net.mbonnin.appengine

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.PemReader
import com.google.api.client.util.SecurityUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import net.mbonnin.appengine.DataStore.KEY_APPLE
import net.mbonnin.appengine.DataStore.KEY_GOOGLE
import okio.Okio
import java.io.StringReader
import java.lang.Exception
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


class MainServlet : HttpServlet() {
    private val moshi = Moshi.Builder().build()!!

    private val mapAdapter by lazy {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
        moshi.adapter<Map<String, String>>(type)
    }

    fun resourceAsMap(path: String): Map<String, String>? {
        val inputStream = this::class.java.getResourceAsStream(path)
        if (inputStream == null) {
            return null
        }
        val source = Okio.buffer(Okio.source(inputStream))
        return source.use {
            mapAdapter.fromJson(it)!!
        }

    }

    fun config(): Config? {
        val inputStream = this::class.java.getResourceAsStream("/config.json")
        if (inputStream == null) {
            return null
        }
        val source = Okio.buffer(Okio.source(inputStream))
        return source.use {
            moshi.adapter(Config::class.java).fromJson(it)!!
        }
    }

    private fun sendGoogle(resp: HttpServletResponse, packageName: String, incomingWebHook: String) {
        val map = resourceAsMap("/secret.json")
        if (map == null) {
            resp.sendError(500)
            resp.writer.write("Cannot find secret, make sure you add a secret.json file in your resources")
            return
        }

        val email = map["client_email"]
        if (email == null) {
            throw(Exception("Cannot find client_email"))
        }
        val privateKey = map["private_key"]
        if (privateKey == null) {
            throw(Exception("Cannot find private_key"))
        }

        val bytes = PemReader.readFirstSectionAndClose(StringReader(privateKey), "PRIVATE KEY").base64DecodedBytes
        val privKey = SecurityUtils.getRsaKeyFactory().generatePrivate(PKCS8EncodedKeySpec(bytes));

        val credential = GoogleCredential.Builder()
                .setTransport(GoogleNetHttpTransport.newTrustedTransport())
                .setJsonFactory(JacksonFactory.getDefaultInstance())
                .setServiceAccountId(email)
                .setServiceAccountScopes(listOf(
                        "https://www.googleapis.com/auth/androidpublisher"
                ))
                .setServiceAccountPrivateKey(privKey)
                .build()

        var accessToken = credential.accessToken
        if (accessToken == null) {
            val success = credential.refreshToken()
            if (!success) {
                throw(Exception("Cannot refresh token"))
            }
        }
        accessToken = credential.accessToken

        val reviews = GooglePlayApi.getReviews(accessToken, packageName)
        if (reviews == null) {
            throw(Exception("Cannot get google reviews"))
        }

        if (reviews.isEmpty()) {
            return
        }

        val lastSeconds = DataStore.readSeconds(KEY_GOOGLE) ?: 0L
        var maxSeconds = 0L
        System.out.println("lastSeconds=$lastSeconds")

        reviews.reversed() // start with the last comment first
                .forEach { review ->
            val userComment = review.comments.firstOrNull()?.userComment
            if (userComment == null) {
                return@forEach
            }

            if (userComment.lastModified.seconds <= lastSeconds) {
                return@forEach
            }

            if (userComment.lastModified.seconds > maxSeconds) {
                maxSeconds = userComment.lastModified.seconds
            }


            val message = SlackMessageBuilder(starRating = userComment.starRating,
                    language = userComment.reviewerLanguage,
                    userName = review.authorName,
                    appVersion = userComment.appVersionName,
                    apiLevel = userComment.androidOsVersion,
                    device = userComment.device,
                    originalText = userComment.text,
                    accessToken = accessToken,
                    seconds = userComment.lastModified.seconds,
                    channel = "android-reviews")
                    .build()
            Slack.sendMessage(message, incomingWebHook)
        }

        if (maxSeconds > 0) {
            System.out.println("maxSeconds=$maxSeconds")
            DataStore.writeSeconds(KEY_GOOGLE, maxSeconds)
        }
    }

    override fun service(req: HttpServletRequest, resp: HttpServletResponse) {
        System.out.println("service=${req.servletPath} pathInfo=${req.pathInfo}")

        val config = config()
        if (config == null) {
            throw(Exception("Cannot get config"))
        }

        sendGoogle(resp, config.packageName, config.incomingWebHook)
        sendApple(resp, config.itunesAppId, config.incomingWebHook)

        resp.status = 200
        resp.writer.write("Hello World.")
    }

    companion object {
        // resp is optional so we can run this in unit tests
        fun sendApple(resp: HttpServletResponse?, itunesAppId: String, incomingWebHook: String) {
            val reviews = Itunes.getReviews(itunesAppId)
            if (reviews == null) {
                throw(Exception("Cannot get apple reviews"))
            }

            val lastId = try {
                DataStore.readSeconds(KEY_APPLE) ?: 0L
            } catch (e: Exception) {
                e.printStackTrace()
                0L
            }
            var maxId = 0L
            System.out.println("lastId=$lastId")

            if (reviews.isEmpty()) {
                return
            }

            reviews.sortedBy { it.id } // start with the last comment first
                    .forEach {  review ->
                if (review.id <= lastId) {
                    return@forEach
                }

                if (review.id > maxId) {
                    maxId = review.id
                }

                val message = SlackMessageBuilder(starRating = review.rating.toInt(),
                        userName = review.author,
                        appVersion = review.version,
                        originalText = "${review.title}\n${review.content}",
                        seconds = Date().time / 1000,
                        language = review.language,
                        channel = "ios-reviews")
                        .build()
                Slack.sendMessage(message, incomingWebHook)
            }

            if (maxId > 0) {
                System.out.println("maxId=$maxId")
                try {
                    DataStore.writeSeconds(KEY_APPLE, maxId)
                } catch (e: Exception) {
                    //
                }
            }
        }
    }
}

