package net.mbonnin.appengine

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.PemReader
import com.google.api.client.util.SecurityUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okio.Okio
import java.io.StringReader
import java.security.spec.PKCS8EncodedKeySpec
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
        val map = resourceAsMap("/config.json")
        if (map == null) {
            return null
        }
        val packageName = map.get("packageName")
        val incomingWebHook = map.get("incomingWebHook")

        if (packageName != null && incomingWebHook != null) {
            return Config(packageName, incomingWebHook)
        } else {
            return null
        }
    }

    override fun service(req: HttpServletRequest, resp: HttpServletResponse) {
        System.out.println("service=${req.servletPath} pathInfo=${req.pathInfo}")

        val config = config()
        if (config == null) {
            resp.sendError(500)
            resp.writer.write("Cannot find config, make sure you add a config.json file in your resources")
            return
        }
        val map = resourceAsMap("/secret.json")
        if (map == null) {
            resp.sendError(500)
            resp.writer.write("Cannot find secret, make sure you add a secret.json file in your resources")
            return
        }

        val email = map["client_email"]
        if (email == null) {
            resp.sendError(500)
            resp.writer.write("Cannot find client_email")
            return
        }
        val privateKey = map["private_key"]
        if (privateKey == null) {
            resp.sendError(500)
            resp.writer.write("Cannot find private_key")
            return
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
                resp.sendError(500)
                resp.writer.write("Cannot refresh token")
                return
            }
        }
        accessToken = credential.accessToken

        val reviews = GooglePlayApi.getReviews(accessToken, config.packageName)
        if (reviews == null) {
            resp.sendError(500)
            System.out.println("cannot get reviews")
            resp.writer.write("Cannot get reviews")
            return
        }

        val lastSeconds = DataStore.readSeconds() ?: 0L
        var maxSeconds = 0L
        System.out.println("lastSeconds=$lastSeconds")

        for (review in reviews.reversed()) { // start with the last comment first
            val userComment = review.comments.firstOrNull()?.userComment
            if (userComment == null) {
                continue
            }

            if (userComment.lastModified.seconds <= lastSeconds) {
                continue
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
                    seconds = userComment.lastModified.seconds)
                    .build()
            Slack.sendMessage(message, config.incomingWebHook)
        }

        if (maxSeconds > 0) {
            System.out.println("maxSeconds=$maxSeconds")
            DataStore.writeSeconds(maxSeconds)
        }

        resp.status = 200
        resp.writer.write("Hello World.")
    }
}

