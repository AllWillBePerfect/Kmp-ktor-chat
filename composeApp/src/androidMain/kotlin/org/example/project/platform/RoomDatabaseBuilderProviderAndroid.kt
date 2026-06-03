package org.example.project.platform

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import org.example.project.data.room.AppDatabase

class RoomDatabaseBuilderProviderAndroid(
    private val context: Context
) : RoomDatabaseBuilderProvider {
    override fun provide(): RoomDatabase.Builder<AppDatabase> {
        val appContext = context.applicationContext
        val dbFile = appContext.getDatabasePath("my_room.db")
        return Room.databaseBuilder<AppDatabase>(
            context = appContext,
            name = dbFile.absolutePath
        )

    }
}