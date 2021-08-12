package com.android.pos.data.repositories

import com.android.pos.data.db.AppDatabase
import com.android.pos.data.entities.TaxData
import com.android.pos.data.entities.TbServiceCharge
import com.android.pos.data.entities.TeamRole
import com.android.pos.data.model.requestModel.CreateServiceChargeRequestModel
import com.android.pos.data.model.requestModel.CreateTaxRequestModel
import com.android.pos.data.model.requestModel.CreateTeamRoleRequestModel
import com.android.pos.data.remote.ApiHelper
import com.android.pos.utils.performGetOperation
import javax.inject.Inject

class TaxServiceChargeRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val apiHelperNew: ApiHelper
) {

    fun getTaxList() =
        performGetOperation(
            databaseQuery = { appDatabase.taxDao().allTax },
            networkCall = { apiHelperNew.getTaxList() },
            saveCallResult = { appDatabase.taxDao().addAllTaxes(it.data) })

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

    suspend fun deleteTaxDatabase(taxId: Int) = appDatabase.taxDao().deleteTaxById(taxId)


    fun getServiceChargeList() =
        performGetOperation(
            databaseQuery = { appDatabase.serviceChargeDao().allServiceCharge },
            networkCall = { apiHelperNew.getServiceChargeList() },
            saveCallResult = { appDatabase.serviceChargeDao().addAllServiceCharge(it.data) })

    suspend fun addServiceCharges(serviceChargeList: List<TbServiceCharge>) {
        appDatabase.serviceChargeDao().addServiceCharges(serviceChargeList)
    }

    suspend fun createServiceCharge(data: CreateServiceChargeRequestModel) =
        apiHelperNew.createServiceCharge(data)

    suspend fun createServiceChargeDatabase(data: TbServiceCharge) =
        appDatabase.serviceChargeDao().addServiceCharge(data)

    suspend fun updateServiceCharge(discountId: Int, data: CreateServiceChargeRequestModel) =
        apiHelperNew.updateServiceCharge(discountId, data)

    suspend fun serChargeActive(id: Int, active: Boolean) =
        apiHelperNew.serChargeActive(id, active)

    suspend fun serChargeActiveDatabase(serChargeId: Int, active: Boolean) =
        appDatabase.serviceChargeDao().activeServiceCharge(serChargeId, active)

    suspend fun deleteServiceCharge(data: Int) = apiHelperNew.deleteServiceCharge(data)

    suspend fun deleteSerChargeDatabase(serChargeId: Int) =
        appDatabase.serviceChargeDao().deleteServiceChargeById(serChargeId)

    fun getTeamRoleList() =
        performGetOperation(
            databaseQuery = { appDatabase.teamRoleDao().allRoles },
            networkCall = { apiHelperNew.getTeamRoleList() },
            saveCallResult = { appDatabase.teamRoleDao().addAllRoles(it.data.teamRoles) })

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

}