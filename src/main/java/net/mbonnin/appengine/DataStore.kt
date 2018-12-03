package net.mbonnin.appengine

import com.google.cloud.datastore.DatastoreOptions
import com.google.cloud.datastore.Entity

object DataStore {
    val datastore = DatastoreOptions.getDefaultInstance().service
    val keyFactory = datastore.newKeyFactory().setKind("date")
    val key = keyFactory.newKey("last")

    fun readSeconds(): Long? {
        val entity = datastore.get(key)
        if (entity == null) {
            System.out.println("cannot read seconds")
            return null
        }

        return entity.getLong("seconds")
    }

    fun writeSeconds(seconds: Long) {
        val entity = Entity.newBuilder(key)
                .set("seconds", seconds)
                .build()

        datastore.put(entity)
    }
}