package org.example.project.platform

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

class DatastoreProviderJvm : DatastoreProvider {
    private var dataStore: DataStore<Preferences>? = null
    private val lock = Any()

    override fun provide(producePath: () -> String): DataStore<Preferences> {
        return synchronized(lock) {
            dataStore ?: run {
                val path = producePath().toPath()
                PreferenceDataStoreFactory.createWithPath(
                    produceFile = { path }
                ).also { dataStore = it }
            }
        }
    }
}