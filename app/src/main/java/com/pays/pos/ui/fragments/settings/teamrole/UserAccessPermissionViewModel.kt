package com.pays.pos.ui.fragments.settings.teamrole

import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pays.pos.R
import com.pays.pos.data.entities.Employee
import com.pays.pos.data.entities.ModulePermission
import com.pays.pos.data.entities.TeamRole
import com.pays.pos.data.model.PermissionModuleListModel
import com.pays.pos.data.model.requestModel.CreateTeamRoleRequestModel
import com.pays.pos.data.model.responseModel.*
import com.pays.pos.data.remote.Constants.LOCATION_ID
import com.pays.pos.data.repositories.PosRepository
import com.pays.pos.data.repositories.TaxServiceChargeRepository
import com.pays.pos.di.PrefProvider
import com.pays.pos.di.RolePermission
import com.pays.pos.utils.Event
import com.pays.pos.utils.statusUtils.Resource
import com.pays.pos.utils.statusUtils.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class UserAccessPermissionViewModel @Inject constructor(
    private val taxServiceChargeRepository: TaxServiceChargeRepository,
    private val prefProvider: PrefProvider,
    private val posRepository: PosRepository,
    private val rolePermission : RolePermission
) : ViewModel() {

    val locationId = prefProvider.getValueInt(LOCATION_ID, 0)

    val createUserPermission =
        MutableLiveData(CreateTeamRoleRequestModel())

    private val _snackbarText = MutableLiveData<Event<Any?>>()
    val snackbarText: LiveData<Event<Any?>> = _snackbarText

    private val _data = MutableLiveData<Event<GetUserPermissionListResponse?>>()
    val data: LiveData<Event<GetUserPermissionListResponse?>> = _data

    private val _showProgress = MutableLiveData<Event<Boolean>>()
    val showProgress: LiveData<Event<Boolean>> = _showProgress

    private val _showEmployeeListDialog = MutableLiveData<Event<TeamRole>>()
    val showEmployeeListDialog: LiveData<Event<TeamRole>> = _showEmployeeListDialog

    private var roleId: Int = -1

    private var isEdit: Boolean = false


    private lateinit var createTeamRoleRequestModel: CreateTeamRoleRequestModel

    private lateinit var resource: Resource<GetUserPermissionListResponse>

    val employeeData = posRepository.employeesList(locationId)
    val getEmployeeListDatabse = posRepository.getEmployeeListDatabse()
    val getTeamRoleList = taxServiceChargeRepository.getTeamRoleList()
    val getTeamModules = taxServiceChargeRepository.getTeamModules()

    private val _allEmployeeList = MutableLiveData<ArrayList<Employee>>()
    val allEmployeeList: LiveData<ArrayList<Employee>> = _allEmployeeList

    private val _selectedEmployeeList =
        MutableLiveData<ArrayList<Employee>>()
    val selectedEmployeeList: LiveData<ArrayList<Employee>> =
        _selectedEmployeeList

    val allEmployeeListToFeed = ArrayList<Employee>()
    val selectedEmployeeListToFeed = ArrayList<Employee>()

    private val _allModuleList = MutableLiveData<ArrayList<ModulePermission>>()
    val allModuleList: LiveData<ArrayList<ModulePermission>> = _allModuleList

    private val _selectedModuleList = MutableLiveData<ArrayList<ModulePermission>>()
    val selectedModuleList: LiveData<ArrayList<ModulePermission>> =
        _selectedModuleList


    val allModuleListToFeed = ArrayList<ModulePermission>()
    val selectedModuleListToFeed = ArrayList<ModulePermission>()


    fun setUserPermissionData(userPermissionData: TeamRole) {
        createUserPermission.value?.name = userPermissionData.name
        selectedEmployeeListToFeed.clear()
        selectedEmployeeListToFeed.addAll(userPermissionData.employees!!)
        _selectedEmployeeList.value = selectedEmployeeListToFeed

        val myCollection = allEmployeeListToFeed
        val iterator = myCollection.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            selectedEmployeeListToFeed.forEach { selectedEmployee ->
                if (item.id == selectedEmployee.id) {
                    iterator.remove()
                    _allEmployeeList.value = allEmployeeListToFeed

                }
            }
        }


    }

    fun setModuleData(userPermissionData: TeamRole) {
        selectedModuleListToFeed.clear()
        selectedModuleListToFeed.addAll(userPermissionData.modulePermission!!)
        _selectedModuleList.value = selectedModuleListToFeed

        val myCollection = allModuleListToFeed
        val iterator = myCollection.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            selectedModuleListToFeed.forEach { selectedEmployee ->
                if (item.id == selectedEmployee.id) {
                    iterator.remove()
                    _allModuleList.value = allModuleListToFeed

                }
            }
        }


    }

    fun isEditData(isEdit: Boolean, roleId: Int) {
        this.roleId = roleId
        this.isEdit = isEdit
    }

    fun setEmployeeList(employeeList: List<Employee>) {
        allEmployeeListToFeed.clear()
        allEmployeeListToFeed.addAll(employeeList)
    }

    fun employeeRemoved(
        employeeName: Employee,
        isEmployeeRemoved: Boolean
    ) {
        if (isEmployeeRemoved) {
            allEmployeeListToFeed.remove(employeeName)
            selectedEmployeeListToFeed.add(employeeName)

        } else {
            allEmployeeListToFeed.add(employeeName)
            selectedEmployeeListToFeed.remove(employeeName)
        }
        passEmployeeDataToUI()
    }

    fun addAllEmployee() {
        selectedEmployeeListToFeed.addAll(allEmployeeListToFeed)
        allEmployeeListToFeed.removeAll(allEmployeeListToFeed)
        passEmployeeDataToUI()
    }

    fun removeAllEmployee() {
        allEmployeeListToFeed.addAll(selectedEmployeeListToFeed)
        selectedEmployeeListToFeed.removeAll(selectedEmployeeListToFeed)
        passEmployeeDataToUI()
    }

    fun passEmployeeDataToUI() {
        _allEmployeeList.value = allEmployeeListToFeed
        _selectedEmployeeList.value = selectedEmployeeListToFeed
    }


    fun setModuleList(timeSheet: List<ModulePermission>) {
        allModuleListToFeed.clear()
        allModuleListToFeed.addAll(timeSheet)
    }

    fun permissionModuleRemoved(
        employeeName: ModulePermission,
        isModuleRemoved: Boolean
    ) {
        if (isModuleRemoved) {
            allModuleListToFeed.remove(employeeName)
            selectedModuleListToFeed.add(employeeName)

        } else {
            allModuleListToFeed.add(employeeName)
            selectedModuleListToFeed.remove(employeeName)
        }
        passModuleDataToUI()

    }

    fun addAllModule() {
        selectedModuleListToFeed.addAll(allModuleListToFeed)
        allModuleListToFeed.removeAll(allModuleListToFeed)
        passModuleDataToUI()
    }

    fun removeAllModule() {
        allModuleListToFeed.addAll(selectedModuleListToFeed)
        selectedModuleListToFeed.removeAll(selectedModuleListToFeed)
        passModuleDataToUI()
    }

    private fun passModuleDataToUI() {
        _allModuleList.value = allModuleListToFeed
        _selectedModuleList.value = selectedModuleListToFeed
    }

    fun assignMember(teamRole: TeamRole) {

        _showEmployeeListDialog.value = Event(teamRole)
    }

    fun assignRole(selectedItemList: ArrayList<Employee>, teamRole: TeamRole) {
        //this.teamRole = teamRole
        _showProgress.value = Event(true)
        createTeamRoleRequestModel = CreateTeamRoleRequestModel().apply {
            id = teamRole.id
            name = teamRole.name
            val idList = ArrayList<Int>()
            selectedItemList.forEach {
                idList.add(it.id)
            }
            employeeIds = idList

            val moduleidList = ArrayList<Int>()
            teamRole.modulePermission?.forEach {
                moduleidList.add(it.id)
            }
            moduleIds = moduleidList

        }
        viewModelScope.launch {

            resource = taxServiceChargeRepository.updateTeamRole(
                teamRole.id,
                createTeamRoleRequestModel
            )

            when (resource.status) {
                Status.SUCCESS -> {
                    _showProgress.value = Event(false)
                    resource.data.let { logInResponse ->
                        if (logInResponse?.status == 200) {

                            resource.data?.let { createTeamRole ->

                                val role = createTeamRole.data.teamRoles
                                rolePermission.findCurrentUserRoleAndSave(role)
                                taxServiceChargeRepository.createTeamRoleDatabase(
                                    role
                                )
                                _data.value = Event(createTeamRole)
                            }
                        } else {
                            _snackbarText.value = Event(resource.message)
                        }
                    }
                }

                Status.ERROR -> {
                    _snackbarText.value = Event(resource.message)
                    _showProgress.value = Event(false)
                }

                Status.LOADING -> {
                    _showProgress.value = Event(true)
                }
            }
        }
    }


    fun submit() {
        val value = createUserPermission.value
        if (TextUtils.isEmpty(value?.name?.trim())) {
            _snackbarText.value = Event(R.string.role_name_validate)
        } else {
            _showProgress.value = Event(true)

            createTeamRoleRequestModel = CreateTeamRoleRequestModel().apply {
                if (isEdit) id = roleId
                name = value!!.name.trim().replace("\\s+".toRegex(), " ")
                val idList = ArrayList<Int>()
                selectedEmployeeListToFeed.forEach {
                    idList.add(it.id)
                }
                employeeIds = idList

                val moduleidList = ArrayList<Int>()
                selectedModuleListToFeed.forEach {
                    moduleidList.add(it.id)
                }
                moduleIds = moduleidList

            }

            viewModelScope.launch {
                if (isEdit) {
                    resource = taxServiceChargeRepository.updateTeamRole(
                        roleId,
                        createTeamRoleRequestModel
                    )
                } else {
                    resource = taxServiceChargeRepository.createTeamRole(createTeamRoleRequestModel)
                }

                when (resource.status) {
                    Status.SUCCESS -> {
                        _showProgress.value = Event(false)
                        resource.data.let { logInResponse ->
                            if (logInResponse?.status == 200) {

                                resource.data?.let { createTeamRole ->

                                    val role = createTeamRole.data.teamRoles
                                    rolePermission.findCurrentUserRoleAndSave(role)
                                    taxServiceChargeRepository.createTeamRoleDatabase(
                                        role
                                    )

                                    removeAllEmployee()
                                    removeAllModule()
                                    _data.value = Event(createTeamRole)
                                }
                            } else {
                                _snackbarText.value = Event(resource.message)
                            }
                        }
                    }

                    Status.ERROR -> {
                        _snackbarText.value = Event(resource.message)
                        _showProgress.value = Event(false)
                    }

                    Status.LOADING -> {
                        _showProgress.value = Event(true)
                    }
                }
            }

        }

    }


}