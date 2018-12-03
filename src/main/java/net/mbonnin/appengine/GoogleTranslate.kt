package net.mbonnin.appengine

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import okio.Okio
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

@JsonClass(generateAdapter = true)
data class Translation(val translatedText: String)
@JsonClass(generateAdapter = true)
data class Data(val translations: List<Translation>)
@JsonClass(generateAdapter = true)
data class TranslationResponse(val data: Data)

object GoogleTranslate {
    private val moshi = Moshi.Builder().build()!!

    private val mapAdapter by lazy {
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        moshi.adapter<Map<String, Any>>(type)
    }

    fun translate(text: String, target: String, accessToken: String): String? {
        val url = URL("https://translation.googleapis.com/language/translate/v2?")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.doInput = true
        connection.doOutput = true

        connection.addRequestProperty("Content-Type", "application/json")
        connection.addRequestProperty("Authorization", "Bearer $accessToken")

        val postMap = mapOf("target" to target,
                "q" to listOf(text))

        val sink = Okio.buffer(Okio.sink(connection.getOutputStream()))
        sink.use {
            mapAdapter.toJson(it, postMap)
            sink.flush()
        }

        val status = connection.responseCode
        if (status != HttpURLConnection.HTTP_OK) {
            val response = connection.getInputStream().bufferedReader().use {
                it.readText()
            }
            System.out.println("cannot translate: $response")
            return null
        }


        return try {
            val translationResponse = Okio.buffer(Okio.source(connection.inputStream)).use {
                moshi.adapter(TranslationResponse::class.java).fromJson(it)
            }

            translationResponse!!.data.translations[0].translatedText
        } catch (e:Exception) {
            e.printStackTrace(System.out)
            return null
        }
    }
}