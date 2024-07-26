package com.pays.pos.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pays.pos.data.dao.*
import com.pays.pos.data.entities.*
import com.pays.pos.data.model.CharacterModel
import com.pays.pos.data.model.PrinterQueueModel
import com.pays.pos.data.model.ShiftRportConfiguration
import com.pays.pos.data.model.SplitDetailListModel
import com.pays.pos.data.model.responseModel.GetCustomerReceiptSettingsResponse
import com.pays.pos.data.model.responseModel.GetKitchenReceiptSettingsResponse
import com.pays.pos.data.model.responseModel.GetTipReponse
import com.pays.pos.data.model.responseModel.NoteResponse
import com.pays.pos.data.model.responseModel.PrinterResponse
import com.pays.pos.data.model.responseModel.VenueDetailsResponse
import com.pays.pos.data.remote.Constants.DATABASE_NAME
import com.pays.pos.data.typeconvert.TCBusiness
import com.pays.pos.data.typeconvert.TCCustomer
import com.pays.pos.data.typeconvert.TCCustomerReceiptPrinters
import com.pays.pos.data.typeconvert.TCDineInList
import com.pays.pos.data.typeconvert.TCKitchenReceiptPrinters
import com.pays.pos.data.typeconvert.TCLoyaltyPrograms
import com.pays.pos.data.typeconvert.TCModifier
import com.pays.pos.data.typeconvert.TCOption
import com.pays.pos.data.typeconvert.TCOptionSets
import com.pays.pos.data.typeconvert.TCOrderItemsPrinter
import com.pays.pos.data.typeconvert.TCOrderTypes
import com.pays.pos.data.typeconvert.TCPrinter
import com.pays.pos.data.typeconvert.TCPrinterCategories
import com.pays.pos.data.typeconvert.TCPrinterQueueData
import com.pays.pos.data.typeconvert.TCPrinterQueueSuucessModel
import com.pays.pos.data.typeconvert.TCServiceCharge
import com.pays.pos.data.typeconvert.TCVariations
import com.pays.pos.data.typeconvert.TypeConvertersEmployee
import com.pays.pos.data.typeconvert.TypeConvertersIds
import com.pays.pos.data.typeconvert.TypeConvertersItems
import com.pays.pos.data.typeconvert.TypeConvertersModule
import com.pays.pos.data.typeconvert.TypeConvertersTax
import com.pays.pos.data.typeconvert.TypeConvertorAddress
import com.pays.pos.data.typeconvert.TypeConvertorPhone


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
        VenueDetailsResponse.Data.WastageReason::class, TbCartItem::class, CartModelBackup::class, OrderTypeBackup::class, TbLabelPrinterSettings::class],
    version = 19
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


public abstract class AppDatabase : RoomDatabase() {

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
    abstract fun orderTypeBackupDao(): OrderTypeBackupDao
    abstract fun labelPrinterSettings(): LabelPrinterSettingsDao

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

        private val MIGRATION_13_14: Migration = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE TbItem ADD COLUMN cartItemId INTEGER DEFAULT 0 NOT NULL")

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        }

        private val MIGRATION_14_15: Migration = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE TbItem ADD COLUMN isItemEdited INTEGER DEFAULT 0 NOT NULL")

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

        private val MIGRATION_15_16: Migration = object : Migration(15,16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL(
                        "CREATE TABLE IF NOT EXISTS `CartModelBackup` " +
                                "(`id` Integer PRIMARY KEY NOT NULL, " +
                                "`data` TEXT NOT NULL)"
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        private val MIGRATION_16_17: Migration = object : Migration(16,17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL(
                        "CREATE TABLE IF NOT EXISTS `TbLabelPrinterSettings` " +
                                "(`id` Integer PRIMARY KEY NOT NULL, " +
                                "`oneItemPerReciept` INTEGER NOT NULL DEFAULT(0))"
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        private fun buildDatabase(appContext: Context) =
            Room.databaseBuilder(appContext, AppDatabase::class.java, DATABASE_NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7
                    , MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11,MIGRATION_11_12,
                    MIGRATION_12_13,MIGRATION_13_14,MIGRATION_14_15,MIGRATION_15_16, MIGRATION_16_17
                ).fallbackToDestructiveMigration()
                .build()
    }

}
