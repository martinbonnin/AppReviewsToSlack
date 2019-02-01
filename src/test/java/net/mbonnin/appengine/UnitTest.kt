package net.mbonnin.appengine

import org.junit.Test

class UnitTest {
    @Test
    fun testSlack() {
        val message = SlackMessageBuilder(starRating = 5,
                language = "en_FR",
                userName = "mbonnin",
                appVersion = "1.0",
                apiLevel = 28,
                device = "Pixel 3",
                originalText = "Trop d'la balle !",
                accessToken = "",
                seconds = 1543860579,
                channel = "android-reviews")
                .build()
        Slack.sendMessage(message, "")
    }

    @Test
    fun itunesTest() {
        MainServlet.sendApple(null, "336978041", "")
    }
}