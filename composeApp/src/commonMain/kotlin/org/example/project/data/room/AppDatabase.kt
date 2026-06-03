package org.example.project.data.room

import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.SQLiteConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [MessageEntity::class],
    version = 2
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}

@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

fun getRoomDatabase(
    builder: RoomDatabase.Builder<AppDatabase>
): AppDatabase {
    return builder
        .addMigrations(MIGRATION_1_2)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        val statement = connection.prepare(
            """
            ALTER TABLE messages
            ADD COLUMN clientMessageId TEXT
            """.trimIndent()
        )
        try {
            statement.step()
        } finally {
            statement.close()
        }
    }
}

@Dao
interface MessageDao {
    @Query(
        """
        SELECT * FROM messages
        WHERE chatId = :chatId
        ORDER BY createdAt ASC
        """
    )
    fun observeMessages(chatId: String): Flow<List<MessageEntity>>

    @Query(
        """
        SELECT * FROM messages
        WHERE syncStatus IN (:statuses)
        ORDER BY createdAt ASC
        """
    )
    suspend fun getMessagesBySyncStatuses(statuses: List<String>): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<MessageEntity>)

    @Query("SELECT * FROM messages WHERE localId = :localId LIMIT 1")
    suspend fun getByLocalId(localId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE clientMessageId = :clientMessageId LIMIT 1")
    suspend fun getByClientMessageId(clientMessageId: String): MessageEntity?

    @Query(
        """
        UPDATE messages
        SET syncStatus = :syncStatus,
            retryCount = :retryCount,
            lastSyncError = :lastSyncError
        WHERE localId = :localId
        """
    )
    suspend fun updateSyncState(
        localId: String,
        syncStatus: String,
        retryCount: Int,
        lastSyncError: String?
    )

    @Query(
        """
        UPDATE messages
        SET remoteId = :remoteId,
            createdAt = :createdAt,
            syncStatus = :syncStatus,
            lastSyncError = NULL
        WHERE localId = :localId
        """
    )
    suspend fun markCompleted(
        localId: String,
        remoteId: String,
        createdAt: String,
        syncStatus: String = "COMPLETED"
    )
}

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val localId: String,
    val remoteId: String?,
    val clientMessageId: String?,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val createdAt: String,
    val syncStatus: String,
    val retryCount: Int = 0,
    val lastSyncError: String? = null,
)
