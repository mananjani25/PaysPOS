package com.android.pos.di

import android.content.Context
import android.util.Log
import android.view.View
import com.android.pos.data.entities.TeamRole
import com.android.pos.data.remote.Constants
import com.android.pos.utils.extensions.showAlert
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RolePermission @Inject constructor(
    @ApplicationContext private val context: Context,
    private var prefProvider: PrefProvider
) {

    private val MODULE_CUSTOMER = "Customer"
    private val MODULE_REPORT_SUMMARY = "ReportSummary"
    private val MODULE_TRANSACTIONS = "Transactions"
    private val MODULE_CASH_LOG = "Cash Logs"
    private val MODULE_TIMESHEET = "Employee Timesheet"
    private val MODULE_EMPLOYEE = "Employee"
    private val MODULE_TABLE = "Table"
    private val MODULE_CLEAR_TABLE = "Clear Table"
    private val MODULE_DISCOUNT = "Discount"
    private val MODULE_INVENTORY = "Inventory"
    private val MODULE_MANUAL_SALES = "Manual Sales"
    private val MODULE_CANCEL_ORDER = "Cancel Order"
    private val MODULE_CASH_DRAWER = "Cash Drawer"

    fun findCurrentUserRoleAndSave(teamRoles: List<TeamRole>) {

        try {

            prefProvider = PrefProvider(context)

            for (teamRole in teamRoles) {
                if (teamRole.id != prefProvider.getEmployeeRoleId()) continue
                prefProvider.saveCurrentRoleDetails(teamRole)
                break
            }

        }catch (e:Exception){
            Log.d("Exception","findCurrentUserRoleAndSave")
        }


    }

     private fun checkPermission(root: View?, moduleName: String): Boolean {
        prefProvider.getCurrentEmployeeRole()?.let {
            if (it.modulePermission?.isNotEmpty() == true) {
                it.modulePermission.forEach { modulePermission ->
                    if (modulePermission.name.equals(moduleName, true)) {
                        return true
                    }
                }
            }
        }
        root?.showAlert("You do not have permission to access this feature.\nPlease contact your manager.")
        return false
    }

    private fun checkPermissionForCashDrawer(moduleName: String): Boolean {
        prefProvider.getCurrentEmployeeRole()?.let {
            if (it.modulePermission?.isNotEmpty() == true) {
                it.modulePermission.forEach { modulePermission ->
                    if (modulePermission.name.equals(moduleName, true)) {
                        return true
                    }
                }
            }
        }
      //  root?.showAlert("You do not have permission to access this feature.\nPlease contact your manager.")
        return false
    }

    fun hasUserAccessPermission(root: View?): Boolean {
        val permission = prefProvider.isOwner() || prefProvider.isAdmin()
        return if (permission) {
            true
        } else {
            root?.showAlert(
                "You do not have permission to access this feature.\n" +
                        "Please contact your manager."
            )
            false
        }
    }

    fun isDefaultUserRoleWithoutAlert(name: String): Boolean {
        return (name.equals(Constants.ROLE_MANAGER, true)
                || name.equals(Constants.ROLE_OWNER, true)
                || name.equals(Constants.ROLE_ADMIN, true))
    }

    fun isDefaultUserRole(name: String, root: View?): Boolean {
        return if (name.equals(Constants.ROLE_MANAGER, true)
            || name.equals(Constants.ROLE_OWNER, true)
            || name.equals(Constants.ROLE_ADMIN, true)
        ) {
            root?.showAlert("This is the system generated default user role. You cant delete it !!")
            false
        } else {
            true
        }
    }

    fun hasCustomerPermission(root: View?): Boolean {
        return checkPermission(root, MODULE_CUSTOMER)
    }

    fun hasReportSummaryPermission(root: View?): Boolean {
        return checkPermission(root, MODULE_REPORT_SUMMARY)
    }

    fun hasTransactionPermission(root: View?): Boolean {
        return checkPermission(root, MODULE_TRANSACTIONS)
    }

    fun hasCashLogPermission(root: View?): Boolean {
        return checkPermission(root, MODULE_CASH_LOG)
    }

    fun hasEmployeeTimesheetPermission(root: View?): Boolean {
        return checkPermission(root, MODULE_TIMESHEET)
    }

    fun hasEmployeePermission(root: View?): Boolean {
        return checkPermission(root, MODULE_EMPLOYEE)
    }

    fun hasTablePermission(root: View?): Boolean {
        return checkPermission(root, MODULE_TABLE)
    }

    fun hasCashDrawerPermission():Boolean{
        return checkPermissionForCashDrawer(MODULE_CASH_DRAWER)
    }

    fun hasClearTablePermission(root: View?): Boolean {
        return checkPermission(root, MODULE_CLEAR_TABLE)
    }

    fun hasDiscountPermission(root: View?): Boolean {
        return checkPermission(root, MODULE_DISCOUNT)
    }

    fun hasInventoryPermission(root: View?): Boolean {
        return checkPermission(root, MODULE_INVENTORY)
    }

    fun hasManualSalesPermission(root: View?): Boolean {
        return checkPermission(root, MODULE_MANUAL_SALES)
    }

    fun hasCancelOrderPermission(root: View?): Boolean {
        return checkPermission(root, MODULE_CANCEL_ORDER)
    }
}