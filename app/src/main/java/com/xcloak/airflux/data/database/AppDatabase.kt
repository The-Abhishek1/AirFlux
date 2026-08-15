package com.xcloak.airflux.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.xcloak.airflux.data.database.dao.HistoryDao
import com.xcloak.airflux.data.database.entity.HistoryEntity
import com.xcloak.airflux.data.database.entity.HistoryType

class Converters {
    @TypeConverter
    fun fromHistoryType(value: HistoryType): String = value.name

    @TypeConverter
    fun toHistoryType(value: String): HistoryType = HistoryType.valueOf(value)
}

@Database(entities = [HistoryEntity::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "airflux_db")
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}