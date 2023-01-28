package net.mbonnin.appengine

import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import org.apache.commons.lang3.StringEscapeUtils

enum class OS {
    ANDROID, IOS
}
class SlackMessageBuilder(val starRating: Int,
                          val os: OS,
                          val language: String? = null, // in the form en_us
                          val userName: String?,
                          val appVersion: String?,
                          val apiLevel: Int? = null,
                          val device: String? = null,
                          val originalText: String,
                          val seconds: Long) {

    private fun username(): String {
        if (!userName.isNullOrBlank()) {
            return " *$userName*"
        } else {
            return ""
        }
    }
    private fun text(): String {
        val list = mutableListOf<String>()
        val os = when(os) {
            OS.ANDROID -> ":android:"
            OS.IOS -> ":apple:"
        }
        list.add(os)
        list.add("${stars()} ${flag()} ${username()}")
        if (appVersion != null) {
            list.add("_version_: $appVersion")
        }
        if (apiLevel != null) {
            list.add("_apiLevel_: $apiLevel")
        }
        if (device != null) {
            list.add("_device_: $device")
        }
        return list.joinToString("\n")
    }

    private fun stars(): String {
        return "★".repeat(starRating) + "☆".repeat(5 - starRating)
    }

    private fun flag(): String {
        return language?.let {
            ":flag-${it.lowercase()}:"
        } ?: ""
    }

    private fun color(): String {
        return when (starRating) {
            in 0..1 -> "#f0603a"
            in 2..3 -> "#fac717"
            else -> "36a64f"
        }
    }

    private fun encode(text: String): String {
        return text
    }
    fun build(): Map<String, Any> {

        val attachments = mutableListOf<Map<String, String>>()

        if (language?.contains("fr", ignoreCase = true) == false) {
            val translatedText = translatedText()
            if (translatedText != null) {
                attachments.add(
                        mapOf("text" to encode(translatedText),
                                "color" to color())
                )
            }
        }
        attachments.add(
                mapOf("text" to encode(originalText),
                        "color" to color(),
                        "ts" to seconds.toString())
        )

        return mapOf(
                "text" to text(),
                "attachments" to attachments,
        )
    }

    private fun translatedText(): String? {
        val translate = TranslateOptions.getDefaultInstance().service

        val translation = translate.translate(
                originalText,
                Translate.TranslateOption.targetLanguage("fr"))

        return StringEscapeUtils.unescapeHtml4(translation.translatedText)
    }
}