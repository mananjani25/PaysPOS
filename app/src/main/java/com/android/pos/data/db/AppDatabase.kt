package com.android.pos.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.android.pos.data.dao.*
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.TbCategory
import com.android.pos.data.entities.TbItem
import com.android.pos.data.model.CharacterModel
import com.android.pos.data.model.CustomerListResponse
import com.android.pos.data.model.responseModel.*
import com.android.pos.data.remote.Constants.DATABASE_NAME
import com.android.pos.data.typeconvert.TypeConvertersIds
import com.android.pos.data.typeconvert.TypeConvertorAddress
import com.android.pos.data.typeconvert.TypeConvertorPhone


@Database(
    entities = [CharacterModel::class, TbCategory::class, TbItem::class, GetTaxResponse.TaxData::class,
        GetTipReponse.Data::class, GetDiscountResponse.Data::class,
        NoteResponse.Data::class, GetServiceChargeResponse.Data::class,
        EmployeeListResponse.Data.Employee::class, CartModel::class, CustomerListResponse.Data::class],
    version = 2
)
@TypeConverters(TypeConvertersIds::class,TypeConvertorAddress::class,TypeConvertorPhone::class)


abstract class AppDatabase : RoomDatabase() {

    abstract fun characterDao(): CharacterDao
    abstract fun categoryDao(): CategoryDao
    abstract fun itemDao(): DBItemDao
    abstract fun taxDao(): TaxDao
    abstract fun tipDao(): TipsDao
    abstract fun discountDao(): DiscountDao
    abstract fun notesDao(): NotesDao
    abstract fun serviceChargeDao(): ServiceChargeDao
    abstract fun cartDao(): CartDao
    abstract fun employeeDao(): EmployeeDao
    abstract fun customerDao():CustomerDao

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
