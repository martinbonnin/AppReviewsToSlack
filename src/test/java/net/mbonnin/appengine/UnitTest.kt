package net.mbonnin.appengine

import org.junit.Test

class UnitTest {
    @Test
    fun test() {
        val message = SlackMessageBuilder(starRating = 5,
                language = "en_FR",
                userName = "mbonnin",
                appVersion = "1.0",
                apiLevel = 28,
                device = "Pixel 3",
                originalText = "Trop d'la balle !",
                accessToken = "",
                seconds = 1543860579)
                .build()
        Slack.sendMessage(message, "")
    }
}