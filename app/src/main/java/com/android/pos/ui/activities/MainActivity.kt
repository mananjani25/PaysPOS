package com.android.pos.ui.activities

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.android.pos.R
import com.android.pos.data.remote.Constants
import com.android.pos.data.remote.Constants.IS_CLOCKOUT
import com.android.pos.data.remote.Constants.PASSCODE
import com.android.pos.data.remote.Constants.TERMINAL_ID
import com.android.pos.data.repositories.UserRepository
import com.android.pos.databinding.ParentActivityBinding
import com.android.pos.di.PrefProvider
import com.android.pos.ui.fragments.dashboard.DashboardCategoryNew
import com.android.pos.utils.extensions.alert
import com.android.pos.utils.statusUtils.Status
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private var builder: Dialog? = null
    private lateinit var binding: ParentActivityBinding
    private var navController: NavController? = null
    private lateinit var listner: NavController.OnDestinationChangedListener

    @Inject
    lateinit var prefProvider: PrefProvider

    @Inject
    lateinit var repo: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            window.statusBarColor = getColor(R.color.txtColorGray)
        }
        binding = DataBindingUtil.setContentView(this, R.layout.parent_activity)

        binding.lifecycleOwner = this

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFrag) as NavHostFragment
        navController = navHostFragment.navController

//        navController = findNavController(R.id.navHostFrag) as NavHostFragment

        listner = NavController.OnDestinationChangedListener { controller, destination, arguments ->

            if (destination.id == R.id.dashboard || destination.id == R.id.dashboardCategory || destination.id == R.id.teamList ||
                destination.id == R.id.settings || destination.id == R.id.inventory || destination.id == R.id.reports || destination.id == R.id.customer
            ) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            } else {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

            }
        }
        binding.navView.setupWithNavController(navController!!)
        val imgBack = binding.navView.getHeaderView(0).findViewById<ImageView>(R.id.imgBack)
        imgBack.setOnClickListener {
            disableDrawer()

        }

        binding.navView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.menuHome -> {
                    disableDrawer()
                    navController?.navigate(R.id.action_global_dashboardCategory)
                    return@setNavigationItemSelectedListener true
                }
                R.id.menuOrders -> {
                    disableDrawer()
                    navController?.navigate(R.id.action_global_orders)
                    return@setNavigationItemSelectedListener true
                }
                R.id.menuTransactions -> {
                    disableDrawer()
                    navController?.navigate(R.id.action_global_transactionFragment)
                    return@setNavigationItemSelectedListener true
                }
                R.id.menuCashLog -> {
                    disableDrawer()
                    navController?.navigate(R.id.action_global_cashLogFragment)
                    return@setNavigationItemSelectedListener true
                }
                R.id.menuReports -> {
                    disableDrawer()
                    navController?.navigate(R.id.action_global_reports)
                    return@setNavigationItemSelectedListener true
                }

                R.id.menuCustomers -> {
                    disableDrawer()
                    navController?.navigate(R.id.action_global_customer)
                    return@setNavigationItemSelectedListener true
                }
                R.id.menuTeam -> {
                    disableDrawer()
                    navController?.navigate(R.id.action_global_teamList)
                    return@setNavigationItemSelectedListener true
                }
                R.id.memuInventory -> {
                    disableDrawer()
                    navController?.navigate(R.id.action_global_inventory)
                    return@setNavigationItemSelectedListener true
                }
                R.id.menuSettings -> {
                    disableDrawer()
                    navController?.navigate(R.id.action_global_settings)
                    return@setNavigationItemSelectedListener true
                }
                R.id.menuSupport -> {
                    disableDrawer()
                    return@setNavigationItemSelectedListener true
                }

                R.id.menuLogout -> {

                    alertLogout()
                    //logoutAPI()

                    return@setNavigationItemSelectedListener true

                }

            }

            return@setNavigationItemSelectedListener false
        }
        // setContentView(R.layout.fragment_custom_item)


//         passCodeView.setKeyTextColor(resources.getColor(R.color.white))
//
//         val typeface: Typeface? =
//             ResourcesCompat.getFont(this, R.font.sf_pro_display_regular)
//         passCodeView.setTypeFace(typeface)
    }

    fun alertLogout() {
        alert("Logout", "Are You Sure want to Logout?") {
            this.positiveButton("Logout") {
                logoutAPI()
            }

            this.negativeButton("Cancel") {


            }

        }
    }


    private fun logout() {
        prefProvider.setValue(Constants.AUTH_TOKEN, "")
        navController?.navigate(R.id.action_global_login)
    }

    override fun onResume() {
        super.onResume()
        navController?.addOnDestinationChangedListener(listner)
    }

    override fun onPause() {
        super.onPause()
        navController?.removeOnDestinationChangedListener(listner)
    }

    fun enableDrawer() {
        binding.drawerLayout.openDrawer(GravityCompat.START)

    }

    fun disableDrawer() {
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        navController?.navigateUp()
        return super.onSupportNavigateUp()
    }

    fun logoutAPI() {

        showDialog()

        lifecycleScope.launch {
            val dataClockout = HashMap<String, String>()
            dataClockout["passcode"] = prefProvider.getValue(PASSCODE, "").toString()
            dataClockout["terminal_id"] = prefProvider.getValueInt(TERMINAL_ID, -1).toString()

            val resourceClockout = repo.employeeClockOut(dataClockout)
            when (resourceClockout.status) {
                Status.SUCCESS -> {
                    prefProvider.setValueboolean(IS_CLOCKOUT, true)
                    resourceClockout.data.let {
                        if (it?.status == 200) {
                            resourceClockout.data?.let {
                                val data = HashMap<String, String>()
                                data["email"] =
                                    prefProvider.getValue(Constants.EMAIL, "").toString()
                                val resource = repo.logout(data)
                                when (resource.status) {
                                    Status.SUCCESS -> {
                                        resource.data?.let { it ->
                                            dismissDialog()
                                            if (it?.status == 200) {
                                                clearPreferances()
                                                disableDrawer()
                                                logout()
                                            }

                                        }
                                    }
                                    Status.LOADING -> {


                                    }
                                    Status.ERROR -> {
                                        dismissDialog()

                                    }

                                }
                            }
                        } else {
                        }

                    }

                }

                Status.ERROR -> {
                    dismissDialog()
                }

                Status.LOADING -> {
                }
            }


        }
    }

    private fun clearPreferances() {
        prefProvider.setClear()
    }

    private fun showDialog() {
        if (builder == null) {
            builder = Dialog(this@MainActivity)
        }
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.view_loading, null)
        builder?.setContentView(dialogView)
        builder?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        builder?.window?.setBackgroundDrawable(
            ColorDrawable(Color.WHITE)
        )
        builder?.setCanceledOnTouchOutside(false)
        builder?.setCancelable(false)
        builder?.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        if (!builder!!.isShowing) {
            if (!this.isFinishing) {
                builder!!.show()
            }
        }

    }

    private fun dismissDialog() {
        if (builder != null && builder!!.isShowing) {
            builder?.dismiss()
        }

    }
}