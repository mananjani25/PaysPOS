package com.android.pos.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.android.pos.data.dao.CategoryDao
import com.android.pos.data.dao.DBItemDao
import com.android.pos.data.entities.TbCategory
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.CharacterModel
import com.android.pos.data.remote.Constants.DATABASE_NAME


@Database(
    entities = [CharacterModel::class, TbCategory::class, TbItem::class],
    version = 2
)

abstract class AppDatabase : RoomDatabase() {

    abstract fun characterDao(): CharacterDao
    abstract fun categoryDao(): CategoryDao
    abstract fun itemDao(): DBItemDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also {
                    instance = it
                }
            }

        private fun buildDatabase(appContext: Context) =
            Room.databaseBuilder(appContext, AppDatabase::class.java, DATABASE_NAME)
                .fallbackToDestructiveMigration()
                .build()
    }

}
