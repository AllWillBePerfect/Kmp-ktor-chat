package org.example.project.platform

import androidx.room.Room
import androidx.room.RoomDatabase
import org.example.project.data.room.AppDatabase
import java.io.File

class RoomDatabaseBuilderProviderJvm : RoomDatabaseBuilderProvider {
    override fun provide(): RoomDatabase.Builder<AppDatabase> {
        val dbFile = File(System.getProperty("java.io.tmpdir"), "my_room.db")
        return Room.databaseBuilder<AppDatabase>(
                name = dbFile.absolutePath,
        )
    }
}