package com.example.travelplanner
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Trip::class, Destination::class, Stop::class, Travel::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun getAppDao(): AppDao

    companion object {
        val databaseName = "travelplanner_db"
        var appDatabase: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase? {
            if (appDatabase == null) {
                appDatabase = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    AppDatabase.databaseName
                )
                    .allowMainThreadQueries()
                    .build()
            }
            return appDatabase
        }
    }
}
