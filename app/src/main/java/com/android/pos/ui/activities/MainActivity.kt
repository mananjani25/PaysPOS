package com.android.pos.ui.activities

import android.Manifest
import android.app.Dialog
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.android.pos.BuildConfig
import com.android.pos.R
import com.android.pos.data.remote.Constants
import com.android.pos.data.repositories.UserRepository
import com.android.pos.databinding.ParentActivityBinding
import com.android.pos.di.ApiModule.BASE_URL
import com.android.pos.di.HostSelectionInterceptor
import com.android.pos.di.PrefProvider
import com.android.pos.di.RolePermission
import com.android.pos.ui.fragments.settings.hardware.Hardware
import com.android.pos.utils.AlertUtils
import com.android.pos.utils.FileUtils
import com.android.pos.utils.ProgressUtils
import com.android.pos.utils.extensions.alert
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.IOException
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : BaseScannerActivity() {

    private var cameraUri: Uri? = null
    private var selectedFilePath: String? = ""
    private var builder: Dialog? = null
    private lateinit var binding: ParentActivityBinding
    private var navController: NavController? = null
    private lateinit var listner: NavController.OnDestinationChangedListener
    private val viewModel by viewModels<MainViewModel>()
    var activityResultCallBack: ActivityResultCallBack? = null
    private val TAG = "MainActivity"


    @set:Inject
    internal var prefProvider: PrefProvider? = null

    @set:Inject
    var hostSelectionInterceptor: HostSelectionInterceptor? = null

    @Inject
    lateinit var rolePermission: RolePermission

    @Inject
    lateinit var repo: UserRepository
    private lateinit var mFirebaseAnalytics: FirebaseAnalytics
    var broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            var message = intent?.getStringExtra("message")
            var isAuto = intent?.getBooleanExtra("isAuto", false)
            if (isAuto == true) {
                AlertUtils.showCustomAlertWithYesNoListener(
                    context,
                    message
                ) { _, _ ->
                    clockoutFromSystem()
                }
            } else {
                AlertUtils.showCustomAlertWithYesNoListener(
                    context,
                    "You are clocked out in different System.\n Do you want to clock out forcefully in your System."
                ) { _, _ ->
                    clockoutFromSystem()
                }
            }
        }

    }

    private fun clockoutFromSystem() {
        prefProvider?.setValueInt(Constants.EMPLOYEE_ID, 0)
        prefProvider?.setValue(Constants.EMPLOYEE_NAME, "")
        prefProvider?.setValue(
            Constants.EMPLOYEE_ROLE,
            ""
        )
        prefProvider?.setValueInt(
            Constants.EMPLOYEE_ROLE_ID,
            0
        )
        prefProvider?.setValue(Constants.PASSCODE, "")
        var bundle: Bundle = Bundle()
        bundle.putBoolean("isSwap", true)
        bundle.putBoolean("isDashboard", false)
        bundle.putBoolean("isExit", true)
        navController?.navigate(R.id.action_global_login, bundle)


    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        registerReceiver(broadcastReceiver, IntentFilter(Constants.SEND_CLOCKOUT_NOTIFICATION))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            window.statusBarColor = getColor(R.color.txtColorGray)
        }
        binding = DataBindingUtil.setContentView(this, R.layout.parent_activity)
        supportActionBar?.hide()
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
                R.id.menuHardware -> {
                    disableDrawer()
                    navController?.navigate(R.id.hardware)
                    return@setNavigationItemSelectedListener true
                }
                R.id.menuOrders -> {
                    disableDrawer()
                    navController?.navigate(R.id.action_global_orders)
                    return@setNavigationItemSelectedListener true
                }
                R.id.menuTransactions -> {
                    disableDrawer()
                    if (rolePermission.hasTransactionPermission(binding.root)) {
                        navController?.navigate(R.id.action_global_transactionFragment)
                        return@setNavigationItemSelectedListener true
                    }
                }
                R.id.menuCashLog -> {
                    disableDrawer()
                    if (rolePermission.hasCashLogPermission(binding.root)) {
                        navController?.navigate(R.id.action_global_cashLogFragment)
                        return@setNavigationItemSelectedListener true
                    }
                }
                R.id.menuReports -> {
                    disableDrawer()
                    navController?.navigate(R.id.action_global_reports)
                    return@setNavigationItemSelectedListener true
                }

                R.id.menuCustomers -> {
                    disableDrawer()
                    if (rolePermission.hasCustomerPermission(binding.root)) {
                        navController?.navigate(R.id.action_global_customer)
                        return@setNavigationItemSelectedListener true
                    }
                }
                R.id.menuTeam -> {
                    disableDrawer()
                    navController?.navigate(R.id.action_global_teamList)
                    return@setNavigationItemSelectedListener true
                }
                R.id.memuInventory -> {
                    disableDrawer()
                    if (rolePermission.hasInventoryPermission(binding.root)) {
                        navController?.navigate(R.id.action_global_inventory)
                        return@setNavigationItemSelectedListener true
                    }
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
                    return@setNavigationItemSelectedListener true

                }

            }

            return@setNavigationItemSelectedListener false
        }

        observeShowProgress()

    }

    fun alertLogout() {
        alert("", "Are you sure you want to Logout?") {
            this.positiveButton("Logout") {
                viewModel.logoutAPI()
            }
            this.negativeButton("Cancel") {
            }

        }
    }


    private fun logout() {
        prefProvider?.setValue(Constants.AUTH_TOKEN, "")
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

    private fun disableDrawer() {
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun clearPreferences() {
        prefProvider?.setClear()
    }

    private fun observeShowProgress() {

        viewModel.showProgress.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    ProgressUtils.showProgressDialog(this)
                } else {
                    ProgressUtils.dismissProgressDialog()
                }
            }
        }


        viewModel.logout.observe(this) { event ->
            event.getContentIfNotHandled()?.let {
                if (it) {
                    clearPreferences()
                    disableDrawer()
                    logout()
                    prefProvider?.setValue(Constants.BASE_URL_NEW, BASE_URL)
                    hostSelectionInterceptor?.setHostBaseUrl()

                    viewModel.clearTable()


                }
            }
        }

    }

    //Capture Photo
    public fun capturePhoto() {
        selectedFilePath = null
        //Create a file to store the image
        var photoFile: File? = null
        try {
            photoFile = FileUtils.createImageOrVideoFile(this, Constants.MEDIA_TYPE_IMAGE)
        } catch (ex: IOException) {
            ex.printStackTrace()
            Toast.makeText(this, R.string.error_something_wrong, Toast.LENGTH_SHORT).show()
        }

        photoFile?.let { photo ->
            selectedFilePath = photo.absolutePath
            cameraUri = FileProvider.getUriForFile(
                this,
                BuildConfig.APPLICATION_ID + ".provider",
                photo
            )
            val pictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                pictureIntent.clipData = ClipData.newRawUri("", cameraUri)
                pictureIntent.addFlags(
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            if (pictureIntent.resolveActivity(this.packageManager) != null) {
                this.startActivityForResult(pictureIntent, Constants.REQUEST_GET_IMAGE_CAMERA)
            } else {
                Toast.makeText(
                    this,
                    R.string.error_camera_app_not_found,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == Constants.REQUEST_GET_IMAGE_CAMERA && cameraUri != null) {
                if (selectedFilePath == null && cameraUri != null) {
                    selectedFilePath = FileUtils.getPath(this, cameraUri!!)
                }
                //received new file path
                activityResultCallBack?.onReceivedCameraCapturedPath(
                    mediaType = Constants.MEDIA_TYPE_IMAGE,
                    mediaPath = selectedFilePath
                )
            }
        }
    }

    interface ActivityResultCallBack {
        fun onReceivedCameraCapturedPath(mediaType: Int, mediaPath: String?)
    }

    fun requestLocationPermissions(): Boolean {
        val permissionsLocation = arrayOf<String>(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        return if ((ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED)
        ) {
            true
        } else {
            ActivityCompat.requestPermissions(
                this,
                permissionsLocation,
                Constants.REQUEST_LOCATION_PERMISSION
            )
            false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.e("!_@_", "$requestCode")
        when (requestCode) {
            Constants.REQUEST_LOCATION_PERMISSION ->
                if (permissions.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //permission with request code 1 granted
                    Log.e("!_@_", "Permission Granted")
                    requestCallBack?.invoke()
                } else {
                    //permission with request code 1 was not granted
                    Log.e("!_@_", "Permission not granted")
                }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    fun getRequestCallBack(requestGrantedCallBack: (() -> Unit)) {
        this.requestCallBack = requestGrantedCallBack
    }

    private var requestCallBack: (() -> Unit)? = null

    override fun onSupportNavigateUp(): Boolean {
        navController?.navigateUp()
        return super.onSupportNavigateUp()
    }

    fun getSpecificFragment(fragmentTag: Int): Fragment? {
        val navHostFragment: Fragment? = supportFragmentManager.findFragmentById(R.id.navHostFrag)
        if (navHostFragment?.childFragmentManager != null) {
            val fragmentList: List<Fragment> = navHostFragment.childFragmentManager.fragments
            for (fragment in fragmentList) {
                if (Constants.FRAGMENT_HARDWARE == fragmentTag && fragment is Hardware) {
                    return (fragment as Hardware)
                }
            }
        }
        return null
    }

    public var fragmentCallBack: ((Fragment?) -> Unit)? = null
    fun loadFragmentInSettings(fragment: Fragment?) {
        fragmentCallBack?.invoke(fragment)
    }
}