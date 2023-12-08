package com.android.pos.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.android.pos.data.dao.BusinessDetailsDao
import com.android.pos.data.dao.CancelOrderReasonsDao
import com.android.pos.data.dao.CartDao
import com.android.pos.data.dao.CashDiscountsDao
import com.android.pos.data.dao.CategoryDao
import com.android.pos.data.dao.CountryListDao
import com.android.pos.data.dao.CustomerDao
import com.android.pos.data.dao.CustomerSettingsDao
import com.android.pos.data.dao.DBItemDao
import com.android.pos.data.dao.DiscountDao
import com.android.pos.data.dao.EODReportDao
import com.android.pos.data.dao.EmployeeDao
import com.android.pos.data.dao.ItemModifierSetsDao
import com.android.pos.data.dao.KitchenSettingsDao
import com.android.pos.data.dao.LoyaltyProgramsDao
import com.android.pos.data.dao.ModifierSetDao
import com.android.pos.data.dao.ModuleDao
import com.android.pos.data.dao.NotesDao
import com.android.pos.data.dao.OptionSetDao
import com.android.pos.data.dao.OrderTypeDao
import com.android.pos.data.dao.PAXDao
import com.android.pos.data.dao.PrinterDao
import com.android.pos.data.dao.PrinterQueueDao
import com.android.pos.data.dao.ServiceChargeDao
import com.android.pos.data.dao.SplitListDao
import com.android.pos.data.dao.TaxDao
import com.android.pos.data.dao.TeamRoleDao
import com.android.pos.data.dao.TerminalsDao
import com.android.pos.data.dao.TimeZonesDao
import com.android.pos.data.dao.TipsDao
import com.android.pos.data.dao.WastageReasonsDao
import com.android.pos.data.dao.cardReaderDao
import com.android.pos.data.entities.CartModel
import com.android.pos.data.entities.CashDiscountModel
import com.android.pos.data.entities.DineInCartModel
import com.android.pos.data.entities.Employee
import com.android.pos.data.entities.ItemModifierSets
import com.android.pos.data.entities.LoyaltyProgramsModel
import com.android.pos.data.entities.ModifierSet
import com.android.pos.data.entities.ModulePermission
import com.android.pos.data.entities.OptionSet
import com.android.pos.data.entities.PAXData
import com.android.pos.data.entities.TaxData
import com.android.pos.data.entities.TbBusinessDetails
import com.android.pos.data.entities.TbCardReader
import com.android.pos.data.entities.TbCartItem
import com.android.pos.data.entities.TbCategory
import com.android.pos.data.entities.TbCountryList
import com.android.pos.data.entities.TbCustomer
import com.android.pos.data.entities.TbDiscount
import com.android.pos.data.entities.TbItem
import com.android.pos.data.entities.TbOrderType
import com.android.pos.data.entities.TbServiceCharge
import com.android.pos.data.entities.TbTimeZones
import com.android.pos.data.entities.TeamRole
import com.android.pos.data.entities.TypeConvertersQueueDineIn
import com.android.pos.data.model.CharacterModel
import com.android.pos.data.model.PrinterQueueModel
import com.android.pos.data.model.ShiftRportConfiguration
import com.android.pos.data.model.SplitDetailListModel
import com.android.pos.data.model.responseModel.GetCustomerReceiptSettingsResponse
import com.android.pos.data.model.responseModel.GetKitchenReceiptSettingsResponse
import com.android.pos.data.model.responseModel.GetTipReponse
import com.android.pos.data.model.responseModel.NoteResponse
import com.android.pos.data.model.responseModel.PrinterResponse
import com.android.pos.data.model.responseModel.VenueDetailsResponse
import com.android.pos.data.remote.Constants.DATABASE_NAME
import com.android.pos.data.typeconvert.TCBusiness
import com.android.pos.data.typeconvert.TCCustomer
import com.android.pos.data.typeconvert.TCCustomerReceiptPrinters
import com.android.pos.data.typeconvert.TCDineInList
import com.android.pos.data.typeconvert.TCKitchenReceiptPrinters
import com.android.pos.data.typeconvert.TCLoyaltyPrograms
import com.android.pos.data.typeconvert.TCModifier
import com.android.pos.data.typeconvert.TCOption
import com.android.pos.data.typeconvert.TCOptionSets
import com.android.pos.data.typeconvert.TCOrderItemsPrinter
import com.android.pos.data.typeconvert.TCOrderTypes
import com.android.pos.data.typeconvert.TCPrinter
import com.android.pos.data.typeconvert.TCPrinterCategories
import com.android.pos.data.typeconvert.TCPrinterQueueData
import com.android.pos.data.typeconvert.TCPrinterQueueSuucessModel
import com.android.pos.data.typeconvert.TCServiceCharge
import com.android.pos.data.typeconvert.TCVariations
import com.android.pos.data.typeconvert.TypeConvertersEmployee
import com.android.pos.data.typeconvert.TypeConvertersIds
import com.android.pos.data.typeconvert.TypeConvertersItems
import com.android.pos.data.typeconvert.TypeConvertersModule
import com.android.pos.data.typeconvert.TypeConvertersTax
import com.android.pos.data.typeconvert.TypeConvertorAddress
import com.android.pos.data.typeconvert.TypeConvertorPhone


