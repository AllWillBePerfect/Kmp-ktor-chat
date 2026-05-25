package org.example.project.platform

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

interface DatastoreProvider {

    fun provide(producePath: () -> String) : DataStore<Preferences>

}