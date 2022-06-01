package com.android.pos.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.android.pos.data.dao.*
import com.android.pos.data.entities.*
import com.android.pos.data.model.CharacterModel
import com.android.pos.data.model.ShiftRportConfiguration
import com.android.pos.data.model.SplitDetailListModel
import com.android.pos.data.model.responseModel.*
import com.android.pos.data.remote.Constants.DATABASE_NAME
import com.android.pos.data.typeconvert.*


@Database(
    entities = [CharacterModel::class, TbCategory::class, TbItem::class, TaxData::class,
        GetTipReponse.Data::class, TbDiscount::class, NoteResponse.Data::class,
        TbServiceCharge::class, Employee::class, CartModel::class,
        TbCustomer::class, ModifierSet::class, TeamRole::class,
        ModulePermission::class, TbOrderType::class, VenueDetailsResponse.Data.Terminal::class,
        ItemModifierSets::class, OptionSet::class, PrinterResponse.Data.CustomerReceiptPrinters::class,
        PrinterResponse.Data.KitchenReceiptPrinters::class, GetKitchenReceiptSettingsResponse.Data::class,
        GetCustomerReceiptSettingsResponse.Data::class, LoyaltyProgramsModel::class, SplitDetailListModel::class,
        CashDiscountModel::class, TbCountryList::class, TbCardReader::class, VenueDetailsResponse.Data.CancelOrderReason::class, DineInCartModel::class, ShiftRportConfiguration::class],
    version = 1
)
@TypeConverters(
    TypeConvertersIds::class,
    TypeConvertorAddress::class,
    TypeConvertorPhone::class,
    TypeConvertersEmployee::class,
    TCModifier::class,
    TypeConvertersTax::class,
    TypeConvertersModule::class,
    TCServiceCharge::class,
    TCCustomer::class,
    TCOption::class,
    TCVariations::class,
    TCOptionSets::class,
    TCCustomerReceiptPrinters::class,
    TCPrinter::class,
    TCOrderTypes::class,
    TCKitchenReceiptPrinters::class,
    TCPrinterCategories::class,
    TCDineInList::class,
    TCLoyaltyPrograms::class,
)


abstract class AppDatabase : RoomDatabase() {

    abstract fun characterDao(): CharacterDao
    abstract fun categoryDao(): CategoryDao
    abstract fun itemDao(): DBItemDao
    abstract fun taxDao(): TaxDao
    abstract fun splitDao(): SplitListDao
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
    abstract fun optionSetDao(): OptionSetDao
    abstract fun orderTypeDao(): OrderTypeDao
    abstract fun terminalDao(): TerminalsDao
    abstract fun itemModifierSetsDao(): ItemModifierSetsDao
    abstract fun printerDao(): PrinterDao
    abstract fun kitchenSettingsDao(): KitchenSettingsDao
    abstract fun customerSettingsDao(): CustomerSettingsDao
    abstract fun loyaltyProgramsDao(): LoyaltyProgramsDao
    abstract fun cashDiscountDao(): CashDiscountsDao
    abstract fun countryListDao(): CountryListDao
    abstract fun cardReaderDao(): cardReaderDao
    abstract fun cancelOrderReasonDao(): CancelOrderReasonsDao
    abstract fun eodReportSettings():EODReportDao

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
