package com.pays.pos.data.repositories

import com.pays.pos.data.db.AppDatabase
import com.pays.pos.data.entities.*
import com.pays.pos.data.model.requestModel.*
import com.pays.pos.data.model.responseModel.GetCustomerReceiptSettingsResponse
import com.pays.pos.data.model.responseModel.GetKitchenReceiptSettingsResponse
import com.pays.pos.data.model.responseModel.GetTaxResponse
import com.pays.pos.data.model.responseModel.ServiceChargeListResponse
import com.pays.pos.data.remote.ApiHelper
import com.pays.pos.di.RolePermission
import com.pays.pos.utils.performGetOperation
import com.pays.pos.utils.performGetOperationDatabase
import com.pays.pos.utils.statusUtils.Resource
import javax.inject.Inject

class TaxServiceChargeRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val apiHelperNew: ApiHelper,
    private val rolePermission: RolePermission
) {

    suspend fun createLoyaltyPoint(loyaltyPointRequest: LoyaltyPointRequest) =
        apiHelperNew.createLoyaltyPoint(loyaltyPointRequest)

    suspend fun editLoyaltyPoint(loyaltyPointRequest: LoyaltyPointRequest) =
        apiHelperNew.editLoyaltyPoint(loyaltyPointRequest)


    fun getTempTaxList() =
        performGetOperationDatabase(databaseQuery = { appDatabase.taxDao().allTax })

    fun getTaxList() =
        performGetOperation(
            databaseQuery = { appDatabase.taxDao().allTax },
            networkCall = { apiHelperNew.getTaxList() },
            saveCallResult = { appDatabase.taxDao().addAllTaxes(it.data) })

    suspend fun getTaxesList(): Resource<GetTaxResponse> {
        return apiHelperNew.getTaxList()
    }

    suspend fun getItemsListOfTax(taxId: Int): TaxData? = appDatabase.taxDao().taxById(taxId)

    fun enableTaxes() =
        performGetOperationDatabase(
            databaseQuery = { appDatabase.taxDao().enableTax })

    suspend fun addAllTaxListDatabase(data: List<TaxData>) =
        appDatabase.taxDao().addAllTaxes(data)

    suspend fun addAllTaxDatabase(data: List<TaxData>) =
        appDatabase.taxDao().addAllTaxesSuspend(data)

    suspend fun createTax(data: CreateTaxRequestModel) = apiHelperNew.createTax(data)

    suspend fun createTaxDatabase(data: TaxData) =
        appDatabase.taxDao().addTax(data)

    suspend fun updateTax(taxId: Int, data: CreateTaxRequestModel) =
        apiHelperNew.updateTax(taxId, data)

    suspend fun taxActive(id: Int, active: Boolean) =
        apiHelperNew.taxActive(id, active)

    suspend fun taxActiveDatabase(taxId: Int, active: Boolean) =
        appDatabase.taxDao().activeTax(taxId, active)

    suspend fun deleteTax(data: Int) = apiHelperNew.deleteTax(data)

    suspend fun deleteTaxFromDb() = appDatabase.taxDao().delete()

    suspend fun deleteTaxDatabase(taxId: Int) = appDatabase.taxDao().deleteTaxById(taxId)

    /*Added by Rahul, to solved the tax update issue - START*/
    suspend fun updateTaxStatus(taxId: Int,isActive:Boolean,isDeleted:Boolean) = appDatabase.taxDao().updateTaxStatus(taxId,isActive,isDeleted)
    /*Added by Rahul, to solved the tax update issue - END*/

    /*Added by Rahul, to solved the tax update issue - START*/
    suspend fun updateTax(id: Int, name: String?, active: Boolean?, isDeleted: Boolean?, itemIds:List<Int>) = appDatabase.taxDao().updateTax(id, name, active, isDeleted, itemIds)
    /*Added by Rahul, to solved the tax update issue - END*/


    fun getCurrentUserTeamRoleFromDb(taxId: Int) =
        performGetOperationDatabase(databaseQuery = { appDatabase.teamRoleDao().roleById(taxId) })


    fun getServiceChargeList() =
        performGetOperation(
            databaseQuery = { appDatabase.serviceChargeDao().allServiceCharge },
            networkCall = { apiHelperNew.getServiceChargeList() },
            saveCallResult = { appDatabase.serviceChargeDao().addAllServiceCharge(it.data) })


    suspend fun getServiceChargeWholeList(terminalId: Int): Resource<ServiceChargeListResponse> {
        return apiHelperNew.getServiceChargeWholeList(terminalId)
    }
//    suspend fun getServiceChargeWholeList(): Resource<ServiceChargeListResponse> {
//        performGetOperation(d by Rahul, to solved the tax update
//            databaseQuery = { appDatabase.serviceChargeDao().allServiceCharge },
//            networkCall = { apiHelperNew.getServiceChargeWholeList() },
//            saveCallResult = {
//                appDatabase.serviceChargeDao().addAllServiceCharge(it.data)
//            }
//        )
//    }

    /*Added by Rahul, to solved the tax update issue - START*/
    suspend fun fetchAllItemsList(): List<TbItem?>? {
        return appDatabase.itemDao().allItemsList()
    }


    suspend fun insertAllTbItems(items: List<TbItem?>?) {
        return appDatabase.itemDao().insertAllTbItems(items)
    }
    /*Added by Rahul, to solved the tax update issue - END*/

    fun loyaltyPointList() =
        performGetOperation(
            databaseQuery = { appDatabase.loyaltyProgramsDao().all },
            networkCall = { apiHelperNew.loyaltyPointList() },
            saveCallResult = { appDatabase.loyaltyProgramsDao().addAll(it.data) })

    suspend fun deleteServiceChargesFromDb() {
        appDatabase.serviceChargeDao().delete()
    }

    suspend fun addServiceCharges(serviceChargeList: List<TbServiceCharge>) {
        appDatabase.serviceChargeDao().addServiceCharges(serviceChargeList)
    }

    suspend fun addServiceCharge(serviceChargeList: TbServiceCharge) {
        appDatabase.serviceChargeDao().addServiceCharge(serviceChargeList)
    }

    suspend fun createServiceCharge(data: CreateServiceChargeRequestModel) =
        apiHelperNew.createServiceCharge(data)

    suspend fun createServiceChargeDatabase(data: TbServiceCharge) =
        appDatabase.serviceChargeDao().addServiceCharge(data)

    suspend fun updateServiceCharge(discountId: Int, data: CreateServiceChargeRequestModel) =
        apiHelperNew.updateServiceCharge(discountId, data)

    suspend fun serChargeActive(id: Int, active: Boolean) =
        apiHelperNew.serChargeActive(id, active)

    suspend fun loyaltyPointActive(id: Int, active: Boolean) =
        apiHelperNew.loyaltyPointActive(id, active)

    suspend fun serChargeActiveDatabase(serChargeId: Int, active: Boolean) =
        appDatabase.serviceChargeDao().activeServiceCharge(serChargeId, active)

    suspend fun loyaltyProgramActiveDatabase(loyaltyId: Int, active: Boolean) =
        appDatabase.loyaltyProgramsDao().activeLoyaltyProgram(loyaltyId, active)

    suspend fun deleteServiceCharge(data: Int) = apiHelperNew.deleteServiceCharge(data)
    suspend fun updateServiceChargeEnable(id: Int, enableServicecharge: Boolean) =
        apiHelperNew.updateServiceChargeEnable(id, enableServicecharge)

    suspend fun updateServiceChargeDineinEnable(id: Int, enableServicecharge: Boolean) =
        apiHelperNew.updateServiceChargeDininEnable(id, enableServicecharge)

    suspend fun deleteLoyaltyPoint(data: Int) = apiHelperNew.deleteLoyaltyPoint(data)

    suspend fun deleteSerChargeDatabase(serChargeId: Int) =
        appDatabase.serviceChargeDao().deleteServiceChargeById(serChargeId)

    suspend fun deleteLoyaltyPointDatabase(serChargeId: Int) =
        appDatabase.loyaltyProgramsDao().deleteTipById(serChargeId)

    suspend fun addLoyaltyPointDatabase(loyaltyProgramsModel: LoyaltyProgramsModel) =
        appDatabase.loyaltyProgramsDao().add(loyaltyProgramsModel)

    fun getTeamRoleList() =
        performGetOperation(
            databaseQuery = { appDatabase.teamRoleDao().allRoles },
            networkCall = { apiHelperNew.getTeamRoleList() },
            saveCallResult = {
                appDatabase.teamRoleDao().addAllRoles(it.data.teamRoles)
                rolePermission.findCurrentUserRoleAndSave(it.data.teamRoles)
            })


    fun getTeamRoleListFromDatabase() =
        performGetOperationDatabase(
            databaseQuery = { appDatabase.teamRoleDao().allRoles })

    fun getTeamModules() =
        performGetOperation(
            databaseQuery = { appDatabase.moduleDao().allModules },
            networkCall = { apiHelperNew.getTeamModules() },
            saveCallResult = { appDatabase.moduleDao().addAllModules(it.data) })

    /*fun getTeamModules() =
        performGetOperationNew(networkCall = { apiHelperNew.getTeamModules() })*/

    suspend fun createTeamRole(data: CreateTeamRoleRequestModel) =
        apiHelperNew.createTeamRole(data)

    suspend fun createTeamRoleDatabase(data: List<TeamRole>) =
        appDatabase.teamRoleDao().addAllRolesSuspend(data)

    suspend fun updateTeamRole(discountId: Int, data: CreateTeamRoleRequestModel) =
        apiHelperNew.updateTeamRole(discountId, data)

    suspend fun deleteTeamRole(data: Int) = apiHelperNew.deleteTeamRole(data)

    suspend fun deleteTeamRoleDatabase(teamRoleId: Int) =
        appDatabase.teamRoleDao().deleteRoleById(teamRoleId)

    suspend fun getTeamMemberTimeSheet(data: CreateTeamRoleRequestModel) =
        apiHelperNew.getTeamMemberTimeSheet(data)

    fun getKitchenReceiptSettingsDb() = performGetOperationDatabase(databaseQuery = {
        appDatabase.kitchenSettingsDao().getKitchenSettings
    })

    fun getKitchenReceiptSettings() =
        performGetOperation(databaseQuery = { appDatabase.kitchenSettingsDao().getKitchenSettings },
            networkCall = { apiHelperNew.getKitchenReceiptSettings() },
            saveCallResult = {
                appDatabase.kitchenSettingsDao().add(
                    if (it.data != null) {
                        it.data
                    } else {
                        val model = GetKitchenReceiptSettingsResponse.Data()
                        model
                    }
                )
            })


    fun getCustomerReceiptSettingsDb() = performGetOperationDatabase(databaseQuery = {
        appDatabase.customerSettingsDao().getCustomerSettings
    })

    fun getCustomerReceiptSettings() =
        performGetOperation(databaseQuery = { appDatabase.customerSettingsDao().getCustomerSettings },
            networkCall = { apiHelperNew.getCustomerReceiptSettings() },
            saveCallResult = {
                appDatabase.customerSettingsDao().add(
                    if (it.data != null) {
                        it.data
                    } else {
                        val model = GetCustomerReceiptSettingsResponse.Data()
                        model
                    }
                )
            })


    suspend fun getTransactionList(data: HashMap<String, String>) =
        apiHelperNew.getTransactionList(data)

    suspend fun refundPayment(data: RefundRequestModel) =
        apiHelperNew.refundPayment(data)

    suspend fun updateKitchenReceiptSettings(id: Int?, model: UpdateKitchenReceiptRequestModel) =
        apiHelperNew.updateKitchenSettings(id!!, model)

    suspend fun updateCustomerReceiptSettings(id: Int, model: UpdateCustomerReceiptRequestModel) =
        apiHelperNew.updateCustomerSettings(id, model)


}