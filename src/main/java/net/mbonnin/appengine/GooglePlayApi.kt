package net.mbonnin.appengine

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.AndroidPublisherScopes
import com.google.api.services.androidpublisher.model.Review
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials

object GooglePlayApi {
    fun getReviews(packageName: String): List<Review> {
        val resourceStream = this::class.java.getResourceAsStream("/secret.json")
        val credentials = GoogleCredentials.fromStream(resourceStream)
            .createScoped(AndroidPublisherScopes.ANDROIDPUBLISHER)

        val androidPublisher = AndroidPublisher.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            HttpCredentialsAdapter(credentials)
        )
            .setApplicationName(packageName)
            .build()

        val reviews = androidPublisher.reviews().list(packageName).execute()
        return reviews.reviews
    }
}