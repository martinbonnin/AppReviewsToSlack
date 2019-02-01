package net.mbonnin.appengine

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Config(val packageName: String, val incomingWebHook: String, val itunesAppId: String)