@Database(
    entities = [CharacterModel::class, TbCategory::class, TbItem::class, TaxData::class,
        GetTipReponse.Data::class, TbDiscount::class, NoteResponse.Data::class,
        TbServiceCharge::class, Employee::class, CartModel::class,
        TbCustomer::class, ModifierSet::class, TeamRole::class,
        ModulePermission::class, TbOrderType::class, VenueDetailsResponse.Data.Terminal::class,
        ItemModifierSets::class, OptionSet::class, PrinterResponse.Data.CustomerReceiptPrinters::class,
        PrinterResponse.Data.KitchenReceiptPrinters::class, GetKitchenReceiptSettingsResponse.Data::class,
        GetCustomerReceiptSettingsResponse.Data::class, LoyaltyProgramsModel::class, SplitDetailListModel::class,
        CashDiscountModel::class, TbCountryList::class, TbCardReader::class, PAXData::class, VenueDetailsResponse.Data.CancelOrderReason::class,
        DineInCartModel::class, ShiftRportConfiguration::class, TbBusinessDetails::class, TbTimeZones::class, PrinterQueueModel::class,
        VenueDetailsResponse.Data.WastageReason::class, TbCartItem::class],
    version = 13
)
@TypeConverters(
    TypeConvertersItems::class,
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
    TCBusiness::class,
    TCPrinterQueueData::class,
    TCOrderItemsPrinter::class,
    TCPrinterQueueSuucessModel::class,
    TypeConvertersQueueDineIn::class
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
    abstract fun PAXDao():PAXDao
    abstract fun cancelOrderReasonDao(): CancelOrderReasonsDao
    abstract fun eodReportSettings(): EODReportDao
    abstract fun businessDetailsDao(): BusinessDetailsDao
    abstract fun timeZonesDao(): TimeZonesDao
    abstract fun printerQueueDao(): PrinterQueueDao
    abstract fun wastageReasonsDao(): WastageReasonsDao

    companion object {


        @Volatile
        private var instance: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also {
                    instance = it
                }
            }

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE TbOrderType ADD COLUMN primaryOrderType INTEGER DEFAULT 0 NOT NULL")

            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE TbItem ADD COLUMN id INTEGER DEFAULT 0 NOT NULL")
                database.execSQL("ALTER TABLE TbItem ADD COLUMN headerPositionDinein INTEGER DEFAULT 0 NOT NULL")
                database.execSQL("ALTER TABLE TbItem ADD COLUMN singleItemPrice DOUBLE DEFAULT 0.0 NOT NULL")

            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {

                    database.execSQL("ALTER TABLE TbCustomerSettings ADD COLUMN showCashDisSurCharg boolean DEFAULT 0")
                } catch (e: Exception) {
                    e.printStackTrace()
                }


            }

        }

        val MIGRATION_4_5: Migration = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {

                    database.execSQL("ALTER TABLE TbItem ADD COLUMN price_without_markup DOUBLE DEFAULT 0.0 NOT NULL")
                } catch (e: Exception) {
                    e.printStackTrace()
                }


            }

        }

        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE TbItem ADD COLUMN dineInSort INTEGER DEFAULT 0 NOT NULL")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        }

        val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE EodShiftReport ADD COLUMN clockInOut INTEGER DEFAULT 0 NOT NULL")
                    database.execSQL("CREATE TABLE IF NOT EXISTS `TbWastageReason` " +
                            "(`id` INTEGER PRIMARY KEY NOT NULL, " +
                            "`isActive` INTEGER NOT NULL, " +
                            "`name` TEXT NOT NULL, " +
                            "`sort` INTEGER NOT NULL, " +
                            "`locationID` INTEGER NOT NULL, " +
                            "`createdAt` TEXT NOT NULL, " +
                            "`updatedAt` TEXT NOT NULL, " +
                            "`deletedAt` TEXT)")
                //database.execSQL("CREATE TABLE IF NOT EXISTS `TbWastageReason` (`id` INTEGER, PRIMARY KEY(`id`), `name` TEXT NOT NULL)")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        }

        private val MIGRATION_7_8: Migration = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE PrinterQueue ADD COLUMN employeeName TEXT DEFAULT '' NOT NULL")
                database.execSQL("ALTER TABLE PrinterQueue ADD COLUMN dateAndTime TEXT DEFAULT '' NOT NULL")
                database.execSQL("ALTER TABLE PrinterQueue ADD COLUMN orderNote TEXT DEFAULT '' NOT NULL")
                database.execSQL("ALTER TABLE PrinterQueue ADD COLUMN guestAttributes TEXT DEFAULT '' NOT NULL")
            }

        }

        private val MIGRATION_8_9: Migration = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE EodShiftReport ADD COLUMN isItemWiseSales INTEGER DEFAULT 0 NOT NULL")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        }
        private val MIGRATION_9_10: Migration = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE TbItem ADD COLUMN itemOriginalModifiersList TEXT")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        }

        private val MIGRATION_10_11: Migration = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL(
                        "CREATE TABLE IF NOT EXISTS `PAXData` " +
                                "(`globalUid` TEXT PRIMARY KEY NOT NULL, " +
                                "`extData` TEXT NOT NULL, " +
                                "`refNumber` TEXT NOT NULL, " +
                                "`eCRRefNumber` TEXT NOT NULL, " +
                                "`paxToken` TEXT NOT NULL, " +
                                "`cardLastDigits` TEXT NOT NULL, " +
                                "`EDCType` TEXT NOT NULL)"
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        private val MIGRATION_11_12: Migration = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE PrinterQueue ADD COLUMN deliveryType TEXT DEFAULT '' NOT NULL")
                    database.execSQL("ALTER TABLE TbOrderType ADD COLUMN isDefault INTEGER DEFAULT 0 NOT NULL")

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        }

        private val MIGRATION_12_13: Migration = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {

                    database.execSQL("UPDATE TABLE TbItem SET COLUMN cost DOUBLE DEFAULT 0.0 NOT NULL")
                    database.execSQL("ALTER TABLE TbItem ADD COLUMN `priceType` TEXT")
                    database.execSQL("ALTER TABLE TbItem ADD COLUMN `kitchenName` TEXT")
                    database.execSQL("ALTER TABLE TbItem ADD COLUMN `productCode` TEXT")
                    database.execSQL("ALTER TABLE TbItem ADD COLUMN `modifierGroupIds` TEXT")
                    database.execSQL("ALTER TABLE TbItem ADD COLUMN `option_set_ids` TEXT")
                    database.execSQL("ALTER TABLE TbItem ADD COLUMN isTax boolean DEFAULT 0")
                    database.execSQL("ALTER TABLE TbItem ADD COLUMN isDiscountDefault boolean DEFAULT 0")

                    database.execSQL(
                        "CREATE TABLE IF NOT EXISTS `TbCartItem` " +
                                "(`cartItemId` INTEGER PRIMARY KEY AUTOINCREMENT, " +
                                "`itemId` INTEGER NOT NULL, " +
                                "`name` TEXT NOT NULL, " +
                                "`id` INTEGER NOT NULL, " +
                                "`price` REAL NOT NULL, " +
                                "`quantity` INTEGER NOT NULL, " +
                                "`sku` TEXT NOT NULL, " +
                                "`isHide` INTEGER NOT NULL, " +
                                "`sort` INTEGER NOT NULL, " +
                                "`dineInSort` INTEGER NOT NULL, " +
                                "`hide_status` TEXT, " +
                                "`website_hide_status` TEXT, " +
                                "`taxes` TEXT NOT NULL, " +
                                "`imageUrl` TEXT, " +
                                "`thumbImageUrl` TEXT, " +
                                "`createdAt` TEXT NOT NULL, " +
                                "`updatedAt` TEXT NOT NULL, " +
                                "`customItemID` INTEGER NOT NULL, " +
                                "`categoryId` INTEGER NOT NULL, " +
                                "`categoryName` TEXT NOT NULL, " +
                                "`shortDescription` TEXT NOT NULL, " +
                                "`note` TEXT NOT NULL, " +
                                "`itemQuantity` INTEGER NOT NULL, " +
                                "`isManualSales` INTEGER NOT NULL, " +
                                "`isChecked` INTEGER NOT NULL, " +
                                "`modifiers_set_ids` TEXT NOT NULL, " +
                                "`modifiers` TEXT NOT NULL, " +
                                "`customItemCount` INTEGER NOT NULL, " +
                                "`discountPrice` REAL NOT NULL, " +
                                "`singleItemPrice` REAL NOT NULL, " +
                                "`discountId` INTEGER, " +
                                "`discountType` TEXT NOT NULL, " +
                                "`variationsAttributes` TEXT NOT NULL, " +
                                "`optionSets` TEXT, " +
                                "`orderItemId` INTEGER, " +
                                "`isFired` INTEGER NOT NULL, " +
                                "`timeStamp` TEXT, " +
                                "`isPaid` INTEGER NOT NULL, " +
                                "`isEdited` INTEGER NOT NULL, " +
                                "`guestItemId` INTEGER, " +
                                "`isDestroy` INTEGER NOT NULL, " +
                                "`reorder` INTEGER NOT NULL, " +
                                "`manualSaleId` TEXT NOT NULL, " +
                                "`isDeleted` INTEGER NOT NULL, " +
                                "`headerPositionDinein` INTEGER NOT NULL, " +
                                "`itemOriginalModifiersList` TEXT, " +
                                "`employeeID` INTEGER NOT NULL, " +
                                "`isManualSaleItem` INTEGER NOT NULL, " +
                                "`orderType` TEXT NOT NULL, " +
                                "`orderTypeName` TEXT NOT NULL, " +
                                "`guestIndexForDineIn` INTEGER NOT NULL, " +
                                "`orderTypeId` INTEGER NOT NULL)"
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    database.execSQL("ALTER TABLE PrinterQueue ADD COLUMN deliveryType TEXT DEFAULT '' NOT NULL")
                    database.execSQL("ALTER TABLE TbOrderType ADD COLUMN isDefault INTEGER DEFAULT 0 NOT NULL")

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        private fun buildDatabase(appContext: Context) =
            Room.databaseBuilder(appContext, AppDatabase::class.java, DATABASE_NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7
                    , MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_12_13
                ).fallbackToDestructiveMigration()
                .build()
    }

}
