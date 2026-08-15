package com.xcloak.airflux.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.xcloak.airflux.data.database.dao.ChatDao
import com.xcloak.airflux.data.database.dao.HistoryDao
import com.xcloak.airflux.data.database.entity.ChatChannel
import com.xcloak.airflux.data.database.entity.ChatMessageEntity
import com.xcloak.airflux.data.database.entity.HistoryEntity
import com.xcloak.airflux.data.database.entity.HistoryType

class Converters {
    @TypeConverter
    fun fromHistoryType(value: HistoryType): String = value.name

    @TypeConverter
    fun toHistoryType(value: String): HistoryType = HistoryType.valueOf(value)

    @TypeConverter
    fun fromChatChannel(value: ChatChannel): String = value.name

    @TypeConverter
    fun toChatChannel(value: String): ChatChannel = ChatChannel.valueOf(value)
}

@Database(entities = [HistoryEntity::class, ChatMessageEntity::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "airflux_db")
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}