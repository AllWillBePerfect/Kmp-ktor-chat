package org.example.project.platform

import androidx.room.RoomDatabase
import org.example.project.data.room.AppDatabase

interface RoomDatabaseBuilderProvider {

    fun provide(): RoomDatabase.Builder<AppDatabase>
}