package com.android.pos.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.android.pos.data.dao.*
import com.android.pos.data.entities.*
import com.android.pos.data.model.CharacterModel
import com.android.pos.data.model.CustomerListResponse
import com.android.pos.data.model.responseModel.GetTipReponse
import com.android.pos.data.model.responseModel.NoteResponse
import com.android.pos.data.remote.Constants.DATABASE_NAME
import com.android.pos.data.typeconvert.*


@Database(
    entities = [CharacterModel::class, TbCategory::class, TbItem::class, TaxData::class,
        GetTipReponse.Data::class, TbDiscount::class, NoteResponse.Data::class,
        TbServiceCharge::class, Employee::class, CartModel::class,
        CustomerListResponse.Data::class, ModifierSet::class, TeamRole::class, ItemModifierSet::class,
        ModulePermission::class],
    version = 2
)
@TypeConverters(
    TypeConvertersIds::class,
    TypeConvertorAddress::class,
    TypeConvertorPhone::class,
    TypeConvertersEmployee::class,
    TCModifier::class,
    TypeConvertersTax::class,
    TypeConvertersModule::class,
    TCServiceCharge::class
)


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
    abstract fun customerDao(): CustomerDao
    abstract fun teamRoleDao(): TeamRoleDao
    abstract fun moduleDao(): ModuleDao
    abstract fun modifierSetDao(): ModifierSetDao
    abstract fun itemModifierSetDao(): ItemModifierSetDao

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
