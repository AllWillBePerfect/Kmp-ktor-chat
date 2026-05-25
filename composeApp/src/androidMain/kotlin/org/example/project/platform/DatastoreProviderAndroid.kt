package org.example.project.platform

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences

class DatastoreProviderAndroid(
    private val appContext: Application
) : DatastoreProvider {
    private var dataStore: DataStore<Preferences>? = null
    private val lock = Any()

    override fun provide(producePath: () -> String): DataStore<Preferences> {
        return synchronized(lock) {
            dataStore ?: run {
                val file = appContext.filesDir.resolve(producePath())

                PreferenceDataStoreFactory.create(
                    produceFile = { file }
                ).also { dataStore = it }
            }
        }
    }
}