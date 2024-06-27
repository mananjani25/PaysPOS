package com.pays.pos.data.repositories


import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.pays.pos.data.db.AppDatabase
import com.pays.pos.data.db.IDataManager
import com.pays.pos.data.model.PrinterQueueModel
import com.pays.pos.data.model.ShiftRportConfiguration
import com.pays.pos.data.model.SplitDetailListModel
import com.pays.pos.data.model.requestModel.CashInOutModel
import com.pays.pos.data.model.requestModel.CashLogRequest
import com.pays.pos.data.model.requestModel.CreateCategoryRequestModel
import com.pays.pos.data.model.requestModel.CreateCustomerRequestModel
import com.pays.pos.data.model.requestModel.CreateEmployeeRequestModel
import com.pays.pos.data.model.requestModel.CreateItemRequestModel
import com.pays.pos.data.model.requestModel.CreateModifierRequest
import com.pays.pos.data.model.requestModel.CreateNoteRequest
import com.pays.pos.data.model.requestModel.CreateOptionRequestModel
import com.pays.pos.data.model.requestModel.CreatePrinterRequestModel
import com.pays.pos.data.model.requestModel.CreateQueuePrinterRequestModel
import com.pays.pos.data.model.requestModel.GuestPaymentRequest
import com.pays.pos.data.model.requestModel.MergeTableRequest
import com.pays.pos.data.model.requestModel.OrderCancelRequest
import com.pays.pos.data.model.requestModel.OrderRequestModel
import com.pays.pos.data.model.requestModel.RefundRequestModelOnlineOrder
import com.pays.pos.data.model.requestModel.SpitByOrderRequestModel
import com.pays.pos.data.model.requestModel.WastageItemRequest
import com.pays.pos.data.model.requestModel.giftCard.request.GiftCardAddValueRequest
import com.pays.pos.data.model.requestModel.giftCard.request.GiftCardCheckBalanceRequest
import com.pays.pos.data.model.requestModel.giftCard.request.SellGiftCardRequestModel
import com.pays.pos.data.remote.ApiHelper
import com.pays.pos.data.remote.Constants
import com.pays.pos.data.remote.Constants.DINE_IN
import com.pays.pos.data.remote.Constants.EMPLOYEE_ID
import com.pays.pos.data.remote.Constants.LOCATION_ID
import com.pays.pos.data.remote.Constants.SYNC_SETTING_TIME_STAMP
import com.pays.pos.data.remote.Constants.TERMINAL_ID
import com.pays.pos.di.PrefProvider
import com.pays.pos.utils.LogUtil
import com.pays.pos.utils.performGetOperation
import com.pays.pos.utils.performGetOperationDatabase
import com.pays.pos.utils.performGetOperationNew
import com.pays.pos.utils.statusUtils.Resource
import com.google.gson.Gson
import com.pays.pos.data.entities.*
import com.pays.pos.data.model.responseModel.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject


public class KioskRepository @Inject constructor(
    private val prefProvider: PrefProvider,
    private val appDatabase: AppDatabase) : IDataManager {

    override suspend fun abs() {
        appDatabase.characterDao().getCharacter(0)
    }


    fun getKitchenPrinters() =
        performGetOperationDatabase { appDatabase.printerDao().kitchenPrintList }

    suspend fun getKitchenPrintersList() = appDatabase.printerDao().getKitchenPrinterList()
    suspend fun getKitchenPrinterForPrint() = appDatabase.printerDao().getKitchenPrinterForPrinting()
    suspend fun getKitchenSettingsNormalData() = appDatabase.kitchenSettingsDao().getKitchenSettingsNormalData()

}

