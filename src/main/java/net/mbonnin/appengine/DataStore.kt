package net.mbonnin.appengine

import com.google.cloud.datastore.DatastoreOptions
import com.google.cloud.datastore.Entity

object DataStore {
    private val datastore = DatastoreOptions.getDefaultInstance().service
    private val keyFactory = datastore.newKeyFactory().setKind("date")
    const val KEY_GOOGLE = "last"
    const val KEY_APPLE = "last_apple"

    fun readSeconds(key: String): Long? {
        val entity = datastore.get(keyFactory.newKey(key))
        if (entity == null) {
            println("cannot read seconds")
            return null
        }

        return entity.getLong("seconds")
    }

    fun writeSeconds(key: String, seconds: Long) {
        val entity = Entity.newBuilder(keyFactory.newKey(key))
                .set("seconds", seconds)
                .build()

        datastore.put(entity)
    }
}